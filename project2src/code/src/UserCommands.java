
import java.io.BufferedReader;  // Buffered Reader used for reading input
import java.io.IOException; // In the case that the group or file server times out
// or doesn't respond, we import IOException.
import java.io.InputStreamReader; // Used for Buffered Reader 
import java.util.Arrays; // Test only: use to print out String array with Arrays.toString()
import java.util.List;

import java.util.*;

public class UserCommands {
/**
 * 
 * Instantiates either file client or group client.
 * The main() method will read in input from the user.
 * The user will specify what server (group or file) they want
 * to talk to and what command they wish to execute.
 */
	
	// want file or group client
	// get the user name from the user
	// 
	
	// both throw IOException
	// BR br = new BR(new InputStreamerReader(System.in))
	// br.readline() // returns a string 
	/**
	 *  
	 * @throws IOException possible with readline() method of BufferedReader
	 * in the case that either the server didn't respond. The user asks for either
	 * the group or file server, and either server could time out, which might
	 * be caused by something like the server dying.
	 */
	public static void main(String [] args)
	{
		UserToken userToken = connectUserToGroupServer();
		String userInput = "";
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		// If we get to here, the client is in the user list and we have the
		// token. The client is free to create groups, upload files, etc
		System.out.printf("Enter a command.\n");
		System.out.printf("Type \"quit\" at any time to quit the program.\n");
		do
		{
			try
			{
				// TODO: Finish connecting user to the server and add the commands
				userInput = br.readLine();
				if(userInput.equals("quit"))
					System.exit(0);
				System.out.printf("You entered: \"%s\"\n", userInput);
				// use split() on s to get array
				String[] userCommands = userInput.split(" ");
				System.out.printf("Test: %s\n", Arrays.toString(userCommands));
				parseCommands(userCommands, userToken);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		// Quit when the user tells us to quit
		while (!userInput.equals("quit")); 
		
		if(userInput.equals("quit"))
			System.exit(0);

		// we have the client
		// they want to manage group or file server
		// get their tokens 
		// instantiate a file client or group client
		// ask them for commands
		// this loop doesn't end
		
		// Depending on where they are connecting, parse the commands
		// by calling either the file clients or the group clients methods
		// Then wait for a response from one of the two servers depending
		// on which one they asked for.
	}

	/**
	 * 
	 * @return The return UserToken will never be null. The user will enter an
	 * approved username or will quit.
	 */
	private static UserToken connectUserToGroupServer() 
	{
		try 
		{
			String username = "";
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			// We need to connect the user to the group server -- we create a
			// client
			GroupClient client = new GroupClient();
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
				userToken = client.getToken(username);
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
			} while (userToken != null);
			
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
	 * 
	 * @param userCommands is the commandline / console input from the user.
	 * The approved user is allowed to upload files, deletes files, create groups, etc.
	 */
	private static void parseCommands(String[] userCommands, UserToken userToken) 
	{	
		// s is a big string of messages of successes and failures of any server operations.
		// The String will be printed out after all of the commands execute.
		String s = "";
		String username = "";
		String groupName = "";
		// GroupClient is used for the Group server commands	
		GroupClient gc = new GroupClient();
		// FileClient is used for the File server commands
		FileClient fc = new FileClient();
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
					case "lel":
						System.out.printf("Laugh extra loud!\n");
						break;
					case "gcreateuser":
						// should follow with correct String
						i++;
						username = userCommands[i];
						if(gc.createUser(username, userToken))
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
						// TODO: catch ArrayOutOfBoundsException
						username = userCommands[i];
						if(gc.deleteUser(username, userToken))
						{
							s = s + ("Successfully deleted username \"" + username + "\".\n");
						}
						// User did not have admin privileges
						else						
						{
							s = s + ("Unable to delete username \"" + username + "\" due to insufficient privileges." +
									"Admin privileges are required to delete users.\n");
						} 
						break;
					case "gcreategroup":
						i++;
						groupName = userCommands[i];
						if(userToken == gc.createGroup(groupName, userToken))
						{
							s = s + ("Successfully created group \"" + groupName + "\".\n");
						}
						else
						{
							s = s + ("Unsuccesful in creating group \"" + groupName + "\".\n");
						}
						break;
					case "gdeletegroup":
						i++;
						groupName = userCommands[i];
						if(userToken == gc.deleteGroup(groupName, userToken))
						{
							s = s + ("Successfully deleted group \"" + groupName + "\".\n");
						}
						else
						{
							s = s + ("Unsuccesful in deleting group \"" + groupName + "\".\n");
						}
						break;
					case "gaddusertogroup":
						i++;	
						groupName = userCommands[i];
						i++;	
						username = userCommands[i];
						if(gc.addUserToGroup(username, groupName, userToken))
						{
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
						groupName = userCommands[i];
						i++;	
						username = userCommands[i];
						if(gc.deleteUserFromGroup(username, groupName, userToken))
						{
							s = s + ("Successfully deleted user \"" + username + "\" from group \"" + groupName + "\".\n");
						}
						// Possibly the user wasn't the owner/creator of the group.
						else
						{
							s = s + ("Unsuccesful in deleteing user \"" + username + "\" from group \"" + groupName + "\".");
							s = s + (" Note that only the owner of a group can delete users from a group.\n");
						}
						break;
					// TODO: Make sure listMembers implemntation is correct
					case "glistmembers":
						i++;
						groupName = userCommands[i];
						List<String> userList = new ArrayList<String>();
						userList = gc.listMembers(groupName, userToken);
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
							s = s + ("Insufficient privileges to print users in group \"" + groupName + "\". Only owners can print group members.\n");
						}
						break;
						// ===== File Server Commands =====
						// TODO: Make sure my listFiles implementation is correct =D
					case "flistfiles":
						List<String> fileList = new ArrayList<String>();
						fileList = fc.listFiles(userToken);
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
							s = s + ("Insufficient privileges to print files in group \"" + groupName + "\". Only owners can print group members.\n");
						}
						break;
					case "fupload":
						i++;
						String sourceFile = userCommands[i];
						i++;
						String destFile = userCommands[i];
						i++;
						groupName = userCommands[i];
						// Success
						if(fc.upload(sourceFile, destFile, groupName, userToken))
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
						if(fc.download(sourceFile, destFile, userToken))
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
						if(fc.delete(fileName, userToken))
						{
							s = s + "Successfully deleted file \"" + fileName + "\" from the file server.\n";
						}
						// Failure
						else
						{
							s = s + "unsuccesful in deleting file \"" + fileName + "\" from the file server.\n";
							s = s + "Note that you must be an admin or part of the correct group to delete a file.\n";
						}
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
	}
}
