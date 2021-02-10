import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.lang.invoke.MethodHandles;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.CvType;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;

public class ImageOperator implements Runnable {
    static {System.out.println("Starting class: " + MethodHandles.lookup().lookupClass().getCanonicalName());}
    
    private static final String pId = new String("[ImageOperator]");
    private static final double VERTICAL_CAMERA_ANGLE_OF_VIEW = 35.0;

    // This object is used to send the image to the Dashboard
    private CvSource outputStream;

    // This field is used to determine if debugging information should be displayed.
    // Use the setDebuggingEnabled() method to set this value.
    private boolean debuggingEnabled = false;

    /**
     * This method sets the field to display debugging information.
     * 
     * @param enabled Set to true to display debugging information.
     */
    public void setDebuggingEnabled(boolean enabled) {
        debuggingEnabled = enabled;
    }

    @Override
    public void run() {
        System.out.println(pId + " Starting run");

        this.setDebuggingEnabled(Main.debug);

        int height = 24;
        int width = 320;
        int fps = 10; // it paces itself to match contours for the process but server goes at this speed
        int jpegQuality = 10; // lower is more compression - less quality, 100 is no compression, -1 is default compression

        Mat mat; // Mat to draw on
        Mat targetIconTemp = new Mat(); // Mat for target to display
        
        // The following single statement does correctly define a camera server for the OpenCV image to be displayed in ShuffleBoard
        // The limitation is there is no visibility to the MjpegSever parameters such as Quailty (compression)
        // outputStream = CameraServer.getInstance().putVideo("OperatorImage", 640, 45);

        // Or start the video stream this way but then it isn't shown by its "nice" name and ShuffleBoard does display it
        // We have control over the port used, Quality (compression) parameters and more
        // CvSource outputStream = new CvSource("OperatorImage", VideoMode.PixelFormat.kMJPEG, 640, 45, 30);
        // MjpegServer mjpegServer = new MjpegServer("serve_OperatorImage", 1186);
        // mjpegServer.setCompression(50);
        // mjpegServer.setSource(outputStream);

        // The following will display the image on ShuffleBoard and reveal the MjpegServer parameters
        CvSource outputStream = new CvSource("OperatorImage", VideoMode.PixelFormat.kMJPEG, width, height, fps);
        MjpegServer mjpegServer = CameraServer.getInstance().startAutomaticCapture(outputStream);
        mjpegServer.setResolution(width, height);
        mjpegServer.setFPS(fps);
        mjpegServer.setCompression(jpegQuality);

        //////////////////
        // put to the Shuffleboard now
        // Widget in Shuffleboard Tab
        Map<String, Object> cameraWidgetProperties = new HashMap<String, Object>();
        cameraWidgetProperties.put("Show crosshair", false);
        cameraWidgetProperties.put("Show controls", false);

        synchronized (Main.tabLock)
        {
            Main.cameraTab.add("High Power Port Alignment", outputStream).withWidget(BuiltInWidgets.kCameraStream)
                    .withProperties(cameraWidgetProperties)
                    .withSize(12, 1)
                    .withPosition(20, 0)
            ;

            Shuffleboard.update();
        }
        //
        //////////////////

        while(true){
            try{
                // DRAW HERE
                double portDistance;
                double angleToTurn;
                int contourIndex;
                boolean isTargetFound;
                double shapeQuality;

                // could consider black & white mat to save network bandwidth but it's pretty small even with color
                mat = Mat.zeros(height, width, CvType.CV_8UC3); // blank color Mat to draw on

                synchronized (Main.tapeLock)
                {
                    // get the data to draw the "cartoon" image
                    if (!Main.isDistanceAngleFresh)
                    {
                       Main.tapeLock.wait();
                    }

                    portDistance = Main.tapeDistance;
                    angleToTurn = Main.tapeAngle;
                    contourIndex = Main.tapeContours; // 0 is the index of the first contour; should be the only one; not checking for more than 1 contour if the best is the first checking
                    isTargetFound = Main.isTargetFound;
                    shapeQuality = Main.shapeQuality;
                    Main.targetIcon.copyTo(targetIconTemp);
                    Main.isDistanceAngleFresh = false; // these data captured to be processed so mark them as used
                }

                Core.transpose(targetIconTemp, targetIconTemp); // camera is rotated so make image look right for humans
                Core.flip(targetIconTemp, targetIconTemp, 0);

                // if there is a target, then color it white if good shape and red for poor shape
                // if no target, leave the image alone and maybe the operator can recognize it (but I doubt it)
                if( isTargetFound ) {
                    for (int idxr = 0; idxr <24; idxr++)
                    for (int idxc = 0; idxc <24; idxc++) {
                        double[] pixel = {0,0,0};
                        pixel = targetIconTemp.get(idxr, idxc);
                        if ( pixel[0] > 40 || pixel[1] > 40 || pixel[2] > 40) {
                            if(shapeQuality > Main.shapeQualityBad) targetIconTemp.put(idxr, idxc, new byte[]{90, 120, -1});
                            else targetIconTemp.put(idxr, idxc, new byte[]{-1,-1,-1}); // byte -1 is 0xff or 255
                    }
                    }
                }
                // draw the target icon in the center of the camera frame
                targetIconTemp.copyTo(mat.submat(0, 24, (mat.width()/2)-12, (mat.width()/2)+12));
                
                if(!isTargetFound)
                {
                    Imgproc.putText(mat, "NO TARGET", new Point(34, 14),
                        Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 255), 2);
                }
                else
                { // target information
                    // Draw distance text
                    String printDistance = String.format("%d", (int)(portDistance+.5));

                    Imgproc.putText(mat, printDistance, new Point(5, 13),
                        Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 255), 2);

                    Imgproc.putText(mat, "inches", new Point(5, 22),
                        Core.FONT_HERSHEY_SIMPLEX, .3, new Scalar(255, 255, 255), 1);

                    Imgproc.putText(mat, printDistance, new Point(mat.width() - 37, 13),
                        Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 255), 2);

                    Imgproc.putText(mat, "inches", new Point(mat.width() - 37, 22),
                        Core.FONT_HERSHEY_SIMPLEX, .3, new Scalar(255, 255, 255), 1);
                    
                    // where center of camera frame is pointing (relative to the target)
                    int offset = (int)
                        ( ((double)mat.width()/VERTICAL_CAMERA_ANGLE_OF_VIEW)*angleToTurn + (double)mat.width()/2.0 );

                    // Draw the white cross representing the center of the camera frame.
                    Imgproc.drawMarker(mat, new Point(offset, (int)(mat.height() / 2.0)), 
                        new Scalar(255, 255, 255), Imgproc.MARKER_CROSS, 40);
                    // Draw a green circle around the cross
                    Imgproc.circle(mat, new Point(offset, (int)(mat.height() / 2.0)), (int)(mat.height() / 2.0), 
                        new Scalar(0, 255, 0), 2);

                    // // make a hexagon pretend target
                    // ArrayList<MatOfPoint> listOfHexagonPoints = new ArrayList<MatOfPoint>();
                    // listOfHexagonPoints.add(new MatOfPoint
                    //     (
                    //     new Point(              offset, mat.height()), // bottom
                    //     new Point( height*0.5 + offset, mat.height()*0.667), // lower right
                    //     new Point( height*0.5 + offset, mat.height()*0.333), // upper right
                    //     new Point(              offset, 0.),                 // top
                    //     new Point( -height*0.5+ offset, mat.height()*0.333), // upper left
                    //     new Point( -height*0.5+ offset, mat.height()*0.667)  // lower left
                    //     )
                    // ); 

                    // if( (contourIndex > 0) || (Main.shapeQuality  > Main.shapeQualityBad))  // 0 is the index of the first contour; should be the only one
                    // {
                    //     Imgproc.fillConvexPoly(mat, listOfHexagonPoints.get(0), new Scalar(0, 0, 255), 1, 0); // filled red for potentially bad
                    // }
                    // else
                    // {
                    //     Imgproc.polylines(mat, listOfHexagonPoints, true, new Scalar(0xad, 0xa9, 0xaa), 2, 1); // silvery outline for potentially good
                    // }
                }

