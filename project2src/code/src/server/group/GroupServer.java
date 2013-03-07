/* Group server. Server loads the users from UserList.bin.
 * If user list does not exists, it creates a new list and makes the user the server administrator.
 * On exit, the server saves the user list to file. 
 */

/*
 * TODO: This file will need to be modified to save state related to
 *       groups that are created in the system
 *
 */

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.*;

/**
 * Handles connections to the {@link GroupClient}.
 * Creates a listener to accept incoming connections.
 * Note however that the GroupServer does not know who is connecting to it, but will assume that the client understands the protocol.
 */
public class GroupServer extends Server
{
	/**
	 * The default port for GroupServers to listen from.
	 * This port, should it be used, must be open in the firewall to accept external incoming connections.
	 */
	public static final int SERVER_PORT = 8765;
	
	/**
	 * The list of users that the GroupServer has.
	 */
	public UserList userList;
	
	/**
	 * The list of groups that the GroupServer has.
	 */
	public GroupList groupList;
	
	/**
	 * Default constructor.
	 * Uses the default port of 4321
	 */
	public GroupServer()
	{
		super(SERVER_PORT, "ALPHA");
	}
	
	/**
	 * Constructor to specify a port.
	 * 
	 * @param _port An integer representing the port number to use to accept connections from.
	 */
	public GroupServer(int _port)
	{
		super(_port, "ALPHA");
	}
	
	// TODO: password support
	public void start()
	{
		// Overwrote server.start() because if no user file exists, initial admin account needs to be created
		
		// This runs a thread that saves the lists on program exit
		Runtime runtime = Runtime.getRuntime();
		runtime.addShutdownHook(new ShutDownListener(this));
		
		// Open user file to get user list
		try
		{
			String userFile = "UserList.bin";
			String groupFile = "GroupList.bin";
			ObjectInputStream userStream;
			ObjectInputStream groupStream;
			// FileInputStream fis = new FileInputStream(userFile);
			userStream = new ObjectInputStream(new FileInputStream(userFile));
			userList = (UserList)userStream.readObject();
			// fis.close();
			userStream.close();
			
			groupStream = new ObjectInputStream(new FileInputStream(groupFile));
			groupList = (GroupList)groupStream.readObject();
			groupStream.close();
		}// end try block
		catch (FileNotFoundException e)
		{
			System.out.println("UserList File Does Not Exist. Creating UserList...");
			System.out.println("No users currently exist. Your account will be the administrator.");
			System.out.print("Enter your username: ");
			Scanner console = new Scanner(System.in);
			String username = console.next();
			System.out.print("\nEnter your password: ");
			String password = console.next();
			// Create a new list, add current user to the ADMIN group. They now own the ADMIN group.
			userList = new UserList();
			userList.addUser(username, password);
			userList.addGroup(username, "ADMIN");
			userList.addOwnership(username, "ADMIN");
			
			groupList = new GroupList();
			groupList.addGroup("ADMIN", username);
			groupList.addUser("ADMIN", username);
			
			console.close();
		}// end catch(FileNotFoundException)
		catch (IOException e)
		{
			System.out.println("Error reading from UserList file");
			System.exit(-1);
		}
		catch (ClassNotFoundException e)
		{
			System.out.println("Error reading from UserList file");
			System.exit(-1);
		}
		
		// Autosave Daemon. Saves lists every 5 minutes
		AutoSave aSave = new AutoSave(this);
		aSave.setDaemon(true);
		aSave.start();
		
		// This block listens for connections and creates threads on new connections
		try
		{
			
			final ServerSocket serverSock = new ServerSocket(port);
			
			Socket sock = null;
			GroupThread thread = null;
			
			while (true)
			{
				sock = serverSock.accept();
				thread = new GroupThread(sock, this);
				thread.start();
			}
			// serverSock.close();
		}// end try block
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}// end method start()
}// end class GroupServer

/**
 * This thread saves the user and group lists upon shutdown.
 */
class ShutDownListener extends Thread
{
	/**
	 * The group server to write out.
	 */
	public GroupServer my_gs;
	
	/**
	 * The constructor.
	 * Takes in the GroupServer to write out.
	 * 
	 * @param _gs The GroupServer to write out.
	 */
	public ShutDownListener(GroupServer _gs)
	{
		my_gs = _gs;
	}
	
	public void run()
	{
		System.out.println("Shutting down server");
		ObjectOutputStream outStream;
		try
		{
			outStream = new ObjectOutputStream(new FileOutputStream("UserList.bin"));
			outStream.writeObject(my_gs.userList);
			outStream.flush();
			outStream.close();
			outStream = new ObjectOutputStream(new FileOutputStream("GroupList.bin"));
			outStream.writeObject(my_gs.groupList);
			outStream.flush();
			outStream.close();
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}// end method run()
}// end class ShutDownListener

/**
 * This thread automatically saves the user and group lists every five minutes.
 */
class AutoSave extends Thread
{
	/**
	 * The GroupServer to write out.
	 */
	public GroupServer my_gs;
	
	/**
	 * The constructor.
	 * Takes in the GroupServer to write out.
	 * 
	 * @param _gs The GroupServer to write out.
	 */
	public AutoSave(GroupServer _gs)
	{
		my_gs = _gs;
	}
	
	public void run()
	{
		do
		{
			try
			{
				Thread.sleep(300000); // Save group and user lists every 5 minutes
				System.out.println("Autosave group and user lists...");
				ObjectOutputStream outStream;
				try
				{
					outStream = new ObjectOutputStream(new FileOutputStream("UserList.bin"));
					outStream.writeObject(my_gs.userList);
					outStream.flush();
					outStream.close();
					
					outStream = new ObjectOutputStream(new FileOutputStream("GroupList.bin"));
					outStream.writeObject(my_gs.groupList);
					outStream.flush();
					outStream.close();
				}
				catch (Exception e)
				{
					System.err.println("Error: " + e.getMessage());
					e.printStackTrace(System.err);
				}
			}// end try block
			catch (Exception e)
			{
				System.out.println("Autosave Interrupted");
			}
		} while (true); // end do while
	}// end method run()
}// end class AutoSave
