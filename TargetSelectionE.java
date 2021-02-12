import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * This class is used to select the target from the camera frame. The user MUST
 * MODIFY the process() method. The user must create a new gripPowerCellIntakeVisionPipeline class
 * using GRIP, modify the TargetData class, and modify this class.
 * 
 * @author FRC Team 4237
 * @version 2019.01.28.14.20
 */
public class TargetSelectionE
{
	static {System.out.println("Starting class: " + MethodHandles.lookup().lookupClass().getCanonicalName());}

	private static final String pId = new String("[TargetSelectionE]");

	// This object is used to run the gripPowerCellIntakeVisionPipeline
	private GRIPPowerCellIntakeVisionPipeline gripPowerCellIntakeVisionPipeline = new GRIPPowerCellIntakeVisionPipeline();

	// This field is used to determine if debugging information should be displayed.
	private boolean debuggingEnabled = false;

	TargetSelectionE()
	{
	}

	/**
	 * This method sets the field to display debugging information.
	 * 
	 * @param enabled
	 *                    Set to true to display debugging information.
	 */
	public void setDebuggingEnabled(boolean enabled)
	{
		debuggingEnabled = enabled;
	}

	/**
	 * This method is used to select the next target. The user MUST MODIFY this
	 * method.
	 * 
	 * @param mat
	 *                           The camera frame containing the image to process.
	 * @param nextTargetData
	 *                           The target data found in the camera frame.
	 */
	public void process(Mat mat, TargetDataE nextTargetData)
	{
		Mat matForHough = new Mat();
  		double centerTarget = 5; // FIXME
		int distanceTarget = Integer.MIN_VALUE;
		boolean isTargetFoundLocal = true;
		// Let the gripPowerCellIntakeVisionPipeline filter through the camera frame
		gripPowerCellIntakeVisionPipeline.process(mat);

		gripPowerCellIntakeVisionPipeline.hsvThresholdOutput().copyTo(matForHough);
        detectPowerCells(matForHough, mat);
		matForHough.release();

		// The gripPowerCellIntakeVisionPipeline creates an array of contours that must be searched to find
		// the target.
		ArrayList<MatOfPoint> filteredContours;
		filteredContours = new ArrayList<MatOfPoint>(gripPowerCellIntakeVisionPipeline.filterContoursOutput());

	// Check if no contours were found in the camera frame.
		if (filteredContours.isEmpty())
		{
			//if (debuggingEnabled) FIXME
			{
				System.out.println(pId + " No Contours");

				// Display a message if no contours are found.
				Imgproc.putText(mat, "No Contours", new Point(20, 20), Core.FONT_HERSHEY_SIMPLEX, 0.25,
						new Scalar(255, 255, 0), 1);
			}

		}
		else // if contours were found ...
		{
			//if (debuggingEnabled) FIXME
			{
				System.out.println(pId + " " + filteredContours.size() + " contours");

				// Draw all contours at once (negative index).
				// Positive thickness means not filled, negative thickness means filled.
				Imgproc.drawContours(mat, filteredContours, -1, new Scalar(255, 255, 0), 1);
			}
		}

		//Update the target
		nextTargetData.center = centerTarget;
		nextTargetData.distance = distanceTarget;
		nextTargetData.isFreshData = true;
		nextTargetData.isTargetFound = isTargetFoundLocal;
		
		if (debuggingEnabled)
		{
			System.out.println("Distance: " + distanceTarget);
		}

		gripPowerCellIntakeVisionPipeline.releaseAll();
	}
	
	 public void detectPowerCells(Mat input, Mat output) 
    {
        desaturate(input, input);
        Mat circles = new Mat();
        Imgproc.blur(input, input, new Size(7, 7), new Point(2, 2));
		
	// Tuning HoughCircles is tricky - read the help by hovering over the method name to set circle size to search for
	// and how close together they can be.  No hovering in FRC OpenCV
        //Imgproc.HoughCircles(input, circles, Imgproc.CV_HOUGH_GRADIENT, 2, 100, 100, 90, 0, 1000);
        Imgproc.HoughCircles(
			input, //Input image (grayscale).
			circles, //A vector that stores sets of 3 values: xc,yc,r for each detected circle.
			Imgproc.CV_HOUGH_GRADIENT, //Define the detection method. Currently this is the only one available in OpenCV.
			1., //The inverse ratio of resolution.
			(double)input.rows()/8., //Minimum distance between detected centers.
			200., //param_1: Upper threshold for the internal Canny edge detector.
				// param1: sensitivity of strength of edge
					//	too high - no edges detected
					//	too low - too much clutter
			10., //param_2: Threshold for center detection.
					// param2: how many edge points needed to find a circle
					//	too low and everything is a circle.  It's related to circumference. Accumulator Threshold
			2, //Minimum radius to be detected. If unknown, put zero as default.
			30 //Maximum radius to be detected. If unknown, put zero as default.
			);

        //System.out.println(String.valueOf("size: " + circles.cols()) + ", " + String.valueOf(circles.rows()));
        //System.out.println("size: " + circles.cols() + ", " + circles.rows());

        if (circles.cols() > 0) 
        {
			System.out.println("Hough Circles=" + circles.cols());
			// debug output Print the circle contours
			//System.out.println("Contour Index = " + contourIndex);
			//System.out.println(contour.dump()); // OpenCV Mat dump one line string of numbers
			// or more control over formating with your own array to manipualte
			//System.out.print("[Vision] " + aContour.size() + " points in contour\n[Vision]"); // a contour is a bunch of points
			// convert MatofPoint to an array of those Points and iterate (could do list of Points but no need for this)
			//for(Point aPoint : aContour.toArray())System.out.print(" " + aPoint); // print each point
			//System.out.println(circles.dump());
			
            for (int x=0; x < Math.min(circles.cols(), 5); x++ ) // display 5 circles at the most
            {
                double circleVec[] = circles.get(0, x);

                if (circleVec == null) 
                {
                    break;
                }

                Point center = new Point((int) circleVec[0], (int) circleVec[1]);
                int radius = (int) circleVec[2];
                //System.out.println(" x, y, r " + (circleVec[0]) + " " + (circleVec[1]) + " " + (circleVec[2]));

                //Imgproc.circle(output, center, 1, new Scalar(70, 255, 70), 4); // "dot" in the center
                Imgproc.circle(output, center, radius, new Scalar(70, 255, 70), 1); // perimeter
            }
        }

        Imgproc.putText(output, "HoughCircles", new Point(10, 10), Core.FONT_HERSHEY_SIMPLEX, 0.25, new Scalar(70, 255, 70), 1);

        circles.release();
        //input.release();
    }

    /**
	 * Converts a color image into shades of grey.
	 * @param input The image on which to perform the desaturate.
	 * @param output The image in which to store the output.
	 */
	private void desaturate(Mat input, Mat output) {
		switch (input.channels()) {
			case 1:
				// If the input is already one channel, it's already desaturated
				input.copyTo(output);
				break;
			case 3:
				Imgproc.cvtColor(input, output, Imgproc.COLOR_BGR2GRAY);
				break;
			case 4:
				Imgproc.cvtColor(input, output, Imgproc.COLOR_BGRA2GRAY);
				break;
			default:
				throw new IllegalArgumentException("Input to desaturate must have 1, 3, or 4 channels");
		}
	}
}
