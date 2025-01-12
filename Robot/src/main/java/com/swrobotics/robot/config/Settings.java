package com.swrobotics.robot.config;

import edu.wpi.first.wpilibj.RobotBase;

import java.util.Map;

public class Settings {

    public static final RobotType robot = RobotType.SIMULATION;

    public static Mode getMode() {
        switch (robot) {
            case COMPETITION:
                return RobotBase.isReal() ? Mode.REAL : Mode.REPLAY;
            case SIMULATION:
                return RobotBase.isReal()
                        ? Mode.REAL
                        : Mode.SIMULATION; // Make sure we didn't accedentally leave it on
                // simulation
            default:
                return Mode.REAL;
        }
    }

    public static final Map<RobotType, String> logFolders =
            Map.of(RobotType.COMPETITION, "/media/sda1/");

    public static enum RobotType {
        COMPETITION,
        SIMULATION
    }

    public static enum Mode {
        REAL,
        REPLAY,
        SIMULATION
    }
}
