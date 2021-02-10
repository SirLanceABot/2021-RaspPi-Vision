import java.lang.invoke.MethodHandles;

import org.opencv.core.Core;
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
public class CameraProcessB implements Runnable
{
	static {System.out.println("Starting class: " + MethodHandles.lookup().lookupClass().getCanonicalName());}

	private static final String pId = new String("[BCameraProcess]");

	private String cameraName;// = "Turret Camera";
	private int cameraExposure; // = 0;
	private boolean cameraAutoExposure;
	private int cameraWidth;// = 160;
	private int cameraHeight;// = 120;
	private PipelineProcessB pipelineProcessB;
	private Thread pipeline;

	// This object is used to capture frames from the camera.
	// The captured image is stored to a Mat
	private CvSink inputStream;

	// This object is used to store the camera frame returned from the inputStream
	// Mats require a lot of memory. Placing this in a loop will cause an 'out of
	// memory' error.
	protected Image cameraFrame;// = new Image();
	private Mat cameraFrameTemp;// = new Mat(120, 160, CvType.CV_8UC3);

	// This field is used to determine if debugging information should be displayed.
	// Use the setDebuggingEnabled() method to set this value.
	private boolean debuggingEnabled = false;

	// These fields are used to set the camera resolution and camera name.
	// Use the set...() method to set these values.

	private VideoSource camera;
	private Main.CameraConfig config;

	public CameraProcessB(VideoSource camera, Main.CameraConfig cameraConfig)
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
		pipelineProcessB.setDebuggingEnabled(enabled);
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
		System.out.println(pId + " setting exposure to " + exposure);
		Main.cameraB.setExposureManual(exposure);
	}

	// TODO: write the method
	public void setAutoExposure(boolean enabled)
	{
	}

	public void run()
	{
		// This variable will be used to time each iteration of the thread loop.
		double loopTotalTime = -999.0;
		
		cameraFrameTemp.setTo(new Scalar(170, 170, 170));
		this.cameraFrame.setImage(cameraFrameTemp);

		// Set up the input stream to get frames from the camera.
		// inputStream = CameraServer.getInstance().getVideo();
		inputStream = new CvSink("cvsink");
		inputStream.setSource(camera);

		pipelineProcessB = new PipelineProcessB(this, config);
		pipeline = new Thread(pipelineProcessB, "4237Bpipeline");

		try 
		{
			Thread.sleep(2000);
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
		pipeline.start();

		this.setDebuggingEnabled(Main.debug);
		
	    // experiment with white balance - suggest using about 3500K then forget it.  Run GRIP after setting.

		int light = 38, dark = 10, currentExposure = dark;

		setExposure(currentExposure); // once in awhile the exposure isn't set on camera start up so make sure here

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
				cameraFrameTemp.setTo(new Scalar(170, 170, 170));
			}

			//if(turn up exposure so driver can see for example to climb)
			// {
			// 	if(first time) currentExposure = light;
			// 	setExposure(currentExposure);
			//  first time = false
			// }

			// if(currentExposure == light) or use the same logic as above to know to set exposure
			// {
			// 	Core.transpose(cameraFrameTemp, cameraFrameTemp);
			// 	Core.flip(cameraFrameTemp, cameraFrameTemp, 0);
			// }

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
