/**
 * This class holds the file representation for files in the {@link FileServer}.
 * Files have some meta data that handles accessibility, which includes a group and an owner.
 * Enforcing accessibility should be handled by {@link GroupThread}.
 */
public class ShareFile implements java.io.Serializable, Comparable<ShareFile>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6699986336399821598L;
	
	/**
	 * The group name that the file is accessible to.
	 */
	private String group;
	
	/**
	 * The path of the file.
	 */
	private String path;
	
	/**
	 * The owner of the file.
	 */
	private String owner;
	
	/**
	 * Constructor.
	 * Creates a new file with the given owner, group, and path.
	 * 
	 * @param _owner The String representing the username of the owner.
	 * @param _group The String representing the group that is allowed to have access to the file.
	 * @param _path The String representing the path that the file will be found in on the Server.
	 */
	public ShareFile(String _owner, String _group, String _path)
	{
		group = _group;
		owner = _owner;
		path = _path;
	}
	
	/**
	 * Gets the path of the file.
	 * 
	 * @return Returns the String representing the location of the file, including the file name.
	 */
	public String getPath()
	{
		return path;
	}
	
	/**
	 * Gets the owner of the file.
	 * 
	 * @return Returns the String representing the username of the owner of the file.
	 */
	public String getOwner()
	{
		return owner;
	}
	
	/**
	 * Gets the group that has access to the file.
	 * 
	 * @return Returns the String representing the group that can access the file.
	 */
	public String getGroup()
	{
		return group;
	}
	
	public int compareTo(ShareFile rhs)
	{
		if (path.compareTo(rhs.getPath()) == 0)
			return 0;
		else if (path.compareTo(rhs.getPath()) < 0)
			return -1;
		else
			return 1;
	}
}// end class ShareFile
