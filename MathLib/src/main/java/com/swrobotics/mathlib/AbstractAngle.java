package com.swrobotics.mathlib;

import edu.wpi.first.math.geometry.Rotation2d;

import java.util.Objects;

/**
 * Implements the operations common to all types of angle, but depend on the specific type. This
 * class exists to avoid having the type parameter in Angle, as well as requiring that you know what
 * type of angle you are working with.
 *
 * @param <T> the type of the implementing class
 */
public abstract class AbstractAngle<T extends AbstractAngle<T>> implements Angle {
    private final double rad;

    // Store results of sin() and cos() to reduce trigonometry usage
    private double cacheSin = -2, cacheCos = -2;

    /**
     * Creates a new instance from a radian measurement.
     *
     * @param rad radians
     */
    protected AbstractAngle(double rad) {
        this.rad = rad;
    }

    /**
     * Creates a new instance of the type parameter from a radian measurement.
     *
     * @param rad radians
     * @return new instance
     */
    protected abstract T create(double rad);

    /**
     * Gets the angle in radians.
     *
     * @return radians
     */
    public double rad() {
        return rad;
    }

    /**
     * Gets the angle in degrees.
     *
     * @return degrees
     */
    public double deg() {
        return Math.toDegrees(rad());
    }

    /**
     * Gets the angle in rotations.
     *
     * @return rotations
     */
    public double rot() {
        return rad() / MathUtil.TAU;
    }

    /**
     * Gets the angle as a Rotation2d
     *
     * @return Rotation2d
     */
    public Rotation2d rotation2d() {
        return new Rotation2d(rad);
    }

    /**
     * Adds this angle and another angle together.
     *
     * @param o other angle to add
     * @return sum
     */
    public T add(T o) {
        return create(rad + o.rad());
    }

    /**
     * Subtracts another angle from this angle.
     *
     * @param o other angle to subtract
     * @return difference
     */
    public T sub(T o) {
        return create(rad - o.rad());
    }

    /**
     * Multiplies this angle by a given scaling factor.
     *
     * @param scalar scaling factor
     * @return scaled angle
     */
    public T mul(double scalar) {
        return create(rad * scalar);
    }

    /**
     * Divides this angle by a given scaling factor.
     *
     * @param scalar scaling factor
     * @return scaled angle
     */
    public T div(double scalar) {
        return create(rad / scalar);
    }

    /**
     * Calculates the absolute value of this angle's measure. The returned angle will be the same
     * direction as this angle, with a guaranteed >=0 measure.
     *
     * @return absolute value
     */
    public T abs() {
        return create(Math.abs(rad));
    }

    /**
     * Wraps this angle within bounds specified in radians.
     *
     * @param min minimum bound in radians
     * @param max maximum bound in radians
     * @return wrapped angle
     */
    public T wrapRad(double min, double max) {
        return create(MathUtil.wrap(rad, min, max));
    }

    /**
     * Wraps this angle within bounds specified in degrees.
     *
     * @param min minimum bound in degrees
     * @param max maximum bound in degrees
     * @return wrapped angle
     */
    public T wrapDeg(double min, double max) {
        return wrapRad(Math.toRadians(min), Math.toRadians(max));
    }

    /**
     * Wraps this angle within bounds specified in rotations.
     *
     * @param min minimum bound in rotations
     * @param max maximum bound in rotations
     * @return wrapped angle
     */
    public T wrapRot(double min, double max) {
        return wrapRad(min * MathUtil.TAU, max * MathUtil.TAU);
    }

    /**
     * Wraps this angle within specified bounds.
     *
     * @param min minimum bound
     * @param max maximum bound
     * @return wrapped angle
     */
    public T wrap(T min, T max) {
        return wrapRad(min.rad(), max.rad());
    }

    /**
     * Convenience method to wrap this angle around bounds centered at zero in radians.
     *
     * @param range minimum and maximum distance from zero in radians
     * @return wrapped angle
     */
    public T wrapRad(double range) {
        return wrapRad(-range, range);
    }

    /**
     * Convenience method to wrap this angle around bounds centered at zero in degrees.
     *
     * @param range minimum and maximum distance from zero in degrees
     * @return wrapped angle
     */
    public T wrapDeg(double range) {
        return wrapDeg(-range, range);
    }

    /**
     * Convenience method to wrap this angle around bounds centered at zero in degrees.
     *
     * @param range minimum and maximum distance from zero in rotations
     * @return wrapped angle
     */
    public T wrapRot(double range) {
        return wrapRot(-range, range);
    }

    // Calculates the absolute difference in radians between this angle and another
    private double absDiffRad(double o) {
        double normSelf = MathUtil.wrap(rad, 0, MathUtil.TAU);
        double normOther = MathUtil.wrap(o, 0, MathUtil.TAU);

        double diffRad = normOther - normSelf;
        double direct = Math.abs(diffRad);
        double wrapped = MathUtil.TAU - direct;

        return Math.min(direct, wrapped);
    }

    /**
     * Calculates the absolute difference between this angle and another. The returned angle has the
     * same direction as this angle.
     *
     * @param o other angle
     * @return absolute difference
     */
    public T getAbsDiff(T o) {
        return create(absDiffRad(o.rad()));
    }

    /**
     * Gets whether this angle is within the specified tolerance from another angle.
     *
     * @param o angle to compare to
     * @param tol tolerance
     * @return whether this angle is within tolerance of the other
     */
    public boolean inTolerance(T o, Angle tol) {
        return absDiffRad(o.rad()) < tol.ccw().abs().rad();
    }

    @Override
    public T negate() {
        return create(-rad);
    }

    /**
     * Gets the sine of this angle.
     *
     * @return sine
     */
    public double sin() {
        if (cacheSin < -1.5) cacheSin = Math.sin(rad);
        return cacheSin;
    }

    /**
     * Gets the cosine of this angle.
     *
     * @return cosine
     */
    public double cos() {
        if (cacheCos < -1.5) cacheCos = Math.cos(rad);
        return cacheCos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof AbstractAngle)) return false;
        AbstractAngle<?> that = (AbstractAngle<?>) o;

        return this.ccw().rad() == that.ccw().rad();
    }

    @Override
    public int hashCode() {
        return Objects.hash(rad);
    }
}
