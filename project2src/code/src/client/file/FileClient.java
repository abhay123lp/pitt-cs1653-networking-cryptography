package client.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.SecureRandom;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;

import client.Client;
import client.ClientInterface;
import client.group.GroupClient;

import message.Envelope;
import message.Field;
import message.UserToken;

/**
 * Handles connections to the {@link FileServer}.
 * Note however that the FileClient does not know who it is connecting to, but will assume that the server understands the protocol.
 */
public class FileClient extends Client implements FileInterface, ClientInterface
{
	private static final String SYM_KEY_ALG = "AES/CTR/NoPadding";
	private static final int IV_BYTES = 16;
	
	private String tempFile = "temp_3m9ectnectwxnrwxn";
	private String fileExtension = "";
	
	public boolean delete(String filename, UserToken token)
	{
		String remotePath;
		if (filename.charAt(0) == '/')
		{
			remotePath = filename.substring(1);
		}
		else
		{
			remotePath = filename;
		}
		try
		{
			output.writeObject(this.encryptMessageWithSymmetricKey("DELETEF", token, new Object[]{remotePath}));			
			Envelope response = (Envelope)input.readObject();
			if(!checkValidityOfMessage(response))
			{
				return false; //TODO bad handling
			}
			
			if (response.getMessage().compareTo("OK") == 0)
			{
				System.out.printf("File %s deleted successfully\n", filename);
			}
			else
			{
				System.out.printf("Error deleting file %s (%s)\n", filename, response.getMessage());
				return false;
			}
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		catch (ClassNotFoundException e1)
		{
			e1.printStackTrace();
		}
		return true;
	}// end method delete
	
	public boolean download(String sourceFile, String destFile, UserToken token, GroupClient groupClient)
	{
		if (sourceFile.charAt(0) == '/')
		{
			sourceFile = sourceFile.substring(1);
		}
		
		File file = new File(tempFile);
		try
		{
			if (!file.exists())
			{
				file.createNewFile();
				FileOutputStream fos = new FileOutputStream(file);
				
				//Success
				output.writeObject(this.encryptMessageWithSymmetricKey("DOWNLOADF", token, new Object[]{sourceFile}));
				
				Envelope env = (Envelope)input.readObject();
				if(!checkValidityOfMessage(env))
				{
					fos.close();
					return false;//TODO bad handling
				}
				if(!env.getMessage().equals("METADATA"))
				{
					System.out.println("Server did not give metadata... something bad happened...");
					fos.close();
					return false; //TODO bad handling
				}
				Object[] metaData = (Object[])this.getFromEnvelope(Field.DATA);
				String groupName = (String)metaData[0];
				Integer epoch = (Integer)metaData[1];
				
				output.writeObject(this.encryptMessageWithSymmetricKey("DOWNLOADF", token, new Object[]{sourceFile}));
				env = (Envelope)input.readObject();
				if(!checkValidityOfMessage(env))
				{
					fos.close();
					return false;//TODO bad handling
				}
				
				// Why can't you just use .equals()?
				while (env.getMessage().compareTo("CHUNK") == 0)
				{
					Object[] objData = (Object[])getFromEnvelope(Field.DATA);
					
					byte[] inBytes = (byte[])objData[0];
					Integer lastIndex = (Integer)objData[1];
					fos.write(inBytes, 0, lastIndex);
					System.out.printf(".");
					
					//Success, want to continue
					output.writeObject(this.encryptMessageWithSymmetricKey("DOWNLOADF", null, null));
					env = (Envelope)input.readObject();
					if(!checkValidityOfMessage(env))
					{
						fos.close();
						return false; //TODO bad handling
					}
				}
				fos.close();
				
				// again, why not .equals()?
				if (env.getMessage().compareTo("EOF") == 0)
				{
					System.out.printf("\nTransfer successful file %s\n", sourceFile);
					
					//Success
					output.writeObject(this.encryptMessageWithSymmetricKey("OK", null, null));
					
					//now decrypt the file
					decrypt(tempFile, destFile, groupClient.getKey(groupName, epoch));
					deleteFile(tempFile);
				}
				else
				{
					System.out.printf("Error reading file %s (%s)\n", sourceFile, env.getMessage());
					file.delete();
					return false;
				}
			}// end if block
			else
			{
				System.out.printf("Error couldn't create file %s\n", tempFile); // BECAUSE FILE EXISTS (sucky message)
				return false;
			}
		}// end try block
		catch (IOException e1)
		{
			System.out.printf("Error couldn't create file %s\n", tempFile);
			return false;
		}
		catch (ClassNotFoundException e1)
		{
			e1.printStackTrace();
		}
		return true;
	}// end method download(String, String, UserToken)
	
	@SuppressWarnings("unchecked")
	public List<String> listFiles(UserToken token)
	{
		try
		{
			output.writeObject(this.encryptMessageWithSymmetricKey("LFILES", token, null));
			
			Envelope e = (Envelope)input.readObject();
			if(!checkValidityOfMessage(e))
			{
				return null;
			}
			
			// If server indicates success, return the member list
			if (e.getMessage().equals("OK"))
			{
				Object[] objData = (Object[])getFromEnvelope(Field.DATA);
				return (List<String>)objData[0];
			}
			return null;
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			return null;
		}
	}// end method listFiles(UserToken)
	
	/*
	 * 1) Make temp file
	 * 2) Copy sourcefile to temp file. 
	 * 3) Encrypt temp file with 10 rounds of AES.
	 * 4) Delete the temp file after sending it off to file server.
	 */
	public boolean upload(String sourceFile, String destFile, String group, UserToken token, Key aesKey, int epoch)
	{
		if (sourceFile.charAt(0) == '/')
		{
			sourceFile = sourceFile.substring(1);
		}
		
		try
		{
			/*
			 * We aren't actually uploading sourceFile anymore -- there is going to be an encrypted temp file
			 * The source file gets encrypted into the temp file. We upload the temp file as the source file.
			 */
			
			fileExtension = "";
			if(sourceFile.contains("."))
			{
				int index = sourceFile.indexOf('.');
				fileExtension = sourceFile.substring(index, sourceFile.length());				
			}
			// tempfile is a global variable			
			encrypt(sourceFile, tempFile + fileExtension, aesKey);
			// Now we have the encrypted sourcefile stored in the tempFile
			// Upload the ecnrypted tempFile (source file)
			output.writeObject(this.encryptMessageWithSymmetricKey("UPLOADF", token, new Object[]{destFile, group, epoch}));
			// delete the temp file
			
			FileInputStream fis = new FileInputStream(tempFile + fileExtension);
						
			Envelope env = (Envelope)input.readObject();
			if(!checkValidityOfMessage(env))
			{
				fis.close();
				return false;
			}
			
			// If server indicates success, return the member list
			if (env.getMessage().equals("READY"))
			{
				System.out.printf("Meta data upload successful\n");
				
			}
			else
			{
				
				System.out.printf("Upload failed: %s\n", env.getMessage());
				fis.close();
				return false;
			}
			
			do
			{
				byte[] buf = new byte[4096];
				if (env.getMessage().compareTo("READY") != 0)
				{
					System.out.printf("Server error: %s\n", env.getMessage());
					fis.close();
					return false;
				}
				int n = fis.read(buf); // can throw an IOException
				if (n > 0)
				{
					System.out.printf(".");
				}
				else if (n < 0)
				{
					System.out.println("Read error");
					fis.close();
					return false;
				}
				
				output.writeObject(this.encryptMessageWithSymmetricKey("CHUNK", token, new Object[]{buf, new Integer(n)}));
				
				env = (Envelope)input.readObject();
				if(!checkValidityOfMessage(env))
				{
					fis.close();
					return false;
				}
			} while (fis.available() > 0);
			
			fis.close();
			
			// If server indicates success, return the member list
			if (env.getMessage().compareTo("READY") == 0)
			{
				deleteFile(tempFile + fileExtension);
				Envelope message = this.encryptMessageWithSymmetricKey("EOF", null, null); 
				output.writeObject(message);
				
				env = (Envelope)input.readObject();
				if(!checkValidityOfMessage(env))
				{
					return false;
				}
				if (env.getMessage().compareTo("OK") == 0)
				{
					System.out.printf("\nFile data upload successful\n");
				}
				else
				{
					System.out.printf("\nUpload failed: %s\n", env.getMessage());
					return false;
				}
			}// end if block
			else
			{
				System.out.printf("Upload failed: %s\n", env.getMessage());
				return false;
			}
		}// end try block
		catch (Exception e1)
		{
			e1.printStackTrace(System.err);
			return false;
		}
		return true;
	}// end method upload(String, String, UserToken)
	
	/**
	 * Use this method to encrypt with the provided AES key.
	 * @param inputFile
	 * @param outputFile
	 */
	private void encrypt(String inputFile, String outputFile, Key AESkey)
	{
		try
		{
			BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(inputFile));
			BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
			encrypt(inputStream, outputStream, AESkey);
			inputStream.close();
			outputStream.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Use this method to decrypt with the provided AES key.
	 * @param inputFile
	 * @param outputFile
	 */
	private boolean decrypt(String inputFile, String outputFile, Key AESkey)
	{
		if(AESkey == null)
		{
			System.out.println("Decryption Key cannot be null.");
			return false;
		}
		try
		{
			BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(inputFile));
			BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
			decrypt(inputStream, outputStream, AESkey);
			inputStream.close();
			outputStream.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private boolean encrypt(InputStream inputStream, OutputStream outputStream, Key aesKey) 
	{
		if(aesKey == null)
		{
			System.out.println("Encryption Key cannot be null.");
			return false;
		}
		try
		{
			SecureRandom random = new SecureRandom();
			byte[] iv = new byte[IV_BYTES];
			random.nextBytes(iv);
			outputStream.write(iv);
			outputStream.flush();	
			Cipher cipher = Cipher.getInstance(SYM_KEY_ALG);
			IvParameterSpec ivSpec = new IvParameterSpec(iv);    	
			cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
			outputStream = new CipherOutputStream(outputStream, cipher);
			byte[] buf = new byte[4096];
			int numRead = 0;
			while ((numRead = inputStream.read(buf)) >= 0) 
			{
				outputStream.write(buf, 0, numRead);
			}
			outputStream.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void decrypt(InputStream inputStream, OutputStream outputStream, Key aesKey)
	{
		try
		{
			byte[] initV = new byte[IV_BYTES];
			inputStream.read(initV);
	
			Cipher cipher = Cipher.getInstance(SYM_KEY_ALG);
			IvParameterSpec ivSpec = new IvParameterSpec(initV);
			cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec); 
	
			inputStream = new CipherInputStream(inputStream, cipher);
			byte[] buffer = new byte[4096];
			int readNum = 0;
			while ((readNum = inputStream.read(buffer)) >= 0) 
			{
				outputStream.write(buffer, 0, readNum);
			}
			outputStream.close();			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void deleteFile(String filepath)
	{
		File file = new File(filepath);
		try
		{
			if(file.delete())
				System.out.println("Successfully deleted " + filepath);
			else
				System.out.println("Unable to delete " + filepath);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}// end class File
