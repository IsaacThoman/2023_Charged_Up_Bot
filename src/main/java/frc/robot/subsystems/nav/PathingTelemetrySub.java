package frc.robot.subsystems.nav;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants;
import frc.robot.lib.FieldObjects.FieldLine;
import frc.robot.lib.FieldObjects.FieldTag;
import frc.robot.lib.FieldObjects.Node;
import frc.robot.subsystems.state.StateControllerSubsystem;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class PathingTelemetrySub extends GraphicalTelemetrySubsystem {
    private StateControllerSubsystem stateControllerSubsystem;
    public PathingTelemetrySub(StateControllerSubsystem stateControllerSubsystem) {
        super("FieldNav",768,480,6);
        this.stateControllerSubsystem = stateControllerSubsystem;
    }


    protected void drawThings(Mat mat){
        clearScreen(mat);
        dontTouchMe = true;
       /* List<MatOfPoint> pointList2 = new ArrayList<MatOfPoint>();
        pointList2.add(new MatOfPoint(
                new Point(50,50),
                new Point(25,100),
                new Point(75,100)
        ));
        Imgproc.polylines (mat, pointList2, true, new Scalar(255, 255, 255), 1);*/


        //draws a small point at each meter
        for(int markX = -8; markX<=8; markX++)
            for(int markY = -4; markY<=4; markY++){
                double[] pt = metersPosToPixelsPos(new double[] {markX,markY});
                Imgproc.rectangle(mat,new Point(pt[0],pt[1]),new Point(pt[0]+0,pt[1]+0), new Scalar(0,255,0),2);
            }

        //draws all barriers
        for(FieldLine line: fieldLines){
            double[] pt1Pix = metersPosToPixelsPos(new double[] {line.getLine().getX1(), line.getLine().getY1()});
            double[] pt2Pix = metersPosToPixelsPos(new double[] {line.getLine().getX2(), line.getLine().getY2()});
            Imgproc.line(mat,new Point(pt1Pix[0],pt1Pix[1]),new Point(pt2Pix[0],pt2Pix[1]), line.getColor(),4);
        }

        for(Node node : nodes){
            Scalar color = new Scalar(255,255,255);
            int radius = 5;
            switch(node.getNodeType()){
                case CONE: color = new Scalar(0,240,255); break; //yellow?
                case CUBE: color = new Scalar(298,20,188); break; //purple?
                case HYBRID: color = new Scalar(46,162,0); break; //green?
            }
            switch(node.getLevel()){
                case POS1: radius = 3; break;
                case POS2: radius = 4; break;
                case POS3: radius = 5; break;
            }
            if(node.getLevel() == stateControllerSubsystem.getPlacingLevel())
                //if(Timer.getFPGATimestamp()%1 > 0.5)
                    color = new Scalar(color.val[0]-175,color.val[1]-175,color.val[2]+175);
            Imgproc.circle(mat, metersPosToPixelsPos(new Point(node.getX(),node.getY())),radius,color,2);
        }



        /**Draw robot */
        drawRotatedRect(mat, robotPose.getX(), robotPose.getY(), Constants.PathingConstants.ROBOT_WIDTH,Constants.PathingConstants.ROBOT_LENGTH,robotPose.getRotation(), new Scalar(0,0,255), 2);

        double botFromNavEndDist = Math.sqrt(Math.pow(robotPose.getX() - destinationPose.getX(),2)+Math.pow(robotPose.getY() - destinationPose.getY(),2));
        Scalar navEndPoseColor = new Scalar(0,0,130);
        if(botFromNavEndDist<=Constants.DriveConstants.posTolerance)
            navEndPoseColor = new Scalar(0,180,0);
        /** Draw nav end pose*/
        drawRotatedRect(mat, destinationPose.getX(), destinationPose.getY(), Constants.PathingConstants.ROBOT_WIDTH,Constants.PathingConstants.ROBOT_LENGTH,destinationPose.getRotation(), navEndPoseColor, 2);


        /** Draw setpoints */
        for(Pose2d setPoint : setPoints){
            Scalar color = new Scalar(12,79,78);
            if(setPoint.getX() == destinationPose.getX() && setPoint.getY() == destinationPose.getY())
                color = new Scalar(31,216,214);
            Imgproc.circle(mat, metersPosToPixelsPos(new Point(setPoint.getX(),setPoint.getY())),1,color,2);
        }


        /** Draw camera dot*/
        double robotAngle = robotPose.getRotation().getRadians();
        double camX = robotPose.getX() + Math.cos(robotAngle+ Constants.VisionConstants.camDirFromCenter) * Constants.VisionConstants.camDistFromCenter;
        double camY = robotPose.getY() + Math.sin(robotAngle + Constants.VisionConstants.camDirFromCenter)* Constants.VisionConstants.camDistFromCenter;
        Imgproc.circle(mat,metersPosToPixelsPos(new Point(camX,camY)),2,new Scalar(255,255,255),2);

        /** Draw corner points */
        for(Pose2d pose : cornerPoints)
            Imgproc.circle(mat, metersPosToPixelsPos(new Point(pose.getX(), pose.getY())),2,new Scalar(100,100,100),2);


//        //draw desired point
//        double[] navDesiredPointInPixels = metersPosToPixelsPos(NavigationField.getDesiredXY());
//        Point navPointAsPoint = new Point(navDesiredPointInPixels[0],navDesiredPointInPixels[1]);
//        Imgproc.line(mat, navPointAsPoint,navPointAsPoint,new Scalar(236,144,0),10);


        //draw navigation lines
        for(int i = 1; i<navPoses.size(); i++)
            Imgproc.line(mat, metersPosToPixelsPos(new Point(navPoses.get(i-1).getX(), navPoses.get(i-1).getY())),metersPosToPixelsPos(new Point(navPoses.get(i).getX(), navPoses.get(i).getY())),new Scalar(255,0,255),2);
       // SmartDashboard.putNumber("navPosesInPathingTelemetry",navPoses.size());
        String out = "";
        for(Pose2d pose : navPoses){
            out+= "(" + pose.getX() + ", " + pose.getY() + "), ";
        }
       // SmartDashboard.putString("navPoses", out);
        if(navPoses.size()>0){
            Pose2d lastPose = navPoses.get(navPoses.size()-1);
            Point lastPosePix = metersPosToPixelsPos(new Point(lastPose.getX(),lastPose.getY()));
            Imgproc.circle(mat,lastPosePix,1,new Scalar(0,255,255),4);

        }
        //draw AprilTags
        if(fieldTags!=null)
            for(FieldTag tag : fieldTags){
                double x = tag.getPose().getX();
                double y = tag.getPose().getY();
                double ang = tag.getPose().getRotation().getRadians();

                double x1 = x + Math.cos(ang+Math.PI/2)*0.1;
                double x2 = x - Math.cos(ang+Math.PI/2)*0.1;
                double y1 = y + Math.sin(ang+Math.PI/2)*0.1;
                double y2 = y - Math.sin(ang+Math.PI/2)*0.1;

                Imgproc.line(mat, metersPosToPixelsPos(new Point(x1,y1)), metersPosToPixelsPos(new Point(x2,y2)), new Scalar(212,182,0),2);
              //  Imgproc.circle(mat, metersPosToPixelsPos(new Point(x,y)),3,new Scalar(255,255,255),2);
            }

        //draw test line
      //  Imgproc.line(mat,metersPosToPixelsPos(new Point(SmartDashboard.getNumber("x1",0),SmartDashboard.getNumber("y1",0))), metersPosToPixelsPos(new Point(SmartDashboard.getNumber("x2",0),SmartDashboard.getNumber("y2",0))),new Scalar(255,255,255),2);


        //coords and heading
        Imgproc.putText(mat,"("+(Math.floor(robotPose.getX()*1000)/1000.0)+", "+(Math.floor(robotPose.getY()*1000)/1000.0)+")",new Point(128,20),5,1,new Scalar(255,255,255));
        Imgproc.putText(mat,(Math.floor(robotPose.getRotation().getDegrees()*100)/100.0) + "*",new Point(128,40),5,1,new Scalar(255,255,255));

        //   SmartDashboard.putNumber("point?",barriers.size());
        drawStateTelem(mat);

        //draw preplaced items
        for(int i = 0; i<Constants.FieldConstants.preplacedItemXDiffsFromCenterInches.length; i++){
            for(int j = 0; j<Constants.FieldConstants.preplacedItemYDiffsFromCenterInches.length; j++){
                double x = Units.inchesToMeters(Constants.FieldConstants.preplacedItemXDiffsFromCenterInches[i]);
                double y = -Units.inchesToMeters(Constants.FieldConstants.preplacedItemYDiffsFromCenterInches[j]);
                Imgproc.circle(mat, metersPosToPixelsPos(new Point(x,y)),3,new Scalar(255,255,255),2);
            }
        }

        //draw arrow to show heading on robot
        Point robotPosePixels = metersPosToPixelsPos(new Point(robotPose.getX(),robotPose.getY()));
        drawArrow(mat, robotPosePixels.x,robotPosePixels.y, robotPose.getRotation().getRadians(), 15, new Scalar(0,0,255));

        dontTouchMe = false;
    }

    void drawStateTelem(Mat mat){
        Scalar coneOrCubeColor = new Scalar(200,200,200); //gray
        switch(stateControllerSubsystem.getItemType()){
            case CONE: coneOrCubeColor = new Scalar(0,222,255); break;
            case CUBE: coneOrCubeColor = new Scalar(181,0,175); break;
        }
        List<MatOfPoint> box1 = new ArrayList<MatOfPoint>();
        box1.add(new MatOfPoint(
                new Point(8,14),
                new Point(8,32),
                new Point(26,32),
                new Point(26,14)
        ));
        Imgproc.fillPoly(mat,box1,coneOrCubeColor);
        String fallenString = ""; if(stateControllerSubsystem.getItemFallen() == StateControllerSubsystem.ItemFallen.FALLEN_CONE) fallenString = "_FALLEN";
        Imgproc.putText(mat,stateControllerSubsystem.getItemType().toString()+fallenString,new Point(32,26),5,0.5,new Scalar(255,255,255));

        Scalar agnosticGrabberColor = new Scalar(200,200,200);
        switch(stateControllerSubsystem.getAgnosticGrabberMode()){
            case INTAKING: agnosticGrabberColor = new Scalar(0,255,0); break;
            case PLACING: agnosticGrabberColor = new Scalar(0,0,255); break;
        }
        List<MatOfPoint> box2 = new ArrayList<MatOfPoint>();
        box2.add(new MatOfPoint(
                new Point(8,14+26),
                new Point(8,32+26),
                new Point(26,32+26),
                new Point(26,14+26)
        ));
        Imgproc.fillPoly(mat,box2,agnosticGrabberColor);
        Imgproc.putText(mat,stateControllerSubsystem.getAgnosticGrabberMode().toString(),new Point(32,26+26),5,0.5,new Scalar(255,255,255));


        /* //bro, stop.
        drawArm(mat,64,200,20,Math.PI/5,12,-Math.PI/2.5,30,10);

        //show intaking, outtaking, or holding
        double arrowAngleDeg = 90;
        Scalar arrowColor = new Scalar(255,255,255);
        switch(stateControllerSubsystem.getAgnosticGrabberMode()){
            case PLACING: arrowAngleDeg = 0; arrowColor = new Scalar(0,0,255); break;
            case INTAKING: arrowAngleDeg = 180; arrowColor = new Scalar(0,255,0); break;
        }

        Imgproc.rectangle(mat, new Point(10,50),new Point(50,100),new Scalar(255,255,255),2);
        drawArrow(mat,75,75,Math.toRadians(arrowAngleDeg),20,arrowColor);
        */

        Scalar warningColor = new Scalar(255,255,255);
        if(Timer.getFPGATimestamp()%1 > 0.5)
            warningColor = new Scalar(0,0,255);
        if(Constants.SimulationConstants.simulationEnabled)
            Imgproc.putText(mat,"SIMULATED ODOMETRY IN USE",new Point(128,480-6),5,0.5,warningColor);


    }

    public void drawArm(Mat mat,double x, double y, double len1, double ang1, double len2, double ang2,double baseWidth, double startingHeight){
        Scalar color = new Scalar(255,255,255);
        double armBaseX = x;
        double armBaseY = y-startingHeight;
        double arm1EndX = armBaseX + len1*Math.cos(ang1);
        double arm1EndY = armBaseY - len1*Math.sin(ang1);
        double arm2EndX = arm1EndX + len2*Math.cos(ang2);
        double arm2EndY = arm1EndY - len2*Math.sin(ang2);

        Imgproc.line(mat, new Point(x-baseWidth/2, y),new Point(x+baseWidth/2, y),color,2); //base
        Imgproc.line(mat, new Point(x, y),new Point(armBaseX,armBaseY),color,2); //base
        Imgproc.line(mat, new Point(armBaseX, armBaseY),new Point(arm1EndX,arm1EndY),color,2); //arm1
        Imgproc.line(mat, new Point(arm1EndX, arm1EndY),new Point(arm2EndX,arm2EndY),color,2); //arm2


    }
    public void drawArrow(Mat mat, double x, double y, double angle, double length, Scalar color){
        double startX = x- length/2*Math.cos(angle);
        double startY = y+ length/2*Math.sin(angle);
        double endX = x+ length/2*Math.cos(angle);
        double endY = y- length/2*Math.sin(angle);
        double branch1X = endX + length/2.5*Math.cos(angle+Math.toRadians(135));
        double branch1Y = endY - length/2.5*Math.sin(angle+Math.toRadians(135));
        double branch2X = endX + length/2.5*Math.cos(angle-Math.toRadians(135));
        double branch2Y = endY - length/2.5*Math.sin(angle-Math.toRadians(135));

        Imgproc.line(mat, new Point(startX,startY),new Point(endX,endY),color,2);
        Imgproc.line(mat, new Point(endX,endY),new Point(branch1X,branch1Y),color,2);
        Imgproc.line(mat, new Point(endX,endY),new Point(branch2X,branch2Y),color,2);
    }

    public void drawRotatedRect(Mat mat, double centerX, double centerY, double l, double w, Rotation2d angle, Scalar color, int thickness){
        double ang1 = Math.atan2(l, w);
        double ang2 = Math.atan2(l, -w);
        double cornerDist = Math.sqrt(Math.pow(l/2,2) + Math.pow(w/2,2));
        double robotAngle = angle.getRadians();
        Point[] robotPts = new Point[]{
                new Point(cornerDist*Math.cos(robotAngle+ang1) + centerX, cornerDist*Math.sin(robotAngle+ang1) + centerY),
                new Point(cornerDist*Math.cos(robotAngle-ang1) + centerX, cornerDist*Math.sin(robotAngle-ang1) + centerY),
                new Point(cornerDist*Math.cos(robotAngle-ang2) + centerX, cornerDist*Math.sin(robotAngle-ang2) + centerY),
                new Point(cornerDist*Math.cos(robotAngle+ang2) + centerX, cornerDist*Math.sin(robotAngle+ang2) + centerY)

        };
        //convert corner points to pixels
        for(int i = 0; i<robotPts.length; i++)
            robotPts[i] = metersPosToPixelsPos(robotPts[i]);
        //draw robot to screen
        List<MatOfPoint> pointList2 = new ArrayList<MatOfPoint>();
        pointList2.add(new MatOfPoint(robotPts[0],robotPts[1],robotPts[2],robotPts[3])); //ew gross
        Imgproc.polylines(mat,pointList2 ,true,color,thickness);
    }

    void clearScreen(Mat mat){
        List<MatOfPoint> pointList1 = new ArrayList<MatOfPoint>();
        pointList1.add(new MatOfPoint(
                new Point(0,0),
                new Point(0,480),
                new Point(768,480),
                new Point(768,0)
        ));
        Imgproc.fillPoly(mat,pointList1,new Scalar(0,0,0));


        List<MatOfPoint> pointList2 = new ArrayList<MatOfPoint>();
        pointList2.add(new MatOfPoint(
                new Point(0,0),
                new Point(0,480),
                new Point(128,480),
                new Point(128,0)
        ));
        Imgproc.fillPoly(mat,pointList2,new Scalar(42,42,42));
    }


    private ArrayList<FieldLine> fieldLines = new ArrayList<>();
    private ArrayList<Node> nodes = new ArrayList<Node>();
    public void updateBarriers(ArrayList<FieldLine> fieldLines){
        if(dontTouchMe) return;
        this.fieldLines.clear();
        for(FieldLine line : fieldLines)
            this.fieldLines.add(line);
    }



    Pose2d robotPose = new Pose2d();
    Pose2d destinationPose = new Pose2d();
    ArrayList<Pose2d> setPoints = new ArrayList<Pose2d>();
    boolean robotOrientedView = false;

    public void updateRobotPose(Pose2d newPose){
        if(dontTouchMe) return;
        robotPose = newPose;
    }
    public void updateDestinationPose(Pose2d newPose){
        if(dontTouchMe) return;
        destinationPose = newPose;
    }

ArrayList<Pose2d> navPoses = new ArrayList<Pose2d>();
public void updateNavPoses(ArrayList<Pose2d> navPoses){
    if(dontTouchMe) return;
    this.navPoses.clear();
    for(Pose2d pose : navPoses)
        this.navPoses.add(pose);
}

    private ArrayList<FieldTag> fieldTags;
public void updateFieldTags(ArrayList<FieldTag> fieldTags){
    if(dontTouchMe) return;
    this.fieldTags = fieldTags;
}

public void updateSetPoints(ArrayList<Pose2d> setPoints){
    if(dontTouchMe) return;
    this.setPoints = setPoints;
}

public void updateNodes(ArrayList<Node> nodes){this.nodes = nodes;}

    public  Point metersPosToPixelsPos(Point posInMeters){
        double centerX = 640/2;
        double centerY = 480/2;
        double ang = Math.toRadians(180);

        posInMeters.x = -posInMeters.x; //what?
   //     posInMeters.x += SmartDashboard.getNumber("TCamX",0);
   //     posInMeters.y += SmartDashboard.getNumber("TCamY",0);

        if(robotOrientedView) {
            posInMeters.x += robotPose.getX();
            posInMeters.y -= robotPose.getY();
            ang-=Math.PI/2;
        }



     //   double zoom = SmartDashboard.getNumber("TCamZoom",1) * 0.6  ;
        double zoom = 1 * 0.6;

     //   ang+= Math.toRadians(SmartDashboard.getNumber("TCamAngle",0));
        if(robotOrientedView)
            ang+= robotPose.getRotation().getRadians();

        double outX = centerX;
        double outY = centerY;
        outX+= posInMeters.x * 60;
        outY+= posInMeters.y * 60;

        double dist = Math.sqrt(Math.pow(outX - centerX ,2) + Math.pow(outY -centerY,2)) * zoom;
        double dir = Math.atan2(outY -centerY, outX - centerX);
        outX = centerX + dist * Math.cos(dir+ang);
        outY = centerY + dist * Math.sin(dir+ang);

        outX+=128;
        if(outX<128)
            outX = 128;
        return new Point (outX,outY);
    }

    public double[] metersPosToPixelsPos(double[] posInMeters){
        Point point = new Point(posInMeters[0],posInMeters[1]);
        Point newPoint = metersPosToPixelsPos(point);
        return new double[]{newPoint.x,newPoint.y};
    }

    public void init() {
       //     SmartDashboard.putNumber("TCamAngle",0);
        //    SmartDashboard.putNumber("TCamZoom",1.0);
        //    SmartDashboard.putNumber("TCamX",0);
        //    SmartDashboard.putNumber("TCamY",0);

    }

    public void periodic(){
   // Line2D.Double pathLine = new Line2D.Double(robotPose.getX(), robotPose.getY(), 2, 0.3);
  //  SmartDashboard.putBoolean("Clear path to (2, 0.3)", NavigationField.clearPathOnLine(pathLine));
    }
    private ArrayList<Pose2d> cornerPoints = new ArrayList<>();
    public void updateCornerPoints(ArrayList<Pose2d> cornerPoints){
        if(dontTouchMe) return;
        this.cornerPoints.clear();
        for(Pose2d pose : cornerPoints)
            this.cornerPoints.add(pose);
    }
}
