package server.group;

/* This thread does all the work. It communicates with the client through Envelopes.
 * 
 */

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

import server.ServerThread;

import message.Envelope;
import message.Token;
import message.UserToken;


/**
 * The thread spawned by {@link GroupServer} after accepting a connection.
 * This thread handles all envelopes coming to the server from a client.
 * 
 * @see GroupClient
 */
public class GroupThread extends ServerThread
{
	/**
	 * my_gs represents the GroupServer to be used by this class.
	 */
	private GroupServer my_gs;
	
	/**
	 * ADMIN_GROUP_NAME is a constant representing the name of the administrative group in the group server.
	 */
	private static final String ADMIN_GROUP_NAME = "ADMIN"; 
	
	/**
	 * This constructor sets the socket class variable and GroupServer class variable.
	 * 
	 * @param _socket The socket to use.
	 * @param _gs The group server object to use.
	 */
	public GroupThread(Socket _socket, GroupServer _gs, RSAPrivateKey _privKey, RSAPublicKey _pubKey)
	{
		super(_socket, _pubKey, _privKey);
//		socket = _socket;
		my_gs = _gs;
//		privateKey = _privKey;
//		publicKey = _pubKey;
		
	}
	
	/**
	 * This method runs all of the group server commands such as adding a user to a group, removing a user from a group, adding a group, etc...
	 */
	public void run()
	{
		boolean proceed = true;
		
		try
		{
			// Announces connection and opens object streams
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			do
			{
				Envelope message = (Envelope)input.readObject();
				System.out.println("Request received: " + message.getMessage());
				Envelope response;
				
				if (message.getMessage().equals("GET"))// Client wants a token
				{
					//String username = (String)message.getObjContents().get(0); // Get the username
					
					String username = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(0), (byte[])message.getObjContents().get(2)));
					
					if (username == null)
					{
						response = new Envelope("FAIL");
						response.addObject(null);
																		
						output.writeObject(response);
					}
					else
					{
						String password = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(1), (byte[])message.getObjContents().get(2)));
						
						if(checkPassword(username, password))
						{
							UserToken yourToken = createToken(username); // Create a token
							
							// Respond to the client. On error, the client will receive a null token
							response = encryptResponseWithSymmetricKey(new Object[]{yourToken}, "OK");
							output.writeObject(response);
						}
						// Password did not match
						else
						{
							// Respond to the client. On error, the client will receive a null token
							response = new Envelope("FAIL");
							output.writeObject(response);
						}
					}
				}// end if block
				else if (message.getMessage().equals("REQUEST_SECURE_CONNECTION"))// Client wants a token
				{
					output.writeObject(this.setUpSecureConnection(message));
				}
				else if (message.getMessage().equals("CUSER")) // Client wants to create a user
				{
					if (message.getObjContents().size() < 3)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");
						
						if (message.getObjContents().get(0) != null)
						{
							if (message.getObjContents().get(1) != null)
							{
								String username = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(0), (byte[])message.getObjContents().get(3)));
								System.out.println(username);
								UserToken yourToken = (UserToken)convertToObject(decryptObjects((byte[])message.getObjContents().get(2), (byte[])message.getObjContents().get(3)));
								
								boolean checkToken = yourToken.RSAVerifySignature("SHA1withRSA", "BC", publicKey);
								
								if(checkToken)
								{					
									String password = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(1), (byte[])message.getObjContents().get(3)));
									if (createUser(username, yourToken, password))
									{
										response = new Envelope("OK"); // Success
									}
								}
								else
								{
									System.out.println("Token not authenticated.");
								}
							}
						}// end if block
					}// end else block
					output.writeObject(response);
				}// end else if block
				else if (message.getMessage().equals("DUSER")) // Client wants to delete a user
				{
					if (message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");
						
						if (message.getObjContents().get(0) != null)
						{
							if (message.getObjContents().get(1) != null)
							{
								String username = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(0), (byte[])message.getObjContents().get(2)));
								UserToken yourToken = (UserToken)convertToObject(decryptObjects((byte[])message.getObjContents().get(1), (byte[])message.getObjContents().get(2)));

								boolean checkToken = yourToken.RSAVerifySignature("SHA1withRSA", "BC", publicKey);
								
								if(checkToken)
								{
									if (deleteUser(username, yourToken))
									{
										response = new Envelope("OK"); // Success
									}
								}
								else
								{
									System.out.println("Token not authenticated.");
								}
							}
						}// end if block
					}// end else block
					output.writeObject(response);
				}// end else if block
				else if (message.getMessage().equals("CGROUP")) // Client wants to create a group
				{
					/* TODO: Write this handler */
					if (message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");
						
						if (message.getObjContents().get(0) != null)
						{
							if (message.getObjContents().get(1) != null)
							{
								String groupname = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(0), (byte[])message.getObjContents().get(2)));
								
								Token yourToken = (Token)convertToObject(decryptObjects((byte[])message.getObjContents().get(1), (byte[])message.getObjContents().get(2)));
								
								boolean checkToken = yourToken.RSAVerifySignature("SHA1withRSA", "BC", publicKey);
								if(checkToken)
								{
									if (createGroup(groupname, yourToken))
									{
										response = new Envelope("OK"); // Success
										yourToken.getGroups().add(groupname);
										yourToken.generateRSASignature("SHA1withRSA", "BC", privateKey);
										response = encryptResponseWithSymmetricKey(new Object[]{yourToken}, "OK");
									}
								}
								else
								{
									System.out.println("Token not authenticated.");
								}
							}
						}// end if block
					}// end else block
					output.writeObject(response);
				}// end else if block
				else if (message.getMessage().equals("DGROUP")) // Client wants to delete a group
				{
					/* TODO: Write this handler */
					if (message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");
						
						if (message.getObjContents().get(0) != null)
						{
							if (message.getObjContents().get(1) != null)
							{
								
								String groupname = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(0), (byte[])message.getObjContents().get(2)));
								
								UserToken yourToken = (UserToken)convertToObject(decryptObjects((byte[])message.getObjContents().get(1), (byte[])message.getObjContents().get(2)));
								
								boolean checkToken = yourToken.RSAVerifySignature("SHA1withRSA", "BC", publicKey);
								if(checkToken)
								{																	
									if (deleteGroup(groupname, yourToken))
									{
										response = new Envelope("OK"); // Success
										yourToken.getGroups().remove(groupname); // remove the group from the users token
										yourToken.generateRSASignature("SHA1withRSA", "BC", privateKey);
										
										response = encryptResponseWithSymmetricKey(new Object[]{yourToken}, "OK");
									}
								}
								else
								{
									System.out.println("Token not authenticated.");
								}
							}
						}// end if block
					}// end else block
					output.writeObject(response);
				}// end else if block
				else if (message.getMessage().equals("LMEMBERS")) // Client wants a list of members in a group
				{
					/* TODO: Write this handler */
					if (message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");
						
						if (message.getObjContents().get(0) != null)
						{
							if (message.getObjContents().get(1) != null)
							{
								String groupname = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(0), (byte[])message.getObjContents().get(2)));
								
								UserToken yourToken = (UserToken)convertToObject(decryptObjects((byte[])message.getObjContents().get(1), (byte[])message.getObjContents().get(2)));
								
								boolean checkToken = yourToken.RSAVerifySignature("SHA1withRSA", "BC", publicKey);
								
								if(checkToken){
								
								List<String> members = listMembers(groupname, yourToken);
								
									if (members != null)
									{
										response = new Envelope("OK"); // Success
										
										response = encryptResponseWithSymmetricKey(new Object[]{members, new Integer(members.size()) }, "OK");
									}
								}
								else
								{
									System.out.println("Token not authenticated.");
								}
							}
						}// end if block
					}// end else block
					output.writeObject(response);
				}// end else if block
				else if (message.getMessage().equals("AUSERTOGROUP")) // Client wants to add user to a group
				{
					/* TODO: Write this handler */
					if (message.getObjContents().size() < 3)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");
						
						if (message.getObjContents().get(0) != null)
						{
							if (message.getObjContents().get(1) != null)
							{
								if (message.getObjContents().get(2) != null)
								{
									String username = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(0), (byte[])message.getObjContents().get(3)));
									String groupname = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(1), (byte[])message.getObjContents().get(3)));
									
									UserToken yourToken = (UserToken)convertToObject(decryptObjects((byte[])message.getObjContents().get(2), (byte[])message.getObjContents().get(3)));
									
									boolean checkToken = yourToken.RSAVerifySignature("SHA1withRSA", "BC", publicKey);
									
									if(checkToken)
									{
										if (addUserToGroup(username, groupname, yourToken))
										{
											response = new Envelope("OK"); // Success
											// Add the group name for the user in the token
											if (username.equals(yourToken.getSubject()))
											{
												yourToken.getGroups().add(groupname);
											}
											yourToken.generateRSASignature("SHA1withRSA", "BC", privateKey);
											response = encryptResponseWithSymmetricKey(new Object[]{yourToken}, "OK");
										}
									}
									else
									{
										System.out.println("Token not authenticated.");
									}
								}// end if block
							}// end if block
						}// end if block
					}// end else block
					output.writeObject(response);
				}// end else if block
				else if (message.getMessage().equals("RUSERFROMGROUP")) // Client wants to remove user from a group
				{
					/* TODO: Write this handler */
					if (message.getObjContents().size() < 3)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");
						
						if (message.getObjContents().get(0) != null)
						{
							if (message.getObjContents().get(1) != null)
							{
								if (message.getObjContents().get(2) != null)
								{
									String username = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(0), (byte[])message.getObjContents().get(3)));
									String groupname = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(1), (byte[])message.getObjContents().get(3)));
									
									UserToken yourToken = (UserToken)convertToObject(decryptObjects((byte[])message.getObjContents().get(2), (byte[])message.getObjContents().get(3)));
									
									boolean checkToken = yourToken.RSAVerifySignature("SHA1withRSA", "BC", publicKey);
									
									if(checkToken){
									
										if (deleteUserFromGroup(username, groupname, yourToken))
										{
											response = new Envelope("OK"); // Success
											// Remove the groupname from the user in the token
											yourToken.getGroups().remove(groupname);
											yourToken.generateRSASignature("SHA1withRSA", "BC", privateKey);
											response = encryptResponseWithSymmetricKey(new Object[]{yourToken}, "OK");
										}
									}
									else
									{
										System.out.println("Token not authenticated.");
									}
								}// end if block
							}// end if block
						}// end if block
					}// end else block
					output.writeObject(response);
				}// end else if block
				else if (message.getMessage().equals("DISCONNECT")) // Client wants to disconnect
				{
					socket.close(); // Close the socket
					proceed = false; // End this communication loop
				}
				else
				{
					response = new Envelope("FAIL"); // Server does not understand client request
					output.writeObject(response);
				}
