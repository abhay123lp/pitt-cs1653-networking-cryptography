package client.group;

/* Implements the GroupClient Interface */

import java.security.Key;
import java.util.ArrayList;
import java.util.List;

import client.Client;
import client.ClientInterface;

import message.Envelope;
import message.Field;
import message.GroupKeysMap;
import message.UserToken;

/**
 * Handles connections to the {@link GroupServer}.
 * Note however that the GroupClient does not know who it is connecting to, 
 * but will assume that the server understands the protocol.
 */
public class GroupClient extends Client implements GroupInterface, ClientInterface
{
	ArrayList<GroupKeysMap> keyTable = null;
	
	@SuppressWarnings("unchecked")
	public UserToken getToken(String username, String password)
	{
		try
		{
			// Tell the server to return a token.
			output.writeObject(encryptMessageWithSymmetricKey("GET", null, new Object[]{username, password}));

			// Get the response from the server
			Envelope response = (Envelope)input.readObject();
			if(!checkValidityOfMessage(response))
			{
				return null;
			}
			
			// Successful response
			if (response.getMessage().equals("OK"))
			{
				Object[] objData = (Object[])getFromEnvelope(Field.DATA);
				keyTable = (ArrayList<GroupKeysMap>)objData[0];	
				return (UserToken)getFromEnvelope(Field.TOKEN);
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
	
	public UserToken getToken(UserToken groupToken, String fileServerName, String ipAddress, int portNumber)
	{
		try
		{
			// Tell the server to return a token.
			output.writeObject(encryptMessageWithSymmetricKey("GET", groupToken, new Object[]{fileServerName, ipAddress, portNumber}));
			
			// Get the response from the server
			Envelope response = (Envelope)input.readObject();
			if(!checkValidityOfMessage(response))
			{
				return null;
			}
			
			// Successful response
			if (response.getMessage().equals("OK"))
			{
				return (UserToken)getFromEnvelope(Field.TOKEN);
			}// end if block
			return null;
		}// end try block
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}// end method getToken(String)
		
	public boolean createUser(String username, UserToken token, String password)
	{
		try
		{
			output.writeObject(encryptMessageWithSymmetricKey("CUSER", token, new Object[]{username, password}));

			Envelope response = (Envelope)input.readObject();
			if(!checkValidityOfMessage(response))
			{
				return false;
			}

			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return true;
			}
			return false;
		}// end try block
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			return false;
		}
	}// end method createUser(String, UserToken)

	@SuppressWarnings("unchecked")
	public boolean deleteUser(String username, UserToken token)
	{
		try
		{
			output.writeObject(encryptMessageWithSymmetricKey("DUSER", token, new Object[]{username}));
			
			Envelope response = (Envelope)input.readObject();
			if(!checkValidityOfMessage(response))
			{
				return false;
			}

			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				Object[] objData = (Object[])getFromEnvelope(Field.DATA);
				keyTable = (ArrayList<GroupKeysMap>)objData[0];
				return true;
			}
			return false;
		}// end block try
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			return false;
		}
	}// end method deleteUser(String, UserToken)

	@SuppressWarnings("unchecked")
	public UserToken createGroup(String groupname, UserToken token)
	{
		try
		{
			output.writeObject(encryptMessageWithSymmetricKey("CGROUP", token, new Object[]{groupname}));

			Envelope response = (Envelope)input.readObject();
			if(!checkValidityOfMessage(response))
			{
				return null;
			}

			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				Object[] objData = (Object[])getFromEnvelope(Field.DATA);
				keyTable = (ArrayList<GroupKeysMap>)objData[0];
				return (UserToken)getFromEnvelope(Field.TOKEN);
			}
			return null;
		}// end block try
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			return null;
		}
	}// end method createGroup(String, UserToken)

	@SuppressWarnings("unchecked")
	public UserToken deleteGroup(String groupname, UserToken token)
	{
		try
		{
			output.writeObject(encryptMessageWithSymmetricKey("DGROUP", token, new Object[]{groupname}));

			Envelope response = (Envelope)input.readObject();
			if(!checkValidityOfMessage(response))
			{
				return null;
			}
			
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				Object[] objData = (Object[])getFromEnvelope(Field.DATA);
				keyTable = (ArrayList<GroupKeysMap>)objData[0];
				return (UserToken)getFromEnvelope(Field.TOKEN);
						//(UserToken)convertToObject(decryptObjectBytes((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
			}
			return null;
		}// end try block
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			return null;
		}
	}// end method deleteGroup(String, UserToken)

	@SuppressWarnings("unchecked")
	public List<String> listMembers(String group, UserToken token)
	{
		try
		{
			output.writeObject(encryptMessageWithSymmetricKey("LMEMBERS", token, new Object[]{group}));

			Envelope response = (Envelope)input.readObject();
			if(!checkValidityOfMessage(response))
			{
				return null;
			}

			// If server indicates success, return the member list
			if (response.getMessage().equals("OK"))
			{
				Object[] objData = (Object[])getFromEnvelope(Field.DATA);
				return (List<String>)objData[0];
			}
			return null;
		}// end try block
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			return null;
		}
	}// end method listMembers(String, UserToken)

	@SuppressWarnings("unchecked")
	public UserToken addUserToGroup(String username, String groupname, UserToken token)
	{
		try
		{
			output.writeObject(encryptMessageWithSymmetricKey("AUSERTOGROUP", token, new Object[]{username, groupname}));

			Envelope response = (Envelope)input.readObject();
			if(!checkValidityOfMessage(response))
			{
				return null;
			}
			
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				Object[] objData = (Object[])getFromEnvelope(Field.DATA);
				keyTable = (ArrayList<GroupKeysMap>)objData[0];
				return (UserToken)getFromEnvelope(Field.TOKEN);
			}
			return null;
		}// end block try
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			return null;
		}
	}// end method addUserToGrpu(String, String, UserToken)

	@SuppressWarnings("unchecked")
	public UserToken deleteUserFromGroup(String username, String groupname, UserToken token)
	{
		try
		{
			output.writeObject(encryptMessageWithSymmetricKey("RUSERFROMGROUP", token, new Object[]{username, groupname}));

			Envelope response = (Envelope)input.readObject();
			if(!checkValidityOfMessage(response))
			{
				return null;
			}
			
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				Object[] objData = (Object[])getFromEnvelope(Field.DATA);
				keyTable = (ArrayList<GroupKeysMap>)objData[0];
				return (UserToken)getFromEnvelope(Field.TOKEN);
			}
			return null;
		}// end block try
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			return null;
		}
	}// end method deleteUserFromGroup(String, String, UserToken)	
	
	/**
	 * 
	 * @param groupname This is the group the client (UserCommands) wants to UPLOAD to.
	 * @return The current key (latest epoch) is returned so the user can use symmetric encryption to
	 * 			encrypt the file before uploading the encrpyted file to the file server.
	 */
	public Key getEncryptionKey(String groupname)
	{
		if(keyTable == null)
		{
			System.out.println("The keyTable is null. There is no encryption key to return.");
			return null;
		}
		else
		{			
			for(int i = 0; i < keyTable.size(); i++){
				if(keyTable.get(i).checkGroupName(groupname)){
					// return the last key in the last index of the array of keys
					// because index == epoch number
					return keyTable.get(i).getLastKey();					
				}
			}
			return null;
		}
	}
	
	/**
	 * 
	 * @param groupname This is the group the client wants to download from.
	 * @param epoch This is the epoch number of the file being downloaded. The file 
	 * 		epoch is part of the metadata.
	 * @return We return the key to decrypt the current file based on the groupname 
	 * 		and epoch of the file's metadata.
	 */
	public Key getKey(String groupname, int epoch)
	{
		if(keyTable == null)
		{
			System.out.println("The keyTable is null. There is no decryption key to return.");
			return null;
		}
		else
		{
			for(int i = 0; i < keyTable.size(); i++){
				if(keyTable.get(i).checkGroupName(groupname)){
					return keyTable.get(i).getKey(epoch);
				}
			}
			return null;
		}
	}
	
	/**
	 * 
	 * @param groupname The groupname the user requests
	 * @return Return the last epoch of the ArrayList for the user's requested groupname
	 */
	public int getEpoch(String groupname)
	{
		if(keyTable == null)
		{
			System.out.println("The keyTable is null. There is no epoch to return.");
			return -1;
		}
		else
		{
			for(int i = 0; i < keyTable.size(); i++){
				if(keyTable.get(i).checkGroupName(groupname)){
					return keyTable.get(i).getEpoch();
				}
			}
			return 0;
		}
	}
}// end class GroupClient
