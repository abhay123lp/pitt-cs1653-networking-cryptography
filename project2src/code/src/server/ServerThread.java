/**
 * 
 */
package server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import client.CAClient;

import message.Envelope;
import message.Field;
import message.UserToken;

/**
 * @author ongnathan
 *
 */
public abstract class ServerThread extends Thread
{
	
	
	// Include three fields name of server, ip of server, port of server
	// constructor ask to pass in those parameters, modify constructor, store values in fields
	// Check token validity......
	
	private String serverName;
	private String ipAddress;
	private int portNumber;
		
	protected static final String SYM_KEY_ALG = "AES/CTR/NoPadding";
	protected static final String PROVIDER = "BC";
	protected static final String ASYM_ALGORITHM = "RSA";
	private static final int IV_BYTES = 16;
	private static final int ENV_CONTENTS_SIZE = 5;
	private static final String CA_LOC = "localhost";
	private static final int CA_PORT = 4999;
	private static final String HMAC_ALGORITHM = "HmacSHA1";
	
	private final SecureRandom random;
	
	protected final Socket socket;
	protected final RSAPublicKey publicKey;
	protected final RSAPrivateKey privateKey;
	
	private RSAPublicKey groupServerPublicKey; //FOR USE BY NON GROUP SERVERS ONLY
	private volatile int numberOfMessage;
	private Key confidentialKey;
	private Key integrityKey;
	private byte[] challengeBytes;
	private Object[] lastMessageContents;
	
	/**
	 * 
	 */
	public ServerThread(Socket socket, RSAPublicKey publicKey, RSAPrivateKey privateKey, String sName, String ipAdd, int portNum)
	{
		this.socket = socket;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.random = new SecureRandom();
		this.confidentialKey = null;
		this.groupServerPublicKey = null;
		this.numberOfMessage = 0;
		this.challengeBytes = new byte[20];
		this.lastMessageContents = null;
		this.serverName = sName;
		this.ipAddress = ipAdd;
		this.portNumber = portNum;
	}
	
	public String getServerName(){
		return this.serverName;
	}
	
	public String getIPAddress(){
		return this.ipAddress;
	}
	
	public int getPortNumber(){
		return this.portNumber;
	}
	
	public abstract void run();
	
	protected IvParameterSpec ivAES()
	{
		byte[] bytesIV = new byte[IV_BYTES];	
		this.random.nextBytes(bytesIV);

		// Create the IV
		return new IvParameterSpec(bytesIV);
	}
	
	private void unencryptMessage(Envelope env){
		
		// Decrypt message enveloope store in Object[] lastMessageContents array
		lastMessageContents = new Object[ENV_CONTENTS_SIZE];
		
		// Get the IV we will use for decryption
		byte[] ivByteArray = (byte[])env.getObjContents().get(1);
		lastMessageContents[1] = ivByteArray;
		
		// Get HMAC
		lastMessageContents[0] = decryptObjectBytes((byte[])env.getObjContents().get(0), ivByteArray);
		
		// Get INT
		lastMessageContents[2] = convertToObject(decryptObjectBytes((byte[])env.getObjContents().get(2), ivByteArray));	
		
		// Get Token if it exists
		if(env.getObjContents().get(3) != null){
			lastMessageContents[3] = convertToObject(decryptObjectBytes((byte[])env.getObjContents().get(3), ivByteArray));
		}
		
		// Get Data if it exists
		if(env.getObjContents().get(4) != null){
			
			lastMessageContents[4] = convertToObject(decryptObjectBytes((byte[])env.getObjContents().get(4), ivByteArray));
			
		}
		
		
	}
	
	
	
