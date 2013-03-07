/* File worker thread handles the business of uploading, downloading, and removing files for clients with valid tokens */

import java.lang.Thread;
import java.net.Socket;
import java.security.Key;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The thread spawned by {@link FileServer} after accepting a connection.
 * This thread handles all envelopes coming to the server from a client.
 * 
 * @see FileClient
 */
public class FileThread extends Thread
{
	/**
	 * Class level variable specifying the socket.
	 */
	private final Socket socket;
	
	private final String PROVIDER = "BC";
	private final String ASYM_ALGORITHM = "RSA";
	
	//private RSAPublicKey publicKey;
	private RSAPrivateKey privateKey; 
	
	private Key SYMMETRIC_KEY; 
	private final String SYM_KEY_ALG = "AES/CTR/NoPadding";
	
	private static final int IV_BYTES = 16;
	
	
	
	
	/**
	 * This constructor initializes the socket class variable to the socket passed in as a parameter.
	 * 
	 * @param _socket The socket to be utilized by this object.
	 */
	public FileThread(Socket _socket)
	{
		socket = _socket;
	}
			
	
	private static IvParameterSpec ivAES(int ivBytes){

		// Random Number used for IV
		SecureRandom randomNumber = new SecureRandom();

		byte[] bytesIV = new byte[ivBytes];	
		randomNumber.nextBytes(bytesIV);

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
		}

