package com.swrobotics.lib.motor2.sim;

public interface SimControlMode<D> {
    default void begin() {}
    double calc(D demand);
}
