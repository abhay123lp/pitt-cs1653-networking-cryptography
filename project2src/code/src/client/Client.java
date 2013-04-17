package client;

import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;

import client.certificateAuthority.CAClient;

import message.Envelope;
import message.Field;
import message.UserToken;

/**
 * The Client abstract class holds implementation of connecting to a server, but does not hold specific methods on group server or file server requests.
 * 
 * @see GroupClient, FileClient
 */
public abstract class Client implements ClientInterface
{
	/*
	 * protected keyword is like private but subclasses have access Socket and input/output streams
	 */
	/**
	 * The socket that will connect to the server.
	 */
	protected Socket sock;
	
	private MessageDigest md; 
	
	/**
	 * The output stream to the server.
	 */
	protected ObjectOutputStream output;
	
	/**
	 * The input stream from the server.
	 */
	protected ObjectInputStream input;
	
	protected static final String RSA_ALGORITHM = "RSA/None/NoPadding";
	private static final String SYM_ALGORITHM = "AES";
	protected static final String SYM_KEY_ALG = "AES/CTR/NoPadding";
	protected static final String PROVIDER = "BC";
	private static final int IV_BYTES = 16;
	private static final String CA_LOC = "localhost";
	private static final String HMAC_ALGORITHM = "HmacSHA1";
	private static final int ENV_CONTENTS_SIZE = 5;
	
	protected Key confidentialKey;
	protected Key integrityKey;
	
	private SecureRandom random;
	private Object[] lastMessageContents;
	private volatile int numberOfMessage;
	
	public Client()
	{
		this.random = new SecureRandom();
		this.confidentialKey = this.generateSymmetricKey();
		this.integrityKey = this.generateSymmetricKey();
		this.lastMessageContents = null;
		this.numberOfMessage = 0;
		try {
			this.md = MessageDigest.getInstance("SHA", "BC");
		} catch (NoSuchAlgorithmException e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		} catch(NoSuchProviderException e){
			
			e.printStackTrace();
			
		}
	}
	
