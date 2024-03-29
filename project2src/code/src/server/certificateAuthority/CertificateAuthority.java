package server.certificateAuthority;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
//import java.security.KeyPair;
//import java.security.KeyPairGenerator;
//import java.security.NoSuchAlgorithmException;
//import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
//import java.security.SecureRandom;
//import java.security.interfaces.RSAPrivateKey;
//import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import message.Envelope;

import server.Server;
import server.group.GroupServer;

/**
 * Assumes this is up 24/7, so we do not deal with writing out upon shut down.
 *
 */
public class CertificateAuthority extends Server
{
	protected ArrayList<String> serverList;
	protected Hashtable<String, PublicKey> serverPublicKeyPairs; //server name --> public key
	
	private static final int DEF_PORT = 4999;
	private static final String NAME = "Certificate Authority";
	private static final String TEMP_FILE_NAME = "caTemp.tmp";
	
	public static void main(String[] args)
	{
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		if (args.length > 0)
		{
			try
			{
				CertificateAuthority server = new CertificateAuthority(Integer.parseInt(args[0]));
				server.start();
			}
			catch (NumberFormatException e)
			{
				System.out.printf("Enter a valid port number or pass no arguments to use the default port (%d)\n", GroupServer.SERVER_PORT);
			}
		}
		else
		{
			CertificateAuthority server = new CertificateAuthority();
			server.start();
		}
	}// end method main(String[])
	
	public CertificateAuthority()
	{
		this(DEF_PORT);
	}
	
	public CertificateAuthority(int port)
	{
		super(port, NAME);
		this.serverList = new ArrayList<String>();
		this.serverPublicKeyPairs = new Hashtable<String, PublicKey>();
	}
	
	public void start()
	{
		File autoSaved = new File(TEMP_FILE_NAME);
		if(autoSaved.exists())
		{
			//TODO reload everything
		}
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutDownListenerCA()));
		ScheduledExecutorService timer = new ScheduledThreadPoolExecutor(1);
		timer.scheduleAtFixedRate(new AutoSaveCA(this), 0, 3, TimeUnit.MINUTES);
		
		ServerSocket serverSock = null;
		try
		{
			serverSock = new ServerSocket(this.port);
		}
		catch (IOException e1)
		{
			System.err.println("FATAL ERROR");
			e1.printStackTrace();
			return;
		}
		
		try
		{
			Socket sock = null;
			CAThread thread = null;
			
			while (true)
			{
				sock = serverSock.accept();
				thread = new CAThread(sock, this);
				System.out.println("got connection from " + sock.getInetAddress().getCanonicalHostName());
				thread.start();
			}
		}// end try block
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}

		try
		{
			serverSock.close();
		}
		catch (IOException e1)
		{
			System.err.println("FATAL ERROR");
			e1.printStackTrace();
			return;
		}
		timer.shutdownNow();
	}
}

/**
 * This thread saves the user and group lists upon shutdown.
 */
class ShutDownListenerCA extends Thread
{
	private static final String TEMP_FILE_NAME = "caTemp.tmp";
	
	public ShutDownListenerCA()
	{
		super();
	}
	
	public final void run()
	{
		System.out.println("Shutting down server");
		File f = new File(TEMP_FILE_NAME);
		f.delete();
	}// end method run()
}// end class ShutDownListener

/**
 * This thread automatically saves the user and group lists every five minutes.
 */
class AutoSaveCA extends Thread
{
	/**
	 * The GroupServer to write out.
	 */
	private final CertificateAuthority certificateAuthority;
	
	private static final String TEMP_FILE_NAME = "caTemp.tmp";
	
	/**
	 * The constructor.
	 * Takes in the GroupServer to write out.
	 * 
	 * @param certificateAuthority The GroupServer to write out.
	 */
	public AutoSaveCA(CertificateAuthority certificateAuthority)
	{
		this.certificateAuthority = certificateAuthority;
	}
	
	public final void run()
	{
		try
		{
			System.out.println("Autosave group and user lists...");
			ObjectOutputStream outStream;
			try
			{
				outStream = new ObjectOutputStream(new FileOutputStream(TEMP_FILE_NAME));
				outStream.writeObject(this.certificateAuthority.serverList);
				outStream.writeObject(this.certificateAuthority.serverPublicKeyPairs);
				outStream.flush();
				outStream.close();
			}
			catch (Exception e)
			{
				e.printStackTrace(System.err);
			}
		}// end try block
		catch (Exception e)
		{
			System.out.println("Autosave Interrupted");
		}
	}// end method run()
}//end class AutoSave

class CAThread extends Thread
{
	/**
	 * socket is a class level variable representing the socket to be used by this class.
	 */
	private final Socket socket;
	
	private CertificateAuthority ca;
	
	public CAThread(Socket socket, CertificateAuthority ca) throws IOException
	{
		super();
		this.socket = socket;
		this.ca = ca;
	}
	
	public final void run()
	{
		try
		{
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			//get message, get public key, store in hashmap and key
			Envelope received = null;
			try
			{
				received = (Envelope)input.readObject();
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
				System.err.println("FATAL ERROR");
				return;
			}
			catch (IOException e)
			{
				e.printStackTrace();
				System.err.println("FATAL ERROR");
				return;
			}
			
			String message = received.getMessage();
			Envelope response = null;
			
			if(message.equals("ADDKEY"))
			{
				String serverName = (String)received.getObjContents().get(0);
				PublicKey pubKey = (PublicKey)received.getObjContents().get(1);
				if(this.ca.serverList.contains(serverName) || serverName == null || pubKey == null)
				{
					response = new Envelope("FAIL");
				}
				else
				{
					try
					{
						this.ca.serverList.add(serverName);
						this.ca.serverPublicKeyPairs.put(serverName, pubKey);
					}
					catch(Exception e)
					{
						e.printStackTrace();
						return;
					}
					
					response = new Envelope("OK");
				}
			}
			else if(message.equals("GETKEY"))
			{
				String serverName = (String)received.getObjContents().get(0);
				if(!this.ca.serverList.contains(serverName))
				{
					response = new Envelope("FAIL");
				}
				else
				{
					response = new Envelope("OK");
					response.addObject(this.ca.serverPublicKeyPairs.get(serverName));
				}
			}
			else
			{
				//unrecognized command
				response = new Envelope("FAIL");
			}
			
			try
			{
				output.writeObject(response);
			}
			catch (IOException e)
			{
				System.err.println("FATAL ERROR");
				e.printStackTrace();
				return;
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
