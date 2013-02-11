
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
 * @class UserCommands.java is a command-line interface that bridges the gap between the user and the 
 * group and file servers.
 *
 */
public class UserCommands {

	private static FileClient fileClient;
	private static GroupClient groupClient;
	
	/**
	 * The main method connects a user to the group server. It then connects the user to the file 
	 * server. A user must be in the user list before being connected to the group server. To do anything, a user must 
	 * be logged into the group server. Until the user is logged into the group server, the user cannot enter commands.
	 * After logging in, the user will be prompted to enter commands. Once the user enters "quit," the user will be
	 * disconnected from the group and file servers.
	 */
	public static void main(String [] args)
	{
		groupClient = new GroupClient();
		// Port 8765 = group server
		groupClient.connect("localhost", 8765);
		fileClient = new FileClient();
		// Port 4321 = file server
		fileClient.connect("localhost", 4321);
		UserToken userToken = connectUserToGroupServer();
		
		String userInput = "";
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));		
		System.out.printf("Enter a command.\n");
		System.out.printf("Type \"quit\" at any time to quit the program.\n");
		do
		{
			try
			{
				userInput = br.readLine();
				if(userInput.equals("quit"))
					System.exit(0);
				System.out.printf("You entered: \"%s\"\n", userInput);
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
		fileClient.disconnect();

	} // end of main()

	/**
	 * This method connects the user to the group server and returns that user's 
	 * token to the main method.
	 * @return The return UserToken will never be null. The user will enter an
	 * approved username or will quit. 
	 */
	private static UserToken connectUserToGroupServer() 
	{
		try 
		{
			String username = "";
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			UserToken userToken = null;
			// Look up the user in the file list.
			// Quit if the user tells to quit. The user can
			// only be approved if they are in the file list.
			do 
			{
				System.out.printf("Please enter your user name: ");
				// The user enters their username
				username = br.readLine();
				// TODO: Ensure that no user is ever the user "quit"
				if (username.equals("quit"))
					System.exit(0);
				// The user/client is given a token by the group server
				// if their username is in the user list
				userToken = groupClient.getToken(username);
				// If the user does not exist and is not in the user list,
				// getToken(username) returns null.
				if (userToken == null) 
				{
					System.out
							.printf("The username \"%s\" was not found in the file list.",
									username);
					System.out
							.printf("Type \"quit\" at any time to quit the program.\n");
				}
			} while (userToken == null);
			
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
				switch(userCommands[i])
				{
					// ===== Group Server commands	=====
					case "gcreateuser":
						// should follow with correct String
						i++;
						username = userCommands[i];
						if(groupClient.createUser(username, userToken))
						{
							s = s + ("Successfully created username \"" + username + "\".\n");
						}
						// User did not have admin privileges
						else						
						{
							s = s + ("Unable to create username \"" + username + "\" due to insufficient privileges." +
									"Admin privileges are required to create users.\n");
						} 
						break;
					case "gdeleteuser":
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
						break;
					case "gcreategroup":
						i++;
						groupName = userCommands[i];
						//groupClient.createGroup(groupName, userToken);
						if(groupClient.createGroup(groupName, userToken) == null)
						{
							s = s + ("Unsuccesful in creating group \"" + groupName + "\".\n");
						}
						else
						{
							s = s + ("Successfully created group \"" + groupName + "\".\n");							
						}
						break;
					case "gdeletegroup":
						i++;
						groupName = userCommands[i];
						if(groupClient.deleteGroup(groupName, userToken) == null)
						{
							s = s + ("Unsuccesful in deleting group \"" + groupName + "\".\n");
						}
						else
						{
							s = s + ("Successfully deleted group \"" + groupName + "\".\n");
						}
						break;
					case "gaddusertogroup":
						i++;	
						username = userCommands[i];
						i++;	
						groupName = userCommands[i];
						// Here we need to 
						newToken = groupClient.addUserToGroup(username, groupName, userToken);
						if(newToken != null)
						{
							// We update the user's token in case they were added to a new group.
							userToken = newToken;
							s = s + ("Successfully added user \"" + username + "\" to group \"" + groupName + "\".\n");
						}
						// Possibly the user wasn't the owner/creator of the group.
						else
						{
							s = s + ("Unsuccesful in adding user \"" + username + "\" to group \"" + groupName + "\".");
							s = s + (" Note that only the owner of a group can add users to a group.\n");
						}
						break;
					case "gdeleteuserfromgroup":
						i++;	
						username = userCommands[i];
						i++;	
						groupName = userCommands[i];
						newToken = groupClient.deleteUserFromGroup(username, groupName, userToken);
						if(newToken != null)
						{
							userToken = newToken;
							s = s + ("Successfully deleted user \"" + username + "\" from group \"" + groupName + "\".\n");
						}
						// Possibly the user wasn't the owner/creator of the group.
						else
						{
							s = s + ("Unsuccesful in deleteing user \"" + username + "\" from group \"" + groupName + "\".");
							s = s + (" Note that only the owner of a group can delete users from a group.\n");
						}
						break;
					case "glistmembers":
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
								s = s + user + "\n";
							}
						}
						else
						{
						s = s
								+ ("Insufficient privileges to print users in group \""
										+ groupName + "\". Only owners can print group members.\n");
						}
						break;
						// ===== File Server Commands =====
					case "flistfiles":
						List<String> fileList = new ArrayList<String>();
						fileList = fileClient.listFiles(userToken);
						if( fileList != null)
						{
							s = s + "There are " + fileList.size() + " files viewable by you:\n";
							// Concatenate the files
							for(String file : fileList)
							{
								s = s + file + "\n";
							}
						}
						else
						{
						s = s
								+ ("Insufficient privileges to print files in group \""
										+ groupName + "\". Only owners can print group members.\n");
						}
						break;
					case "fupload":
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
						break;
						// TODO: list assumptions of users and admin privileges 
					case "fdownload":
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
						break;
					case "fdelete":
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
						break;
					case "help":
							printListOfCommands();
						break;
					case "man":
						i++;						
						manual(userCommands[i]);
						break;
					default:		
						s = s + "The command \"" + userCommands[i] + "\" is not a valid command.";
						break;
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
		System.out.printf("\tflistfiles\n");
		System.out.printf("\tfupload\n");
		System.out.printf("\tfdownload\n");
		System.out.printf("\tfdelete\n");
		System.out.printf("\thelp\n");		
		System.out.printf("\tman\n");
		
		System.out.printf("Type \"man COMMAND-NAME\" to see information on how to use a command.\n");
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
			switch(command)
			{			
				case "help":
					printListOfCommands();
					break;
				case "gcreateuser":
					printTextFile("gcreateuser");
					break;
				case "gdeleteuser":		
					printTextFile("gdeleteuser");
					break;
				case "gcreategroup":	
					printTextFile("gcreategroup");
					break;
				case "gdeletegroup":	
					printTextFile("gdeletegroup");
					break;
				case "gaddusertogroup":				
					printTextFile("gaddusertogroup");
					break;
				case "gdeleteuserfromgroup":		
					printTextFile("gdeleteuserfromgroup");
					break;
				case "glistmembers":
					printTextFile("glistmembers");
					break;
				case "flistfiles":				
					printTextFile("flistfiles");
					break;
				case "fupload":				
					printTextFile("fupload");
					break;
				case "fdownload":				
					printTextFile("fdownload");
					break;
				case "fdelete":				
					printTextFile("fdelete");
					break;
				case "*":
					printTextFile("gcreateuser");
					printTextFile("gdeleteuser");
					printTextFile("gcreategroup");
					printTextFile("gdeletegroup");		
					printTextFile("gaddusertogroup");
					printTextFile("gdeleteuserfromgroup");
					printTextFile("glistmembers");			
					printTextFile("flistfiles");	
					printTextFile("fupload");		
					printTextFile("fdownload");		
					printTextFile("fdelete");					
					break;
				default:
					System.out.printf("Check that you entered a valid command. Type \"help\" if necessary.\n");
					break;
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
			BufferedReader br = new BufferedReader(new FileReader(fileName + ".txt"));
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
