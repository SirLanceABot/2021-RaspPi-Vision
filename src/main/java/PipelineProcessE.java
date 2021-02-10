import java.io.File;
import java.lang.invoke.MethodHandles;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj.Timer;

/**
 * This class creates a camera thread to process camera frames. DO NOT MODIFY
 * this class. The user must create a new GripPipeline class using GRIP, modify
 * the TargetData class, and modify the TargetSelection class.
 * 
 * @author FRC Team 4237
 * @version 2019.01.28.14.20
 */
public class PipelineProcessE implements Runnable
{
	static {System.out.println("Starting class: " + MethodHandles.lookup().lookupClass().getCanonicalName());}

	private static final String pId = new String("[EPipelineProcess]");

	// This object is used to call its process() method if a target is found in the
	// new camera frame.
	// The process() method must be created by the user.
	private TargetSelectionE targetSelection = new TargetSelectionE();

	// This object is used to store the current target data.
	private TargetDataE currentTargetData = new TargetDataE();

	// This object is used to store the next target data.
	private TargetDataE nextTargetData = new TargetDataE();

	// This object is used to capture frames from the camera.
	// The captured image is stored to a Mat
	private CvSink inputStream;

	// This object is used to send the image to the Dashboard
	private CvSource outputStream;

	// This object is used to store the camera frame returned from the inputStream
	// Mats require a lot of memory. Placing this in a loop will cause an 'out of
	// memory' error.
	private Mat mat;// = new Mat(240, 320, CvType.CV_8UC3);

	// This field is used to determine if debugging information should be displayed.
	// Use the setDebuggingEnabled() method to set this value.
	private boolean debuggingEnabled = false;

	// These fields are used to set the camera resolution and camera name.
	// Use the set...() method to set these values.
	private int cameraWidth;// = 320;
	private int cameraHeight;// = 240;
	private String cameraName;// = "Intake Camera";

	private VideoSource camera;
	private CameraProcessE cameraProcess;

	protected PipelineProcessE(CameraProcessE cameraProcess, Main.CameraConfig cameraConfig)
	{
		this.cameraProcess = cameraProcess;
		this.cameraName = cameraConfig.name;
		this.cameraWidth = cameraConfig.width;
		this.cameraHeight = cameraConfig.height;
		mat = new Mat(cameraHeight, cameraWidth, CvType.CV_8UC3);
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
		targetSelection.setDebuggingEnabled(enabled);
	}

	/**
	 * This method sets the camera resolution.
	 */
	public void setCameraResolution(int width, int height)
	{
		cameraWidth = width;
		cameraHeight = height;
	}

	/**
	 * This method sets the camera name.
	 */
	public void setCameraName(String name)
	{
		cameraName = name;
	}

	// TODO: write the method
	public void setExposure(int exposure)
	{

	}

	// TODO: write the method
	public void setAutoExposure(boolean enabled)
	{

	}

	/**
	 * This method is the ONLY method that can be used outside of the package to get
	 * the target data.
	 * 
	 * @return The target data set based on the GripPipeline and TargetSelection.
	 */
	public TargetDataE getTargetData()
	{
		TargetDataE targetData = new TargetDataE();

		targetData = get();

		return targetData;
	}

