/**
 * 
 */

package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
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
//			System.out.println("Attempting to connect...");
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
			
//			System.out.println("Success!  Connected to " + server + " at port " + port);
			return true;
		}// end method connect(String, int)
}
