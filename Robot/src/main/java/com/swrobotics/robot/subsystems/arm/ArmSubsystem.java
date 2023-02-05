package com.swrobotics.robot.subsystems.arm;

import com.swrobotics.lib.net.NTBoolean;
import com.swrobotics.lib.net.NTDouble;
import com.swrobotics.lib.net.NTEntry;
import com.swrobotics.lib.net.NTString;
import com.swrobotics.mathlib.MathUtil;
import com.swrobotics.mathlib.Vec2d;
import com.swrobotics.messenger.client.MessengerClient;
import com.swrobotics.robot.subsystems.arm.joint.ArmJoint;
import com.swrobotics.robot.subsystems.arm.joint.PhysicalJoint;
import com.swrobotics.robot.subsystems.arm.joint.ArmPhysicsSim;
import com.swrobotics.shared.arm.ArmPose;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.smartdashboard.Mechanism2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import java.util.List;

import static com.swrobotics.robot.subsystems.arm.ArmPathfinder.toStateSpaceVec;
import static com.swrobotics.shared.arm.ArmConstants.*;

// All arm kinematics is treated as a 2d coordinate system, with
// the X axis representing forward, and the Y axis representing up
public final class ArmSubsystem extends SubsystemBase {
    // CAN IDs of the motors  FIXME
    private static final int BOTTOM_MOTOR_ID = 7612;
    private static final int TOP_MOTOR_ID = 7613;

    private static final NTDouble SPEED = new NTDouble("Arm/Speed", 0.5);
    private static final NTDouble STOP_TOL = new NTDouble("Arm/Stop Tolerance", 0.01);
    private static final NTDouble START_TOL = new NTDouble("Arm/Start Tolerance", 0.02); // Must be larger than stop tolerance
    private static final NTDouble FOLLOW_TOL = new NTDouble("Arm/Follow Tolerance", 0.04);

    private static final NTBoolean HOME_CALIBRATE = new NTBoolean("Arm/Home/Calibrate", false);
    private static final NTDouble HOME_BOTTOM = new NTDouble("Arm/Home/Bottom", 0);
    private static final NTDouble HOME_TOP = new NTDouble("Arm/Home/Top", 0);

    private static final NTEntry<Double> LOG_CURRENT_BOTTOM = new NTDouble("Log/Arm/Current Bottom", 0).setTemporary();
    private static final NTEntry<Double> LOG_CURRENT_TOP = new NTDouble("Log/Arm/Current Top", 0).setTemporary();
    private static final NTEntry<Double> LOG_TARGET_BOTTOM = new NTDouble("Log/Arm/Target Bottom", 0).setTemporary();
    private static final NTEntry<Double> LOG_TARGET_TOP = new NTDouble("Log/Arm/Target Top", 0).setTemporary();
    private static final NTEntry<Double> LOG_MOTOR_BOTTOM = new NTDouble("Log/Arm/Motor Out Bottom", 0).setTemporary();
    private static final NTEntry<Double> LOG_MOTOR_TOP = new NTDouble("Log/Arm/Motor Out Top", 0).setTemporary();

    private final ArmJoint topJoint, bottomJoint;
    private final ArmPathfinder finder;
    private ArmPose targetPose;
    private boolean inTolerance;

    private final ArmVisualizer currentVisualizer;
    private final ArmVisualizer targetVisualizer;

    private final ArmPhysicsSim sim;

    public ArmSubsystem(MessengerClient msg) {
        if (RobotBase.isSimulation()) {
            sim = new ArmPhysicsSim();
            bottomJoint = sim.getBottomJoint();
            topJoint = sim.getTopJoint();
        } else {
            sim = null;
            bottomJoint = new PhysicalJoint(BOTTOM_MOTOR_ID, BOTTOM_GEAR_RATIO);
            topJoint = new PhysicalJoint(TOP_MOTOR_ID, TOP_GEAR_RATIO);
        }

        double extent = (BOTTOM_LENGTH + TOP_LENGTH) * 2;
        Mechanism2d mechanism = new Mechanism2d(extent, extent);
        currentVisualizer = new ArmVisualizer(
                extent / 2, extent / 2,
                mechanism, "Current Arm",
                Color.kDarkGreen, Color.kGreen
        );
        targetVisualizer = new ArmVisualizer(
                extent / 2, extent / 2,
                mechanism, "Target Arm",
                Color.kDarkRed, Color.kRed
        );
        SmartDashboard.putData("Arm", mechanism);

        finder = new ArmPathfinder(msg);

        ArmPose home = new ArmPose(HOME_BOTTOM.get(), HOME_TOP.get());
        calibrate(home);
        targetPose = home;
        inTolerance = false;

        msg.addHandler("Debug:ArmSetTarget", (type, reader) -> {
            double x = reader.readDouble();
            double y = reader.readDouble();
            setTargetPosition(new Translation2d(x, y));
        });
    }

