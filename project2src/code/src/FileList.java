/* This list represents the files on the server */
import java.util.*;

/**
 * A list of all the files in a particular {@link FileServer}.
 * 
 * @see FileThread
 */
public class FileList implements java.io.Serializable
{
	/* Serializable so it can be stored in a file for persistence */
	/**
	 * 
	 */
	private static final long serialVersionUID = -8911161283900260136L;
	
	/**
	 * ArrayList of ShareFile objects that represent the list of files to share.
	 */
	private ArrayList<ShareFile> list; // The list of files to share.
	
	/**
	 * Constructor that initializes the list class variable.
	 */
	public FileList()
	{
		list = new ArrayList<ShareFile>();
	}
	
	/**
	 * This method adds a file to the file server.
	 * 
	 * @param owner The string value describing the owner of the file.
	 * @param group The string value describing the group the file belongs to.
	 * @param path The string value describing where the file currently lives on the users local machine.
	 */
	public synchronized void addFile(String owner, String group, String path)
	{
		ShareFile newFile = new ShareFile(owner, group, path);
		list.add(newFile);
	}
	
	/**
	 * This method will remove a file specified in the path provided.
	 * 
	 * @param path The string value of the path of the file to remove.
	 */
	public synchronized void removeFile(String path)
	{
		for (int i = 0; i < list.size(); i++ )
		{
			if (list.get(i).getPath().compareTo(path) == 0)
			{
				list.remove(i);
			}
		}
	}
	
	/**
	 * This method will check if a file specified in the path exists.
	 * 
	 * @param path The string representing the path of the file to check for existence.
	 * @return Returns a true value indicating if the file exists, otherwise it returns false.
	 */
	public synchronized boolean checkFile(String path)
	{
		for (int i = 0; i < list.size(); i++ )
		{
			// again, why not .equals()?
			if (list.get(i).getPath().compareTo(path) == 0)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * This method will return a list of shared files.
	 * 
	 * @return Returns an array list of shared files.
	 */
	public synchronized ArrayList<ShareFile> getFiles()
	{
		// why?
		Collections.sort(list);
		return list;
	}
	
	/**
	 * This method will get a file specified in the path.
	 * 
	 * @param path The string value of the path of the file to return.
	 * @return Returns a ShareFile object of the file specified in the path.
	 */
	public synchronized ShareFile getFile(String path)
	{
		for (int i = 0; i < list.size(); i++ )
		{
			// again, why not .equals()?
			if (list.get(i).getPath().compareTo(path) == 0)
			{
				return list.get(i);
			}
		}
		return null;
	}
}// end class FileList