                outputStream.putFrame(mat);

             }  catch (Exception exception)
                {
                    System.out.println(pId + " error " + exception);
                    exception.printStackTrace();
                } 
            }
        }
    }
// parking lot for junk
// rotate the hexagon and NOT this way
//Mat subMat = mat.submat(mat.height() / 2 - 21, mat.height() / 2 + 21, -21 + offset, 21 + offset);
//Creating the transformation matrix M
//Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(offset, mat.height() /2), 30, 1);
//Rotating the given image
//Imgproc.warpAffine(subMat, subMat,rotationMatrix, new Size(42, 42));

// if(angleToTurn > -15 && angleToTurn < 0)
// {
//     Imgproc.putText(mat, String.format("Turn left %d deg.", angleToTurn), new Point(15, 35),
//         Core.FONT_HERSHEY_SIMPLEX, .4, new Scalar(255, 255, 255), 1);
// }
// else if(angleToTurn == 0)
// {
//     Imgproc.putText(mat, String.format("Shoot!"), new Point(15, 35),
//         Core.FONT_HERSHEY_SIMPLEX, .4, new Scalar(255, 255, 255), 1);
// }
// else if(angleToTurn > 0 && angleToTurn < 15)
// {
//     Imgproc.putText(mat, String.format("Turn right %d deg.", angleToTurn), new Point(15, 35),
//         Core.FONT_HERSHEY_SIMPLEX, .4, new Scalar(255, 255, 255), 1);
// }
