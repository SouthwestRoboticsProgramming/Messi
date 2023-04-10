package com.swrobotics.lib.motor.ctre;

import com.swrobotics.lib.motor.PWMMotor;
import edu.wpi.first.wpilibj.motorcontrol.VictorSP;

public final class PWMVictorSPMotor extends PWMMotor {
    public PWMVictorSPMotor(int pwmPort) {
        super(new VictorSP(pwmPort));
    }
}