	/**
	 * This method is required in order to run this class as a thread, it is part of
	 * the Runnable interface. The start() method outside this class, calls this
	 * run() method to run as a separate thread.
	 */
	public void run()
	{
		System.out.println(pId + " Starting run");
 
        this.setDebuggingEnabled(Main.debug);

		// This variable will be used to time each iteration of the thread loop.
		double loopTotalTime = -999.0;
		double loopTargetTime = -999.0;
		double loopWaitTime = -999.0;

		// Set up the input stream to get frames from the camera.
		// inputStream = CameraServer.getInstance().getVideo();
		inputStream = new CvSink("cvsink");
		inputStream.setSource(camera);

		if (Main.displayIntakeContours)
		{
			outputStream = CameraServer.getInstance().putVideo("IntakeContours", 320, 240);
		}
		
        // //////////////////
        // // Widget in Shuffleboard Tab
		// Map<String, Object> mapVideo = new HashMap<String, Object>();
		// mapVideo.put("Show crosshair", false);
		// mapVideo.put("Show controls", false);

		// synchronized(Main.tabLock)
		// {
		// Main.cameraTab.add("IntakeContours", outputStream)
		// .withWidget(BuiltInWidgets.kCameraStream)
		// .withProperties(mapVideo)
		// //.withSize(12, 8)
		// //.withPosition(1, 2)
		// ;
		//
		// Shuffleboard.update();
 		// }
        // //////////////////

		// This is the thread loop. It can be stopped by calling the interrupt() method.
		while (!Thread.interrupted())
		{
			if (debuggingEnabled)
			{
				loopTotalTime = Timer.getFPGATimestamp();
			}

			// Reset the next target data and bump up the frame number
			nextTargetData.reset();
			nextTargetData.incrFrameNumber();

			// Tell the input stream to grab a frame from the camera and store it to the
			// mat.
			// Check if there was an error with the frame grab.
			if (debuggingEnabled)
			{
				loopWaitTime = Timer.getFPGATimestamp();
			}

			this.cameraProcess.cameraFrame.getImage(mat);

			if (debuggingEnabled)
			{
				loopWaitTime = Timer.getFPGATimestamp() - loopWaitTime;
			}

			if (mat == null) // threads start at different times so skip problems expected at the beginning
			{
				System.out.println(pId + " Skipping null mat");
				continue;
			}
			
			if (mat.empty()) // threads start at different times so skip problems expected at the beginning
			{
				System.out.println(pId + " Skipping empty mat");
				continue;
			}
			
			// Scaling if needed to reduce ethernet load might go here
			// input mat must not be output mat
			// Imgproc.resize(mat, differentmat, new Size(), 0.8, 0.8, Imgproc.INTER_AREA);

			if (Main.logImage)
			{
				try
				{
					String filename = String.format("/mnt/usb/ER/%06d.jpg", nextTargetData.frameNumber);
					final File file = new File(filename);
					filename = file.toString();
					if (!Imgcodecs.imwrite(filename, mat))
					{
						System.out.println(pId + " Error writing ER");
					}
				} catch (Exception e)
				{
					System.out.println(pId + " Error saving image file" + e.toString());
				}
			}

			// Call the process() method that was created by the user to process the camera
			// frame.
			if (debuggingEnabled)
			{
				loopTargetTime = Timer.getFPGATimestamp();
			}

			targetSelection.process(mat, nextTargetData); // sets currentTargetData from nextTargetData

			if (debuggingEnabled)
			{
				loopTargetTime = Timer.getFPGATimestamp() - loopTargetTime;
			}
			
			// The synchronized set() method is ONLY called twice.
			// (1) Here in the thread loop and (2) after the thread loop is terminated
			// below.
			set(nextTargetData); // sets currentTargetData from nextTargetData

			Main.sendMessage.Communicate("Intake " + currentTargetData.toJson());

			if (Main.logImage)
			{
				try
				{
					String filename = String.format("/mnt/usb/E/%06d.jpg", currentTargetData.frameNumber);
					final File file = new File(filename);
					filename = file.toString();
					if (!Imgcodecs.imwrite(filename, mat))
					{
						System.out.println(pId + " Error writing E");
					}
				} catch (Exception e)
				{
					System.out.println(pId + " Error saving image file" + e.toString());
				}
			}

			if (Main.displayIntakeContours)
			{
				// Display the camera frame in the output stream.
				Imgproc.putText(mat, "Intake Contours", new Point(25, 30), Core.FONT_HERSHEY_SIMPLEX, 0.5,
						new Scalar(100, 100, 255), 1);
				outputStream.putFrame(mat);
			}

			if (debuggingEnabled)
			{
				loopTotalTime = Timer.getFPGATimestamp() - loopTotalTime;
				System.out.format("%s %6.2f FPS, loop time %5.3f, target time %5.3f, image wait time %5.3f\n", pId, 1.0/loopTotalTime,
				 loopTotalTime,	loopTargetTime, loopWaitTime);
			}
		} // End of the thread loop

		// The thread loop was interrupted so reset the target data.
		nextTargetData.reset();

		// The synchronized set() method is ONLY called twice.
		// (1) Here after the thread loop is terminated and (2) in the thread loop
		// above.
		set(nextTargetData);

		// Free the mat memory.
		mat.release();

			System.out.println(pId + " Camera Frame Grab Interrupted and Ended Thread");
	}

	/**
	 * This method stores the target data. This method is synchronized with the
	 * get() method to ensure integrity of the target data.
	 * 
	 * @param targetData
	 *                       A TargetData object containing the new target data to
	 *                       store
	 */
	private synchronized void set(TargetDataE targetData)
	{
		currentTargetData.set(targetData);
	}

	/**
	 * This method retrieves the target data. This method is synchronized with the
	 * set() method to ensure integrity of the target data.
	 * 
	 * @return A TargetData object containing the target data
	 */
	private synchronized TargetDataE get()
	{
		return currentTargetData.get();
	}
}