    public void calibrate(ArmPose currentPose) {
        bottomJoint.setCurrentAngle(currentPose.bottomAngle);
        topJoint.setCurrentAngle(currentPose.topAngle);
    }

    public ArmPose getCurrentPose() {
        return new ArmPose(bottomJoint.getCurrentAngle(), topJoint.getCurrentAngle());
    }

    @Override
    public void simulationPeriodic() {
        sim.update();
    }

    private void idle() {
        LOG_MOTOR_BOTTOM.set(0.0);
        LOG_MOTOR_TOP.set(0.0);
        bottomJoint.setMotorOutput(0);
        topJoint.setMotorOutput(0);
    }

    @Override
    public void periodic() {
        ArmPose currentPose = getCurrentPose();
        if (HOME_CALIBRATE.get()) {
            HOME_BOTTOM.set(currentPose.bottomAngle);
            HOME_TOP.set(currentPose.topAngle);
            HOME_CALIBRATE.set(false);
        }

        currentVisualizer.setPose(currentPose);
        LOG_CURRENT_BOTTOM.set(currentPose.bottomAngle);
        LOG_CURRENT_TOP.set(currentPose.topAngle);

        if (targetPose == null) {
            idle();
            return;
        }

        targetVisualizer.setPose(targetPose);
        finder.setInfo(currentPose, targetPose);

        double startTol = START_TOL.get();
        double stopTol = STOP_TOL.get();
        double followTol = FOLLOW_TOL.get();

        ArmPose currentTarget = null;
        if (!finder.isPathValid()) {
            currentTarget = targetPose;
        } else {
            List<ArmPose> currentPath = finder.getPath();

            Vec2d currentPoseVec = toStateSpaceVec(currentPose);
            for (int i = currentPath.size() - 1; i > 0; i--) {
                ArmPose pose = currentPath.get(i);
                Vec2d point = toStateSpaceVec(pose);
                Vec2d prev = toStateSpaceVec(currentPath.get(i - 1));

                double dist = currentPoseVec.distanceToLineSegmentSq(point, prev);

                if (dist < followTol * followTol) {
                    currentTarget = pose;
                    status.set("following");
                    break;
                }
            }

            // If we aren't near the path, go directly to target while pathfinder catches up
            if (currentTarget == null) {
                currentTarget = targetPose;
            }
        }

        LOG_TARGET_BOTTOM.set(currentTarget.bottomAngle);
        LOG_TARGET_TOP.set(currentTarget.topAngle);

        double topAngle = MathUtil.wrap(currentTarget.topAngle + Math.PI, 0, Math.PI * 2) - Math.PI;

        Vec2d towardsTarget = new Vec2d(currentTarget.bottomAngle, topAngle)
                .sub(currentPose.bottomAngle, currentPose.topAngle);

        // Tolerance hysteresis so the motor doesn't do the shaky shaky
        double magSq = towardsTarget.magnitudeSq();
        if (magSq > startTol * startTol) {
            inTolerance = false;
        } else if (magSq < stopTol * stopTol) {
            inTolerance = true;
        }

        // Linear feedforward control
        // Assumes gear friction overrides gravity, but will still
        // account for slight movement
        double bottomMotorOut, topMotorOut;
        if (inTolerance) {
            bottomMotorOut = 0;
            topMotorOut = 0;
        } else {
            towardsTarget.normalize().mul(SPEED.get());
            bottomMotorOut = towardsTarget.x;
            topMotorOut = towardsTarget.y;
        }

        bottomJoint.setMotorOutput(bottomMotorOut);
        topJoint.setMotorOutput(topMotorOut);
        LOG_MOTOR_BOTTOM.set(bottomMotorOut);
        LOG_MOTOR_TOP.set(topMotorOut);
    }

    public void setTargetPosition(Translation2d position) {
        targetPose = ArmPose.fromEndPosition(position);
    }
}
