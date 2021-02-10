// UDP send program

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UdpSend
{
    static {System.out.println("Starting class: " + MethodHandles.lookup().lookupClass().getCanonicalName());}

    private static final String pId = new String("[UdpSend]");

    private InetAddress address;
    private boolean isConnected;

    private byte[] UDPbuffer = new byte[Main.MAXIMUM_MESSAGE_LENGTH];
    private final int bufLength = UDPbuffer.length; // save original length because length property is changed with usage

    private DatagramPacket packet;
    private DatagramSocket datagramSocket;
    private int port;
    private String URL;

    public UdpSend(int port, String URL)
    {
        isConnected = false;
        this.port = port;
        this.URL = URL;
        Connect();
    }

    public synchronized void Communicate(String message)
    {
        if(isConnected)
        {
        UDPbuffer = message.getBytes();

        packet.setData(UDPbuffer, 0, UDPbuffer.length);

        try
        {
            datagramSocket.send(packet); // send target information to robot
        } 
        catch (IOException e)
        {
            e.printStackTrace();
        }
        }
    }

    public synchronized boolean isConnected() // getter for isConnected
    {
        return isConnected;
    }

    public synchronized void Connect ()
    {
        if(isConnected) return; // cannot connect if already connected

        try
        {
            System.out.println(pId + " Sending UDP messages to " + URL + ":" + port);
            address = InetAddress.getByName(URL);
        } 
        catch (UnknownHostException e)
        {
            System.out.println(pId + " Requested message receiver not responding.");

            e.printStackTrace();
            return;
        }

        try
        {
            datagramSocket = new DatagramSocket();
            datagramSocket.setBroadcast(true);
            // Setting the port address to reuse can sometimes help and I can't think of how
            // it would hurt us if we only have one process running on a port.
            // Especially if the socket isn't closed, it can take some time to free the port
            // for reuse so if the program is restarted quickly and the port isn't noticed
            // to be free by the operating system,
            // there can be a socket exception if Reuse isn't set.
            // Example: socket = new DatagramSocket(port);
            // Example: socket.setReuseAddress(true);

            // datagramSocket.setSoTimeout(2000); // robot response receive timeout in
            // milliseconds check in case robot
            // isn't responding. Not used if no attempt to receive a response
        } 
        catch (SocketException e)
        {
            e.printStackTrace();
            return;
        }

        packet = new DatagramPacket(UDPbuffer, bufLength, address, port);

        isConnected = true; // made it all the way here so assume all is OK
    }

}
