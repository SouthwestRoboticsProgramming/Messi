package com.swrobotics.robot.commands.Intake;

import com.swrobotics.lib.net.NTDouble;
import com.swrobotics.robot.subsystems.intake2.Intake2;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.CommandBase;


public class IntakeCube extends CommandBase {
    private final Intake2 intake;

    private boolean countdownstarted;
    private final NTDouble CONTINUE;

    private Timer timer;

    public IntakeCube(Intake2 intake) {
        this.intake = intake;
        this.CONTINUE = intake.CUBE_INTAKE_CONTINUE;
        addRequirements(intake);
        timer = new Timer();
    }


    @Override
    public void initialize() {
        timer.start();
        if (intake.hasElement.get()) {
            intake.Outake();
        } else {
            intake.Intake();
        }
    }

    @Override
    public void end(boolean not_needed) {
        timer.stop();
        intake.Stop();
        if (!not_needed) {

            intake.hasElement.set(!intake.hasElement.get());
        }
    }
    @Override
    public boolean isFinished() {
        if(intake.cubeBeamIsBroken() && !countdownstarted) {
            timer.start();
            countdownstarted = !countdownstarted;
        }
        return timer.hasElapsed(CONTINUE.get());
    }




}