	/**
	 * 
	 * @param Envelope env
	 * @return
	 */
	protected boolean checkValidityOfMessage(Envelope env){
						
		boolean isHMACValid = true;
		boolean isMsgNumValid = true;
		boolean isTokenValid = true;
		
		// Unencrypt Message Envelope
		unencryptMessage(env);
			
		/* *** Check HMAC *** */
		byte[] HMAC = (byte[])lastMessageContents[0];
				
		//Key integrityKey = genterateSymmetricKey();
		int lengthOfMastArray = 0;
		
		byte[] msgBytes = convertToByteArray(env.getMessage());
		lengthOfMastArray = lengthOfMastArray + msgBytes.length;		
						
		byte[] ivBytes = (byte[])lastMessageContents[1];
		lengthOfMastArray = lengthOfMastArray + ivBytes.length;
		
		byte[] intBytes = convertToByteArray(lastMessageContents[2]);
		lengthOfMastArray = lengthOfMastArray + intBytes.length;
		
		byte[] tokenBytes = null;
		
		if(lastMessageContents[3] != null){
			tokenBytes = convertToByteArray(lastMessageContents[3]);
			lengthOfMastArray = lengthOfMastArray + tokenBytes.length;
		} 
		
		int sizeOfVarLenData = 0;
		
		ArrayList<byte[]> alVarData = new ArrayList<byte[]>();
		
		if(lastMessageContents[4] != null){
			
			Object[] data = (Object[])lastMessageContents[4];
			
			for(int i = 0; i < data.length; i++){
				byte[] varLenData = convertToByteArray(data[i]);
				alVarData.add(varLenData);
				sizeOfVarLenData+= varLenData.length;
			}
			lengthOfMastArray = lengthOfMastArray + sizeOfVarLenData;
		}
		
		
		byte[] masterArray = new byte[lengthOfMastArray];
										
		int indexToStart = 0;
		
		for(int i = 0; i < msgBytes.length; i++){
			masterArray[indexToStart] = msgBytes[i];
			indexToStart++;
		}
		
		for(int i = 0; i < ivBytes.length; i++){
			masterArray[indexToStart] = ivBytes[i];
			indexToStart++;
		}
		
		for(int i = 0; i < intBytes.length; i++){
			masterArray[indexToStart] = intBytes[i];
			indexToStart++;
		}
		
		if(tokenBytes != null){
			for(int i = 0; i < tokenBytes.length; i++){
				masterArray[indexToStart] = tokenBytes[i];
				indexToStart++;
			}
		}
		
		if(lastMessageContents[4] != null){
			for(int i = 0; i < alVarData.size(); i++){
				
				byte[] loopArray = alVarData.get(i);
				
				for(int j = 0; j < loopArray.length; j++){
					masterArray[indexToStart] = loopArray[j];
					indexToStart++;
				}
				
			}
		}
				
		byte[] testHMAC = createHMAC(integrityKey, masterArray);
		
		// Check if HMACs are equal
		for(int i = 0; i < HMAC.length; i++){
			
			if(testHMAC[i] != HMAC[i]){
				isHMACValid = false;
				break;
			}
			
		}
		
		// If false return 
		if(isHMACValid == false){
			System.out.println("FAIL HMAC CHECK");
			return isHMACValid;
		}
				
		// Check Number of Message
		
		Integer incomingMessageNumber = (Integer)lastMessageContents[2];
					
		
		if(incomingMessageNumber == numberOfMessage){
			
			// Number of the message to send out for response to user
			//incomingMessageNumber = incomingMessageNumber + 1;
			synchronized(Integer.class)
			{
				numberOfMessage = numberOfMessage + 1;
			}
			
		} else {
						
			// Not valid, we will disconnect after receiving -1
			System.out.println("FAIL MESSAGE NUMBER CHECK");
			isMsgNumValid = false;
			return isMsgNumValid;
			
		}
		
		// Check Token
		if(lastMessageContents[3] != null){
			
			UserToken checkToken = (UserToken)lastMessageContents[3];
			// Group Server public key
			if(!isValidToken(checkToken)){
				// Token is not valid....return false
				System.out.println("FAIL TOKEN CHECK");
				isTokenValid = false;
				return isTokenValid;
			}
			
			
		}
				
		return true;
				
	}
	
	protected Object getFromEnvelope(Field f){
		
		return lastMessageContents[f.ordinal()];	
				
	}
	
		
	
	protected static byte[] convertToByteArray(Object objToConvert){
		
		try{
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    ObjectOutputStream oos = new ObjectOutputStream(baos);
		    oos.writeObject(objToConvert);
		    //return b.toByteArray(); 
			return baos.toByteArray();
		
		} catch(Exception ex){
		
//			System.out.println("Error creating byte array envelope: " + ex.toString());
			ex.printStackTrace();
		}
		
		return null;
		
	}
	
