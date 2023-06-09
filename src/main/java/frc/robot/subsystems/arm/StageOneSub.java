// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.arm;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ctre.phoenix.motorcontrol.DemandType;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.InvertType;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.StatusFrameEnhanced;
import com.ctre.phoenix.motorcontrol.SupplyCurrentLimitConfiguration;
import com.ctre.phoenix.motorcontrol.TalonSRXControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;

import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.ArmConstants;
import frc.robot.Constants.RobotConstants;

public class StageOneSub extends SubsystemBase {
  private TalonSRX armMotorPrimary;
  private TalonSRX armMotorSecondary;
  private double setpoint;
  private double angle;
  private double AFF;
  private double length;
  private double springConstant;
  private double encoderRatio;
  private double mass;
  private double[] defaultCGCoordinateRelativeToPivot;
  private double[] defaultSpringStartCoordinateRelativeToPivot;
  private double[] defaultSpringEndCoordinateRelativeToPivot;
  private double restingSpringLength;
  private double voltsPerTorque;
  private double[] pivotCoordinate;
  private double softLimitForward;
  private double softLimitReverse;
  private double continuousCurrentLimit;
  private double peakCurrentLimit;
  private double peakCurrentTime;
  private double velocity;

  private double simulatedAngleRad = 0;

  /** Creates a new ArmStageOne. */
  public StageOneSub() {
    
    SmartDashboard.putNumber("stageOneVelocity", Units.radiansToDegrees(ArmConstants.stageOneMotionCruiseVelocity));
    SmartDashboard.putNumber("stageOneAccel", Units.radiansToDegrees(ArmConstants.stageOneMotionMaxAcceleration));
    SmartDashboard.putNumber("stageOneP", ArmConstants.stageOne_kP);
    SmartDashboard.putNumber("stageOneI", ArmConstants.stageOne_kI);
    SmartDashboard.putNumber("stageOneD", ArmConstants.stageOne_kD);
    SmartDashboard.putNumber("stageOneEfficiencyMultiplier", 1);
    
    instantiateConstants();
    instantiateMotorControllers();
    resetMotorControllers();
    configMotorControllers();
  }
  //Config
  private void instantiateConstants() {
    length = ArmConstants.stageOneLength;
    springConstant = ArmConstants.stageOneSpringConstant; 
    encoderRatio = ArmConstants.stageOneEncoderRatio; 
    mass = ArmConstants.stageOneMass; 
    defaultCGCoordinateRelativeToPivot = ArmConstants.stageOneDefaultCGCoordinateRelativeToPivot;
    defaultSpringStartCoordinateRelativeToPivot = ArmConstants.stageOneDefaultSpringStartCoordinateRelativeToPivot;
    defaultSpringEndCoordinateRelativeToPivot = ArmConstants.stageOneDefaultSpringEndCoordinateRelativeToPivot;
    restingSpringLength = ArmConstants.stageOneRestingSpringLength;
    voltsPerTorque = ArmConstants.stageOneOutputVoltsPerTorque;
    pivotCoordinate = ArmConstants.stageOnePivotCoordinate;
    softLimitForward = ArmConstants.stageOneSoftLimitForward;
    softLimitReverse = ArmConstants.stageOneSoftLimitReverse;
    continuousCurrentLimit = ArmConstants.stageOneContinuousCurrentLimit;
    peakCurrentLimit = ArmConstants.stageOnePeakCurrentLimit;
    peakCurrentTime = ArmConstants.stageOnePeakCurrentTime;
  }
  private void instantiateMotorControllers() {
    armMotorPrimary = new TalonSRX(ArmConstants.stageOneTalonRightID);
    armMotorSecondary = new TalonSRX(ArmConstants.stageOneTalonLeftID);
  }
  private void resetMotorControllers() {
    armMotorPrimary.configFactoryDefault(1000);
    armMotorSecondary.configFactoryDefault(1000);
  }
  private void configMotorControllers() {
    configMotorStatusFrames();
    //current limit
    armMotorPrimary.configSupplyCurrentLimit(new SupplyCurrentLimitConfiguration(true, continuousCurrentLimit, peakCurrentLimit, peakCurrentTime), 1000);
    //soft limit
    armMotorPrimary.configForwardSoftLimitThreshold(calculateEncoderFromOutput(softLimitForward - ArmConstants.stageOneEncoderOffset), 1000);
    armMotorPrimary.configReverseSoftLimitThreshold(calculateEncoderFromOutput(softLimitReverse - ArmConstants.stageOneEncoderOffset), 1000);
    armMotorPrimary.configForwardSoftLimitEnable(true, 1000);
    armMotorPrimary.configReverseSoftLimitEnable(true, 1000);
    //encoder
    armMotorPrimary.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Absolute, 0,1000);
    armMotorPrimary.configFeedbackNotContinuous(true,1000);
    //PID
    armMotorPrimary.config_kP(0,ArmConstants.stageOne_kP,1000);
    armMotorPrimary.config_kI(0,ArmConstants.stageOne_kI,1000);
    armMotorPrimary.config_IntegralZone(0, calculateEncoderFromOutput(Units.degreesToRadians(2)), 1000);
    armMotorPrimary.config_kD(0,ArmConstants.stageOne_kD,1000);
    armMotorPrimary.configClosedLoopPeriod(0, 1, 1000);
    //motion magic
    armMotorPrimary.configMotionCruiseVelocity(calculateEncoderFromOutput(ArmConstants.stageOneMotionCruiseVelocity) * 10, 1000);
    armMotorPrimary.configMotionAcceleration(calculateEncoderFromOutput(ArmConstants.stageOneMotionMaxAcceleration) * 10, 1000);
    armMotorPrimary.configMotionSCurveStrength(1, 1000);   
    //voltage compensation
    armMotorPrimary.configVoltageCompSaturation(RobotConstants.ROBOT_NOMINAL_VOLTAGE, 1000);
    armMotorPrimary.enableVoltageCompensation(true);
    //Neutral Mode
    armMotorPrimary.setNeutralMode(NeutralMode.Coast);
    //directionality
    armMotorPrimary.setSensorPhase(false);
    armMotorPrimary.setInverted(true);
    //second motor
    armMotorSecondary.follow(armMotorPrimary);
    armMotorSecondary.setInverted(InvertType.OpposeMaster);
    armMotorSecondary.configSupplyCurrentLimit(new SupplyCurrentLimitConfiguration(true, continuousCurrentLimit, peakCurrentLimit, peakCurrentTime), 1000);
    armMotorSecondary.configVoltageCompSaturation(RobotConstants.ROBOT_NOMINAL_VOLTAGE, 1000);
    armMotorSecondary.enableVoltageCompensation(true);
    armMotorSecondary.setNeutralMode(NeutralMode.Coast);
  }
  public void configMotorStatusFrames() {
    //primary
    armMotorPrimary.setStatusFramePeriod(StatusFrameEnhanced.Status_1_General, 5, 1000);
    armMotorPrimary.setStatusFramePeriod(StatusFrameEnhanced.Status_2_Feedback0, 15, 1000);
    armMotorPrimary.setStatusFramePeriod(StatusFrameEnhanced.Status_3_Quadrature, 199, 1000);
    armMotorPrimary.setStatusFramePeriod(StatusFrameEnhanced.Status_4_AinTempVbat, 211, 1000);
    armMotorPrimary.setStatusFramePeriod(StatusFrameEnhanced.Status_8_PulseWidth, 223, 1000);
    armMotorPrimary.setStatusFramePeriod(StatusFrameEnhanced.Status_10_MotionMagic, 227, 1000);
    armMotorPrimary.setStatusFramePeriod(StatusFrameEnhanced.Status_12_Feedback1, 229, 1000);
    armMotorPrimary.setStatusFramePeriod(StatusFrameEnhanced.Status_13_Base_PIDF0, 233, 1000);
    armMotorPrimary.setStatusFramePeriod(StatusFrameEnhanced.Status_14_Turn_PIDF1, 239, 1000);
    armMotorPrimary.setStatusFramePeriod(StatusFrameEnhanced.Status_21_FeedbackIntegrated, 241, 1000);
    armMotorPrimary.setStatusFramePeriod(StatusFrameEnhanced.Status_Brushless_Current, 251, 1000);
    //secondary
    armMotorSecondary.setStatusFramePeriod(StatusFrameEnhanced.Status_1_General, 20, 1000);
    armMotorSecondary.setStatusFramePeriod(StatusFrameEnhanced.Status_2_Feedback0, 197, 1000);
    armMotorSecondary.setStatusFramePeriod(StatusFrameEnhanced.Status_3_Quadrature, 199, 1000);
    armMotorSecondary.setStatusFramePeriod(StatusFrameEnhanced.Status_4_AinTempVbat, 211, 1000);
    armMotorSecondary.setStatusFramePeriod(StatusFrameEnhanced.Status_8_PulseWidth, 223, 1000);
    armMotorSecondary.setStatusFramePeriod(StatusFrameEnhanced.Status_10_MotionMagic, 227, 1000);
    armMotorSecondary.setStatusFramePeriod(StatusFrameEnhanced.Status_12_Feedback1, 229, 1000);
    armMotorSecondary.setStatusFramePeriod(StatusFrameEnhanced.Status_13_Base_PIDF0, 233, 1000);
    armMotorSecondary.setStatusFramePeriod(StatusFrameEnhanced.Status_14_Turn_PIDF1, 239, 1000);
    armMotorSecondary.setStatusFramePeriod(StatusFrameEnhanced.Status_21_FeedbackIntegrated, 241, 1000);
    armMotorSecondary.setStatusFramePeriod(StatusFrameEnhanced.Status_Brushless_Current, 251, 1000);
  }
  //Getters
  public double getLength() {
    return length;
  }
  public double getAngle() {
    if(Constants.SimulationConstants.simulationEnabled) return simulatedAngleRad;
    return angle;
  }
  public double getSpringConstant() {
    return springConstant;
  }
  public double getMass() {
    return mass;
  }
  public double[] getDefaultCGCoordinateRelativeToPivot() {
    return defaultCGCoordinateRelativeToPivot;
  }
  public double[] getDefaultSpringStartCoordinateRelativeToPivot() {
    return defaultSpringStartCoordinateRelativeToPivot;
  }
  public double[] getDefaultSpringEndCoordinateRelativeToPivot() {
    return defaultSpringEndCoordinateRelativeToPivot;
  }
  public double getRestingSpringLength() {
    return restingSpringLength;
  }
  public double getVoltsPerTorque() {
    return voltsPerTorque;
  }
  public double[] getPivotCoordinate() {
    return pivotCoordinate;
  }
  public double getVelocity() {
    return velocity;
  }
  //Setters
  public void setAFF(double AFF){
    this.AFF = AFF;
  }
  public void setSetpoint(double setpoint) {
    this.setpoint = setpoint;
  }
  //Util
  public void calculateStageData() {
    angle = getEncoderPosition();
    velocity = getEncoderVelocity();
  }
  public double calculateOutputFromEncoder(double encoder) {
    double radians = encoder * ArmConstants.stageOneEncoderTicksToRadians;
    double output = radians / encoderRatio;

    return output;
  }
  public double calculateEncoderFromOutput(double output) {
    double ticks = output / ArmConstants.stageOneEncoderTicksToRadians;
    double encoder = ticks * encoderRatio;

    return encoder;
  }
  private void setArmPosition(){
    double output = setpoint;
    SmartDashboard.putNumber("stageOneSet", Units.radiansToDegrees(output));
    output = output - ArmConstants.stageOneEncoderOffset;
    double encoder = calculateEncoderFromOutput(output);

    armMotorPrimary.set(TalonSRXControlMode.MotionMagic, encoder, DemandType.ArbitraryFeedForward, (AFF/12) * (1/SmartDashboard.getNumber("stageOneEfficiencyMultiplier", 1)));
  }
  private double getEncoderPosition() {
    double encoder = armMotorPrimary.getSelectedSensorPosition();
    double output = calculateOutputFromEncoder(encoder);
    
    if (output < Math.PI - ArmConstants.stageOneEncoderOffset) {
      output = output + ArmConstants.stageOneEncoderOffset;
    }
    else {
      output = -Math.PI + ((output + ArmConstants.stageOneEncoderOffset) - Math.PI);
    }
    
    return output;
  }
  private double getEncoderVelocity() {
    double encoder = armMotorPrimary.getSelectedSensorVelocity();
    double output = calculateOutputFromEncoder(encoder) / 10;

    return output;
  }
  double timeSinceLastSimUpdate = Timer.getFPGATimestamp();
  @Override
  public void periodic() {
    //SmartDashboard.putNumber("stageOne_setpoint",setpoint);
    if(Constants.SimulationConstants.simulationEnabled){
      simulatedAngleRad += ((Timer.getFPGATimestamp() - timeSinceLastSimUpdate) * (setpoint - simulatedAngleRad) * Constants.SimulationConstants.armStageOneSpeedMultiplier);
      timeSinceLastSimUpdate = Timer.getFPGATimestamp();
    }

    calculateStageData();
    setArmPosition();
    SmartDashboard.putNumber("stageOneAngle", Units.radiansToDegrees(angle));

    /*
    armMotorPrimary.configMotionCruiseVelocity(calculateEncoderFromOutput(Units.degreesToRadians(SmartDashboard.getNumber("stageOneVelocity", Units.radiansToDegrees(ArmConstants.stageOneMotionCruiseVelocity)))) * 10);
    armMotorPrimary.configMotionAcceleration(calculateEncoderFromOutput(Units.degreesToRadians(SmartDashboard.getNumber("stageOneAccel", Units.radiansToDegrees(ArmConstants.stageOneMotionMaxAcceleration)))) * 10);
    armMotorPrimary.config_kP(0, SmartDashboard.getNumber("stageOneP", ArmConstants.stageOne_kP));
    armMotorPrimary.config_kI(0, SmartDashboard.getNumber("stageOneI", ArmConstants.stageOne_kI));
    armMotorPrimary.config_kD(0, SmartDashboard.getNumber("stageOneD", ArmConstants.stageOne_kD));
    */
    //SmartDashboard.putNumber("stageOneVelocity", velocity);
  }
}