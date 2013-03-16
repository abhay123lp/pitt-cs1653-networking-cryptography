package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import message.Envelope;
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
		Envelope env = this.encryptResponseWithSymmetricKey(new Object[]{remotePath,  token}, "DELETEF");
		try
		{
			output.writeObject(env);
			env = (Envelope)input.readObject();
			
			if (env.getMessage().compareTo("OK") == 0)
			{
				System.out.printf("File %s deleted successfully\n", filename);
			}
			else
			{
				System.out.printf("Error deleting file %s (%s)\n", filename, env.getMessage());
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
				output.writeObject(this.encryptResponseWithSymmetricKey(new Object[]{sourceFile, token}, "DOWNLOADF"));
				
				Envelope env = (Envelope)input.readObject();
				
				// Why can't you just use .equals()?
				while (env.getMessage().compareTo("CHUNK") == 0)
				{
					fos.write((byte[])env.getObjContents().get(0), 0, (Integer)env.getObjContents().get(1));
					System.out.printf(".");
					env = new Envelope("DOWNLOADF"); // Success
					output.writeObject(env);
					env = (Envelope)input.readObject();
				}
				fos.close();
				
				// again, why not .equals()?
				if (env.getMessage().compareTo("EOF") == 0)
				{
					// FIXME fos is closed already...
					fos.close();
					System.out.printf("\nTransfer successful file %s\n", sourceFile);
					env = new Envelope("OK"); // Success
					output.writeObject(env);
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
			output.writeObject(this.encryptResponseWithSymmetricKey(new Object[]{token}, "LFILES"));
			
			Envelope e = (Envelope)input.readObject();
			
			// If server indicates success, return the member list
			if (e.getMessage().equals("OK"))
			{
				return (List<String>)e.getObjContents().get(0); // This cast creates compiler warnings. Sorry.
			}
			return null;
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}// end method listFiles(UserToken)
	
	public boolean upload(String sourceFile, String destFile, String group, UserToken token)
	{
		if (destFile.charAt(0) != '/')
		{
			destFile = "/" + destFile;
		}
		
		try
		{
//			Envelope message = null, env = null;
//			// Tell the server to return the member list
//			message = new Envelope("UPLOADF");
//			message.addObject(destFile);
//			message.addObject(group);
//			message.addObject(token); // Add requester's token
			output.writeObject(this.encryptResponseWithSymmetricKey(new Object[]{destFile, group, token}, "UPLOADF"));
			
			FileInputStream fis = new FileInputStream(sourceFile); // FIXME never closed
			
			Envelope env = (Envelope)input.readObject();
			
			// If server indicates success, return the member list
			if (env.getMessage().equals("READY"))
			{
				System.out.printf("Meta data upload successful\n");
				
			}
			else
			{
				
				System.out.printf("Upload failed: %s\n", env.getMessage());
				return false;
			}
			
			do
			{
				byte[] buf = new byte[4096];
				if (env.getMessage().compareTo("READY") != 0)
				{
					System.out.printf("Server error: %s\n", env.getMessage());
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
					return false;
				}
				
//				Envelope message = new Envelope("CHUNK");
//				message.addObject(buf);
//				message.addObject(new Integer(n));
				
				output.writeObject(this.encryptResponseWithSymmetricKey(new Object[]{buf, new Integer(n)}, "CHUNK"));
				
//				env = (Envelope)input.readObject();	//FIXME is this supposed to happen twice?
			} while (fis.available() > 0);
			
			// If server indicates success, return the member list
			if (env.getMessage().compareTo("READY") == 0)
			{
				Envelope message = new Envelope("EOF");
				output.writeObject(message);
				
				env = (Envelope)input.readObject();
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
			System.err.println("Error: " + e1.getMessage());
			e1.printStackTrace(System.err);
			return false;
		}
		return true;
	}// end method upload(String, String, UserToken)
}// end class File
