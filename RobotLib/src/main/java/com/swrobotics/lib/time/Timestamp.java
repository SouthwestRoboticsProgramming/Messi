package com.swrobotics.lib.time;

import java.util.Objects;

/** Represents a specific moment in time. */
public final class Timestamp {
    private final double nanoTime;

    /**
     * Gets the current time.
     *
     * @return current time
     */
    public static Timestamp now() {
        return new Timestamp(System.nanoTime());
    }

    /**
     * Creates a new timestamp based on time in nanoseconds.
     *
     * @param nanoTime time in nanoseconds
     */
    public Timestamp(double nanoTime) {
        this.nanoTime = nanoTime;
    }

    /**
     * Gets the duration between this timestamp and a previous time.
     *
     * @param prevTime previous time to compare
     * @return duration between previous time and this time
     */
    public Duration durationSince(Timestamp prevTime) {
        return new Duration(nanoTime - prevTime.nanoTime, TimeUnit.NANOSECONDS);
    }

    /**
     * Gets the timestamp after a specified duration from this time.
     *
     * @param dur duration
     * @return timestamp after the duration from this time
     */
    public Timestamp after(Duration dur) {
        return new Timestamp(nanoTime + dur.getDurationNanos());
    }

    /**
     * Gets whether a given timestamp is exactly this time or after this time.
     *
     * @param o timestamp to compare to
     * @return if same time or after
     */
    public boolean isAtOrAfter(Timestamp o) {
        return nanoTime >= o.nanoTime;
    }

    /**
     * Gets the timestamp after a specified duration in nanoseconds.
     *
     * @param nanos duration in nanoseconds
     * @return timestamp after the duration from this time
     */
    public Timestamp addNanos(double nanos) {
        return new Timestamp(nanoTime + nanos);
    }

    /**
     * Gets the represented time in nanoseconds.
     *
     * @return time in nanoseconds
     */
    public double getNanoTime() {
        return nanoTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Timestamp timestamp = (Timestamp) o;
        return Double.compare(timestamp.nanoTime, nanoTime) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nanoTime);
    }
}
