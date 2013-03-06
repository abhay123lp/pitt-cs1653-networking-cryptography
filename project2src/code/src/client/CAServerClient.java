/**
 * 
 */
package client;

import java.io.IOException;
import java.security.PublicKey;

import message.Envelope;


/**
 * 
 *
 */
public class CAServerClient extends Client
{
	private PublicKey publicKey;
	private String serverName;
	
	/**
	 * 
	 */
	public CAServerClient(String serverName, PublicKey publicKey)
	{
		this.serverName = serverName;
		this.publicKey = publicKey;
	}
	
	public void run()
	{
		Envelope send = new Envelope("ADDKEY");
		send.addObject(this.serverName);
		send.addObject(this.publicKey);
		Envelope response = null;
		try
		{
			this.output.writeObject(send);
			response = (Envelope)this.input.readObject();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
			return;
		}
		
		if(response.getMessage().equals("OK"))
		{
			System.out.println("Public Key accepted");
		}
		else
		{
			System.out.println("Something went wrong...");
		}
	}
}
