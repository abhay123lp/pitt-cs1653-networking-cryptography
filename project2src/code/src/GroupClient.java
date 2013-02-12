/* Implements the GroupClient Interface */

import java.util.ArrayList;
import java.util.List;
import java.io.ObjectInputStream;

/**
 * Handles connections to the {@link GroupServer}.
 * Note however that the GroupClient does not know who it is connecting to, but will assume that the server understands the protocol.
 */
public class GroupClient extends Client implements GroupInterface, ClientInterface
{
	public UserToken getToken(String username)
	{
		try
		{
			UserToken token = null;
			Envelope message = null, response = null;
			
			// Tell the server to return a token.
			message = new Envelope("GET");
			message.addObject(username); // Add user name string
			output.writeObject(message);
			
			// Get the response from the server
			response = (Envelope)input.readObject();
			
			// Successful response
			if (response.getMessage().equals("OK"))
			{
				// If there is a token in the Envelope, return it
				ArrayList<Object> temp = null;
				temp = response.getObjContents();
				
				if (temp.size() == 1)
				{
					token = (UserToken)temp.get(0);
					return token;
				}
			}// end if block
			return null;
		}// end try block
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}// end method getToken(String)
	
	public boolean createUser(String username, UserToken token)
	{
		try
		{
			Envelope message = null, response = null;
			// Tell the server to create a user
			message = new Envelope("CUSER");
			message.addObject(username); // Add user name string
			message.addObject(token); // Add the requester's token
			output.writeObject(message);
			
			response = (Envelope)input.readObject();
			
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return true;
			}
			
			return false;
		}// end try block
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}// end method createUser(String, UserToken)
	
	public boolean deleteUser(String username, UserToken token)
	{
		try
		{
			Envelope message = null, response = null;
			
			// Tell the server to delete a user
			message = new Envelope("DUSER");
			message.addObject(username); // Add user name
			message.addObject(token); // Add requester's token
			output.writeObject(message);
			
			response = (Envelope)input.readObject();
			
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return true;
			}
			
			return false;
		}// end block try
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}// end method deleteUser(String, UserToken)
	
	public UserToken createGroup(String groupname, UserToken token)
	{
		try
		{
			Envelope message = null, response = null;
			// Tell the server to create a group
			message = new Envelope("CGROUP");
			message.addObject(groupname); // Add the group name string
			message.addObject(token); // Add the requester's token
			output.writeObject(message);
			
			response = (Envelope)input.readObject();
			
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return (UserToken)response.getObjContents().get(0);
			}
			
			return null;
		}// end block try
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}// end method createGroup(String, UserToken)
	
	public UserToken deleteGroup(String groupname, UserToken token)
	{
		try
		{
			Envelope message = null, response = null;
			// Tell the server to delete a group
			message = new Envelope("DGROUP");
			message.addObject(groupname); // Add group name string
			message.addObject(token); // Add requester's token
			output.writeObject(message);
			
			response = (Envelope)input.readObject();
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return (UserToken)response.getObjContents().get(0);
			}
			
			return null;
		}// end try block
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}// end method deleteGroup(String, UserToken)
	
	@SuppressWarnings("unchecked")
	public List<String> listMembers(String group, UserToken token)
	{
		try
		{
			Envelope message = null, response = null;
			// Tell the server to return the member list
			message = new Envelope("LMEMBERS");
			message.addObject(group); // Add group name string
			message.addObject(token); // Add requester's token
			output.writeObject(message);
//			output.flush();
			
			response = (Envelope)input.readObject();
//			input.skip(input.available());
			
			// If server indicates success, return the member list
			if (response.getMessage().equals("OK"))
			{
				System.out.print(Integer.valueOf((Integer)response.getObjContents().get(1)));
				return (List<String>)response.getObjContents().get(0); // This cast creates compiler warnings. Sorry.
			}
			
			return null;
		}// end try block
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}// end method listMembers(String, UserToken)
	
	public UserToken addUserToGroup(String username, String groupname, UserToken token)
	{
		try
		{
			Envelope message = null, response = null;
			// Tell the server to add a user to the group
			message = new Envelope("AUSERTOGROUP");
			message.addObject(username); // Add user name string
			message.addObject(groupname); // Add group name string
			message.addObject(token); // Add requester's token
			output.writeObject(message);
			
			response = (Envelope)input.readObject();
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return (UserToken)response.getObjContents().get(0);
			}
			
			return null;
		}// end block try
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}// end method addUserToGrpu(String, String, UserToken)
	
	public UserToken deleteUserFromGroup(String username, String groupname, UserToken token)
	{
		try
		{
			Envelope message = null, response = null;
			// Tell the server to remove a user from the group
			message = new Envelope("RUSERFROMGROUP");
			message.addObject(username); // Add user name string
			message.addObject(groupname); // Add group name string
			message.addObject(token); // Add requester's token
			output.writeObject(message);
			
			response = (Envelope)input.readObject();
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return (UserToken)response.getObjContents().get(0);
			}
			
			return null;
		}// end block try
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}// end method deleteUserFromGroup(String, String, UserToken)
}// end class GroupClient
