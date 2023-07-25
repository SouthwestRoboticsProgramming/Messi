package com.swrobotics.lib.net;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.trajectory.TrapezoidProfile.Constraints;

public final class NTUtil {
    public static PIDController tunablePID(NTEntry<Double> kP, NTEntry<Double> kI, NTEntry<Double> kD) {
        PIDController pid = new PIDController(kP.get(), kI.get(), kD.get());
        kP.onChange(pid::setP);
        kI.onChange(pid::setI);
        kD.onChange(pid::setD);
        return pid;
    }

    public static ProfiledPIDController tunableProfiledPID(NTEntry<Double> kP, NTEntry<Double> kI, NTEntry<Double> kD, Constraints constraints) {
        ProfiledPIDController pid = new ProfiledPIDController(kP.get(), kI.get(), kD.get(), constraints);
        kP.onChange(pid::setP);
        kI.onChange(pid::setI);
        kD.onChange(pid::setD);
        return pid;
    }
}