//				output.flush();
			} while (proceed);
		}// end try block
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}// end method run()
	
	/**
	 * Creates a valid {@link UserToken}.
	 * 
	 * @param username The String representing the username.
	 * @return Returns a valid UserToken
	 */
	// TODO: password support
	private UserToken createToken(String username)
	{
		// Check that user exists
		if (my_gs.userList.checkUser(username))
		{
			// Issue a new token with server's name, user's name, and user's groups
			UserToken yourToken = new Token(my_gs.name, username, my_gs.userList.getUserGroups(username));
			yourToken.generateRSASignature("SHA1withRSA", "BC", privateKey);
			return yourToken;
		}
		else
		{
			return null;
		}
	}// end method createToken(String)
	
	// TODO: update with password
	/**
	 * This method will create a new user in the group server.
	 * 
	 * @param username The username to add to the group server.
	 * @param yourToken The token of the requester.
	 * @return Returns a boolean value indicating if the user was created. Will return true if the user was created,
	 *         false if the username already exists, false if the requester is not an admin, and false if the requester
	 *         does not exist. False if the password does match the username
	 */
	private boolean createUser(String username, UserToken yourToken, String password)
	{
		String requester = yourToken.getSubject();
		
		// Check if requester exists
		if (my_gs.userList.checkUser(requester))
		{
			// Get the user's groups
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			// requester needs to be an administrator
			if (temp.contains(ADMIN_GROUP_NAME))
			{
				// Does user already exist?
				if (my_gs.userList.checkUser(username))
				{
					return false; // User already exists
				}
				else
				{
					my_gs.userList.addUser(username, password);
					return true;
				}
			}// end if block
			else
			{
				return false; // requester not an administrator
			}
		}// end if block
		else
		{
			return false; // requester does not exist
		}
	}// end method createUser(String, UserToken)
	
	private boolean checkPassword(String username, String password)
	{
		return my_gs.userList.checkPassword(username, password);
	}
	
	/**
	 * This method will delete a user from the group server.
	 * 
	 * @param username The username to create.
	 * @param yourToken The token of the requester.
	 * @return Returns a boolean value indicating if the user was deleted. Will return true if the user was deleted,
	 *         false if the username already exists, false if the requester is not an admin, and false if the requester does not exist.
	 */
	private boolean deleteUser(String username, UserToken yourToken)
	{
		String requester = yourToken.getSubject();
		
		// Make sure that the user cannot delete him or herself.
		if (requester.equals(username))
		{
			return false;
		}
		
		// Does requester exist?
		if (my_gs.userList.checkUser(requester))
		{
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			// requester needs to be an administer
			if (temp.contains(ADMIN_GROUP_NAME))
			{
				// Does user exist?
				if (!yourToken.getSubject().equals(username) && my_gs.userList.checkUser(username))
				{
					// User needs deleted from the groups they belong
					ArrayList<String> deleteFromGroups = new ArrayList<String>();
					
					// This will produce a hard copy of the list of groups this user belongs
					for (int index = 0; index < my_gs.userList.getUserGroups(username).size(); index++ )
					{
						deleteFromGroups.add(my_gs.userList.getUserGroups(username).get(index));
					}
					
					// Delete the user from the groups
					// If user is the owner, removeMember will automatically delete group!
					for (int index = 0; index < deleteFromGroups.size(); index++ )
					{
						// -- old implementation -- my_gs.groupList.removeMember(username, deleteFromGroups.get(index));
						my_gs.groupList.removeUser(deleteFromGroups.get(index), username);
					}
					
					// If groups are owned, they must be deleted
					ArrayList<String> deleteOwnedGroup = new ArrayList<String>();
					
					// Make a hard copy of the user's ownership list
					for (int index = 0; index < my_gs.userList.getUserOwnership(username).size(); index++ )
					{
						deleteOwnedGroup.add(my_gs.userList.getUserOwnership(username).get(index));
					}
					
					// Delete owned groups
					for (int index = 0; index < deleteOwnedGroup.size(); index++ )
					{
						// Use the delete group method. Token must be created for this action
						Token newToken = new Token(my_gs.name, username, deleteOwnedGroup);
						newToken.generateRSASignature("SHA1withRSA", "BC", privateKey);
						deleteGroup(deleteOwnedGroup.get(index), newToken);
					}
					
					// Delete the user from the user list
					my_gs.userList.deleteUser(username);
					
					return true;
				}// end if block
				else
				{
					return false; // User does not exist
				}
			}// end if block
			else
			{
				return false; // requester is not an administer
			}
		}// end if block
		else
		{
			return false; // requester does not exist
		}
	}// end method deleteUser(String, UserToken)
	
	// TODO
	/**
	 * Will create a group for a user.
	 * Any user can create a group.
	 * 
	 * @param groupname The String representing the group's name.
	 * @param token The UserToken from the requesting user.
	 * @return Returns a boolean depending on whether or not the group creation was successful.
	 */
	private boolean createGroup(String groupname, UserToken token)
	{
		String requester = token.getSubject();
		
		// Check if requester exists
		if (my_gs.userList.checkUser(requester))
		{
			// Does group already exist?
			if (my_gs.groupList.checkGroup(groupname))
			{
				return false; // Group already exists
			}
			else
			{
				// Add group to group list
				my_gs.groupList.addGroup(groupname, requester);
				my_gs.groupList.addUser(groupname, requester);
				// Add group and owner to user list
				my_gs.userList.addGroup(requester, groupname);
				my_gs.userList.addOwnership(requester, groupname);
				return true;
			}
		}// end if block
		return false; // requester does not exist
	}// end method createGroup(String, UserToken)
	
	// TODO
	/**
	 * Will delete a group if the user is an admin or owner of the group as specified by UserToken.
	 * 
	 * @param groupname The String representing the group's name.
	 * @param token The UserToken from the requesting user.
	 * @return Returns a boolean depending on whether or not the group deletion was successful.
	 */
	private boolean deleteGroup(String groupname, UserToken token)
	{
		String requester = token.getSubject();
		
		// Does requester exist?
		if (my_gs.userList.checkUser(requester) && my_gs.groupList.checkGroup(groupname))
		{
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			
			// Get the owner of the group
			String ownerOfGroup = my_gs.groupList.getGroupOwner(groupname);
			
			// Requester needs to be an administer or owner of the group
			if (temp.contains(ADMIN_GROUP_NAME) || ownerOfGroup.equals(requester))
			{
				// (1)
				ArrayList<String> usersInGroup = my_gs.groupList.getGroupUsers(groupname);
				
				// (2)
				my_gs.groupList.deleteGroup(groupname);
				
				// (3)
				for (String user : usersInGroup)
				{
					my_gs.userList.removeGroup(user, groupname);
				}
				return true;
			}// end if block
			else
			{
				return false; // requester not an administrator
			}
		}// end if block
		return false; // requester does not exist
	}// end method deleteGroup(String, UserToken)
	
	// TODO
	/**
	 * Adds a user to a group.
	 * 
	 * @param user The String representing the user to add to the group.
	 * @param groupname The String representing the group that the user should be added to.
	 * @param token The UserToken from the requesting user.
	 * @return True if user was added to the group, false if not.
	 */
	private boolean addUserToGroup(String user, String group, UserToken token)
	{
		String requester = token.getSubject();
		
		// Does requester exist and does group exist.
		if (my_gs.userList.checkUser(requester))
		{
			if (my_gs.userList.checkUser(user) && my_gs.groupList.checkGroup(group))
			{
				// Get the list of groups of the requester
				ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
				
				// Get the owner of the group we are trying to add the user to
				String ownerOfGroup = my_gs.groupList.getGroupOwner(group);
				
				// Requester needs to be an administer or owner of the group to add a user to a group
				if (temp.contains(ADMIN_GROUP_NAME) || ownerOfGroup.equals(requester))
				{
					// Make sure that the user is not already in the group
					if (my_gs.userList.getUserGroups(user).contains(group))
					{
						return false; // user already exists in group
					}

					// add group to the user
					my_gs.userList.addGroup(user, group);
					// add the user the group in group list
					return my_gs.groupList.addUser(group, user);
				}
			}// end if block
		}// end if block
		return false; // requester does not exist
	}// end method addUserToGroup(String, String, UserToken)
	
	// Note: does not alter user token here
	// TODO
	/**
	 * Deletes a user from a group.
	 * 
	 * @param user The String representing the user to add to the group.
	 * @param groupname The String representing the group that the user should be added to.
	 * @param token The UserToken from the requesting user.
	 * @return True if user was deleted from the group, false if not.
	 */
	private boolean deleteUserFromGroup(String user, String group, UserToken token)
	{
		// user does not exist, group does not exist, or token user is not an owner of the group or admin, or if the user is not part of the group
		if (!my_gs.userList.checkUser(user) || !my_gs.groupList.checkGroup(group) || !(my_gs.userList.getUserOwnership(token.getSubject()).contains(group) || my_gs.groupList.getGroupUsers(ADMIN_GROUP_NAME).contains(token.getSubject())) || !my_gs.groupList.getGroupUsers(group).contains(user))
		{
			return false;
		}
		my_gs.userList.removeGroup(user, group);
		return my_gs.groupList.removeUser(group, user);
	}
	
	// TODO
	/**
	 * Will return a list of members of a group if the user is an admin or owner of the group.
	 * 
	 * @param group The String representing the group.
	 * @param yourToken The UserToken from the requesting user.
	 * @return Returns a list of members of the group.
	 */
	private List<String> listMembers(String groupname, UserToken token)
	{
		String requester = token.getSubject();
		
		// Check if group exists
		if (my_gs.groupList.checkGroup(groupname))
		{
			// Get the owner of the group
			String ownerOfGroup = my_gs.groupList.getGroupOwner(groupname);
			
			// ****** NOTE *******
			// requester needs to be an administer or owner of the group to see list of members
			if (ownerOfGroup.equals(requester) || my_gs.groupList.getGroupUsers(ADMIN_GROUP_NAME).contains(requester))
			{
				return my_gs.groupList.getGroupUsers(groupname);
			}
		}
		return null;
	}// end method listMembers(String, UserToken)
}// end class GroupThread
