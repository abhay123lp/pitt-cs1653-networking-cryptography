/**
 * 
 */
package server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import client.certificateAuthority.CAClient;

import message.Envelope;
import message.Field;
import message.UserToken;

/**
 *
 */
public abstract class ServerThread extends Thread
{
	private String serverName;
	private String ipAddress;
	private int portNumber;
	
	private static final String SYM_KEY_TYPE = "AES";
	protected static final String SYM_KEY_ALG = SYM_KEY_TYPE + "/CTR/NoPadding";
	protected static final String PROVIDER = "BC";
	protected static final String ASYM_ALGORITHM = "RSA";
	private static final int IV_BYTES = 16;
	private static final int ENV_CONTENTS_SIZE = 5;
	private static final String CA_LOC = "localhost";
	private static final int CA_PORT = 4999;
	private static final String HMAC_ALGORITHM = "HmacSHA1";
	private static final int HASH_CHALLENGE_INPUT_LENGTH = 16;
	private static final int HASH_CHALLENGE_RANDOM_LENGTH = 3;
	
	private final SecureRandom random;
	
	protected final Socket socket;
	protected final RSAPublicKey publicKey;
	protected final RSAPrivateKey privateKey;
	
	private RSAPublicKey groupServerPublicKey; //FOR USE BY NON GROUP SERVERS ONLY
	private Key hashInversionKey;
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
		this.hashInversionKey = null;
		try
		{
			this.hashInversionKey = KeyGenerator.getInstance(SYM_KEY_TYPE, PROVIDER).generateKey();
		}
		catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (NoSuchProviderException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		    oos.flush();
			return baos.toByteArray();
		} catch(Exception ex){
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
						}
						else
						{
							response.addObject(null);
						}
						break;
				}
			}
			
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
	
	private Envelope hold;
	
	//From the client
	//NULL MEANS ERROR AND SHOULD TERMINATE
	protected Envelope setUpSecureConnection(Envelope requestSecureConnection)
	{
		if(requestSecureConnection.getObjContents().size() == 5)
		{
			if(!this.checkValidityOfMessage(requestSecureConnection))
			{
				System.out.println("Some error occurred with the envelope");
				return null;
			}
			try
			{
				byte[] encryptedChallenge = (byte[])((Object[])this.getFromEnvelope(Field.DATA))[0];
				Cipher objCipher = Cipher.getInstance(ASYM_ALGORITHM, PROVIDER);
				objCipher.init(Cipher.DECRYPT_MODE, privateKey); 
				byte[] serversChallenge = objCipher.doFinal(encryptedChallenge);
				if(this.getFromEnvelope(Field.TOKEN) != null)
				{
					UserToken tokenCheck = (UserToken)this.getFromEnvelope(Field.TOKEN);
					if(!this.isValidToken(tokenCheck))
					{
						System.out.println("Token does not check out.");
						return null;
					}
				}
				
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
		
		this.hold = requestSecureConnection;
		
		// Send a hash inversion challenge
		try
		{
			Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);
			objCipher.init(Cipher.ENCRYPT_MODE, this.hashInversionKey);
			byte[] randomInput = this.generateRandomInput(HASH_CHALLENGE_INPUT_LENGTH, HASH_CHALLENGE_RANDOM_LENGTH);
			long currTime = System.currentTimeMillis();
			Envelope hashChallengeEnv = new Envelope("HASH_CHALLENGE");
			hashChallengeEnv.addObject(HASH_CHALLENGE_INPUT_LENGTH);
			hashChallengeEnv.addObject(HASH_CHALLENGE_RANDOM_LENGTH);
			hashChallengeEnv.addObject(generateSHAHash(randomInput));
			hashChallengeEnv.addObject(new byte[][]{objCipher.doFinal(String.valueOf(currTime).getBytes()), objCipher.doFinal(randomInput)});
			return hashChallengeEnv;
		}
		catch(Exception e) //TODO bad
		{
			e.printStackTrace();
		}
		return null;
	}
	
	//----hash challenge stuff----//
	private static MessageDigest md;
	static
	{
		try
		{
			md = MessageDigest.getInstance("SHA", "BC");
		}
		catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (NoSuchProviderException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected Envelope afterHashInversionChallenge(Envelope hashInversion)
	{
		if(!checkHashInversion(hashInversion))
		{
			System.out.println("Fail hash inversion check");
			return null;
		}
		byte[] encryptedChallenge = (byte[])this.hold.getObjContents().get(0); // Get the encrypted challenge
		byte[] encryptedConfidentialKey = (byte[])this.hold.getObjContents().get(1); // Get the encrypted key
		byte[] encryptedIntegrityKey = (byte[])this.hold.getObjContents().get(2); //Get the integrity key
		byte[] encryptedGroupServerName = null;
		if(this.hold.getObjContents().size() > 3)
		{
			encryptedGroupServerName = (byte[])this.hold.getObjContents().get(3); //get the encrypted server name
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
					caClient.run();
					caClient.disconnect();
					this.groupServerPublicKey = (RSAPublicKey)caClient.getPublicKey();
				}

				//SecretKeySpec secretKeySpec 
				this.confidentialKey = new SecretKeySpec(decryptedConfidentialKey, "AES");
				this.integrityKey = new SecretKeySpec(decryptedIntegrityKey, "AES");
				
				//getting a random integer
				this.random.nextBytes(this.challengeBytes);

				return encryptMessageWithSymmetricKey("OK", null, new Object[] {decryptedChallenge, this.challengeBytes});
			}
		}
		catch(NoSuchAlgorithmException e)
		{
			e.printStackTrace();
			response = null;
		}
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
	
	private boolean checkHashInversion(Envelope hashInversion)
	{
		try
		{
			Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);
			objCipher.init(Cipher.DECRYPT_MODE, this.hashInversionKey);
			byte[] computedInput = (byte[])hashInversion.getObjContents().get(0);
			byte[][] encryptedState = (byte[][])hashInversion.getObjContents().get(1);
			byte[] actualInput = objCipher.doFinal(encryptedState[1]);
			return new String(computedInput).equals(new String(actualInput));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	private byte[] generateSHAHash(byte[] array)
	{
		md.reset();
		md.update(array);
		return md.digest();
	}
	
	private byte[] generateRandomInput(int totalSize, int lengthOfRandom)
	{
		return combineBytes(generateAllOnes(totalSize-lengthOfRandom), generateRandomBytes(lengthOfRandom));
	}
	
	private byte[] generateAllOnes(int length)
	{
		byte[] ones = new byte[length];
		byte one = Byte.MAX_VALUE;
		for(int i = 0; i < length; i++)
		{
			ones[i] = one;
		}
		return ones;
	}
	
	private byte[] generateRandomBytes(int length)
	{
		SecureRandom secRand = new SecureRandom();
		byte[] randBytes = new byte[length];
		secRand.nextBytes(randBytes);
		return randBytes;
	}
	
	private byte[] combineBytes(byte[] array1, byte[] array2)
	{
		byte[] total = new byte[array1.length+array2.length];
		System.arraycopy(array1, 0, total, 0, array1.length);
		System.arraycopy(array2, 0, total, array1.length, array2.length);
		return total;
	}
	//---end challenge stuff---//
	
	//FOR USE ONLY WITH FILETHREAD
	protected boolean isValidToken(UserToken t)
	{
		return t.RSAVerifySignature("SHA1withRSA", PROVIDER, (this.groupServerPublicKey == null ? this.publicKey : this.groupServerPublicKey)) && t.getFileServerName().equals(this.serverName) && t.getIPAddress().equals(this.ipAddress) && t.getPortNumber() == this.portNumber;
	}
	
	protected void resetMessageCounter()
	{
		synchronized(Integer.class)
		{
			this.numberOfMessage = 0;
		}
	}
}
