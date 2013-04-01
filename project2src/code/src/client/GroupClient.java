package client;

/* Implements the GroupClient Interface */

import java.util.List;

import message.Envelope;
import message.UserToken;

/**
 * Handles connections to the {@link GroupServer}.
 * Note however that the GroupClient does not know who it is connecting to, 
 * but will assume that the server understands the protocol.
 */
public class GroupClient extends Client implements GroupInterface, ClientInterface
{
	public UserToken getToken(String username, String password)
	{
		try
		{
//			Envelope message = null, response = null;

			// Tell the server to return a token.
//			message = encryptMessageWithSymmetricKey(new Object[]{username, password}, "GET");
			output.writeObject(encryptMessageWithSymmetricKey(new Object[]{username, password}, "GET"));

			// Get the response from the server
			Envelope response = (Envelope)input.readObject();

			// Successful response
			if (response.getMessage().equals("OK"))
			{
				return (UserToken)convertToObject(decryptObjectBytes((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
			}// end if block
			return null;
		}// end try block
		catch (Exception e)
		{
//			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}// end method getToken(String)

	// TODO: finish adding password support
	public boolean createUser(String username, UserToken token, String password)
	{
		try
		{
//			Envelope message = null, response = null;

//			message = encryptMessageWithSymmetricKey(new Object[]{username, password, token}, "CUSER");
			output.writeObject(encryptMessageWithSymmetricKey(new Object[]{username, password, token}, "CUSER"));

			Envelope response = (Envelope)input.readObject();

			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return true;
			}
			return false;
		}// end try block
		catch (Exception e)
		{
//			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}// end method createUser(String, UserToken)

	public boolean deleteUser(String username, UserToken token)
	{
		try
		{
//			Envelope message = null, response = null;

//			message = encryptMessageWithSymmetricKey(new Object[]{username, token}, "DUSER");
			output.writeObject(encryptMessageWithSymmetricKey(new Object[]{username, token}, "DUSER"));

			Envelope response = (Envelope)input.readObject();

			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return true;
			}
			return false;
		}// end block try
		catch (Exception e)
		{
//			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}// end method deleteUser(String, UserToken)

	public UserToken createGroup(String groupname, UserToken token)
	{
		try
		{
//			Envelope message = null, response = null;

//			message = encryptMessageWithSymmetricKey(new Object[]{groupname, token}, "CGROUP");
			output.writeObject(encryptMessageWithSymmetricKey(new Object[]{groupname, token}, "CGROUP"));

			Envelope response = (Envelope)input.readObject();

			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return (UserToken)convertToObject(decryptObjectBytes((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
			}
			return null;
		}// end block try
		catch (Exception e)
		{
//			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}// end method createGroup(String, UserToken)

	public UserToken deleteGroup(String groupname, UserToken token)
	{
		try
		{
//			Envelope message = null, response = null;

//			message = encryptMessageWithSymmetricKey(new Object[]{groupname, token}, "DGROUP");
			output.writeObject(encryptMessageWithSymmetricKey(new Object[]{groupname, token}, "DGROUP"));

			Envelope response = (Envelope)input.readObject();
			
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return (UserToken)convertToObject(decryptObjectBytes((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
			}
			return null;
		}// end try block
		catch (Exception e)
		{
//			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}// end method deleteGroup(String, UserToken)

	@SuppressWarnings("unchecked")
	public List<String> listMembers(String group, UserToken token)
	{
		try
		{
//			Envelope message = null, response = null;

//			message = encryptMessageWithSymmetricKey(new Object[]{group, token}, "LMEMBERS");
			output.writeObject(encryptMessageWithSymmetricKey(new Object[]{group, token}, "LMEMBERS"));

			Envelope response = (Envelope)input.readObject();

			// If server indicates success, return the member list
			if (response.getMessage().equals("OK"))
			{
				return (List<String>)convertToObject(decryptObjectBytes((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(2)));
			}
			return null;
		}// end try block
		catch (Exception e)
		{
//			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}// end method listMembers(String, UserToken)

	public UserToken addUserToGroup(String username, String groupname, UserToken token)
	{
		try
		{
//			Envelope message = null, response = null;

//			message = encryptMessageWithSymmetricKey(new Object[]{username, groupname,  token}, "AUSERTOGROUP");
			output.writeObject(encryptMessageWithSymmetricKey(new Object[]{username, groupname,  token}, "AUSERTOGROUP"));

			Envelope response = (Envelope)input.readObject();
			
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return (UserToken)convertToObject(decryptObjectBytes((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
			}
			return null;
		}// end block try
		catch (Exception e)
		{
//			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}// end method addUserToGrpu(String, String, UserToken)

	public UserToken deleteUserFromGroup(String username, String groupname, UserToken token)
	{
		try
		{
//			Envelope message = null, response = null;

//			message = encryptMessageWithSymmetricKey(new Object[]{username, groupname,  token}, "RUSERFROMGROUP");
			output.writeObject(encryptMessageWithSymmetricKey(new Object[]{username, groupname,  token}, "RUSERFROMGROUP"));

			Envelope response = (Envelope)input.readObject();
			
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return (UserToken)convertToObject(decryptObjectBytes((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
			}
			return null;
		}// end block try
		catch (Exception e)
		{
//			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}// end method deleteUserFromGroup(String, String, UserToken)
}// end class GroupClient