	protected static Object convertToObject(byte[] bytesToConvert){
		
		try
		{
			
			ByteArrayInputStream bais = new ByteArrayInputStream(bytesToConvert);
	        ObjectInputStream ois = new ObjectInputStream(bais);
	        
	        return ois.readObject();
	        
		} catch(Exception ex){
			
//			System.out.println("Error byte array to object: " + ex.toString());
			ex.printStackTrace();
			
		}
		
		return null;
		
	}
	
//	/**
//	 * This method will encrypt data via an AES encryption algorithm utilizing an IV and symmetric key.
//	 * @param algorithm The algorithm to use.
//	 * @param provider The security provider.
//	 * @param key The symmetric key
//	 * @param iv The IV used for encryption.
//	 * @param dataToEncrypt The clear data to encrypt.
//	 * @return byte[] array of encrypted data.
//	 */
//	private byte[] AESEncrypt(String algorithm, String provider, Key key, IvParameterSpec iv, byte[] byteData){
//
//		try{
//
//			// Create the cipher object
//			Cipher objCipher = Cipher.getInstance(algorithm, provider);
//
//			// Initialize the cipher encryption object, add the key, and add the IV
//			objCipher.init(Cipher.ENCRYPT_MODE, key, iv); 
//			
//			// Encrypt the data and store in encryptedData
//			return objCipher.doFinal(byteData);
//
//
//		} catch(Exception ex){
//			System.out.println(ex.toString());
//			ex.printStackTrace();
//		}
//
//		return null;
//	}
//	
//	
//	/**
//	 * This method will decrypt the data.
//	 * @param algorithm The algorithm to use.
//	 * @param provider The security provider.
//	 * @param key The symmetric key to use.
//	 * @param iv The IV to use for decryption.
//	 * @param dataToDecrypt The data to decrypt.
//	 * @return byte[] array of decrypted data.
//	 */
//	private byte[] AESDecrypt(String algorithm, String provider, Key key, IvParameterSpec iv, byte[] dataToDecrypt){
//
//		try{
//
//			Cipher objCipher = Cipher.getInstance(algorithm, provider);
//
//			// Initialize the cipher encryption object, add the key, and add the IV
//			objCipher.init(Cipher.DECRYPT_MODE, key, iv); 
//
//			// Encrypt the data and store in encryptedData
//			return objCipher.doFinal(dataToDecrypt);
//
//		} catch(Exception ex){
//			System.out.println(ex.toString());
//			ex.printStackTrace();
//		}
//
//		return null;
//	}
	
	
		
	
	private byte[] createHMAC(Key symmetricIntegrityKey, byte[] msgData){
		
		
		try{
			Mac HMAC = Mac.getInstance(HMAC_ALGORITHM, PROVIDER);
			HMAC.init(symmetricIntegrityKey);
			
			return HMAC.doFinal(msgData);
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		return null;
		
		
		
		
	}
	
		
	protected Envelope encryptMessageWithSymmetricKey(String message, UserToken token, Object[] data){
		
		try{
			
			Envelope response = new Envelope(message);
			
			Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);
			
			IvParameterSpec IV = ivAES();
			
			// Initialize the cipher encryption object, add the key, and add the IV
			objCipher.init(Cipher.ENCRYPT_MODE, this.confidentialKey, IV); 
			
			for(Field f : Field.values()){
				
				switch(f){
					case HMAC:
						
						//Key integrityKey = genterateSymmetricKey();
						int lengthOfMastArray = 0;
						
						byte[] msgBytes = convertToByteArray(message);
						lengthOfMastArray = lengthOfMastArray + msgBytes.length;		
						
						byte[] ivBytes = IV.getIV();
						lengthOfMastArray = lengthOfMastArray + ivBytes.length;
						
						byte[] intBytes = convertToByteArray(numberOfMessage);
						lengthOfMastArray = lengthOfMastArray + intBytes.length;
						
						byte[] tokenBytes = null;
						
						if(token != null){
							tokenBytes = convertToByteArray(token);
							lengthOfMastArray = lengthOfMastArray + tokenBytes.length;
						} 
						
						int sizeOfVarLenData = 0;
						
						ArrayList<byte[]> alVarData = new ArrayList<byte[]>();
						
						if(data != null){
							for(int i = 0; i < data.length; i++){
								byte[] varLenData = convertToByteArray(data[i]);
								alVarData.add(varLenData);
								sizeOfVarLenData+= varLenData.length;
							}
							lengthOfMastArray = lengthOfMastArray + sizeOfVarLenData;
						}
						
						
						byte[] masterArray = new byte[lengthOfMastArray];
						
						int indexToStart = 0;
						
						for(int i = 0; i < msgBytes.length; i++){
							masterArray[indexToStart] = msgBytes[i];
							indexToStart++;
						}
						
						for(int i = 0; i < ivBytes.length; i++){
							masterArray[indexToStart] = ivBytes[i];
							indexToStart++;
						}
						
						for(int i = 0; i < intBytes.length; i++){
							masterArray[indexToStart] = intBytes[i];
							indexToStart++;
						}
						
						if(tokenBytes != null){
							for(int i = 0; i < tokenBytes.length; i++){
								masterArray[indexToStart] = tokenBytes[i];
								indexToStart++;
							}
						}
						
						if(data != null){
							for(int i = 0; i < alVarData.size(); i++){
								
								byte[] loopArray = alVarData.get(i);
								
								for(int j = 0; j < loopArray.length; j++){
									masterArray[indexToStart] = loopArray[j];
									indexToStart++;
								}
								
							}
						}
						
						// Add HMAC to envelope
						response.addObject(objCipher.doFinal(createHMAC(integrityKey, masterArray)));				
						break;
						
					case IV:
						response.addObject(IV.getIV());				
						break;
						
					case INT:				
						response.addObject(objCipher.doFinal(convertToByteArray(numberOfMessage)));				
						break;
						
					case TOKEN:
						if(token != null){
							response.addObject(objCipher.doFinal(convertToByteArray(token)));
						}
						else
						{
							response.addObject(null);
						}
						break;
						
					case DATA:
						if(data != null){
							
							response.addObject(objCipher.doFinal(convertToByteArray(data)));
							
							//for(int i = 0; i < data.length; i++){
							// encrypt and add to envelope
							//	response.addObject(objCipher.doFinal(convertToByteArray(data[i])));
							//}
						}
						else
						{
							response.addObject(null);
						}
						break;
				}
			}
			//byte[] dataToEncryptBytes = dataToEncrypt.getBytes();
			
			/*
		for(Object o : objs){
			
			byte[] newEncryptedChallenge = objCipher.doFinal(convertToByteArray(o));	
			response.addObject(newEncryptedChallenge);
			
		}
		response.addObject(IV.getIV());
																
		return response;
			 */
			
			//increment number of message
			synchronized(Integer.class)
			{
				numberOfMessage = numberOfMessage + 1;
			}
			
			return response;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	protected byte[] decryptObjectBytes(byte[] objByte, byte[] iv)  {
		
		try{

			Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);

			// Initialize the cipher encryption object, add the key, and add the IV
			objCipher.init(Cipher.DECRYPT_MODE, this.confidentialKey, new IvParameterSpec(iv)); 

			// Encrypt the data and store in encryptedData
			return objCipher.doFinal(objByte);

		} catch(Exception ex){
			ex.printStackTrace();
		}

		return null;
	}
	
	//From the client
	//NULL MEANS ERROR AND SHOULD TERMINATE
	protected Envelope setUpSecureConnection(Envelope requestSecureConnection)
	{
		if(requestSecureConnection.getObjContents().size() == 1)
		{
			try
			{
				byte[] encryptedChallenge = (byte[])requestSecureConnection.getObjContents().get(0); //should be encrypted with public key
				Cipher objCipher = Cipher.getInstance(ASYM_ALGORITHM, PROVIDER);
				objCipher.init(Cipher.DECRYPT_MODE, privateKey); 
				byte[] serversChallenge = objCipher.doFinal(encryptedChallenge);
				
				if(serversChallenge == null || serversChallenge.length != 20)
				{
					System.out.println("Challenge fails pretest");
					return null;
				}
				for(int i = 0; i < serversChallenge.length; i++)
				{
					if(this.challengeBytes[i] != serversChallenge[i])
					{
						System.out.println("Challenge fails comparison test.");
						return null;
					}
				}
				return new Envelope("OK");
			}
			catch(NoSuchAlgorithmException e)
			{
				e.printStackTrace();
			}
//			catch(InvalidAlgorithmParameterException e)
//			{
//				e.printStackTrace();
//				response = null;
//			}
			catch(NoSuchPaddingException e)
			{
				e.printStackTrace();
			}
			catch(NoSuchProviderException e)
			{
				e.printStackTrace();
			}
			catch(InvalidKeyException e)
			{
				e.printStackTrace();
			}
			catch(BadPaddingException e)
			{
				e.printStackTrace();
			}
			catch(IllegalBlockSizeException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		byte[] encryptedChallenge = (byte[])requestSecureConnection.getObjContents().get(0); // Get the encrypted challenge
		byte[] encryptedConfidentialKey = (byte[])requestSecureConnection.getObjContents().get(1); // Get the encrypted key
		byte[] encryptedIntegrityKey = (byte[])requestSecureConnection.getObjContents().get(2); //Get the integrity key
		byte[] encryptedGroupServerName = null;
		if(requestSecureConnection.getObjContents().size() > 3)
		{
			encryptedGroupServerName = (byte[])requestSecureConnection.getObjContents().get(3); //get the encrypted server name
		}
		Envelope response = null;
		try
		{
			if (encryptedChallenge == null || encryptedConfidentialKey == null || encryptedIntegrityKey == null)
			{
				response = new Envelope("FAIL");
			}
			else
			{
				// Create the cipher object
				Cipher objCipher = Cipher.getInstance(ASYM_ALGORITHM, PROVIDER);

				// Initialize the cipher encryption object, add the key 
				objCipher.init(Cipher.DECRYPT_MODE, privateKey); 

				// Encrypt the data and store in encryptedData
				byte[] decryptedChallenge = objCipher.doFinal(encryptedChallenge);
				byte[] decryptedConfidentialKey = objCipher.doFinal(encryptedConfidentialKey);
				byte[] decryptedIntegrityKey = objCipher.doFinal(encryptedIntegrityKey);
				
				//For non-group servers
				if(encryptedGroupServerName != null)
				{
					String decryptedGroupServerName = new String(objCipher.doFinal(encryptedGroupServerName));
					CAClient caClient = new CAClient(decryptedGroupServerName);
					caClient.connect(CA_LOC, CA_PORT, null);
//					caClient.connect();
					caClient.run();
					caClient.disconnect();
					this.groupServerPublicKey = (RSAPublicKey)caClient.getPublicKey();
				}

				//SecretKeySpec secretKeySpec 
				this.confidentialKey = new SecretKeySpec(decryptedConfidentialKey, "AES");
				this.integrityKey = new SecretKeySpec(decryptedIntegrityKey, "AES");
				
				//getting a random integer
				this.random.nextBytes(this.challengeBytes);

				//TODO encrypt all material and send it back
				return encryptMessageWithSymmetricKey("OK", null, new Object[] {decryptedChallenge, this.challengeBytes});
				
//				objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);
//				// Get the IV to use
//				IvParameterSpec IV = ivAES();
//
//				// Initialize the cipher encryption object, add the key, and add the IV
//				objCipher.init(Cipher.ENCRYPT_MODE, this.confidentialKey, IV); 
//
//				// Encrypt the data and store in encryptedData
//				byte[] newEncryptedChallenge = objCipher.doFinal(decryptedChallenge);
//
//				// Respond to the client. On error, the client will receive a null token
//				response = new Envelope("OK");
//
//				response.addObject(newEncryptedChallenge);
//				response.addObject(IV.getIV());
			}
		}
		catch(NoSuchAlgorithmException e)
		{
			e.printStackTrace();
			response = null;
		}
//		catch(InvalidAlgorithmParameterException e)
//		{
//			e.printStackTrace();
//			response = null;
//		}
		catch(NoSuchPaddingException e)
		{
			e.printStackTrace();
			response = null;
		}
		catch(NoSuchProviderException e)
		{
			e.printStackTrace();
			response = null;
		}
		catch(InvalidKeyException e)
		{
			e.printStackTrace();
			response = null;
		}
		catch(BadPaddingException e)
		{
			e.printStackTrace();
			response = null;
		}
		catch(IllegalBlockSizeException e)
		{
			e.printStackTrace();
			response = null;
		}
		
		return response;
	}
	
	//FOR USE ONLY WITH FILETHREAD
	
	protected boolean isValidToken(UserToken t)
	{
		return t.RSAVerifySignature("SHA1withRSA", PROVIDER, (this.groupServerPublicKey == null ? this.publicKey : this.groupServerPublicKey)) 
				&& t.getFileServerName().equals(this.serverName) && t.getIPAddress().equals(this.ipAddress) && t.getPortNumber() == this.portNumber;
	}
	
	protected void resetMessageCounter()
	{
		synchronized(Integer.class)
		{
			this.numberOfMessage = 0;
		}
	}
}
