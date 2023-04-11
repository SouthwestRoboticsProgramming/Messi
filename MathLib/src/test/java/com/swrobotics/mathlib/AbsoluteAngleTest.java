package com.swrobotics.mathlib;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class AbsoluteAngleTest extends AbstractAngleTest<AbsoluteAngle> {
    @Override
    protected AbsoluteAngle create(double rad) {
        return AbsoluteAngle.rad(rad);
    }

    @Test
    public void test_cw() {
        assertEquals(AbsoluteAngle.rad(2).cw().rad(), 2, 0.0001);
        assertEquals(AbsoluteAngle.rad(-12).cw().rad(), -12, 0.0001);
        assertEquals(AbsoluteAngle.rad(45).cw().rad(), 45, 0.0001);
    }

    @Test
    public void test_ccw() {
        assertEquals(AbsoluteAngle.rad(2).ccw().rad(), 2, 0.0001);
        assertEquals(AbsoluteAngle.rad(-12).ccw().rad(), -12, 0.0001);
        assertEquals(AbsoluteAngle.rad(45).ccw().rad(), 45, 0.0001);
    }

    @Test
    public void test_abs() {
        assertEquals(AbsoluteAngle.rad(2).abs().rad(), 2, 0.0001);
        assertEquals(AbsoluteAngle.rad(-12).abs().rad(), -12, 0.0001);
        assertEquals(AbsoluteAngle.rad(45).abs().rad(), 45, 0.0001);
    }
}
