// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.arm;

import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.InvertType;
import com.ctre.phoenix.motorcontrol.TalonSRXControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.revrobotics.*;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

import static frc.robot.Constants.ArmConstants;

public class ArmStageTwo extends SubsystemBase {
  private CANSparkMax armMotorPrimary;
  private CANSparkMax armMotorSecondary;

  private SparkMaxPIDController pidController;
  private RelativeEncoder encoder;

  private double encoderOffset = ArmConstants.kStageTwo_AbsEncoderInitialOffset;
  private double setpointRad = encoderOffset;
  private double AFF;
  private double cG[];
  private double cB[];
  private double kCG[];
  private double kCB[];
  private double angle;

  /** Creates a new ArmStageTwo. */
  public ArmStageTwo() {
    armMotorPrimary = new CANSparkMax(ArmConstants.kStageTwo_MotorLeftChannel, CANSparkMaxLowLevel.MotorType.kBrushless); //"right" motor
    armMotorSecondary = new CANSparkMax(ArmConstants.kStageTwo_MotorRightChannel, CANSparkMaxLowLevel.MotorType.kBrushless);

    encoder =  armMotorPrimary.getAlternateEncoder(SparkMaxAlternateEncoder.Type.kQuadrature,8192); //the Alternate Encoder is automatically configured when the Alternate Encoder object is instantiated

    armMotorPrimary.restoreFactoryDefaults();
    armMotorSecondary.restoreFactoryDefaults();


    armMotorSecondary.follow(armMotorPrimary);
    armMotorSecondary.setInverted(true);

    pidController = armMotorPrimary.getPIDController();
    pidController.setFeedbackDevice(encoder);
    pidController.setP(ArmConstants.stageTwo_kP);
    pidController.setI(ArmConstants.stageTwo_kI);
    pidController.setD(ArmConstants.stageTwo_kD);
    pidController.setOutputRange(-1,1);




  }
  public void calculateStageData() {
    cG = calculateCG();
    cB = calculateCB();
  }
  public double[] calculateCB() {
    Rotation2d cBAngle = new Rotation2d(new Rotation2d(kCB[0], kCB[1]).getRadians() + angle);
    double magnitude = Math.sqrt(Math.pow(kCB[0], 2) + Math.pow(kCB[1], 2));
    return new double[] {cBAngle.getCos() * magnitude, cBAngle.getSin() * magnitude, kCB[2] * (Math.hypot(kCB[4] - cBAngle.getCos(), kCB[5] - cBAngle.getSin()) - kCB[3]), new Rotation2d(kCB[4] - cBAngle.getCos(), kCB[5] - cBAngle.getSin()).getRadians() - cBAngle.getRadians()};
  }
  public double[] calculateCG() {
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
  public void setAFF(double AFF) {
    this.AFF = AFF;
  }
  public double getEncoderRad() {
    return armMotorPrimary.getEncoder().getPosition() * ArmConstants.kstageTwo_encoderTicksToRadians;
  }


  public void limitSwitchPassed(){
    armMotorPrimary.getEncoder().setPosition(ArmConstants.kStageTwo_LimitSwitchAngleRad / Math.PI / 2.0);
  }

  public void setArmPositionRad(double setpoint){
    pidController.setReference(setpoint, CANSparkMax.ControlType.kPosition);
  }


  @Override
  public void periodic() {
    // This method will be called once per scheduler run
    //armMotorPrimary.set(TalonSRXControlMode.PercentOutput,pid.calculate(getAbsoluteEncoderRad())); //replace this with internal PID
  }
}
