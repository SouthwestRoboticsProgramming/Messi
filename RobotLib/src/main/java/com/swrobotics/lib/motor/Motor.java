package com.swrobotics.lib.motor;

/**
 * Abstraction for a motor. This contains the basic functionality every motor controller should
 * provide.
 *
 * <p>Clockwise and counterclockwise are from the perspective of looking at the face of the motor,
 * with the shaft toward you.
 */
public interface Motor {
    /**
     * Sets the percent output of the motor. When not inverted, a positive percentage should be
     * counterclockwise.
     *
     * @param percent percent output from -1 to 1
     */
    void setPercentOut(double percent);

    default void stop() {
        setPercentOut(0);
    }

    /**
     * Sets whether the motor's output should be inverted. If following another motor, this
     * determines whether to match or oppose the leader's output.
     *
     * @param inverted whether to invert output
     */
    void setInverted(boolean inverted);

    /**
     * Sets whether brake mode is enabled. If the motor does not support brake mode, this has no
     * effect.
     *
     * @param brake whether to enable brake mode
     */
    default void setBrakeMode(boolean brake) {}
}
