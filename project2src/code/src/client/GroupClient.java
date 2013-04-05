package client;

/* Implements the GroupClient Interface */

import java.security.Key;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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
	
	/**
	 * New for phase 4: use this hash table to look up arrays of keys for a given groupname.
	 * GroupClient needs to store this information for UserCommands.
	 * The index of the an array for a given group in the hash table is the epoch number (used
	 * to mitigate file leakage).
	 */
	//private Hashtable<String, ArrayList<Key>> keyTable = null;	
	
	ArrayList<GroupKeysMap> keyTable = null;
	
	@SuppressWarnings("unchecked")
	public UserToken getToken(String username, String password)
	{
		try
		{
//			Envelope message = null, response = null;

			// Tell the server to return a token.
//			message = encryptMessageWithSymmetricKey(new Object[]{username, password}, "GET");
			output.writeObject(encryptMessageWithSymmetricKey("GET", null, new Object[]{username, password}));
					//encryptMessageWithSymmetricKey(new Object[]{username, password}, "GET"));

			// Get the response from the server
			Envelope response = (Envelope)input.readObject();
			if(!checkValidityOfMessage(response))
			{
				return null;
			}
			
			//keyTable =  (Hashtable<String, ArrayList<Key>>) ((Object[])getFromEnvelope(Field.DATA))[0];
			
			
			// Successful response
			if (response.getMessage().equals("OK"))
			{
				Object[] objData = (Object[])getFromEnvelope(Field.DATA);
				keyTable = (ArrayList<GroupKeysMap>)objData[0];	
				return (UserToken)getFromEnvelope(Field.TOKEN);
						//(UserToken)convertToObject(decryptObjectBytes((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
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
//			Envelope message = null, response = null;

			// Tell the server to return a token.
//			message = encryptMessageWithSymmetricKey(new Object[]{username, password}, "GET");
			output.writeObject(encryptMessageWithSymmetricKey("GET", groupToken, new Object[]{fileServerName, ipAddress, portNumber}));
					//encryptMessageWithSymmetricKey(new Object[]{username, password}, "GET"));
			
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
						//(UserToken)convertToObject(decryptObjectBytes((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
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
		
	
	public boolean createUser(String username, UserToken token, String password)
	{
		try
		{
//			Envelope message = null, response = null;

//			message = encryptMessageWithSymmetricKey(new Object[]{username, password, token}, "CUSER");
			output.writeObject(encryptMessageWithSymmetricKey("CUSER", token, new Object[]{username, password}));
					//encryptMessageWithSymmetricKey(new Object[]{username, password, token}, "CUSER"));

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
			//output.writeObject(encryptMessageWithSymmetricKey(new Object[]{username, token}, "DUSER"));
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
			//output.writeObject(encryptMessageWithSymmetricKey(new Object[]{groupname, token}, "CGROUP"));
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
						//(UserToken)convertToObject(decryptObjectBytes((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
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
			//output.writeObject(encryptMessageWithSymmetricKey(new Object[]{groupname, token}, "DGROUP"));
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
			//output.writeObject(encryptMessageWithSymmetricKey(new Object[]{group, token}, "LMEMBERS"));
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
						//(List<String>)convertToObject(decryptObjectBytes((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(2)));
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
			//output.writeObject(encryptMessageWithSymmetricKey(new Object[]{username, groupname,  token}, "AUSERTOGROUP"));
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
						//(UserToken)convertToObject(decryptObjectBytes((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
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
			//output.writeObject(encryptMessageWithSymmetricKey(new Object[]{username, groupname,  token}, "RUSERFROMGROUP"));
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
						//(UserToken)convertToObject(decryptObjectBytes((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
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
			//TODO UNCOMMENT
		//	ArrayList<Key> keys = new ArrayList<Key>(keyTable.get(groupname));
			
			for(int i = 0; i < keyTable.size(); i++){
				
				if(keyTable.get(i).checkGroupName(groupname)){
					
					return keyTable.get(i).getLastKey();
					
				}
				
			}
			
			// return the last key in the last index of the array of keys
			// because index == epoch number
			//return keys.get(keys.size() - 1);
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
			//TODO UNCOMMENT 
			//ArrayList<Key> keys = new ArrayList<Key>(keyTable.get(groupname));
			// return the last key in the last index of the array of keys
			// because index == epoch number
			//return keys.get(keys.size() - 1);
			
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
			//TODO UNCOMMENT
			//ArrayList<Key> keys = new ArrayList<Key>(keyTable.get(groupname));
			// return the epoch number, which is the last index
			//return keys.size() - 1;
			
			for(int i = 0; i < keyTable.size(); i++){
				
				if(keyTable.get(i).checkGroupName(groupname)){
					
					return keyTable.get(i).getEpoch();
					
				}
				
			}
			
			
			return 0;
		}
	}
	
}// end class GroupClient
