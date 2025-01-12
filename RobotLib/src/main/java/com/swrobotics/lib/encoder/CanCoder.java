package com.swrobotics.lib.encoder;

import com.ctre.phoenix.sensors.*;
import com.swrobotics.mathlib.Angle;
import com.swrobotics.mathlib.CCWAngle;

/**
 * Represents a CTRE CANCoder, which can measure both relative and absolute angles.
 *
 * <p>Clockwise and counterclockwise are from the perspective of looking at the top (LED) side of
 * the CANCoder.
 */
public final class CanCoder {
    public static final class RelativeEncoder implements Encoder {
        public final CANCoder can;
        private double flip = 1;

        private RelativeEncoder(CANCoder can) {
            this.can = can;
        }

        @Override
        public Angle getAngle() {
            return CCWAngle.deg(can.getPosition() * flip);
        }

        @Override
        public Angle getVelocity() {
            return CCWAngle.deg(can.getVelocity() * flip);
        }

        @Override
        public void setAngle(Angle angle) {
            can.setPosition(angle.ccw().deg() * flip);
        }

        @Override
        public void setInverted(boolean inverted) {
            flip = inverted ? -1 : 1;
        }
    }

    private final CANCoder can;
    private final Encoder relative;
    private final Encoder absolute;

    /**
     * Creates a new CANCoder instance with the specified CAN id on the RoboRIO CAN bus.
     *
     * @param canID can ID of the CANCoder
     */
    public CanCoder(int canID) {
        this(canID, "");
    }

    /**
     * Creates a new CANCoder instance with the specified CAN id and bus.
     *
     * @param canID can ID of the CANCoder
     * @param canBus can bus the CANCoder is wired to
     */
    public CanCoder(int canID, String canBus) {
        can = new CANCoder(canID, canBus);

        CANCoderConfiguration config = new CANCoderConfiguration();
        config.initializationStrategy = SensorInitializationStrategy.BootToAbsolutePosition;
        config.absoluteSensorRange = AbsoluteSensorRange.Unsigned_0_to_360;
        config.sensorTimeBase = SensorTimeBase.PerSecond;
        // Configure for CCW positive reading
        config.sensorDirection = false;
        can.configAllSettings(config);

        relative = new RelativeEncoder(can);

        // Cannot set absolute position, it's absolute
        absolute =
                new Encoder() {
                    private double flip = 1;

                    @Override
                    public Angle getAngle() {
                        return CCWAngle.deg(can.getAbsolutePosition() * flip);
                    }

                    @Override
                    public Angle getVelocity() {
                        return CCWAngle.deg(can.getVelocity() * flip);
                    }

                    @Override
                    public void setInverted(boolean inverted) {
                        flip = inverted ? -1 : 1;
                    }
                };
    }

    /**
     * Gets the relative encoder output from this CANCoder. This can be used as a feedback sensor
     * for Talon FX and Talon SRX motors.
     *
     * @return relative encoder
     */
    public Encoder getRelative() {
        return relative;
    }

    /**
     * Gets the absolute encoder output from thie CANCoder. This cannot be used as a feedback
     * sensor.
     *
     * @return absolute encoder
     */
    public Encoder getAbsolute() {
        return absolute;
    }
}
