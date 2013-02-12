/* This list represents the users on the server */
import java.util.*;

/**
 * A list of all the users in a particular {@link GroupServer}.
 * 
 * @see GroupThread
 */
public class UserList implements java.io.Serializable
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7600343803563417992L;
	
	/**
	 * list is a Hashtable class variable that connects a username (String) to a User object.
	 */
	private Hashtable<String, User> list = new Hashtable<String, User>();
	
	/**
	 * This method will add a user to the class variable list.
	 * 
	 * @param username The username to add.
	 */
	public synchronized void addUser(String username)
	{
		User newUser = new User();
		list.put(username, newUser);
	}
	
	/**
	 * This method will delete a user from the class variable list.
	 * 
	 * @param username The username to delete.
	 */
	public synchronized void deleteUser(String username)
	{
		list.remove(username);
	}
	
	/**
	 * This method will check if a user exists in the list.
	 * 
	 * @param username The username to check in the list.
	 * @return Returns a boolean value indicating if the user exists. If the user exists it returns true, otherwise false.
	 */
	public synchronized boolean checkUser(String username)
	{
		if (list.containsKey(username))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * This method will return a list of groups associated with the specified user.
	 * 
	 * @param username The name of the user.
	 * @return Returns an ArrayList of Strings that represenet the groups associated with the user.
	 */
	public synchronized ArrayList<String> getUserGroups(String username)
	{
		return list.get(username).getGroups();
	}
	
	/**
	 * This method will return a list of the groups owned by the specified user.
	 * 
	 * @param username The name of the user.
	 * @return Returns and ArrayList Strings of the groups owned by the user.
	 */
	public synchronized ArrayList<String> getUserOwnership(String username)
	{
		return list.get(username).getOwnership();
	}
	
	/**
	 * This method will add a group for the specified user.
	 * 
	 * @param user The name of the user.
	 * @param groupname The name of the group.
	 */
	public synchronized void addGroup(String user, String groupname)
	{
		list.get(user).addGroup(groupname);
	}
	
	/**
	 * This method will remove a group from the specified user.
	 * 
	 * @param user The name of the user.
	 * @param groupname The name of the group.
	 */
	public synchronized void removeGroup(String user, String groupname)
	{
		list.get(user).removeGroup(groupname);
	}
	
	/**
	 * This method will add ownership for a specified user for a group.
	 * 
	 * @param user The name of the user.
	 * @param groupname The name of the group.
	 */
	public synchronized void addOwnership(String user, String groupname)
	{
		list.get(user).addOwnership(groupname);
	}
	
	public synchronized void removeOwnership(String user, String groupname)
	{
		list.get(user).removeOwnership(groupname);
	}
	
	/**
	 * This class represents a user object within the {@link UserList}.
	 */
	class User implements java.io.Serializable
	{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -6699986336399821598L;
		/**
		 * The ArrayList of Strings representing the groups for a user.
		 */
		private ArrayList<String> groups;
		/**
		 * The ArrayList of Strings representing the groups the user owns.
		 */
		private ArrayList<String> ownership;
		
		/**
		 * This constructor will initialize the class variables groups and ownership.
		 */
		public User()
		{
			groups = new ArrayList<String>();
			ownership = new ArrayList<String>();
		}
		
		/**
		 * This method will return the groups the user belongs to.
		 * 
		 * @return Returns an ArrayList of Strings of the groups the user belongs to. Specifically the class variable groups.
		 */
		public ArrayList<String> getGroups()
		{
			return groups;
		}
		
		/**
		 * This method will return the groups that the user owns.
		 * 
		 * @return Returns an ArrayList of Strings of the groups the user owns. Specifically the class variable ownership.
		 */
		public ArrayList<String> getOwnership()
		{
			return ownership;
		}
		
		/**
		 * This method will add a group for the user.
		 * 
		 * @param group The name of the group.
		 */
		public void addGroup(String group)
		{
			groups.add(group);
		}
		
		/**
		 * This method will remove a group from a user.
		 * 
		 * @param group The name of the group.
		 */
		public void removeGroup(String group)
		{
			if (!groups.isEmpty())
			{
				if (groups.contains(group))
				{
					groups.remove(groups.indexOf(group));
				}
			}
		}
		
		/**
		 * This method will add ownership of a group to a user.
		 * 
		 * @param group The name of the group.
		 */
		public void addOwnership(String group)
		{
			ownership.add(group);
		}
		
		/**
		 * This method will remove an ownership of a group from a user.
		 * 
		 * @param group The name of the group.
		 */
		public void removeOwnership(String group)
		{
			if (!ownership.isEmpty())
			{
				if (ownership.contains(group))
				{
					ownership.remove(ownership.indexOf(group));
				}
			}
		}
	}// end class User
}// end class UserList
