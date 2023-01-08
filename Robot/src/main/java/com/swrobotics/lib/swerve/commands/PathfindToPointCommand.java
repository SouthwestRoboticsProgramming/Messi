package com.swrobotics.lib.swerve.commands;

import com.swrobotics.mathlib.Vec2d;
import com.swrobotics.robot.RobotContainer;
import com.swrobotics.robot.subsystems.DrivetrainSubsystem;
import com.swrobotics.robot.subsystems.Lights;
import com.swrobotics.robot.subsystems.Pathfinder;
import com.swrobotics.robot.subsystems.Lights.IndicatorMode;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj2.command.CommandBase;

import java.util.List;

public final class PathfindToPointCommand extends CommandBase {
    // Speed at which the robot tries to go
    private static final double VELOCITY = 0.3;

    // Position tolerance in meters, must be larger than pathfinding tile
    private static final double TOLERANCE = 0.175;

    // Angle tolerance in radians
    private static final double ANGLE_TOLERANCE = Math.toRadians(3);

    private final DrivetrainSubsystem drive;
    private final Pathfinder finder;
    private final Lights lights; // For debugging; TODO: Remove

    private final Vec2d goal;
    private boolean done;

    public PathfindToPointCommand(RobotContainer robot, Vec2d goal) {
        drive = robot.m_drivetrainSubsystem;
        finder = robot.m_pathfinder;
        this.lights = robot.m_lights;
        this.goal = goal;

        addRequirements(drive);

        done = false;
    }

    @Override
    public void execute() {
        finder.setGoal(goal.x, goal.y);
        if (!finder.isPathValid()) {
            System.out.println("Path bad");
            lights.set(Lights.Color.RED);
            return;
        }
        List<Vec2d> path = finder.getPath();

        Pose2d currentPose = drive.getPose();
        Vec2d currentPosition = new Vec2d(
                currentPose.getX(),
                currentPose.getY()
        );

        // Because of latency, the starting point of the path can be significantly
        // behind the actual location
        // With the predefined path there is effectively infinite latency so this is very important
        Vec2d target = null;
        for (int i = path.size() - 1; i > 0; i--) {
            Vec2d point = path.get(i);
            Vec2d prev = path.get(i - 1);

            double dist = currentPosition.distanceToLineSegmentSq(point, prev);

            // If the robot is close enough to the line, use its endpoint as the target
            if (dist < TOLERANCE * TOLERANCE) {
                target = point;
                break;
            }
        }

        // If we aren't near the path at all, we need to wait for the pathfinder to make a valid path
        if (target == null) {
            System.err.println("Waiting for pathfinder to catch up");
            drive.setChassisSpeeds(new ChassisSpeeds(0, 0, 0));

            // Indicate that this is what is happening
            lights.set(IndicatorMode.FAILED);
            return;
        }

        // Find normal vector towards target
        double deltaX = target.x - currentPosition.x;
        double deltaY = target.y - currentPosition.y;
        double len = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (len < TOLERANCE) {
            done = true;
        }
        deltaX /= len; deltaY /= len;

        // Scale vector by velocity
        deltaX *= VELOCITY;
        deltaY *= VELOCITY;

        // Calculate speeds
        ChassisSpeeds speeds = ChassisSpeeds.fromFieldRelativeSpeeds(
                deltaX, deltaY,
                0.0,
                currentPose.getRotation()
        );

        // Move
        drive.combineChassisSpeeds(speeds);
        lights.set(IndicatorMode.GOOD); // Indicate it is working correctly
    }

    @Override
    public boolean isFinished() {
        return done;
    }
}