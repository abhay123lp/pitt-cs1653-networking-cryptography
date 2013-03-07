package client;

import java.io.IOException;
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
}
