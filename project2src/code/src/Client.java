import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * The Client abstract class holds implementation of connecting to a server, but does not hold specific methods on group server or file server requests.
 * 
 * @see GroupClient, FileClient
 */
public abstract class Client implements ClientInterface
{
	/*
	 * protected keyword is like private but subclasses have access Socket and input/output streams
	 */
	/**
	 * The socket that will connect to the server.
	 */
	protected Socket sock;
	
	/**
	 * The output stream to the server.
	 */
	protected ObjectOutputStream output;
	
	/**
	 * The input stream from the server.
	 */
	protected ObjectInputStream input;
	
	// javadoc already handled by ClientInterface
	public boolean connect(final String server, final int port)
	{
		if (this.sock != null)
		{
			System.out.println("Disconnecting from previous connection...");
			this.disconnect();
		}
//		System.out.println("Attempting to connect...");
		try
		{
			this.sock = new Socket(server, port);
			// this.sock.setSoTimeout(1000);
			this.output = new ObjectOutputStream(this.sock.getOutputStream());
			this.input = new ObjectInputStream(this.sock.getInputStream());
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
			return false;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		
//		System.out.println("Success!  Connected to " + server + " at port " + port);
		return true;
	}// end method connect(String, int)
	
	// javadoc already handled by ClientInterface
	public boolean isConnected()
	{
		if (sock == null || !sock.isConnected())
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	// javadoc already handled by ClientInterface
	public void disconnect()
	{
		if (isConnected())
		{
			try
			{
				Envelope message = new Envelope("DISCONNECT");
				output.writeObject(message);
				this.output.close();
				this.input.close();
				this.sock.close();
			}
			catch (Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
			}
			this.sock = null;
		}
	}//end method disconnect
}// end class Client