		return null;
	}
	
	
	
	/**
	 * This method executes the processes associated with serving files such as list files, download a file, delete a file, etc...
	 */
	public void run()
	{
		boolean proceed = true;
		try
		{
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			Envelope response;
			
			// Get the IV to use
			IvParameterSpec IV = ivAES(IV_BYTES);
			
			do
			{
				
				
				byte[] decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, (byte[])input.readObject());
				
				Object convertedObj = convertToObject(decryptedMsg);
				
				Envelope e = (Envelope)convertedObj;		
				
				
				//Envelope e = (Envelope)input.readObject();
				System.out.println("Request received: " + e.getMessage());
				
				// Handler to list files that this user is allowed to see
				// RETURNS MESSAGE OK AND LIST OF FILES
				if (e.getMessage().equals("LFILES"))
				{
					/* TODO: Write this handler */
					/*
					 * Note: area of confusion - haveing trouble following the logic behind
					 * 1. FileServer.fileList.getFiles()...what list is this returning?
					 * 2. List<String> listFiles(UserToken token)...this will get all files for user? Where do we use it? *
					 */
					
					// (1) FileClient sends a message - new Envelope("LFILES"))
					// (2) Get the users token - (UserToken)e.getObjContents().get(2);
					// (3) Get groups user belongs to - List<String> groups = yourToken.getGroups();
					// (4) For each group of user, list all files relevant to group
					
					// (2) Get the token of the user
					UserToken uToken = (UserToken)e.getObjContents().get(0);
					// (3) Get the groups user is associated with
					List<String> groups = uToken.getGroups();
					
					// (4)
					List<ShareFile> files = FileServer.fileList.getFiles();
					List<String> visibleFiles = new ArrayList<String>();
					
					for (ShareFile f : files)
					{
						if (groups.contains(f.getGroup()))
						{
							visibleFiles.add(f.getPath());
						}
					}
					
					response = new Envelope("OK");
					response.addObject(visibleFiles);
					
					//return b.toByteArray(); 
					byte[] byteArray = convertToByteArray(response);
																	
					output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
					
					
					//output.writeObject(response);
				}// end if block
				else if (e.getMessage().equals("REQUEST_SECURE_CONNECTION"))// Client wants a token
				{
					byte[] encryptedChallenge = (byte[])e.getObjContents().get(0); // Get the encrypted challenge
					byte[] encryptedKey = (byte[])e.getObjContents().get(0); // Get the encrypted key
					if (encryptedChallenge == null || encryptedKey == null)
					{
						response = new Envelope("FAIL");
						response.addObject(null);
						
						//return b.toByteArray(); 
						byte[] byteArray = convertToByteArray(response);
																		
						output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
						
						//output.writeObject(response);
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
						
						//SYMMETRIC_KEY = new Key(decryptedKey);
											
						// Get the IV to use
						//IvParameterSpec IV = ivAES(IV_BYTES);
						
						//SecretKeySpec secretKeySpec 
						SYMMETRIC_KEY = new SecretKeySpec(decryptedKey, "AES");
						
						// Initialize the cipher encryption object, add the key, and add the IV
						objCipher.init(Cipher.ENCRYPT_MODE, SYMMETRIC_KEY, IV); 

						//byte[] dataToEncryptBytes = dataToEncrypt.getBytes();

						// Encrypt the data and store in encryptedData
						byte[] newEncryptedChallenge = objCipher.doFinal(decryptedChallenge);
																		
						// Respond to the client. On error, the client will receive a null token
						response = new Envelope("OK");
						
						response.addObject(newEncryptedChallenge);
						
						//return b.toByteArray(); 
						byte[] byteArray = convertToByteArray(response);
																		
						output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
						
						//output.writeObject(response);
											
					}
					
				}							
				else if (e.getMessage().equals("UPLOADF"))
				{
					if (e.getObjContents().size() < 3)
					{
						response = new Envelope("FAIL-BADCONTENTS");
					}
					else
					{
						if (e.getObjContents().get(0) == null)
						{
							response = new Envelope("FAIL-BADPATH");
						}
						if (e.getObjContents().get(1) == null)
						{
							response = new Envelope("FAIL-BADGROUP");
						}
						if (e.getObjContents().get(2) == null)
						{
							response = new Envelope("FAIL-BADTOKEN");
						}
						else
						{
							String remotePath = (String)e.getObjContents().get(0);
							String group = (String)e.getObjContents().get(1);
							UserToken yourToken = (UserToken)e.getObjContents().get(2); // Extract token
							
							if (FileServer.fileList.checkFile(remotePath))
							{
								System.out.printf("Error: file already exists at %s\n", remotePath);
								response = new Envelope("FAIL-FILEEXISTS"); // Success
							}
							else if (!yourToken.getGroups().contains(group))
							{
								System.out.printf("Error: user missing valid token for group %s\n", group);
								response = new Envelope("FAIL-UNAUTHORIZED"); // Success
							}
							else
							{
								File file = new File("shared_files/" + remotePath.replace('/', '_'));
								file.createNewFile();
								FileOutputStream fos = new FileOutputStream(file);
								System.out.printf("Successfully created file %s\n", remotePath.replace('/', '_'));
								
								response = new Envelope("READY"); // Success
								
								//return b.toByteArray(); 
								byte[] byteArray = convertToByteArray(response);
																				
								output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
								
								//output.writeObject(response);
								
								//e = (Envelope)input.readObject();
								
								decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, (byte[])input.readObject());
								
								convertedObj = convertToObject(decryptedMsg);
								
								e = (Envelope)convertedObj;	
								
								
								while (e.getMessage().compareTo("CHUNK") == 0)
								{
									fos.write((byte[])e.getObjContents().get(0), 0, (Integer)e.getObjContents().get(1));
									response = new Envelope("READY"); // Success
									
									//return b.toByteArray(); 
									byteArray = convertToByteArray(response);
																					
									output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
									
									//output.writeObject(response);
									
									decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, (byte[])input.readObject());
									
									convertedObj = convertToObject(decryptedMsg);
									
									//Envelope e = (Envelope)convertedObj;	
									
									e = (Envelope)convertedObj;
									
									//e = (Envelope)input.readObject();
								}
								
								if (e.getMessage().compareTo("EOF") == 0)
								{
									System.out.printf("Transfer successful file %s\n", remotePath);
									FileServer.fileList.addFile(yourToken.getSubject(), group, remotePath);
									response = new Envelope("OK"); // Success
								}
								else
								{
									System.out.printf("Error reading file %s from client\n", remotePath);
									response = new Envelope("ERROR-TRANSFER"); // Success
								}
								fos.close();
							}// end else block
						}// end else block
					}// end else block
					//return b.toByteArray(); 
					byte[] byteArray = convertToByteArray(response);
																	
					output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
					//output.writeObject(response);
				}// end else if block
				else if (e.getMessage().compareTo("DOWNLOADF") == 0)
				{
					String remotePath = (String)e.getObjContents().get(0);
					Token t = (Token)e.getObjContents().get(1);
					ShareFile sf = FileServer.fileList.getFile("/" + remotePath);
					if (sf == null)
					{
						System.out.printf("Error: File %s doesn't exist\n", remotePath);
						e = new Envelope("ERROR_FILEMISSING");
						
						byte[] byteArray = convertToByteArray(e);
						
						output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
						
					//	output.writeObject(e);
					}
					else if (!t.getGroups().contains(sf.getGroup()))
					{
						System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
						e = new Envelope("ERROR_PERMISSION");
						
						byte[] byteArray = convertToByteArray(e);
						
						output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
						
						//output.writeObject(e);
					}
					else
					{
						try
						{
							File f = new File("shared_files/_" + remotePath.replace('/', '_'));
							if (!f.exists())
							{
								System.out.printf("Error file %s missing from disk\n", "_" + remotePath.replace('/', '_'));
								e = new Envelope("ERROR_NOTONDISK");
								
								byte[] byteArray = convertToByteArray(e);
								
								output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
								
								//output.writeObject(e);
							}
							else
							{
								FileInputStream fis = new FileInputStream(f);
								do
								{
									byte[] buf = new byte[4096];
									if (e.getMessage().compareTo("DOWNLOADF") != 0)
									{
										System.out.printf("Server error: %s\n", e.getMessage());
										break;
									}
									e = new Envelope("CHUNK");
									int n = fis.read(buf); // can throw an IOException
									if (n > 0)
									{
										System.out.printf(".");
									}
									else if (n < 0)
									{
										System.out.println("Read error");
									}
									
									e.addObject(buf);
									e.addObject(new Integer(n));
																		
									byte[] byteArray = convertToByteArray(e);
									
									output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
									
									//output.writeObject(e);
									
									//e = (Envelope)input.readObject();
									
									decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, (byte[])input.readObject());
									
									convertedObj = convertToObject(decryptedMsg);
									
									e = (Envelope)convertedObj;
									
								} while (fis.available() > 0);
								
								// If server indicates success, return the member list
								if (e.getMessage().compareTo("DOWNLOADF") == 0)
								{
									
									e = new Envelope("EOF");
									
									byte[] byteArray = convertToByteArray(e);
									
									output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );								
									
									//output.writeObject(e);
									
									//e = (Envelope)input.readObject();
									
									decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, (byte[])input.readObject());
									
									convertedObj = convertToObject(decryptedMsg);
									
									e = (Envelope)convertedObj;
									
									
									if (e.getMessage().compareTo("OK") == 0)
									{
										System.out.printf("File data upload successful\n");
									}
									else
									{
										System.out.printf("Upload failed: %s\n", e.getMessage());
									}
								}// end if block
								else
								{
									System.out.printf("Upload failed: %s\n", e.getMessage());
								}
							}// end else block
						}// end try block
						catch (Exception e1)
						{
							System.err.println("Error: " + e.getMessage());
							e1.printStackTrace(System.err);
						}
					}// end else block
				}// end else if block
					// TODO replace all / with System.getProperty("path.separator")
				else if (e.getMessage().compareTo("DELETEF") == 0)
				{
					String remotePath = (String)e.getObjContents().get(0);
					Token t = (Token)e.getObjContents().get(1);
					ShareFile sf = FileServer.fileList.getFile("/" + remotePath);
					if (sf == null)
					{
						System.out.printf("Error: File %s doesn't exist\n", remotePath);
						e = new Envelope("ERROR_DOESNTEXIST");
					}
					else if (!t.getGroups().contains(sf.getGroup()))
					{
						System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
						e = new Envelope("ERROR_PERMISSION");
					}
					else
					{
						try
						{
							File f = new File("shared_files/" + "_" + remotePath.replace('/', '_'));
							
							if (!f.exists())
							{
								System.out.printf("Error file %s missing from disk\n", "_" + remotePath.replace('/', '_'));
								e = new Envelope("ERROR_FILEMISSING");
							}
							else if (f.delete())
							{
								System.out.printf("File %s deleted from disk\n", "_" + remotePath.replace('/', '_'));
								FileServer.fileList.removeFile("/" + remotePath);
								e = new Envelope("OK");
							}
							else
							{
								System.out.printf("Error deleting file %s from disk\n", "_" + remotePath.replace('/', '_'));
								e = new Envelope("ERROR_DELETE");
							}
						}// end try block
						catch (Exception e1)
						{
							System.err.println("Error: " + e1.getMessage());
							e1.printStackTrace(System.err);
							e = new Envelope(e1.getMessage());
						}
					}// end else block

					byte[] byteArray = convertToByteArray(e);
					
					output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
					
					//output.writeObject(e);
				}// end else if block
				else if (e.getMessage().equals("DISCONNECT"))
				{
					socket.close();
					proceed = false;
				}
			} while (proceed);
		}// end try block
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}// end method run()
}// end class FileThread
