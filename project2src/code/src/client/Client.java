package client;

import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
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
	
	protected static final String RSA_ALGORITHM = "RSA/None/NoPadding";
	private static final String SYM_ALGORITHM = "AES";
	protected static final String SYM_KEY_ALG = "AES/CTR/NoPadding";
	protected static final String PROVIDER = "BC";
	protected Key symmetricKey;
	private SecureRandom random;
	private static final int IV_BYTES = 16;
	private final String CAServer = "localhost";
	
	public Client()
	{
		this.random = new SecureRandom();
		this.symmetricKey = this.genterateSymmetricKey();
	}
	
	private byte[] encryptKey(RSAPublicKey pubKey)
	{
		return encryptPublic(pubKey, this.symmetricKey.getEncoded());
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
			KeyGenerator keyGenerator = KeyGenerator.getInstance(SYM_ALGORITHM, PROVIDER);
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

	protected IvParameterSpec ivAES()
	{
		byte[] bytesIV = new byte[IV_BYTES];	
		this.random.nextBytes(bytesIV);

		// Create the IV
		return new IvParameterSpec(bytesIV);
	}

	protected Envelope encryptMessageWithSymmetricKey(Object[] objs, String message)
	{
		try
		{
			Envelope response = new Envelope(message);
			Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);
			IvParameterSpec IV = ivAES();

			objCipher.init(Cipher.ENCRYPT_MODE, symmetricKey, IV); 

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

	protected byte[] decryptObjectBytes(byte[] objByte, byte[] iv)
	{
		try
		{
			Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);

			// Initialize the cipher encryption object, add the key, and add the IV
			objCipher.init(Cipher.DECRYPT_MODE, symmetricKey, new IvParameterSpec(iv)); 

			// Encrypt the data and store in encryptedData
			return objCipher.doFinal(objByte);
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
			//return b.toByteArray(); 
			return baos.toByteArray();
		}
		catch(Exception ex)
		{
			System.out.println("Error creating byte array envelope: " + ex.toString());
			ex.printStackTrace();
		}
		return null;
	}

	protected static Object convertToObject(byte[] bytesToConvert)
	{
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(bytesToConvert);
			ObjectInputStream ois = new ObjectInputStream(bais);
			return ois.readObject();
		}
		catch(Exception ex)
		{
			System.out.println("Error byte array to object: " + ex.toString());
			ex.printStackTrace();
		}
		return null;
	}
	
//	/**
//	 * This method will decrypt the data.
//	 * @param algorithm The algorithm to use.
//	 * @param provider The security provider.
//	 * @param key The symmetric key to use.
//	 * @param iv The IV to use for decryption.
//	 * @param dataToDecrypt The data to decrypt.
//	 * @return byte[] array of decrypted data.
//	 */
//	protected static byte[] AESDecrypt(String algorithm, String provider, Key key, IvParameterSpec iv, byte[] dataToDecrypt){
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
//	
//	
//	/**
//	 * This method will encrypt the data utilizing the public key.
//	 * @param algorithm The algorithm to use.
//	 * @param provider The security provider to use.
//	 * @param pubKey The public key to use.
//	 * @param dataToEncrypt The data to encrypt.
//	 * @return byte[] array of the encrypted data.
//	 */
//	protected static byte[] RSAEncrypt(String algorithm, String provider, RSAPublicKey pubKey, byte[] dataToEncrypt){
//
//		try{
//
//			// Create the cipher object
//			Cipher objCipher = Cipher.getInstance(algorithm, provider);
//
//			// Initialize the cipher encryption object, add the key 
//			objCipher.init(Cipher.ENCRYPT_MODE, pubKey); 
//
//			///byte[] dataToEncryptBytes = dataToEncrypt.getBytes();
//
//			// Encrypt the data and store in encryptedData
//			return objCipher.doFinal(dataToEncrypt);
//
//		} catch(Exception ex){
//			System.out.println(ex.toString());
//			ex.printStackTrace();
//		}
//
//		return null;
//	}
	
	public boolean connect(final String server, final int port, final String serverName)
	{
		return this.connect(server, port, serverName, null);
	}
	
	// javadoc already handled by ClientInterface
	// groupServerName is for the FileServer to retrieve the group server's public key.  It is ignored otherwise.
	public boolean connect(final String server, final int port, final String serverName, final String groupServerName)
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
			this.input = new ObjectInputStream(this.sock.getInputStream());
			
			System.out.println("Connecting to CA");
			CAClient ca = new CAClient(serverName);
			ca.connect(CAServer, 4999, null);
			ca.run();
			ca.disconnect();
			RSAPublicKey serverPublicKey = (RSAPublicKey)ca.getPublicKey();
			
//			System.out.println("I got the public key " + serverPublicKey);
			
			System.out.println("Setting up connection to the Server");
			/* Sending Challenge and Symmetric Key to Server *******/
			Envelope request = new Envelope("REQUEST_SECURE_CONNECTION");
			
			byte[] challengeBytes = new byte[20];
			this.random.nextBytes(challengeBytes); //SYMMETRIC_KEY.getEncoded(); //concatenation.getBytes();
			
			request.addObject(encryptPublic(serverPublicKey, challengeBytes));
			request.addObject(encryptKey(serverPublicKey));
			if(groupServerName != null)
			{
				request.addObject(encryptPublic(serverPublicKey, groupServerName.getBytes()));
			}
			this.output.writeObject(request);
		
			Envelope reqResponse = (Envelope)this.input.readObject();
			if (reqResponse.getMessage().equals("OK"))
			{
				byte[] encryChallenge = (byte[])reqResponse.getObjContents().get(0); // Get the encrypted challenge
				IvParameterSpec ivFromServer = new IvParameterSpec((byte[])reqResponse.getObjContents().get(1));

				if (encryChallenge == null)
				{
					// We should get out of here
					disconnect();
					System.out.println("Entry challenge is null failed.");
				}
				else
				{					
					Cipher objCipher = Cipher.getInstance(SYM_KEY_ALG, PROVIDER);

					// Initialize the cipher encryption object, add the key, and add the IV
					objCipher.init(Cipher.DECRYPT_MODE, symmetricKey, ivFromServer); 

					// Encrypt the data and store in encryptedData
					byte[] decryptedChallenge = objCipher.doFinal(encryChallenge);

					String originalChallenge = new String(challengeBytes);
					String returnedChallenge = new String(decryptedChallenge);

					if(originalChallenge.equals(returnedChallenge))
					{
						// Secure connection
						System.out.println("Success! Secure connection created!");

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
		catch(InvalidAlgorithmParameterException e)
		{
			e.printStackTrace();
			return false;
		}
		catch(InvalidKeyException e)
		{
			e.printStackTrace();
			return false;
		}
		catch(NoSuchPaddingException e)
		{
			e.printStackTrace();
			return false;
		}
		catch(BadPaddingException e)
		{
			e.printStackTrace();
			return false;
		}
		catch(NoSuchProviderException e)
		{
			e.printStackTrace();
			return false;
		}
		catch(IllegalBlockSizeException e)
		{
			e.printStackTrace();
			return false;
		}
		catch(NoSuchAlgorithmException e)
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
