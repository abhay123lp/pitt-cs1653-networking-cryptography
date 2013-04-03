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
import message.Token;
import message.UserToken;

/**
 * The thread spawned by {@link FileServer} after accepting a connection.
 * This thread handles all envelopes coming to the server from a client.
 * 
 * @see FileClient
 */
//TODO check token validity
public class FileThread extends ServerThread
{
	//TODO need to get GroupServer's public key
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
			
			// Get the IV to use
//			IvParameterSpec IV = ivAES();
			
			do
			{
				
//				
//				byte[] decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, (byte[])input.readObject());
//				
//				Object convertedObj = convertToObject(decryptedMsg);
//				
//				Envelope e = (Envelope)convertedObj;		
				
				Envelope e = (Envelope)input.readObject();
				System.out.println("Request received: " + e.getMessage());
				Envelope response = null;
				
				if(e.getMessage().equals("REQUEST_SECURE_CONNECTION"))
				{	
					output.writeObject(this.setUpSecureConnection(e));
					continue;	
				} else {
							
					if(!checkValidityOfMessage(e)){
							
						response = encryptMessageWithSymmetricKey("DISCONNECT", null, null);
						//response = new Envelope("DISCONNECT"); // Server does not understand client request
						output.writeObject(response);
						
						socket.close();
						return;
						
					}
					
				}
				
				// TODO
				// Verify token 
				
				UserToken ut = (UserToken)getFromEnvelope(Field.TOKEN);
				
				if(!this.isValidToken(ut))
				{
					response = encryptMessageWithSymmetricKey("BAD-TOKEN", null, null);
					output.writeObject(response);
					//output.writeObject(new Envelope("ERROR"));
					socket.close();
					return;
				}
				
				/*
				if(getFromEnvelope(Field.TOKEN) != null){
					
					UserToken ut = (UserToken)getFromEnvelope(Field.TOKEN);
					
					// TODO check validity of token			
					// Make sure there are no commands that don't have a token otherwise the else will always disconnect
					
					String fileServerName = ut.getFileServerName();
					String ipAddress = ut.getIPAddress();
					int portNumber = ut.getPortNumber();
					
					if(!this.getServerName().equals(fileServerName) && !this.getIPAddress().equals(ipAddress) && portNumber != this.getPortNumber() ){
						
						response = encryptMessageWithSymmetricKey("DISCONNECT", null, null);
						//response = new Envelope("DISCONNECT"); // Server does not understand client request
						output.writeObject(response);
						
						socket.close();
						return;
						
					}
										
					
				} else {
					
					// TODO DISCONNECT
					
					response = encryptMessageWithSymmetricKey("DISCONNECT", null, null);
					//response = new Envelope("DISCONNECT"); // Server does not understand client request
					output.writeObject(response);
					
					socket.close();
					return;
					
				}
				*/
				
								
				//Envelope e = (Envelope)input.readObject();
//				System.out.println("Request received: " + e.getMessage());
				
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
//					UserToken uToken = (UserToken)e.getObjContents().get(0);
					
					//Object[] objData = (Object[])getFromEnvelope(Field.DATA);
					
					UserToken uToken = (UserToken)getFromEnvelope(Field.TOKEN);
					
					//UserToken uToken = (UserToken)convertToObject(decryptObjectBytes((byte[])e.getObjContents().get(0), (byte[])e.getObjContents().get(1)));
					
					
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
					
					//output.writeObject(encryptMessageWithSymmetricKey(new Object[]{visibleFiles}, "OK"));
					output.writeObject(encryptMessageWithSymmetricKey("OK", uToken, new Object[]{visibleFiles}));
					
//					response = new Envelope("OK");
//					response.addObject(visibleFiles);
//					
//					//return b.toByteArray(); 
//					byte[] byteArray = convertToByteArray(response);
//																	
//					output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, this.symmetricKey, IV,byteArray) );
					
					//output.writeObject(response);
				}// end if block
//				else if (e.getMessage().equals("REQUEST_SECURE_CONNECTION"))// Client wants a token
//				{
//					output.writeObject(this.setUpSecureConnection(e));
//				}
				else if (e.getMessage().equals("UPLOADF"))
				{					
					if (e.getObjContents().size() < 5)
					//if (e.getObjContents().size() < 3)
					{
						response = encryptMessageWithSymmetricKey("FAIL-BADCONTENTS", null, null);
					}
					else
					{
						Object[] objData = (Object[])getFromEnvelope(Field.DATA);
						if(objData[0] == null)
						//if (e.getObjContents().get(0) == null)
						{
							response = encryptMessageWithSymmetricKey("FAIL-BADPATH", null, null);
							//response = new Envelope("FAIL-BADPATH");
						}
						else if(objData[1] == null)
						//if (e.getObjContents().get(1) == null)
						{
							response = encryptMessageWithSymmetricKey("FAIL-BADGROUP", null, null);
							//response = new Envelope("FAIL-BADGROUP");
						}
						else if(getFromEnvelope(Field.TOKEN) == null)
						//if (e.getObjContents().get(2) == null)
						{
							response = encryptMessageWithSymmetricKey("FAIL-BADTOKEN", null, null);
							//response = new Envelope("FAIL-BADTOKEN");
						}
						else
						{
//							byte[] iv = (byte[])e.getObjContents().get(3);
							String remotePath = (String)objData[0];
									//(String)convertToObject(decryptObjectBytes((byte[])e.getObjContents().get(0), iv));
							String group = (String)objData[1];
									//(String)convertToObject(decryptObjectBytes((byte[])e.getObjContents().get(1), iv));
							UserToken yourToken = (UserToken)getFromEnvelope(Field.TOKEN);
									//(Token)convertToObject(decryptObjectBytes((byte[])e.getObjContents().get(2), iv));
//							for(int i = 0; i < yourToken.getGroups().size(); i++)
//							{
//								System.out.println(yourToken.getGroups().get(i));
//							}
//							remotePath = remotePath.substring(7);
//							group = group.substring(7);//FIXME for some reason, seven characters preceeding are trash...
							
//							String remotePath = (String)e.getObjContents().get(0);
//							String group = (String)e.getObjContents().get(1);
//							UserToken yourToken = (UserToken)e.getObjContents().get(2); // Extract token
							
							if (FileServer.fileList.checkFile(remotePath))
							{
								System.out.printf("Error: file already exists at %s\n", remotePath);
								response = encryptMessageWithSymmetricKey("FAIL-FILEEXISTS", null, null);
								//response = new Envelope("FAIL-FILEEXISTS"); // Success
							}
							else if (!yourToken.getGroups().contains(group))
							{
								System.out.printf("Error: user missing valid token for group %s\n", group);
								response = encryptMessageWithSymmetricKey("FAIL-UNAUTHORIZED", null, null);
								//response = new Envelope("FAIL-UNAUTHORIZED"); // Success
							}
							else
							{
								File file = new File("shared_files/" + remotePath.replace('/', '_'));
								file.createNewFile();
								FileOutputStream fos = new FileOutputStream(file);
								System.out.printf("Successfully created file %s\n", remotePath.replace('/', '_'));
								
								response = encryptMessageWithSymmetricKey("READY", null, null);
								//response = new Envelope("READY"); // Success
								
								//return b.toByteArray(); 
//								byte[] byteArray = convertToByteArray(response);
																				
//								output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
								
								output.writeObject(response);
								
								Envelope chunk = (Envelope)input.readObject();
								if(!checkValidityOfMessage(chunk))
								{
									fos.close();
									return;
								}
								
//								decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, (byte[])input.readObject());
//								
//								convertedObj = convertToObject(decryptedMsg);
//								
//								e = (Envelope)convertedObj;	
								
								//TODO think about requiring that the user send his token every time to verify
								while (chunk.getMessage().compareTo("CHUNK") == 0)
								{
									Object[] objChunkData = (Object[])getFromEnvelope(Field.DATA);
//									byte[] ivChunk = (byte[])getFromEnvelope(Field.IV);
											//(byte[])chunk.getObjContents().get(2);
									byte[] inBytes = (byte[])objChunkData[0];
											//(byte[])convertToObject(decryptObjectBytes((byte[])chunk.getObjContents().get(0), ivChunk));
									Integer lastIndex = (Integer)objChunkData[1];
											//(Integer)convertToObject(decryptObjectBytes((byte[])chunk.getObjContents().get(1), ivChunk));
									fos.write(inBytes, 0, lastIndex);
									
									response = encryptMessageWithSymmetricKey("READY", null, null);
									//response = new Envelope("READY"); // Success
									
									//return b.toByteArray(); 
//									byteArray = convertToByteArray(response);
//																					
//									output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
									
									output.writeObject(response);
									
//									decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, (byte[])input.readObject());
//									
//									convertedObj = convertToObject(decryptedMsg);
									
									//Envelope e = (Envelope)convertedObj;	
									
//									e = (Envelope)convertedObj;
									
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
									FileServer.fileList.addFile(yourToken.getSubject(), group, remotePath);
									response = encryptMessageWithSymmetricKey("OK", null, null);
									//response = new Envelope("OK"); // Success
								}
								else
								{
									System.out.printf("Error reading file %s from client\n", remotePath);
									response = encryptMessageWithSymmetricKey("ERROR-TRANSFER", null, null);
									//response = new Envelope("ERROR-TRANSFER"); // Success
								}
								fos.close();
							}// end else block
						}// end else block
					}// end else block
					//return b.toByteArray(); 
//					byte[] byteArray = convertToByteArray(response);
																	
//					output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
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
					
//					String remotePath = (String)e.getObjContents().get(0);
//					Token t = (Token)e.getObjContents().get(1);
//					byte[] iv = (byte[])getFromEnvelope(Field.IV);
							//(byte[])e.getObjContents().get(2);
					
					String remotePath = (String)objData[0];
							//(String)convertToObject(decryptObjectBytes((byte[])e.getObjContents().get(0), iv));
					UserToken t = (UserToken)getFromEnvelope(Field.TOKEN);
					//Token t = (Token)convertToObject(decryptObjectBytes((byte[])e.getObjContents().get(1), iv));
					
					ShareFile sf = FileServer.fileList.getFile(remotePath);
					if (sf == null)
					{
						System.out.printf("Error: File %s doesn't exist\n", remotePath);
//						e = new Envelope("ERROR_FILEMISSING");
//						
//						byte[] byteArray = convertToByteArray(e);
//						
//						output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
						
						output.writeObject(encryptMessageWithSymmetricKey("ERROR_FILEMISSING", null, null));
						//output.writeObject(new Envelope("ERROR_FILEMISSING"));
					}
					else if (!t.getGroups().contains(sf.getGroup()))
					{
						System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
//						e = new Envelope("ERROR_PERMISSION");
//						
//						byte[] byteArray = convertToByteArray(e);
//						
//						output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
						
						output.writeObject(encryptMessageWithSymmetricKey("ERROR_PERMISSION", null, null));
						//output.writeObject(new Envelope("ERROR_PERMISSION"));
					}
					else
					{
						try
						{
							File f = new File("shared_files/" + remotePath.replace('/', '_'));
							if (!f.exists())
							{
								System.out.printf("Error file %s missing from disk\n", remotePath.replace('/', '_'));
//								e = new Envelope("ERROR_NOTONDISK");
//								
//								byte[] byteArray = convertToByteArray(e);
//								
//								output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
								
								output.writeObject(encryptMessageWithSymmetricKey("ERROR_NOTONDISK", null, null));
								//output.writeObject(new Envelope("ERROR_NOTONDISK"));
							}
							else
							{
								FileInputStream fis = new FileInputStream(f);
								Envelope in = e;
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
									
//									Envelope out = new Envelope("CHUNK");
//									out.addObject(buf);
//									out.addObject(new Integer(n));
																		
//									byte[] byteArray = convertToByteArray(out);
//									
//									output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
									
									//output.writeObject(encryptMessageWithSymmetricKey(new Object[]{buf, new Integer(n)}, "CHUNK"));
									output.writeObject(encryptMessageWithSymmetricKey("CHUNK", null, new Object[]{buf, new Integer(n)}));
									
									//e = (Envelope)input.readObject();
									
//									decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, (byte[])input.readObject());
//									
//									convertedObj = convertToObject(decryptedMsg);
//									
//									in = (Envelope)convertedObj;
									in = (Envelope)input.readObject();
									if(!this.checkValidityOfMessage(in))
									{
										return;
									}
									
								} while (fis.available() > 0);
								fis.close();
								
								// If server indicates success, return the member list
								if (in.getMessage().compareTo("DOWNLOADF") == 0)
								{
									
//									Envelope response = new Envelope("EOF");
									
//									byte[] byteArray = convertToByteArray(e);
//									
//									output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );								
//									
									output.writeObject(encryptMessageWithSymmetricKey("EOF", null, null));
									//output.writeObject(new Envelope("EOF"));
									
									in = (Envelope)input.readObject();
									if(!this.checkValidityOfMessage(in))
									{
										System.out.println("Error...something went wrong.");
										return; //TODO bad handling.
									}
//									
//									decryptedMsg = AESDecrypt(SYM_KEY_ALG, PROVIDER,SYMMETRIC_KEY, IV, (byte[])input.readObject());
//									
//									convertedObj = convertToObject(decryptedMsg);
//									
//									in = (Envelope)convertedObj;
									
									
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
//							System.err.println("Error: " + e1.getMessage());
							e1.printStackTrace(System.err);
						}
					}// end else block
				}// end else if block
				else if (e.getMessage().compareTo("DELETEF") == 0)
				{
//					String remotePath = (String)e.getObjContents().get(0);
//					Token t = (Token)e.getObjContents().get(1);
					if(e.getObjContents().size() < 5)
					{
						output.writeObject(encryptMessageWithSymmetricKey("ERROR", null, null));
						continue;
					}
					
					Object[] objData = (Object[])getFromEnvelope(Field.DATA);
					
//					byte[] iv = (byte[])getFromEnvelope(Field.IV);
							//(byte[])e.getObjContents().get(2);
					String remotePath = (String)objData[0];
							//(String)convertToObject(decryptObjectBytes((byte[])e.getObjContents().get(0), iv));
					UserToken t = (UserToken)getFromEnvelope(Field.TOKEN);
					//Token t = (Token)convertToObject(decryptObjectBytes((byte[])e.getObjContents().get(1), iv));
					
					ShareFile sf = FileServer.fileList.getFile("/" + remotePath);
					Envelope out = null;
					if (sf == null)
					{
						System.out.printf("Error: File %s doesn't exist\n", remotePath);
						out = encryptMessageWithSymmetricKey("ERROR_NOTONDISK", null, null);
						//out = new Envelope("ERROR_DOESNTEXIST");
					}
					else if (!t.getGroups().contains(sf.getGroup()))
					{
						System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
						out = encryptMessageWithSymmetricKey("ERROR_PERMISSION", null, null);
						//out = new Envelope("ERROR_PERMISSION");
					}
					else
					{
						try
						{
							File f = new File("shared_files/" + "_" + remotePath.replace('/', '_'));
							
							if (!f.exists())
							{
								System.out.printf("Error file %s missing from disk\n", "_" + remotePath.replace('/', '_'));
								out = encryptMessageWithSymmetricKey("ERROR_FILEMISSING", null, null);
								//out = new Envelope("ERROR_FILEMISSING");
							}
							else if (f.delete())
							{
								System.out.printf("File %s deleted from disk\n", "_" + remotePath.replace('/', '_'));
								FileServer.fileList.removeFile("/" + remotePath);
								out = encryptMessageWithSymmetricKey("OK", null, null);
								//out = new Envelope("OK");
							}
							else
							{
								System.out.printf("Error deleting file %s from disk\n", "_" + remotePath.replace('/', '_'));
								out = encryptMessageWithSymmetricKey("ERROR_DELETE", null, null);
								//	out = new Envelope("ERROR_DELETE");
							}
						}// end try block
						catch (Exception e1)
						{
//							System.err.println("Error: " + e1.getMessage());
							e1.printStackTrace(System.err);
//							e = new Envelope(e1.getMessage());
							out = encryptMessageWithSymmetricKey("ERROR_SYSTEM", null, null);
							//out = new Envelope("ERROR_SYSTEM");
						}
					}// end else block

//					byte[] byteArray = convertToByteArray(e);
					
//					output.writeObject(AESEncrypt(SYM_KEY_ALG, PROVIDER, SYMMETRIC_KEY, IV,byteArray) );
					
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
//			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}// end method run()
}// end class FileThread
