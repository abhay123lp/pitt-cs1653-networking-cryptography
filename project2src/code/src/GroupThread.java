/* This thread does all the work. It communicates with the client through Envelopes.
 * 
 */
import java.lang.Thread;
import java.net.Socket;
import java.io.*;
import java.util.*;

public class GroupThread extends Thread 
{
	private final Socket socket;
	private GroupServer my_gs;
	private static final String ADMIN_GROUP_NAME = "ADMIN";

	public GroupThread(Socket _socket, GroupServer _gs)
	{
		socket = _socket;
		my_gs = _gs;
	}

	public void run()
	{
		boolean proceed = true;

		try
		{
			//Announces connection and opens object streams
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());

			do
			{
				Envelope message = (Envelope)input.readObject();
				System.out.println("Request received: " + message.getMessage());
				Envelope response;

				if(message.getMessage().equals("GET"))//Client wants a token
				{
					String username = (String)message.getObjContents().get(0); //Get the username
					if(username == null)
					{
						response = new Envelope("FAIL");
						response.addObject(null);
						output.writeObject(response);
					}
					else
					{
						UserToken yourToken = createToken(username); //Create a token

						//Respond to the client. On error, the client will receive a null token
						response = new Envelope("OK");
						response.addObject(yourToken);
						output.writeObject(response);
					}
				}
				else if(message.getMessage().equals("CUSER")) //Client wants to create a user
				{
					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								String username = (String)message.getObjContents().get(0); //Extract the username
								UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

								if(createUser(username, yourToken))
								{
									response = new Envelope("OK"); //Success
								}
							}
						}
					}

