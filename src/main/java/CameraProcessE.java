//TODO: import Main.CameraConfig;

import java.lang.invoke.MethodHandles;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.wpilibj.Timer;

/**
 * This class creates a camera thread to process camera frames. DO NOT MODIFY
 * this class. The user must create a new GripPipeline class using GRIP, modify
 * the TargetData class, and modify the TargetSelection class.
 * 
 * @author FRC Team 4237
 * @version 2019.01.28.14.20
 */
public class CameraProcessE implements Runnable
{
	static {System.out.println("Starting class: " + MethodHandles.lookup().lookupClass().getCanonicalName());}

	private static final String pId = new String("[CameraProcessE]");

	private String cameraName;// = "Intake Camera";
	private int cameraExposure; // = 0;
	private boolean cameraAutoExposure;
	private int cameraWidth;// = 320;
	private int cameraHeight;// = 240;
	private PipelineProcessE pipelineProcessE;
	private Thread pipeline;

	// This object is used to capture frames from the camera.
	// The captured image is stored to a Mat
	private CvSink inputStream;

	// This object is used to store the camera frame returned from the inputStream
	// Mats require a lot of memory. Placing this in a loop will cause an 'out of
	// memory' error.
	protected Image cameraFrame;// = new Image();
	private Mat cameraFrameTemp;// = new Mat(240, 320, CvType.CV_8UC3);

	// This field is used to determine if debugging information should be displayed.
	// Use the setDebuggingEnabled() method to set this value.
	private boolean debuggingEnabled = false;

	// These fields are used to set the camera resolution and camera name.
	// Use the set...() method to set these values.

	private VideoSource camera;
	private Main.CameraConfig config;

	public CameraProcessE(VideoSource camera, Main.CameraConfig cameraConfig)
	{
		this.camera = camera;
		this.cameraName = cameraConfig.name;
		// this.cameraExposure = CameraConfig.exposure;
		// this.cameraAutoExposure = CameraConfig.autoExposure;
		this.cameraWidth = cameraConfig.width;
		this.cameraHeight = cameraConfig.height;

		config = cameraConfig;
		cameraFrame = new Image();
		cameraFrameTemp = new Mat(cameraHeight, cameraWidth, CvType.CV_8UC3);
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
		pipelineProcessE.setDebuggingEnabled(enabled);
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

	public void run()
	{
		// This variable will be used to time each iteration of the thread loop.
		double loopTotalTime = -999.0;

		cameraFrameTemp.setTo(new Scalar(100, 100, 100)); // something to process if camera is little slow to start
		this.cameraFrame.setImage(cameraFrameTemp);

		// Set up the input stream to get frames from the camera.
		// inputStream = CameraServer.getInstance().getVideo();
		inputStream = new CvSink("cvsink");
		inputStream.setSource(camera);

		pipelineProcessE = new PipelineProcessE(this, config);
		pipeline = new Thread(pipelineProcessE, "4237Epipeline");
		try 
		{
			Thread.sleep(2000); // let things get settled before grabbing images and starting the pipeline process thread
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
		pipeline.start();

        this.setDebuggingEnabled(Main.debug);

		// This is the thread loop. It can be stopped by calling the interrupt() method.
		while (!Thread.interrupted())
		{
			if (debuggingEnabled)
			{
				loopTotalTime = Timer.getFPGATimestamp();
			}
			
			// Tell the input stream to grab a frame from the camera and store it to the
			// mat.
			// Check if there was an error with the frame grab.
			if (inputStream.grabFrame(cameraFrameTemp) == 0)
			{
				System.out.println(pId + " grabFrame error " + inputStream.getError());
				cameraFrameTemp.setTo(new Scalar(170, 170, 170)); // set a gray image if an error
			}

			this.cameraFrame.setImage(cameraFrameTemp);

			if (debuggingEnabled)
			{
				loopTotalTime = Timer.getFPGATimestamp() - loopTotalTime;
				System.out.format("%s %6.2f FPS, loop/camera time %5.3f\n", pId, 1.0 / loopTotalTime, loopTotalTime);
			}
		} // End of the thread loop

		System.out.println(pId + " Camera Frame Grab Interrupted and Ended Thread");
	}
}
