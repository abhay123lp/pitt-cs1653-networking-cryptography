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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import message.Envelope;

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
	
	protected final RSAPublicKey publicKey;
	protected final RSAPrivateKey privateKey;
	
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
	}
	
	public abstract void run();
	
	protected IvParameterSpec ivAES()
	{
		byte[] bytesIV = new byte[IV_BYTES];	
		this.random.nextBytes(bytesIV);

		// Create the IV
		return new IvParameterSpec(bytesIV);

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
	
	protected Envelope encryptResponseWithSymmetricKey(Object[] objs, String message){
		
		try{
			
		Envelope response = new Envelope(message);
		
		Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);
					
		IvParameterSpec IV = ivAES();
		
		// Initialize the cipher encryption object, add the key, and add the IV
		objCipher.init(Cipher.ENCRYPT_MODE, this.symmetricKey, IV); 

		//byte[] dataToEncryptBytes = dataToEncrypt.getBytes();

		for(Object o : objs){
			
			byte[] newEncryptedChallenge = objCipher.doFinal(convertToByteArray(o));	
			response.addObject(newEncryptedChallenge);
			
		}
		response.addObject(IV.getIV());
																
		return response;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	protected byte[] decryptObjects(byte[] objByte, byte[] iv)  {
		
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
	
}
