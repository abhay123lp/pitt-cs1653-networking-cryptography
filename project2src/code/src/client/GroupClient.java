package client;

/* Implements the GroupClient Interface */

import java.security.Key;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import message.Envelope;
import message.UserToken;

/**
 * Handles connections to the {@link GroupServer}.
 * Note however that the GroupClient does not know who it is connecting to, 
 * but will assume that the server understands the protocol.
 */
public class GroupClient extends Client implements GroupInterface, ClientInterface {

//	private final String PROVIDER = "BC";
//	private final String ASYM_ALGORITHM = "RSA";
	
	private RSAPublicKey publicKey;
	private RSAPrivateKey privateKey; 
	
//	private SecretKey SYMMETRIC_KEY; 
//	private final String SYM_KEY_ALG = "AES/CTR/NoPadding";
	
	private static final int IV_BYTES = 16;


	private static IvParameterSpec ivAES(int ivBytes){

		// Random Number used for IV
		SecureRandom randomNumber = new SecureRandom();

		byte[] bytesIV = new byte[ivBytes];	
		randomNumber.nextBytes(bytesIV);

		// Create the IV
		return new IvParameterSpec(bytesIV);

	}
	
	private  Envelope encryptResponseWithSymmetricKey(Object[] objs, String message){
		
		try{
			
		Envelope response = new Envelope(message);
		
		Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);
					
		IvParameterSpec IV = ivAES(IV_BYTES);
		
		// Initialize the cipher encryption object, add the key, and add the IV
		objCipher.init(Cipher.ENCRYPT_MODE, SYMMETRIC_KEY, IV); 

		//byte[] dataToEncryptBytes = dataToEncrypt.getBytes();

		for(Object o : objs){
			
			byte[] newEncryptedChallenge = objCipher.doFinal(convertToByteArray(o));	
			response.addObject(newEncryptedChallenge);
			
		}
		
		// Encrypt the data and store in encryptedData
		//byte[] newEncryptedChallenge = objCipher.doFinal(decryptedChallenge);
														
		// Respond to the client. On error, the client will receive a null token
		//response = new Envelope("OK");
		
		//response.addObject(newEncryptedChallenge);
		response.addObject(IV.getIV());
		
        //return b.toByteArray(); 
//		byte[] byteArray = convertToByteArray(response);
																
		return response;
		
		//output.writeObject(response);
		
		} catch(Exception e){
			
			e.printStackTrace();
			
		}
		
		
		return null;
		
	}
	
	private byte[] decryptObjects(byte[] objByte, byte[] iv)  {
		
		try{

			Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);

			// Initialize the cipher encryption object, add the key, and add the IV
			objCipher.init(Cipher.DECRYPT_MODE, SYMMETRIC_KEY, new IvParameterSpec(iv)); 

			// Encrypt the data and store in encryptedData
			return objCipher.doFinal(objByte);

		} catch(Exception ex){
			System.out.println(ex.toString());
			ex.printStackTrace();
		}

		return null;
				
		
	}
	
	