					output.writeObject(response);
				}
				else if(message.getMessage().equals("DUSER")) //Client wants to delete a user
				{

					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								String username = (String)message.getObjContents().get(0); //Extract the username
								UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

								if(deleteUser(username, yourToken))
								{
									response = new Envelope("OK"); //Success
								}
							}
						}
					}

					output.writeObject(response);
				}
				else if(message.getMessage().equals("CGROUP")) //Client wants to create a group
				{
					/* TODO:  Write this handler */
					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								// do create a group stuff in here
								String groupname = (String)message.getObjContents().get(0); //Extract the groupname
								UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

								if(createGroup(groupname, yourToken))
								{
									response = new Envelope("OK"); //Success									
									yourToken.getGroups().add(groupname);									
									// Make sure that group client updates its own token
									response.addObject(yourToken);
								}
							}
						}
					}
					output.writeObject(response);
				}
				else if(message.getMessage().equals("DGROUP")) //Client wants to delete a group
				{
					/* TODO:  Write this handler */
					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{								
								String groupname = (String)message.getObjContents().get(0); //Extract the groupname
								UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

								if(deleteGroup(groupname, yourToken))
								{
									response = new Envelope("OK"); //Success									
									yourToken.getGroups().remove(groupname);	// remove the group from the users token																									
									response.addObject(yourToken);	// send the updated token in response
								}
							}
						}
					}
					
					output.writeObject(response);
				}
				else if(message.getMessage().equals("LMEMBERS")) //Client wants a list of members in a group
				{
					/* TODO:  Write this handler */
					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								// Return a list of members in a group 
								String groupname = (String)message.getObjContents().get(0); //Extract the groupname
								UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

								List<String> members = listMembers(groupname, yourToken);

								if( members != null){
									response = new Envelope("OK"); //Success	
									response.addObject(members); // Add member list to the response
								}
							}
						}
					}
					
					output.writeObject(response);
				}
				else if(message.getMessage().equals("AUSERTOGROUP")) //Client wants to add user to a group
				{
					/* TODO:  Write this handler */
					if(message.getObjContents().size() < 3)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{

								if(message.getObjContents().get(2) != null){

									String username = (String)message.getObjContents().get(0);  // Extract the username
									String groupname = (String)message.getObjContents().get(1); //Extract the groupname
									UserToken yourToken = (UserToken)message.getObjContents().get(2); //Extract the token

									if(addUserToGroup(username, groupname, yourToken))
									{
										response = new Envelope("OK"); //Success
										// Add the group name for the user in the token
										if(username.equals(yourToken.getSubject()))
										{
											yourToken.getGroups().add(groupname);	
										}

										response.addObject(yourToken);
									}


								}
							}
						}
					}
					output.writeObject(response);
				}
				else if(message.getMessage().equals("RUSERFROMGROUP")) //Client wants to remove user from a group
				{
					/* TODO:  Write this handler */
					if(message.getObjContents().size() < 3)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								if(message.getObjContents().get(2) != null){
									
									// Remove a user from a group TODO
									String username = (String)message.getObjContents().get(0);  // Extract the username
									String groupname = (String)message.getObjContents().get(1); //Extract the groupname
									UserToken yourToken = (UserToken)message.getObjContents().get(2); //Extract the token

									if(deleteUserFromGroup(username, groupname, yourToken))
									{
										response = new Envelope("OK"); //Success	
										// Remove the groupname from the user in the token
										yourToken.getGroups().remove(groupname);										
										response.addObject(yourToken);

									}
									
								}
								
							}
						}

					}

					output.writeObject(response);
				}
				else if(message.getMessage().equals("DISCONNECT")) //Client wants to disconnect
				{
					socket.close(); //Close the socket
					proceed = false; //End this communication loop
				}
				else
				{
					response = new Envelope("FAIL"); //Server does not understand client request
					output.writeObject(response);
				}
			}while(proceed);	
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	//Method to create tokens
	private UserToken createToken(String username) 
	{
		//Check that user exists
		if(my_gs.userList.checkUser(username))
		{
			//Issue a new token with server's name, user's name, and user's groups
			UserToken yourToken = new Token(my_gs.name, username, my_gs.userList.getUserGroups(username));
			return yourToken;
		}
		else
		{
			return null;
		}
	}

	//Method to create a user
	private boolean createUser(String username, UserToken yourToken)
	{
		String requester = yourToken.getSubject();

		//Check if requester exists
		if(my_gs.userList.checkUser(requester))
		{
			//Get the user's groups
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			//requester needs to be an administrator
			if(temp.contains(ADMIN_GROUP_NAME))
			{
				//Does user already exist?
				if(my_gs.userList.checkUser(username))
				{
					return false; //User already exists
				}
				else
				{
					my_gs.userList.addUser(username);
					return true;
				}
			}
			else
			{
				return false; //requester not an administrator
			}
		}
		else
		{
			return false; //requester does not exist
		}
	}

	//Method to delete a user
	private boolean deleteUser(String username, UserToken yourToken)
	{
		String requester = yourToken.getSubject();

		//Does requester exist?
		if(my_gs.userList.checkUser(requester))
		{
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			//requester needs to be an administer
			if(temp.contains(ADMIN_GROUP_NAME))
			{
				//Does user exist?
				if(my_gs.userList.checkUser(username))
				{
					//User needs deleted from the groups they belong
					ArrayList<String> deleteFromGroups = new ArrayList<String>();

					//This will produce a hard copy of the list of groups this user belongs
					for(int index = 0; index < my_gs.userList.getUserGroups(username).size(); index++)
					{
						deleteFromGroups.add(my_gs.userList.getUserGroups(username).get(index));
					}

					//Delete the user from the groups
					//If user is the owner, removeMember will automatically delete group!
					for(int index = 0; index < deleteFromGroups.size(); index++)
					{
						// -- old implementation -- my_gs.groupList.removeMember(username, deleteFromGroups.get(index));
						my_gs.groupList.removeUser(username, deleteFromGroups.get(index));
					}

					//If groups are owned, they must be deleted
					ArrayList<String> deleteOwnedGroup = new ArrayList<String>();

					//Make a hard copy of the user's ownership list
					for(int index = 0; index < my_gs.userList.getUserOwnership(username).size(); index++)
					{
						deleteOwnedGroup.add(my_gs.userList.getUserOwnership(username).get(index));
					}

					//Delete owned groups
					for(int index = 0; index < deleteOwnedGroup.size(); index++)
					{
						//Use the delete group method. Token must be created for this action
						deleteGroup(deleteOwnedGroup.get(index), new Token(my_gs.name, username, deleteOwnedGroup));
					}

					//Delete the user from the user list
					my_gs.userList.deleteUser(username);

					return true;	
				}
				else
				{
					return false; //User does not exist
				}
			}
			else
			{
				return false; //requester is not an administer
			}
		}
		else
		{
			return false; //requester does not exist
		}
	}



	/**
	 * TODO: createGroup
	 * Will create a group for a user.  Any user can create a group.
	 * @param groupname
	 * @param token
	 * @return
	 */
	private boolean createGroup(String groupname, UserToken token)
	{
		String requester = token.getSubject();

		//Check if requester exists
		if(my_gs.userList.checkUser(requester))
		{
			//Does group already exist?
			if(my_gs.groupList.checkGroup(groupname))
			{
				return false; //Group already exists
			}
			else
			{						
				// Add group to group list
				my_gs.groupList.addGroup(groupname, requester);					
				// Add group and owner to user list
				my_gs.userList.addGroup(requester, groupname);	
				my_gs.userList.addOwnership(requester, groupname);
				return true;
			}
		}

		return false; //requester does not exist

	}

	/**
	 * TODO: deleteGroup
	 * Will delete a group if the user is an admin or owner of the group as specified by UserToken.
	 * @param groupname
	 * @param token
	 * @return
	 */
	private boolean deleteGroup(String groupname, UserToken token){

		String requester = token.getSubject();

		//Does requester exist?
		if(my_gs.userList.checkUser(requester))
		{

			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);

			// Get the owner of the group
			String ownerOfGroup = my_gs.groupList.getGroupOwner(groupname);

			// Requester needs to be an administer or owner of the group
			if(temp.contains(ADMIN_GROUP_NAME) || ownerOfGroup.equals(requester))
			{
				// (1)
				ArrayList<String> usersInGroup = my_gs.groupList.getGroupUsers(groupname);

				// (2)
				my_gs.groupList.deleteGroup(groupname);

				//(3)
				for(String user : usersInGroup){		

					my_gs.userList.removeGroup(user, groupname);	

				}
				return true;
			} else{
				return false; //requester not an administrator
			}
		}

		return false; //requester does not exist

	}

	/**
	 * TODO: addUserToGroup
	 * 
	 * @param user
	 * @param groupname
	 * @param token
	 * @return True if user was added to the group, false if not.
	 */
	private boolean addUserToGroup(String user, String group, UserToken token){
		String requester = token.getSubject();

		// Does requester exist and does group exist.
		if(my_gs.userList.checkUser(requester))
		{
			if(my_gs.userList.checkUser(user) && my_gs.groupList.checkGroup(group)){

				//Get the list of groups of the requester
				ArrayList<String> temp = my_gs.userList.getUserGroups(requester);

				// Get the owner of the group we are trying to add the user to
				String ownerOfGroup = my_gs.groupList.getGroupOwner(group);

				// Requester needs to be an administer or owner of the group to add a user to a group
				if(temp.contains(ADMIN_GROUP_NAME) || ownerOfGroup.equals(requester))
				{
					// Make sure that the user is not already in the group
					if(my_gs.userList.getUserGroups(user).contains(group)){
						return false; // user already exists in group
					} else{
						// add the user the group in group list
						my_gs.groupList.addUser(group, user);
						// add group to the user
						my_gs.userList.addGroup(user, group);
					}										
					return true;
				} 

			}

		}

		return false; //requester does not exist

	}

	//Note: does not alter user token here
	/**
	 * TODO: deleteUserFromGroup
	 * @param user
	 * @param group
	 * @param token
	 * @return
	 */
	private boolean deleteUserFromGroup(String user, String group, UserToken token)
	{
		//user does not exist, group does not exist, or token user is not an owner of the group or admin
		if(!my_gs.userList.checkUser(user) || !my_gs.groupList.checkGroup(group) || !(my_gs.userList.getUserOwnership(token.getSubject()).contains(group) || my_gs.groupList.getGroupUsers(ADMIN_GROUP_NAME).contains(token.getSubject())))
		{
			return false;
		}
		my_gs.groupList.removeUser(group, user);
		my_gs.userList.removeGroup(user, group);
		return true;
	}

	/**
	 * TODO: listMembers
	 * Will return a list of members of a group if the user is an admin or owner of the group.
	 * @param group
	 * @param yourToken
	 * @return a list of members of the group
	 */
	private List<String> listMembers(String groupname, UserToken token)
	{
		String requester = token.getSubject();
		
		// Check if group exists
		if(my_gs.groupList.checkGroup(groupname)){
			
			// Get the owner of the group
			String ownerOfGroup = my_gs.groupList.getGroupOwner(groupname);
			
			// ****** NOTE *******
			// requester needs to be an administer or owner of the group to see list of members
			if(ownerOfGroup.equals(requester) || my_gs.groupList.getGroupUsers(ADMIN_GROUP_NAME).contains(requester) )
			{				
				return my_gs.groupList.getGroupUsers(groupname);
			} 
			
			
		}
		
		return null;
				

	}


}
