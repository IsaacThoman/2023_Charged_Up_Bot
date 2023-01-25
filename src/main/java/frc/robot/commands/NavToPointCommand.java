package frc.robot.commands;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.RunCommand;
import frc.robot.Constants;
import frc.robot.lib.NavigationField;
import frc.robot.subsystems.SwerveSubsystem;


public class NavToPointCommand extends CommandBase {
    private final NavigationField navigationField;
    private final SwerveSubsystem swerveSubsystem;
    Pose2d desiredPose;
    double distToFinished;
    double timeout;

    public NavToPointCommand(NavigationField navigationField, SwerveSubsystem swerveSubsystem,Pose2d desiredPose, double timeout) {
        this.navigationField = navigationField;
        this.swerveSubsystem = swerveSubsystem;
        this.desiredPose = desiredPose;
        this.timeout = timeout;
        // each subsystem used by the command must be passed into the
        // addRequirements() method (which takes a vararg of Subsystem)
        addRequirements(this.swerveSubsystem);
        navigationField.setNavPoint(desiredPose);
    }

    /**
     * The initial subroutine of a command.  Called once when the command is initially scheduled.
     */

    double startTime;
    public void initialize() {
        firstExecuteDone = false;
    }

    public void start(){
        this.startTime = Timer.getFPGATimestamp();
        navigationField.setNavPoint(desiredPose);
        navigationField.engageNav();
    }

    /**
     * The main body of a command.  Called repeatedly while the command is scheduled.
     * (That is, it is called repeatedly until {@link #isFinished()}) returns true.)
     */
    boolean firstExecuteDone = false;

    @Override
    public void execute() {
        if(!firstExecuteDone){
            start();
            firstExecuteDone = true;
        }
        swerveSubsystem.driveToPose(navigationField.getNextNavPoint());
    }


    @Override
    public boolean isFinished() {
        // TODO: Make this return true when this Command no longer needs to run execute()
        boolean timedOut = Timer.getFPGATimestamp() - startTime>timeout;
        boolean reachedGoal = Math.sqrt(Math.pow(desiredPose.getX() - swerveSubsystem.getEstimatedPosition().getX(),2)+Math.pow(desiredPose.getY() - swerveSubsystem.getEstimatedPosition().getY(),2)) < Constants.DriveConstants.posTolerance;
        return timedOut || reachedGoal;
    }


    @Override
    public void end(boolean interrupted) {
        navigationField.disengageNav();
    }
}