/*
  UDP receive program

 Usage:
   Add this file or multiple files - or equivalents that actually route the messages to the right places - to the roboRIO
   Start the thread or multiple threads if each thread processes its own messages
   
   In a RPi test environment with no roboRIO use the below 5 statements in Main.java
   On the roboRIO use something similar in the right place (likely robot.java) and don't call it test!
   
    private static UdpReceive testUDPreceive;
    private static Thread UDPreceiveThread;

    // start test UDP receiver
    UDPreceive = new UdpReceive(5800); // port must match what the RPi is sending on
    UDPreceiveThread = new Thread(UDPreceive, "4237UDPreceive");
    UDPreceiveThread.start();

    This class contains an example of using the TargetData as it might be on the roboRIO TimedRobot
*/

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.util.Timer;
import java.util.TimerTask;

public class UdpReceive implements Runnable {
    static {
        System.out.println("Starting class: " + MethodHandles.lookup().lookupClass().getCanonicalName());
    }

    private static final String pId = new String("[UdpReceive]");

    private static String lastDataReceived = "";
    private DatagramSocket socket = null;


    // example process to use the Target Data
    //
    public UseTargetData useTargetData = new UseTargetData();

    class UseTargetData extends TimerTask // something like a TimedRobot class
    {

        public TargetDataB newTargetDataTurret = new TargetDataB();
        private TargetDataB TargetDataTurret = new TargetDataB();

        public TargetDataE newTargetDataIntake = new TargetDataE();
        private TargetDataE TargetDataIntake = new TargetDataE();

        public void run()
        {
            System.out.print(pId + System.currentTimeMillis());
            if (newTargetDataTurret.isFreshData) // see if there is new data
            {
                TargetDataTurret = newTargetDataTurret.get(); // new data so copy it to private storage for the loop to use
                System.out.println(" Turret " + TargetDataTurret); // new data to be used appropriately; call various getters as needed
            }
            else
                System.out.println(" Stale Turret"); // no new data so do something appropriate with the old data
        }
    }
    //
    // end example process to use the Target Data

    public UdpReceive(int port) {
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(500); // set receive timeout in milliseconds in case RPi is dead
        } catch (SocketException e) {
            // do something when something bad happens
            e.printStackTrace();
        }

        // Setting the port address to reuse can sometimes help and I can't think of how
        // it would hurt us if we only have one process running on a port.
        // Especially if the socket isn't closed, it can take some time to free the port
        // for reuse so if the program is restarted quickly and the port isn't noticed
        // to be free by the operating system,
        // there can be a socket exception if Reuse isn't set.
        // Example: socket = new DatagramSocket(port);
        // Example: socket.setReuseAddress(true);

        // define a Timed Process to simulate the 20 millisecond loop of the roboRIO TimedRobot
        Timer timer = new Timer();
        timer.schedule(
                useTargetData, // example process to use the Target Data
                 0, // initial delay
                20); // subsequent rate
    }

    public void run() {
        System.out.println(pId + " packet listener thread started");
        byte[] bufferMessage = new byte[Main.MAXIMUM_MESSAGE_LENGTH];
        final int bufferMessageLength = bufferMessage.length; // save original length because length property is changed with usage
        DatagramPacket packet = new DatagramPacket(bufferMessage, bufferMessageLength);

        while (true) {
            try {
                // receive request
                packet.setLength(bufferMessageLength);
                socket.receive(packet); // always receive the packets
                byte[] data = packet.getData();
                lastDataReceived = new String(data, 0, packet.getLength());
                //System.out.println(pId + System.currentTimeMillis() + " >" + lastDataReceived + "<");

                if (lastDataReceived.startsWith("Turret ")) {
                    String message = new String(lastDataReceived.substring("Turret ".length()));
                    useTargetData.newTargetDataTurret.fromJson(message);
                    //System.out.println(pId + " Turret " + receivedTargetB);   
                }
                else if (lastDataReceived.startsWith("Intake "))
                {
                    String message = new String(lastDataReceived.substring("Intake ".length()));
                    useTargetData.newTargetDataIntake.fromJson(message);
                    //System.out.println(pId + " Intake " + receivedTargetE);   
                }
                else
                {
                    System.out.println(pId + " Unknown class received UDP " + lastDataReceived);
                }
            } 
            catch (SocketTimeoutException e)
            {
                // do something when no messages for awhile
                System.out.println(pId + " hasn't heard from any vision pipeline for awhile");
            } 
            catch (IOException e)
            {
                e.printStackTrace();
                // could terminate loop but there is no easy restarting
            }
        }
        // socket.close();
    }
}
