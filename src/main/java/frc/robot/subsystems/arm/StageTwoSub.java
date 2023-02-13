// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.arm;

import com.revrobotics.*;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import static frc.robot.Constants.ArmConstants;

public class StageTwoSub extends SubsystemBase {
  private CANSparkMax armMotorPrimary;
  private CANSparkMax armMotorSecondary;

  private SparkMaxPIDController pidController;
  private RelativeEncoder encoder;

  private double encoderOffset = ArmConstants.STAGE_TWO_ABS_ENCODER_INITIAL_OFFSET;
  private double setpointRad = encoderOffset;
  private double AFF;
  private double cG[];
  private double cB[];
  private double kCG[];
  private double kCB[];
  private double kTransmissionData[];
  private double angle;
  private double kLength;
  private double kPivotCoordinate[];
  private boolean kRedirected;
  private double kSpringMountCoordinate[];
  private double kSpringRedirectCoordinate[];
  private double kSpringRestLength;
  private double kCBCoordinate[];
  private double kSpringConstant;

  /** Creates a new ArmStageTwo. */
  public StageTwoSub() {
    armMotorPrimary = new CANSparkMax(ArmConstants.STAGE_TWO_MOTOR_LEFT_CHANNEL, CANSparkMaxLowLevel.MotorType.kBrushless); //"right" motor
    armMotorSecondary = new CANSparkMax(ArmConstants.STAGE_TWO_MOTOR_RIGHT_CHANNEL, CANSparkMaxLowLevel.MotorType.kBrushless);

    encoder =  armMotorPrimary.getAlternateEncoder(SparkMaxAlternateEncoder.Type.kQuadrature,8192); //the Alternate Encoder is automatically configured when the Alternate Encoder object is instantiated

    armMotorPrimary.restoreFactoryDefaults();
    armMotorSecondary.restoreFactoryDefaults();

    armMotorPrimary.setIdleMode(CANSparkMax.IdleMode.kCoast);
    armMotorSecondary.setIdleMode(CANSparkMax.IdleMode.kCoast);


    armMotorSecondary.follow(armMotorPrimary);
    armMotorSecondary.setInverted(true);

    pidController = armMotorPrimary.getPIDController();
    pidController.setFeedbackDevice(encoder);
    pidController.setP(ArmConstants.stageTwo_kP);
    pidController.setI(ArmConstants.stageTwo_kI);
    pidController.setD(ArmConstants.stageTwo_kD);
    pidController.setOutputRange(-1,1);

    kCG = ArmConstants.stageTwoCG;
    kTransmissionData = ArmConstants.stageTwoTransmissionData;
    kLength = ArmConstants.stageTwoLength;
    kPivotCoordinate = ArmConstants.stageTwoPivotCoordinate;
    kRedirected = ArmConstants.stageTwoRedirected;
    kSpringMountCoordinate = ArmConstants.stageTwoSpringMountCoordinate;
    kSpringRedirectCoordinate = ArmConstants.stageTwoSpringRedirectCoordinate;
    kSpringRestLength = ArmConstants.stageTwoSpringRestLength;
    kCBCoordinate = ArmConstants.stageTwoCBCoordinate;
    kSpringConstant = ArmConstants.stageTwoSpringConstant;  
  }

  private void calculateStageData() {
    cG = calculateCG();
  }


  private double[] calculateCG() {
    /*
    x, y -> angle
    angle + angle
    angle -> x, y
    x, y * magnitude
    */
    Rotation2d cGAngle = new Rotation2d(new Rotation2d(kCG[0], kCG[1]).getRadians() + angle);
    double magnitude = Math.sqrt(Math.pow(kCG[0], 2) + Math.pow(kCG[1], 2));
    return new double[] {cGAngle.getCos() * magnitude, cGAngle.getSin() * magnitude, kCG[2]};
  }
  public double[] getCG() {
    return cG;
  }
  public double[] getCB() {
    return cB;
  }
  public double getLength() {
    return kLength;
  }
  public double[] getTransmissionData() {
    return kTransmissionData;
  }
  public double getAngle() {
    return angle;
  }
  public boolean getRedirected() {
    return kRedirected;
  }
  public double[] getSpringMountCoordinate() {
    return kSpringMountCoordinate;
  }
  public double[] getSpringRedirectCoordinate() {
    return kSpringRedirectCoordinate;
  }
  public double getSpringRestLength() {
    return kSpringRestLength;
  }
  public double[] getkCBCoordinate() {
    return kCBCoordinate;
  }
  public double getSpringConstant() {
    return kSpringConstant;
  }

  public void setAFF(double AFF) {
    this.AFF = AFF;
  }
  public double getEncoderRad() {
    return armMotorPrimary.getEncoder().getPosition() * ArmConstants.STAGE_TWO_ENCODER_TICKS_TO_RADIANS;
  }
  public double[] getPivotCoordinate() {
    return kPivotCoordinate;
  }

  public void setFeedForward(double gain){
    pidController.setFF(gain);
  }

  public void limitSwitchPassed(){
    armMotorPrimary.getEncoder().setPosition(ArmConstants.STAGE_TWO_LIMIT_SWITCH_ANGLE_RAD / Math.PI / 2.0);
  }

  void setArmPositionRad(double setpoint){
    pidController.setReference(setpoint, CANSparkMax.ControlType.kPosition);
  }


  @Override
  public void periodic() {
    calculateStageData();
    // This method will be called once per scheduler run
    //armMotorPrimary.set(TalonSRXControlMode.PercentOutput,pid.calculate(getAbsoluteEncoderRad())); //replace this with internal PID
  }
}