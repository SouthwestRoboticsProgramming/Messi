package com.swrobotics.robot.positions;

import com.swrobotics.mathlib.Vec2d;
import com.swrobotics.robot.blockauto.WaypointStorage;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;

public final class ScoringPositions {
    private static final class Position {
        private final String name;
        private final Vec2d blueAlliancePos;
        private final Vec2d redAlliancePos;

        public Position(String name, double y) {
            this.name = name;

            double yMeters = Units.inchesToMeters(y);
            this.blueAlliancePos = new Vec2d(BLUE_X, yMeters);
            this.redAlliancePos = new Vec2d(RED_X, yMeters);
        }

        public Vec2d get(DriverStation.Alliance alliance) {
            if (alliance == DriverStation.Alliance.Red)
                return redAlliancePos;
            return blueAlliancePos;
        }
    }

    // In inches
    private static final double BOTTOM_TO_LOWEST_CONE = 20.19;
    private static final double CONE_SPAN_ACROSS_CUBE = 44;
    private static final double CONE_SPAN_ADJACENT = 22;
    private static final double BOTTOM_TO_LOWEST_CUBE = 42.19;
    private static final double CUBE_SPAN = 66;
    private static final double BLUE_RIGHT_X = 4*12 + 6.25;
    private static final double RED_LEFT_X = BLUE_RIGHT_X + 542.7225;

    private static final double ROBOT_SIZE_FW = 31; // FIXME
    private static final double DIST_FROM_GRIDS = 6 + ROBOT_SIZE_FW / 2;
    private static final double BLUE_X = Units.inchesToMeters(BLUE_RIGHT_X + DIST_FROM_GRIDS);
    private static final double RED_X = Units.inchesToMeters(RED_LEFT_X - DIST_FROM_GRIDS);

    private static final Position[] POSITIONS = {
            new Position("Grid 0 (CONE)", BOTTOM_TO_LOWEST_CONE),
            new Position("Grid 1 (CUBE)", BOTTOM_TO_LOWEST_CUBE),
            new Position("Grid 2 (CONE)", BOTTOM_TO_LOWEST_CONE + CONE_SPAN_ACROSS_CUBE),
            new Position("Grid 3 (CONE)", BOTTOM_TO_LOWEST_CONE + CONE_SPAN_ACROSS_CUBE + CONE_SPAN_ADJACENT),
            new Position("Grid 4 (CUBE)", BOTTOM_TO_LOWEST_CUBE + CUBE_SPAN),
            new Position("Grid 5 (CONE)", BOTTOM_TO_LOWEST_CONE + 2 * CONE_SPAN_ACROSS_CUBE + CONE_SPAN_ADJACENT),
            new Position("Grid 6 (CONE)", BOTTOM_TO_LOWEST_CONE + 2 * CONE_SPAN_ACROSS_CUBE + 2 * CONE_SPAN_ADJACENT),
            new Position("Grid 7 (CUBE)", BOTTOM_TO_LOWEST_CUBE + 2 * CUBE_SPAN),
            new Position("Grid 8 (CONE)", BOTTOM_TO_LOWEST_CONE + 3 * CONE_SPAN_ACROSS_CUBE + 2 * CONE_SPAN_ADJACENT)
    };

    public static Vec2d getPosition(int column) {
        return POSITIONS[column].get(DriverStation.getAlliance());
    }

    public static void update() {
        DriverStation.Alliance alliance = DriverStation.getAlliance();
        for (Position position : POSITIONS) {
            WaypointStorage.registerStaticWaypoint(
                    position.name,
                    position.get(alliance)
            );
        }
    }
}
