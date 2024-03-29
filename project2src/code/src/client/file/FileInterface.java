package client.file;

import java.security.Key;
import java.util.List;

import client.group.GroupClient;

import message.UserToken;

/**
 * Interface describing the operations that must be supported by the client application used to talk with the file servers.
 * All methods must be implemented!
 */
public interface FileInterface
{
	/**
	 * Retrieves a list of files that are allowed to be displayed members of the groups encoded in the supplied user token.
	 * 
	 * @param token The UserToken object assigned to the user invoking this operation
	 * @return A list of filenames
	 */
	public List<String> listFiles(final UserToken token);
	
	/**
	 * Uploads a file to the server to be shared with members of the specified group.
	 * This method should only succeed if the uploader is a member of the group that the file will be shared with.
	 * 
	 * @param sourceFile Path to the local file to upload
	 * @param destFile The filename to use on the server
	 * @param group The group to share this file with
	 * @param token The token of the user uploading the file
	 * @return true on success, false on failure
	 */
	public boolean upload(final String sourceFile, final String destFile, final String group, final UserToken token, final Key key, final int epoch);
	
	/**
	 * Downloads a file from the server.
	 * The user must be a member of the group with which this file is shared.
	 * 
	 * @param sourceFile The filename used on the server
	 * @param destFile The filename to use locally
	 * @param token The token of the user uploading the file
	 * @return true on success, false on failure
	 */
	public boolean download(final String sourceFile, final String destFile, final UserToken token, final GroupClient groupClient);
	
	/**
	 * Deletes a file from the server.
	 * The user must be a member of the group with which this file is shared.
	 * 
	 * @param filename The file to delete
	 * @param token The token of the user requesting the delete
	 * @return true on success, false on failure
	 */
	public boolean delete(final String filename, final UserToken token);
	
} // -- end interface FileClientInterface
