package client;

import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;

import message.Envelope;

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
	
	/**
	 * The output stream to the server.
	 */
	protected ObjectOutputStream output;
	
	/**
	 * The input stream from the server.
	 */
	protected ObjectInputStream input;
	
	protected final String RSA_ALGORITHM = "RSA/None/NoPadding";
	private final String SYM_ALGORITHM = "AES";
	protected final String SYM_KEY_ALG = "AES/CTR/NoPadding";
	protected final String PROVIDER = "BC";
	protected Key SYMMETRIC_KEY;
	private SecureRandom CHALLENGE;
	public RSAPublicKey serverPublicKey;
	private static final int IV_BYTES = 16;
	private byte[] challengeBytes;
	private final String CAServer = "localhost";
	//private CAClient caClient;
	
	public Client(){
	
		/****** TODO *******
		caClient = new CAClient();
		caClient.run();
		serverPublicKey = (RSAPublicKey)caClient.getPublicKey();
		*********/
	}
		
	public Key getSymmetricKey(){
		
		return SYMMETRIC_KEY;
		
	}
	
	public SecureRandom generateChallenge(){
		
		return new SecureRandom();
		
	}
	
//	public void getServerPublicKey(String serverName){
//		
//		
//		
//	}
	
	
	/*	
	public void createSymmetricKey(){
				
		SYMMETRIC_KEY = genterateSymmetricKey(SYM_ALGORITHM, PROVIDER);
									
	}
	*/
	
	public byte[] encryptKey(RSAPublicKey pubKey){
		
		try{
			
			SYMMETRIC_KEY = genterateSymmetricKey(SYM_ALGORITHM, PROVIDER);
			//CHALLENGE = generateChallenge();
						
			//String concatenation = CHALLENGE.toString() + SYMMETRIC_KEY.toString();
			
			// cipher object
			Cipher oCipher = Cipher.getInstance(RSA_ALGORITHM, PROVIDER);
	
			// cipher encryption object
			oCipher.init(Cipher.ENCRYPT_MODE, pubKey); 
	
			byte[] dataToEncryptBytes = SYMMETRIC_KEY.getEncoded(); //concatenation.getBytes();
	
						
			// Encrypt the data and store in encryptedData
			return oCipher.doFinal(dataToEncryptBytes);
		
		} catch(Exception ex){
			
			ex.printStackTrace();
		}
		
		return null;
		
	}
	
	public byte[] encryptChallenge(RSAPublicKey pubKey){
		
		try{
			
			//SYMMETRIC_KEY = genterateSymmetricKey(SYM_ALGORITHM, PROVIDER);
			CHALLENGE = generateChallenge();
						
			//String concatenation = CHALLENGE.toString() + SYMMETRIC_KEY.toString();
			
			// cipher object
			Cipher oCipher = Cipher.getInstance(RSA_ALGORITHM, PROVIDER);
	
			// cipher encryption object
			oCipher.init(Cipher.ENCRYPT_MODE, pubKey); 
	
			challengeBytes = new byte[20];
			
			CHALLENGE.nextBytes(challengeBytes); //SYMMETRIC_KEY.getEncoded(); //concatenation.getBytes();
										
			// Encrypt the data and store in encryptedData
			return oCipher.doFinal(challengeBytes);
		
		} catch(Exception ex){
			ex.printStackTrace();
			
		}
		
		return null;
		
	}
	
	//TODO verify challenge
	
	/**
	 * This method will generate a symmetric key for use.
	 * @param algorithm The encryption algorithm to use
	 * @param provider The security provider
	 * @return Key The key generated for symmetric cryptography
	 */
	private static Key genterateSymmetricKey(String algorithm, String provider){

		try {

			// Random Number used to generate key
			SecureRandom randomNumber = new SecureRandom();	

			// Generate a 128-bit AES Key with Bouncy Castle provider
			KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm, provider);
			keyGenerator.init(128, randomNumber);
			Key key = keyGenerator.generateKey();

			return key;

		} catch (Exception ex){

			System.out.println(ex.toString());
			ex.printStackTrace();

		}

		return null;

	}


	protected static IvParameterSpec ivAES(int ivBytes){

		// Random Number used for IV
		SecureRandom randomNumber = new SecureRandom();

		byte[] bytesIV = new byte[ivBytes];	
		randomNumber.nextBytes(bytesIV);

		// Create the IV
		return new IvParameterSpec(bytesIV);

	}

	protected Envelope encryptResponseWithSymmetricKey(Object[] objs, String message){

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

	protected byte[] decryptObjects(byte[] objByte, byte[] iv)  {

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


	protected static byte[] convertToByteArray(Object objToConvert){

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

	protected static Object convertToObject(byte[] bytesToConvert){

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
	
	
//	
//	
//	private static IvParameterSpec ivAES(int ivBytes){
//
//		// Random Number used for IV
//		SecureRandom randomNumber = new SecureRandom();
//
//		byte[] bytesIV = new byte[ivBytes];	
//		randomNumber.nextBytes(bytesIV);
//
//		// Create the IV
//		return new IvParameterSpec(bytesIV);
//
//	}
//	
//	
//	private static byte[] convertToByteArray(Object objToConvert){
//		
//		try{
//			
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		    ObjectOutputStream oos = new ObjectOutputStream(baos);
//		    oos.writeObject(objToConvert);
//		    //return b.toByteArray(); 
//			return baos.toByteArray();
//		
//		} catch(Exception ex){
//		
//			System.out.println("Error creating byte array envelope: " + ex.toString());
//			ex.printStackTrace();
//		}
//		
//		return null;
//		
//	}
//	
//	private static Object convertToObject(byte[] bytesToConvert){
//		
//		try
//		{
//			
//			ByteArrayInputStream bais = new ByteArrayInputStream(bytesToConvert);
//	        ObjectInputStream ois = new ObjectInputStream(bais);
//	        
//	        return ois.readObject();
//	        
//		} catch(Exception ex){
//			
//			System.out.println("Error byte array to object: " + ex.toString());
//			ex.printStackTrace();
//			
//		}
//		
//		return null;
//		
//	}
//	
//	
//	
//	/**
//	 * This method will encrypt data via an AES encryption algorithm utilizing an IV and symmetric key.
//	 * @param algorithm The algorithm to use.
//	 * @param provider The security provider.
//	 * @param key The symmetric key
//	 * @param iv The IV used for encryption.
//	 * @param dataToEncrypt The clear data to encrypt.
//	 * @return byte[] array of encrypted data.
//	 */
//	private static byte[] AESEncrypt(String algorithm, String provider, Key key, IvParameterSpec iv, byte[] byteData){
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
//	private  Envelope encryptResponseWithSymmetricKey(Object[] objs, String message){
//				
//		try{
//			
//		Envelope response = new Envelope(message);
//		
//		Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);
//					
//		IvParameterSpec IV = ivAES(IV_BYTES);
//		
//		// Initialize the cipher encryption object, add the key, and add the IV
//		objCipher.init(Cipher.ENCRYPT_MODE, SYMMETRIC_KEY, IV); 
//
//		//byte[] dataToEncryptBytes = dataToEncrypt.getBytes();
//
//		for(Object o : objs){
//			
//			byte[] newEncryptedChallenge = objCipher.doFinal(convertToByteArray(o));	
//			response.addObject(newEncryptedChallenge);
//			
//		}
//		
//		// Encrypt the data and store in encryptedData
//		//byte[] newEncryptedChallenge = objCipher.doFinal(decryptedChallenge);
//														
//		// Respond to the client. On error, the client will receive a null token
//		//response = new Envelope("OK");
//		
//		//response.addObject(newEncryptedChallenge);
//		response.addObject(IV.getIV());
//		
//        //return b.toByteArray(); 
////		byte[] byteArray = convertToByteArray(response);
//																
//		return response;
//		
//		//output.writeObject(response);
//		
//		} catch(Exception e){
//			
//			e.printStackTrace();
//			
//		}
//		
//		
//		return null;
//		
//	}
	
	
	
	
	/**
	 * This method will decrypt the data.
	 * @param algorithm The algorithm to use.
	 * @param provider The security provider.
	 * @param key The symmetric key to use.
	 * @param iv The IV to use for decryption.
	 * @param dataToDecrypt The data to decrypt.
	 * @return byte[] array of decrypted data.
	 */
	protected static byte[] AESDecrypt(String algorithm, String provider, Key key, IvParameterSpec iv, byte[] dataToDecrypt){

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
	
	
	/**
	 * This method will encrypt the data utilizing the public key.
	 * @param algorithm The algorithm to use.
	 * @param provider The security provider to use.
	 * @param pubKey The public key to use.
	 * @param dataToEncrypt The data to encrypt.
	 * @return byte[] array of the encrypted data.
	 */
	protected static byte[] RSAEncrypt(String algorithm, String provider, RSAPublicKey pubKey, byte[] dataToEncrypt){

		try{

			// Create the cipher object
			Cipher objCipher = Cipher.getInstance(algorithm, provider);

			// Initialize the cipher encryption object, add the key 
			objCipher.init(Cipher.ENCRYPT_MODE, pubKey); 

			///byte[] dataToEncryptBytes = dataToEncrypt.getBytes();

			// Encrypt the data and store in encryptedData
			return objCipher.doFinal(dataToEncrypt);

		} catch(Exception ex){
			System.out.println(ex.toString());
			ex.printStackTrace();
		}

		return null;
	}
	
	
	
	// javadoc already handled by ClientInterface
	public boolean connect(final String server, final int port, String serverName)
	{
		if (this.sock != null)
		{
			System.out.println("Disconnecting from previous connection...");
			this.disconnect();
		}
//		System.out.println("Attempting to connect...");
		try
		{
			this.sock = new Socket(server, port);
			// this.sock.setSoTimeout(1000);
			this.output = new ObjectOutputStream(this.sock.getOutputStream());
			
			System.out.println("Connecting to CA");
			CAClient ca = new CAClient(serverName);
			ca.connect(CAServer, 4999, null);
			ca.run();
			
			ca.disconnect();
			
			System.out.println("Connected to CA ");
			
			RSAPublicKey serverPublicKey = (RSAPublicKey)ca.getPublicKey();
			
			System.out.println("I got the public key " + serverPublicKey);
						
			/**** Sending Symmetric Key to Server *******/
			Envelope request = new Envelope("REQUEST_SECURE_CONNECTION");
			request.addObject(encryptChallenge(serverPublicKey));
			request.addObject(encryptKey(serverPublicKey));
			
			//return b.toByteArray(); 
			//byte[] byteArray = convertToByteArray(request);
			
			// Get the IV to use
//			IvParameterSpec IV = ivAES(IV_BYTES);
															
			
			//output.writeObject(RSAEncrypt("RSA/None/NoPadding", PROVIDER, serverPublicKey, byteArray));
			//output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
						
			output.writeObject(request);
						
			this.input = new ObjectInputStream(this.sock.getInputStream());
			
			try{
				
//				byte[] decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, (byte[])input.readObject());
				
//				Object convertedObj = convertToObject(decryptedMsg);
				
//				Envelope reqResponse = (Envelope)convertedObj;
				
				Envelope reqResponse = (Envelope)input.readObject();
				
				if (reqResponse.getMessage().equals("OK")){
					
					byte[] encryChallenge = (byte[])reqResponse.getObjContents().get(0); // Get the encrypted challenge
					IvParameterSpec ivFromServer = new IvParameterSpec((byte[])reqResponse.getObjContents().get(1));
					
					if (encryChallenge == null){
						
						// We should get out of here
						disconnect();
						System.out.println("Entry challenge is null failed.");
												
						
					} else {
												
						Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);

						// Get the IV to use
						
						// Initialize the cipher encryption object, add the key, and add the IV
						objCipher.init(Cipher.DECRYPT_MODE, SYMMETRIC_KEY, ivFromServer); 

						// Encrypt the data and store in encryptedData
						byte[] decryptedChallenge = objCipher.doFinal(encryChallenge);
						
						String originalChallenge = new String(challengeBytes);
						String returnedChallenge = new String(decryptedChallenge);
						
						if(originalChallenge.equals(returnedChallenge)){
							
							// Secure connection
							System.out.println("Success! Secure connection created!");
							
						} else {
							
							disconnect();
							System.out.println("Challenge is not equal");
						}
						
					}
										
					
				} else {
					
					// It failed
					
					disconnect();
					System.out.println("Request Response failed.");
					
				}
										
			
			} catch(Exception ex){
				
//				System.out.println(ex.toString());
				ex.printStackTrace();
				
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
		
//		System.out.println("Success!  Connected to " + server + " at port " + port);
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
				Envelope message = new Envelope("DISCONNECT");
				output.writeObject(message);
				this.output.close();
				this.input.close();
				this.sock.close();
			}
			catch (Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
			}
			this.sock = null;
		}
	}//end method disconnect
}// end class Client
