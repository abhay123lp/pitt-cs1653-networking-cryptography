
import java.io.BufferedReader;  // Buffered Reader used for reading input
import java.io.FileNotFoundException;
import java.io.IOException; // In the case that the group or file server times out
// or doesn't respond, we import IOException.
import java.io.InputStreamReader; // Used for Buffered Reader 
import java.util.List;
import java.io.FileReader;

import java.util.*;

/**
 * 
 * UserCommands.java is a command-line interface that bridges the gap between the user and the 
 * group and file servers. The user is logged into the group and file servers and can then enter commands to execute 
 * on the servers.
 *
 */
public class UserCommands {

	/**
	 * The main method connects a user to the group server. It then connects the user to the file 
	 * server. A user must be in the user list before being connected to the group server. To do anything, a user must 
	 * be logged into the group server. Until the user is logged into the group server, the user cannot enter commands.
	 * After logging in, the user will be prompted to enter commands. Once the user enters "quit," the user will be
	 * disconnected from the group and file servers.
	 */
	private static FileClient fileClient;
	private static GroupClient groupClient;
	private static String groupServerIP;
	private static int groupServerPort;

	public static void main(String [] args)
	{
		// TODO: Make user log in. Password needed. Edit userlist.bin.
		groupServerIP = "";
		groupServerPort = -1;
		// TODO: update command-line arguments handling to work with password?
		handleInitialCommandlineArguments(args);		
		groupClient = new GroupClient();
		// The user can define the IP and port number from the commandline.
		System.out.println("Connecting to group server");
		groupClient.connect(groupServerIP, groupServerPort);
		System.out.println("Connected");
		// The user logs into the group server
		UserToken userToken = connectUserToGroupServer();
		fileClient = new FileClient();
		String userInput = "";
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));		
		System.out.printf("Enter a command.\n");
		System.out.printf("Type \"quit\" at any time to quit the program.\n");
		System.out.printf("Type \"fconnect IPaddress port_number\" to connect to a file server.\n");
		do
		{
			try
			{
				System.out.printf("\n>>");
				userInput = br.readLine();
				// use split() on s to get array
				String[] userCommands = userInput.split(" ");
				// The userToken gets updated if they add / delete users from a group
				userToken = parseCommands(userCommands, userToken);				
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}		
		while (!userInput.equals("quit")); // Quit when the user tells us to quit

		groupClient.disconnect();
		if(fileClient.isConnected())
		{
			fileClient.disconnect();	
		}

	} // end of main()

	/**
	 * 
	 * @param args is the arguments specified when the user ran the main method() of UserCommands.java.
	 * 
	 * If the user specified no arguments, a default IP and port number are used for the group server. If one argument 
	 * is passed from the command line, it is assumed it is the IP address. If two arguments are passed, it is assumed 
	 * the IP address of the group server and port number are being passed (in that order).
	 * The default command would look like "java UserCommands". Entering "java UserCommands localhost 8765" is
	 * equivalent.
	 * 
	 * Default GroupServer IP: localhost
	 * Default GroupServer port: 8765
	 */
	private static void handleInitialCommandlineArguments(String[] args) {
		if(args.length == 0)
		{
			// IP = localhost for the groupserver
			// Default group server port #: 8765
			groupServerIP = "localhost"; // default
			groupServerPort = 8765;	// default		
		}
		else if(args.length == 1)
		{
			// Assume the user specified the group server IP
			groupServerIP = args[0]; // user defined
			groupServerPort = 8765; // default
		}
		else if(args.length == 2)
		{
			// Assume the user gives us an IP and port number for group server
			groupServerIP = args[0]; // user defined
			try
			{
				groupServerPort = Integer.parseInt(args[1]); // user defined
			}
			catch(NumberFormatException e)
			{
				System.out.printf("To specify the IP and port number for the group server on the commandline,\n");
				System.out.printf("type \"java UserCommands IPaddress port_number\n");
				System.exit(0);
			}
		}
		// Too many arguments
		else
		{
			System.out.printf("Too many commandline arguments were received.\n");
			System.out.printf("To specify the IP and port number for the group server on the commandline,\n");
			System.out.printf("type \"java UserCommands IPaddress port_number\n");
			System.exit(0);
		}
	}

	/**
	 * This method connects the user to the group server and returns that user's 
	 * token to the main method.
	 * @return The return UserToken will never be null. The user will enter an
	 * approved username and password or will quit. 
	 */
	// TODO: password support
	private static UserToken connectUserToGroupServer() 
	{
		try 
		{
			System.out.printf("Connecting user to group server:\n");

			String username = "";
			String password = "";
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			UserToken userToken = null;
			// Look up the user in the file list.
			// Quit if the user tells to quit. The user can
			// only be approved if they are in the file list.
			do 
			{
				System.out.printf("Please enter your user name: ");
				// The user enters their username
				// TODO: Ensure that no user is ever the user "quit"
				if (username.equals("quit"))
					System.exit(0);
				username = br.readLine();
				System.out.print("Please enter your password: ");
				// The user enters their password
				password = br.readLine();
				
				// The user/client is given a token by the group server
				// if their username is in the user list
				userToken = groupClient.getToken(username, password);
				// If the user does not exist and is not in the user list,
				// getToken(username) returns null.
				if (userToken == null) 
				{
					System.out.printf("The username \"%s\" was not found in the file list.", username);
					System.out.printf(" Or the password was incorrect.\n");
					System.out.printf(" Type \"quit\" at any time to quit the program.\n\n");
				}
			} while (userToken == null);
			System.out.printf("Successfully connected user \"" + username + "\" to group server.");
			return userToken;
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		// this will never return null
		return null;
	}

	/**
	 * This method is home to a switch statement which will parse the user's input into 
	 * commands for the group and file servers.
	 * @param userCommands is the commandline / console input from the user.
	 * The approved user is allowed to upload files, deletes files, create groups, etc.
	 * @return A UserToken is returned to the main method in case the user was deleted from or added to a group.
	 */
	private static UserToken parseCommands(String[] userCommands, UserToken userToken) 
	{	
		if(userCommands.length == 0)
		{
			return userToken;
		}
		else if(userCommands[0].length() == 0)
		{
			return userToken;
		}
		// User is not able to execute file server commands if they are not connected to a file server
		// THe user can, however, execute fconnect by necessity
		else if((userCommands[0].charAt(0) == 'f') && !fileClient.isConnected() && !(userCommands[0].equals("fconnect")))
		{
			System.out.printf("You are not logged into the file server. Use fconnect to log into the file server.\n");
			System.out.printf("Specifiy an IP address and port number: \"fconnect IPaddress port_number\"\n");
			System.out.printf("The default IP address is \"localhost\" and the default port is \"4321\" for the group server\n");
			System.out.printf("Type \"fconnect default\" to automatically use the default settings.\n");
			return userToken;
		}


		// s is a big string of messages of successes and failures of any server operations.
		// The String will be printed out after all of the commands execute.
		String s = "";
		String username = "";
		String groupName = "";
		String sourceFile = "";
		String destFile = "";		
		// FileClient is used for the File server commands		
		// newToken is used when adding or deleting a user from a group
		UserToken newToken = null;
		// The try is to catch any ArrayIndexOutOfBounds exceptions.
		// For our purposes, it's simpler and cleaner to catch the exception.
		// Although it's a performance hit for the exception, taking
		// the performance hit for the exception won't affect the real-world
		// performance of the program on today's modern machines.
		try
		{
			for(int i = 0; i < userCommands.length; i++)
			{				
				// ===== Group Server commands	=====
				// Java 6 Compliant
				if( userCommands[i].equals("gcreateuser"))
				{
					// should follow with correct String
					i++;
					username = userCommands[i];
					i++;
					String password = userCommands[i];
					if(groupClient.createUser(username, userToken, password))
					{
						s = s + ("Successfully created username \"" + username + "\" with password \"" + password + "\".\n");
					}
					// User did not have admin privileges
					else						
					{
						s = s + ("Unable to create username \"" + username + "\" with password \"" + password + "\"." +
								" Note that Admin privileges are required to create users.\n");
					} 
				}
				else if( userCommands[i].equals("gdeleteuser"))
				{
					// should follow with correct String
					i++;
					username = userCommands[i];
					if(groupClient.deleteUser(username, userToken))
					{
						s = s + ("Successfully deleted username \"" + username + "\".\n");
					}
					// User did not have admin privileges
					else						
					{
						s = s + ("Unable to delete username \"" + username + "\" due to insufficient privileges.\n" +
								"Admin privileges are required to delete users.\n");
					} 
				}
				else if( userCommands[i].equals("gcreategroup"))
				{
					i++;
					groupName = userCommands[i];
					//groupClient.createGroup(groupName, userToken);
					newToken = groupClient.createGroup(groupName, userToken);
					if(newToken == null)
					{
						s = s + ("Unsuccesful in creating group \"" + groupName + "\".\n");
					}
					else
					{
						// update the user token
						userToken = newToken;
						s = s + ("Successfully created group \"" + groupName + "\".\n");							
					}
				}
				else if( userCommands[i].equals("gdeletegroup"))
				{
					i++;
					groupName = userCommands[i];
					newToken = groupClient.deleteGroup(groupName, userToken);
					if(newToken == null)
					{
						s = s + ("Unsuccesful in deleting group \"" + groupName + "\".\n");
					}
					else
					{
						// update the user token
						userToken = newToken;
						s = s + ("Successfully deleted group \"" + groupName + "\".\n");
					}
				}
				else if( userCommands[i].equals("gaddusertogroup"))
				{
					i++;	
					username = userCommands[i];
					i++;	
					groupName = userCommands[i];
					// Here we need to 
					newToken = groupClient.addUserToGroup(username, groupName, userToken);
					if(newToken != null)
					{
						// We update the user's token in else if( they were added to a new group.
						userToken = newToken;
						s = s + ("Successfully added user \"" + username + "\" to group \"" + groupName + "\".\n");
					}
					// Possibly the user wasn't the owner/creator of the group.
					else
					{
						s = s + ("Unsuccesful in adding user \"" + username + "\" to group \"" + groupName + "\".");
						s = s + (" Note that only the owner of a group can add users to a group.\n");
					}
				}
				else if( userCommands[i].equals("gdeleteuserfromgroup"))
				{
					i++;	
					username = userCommands[i];
					i++;	
					groupName = userCommands[i];
					newToken = groupClient.deleteUserFromGroup(username, groupName, userToken);
					if(newToken != null)
					{
						// update the user token
						userToken = newToken;
						s = s + ("Successfully deleted user \"" + username + "\" from group \"" + groupName + "\".\n");
					}
					// Possibly the user wasn't the owner/creator of the group.
					else
					{
						s = s + ("Unsuccesful in deleteing user \"" + username + "\" from group \"" + groupName + "\".");
						s = s + (" Note that only the owner of a group can delete users from a group.\n");
					}
				}
				else if( userCommands[i].equals("glistmembers"))
				{
					i++;
					groupName = userCommands[i];
					List<String> userList = new ArrayList<String>();
					userList = groupClient.listMembers(groupName, userToken);
					if( userList != null)
					{
						s = s + "There are " + userList.size() + " users in group \"" + groupName + "\":\n";
						// Concatenate the users
						for(String user : userList)
						{
							s = s + "\t" + user + "\n";
						}
					}
					else
					{
						s = s
								+ ("Unable to print users in group \""
										+ groupName + "\". Note that only owners can print group members.\n");
					}
				}
				// ===== File Server Commands =====
				
				// fconnect does not support multiple commands at once
				else if( userCommands[i].equals("fconnect"))
				{
					if(fileClient.isConnected())
					{
						System.out.printf("Disconnect from the current file server before making a new connection\n");
						return userToken;
					}
					
					if(userCommands.length == 1)
					{
						System.out.printf("You are using fconnect improperly.\n");
						System.out.printf("Type \"help\" or type \"man fconnect\" for help.\n");
						return userToken;
					}
					i++;
					String fileServerIP = userCommands[i];
					// User wanted to use default settings
					if(userCommands[i].equals("default"))
					{
						fileClient = new FileClient();						
						fileClient.connect("localhost", 4321);
					}					
					// Assume user entered "fconnect fileserverIP" without port number
					// The default port number 4321 will be used.
					else if(userCommands.length == 2)
					{
						fileClient = new FileClient();
						fileClient.connect(fileServerIP, 4321);
					}
					
					// User specified IP address and port number
					else
					{
						fileClient = new FileClient();						
						int fileServerPort = -1;							
						try
						{
							i++;
							fileServerPort = Integer.parseInt(userCommands[i]);
						}
						catch(NumberFormatException e)
						{
							System.out.printf("Improper format for \"fconnect\".");
							System.out.printf(" Try \"fconnect IPaddress port_number\"\n");
							return userToken;
						}
						fileClient.connect(fileServerIP, fileServerPort);
					}
				}
				else if( userCommands[i].equals("fdisconnect"))
				{
					if(fileClient.isConnected())
					{
						fileClient.disconnect();
						s = s + "Successfully disconnected from the file server.\n";
					}
				}
				else if( userCommands[i].equals("flistfiles"))
				{
					List<String> fileList = new ArrayList<String>();
					fileList = fileClient.listFiles(userToken);
					if( fileList != null)
					{
						s = s + "There are " + fileList.size() + " files viewable by you:\n";
						// Concatenate the files
						for(String file : fileList)
						{
							s = s + "\t" + file + "\n";
						}
					}
					else
					{
						s = s
								+ ("Insufficient privileges to print files in group \""
										+ groupName + "\". Only owners can print group members.\n");
					}
				}
				else if( userCommands[i].equals("fupload"))
				{
					i++;
					sourceFile = userCommands[i];
					i++;
					destFile = userCommands[i];
					i++;
					groupName = userCommands[i];
					// Success
					if(fileClient.upload(sourceFile, destFile, groupName, userToken))
					{
						s = s + "Successfully uploaded local source file \""
								+ sourceFile + "\" as \"" + destFile
								+ "\" to group \"" + groupName
								+ "\" on the file server.\n";
					}
					// Failure
					else
					{
						s = s + "Unsuccesful in uploading local source file \""
								+ sourceFile + "\" as \"" + destFile
								+ "\" to group \"" + groupName
								+ "\" on the file server.\n";
						s = s + "You need to be a part of a group or admin to upload files.\n";
					}
				}
				// TODO: list assumptions of users and admin privileges 
				else if( userCommands[i].equals("fdownload"))
				{
					i++;
					sourceFile = userCommands[i];
					i++;
					destFile = userCommands[i];
					// Success
					if(fileClient.download(sourceFile, destFile, userToken))
					{
						s = s + "Successfully downloaded to local source file \""
								+ sourceFile + "\" from file \"" + destFile
								+ "\" from the file server.\n";
					}
					// Failure
					else
					{
						s = s + "Unsuccesful in download to local source file \""
								+ sourceFile + "\" from file \"" + destFile
								+ "\" from the file server.\n";
						s = s + "You need to be a part of a group or admin to download files.\n";
					}
				}
				else if( userCommands[i].equals("fdelete"))
				{
					i++;
					String fileName = userCommands[i];
					// Success
					if(fileClient.delete(fileName, userToken))
					{
						s = s + "Successfully deleted file \"" + fileName + "\" from the file server.\n";
					}
					// Failure
					else
					{
						s = s + "Unsuccesful in deleting file \"" + fileName + "\" from the file server.\n";
						s = s + "Note that you must be an admin or part of the correct group to delete a file.\n";
					}
				}
				else if( userCommands[i].equals("help"))
				{
					printListOfCommands();
				}
				else if( userCommands[i].equals("man"))
				{
					i++;						
					manual(userCommands[i]);
				}
				else if( userCommands[i].equals("quit"))
				{
					s = s + "Quitting UserCommands.java. Disconnecting user from any servers.\n";
				}
				else
				{
					// Empty the buffer s so the user can see anything that happened
					System.out.printf(s);
					s = "";
					System.out.printf("The command \"" + userCommands[i] + "\" is not a valid command.\n");
					return userToken;
				} 
			} // end for loop
		} // end try
		catch(ArrayIndexOutOfBoundsException e)
		{
			s = s
					+ "You entered an improperly formatted command. For a list of commands, type help.\n"
					+ " For help on how to use the commands, type help followed by a command\n";
		}		
		// Here we print out the success and failures of commands. 
		// Everything gets printed at once after the commands all finish.
		// TODO: Fix me. If we don't disconnect and reconnect, there are issues updating the user list.
		groupClient.disconnect();
		groupClient.connect(groupServerIP, groupServerPort);
		System.out.printf(s);
		return userToken;
	}

	/**
	 *  This method prints all of the available commands for the group and file servers.
	 */
	private static void printListOfCommands() 
	{
		System.out.printf("Here are the supported group and file server commands:");
		System.out.printf(" g = group server command, f = file server command\n");

		System.out.printf("\tgcreateuser\n");
		System.out.printf("\tgdeleteuser\n");
		System.out.printf("\tgcreategroup\n");
		System.out.printf("\tgdeletegroup\n");
		System.out.printf("\tgaddusertogroup\n");
		System.out.printf("\tgdeleteuserfromgroup\n");
		System.out.printf("\tglistmembers\n");
		System.out.printf("\tfconnect\n");
		System.out.printf("\tfdisconnect\n");
		System.out.printf("\tflistfiles\n");
		System.out.printf("\tfupload\n");
		System.out.printf("\tfdownload\n");
		System.out.printf("\tfdelete\n");
		System.out.printf("\thelp\n");		
		System.out.printf("\tman\n");

		System.out.printf("Type \"man COMMAND-NAME\" to see manual information on how to use a command.\n");
		System.out.printf("Type \"man *\" to see all manual information, including all commands.\n");
	}

	/**
	 * manual() lists help the user requests for available commands.
	 * @param command is the command the user wants help with. If the user types "man gcreateuser" the user will be
	 * printed information about that command. It is possible for the user to type "man *" then the user will be 
	 * presented with information on all of the commands.
	 */
	private static void manual(String command)
	{
		//Java 6 compliant
		if(command.equals("help"))
		{
			printListOfCommands();
		}
		else if(command.equals("gcreateuser"))
		{
			printTextFile("gcreateuser");
		}
		else if(command.equals("gdeleteuser"))	
		{
			printTextFile("gdeleteuser");
		}
		else if(command.equals("gcreategroup"))
		{
			printTextFile("gcreategroup");
		}
		else if(command.equals("gdeletegroup"))
		{
			printTextFile("gdeletegroup");
		}
		else if(command.equals("gaddusertogroup"))
		{
			printTextFile("gaddusertogroup");
		}
		else if(command.equals("gdeleteuserfromgroup"))
		{
			printTextFile("gdeleteuserfromgroup");
		}
		else if(command.equals("glistmembers"))
		{
			printTextFile("glistmembers");
		}
		else if(command.equals("fconnect"))
		{
			printTextFile("fconnect");
		}
		else if(command.equals("fdisconnect"))
		{
			printTextFile("fdisconnect");
		}
		else if(command.equals("flistfiles"))
		{
			printTextFile("flistfiles");
		}
		else if(command.equals("fupload"))
		{
			printTextFile("fupload");
		}
		else if(command.equals("fdownload"))
		{
			printTextFile("fdownload");
		}
		else if(command.equals("fdelete"))
		{
			printTextFile("fdelete");
		}
		else if(command.equals("*"))
		{
			printTextFile("gcreateuser");
			printTextFile("gdeleteuser");
			printTextFile("gcreategroup");
			printTextFile("gdeletegroup");		
			printTextFile("gaddusertogroup");
			printTextFile("gdeleteuserfromgroup");
			printTextFile("glistmembers");
			printTextFile("fconnect");
			printTextFile("fdisconnect");
			printTextFile("flistfiles");	
			printTextFile("fupload");		
			printTextFile("fdownload");		
			printTextFile("fdelete");					
		}
		else
		{
			System.out.printf("Check that you entered a valid command. Type \"help\" if necessary.\n");
		}
	}

	/**
	 * This method is intended to print help files.
	 * It can be used to print any text file in the PWD for Eclipse.
	 * The PWD is the project folder where the bin, src, and .settings folders
	 * are located.
	 */
	private static void printTextFile(String fileName) 
	{		
		System.out.printf("\t%s:\n", fileName);
		try
		{
			System.out.println(System.getProperty("user.dir"));
			BufferedReader br = new BufferedReader(new FileReader(/*"" + System.getProperty("user.dir") + System.getProperty("file.separator") +*/ "help files" + System.getProperty("file.separator") + fileName + ".txt"));
			String line = "";
			while ((line = br.readLine()) != null) 
			{
				System.out.printf("%s\n", line);
			}	
			br.close();	    
		}
		catch(FileNotFoundException e)
		{
			System.out.printf("Unable able to find the file \"" + fileName + ".txt\"\n");
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		System.out.printf("\n");
	}
}