	private void unencryptMessage(Envelope env)
	{
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
	protected boolean checkValidityOfMessage(Envelope env)
	{
		if(env.getObjContents().isEmpty())
		{
			return true;
		}
						
		boolean isHMACValid = true;
		boolean isMsgNumValid = true;
//		boolean isTokenValid = true;
		
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
		if(incomingMessageNumber.equals(Integer.valueOf(numberOfMessage))){
			// Number of the message to send out for response to user					
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
//		if(lastMessageContents[3] != null){
//			
//			UserToken checkToken = (UserToken)lastMessageContents[3];
//			// Group Server public key
//			if(!verifyTokenSignature(checkToken)){
//				// Token is not valid....return false
//				isTokenValid = false;
//				return isTokenValid;
//			}
//		}
		return true;			
	}
	
	protected Object getFromEnvelope(Field f)
	{
		return lastMessageContents[f.ordinal()];			
	}
	
	private byte[] createHMAC(Key symmetricIntegrityKey, byte[] msgData)
	{
		try
		{
			Mac HMAC = Mac.getInstance(HMAC_ALGORITHM, PROVIDER);
			HMAC.init(symmetricIntegrityKey);
			
			return HMAC.doFinal(msgData);	
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		return null;	
	}
	
	private byte[] encryptPublic(RSAPublicKey pubKey, byte[] encoded)
	{
		try
		{
			Cipher oCipher = Cipher.getInstance(RSA_ALGORITHM, PROVIDER);
			oCipher.init(Cipher.ENCRYPT_MODE, pubKey); 
			return oCipher.doFinal(encoded);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
	
	/**
	 * This method will generate a symmetric key for use.
	 */
	private final Key generateSymmetricKey()
	{
		try
		{
			// Random Number used to generate key
			SecureRandom randomNumber = new SecureRandom();	

			// Generate a 128-bit AES Key with Bouncy Castle provider
			KeyGenerator keyGenerator = KeyGenerator.getInstance(SYM_ALGORITHM, PROVIDER);
			keyGenerator.init(128, randomNumber);
			Key key = keyGenerator.generateKey();

			return key;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}

	protected IvParameterSpec ivAES()
	{
		byte[] bytesIV = new byte[IV_BYTES];	
		this.random.nextBytes(bytesIV);

		// Create the IV
		return new IvParameterSpec(bytesIV);
	}
	
	protected Envelope encryptMessageWithSymmetricKey(String message, UserToken token, Object[] data){
		try{
			Envelope response = new Envelope(message);
			Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);
			IvParameterSpec IV = ivAES();
			
			// Initialize the cipher encryption object, add the key, and add the IV
			objCipher.init(Cipher.ENCRYPT_MODE, this.confidentialKey, IV); 
			
			for(Field f : Field.values())
			{
				switch(f)
				{
					case HMAC:
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

	protected byte[] decryptObjectBytes(byte[] objByte, byte[] iv)
	{
		try
		{
			Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);

			// Initialize the cipher encryption object, add the key, and add the IV
			objCipher.init(Cipher.DECRYPT_MODE, confidentialKey, new IvParameterSpec(iv)); 

			// Encrypt the data and store in encryptedData
			byte[] decrypted = objCipher.doFinal(objByte);
			return decrypted;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}

	private static byte[] convertToByteArray(Object objToConvert)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(objToConvert);
			oos.flush();
			//return b.toByteArray(); 
//			System.out.println("CONVERTING TO: " + new String(baos.toByteArray()));
			return baos.toByteArray();
		}
		catch(Exception ex)
		{
//			System.out.println("Error creating byte array envelope: " + ex.toString());
			ex.printStackTrace();
		}
		return null;
	}

	protected static Object convertToObject(byte[] bytesToConvert)
	{
		try
		{
//			System.out.println("Object created is: " + new String(bytesToConvert));
			ByteArrayInputStream bais = new ByteArrayInputStream(bytesToConvert);
			ObjectInputStream ois = new ObjectInputStream(bais);
			return ois.readObject();
		}
		catch(Exception ex)
		{
//			System.out.println("Error byte array to object: " + ex.toString());
			ex.printStackTrace();
		}
		return null;
	}
	
	public boolean connect(final String server, final int port, final String serverName)
	{
		return this.connect(server, port, serverName, null, null);
	}
	
	public Envelope computeHashInversion(Integer inputLen, Integer randByteLen, byte[] hashInput, Object returnMsg){
		// brute force hash inversion
		byte[] bruteForceInput = invertHash(hashInput, inputLen, randByteLen);
		
		Envelope answerEnvelope = new Envelope("HASH_CHALLENGE");		
		answerEnvelope.addObject(bruteForceInput);
		answerEnvelope.addObject(returnMsg);
		return answerEnvelope;
		
		// create envelope as hash challenge - return
		// Envelope has brute force input, return message
		
		
	}
	
	public byte[] invertHash(byte[] digest, int lengthOfOrig, int lengthOfRandom)
	{
		byte[] ones = generateAllOnes(lengthOfOrig-lengthOfRandom);
		byte[] randomIter = new byte[lengthOfRandom];
		for(int i = 0; i < lengthOfRandom; i++)
		{
			randomIter[i] = Byte.MIN_VALUE;
		}
		String digestString = new String(digest);
		byte[] counter = combineBytes(ones, randomIter);
		String counterString = new String(generateSHAHash(counter));
		boolean same = digestString.hashCode() == counterString.hashCode() && digestString.equals(counterString);
		while(!same)
		{
			randomIter[0]++;
			boolean carryOver = randomIter[0] == Byte.MIN_VALUE;
			for(int i = 1; i < lengthOfRandom; i++)
			{
				if(!carryOver)
				{
					break;
				}
				randomIter[i]++;
				carryOver = randomIter[i] == Byte.MIN_VALUE;
			}
			counter = combineBytes(ones, randomIter);
			counterString = new String(generateSHAHash(counter));
			same = digestString.hashCode() == counterString.hashCode() && digestString.equals(counterString);
			if(carryOver)
			{
				//Means it exhausted all possibilities, but did not find the correct thing.
				break;
			}
		}
		return counter;
	}
	
	public byte[] combineBytes(byte[] array1, byte[] array2)
	{
		byte[] total = new byte[array1.length+array2.length];
		System.arraycopy(array1, 0, total, 0, array1.length);
		System.arraycopy(array2, 0, total, array1.length, array2.length);
		return total;
	}
	
	public byte[] generateSHAHash(byte[] array)
	{
//		this.md.reset();
		this.md.update(array);
		return md.digest();
	}
	
	public byte[] generateAllOnes(int length)
	{
		byte[] ones = new byte[length];
		byte one = Byte.MAX_VALUE;
		for(int i = 0; i < length; i++)
		{
			ones[i] = one;
		}
		return ones;
	}
	
	// javadoc already handled by ClientInterface
	// groupServerName is for the FileServer to retrieve the group server's public key.  It is ignored otherwise.
	public boolean connect(final String server, final int port, final String serverName, final String groupServerName, final UserToken serverToken)
	{
		if (this.sock != null)
		{
			System.out.println("Disconnecting from previous connection...");
			this.disconnect();
		}
		try
		{
			this.sock = new Socket(server, port);
			// this.sock.setSoTimeout(1000);
			this.output = new ObjectOutputStream(this.sock.getOutputStream());
			this.input = new ObjectInputStream(this.sock.getInputStream());
			
			System.out.println("Connecting to CA");
			CAClient ca = new CAClient(serverName);
			ca.connect(CA_LOC, 4999, null);
			ca.run();
			ca.disconnect();
			RSAPublicKey serverPublicKey = (RSAPublicKey)ca.getPublicKey();
			
			System.out.println("Setting up connection to the Server");
			/* Sending Challenge and Symmetric Key to Server *******/
			Envelope request = new Envelope("REQUEST_SECURE_CONNECTION");
			
			byte[] challengeBytes = new byte[20];
			this.random.nextBytes(challengeBytes); //SYMMETRIC_KEY.getEncoded(); //concatenation.getBytes();
			
			request.addObject(encryptPublic(serverPublicKey, challengeBytes));
			request.addObject(encryptPublic(serverPublicKey, this.confidentialKey.getEncoded()));
			request.addObject(encryptPublic(serverPublicKey, this.integrityKey.getEncoded()));
			if(groupServerName != null)
			{
				request.addObject(encryptPublic(serverPublicKey, groupServerName.getBytes()));
			}
			this.output.writeObject(request);
		
			Envelope reqResponse = (Envelope)this.input.readObject();
			// check hash inversion here
			// Integer - len of input, Integer - len of random bytes, byte[] - hash of input, byte[] - return message
			// store return message to send back
			Integer lenOfInput = (Integer)reqResponse.getObjContents().get(0);
			Integer lenOfRandBytes = (Integer)reqResponse.getObjContents().get(1);
			byte[] hashOfInput = (byte[])reqResponse.getObjContents().get(2);
			Object returnMessage = reqResponse.getObjContents().get(3);
			
			this.output.writeObject(computeHashInversion(lenOfInput, lenOfRandBytes, hashOfInput, returnMessage));
			
			// Do secure connection stuff below here
			reqResponse = (Envelope)this.input.readObject();
						
			if(!checkValidityOfMessage(reqResponse))
			{
				disconnect();
				System.out.println("Invalid response.");
			}
			if (reqResponse.getMessage().equals("OK"))
			{
				//check the encryption challenge and respond with the server's one.
				if(reqResponse.getObjContents().size() < 5)
				{
					disconnect();
					System.out.println("Reply is invalid.");
				}
				
				Object[] objectsList = (Object[])getFromEnvelope(Field.DATA);
				
				if (objectsList == null || objectsList.length < 2 || objectsList[0] == null || objectsList[1] == null)
				{
					// We should get out of here
					disconnect();
					System.out.println("Entry challenge is null.  Failed.");
				}
				else
				{					
					String originalChallenge = new String(challengeBytes);
					String returnedChallenge = new String((byte[])objectsList[0]);

					if(originalChallenge.equals(returnedChallenge))
					{
						// Secure connection
						System.out.println("Success! Secure connection established!  Sending confirmation...");
						Envelope finalMsg = this.encryptMessageWithSymmetricKey("REQUEST_SECURE_CONNECTION", serverToken, new Object[]{encryptPublic(serverPublicKey, (byte[])objectsList[1])});
						this.output.writeObject(finalMsg);
						reqResponse = (Envelope)this.input.readObject();
						if(reqResponse == null || !reqResponse.getMessage().equals("OK"))
						{
							disconnect();
							System.out.println("Oops, something went wrong...");
						}
					}
					else
					{
						disconnect();
						System.out.println("Challenge is not equal");
					}
				}
			}
			else
			{
				// It failed
				disconnect();
				System.out.println("Request Response failed.");
			}
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
			return false;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
			return false;
		}
		System.out.println("Success!  Connected to " + server + " at port " + port);
		return true;
	}// end method connect(String, int)
	
	// javadoc already handled by ClientInterface
	public boolean isConnected()
	{
		if (sock == null || !sock.isConnected())
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	// javadoc already handled by ClientInterface
	public void disconnect()
	{
		if (isConnected())
		{
			try
			{
				Envelope message = this.encryptMessageWithSymmetricKey("DISCONNECT", null, null);
				output.writeObject(message);
				this.output.close();
				this.input.close();
				this.sock.close();
			}
			catch (Exception e)
			{
				e.printStackTrace(System.err);
			}
			this.sock = null;
			this.numberOfMessage = 0;
		}
	}//end method disconnect
}// end class Client
