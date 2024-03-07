![CI](https://github.com/wpilibsuite/PathWeaver/workflows/CI/badge.svg)
# PathWeaver

PathWeaver is a front end motion planning program. It is primarily designed for FRC teams using WPILib's trajectories and splines. For more instructions on using PathWeaver, refer to the [WPILib instructions](https://docs.wpilib.org/en/stable/docs/software/wpilib-tools/pathweaver/index.html).

## Commenting
For bugs or feature suggestions, make a github issue.

## Building

To run PathWeaver use the command `./gradlew run`

### Requirements
- [JDK 11](https://adoptopenjdk.net/)

## Example Auto Program

    package frc.robot.commands;

    import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
    import frc.robot.RobotContainer;
    import frc.robot.commands.paths.DrivetrainConfig;
    import frc.robot.commands.paths.MultiPartPath;

    public class ExampleAuto extends AutoWithInit {

        public ExampleAuto() {
        }

        @Override
        public void initializeCommands() {
            // !PATHWEAVER_INFO: {"trackWidth":0.7,"gameName":"Crescendo"}
            boolean doLastPart = SmartDashboard.getBoolean("DoLastPart?", true);
            RobotContainer.drivetrain.resetAngle();

            DrivetrainConfig config = RobotContainer.drivetrain.getConfig().makeClone();
            config.maxVelocity = 4;
            config.maxAcceleration = 4;
            config.maxCentripetalAcceleration = 11;
            config.maxAngularAcceleration = 8;
            config.maxAnglularVelocity = 12;
            MultiPartPath pathA = new MultiPartPath(RobotContainer.drivetrain, config, null);
            pathA.resetPosition(0.350, 7.000);
            pathA.addWaypoint(2.050, 7.000);
            pathA.addSequentialCommand(new GrabNote());// ENDPOS:4.202,6.912
            if (doLastPart) {// path on
                pathA.addWaypoint(4.430, 5.364);
                pathA.addWaypoint(3.086, 4.931);
                pathA.addSequentialCommand(new TurnAndShoot());// ENDPOS:2.904,4.931
            }
            pathA.addStop();
            addCommands(pathA.finalizePath());
        }
    }