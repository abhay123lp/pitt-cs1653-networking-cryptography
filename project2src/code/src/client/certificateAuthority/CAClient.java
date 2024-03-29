package client.certificateAuthority;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;

import client.Client;

import message.Envelope;

public class CAClient extends Client
{
	private String serverName;
	private PublicKey publicKey;
	
	private static final String CA_LOC = "localhost";
	private static final int CA_PORT = 4999;
	
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
		
		try
		{
			this.output.writeObject(send);
			this.output.flush();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return;
		}
		
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
			/****** WE MADE NEED TO ADJUST THIS TO ACCOUNT FOR CHECK VALIDITITY ******/
			this.publicKey = (PublicKey)response.getObjContents().get(0);
			System.out.println("Public key was successfully retrieved");
		}
		else
		{
			System.out.println("Could not get the public key for " + this.serverName);
		}
	}
	
	public boolean connect()
	{
		return this.connect(CA_LOC, CA_PORT, null);
	}
	
	// javadoc already handled by ClientInterface
	// had to override this in order to be able to use connect without getting a public key.
	@Override
	public boolean connect(final String server, final int port, final String serverName)
	{
		if (this.sock != null)
		{
			System.out.println("Disconnecting from previous connection...");
			this.disconnect();
		}
		try
		{
			this.sock = new Socket(server, port);
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
		return true;
	}// end method connect(String, int)
	
	@Override
	public void disconnect()
	{
		if (isConnected())
		{
			try
			{
				this.output.close();
				this.input.close();
				this.sock.close();
			}
			catch (Exception e)
			{
				e.printStackTrace(System.err);
			}
			this.sock = null;
		}
	}//end method disconnect
}