private static byte[] convertToByteArray(Object objToConvert){
		
		try{
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    ObjectOutputStream oos = new ObjectOutputStream(baos);
		    oos.writeObject(objToConvert);
		    //return b.toByteArray(); 
			return baos.toByteArray();
		
		} catch(Exception ex){
		
			System.out.println("Error creating byte array envelope: " + ex.toString());
			ex.printStackTrace();
		}
		
		return null;
		
	}
	
	private static Object convertToObject(byte[] bytesToConvert){
		
		try
		{
			
			ByteArrayInputStream bais = new ByteArrayInputStream(bytesToConvert);
	        ObjectInputStream ois = new ObjectInputStream(bais);
	        
	        return ois.readObject();
	        
		} catch(Exception ex){
			
			System.out.println("Error byte array to object: " + ex.toString());
			ex.printStackTrace();
			
		}
		
		return null;
		
	}
	
	
	
	
	
	/**
	 * This method will encrypt data via an AES encryption algorithm utilizing an IV and symmetric key.
	 * @param algorithm The algorithm to use.
	 * @param provider The security provider.
	 * @param key The symmetric key
	 * @param iv The IV used for encryption.
	 * @param dataToEncrypt The clear data to encrypt.
	 * @return byte[] array of encrypted data.
	 */
	private static byte[] AESEncrypt(String algorithm, String provider, Key key, IvParameterSpec iv, byte[] byteData){

		try{

			// Create the cipher object
			Cipher objCipher = Cipher.getInstance(algorithm, provider);

			// Initialize the cipher encryption object, add the key, and add the IV
			objCipher.init(Cipher.ENCRYPT_MODE, key, iv); 
			
			// Encrypt the data and store in encryptedData
			return objCipher.doFinal(byteData);


		} catch(Exception ex){
			System.out.println(ex.toString());
			ex.printStackTrace();
		}

		return null;
	}
	
	
	/**
	 * This method will decrypt the data.
	 * @param algorithm The algorithm to use.
	 * @param provider The security provider.
	 * @param key The symmetric key to use.
	 * @param iv The IV to use for decryption.
	 * @param dataToDecrypt The data to decrypt.
	 * @return byte[] array of decrypted data.
	 */
	private static byte[] AESDecrypt(String algorithm, String provider, Key key, IvParameterSpec iv, byte[] dataToDecrypt){

		try{

			Cipher objCipher = Cipher.getInstance(algorithm, provider);

			// Initialize the cipher encryption object, add the key, and add the IV
			objCipher.init(Cipher.DECRYPT_MODE, key, iv); 

			// Encrypt the data and store in encryptedData
			return objCipher.doFinal(dataToDecrypt);

		} catch(Exception ex){
			System.out.println(ex.toString());
			ex.printStackTrace();
		}

		return null;
	}
	
	public UserToken getToken(String username, String password)
	{
		try
		{
			UserToken token = null;
			Envelope message = null, response = null;
			
			// Tell the server to return a token.
			message = encryptResponseWithSymmetricKey(new Object[]{username, password}, "GET");
			//message = new Envelope("GET");
			//message.addObject(username); // Add user name string
			// Cleartext password from the user UserCommands.java
			//message.addObject(password);
			
			//IvParameterSpec IV = ivAES(IV_BYTES);
			//byte[] byteArray = convertToByteArray(message);
			
			//output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV, byteArray) );
			
			
			output.writeObject(message);
			
			// Get the response from the server
			response = (Envelope)input.readObject();
			
			// Successful response
			if (response.getMessage().equals("OK"))
			{
				
				//String token = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(0), (byte[])message.getObjContents().get(2)));
				
//				Object objectToken = response.getObjContents().get(0);
//				byte[] byteToken = convertToByteArray(objectToken);
				token = (UserToken)convertToObject(decryptObjects((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
				
				
				// If there is a token in the Envelope, return it
				//ArrayList<Object> temp = null;
				//temp = response.getObjContents();
				
				// TODO: password support
				//if (temp.size() == 1)
				//{
				//	token = (UserToken)temp.get(0);
					
					
				//	String groupname = (String)convertToObject(decryptObjects((byte[])message.getObjContents().get(0), (byte[])message.getObjContents().get(2)));
					
				//	UserToken yourToken = (UserToken)convertToObject(decryptObjects((byte[])message.getObjContents().get(1), (byte[])message.getObjContents().get(2)));
					
					
					
					return token;
				//}
			}// end if block
			return null;
		}// end try block
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}// end method getToken(String)
	
	// TODO: finish adding password support
	public boolean createUser(String username, UserToken token, String password)
	{
		try
		{
			Envelope message = null, response = null;
			// Tell the server to create a user
//			message = new Envelope("CUSER");
//			message.addObject(username); // Add user name string
//			message.addObject(token); // Add the requester's token
//			message.addObject(password); // Add the password
			
			message = encryptResponseWithSymmetricKey(new Object[]{username, password, token}, "CUSER");
			
			IvParameterSpec IV = ivAES(IV_BYTES);
			//byte[] byteArray = convertToByteArray(message);
			
			//output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV, byteArray) );
						
			output.writeObject(message);
			
			
			
			//byte[] envelopeBytes = convertToByteArray((Envelope)input.readObject());
			//byte[] decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, envelopeBytes);
			
			//Object convertedObj = convertToObject(decryptedMsg);
			//response = (Envelope)convertedObj;
			
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
			//message = new Envelope("DUSER");
			//message.addObject(username); // Add user name
			//message.addObject(token); // Add requester's token
			
			message = encryptResponseWithSymmetricKey(new Object[]{username, token}, "DUSER");
			
			
						
			//IvParameterSpec IV = ivAES(IV_BYTES);
			//byte[] byteArray = convertToByteArray(message);
			
			//output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV, byteArray) );
			
			output.writeObject(message);
			
			//byte[] envelopeBytes = convertToByteArray((Envelope)input.readObject());
			//byte[] decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, envelopeBytes);
			
			//Object convertedObj = convertToObject(decryptedMsg);
			//response = (Envelope)convertedObj;
			
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
			//message = new Envelope("CGROUP");
			//message.addObject(groupname); // Add the group name string
			//message.addObject(token); // Add the requester's token
			
			message = encryptResponseWithSymmetricKey(new Object[]{groupname, token}, "CGROUP");
			
			//IvParameterSpec IV = ivAES(IV_BYTES);
			//byte[] byteArray = convertToByteArray(message);
			
			//output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV, byteArray) );
			
			output.writeObject(message);
			
			//byte[] envelopeBytes = convertToByteArray((Envelope)input.readObject());
			//byte[] decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, envelopeBytes);
			
			//Object convertedObj = convertToObject(decryptedMsg);
			//response = (Envelope)convertedObj;
			
			response = (Envelope)input.readObject();
			
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				
				return (UserToken)convertToObject(decryptObjects((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
				
				
				
				 //(UserToken)response.getObjContents().get(0);
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
			//message = new Envelope("DGROUP");
			message = encryptResponseWithSymmetricKey(new Object[]{groupname, token}, "DGROUP");
			//message.addObject(groupname); // Add group name string
			//message.addObject(token); // Add requester's token
			//output.writeObject(message);
			
			//IvParameterSpec IV = ivAES(IV_BYTES);
			//byte[] byteArray = convertToByteArray(message);
			
			//output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV, byteArray) );
			
			output.writeObject(message);
			//byte[] envelopeBytes = convertToByteArray((Envelope)input.readObject());
			//byte[] decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, envelopeBytes);
			
			//Object convertedObj = convertToObject(decryptedMsg);
			//response = (Envelope)convertedObj;
			
			
			response = (Envelope)input.readObject();
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				
				return (UserToken)convertToObject(decryptObjects((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
				
				
				//return (UserToken)response.getObjContents().get(0);
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
			//message = new Envelope("LMEMBERS");
			//message.addObject(group); // Add group name string
			//message.addObject(token); // Add requester's token
			
			message = encryptResponseWithSymmetricKey(new Object[]{group, token}, "LMEMBERS");
						
			//IvParameterSpec IV = ivAES(IV_BYTES);
			//byte[] byteArray = convertToByteArray(message);
			
			//output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV, byteArray) );
			
			output.writeObject(message);
//			output.flush();
			
			
		//	byte[] envelopeBytes = convertToByteArray((Envelope)input.readObject());
			//byte[] decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, envelopeBytes);
			
		//	Object convertedObj = convertToObject(decryptedMsg);
			//response = (Envelope)convertedObj;
			
			response = (Envelope)input.readObject();
//			input.skip(input.available());
			
			// If server indicates success, return the member list
			if (response.getMessage().equals("OK"))
			{
				
//				System.out.print(Integer.valueOf(((Integer)convertToObject(decryptObjects((byte[])response.getObjContents().get(1), (byte[])response.getObjContents().get(2))))));
				
				return (List<String>)convertToObject(decryptObjects((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(2)));
				
				
				//System.out.print(Integer.valueOf((Integer)response.getObjContents().get(1)));
			//	return (List<String>)response.getObjContents().get(0); // This cast creates compiler warnings. Sorry.
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
			
			message = encryptResponseWithSymmetricKey(new Object[]{username, groupname,  token}, "AUSERTOGROUP");
			// Tell the server to add a user to the group
			//message = new Envelope("AUSERTOGROUP");
			//message.addObject(username); // Add user name string
			//message.addObject(groupname); // Add group name string
			//message.addObject(token); // Add requester's token
			
			//IvParameterSpec IV = ivAES(IV_BYTES);
			//byte[] byteArray = convertToByteArray(message);
			
			//output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV, byteArray) );
			
			output.writeObject(message);
			
			//byte[] envelopeBytes = convertToByteArray((Envelope)input.readObject());
			//byte[] decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, envelopeBytes);
			
			//Object convertedObj = convertToObject(decryptedMsg);
			//response = (Envelope)convertedObj;
			
			response = (Envelope)input.readObject();
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return (UserToken)convertToObject(decryptObjects((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
				
				//return (UserToken)response.getObjContents().get(0);
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
			//message = new Envelope("RUSERFROMGROUP");
			
			message = encryptResponseWithSymmetricKey(new Object[]{username, groupname,  token}, "RUSERFROMGROUP");
			
		//	message.addObject(username); // Add user name string
		//	message.addObject(groupname); // Add group name string
		//	message.addObject(token); // Add requester's token
			
		//	IvParameterSpec IV = ivAES(IV_BYTES);
		//	byte[] byteArray = convertToByteArray(message);
			
			//output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV, byteArray) );
						
			output.writeObject(message);
			
		//	byte[] envelopeBytes = convertToByteArray((Envelope)input.readObject());
		//	byte[] decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, envelopeBytes);
			
		//	Object convertedObj = convertToObject(decryptedMsg);
		//	response = (Envelope)convertedObj;
			
			response = (Envelope)input.readObject();
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
			{
				return (UserToken)convertToObject(decryptObjects((byte[])response.getObjContents().get(0), (byte[])response.getObjContents().get(1)));
				
				//return (UserToken)response.getObjContents().get(0);
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
