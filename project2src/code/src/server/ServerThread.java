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
import java.util.List;

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
	protected static final String SYM_KEY_ALG = "AES/CTR/NoPadding";
	protected static final String PROVIDER = "BC";
	protected static final String ASYM_ALGORITHM = "RSA";
	private static final int IV_BYTES = 16;
	private static final String HMAC_ALGORITHM = "HmacSHA1";
	
	/**** MESSAGE COUNTER ****/
	private int numberOfMessage;
	
	protected final RSAPublicKey publicKey;
	protected final RSAPrivateKey privateKey;
	
	private RSAPublicKey groupServerPublicKey;
	
	private static final String CA_LOC = "localhost";
	private static final int CA_PORT = 4999;
	
	protected final Socket socket;
	
	private final SecureRandom random;
	
	private Key symmetricKey;

	/**
	 * 
	 */
	public ServerThread(Socket socket, RSAPublicKey publicKey, RSAPrivateKey privateKey)
	{
		this.socket = socket;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.random = new SecureRandom();
		this.symmetricKey = null;
		this.groupServerPublicKey = null;
	}
	
	public abstract void run();
	
	protected IvParameterSpec ivAES()
	{
		byte[] bytesIV = new byte[IV_BYTES];	
		this.random.nextBytes(bytesIV);

		// Create the IV
		return new IvParameterSpec(bytesIV);

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
	
	/**
	 * This method will generate a symmetric key for use.
	 * @param algorithm The encryption algorithm to use
	 * @param provider The security provider
	 * @return Key The key generated for symmetric cryptography
	 */
	private final Key genterateSymmetricKey()
	{
		try
		{
			// Random Number used to generate key
			SecureRandom randomNumber = new SecureRandom();	

			// Generate a 128-bit AES Key with Bouncy Castle provider
			KeyGenerator keyGenerator = KeyGenerator.getInstance(SYM_KEY_ALG, PROVIDER);
			keyGenerator.init(128, randomNumber);
			Key key = keyGenerator.generateKey();

			return key;
		}
		catch (Exception ex)
		{
			System.out.println(ex.toString());
			ex.printStackTrace();
		}
		return null;
	}
	
	
	private byte[] createHMAC(Key keyIntegrity, byte[] msgData){
		
		try {
			
			Mac HMAC = Mac.getInstance(HMAC_ALGORITHM, PROVIDER);
			Key keyHMAC = new SecretKeySpec(keyIntegrity.getEncoded(), HMAC_ALGORITHM);

			/**** Can I pass in arraylist and do HMAC.update() in a loop to add the data??? ****/
			
			HMAC.init(keyHMAC);
			
			return HMAC.doFinal(msgData);
				
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return null;
		
	}
		
	protected Envelope encryptMessageWithSymmetricKey(String message, UserToken token, Object[] data){
	//protected Envelope encryptMessageWithSymmetricKey(Object[] objs, String message){
		
		try{
			
		Envelope response = new Envelope(message);
		
		Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);
					
		IvParameterSpec IV = ivAES();
		
		// Initialize the cipher encryption object, add the key, and add the IV
		objCipher.init(Cipher.ENCRYPT_MODE, this.symmetricKey, IV); 

		//byte[] dataToEncryptBytes = dataToEncrypt.getBytes();
		
		List<byte[]> alHMAC = new ArrayList<byte[]>();
		
		for(Field f : Field.values()){
									
			switch(f){
			
			case HMAC:
				// HMAC
				// Calculate HMAC
				
				int countOfData = data.length;
				byte[] dataArray = new byte[countOfData + 3];
				
				// (1) Convert message to bytes
				byte[] msgBytes = convertToByteArray(message);
				alHMAC.add(msgBytes);
				
				// (2) Convert IV to bytes
				byte[] ivBytes = convertToByteArray(IV);
				alHMAC.add(ivBytes);
				
				// (3)  Convert Integer to bytes				
				byte[] msgNumBytes = convertToByteArray(numberOfMessage);
				alHMAC.add(msgNumBytes);
				
				// (4) Convert data to bytes, add to arraylist of byte[]
				for(int i = 0; i < data.length; i++){
					
					alHMAC.add(convertToByteArray(data[i]));
										
				}
				
				
				/****** NOTE NEED TO SEND INTEGRITY KEY TO CALCULATE HMAC ******/				
				Key symIntegrityKey = genterateSymmetricKey();
				// Add the encrypted symmetric key for the integrity check
				
				response.addObject(objCipher.doFinal(convertToByteArray(symIntegrityKey)));
				// Add the HMAC 
				
				//byte[] dataBytes = alHMAC.toArray(new byte[alHMAC.size()]);
				/******* ISSUE *******/
				response.addObject(createHMAC(symIntegrityKey, alHMAC.toArray()));
				
				break;
				
			case IV:
				// IV
				// Add the encrypted IV to the envelope
				response.addObject(objCipher.doFinal(convertToByteArray(IV)));
				
				break;
				
			case INT:
				// Integer
				response.addObject(objCipher.doFinal(convertToByteArray(numberOfMessage)));
				
				break;
				
			case TOKEN:
				// Token
				response.addObject(objCipher.doFinal(convertToByteArray(token)));
				break;
				
			case DATA:
				// Data
				
				// (4) Convert data to bytes, add to arraylist of byte[]
				for(int i = 0; i < data.length; i++){
					
					response.addObject(objCipher.doFinal(convertToByteArray(data[i])));
										
				}
				
				
				break;
			
			}
			
			
		}

		//for(Object o : objs){
			
			//byte[] newEncryptedChallenge = objCipher.doFinal(convertToByteArray(o));	
			//response.addObject(newEncryptedChallenge);
			
		//}
		//response.addObject(IV.getIV());
																
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
			objCipher.init(Cipher.DECRYPT_MODE, this.symmetricKey, new IvParameterSpec(iv)); 

			// Encrypt the data and store in encryptedData
			return objCipher.doFinal(objByte);

		} catch(Exception ex){
			ex.printStackTrace();
		}

		return null;
	}
	
	protected Envelope setUpSecureConnection(Envelope requestSecureConnection)
	{
		byte[] encryptedChallenge = (byte[])requestSecureConnection.getObjContents().get(0); // Get the encrypted challenge
		byte[] encryptedKey = (byte[])requestSecureConnection.getObjContents().get(1); // Get the encrypted key
		byte[] encryptedGroupServerName = null;
		if(requestSecureConnection.getObjContents().size() > 2)
		{
			encryptedGroupServerName = (byte[])requestSecureConnection.getObjContents().get(2); //get the encrypted server name
		}
		Envelope response = null;
		try
		{
			if (encryptedChallenge == null || encryptedKey == null)
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

				byte[] decryptedKey = objCipher.doFinal(encryptedKey);
				
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
				this.symmetricKey = new SecretKeySpec(decryptedKey, "AES");

				objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);

				// Get the IV to use
				IvParameterSpec IV = ivAES();

				// Initialize the cipher encryption object, add the key, and add the IV
				objCipher.init(Cipher.ENCRYPT_MODE, this.symmetricKey, IV); 

				// Encrypt the data and store in encryptedData
				byte[] newEncryptedChallenge = objCipher.doFinal(decryptedChallenge);

				// Respond to the client. On error, the client will receive a null token
				response = new Envelope("OK");

				response.addObject(newEncryptedChallenge);
				response.addObject(IV.getIV());
			}
		}
		catch(NoSuchAlgorithmException e)
		{
			e.printStackTrace();
			response = null;
		}
		catch(InvalidAlgorithmParameterException e)
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
	
	//FOR USE ONLY WITH FILETHREAD
	protected boolean verifyTokenSignature(UserToken t)
	{
		return t.RSAVerifySignature("SHA1withRSA", PROVIDER, this.groupServerPublicKey);
	}
}
