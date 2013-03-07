package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;

import message.Envelope;


public class CAClient extends Client
{
	private String serverName;
	private PublicKey publicKey;
	
	private static final String CA_NAME = "localhost";
	private static final int CA_PORT = 34567;
	
	public CAClient(String serverName)
	{
		this.serverName = serverName;
	}
	
	public PublicKey getPublicKey()
	{
		return this.publicKey;
	}
	
	public void run()
	{
		Envelope send = new Envelope("GETKEY");
		send.addObject(this.serverName);
		
		Envelope response = null;
		try
		{
			response = (Envelope)this.input.readObject();
		}
		catch (ClassNotFoundException e)
		{
			System.err.println("FATAL ERROR");
			e.printStackTrace();
			return;
		}
		catch (IOException e)
		{
			System.err.println("FATAL ERROR");
			e.printStackTrace();
			return;
		}
		
		if(response.getMessage().equals("OK"))
		{
			this.publicKey = (PublicKey)response.getObjContents().get(0);
			System.out.println("Public key was successfully retrieved");
		}
		else
		{
			System.out.println("Could not get the public key for " + this.serverName);
		}
	}
	
	// javadoc already handled by ClientInterface
	// had to override this in order to be able to use connect without getting a public key.
	@Override
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
}
