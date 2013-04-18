package server.file;

/* File worker thread handles the business of uploading, downloading, and removing files for clients with valid tokens */
import java.net.Socket;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import server.ServerThread;

import message.Envelope;
import message.Field;
import message.ShareFile;
import message.UserToken;

/**
 * The thread spawned by {@link FileServer} after accepting a connection.
 * This thread handles all envelopes coming to the server from a client.
 * 
 * @see FileClient
 */
public class FileThread extends ServerThread
{
	/**
	 * This constructor initializes the socket class variable to the socket passed in as a parameter.
	 * 
	 * @param _socket The socket to be utilized by this object.
	 */
	public FileThread(Socket _socket, RSAPrivateKey _privKey, RSAPublicKey _pubKey, String fileServerName, String ipAddress, int portNumber)
	{
		super(_socket, _pubKey, _privKey, fileServerName, ipAddress, portNumber);
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
			do
			{
				Envelope e = (Envelope)input.readObject();
				System.out.println("Request received: " + e.getMessage());
				Envelope response = null;
				
				if(e.getMessage().equals("REQUEST_SECURE_CONNECTION"))
				{	
					output.writeObject(this.setUpSecureConnection(e));
					continue;	
				}
				else if(e.getMessage().equals("HASH_CHALLENGE"))
				{
					output.writeObject(this.afterHashInversionChallenge(e));
					this.socket.setSoTimeout(0);
					continue;
				}
				else
				{
							
					if(!checkValidityOfMessage(e)){
						response = encryptMessageWithSymmetricKey("DISCONNECT", null, null);
						output.writeObject(response);
						
						socket.close();
						return;
					}
				}
				
				UserToken ut = (UserToken)getFromEnvelope(Field.TOKEN);
				
				if(!this.isValidToken(ut))
				{
					response = encryptMessageWithSymmetricKey("BAD-TOKEN", null, null);
					output.writeObject(response);
					socket.close();
					return;
				}
				
				// Handler to list files that this user is allowed to see
				// RETURNS MESSAGE OK AND LIST OF FILES
				if (e.getMessage().equals("LFILES"))
				{
					// (1) FileClient sends a message - new Envelope("LFILES"))
					// (2) Get the users token - (UserToken)e.getObjContents().get(2);
					// (3) Get groups user belongs to - List<String> groups = yourToken.getGroups();
					// (4) For each group of user, list all files relevant to group
					
					// (2) Get the token of the user
					UserToken uToken = (UserToken)getFromEnvelope(Field.TOKEN);					
					
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
					
					output.writeObject(encryptMessageWithSymmetricKey("OK", uToken, new Object[]{visibleFiles}));
					
				}// end if block
				else if (e.getMessage().equals("UPLOADF"))
				{					
					if (e.getObjContents().size() < 5)
					{
						response = encryptMessageWithSymmetricKey("FAIL-BADCONTENTS", null, null);
					}
					else
					{
						Object[] objData = (Object[])getFromEnvelope(Field.DATA);
						if(objData[0] == null)
						{
							response = encryptMessageWithSymmetricKey("FAIL-BADPATH", null, null);
						}
						else if(objData[1] == null)
						{
							response = encryptMessageWithSymmetricKey("FAIL-BADGROUP", null, null);
						}
						else if(getFromEnvelope(Field.TOKEN) == null)
						{
							response = encryptMessageWithSymmetricKey("FAIL-BADTOKEN", null, null);
						}
						else
						{
							String remotePath = (String)objData[0];
							String group = (String)objData[1];
							int epoch = (Integer)objData[2];
							UserToken yourToken = (UserToken)getFromEnvelope(Field.TOKEN);
							if (FileServer.fileList.checkFile(remotePath))
							{
								System.out.printf("Error: file already exists at %s\n", remotePath);
								response = encryptMessageWithSymmetricKey("FAIL-FILEEXISTS", null, null);
							}
							else if (!yourToken.getGroups().contains(group))
							{
								System.out.printf("Error: user missing valid token for group %s\n", group);
								response = encryptMessageWithSymmetricKey("FAIL-UNAUTHORIZED", null, null);
							}
							else
							{
								File file = new File("shared_files/" + remotePath.replace('/', '_'));
								file.createNewFile();
								FileOutputStream fos = new FileOutputStream(file);
								System.out.printf("Successfully created file %s\n", remotePath.replace('/', '_'));
								
								response = encryptMessageWithSymmetricKey("READY", null, null);								
								output.writeObject(response);
								
								Envelope chunk = (Envelope)input.readObject();
								if(!checkValidityOfMessage(chunk))
								{
									fos.close();
									return;
								}
								while (chunk.getMessage().compareTo("CHUNK") == 0)
								{
									Object[] objChunkData = (Object[])getFromEnvelope(Field.DATA);
									byte[] inBytes = (byte[])objChunkData[0];
									Integer lastIndex = (Integer)objChunkData[1];
									fos.write(inBytes, 0, lastIndex);
									
									response = encryptMessageWithSymmetricKey("READY", null, null);
									output.writeObject(response);
									chunk = (Envelope)input.readObject();
									if(!checkValidityOfMessage(chunk))
									{
										fos.close();
										return;
									}
								}
								
								if (chunk.getMessage().compareTo("EOF") == 0)
								{
									System.out.printf("Transfer successful file %s\n", remotePath);
									FileServer.fileList.addFile(yourToken.getSubject(), group, remotePath, epoch);
									response = encryptMessageWithSymmetricKey("OK", null, null);
								}
								else
								{
									System.out.printf("Error reading file %s from client\n", remotePath);
									response = encryptMessageWithSymmetricKey("ERROR-TRANSFER", null, null);
								}
								fos.close();
							}// end else block
						}// end else block
					}// end else block
					output.writeObject(response);
				}// end else if block
				else if (e.getMessage().compareTo("DOWNLOADF") == 0)
				{
					if(e.getObjContents().size() < 5)
					{
						output.writeObject(encryptMessageWithSymmetricKey("ERROR", null, null));
						continue;
					}
						
					Object[] objData = (Object[])getFromEnvelope(Field.DATA);
					String remotePath = (String)objData[0];
					UserToken t = (UserToken)getFromEnvelope(Field.TOKEN);
					
					ShareFile sf = FileServer.fileList.getFile(remotePath);
					if (sf == null)
					{
						System.out.printf("Error: File %s doesn't exist\n", remotePath);
						output.writeObject(encryptMessageWithSymmetricKey("ERROR_FILEMISSING", null, null));
					}
					else if (!t.getGroups().contains(sf.getGroup()))
					{
						System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
						output.writeObject(encryptMessageWithSymmetricKey("ERROR_PERMISSION", null, null));
					}
					else
					{
						try
						{
							File f = new File("shared_files/" + remotePath.replace('/', '_'));
							if (!f.exists())
							{
								System.out.printf("Error file %s missing from disk\n", remotePath.replace('/', '_'));
								output.writeObject(encryptMessageWithSymmetricKey("ERROR_NOTONDISK", null, null));
							}
							else
							{
								FileInputStream fis = new FileInputStream(f);
								Envelope in = e;
								if (in.getMessage().compareTo("DOWNLOADF") != 0)
								{
									System.out.printf("Server error: %s\n", in.getMessage());
									continue; //TODO bad handling.
								}
								output.writeObject(encryptMessageWithSymmetricKey("METADATA", null, new Object[]{sf.getGroup(), sf.getEpoch()}));
								in = (Envelope)input.readObject();
								if(!this.checkValidityOfMessage(in))
								{
									System.out.println("Meta data transfer of the file failed.");
									output.writeObject(encryptMessageWithSymmetricKey("FAIL", null, null));
									continue;
								}
								
								do
								{
									byte[] buf = new byte[4096];
									if (in.getMessage().compareTo("DOWNLOADF") != 0)
									{
										System.out.printf("Server error: %s\n", in.getMessage());
										break; //TODO bad handling.
									}
									
									int n = fis.read(buf); // can throw an IOException
									if (n > 0)
									{
										System.out.printf(".");
									}
									else if (n < 0)
									{
										System.out.println("Read error");
									}
									
									output.writeObject(encryptMessageWithSymmetricKey("CHUNK", null, new Object[]{buf, new Integer(n)}));

									in = (Envelope)input.readObject();
									if(!this.checkValidityOfMessage(in))
									{
										System.out.println("Something went wrong with the file transfer...");
										return;
									}
									
								} while (fis.available() > 0);
								fis.close();
								
								// If server indicates success, return the member list
								if (in.getMessage().compareTo("DOWNLOADF") == 0)
								{
									output.writeObject(encryptMessageWithSymmetricKey("EOF", null, null));
									
									in = (Envelope)input.readObject();
									if(!this.checkValidityOfMessage(in))
									{
										System.out.println("Error...something went wrong.");
										return; //TODO bad handling.
									}

									if (in.getMessage().compareTo("OK") == 0)
									{
										System.out.printf("File data upload successful\n");
									}
									else
									{
										System.out.printf("Upload failed: %s\n", in.getMessage());
									}
								}// end if block
								else
								{
									System.out.printf("Upload failed: %s\n", in.getMessage());
								}
							}// end else block
						}// end try block
						catch (Exception e1)
						{
							e1.printStackTrace(System.err);
						}
					}// end else block
				}// end else if block
				else if (e.getMessage().compareTo("DELETEF") == 0)
				{
					if(e.getObjContents().size() < 5)
					{
						output.writeObject(encryptMessageWithSymmetricKey("ERROR", null, null));
						continue;
					}
					
					Object[] objData = (Object[])getFromEnvelope(Field.DATA);
					
					String remotePath = (String)objData[0];
					UserToken t = (UserToken)getFromEnvelope(Field.TOKEN);
					
					ShareFile sf = FileServer.fileList.getFile(remotePath);
					Envelope out = null;
					if (sf == null)
					{
						System.out.printf("Error: File %s doesn't exist\n", remotePath);
						out = encryptMessageWithSymmetricKey("ERROR_NOTONDISK", null, null);
					}
					else if (!t.getGroups().contains(sf.getGroup()))
					{
						System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
						out = encryptMessageWithSymmetricKey("ERROR_PERMISSION", null, null);
					}
					else
					{
						try
						{
							File f = new File("shared_files/" + remotePath.replace('/', '_'));
							
							if (!f.exists())
							{
								System.out.printf("Error file %s missing from disk\n", "_" + remotePath.replace('/', '_'));
								out = encryptMessageWithSymmetricKey("ERROR_FILEMISSING", null, null);
							}
							else if (f.delete())
							{
								System.out.printf("File %s deleted from disk\n", "_" + remotePath.replace('/', '_'));
								FileServer.fileList.removeFile(remotePath);
								out = encryptMessageWithSymmetricKey("OK", null, null);
							}
							else
							{
								System.out.printf("Error deleting file %s from disk\n", "_" + remotePath.replace('/', '_'));
								out = encryptMessageWithSymmetricKey("ERROR_DELETE", null, null);
							}
						}// end try block
						catch (Exception e1)
						{
							e1.printStackTrace(System.err);
							out = encryptMessageWithSymmetricKey("ERROR_SYSTEM", null, null);
						}
					}// end else block
					output.writeObject(out);
				}// end else if block
				else if (e.getMessage().equals("DISCONNECT"))
				{
					// TODO check to see if token is passed in with DISCONNECT
					socket.close();
					proceed = false;
				}
			} while (proceed);
		}// end try block
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}
	}// end method run()
}// end class FileThread
