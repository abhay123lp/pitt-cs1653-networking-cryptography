package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import message.Envelope;
import message.Field;
import message.UserToken;

/**
 * Handles connections to the {@link FileServer}.
 * Note however that the FileClient does not know who it is connecting to, but will assume that the server understands the protocol.
 */
public class FileClient extends Client implements FileInterface, ClientInterface
{
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
//		Envelope env = new Envelope("DELETEF"); // Success
//		env.addObject(remotePath);
//		env.addObject(token);
//		Envelope env = this.encryptMessageWithSymmetricKey(new Object[]{remotePath,  token}, "DELETEF");
		try
		{
			output.writeObject(this.encryptMessageWithSymmetricKey("DELETEF", token, new Object[]{remotePath}));
			//output.writeObject(this.encryptMessageWithSymmetricKey(new Object[]{remotePath,  token}, "DELETEF"));
			
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
	
	public boolean download(String sourceFile, String destFile, UserToken token)
	{
		if (sourceFile.charAt(0) == '/')
		{
			sourceFile = sourceFile.substring(1);
		}
		
		File file = new File(destFile);
		try
		{
			if (!file.exists())
			{
				file.createNewFile();
				FileOutputStream fos = new FileOutputStream(file);
				
//				Envelope env = new Envelope("DOWNLOADF"); // Success
//				env.addObject(sourceFile);
//				env.addObject(token);
				//output.writeObject(this.encryptMessageWithSymmetricKey(new Object[]{sourceFile, token}, "DOWNLOADF"));
				output.writeObject(this.encryptMessageWithSymmetricKey("DOWNLOADF", token, new Object[]{sourceFile}));
				
				Envelope env = (Envelope)input.readObject();
				if(!checkValidityOfMessage(env))
				{
					fos.close();
					return false;//TODO bad handling
				}
				//new Envelope("DOWNLOADF");
				
				// Why can't you just use .equals()?
				while (env.getMessage().compareTo("CHUNK") == 0)
				{
					Object[] objData = (Object[])getFromEnvelope(Field.DATA);
					
//					byte[] iv = (byte[])getFromEnvelope(Field.IV);
							//(byte[])env.getObjContents().get(2);
					byte[] inBytes = (byte[])objData[0];
							//(byte[])convertToObject(decryptObjectBytes((byte[])env.getObjContents().get(0), iv));
					Integer lastIndex = (Integer)objData[1];
							//(Integer)convertToObject(decryptObjectBytes((byte[])env.getObjContents().get(1), iv));
					fos.write(inBytes, 0, lastIndex);
					System.out.printf(".");
//					env = new Envelope("DOWNLOADF"); // Success
//					downloadMore = this.encryptMessageWithSymmetricKey("DOWNLOADF", null, null); 
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
					// FIXME fos is closed already...
//					fos.close();
					System.out.printf("\nTransfer successful file %s\n", sourceFile);
//					env = new Envelope("OK"); // Success
					output.writeObject(this.encryptMessageWithSymmetricKey("OK", null, null));
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
				System.out.printf("Error couldn't create file %s\n", destFile); // BECAUSE FILE EXISTS (sucky message)
				return false;
			}
		}// end try block
		catch (IOException e1)
		{
			System.out.printf("Error couldn't create file %s\n", destFile);
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
//			Envelope message = null, e = null;
			// Tell the server to return the member list
//			message = new Envelope("LFILES");
//			message.addObject(token); // Add requester's token
			output.writeObject(this.encryptMessageWithSymmetricKey("LFILES", token, null));
			//output.writeObject(this.encryptMessageWithSymmetricKey(new Object[]{token}, "LFILES"));
			
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
						//(List<String>)convertToObject(decryptObjectBytes((byte[])e.getObjContents().get(0), (byte[])e.getObjContents().get(1))); // This cast creates compiler warnings. Sorry.
			}
			return null;
		}
		catch (Exception e)
		{
//			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}// end method listFiles(UserToken)
	
	public boolean upload(String sourceFile, String destFile, String group, UserToken token)
	{
		if (sourceFile.charAt(0) == '/')
		{
			sourceFile = sourceFile.substring(1);
		}
		
		try
		{
//			Envelope message = null, env = null;
//			// Tell the server to return the member list
//			message = new Envelope("UPLOADF");
//			message.addObject(destFile);
//			message.addObject(group);
//			message.addObject(token); // Add requester's token
			
			output.writeObject(this.encryptMessageWithSymmetricKey("UPLOADF", token, new Object[]{destFile, group}));
			
			//output.writeObject(this.encryptMessageWithSymmetricKey(new Object[]{destFile, group, token}, "UPLOADF"));
			
			FileInputStream fis = new FileInputStream(sourceFile); // FIXME never closed
			
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
				
//				Envelope message = new Envelope("CHUNK");
//				message.addObject(buf);
//				message.addObject(new Integer(n));
				
				output.writeObject(this.encryptMessageWithSymmetricKey("CHUNK", token, new Object[]{buf, new Integer(n)}));
				//output.writeObject(this.encryptMessageWithSymmetricKey(new Object[]{buf, new Integer(n)}, "CHUNK"));
				
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
				Envelope message = this.encryptMessageWithSymmetricKey("EOF", null, null); 
						//new Envelope("EOF");
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
//			System.err.println("Error: " + e1.getMessage());
			e1.printStackTrace(System.err);
			return false;
		}
		return true;
	}// end method upload(String, String, UserToken)
}// end class File
