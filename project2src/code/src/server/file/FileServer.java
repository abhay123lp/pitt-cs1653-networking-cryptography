package server.file;

/* FileServer loads files from FileList.bin.  Stores files in shared_files directory. */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import client.certificateAuthority.CAServerClient;

import server.Server;

/**
 * Handles connections to the {@link FileClient}.
 * Creates a listener to accept incoming connections.
 * Note however that the FileServer does not know who is connecting to it, but will assume that the client understands the protocol.
 */
public class FileServer extends Server
{
	/**
	 * The default port for FileServers to listen from.
	 * This port, should it be used, must be open in the firewall to accept external incoming connections.
	 */
	public static final int SERVER_PORT = 4321; // NOTE: must open port 4321 on firewall for connections
	
	/**
	 * The list of files that the FileServer has.
	 */
	public static FileList fileList;
	
	private static final String CA_LOC = "localhost";
	private static final int DEF_CA_PORT = 4999;
	
	/**
	 * Default constructor.
	 * Uses the default port of 4321
	 */
	public FileServer()
	{
		super(SERVER_PORT, "FilePile");
	}
	
	/**
	 * Constructor to specify a port.
	 * 
	 * @param _port An integer representing the port number to use to accept connections from.
	 */
	public FileServer(int _port)
	{
		super(_port, "FilePile");
	}
	
	public FileServer(int _port, String serverName)
	{
		super(_port, serverName);
	}
	
	public void start()
	{
		String fileFile = "FileList.bin";
		ObjectInputStream fileStream;
		
		// This runs a thread that saves the lists on program exit
		Runtime runtime = Runtime.getRuntime();
		Thread catchExit = new Thread(new ShutDownListenerFS());
		runtime.addShutdownHook(catchExit);
		
		// Open user file to get user list
		try
		{
			FileInputStream fis = new FileInputStream(fileFile);
			fileStream = new ObjectInputStream(fis);
			fileList = (FileList)fileStream.readObject();
		}
		catch (FileNotFoundException e)
		{
			System.out.println("FileList Does Not Exist. Creating FileList...");
			
			fileList = new FileList();
			CAServerClient caServerClient = new CAServerClient(this.name, this.publicKey);
			caServerClient.connect(CA_LOC, DEF_CA_PORT, null);
			caServerClient.run();
			caServerClient.disconnect();
		}
		catch (IOException e)
		{
			System.out.println("Error reading from FileList file");
			System.exit(-1);
		}
		catch (ClassNotFoundException e)
		{
			System.out.println("Error reading from FileList file");
			System.exit(-1);
		}
		
		// I don't even...
		File file = new File("shared_files");
		if (file.mkdir())
		{
			System.out.println("Created new shared_files directory");
		}
		else if (file.exists())
		{
			System.out.println("Found shared_files directory");
		}
		else
		{
			System.out.println("Error creating shared_files directory");
		}
		
		// Autosave Daemon. Saves lists every 5 minutes
		AutoSaveFS aSave = new AutoSaveFS();
		aSave.setDaemon(true);
		aSave.start();
		
		boolean running = true;
		
		try
		{
			final ServerSocket serverSock = new ServerSocket(port);
			System.out.printf("%s up and running\n", this.getClass().getName());
			
			Socket sock = null;
			Thread thread = null;
			
			while (running)
			{
				sock = serverSock.accept();
				thread = new FileThread(sock, this.privateKey, this.publicKey, this.name, sock.getInetAddress().getHostAddress(), this.port);
				thread.start();
			}
			
			serverSock.close();
			System.out.printf("%s shut down\n", this.getClass().getName());
		}// end try block
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}
	}// end method start
}// end class FileServer

/**
 * This thread saves the file list upon shut down.
 */
class ShutDownListenerFS implements Runnable
{
	public void run()
	{
		System.out.println("Shutting down server");
		ObjectOutputStream outStream;
		
		try
		{
			outStream = new ObjectOutputStream(new FileOutputStream("FileList.bin"));
			outStream.writeObject(FileServer.fileList);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}
	}
}// end class ShutDownListenerFS

/**
 * This thread automatically saves the file list every five minutes.
 */
class AutoSaveFS extends Thread
{
	public void run()
	{
		do
		{
			try
			{
				Thread.sleep(300000); // Save user lists every 5 minutes
				System.out.println("Autosave file list...");
				ObjectOutputStream outStream;
				try
				{
					outStream = new ObjectOutputStream(new FileOutputStream("FileList.bin"));
					outStream.writeObject(FileServer.fileList);
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
		} while (true);
	}// end method run()
}// end class AutoSaveFS
