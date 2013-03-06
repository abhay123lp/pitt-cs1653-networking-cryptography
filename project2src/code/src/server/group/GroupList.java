package server.group;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * A list of all the groups in a particular {@link GroupServer}.
 * 
 * @see GroupThread
 */
public class GroupList implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 164220729465294156L;
	
	/**
	 * list is a Hashtable class variable that connects a group name (String) to a Group object.
	 */
	private Hashtable<String, Group> list = new Hashtable<String, Group>();
	
	/**
	 * This method will add a group to the class variable list.
	 * 
	 * @param groupName The name of the group to add.
	 * @param owner The owner of the group.
	 */
	public synchronized void addGroup(String groupName, String owner)
	{
		list.put(groupName, new Group(owner));
	}
	
	/**
	 * This method will delete a group from the class variable list.
	 * 
	 * @param groupName The name of the group to delete.
	 */
	public synchronized void deleteGroup(String groupName)
	{
		list.remove(groupName);
	}
	
	/**
	 * This method will check the existence of a group.
	 * 
	 * @param groupName The name of the group to check.
	 * @return Returns a boolean value indicating true if the group exists, false otherwise.
	 */
	public synchronized boolean checkGroup(String groupName)
	{
		return list.containsKey(groupName);
	}
	
	/**
	 * This method will return an ArrayList of members of group.
	 * 
	 * @param groupName The group name to return the list of members from.
	 * @return Returns a String ArrayList of members of the specified group.
	 */
	public synchronized ArrayList<String> getGroupUsers(String groupName)
	{
		return list.get(groupName).getUsers();
	}
	
	/**
	 * This method will return the name of the owner of the specified group.
	 * 
	 * @param groupName The name of the group.
	 * @return Returs a String value representing the owner of the specified group.
	 */
	public synchronized String getGroupOwner(String groupName)
	{
		return list.get(groupName).getOwner();
	}
	
	/**
	 * This method will add a user to the specified group.
	 * 
	 * @param group The name of the group.
	 * @param username The username to add to the group.
	 */
	public synchronized boolean addUser(String group, String username)
	{
		return list.get(group).addUser(username);
	}
	
	/**
	 * This method will remove a user from the specified group.
	 * 
	 * @param group The name of the group.
	 * @param username The username to remove from the group.
	 */
	public synchronized boolean removeUser(String group, String username)
	{
		return list.get(group).removeUser(username);
	}
	
	// public synchronized void addOwner(String group, String username)
	// {
	// list.get(group).addOwner(username);
	// }
	//
	// public synchronized void removeOwner(String group, String username)
	// {
	// list.get(group).removeOwner(username);
	// }
	
	/**
	 * This class represents a group object within the {@link GroupList}.
	 */
	class Group implements Serializable
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1670533812588746958L;
		
		/**
		 * users is a class variable that holds a list of users in a group.
		 */
		private ArrayList<String> users;
		
		/**
		 * onwer is a class variable that holds the name of the owner of the group.
		 */
		private final String owner;
		
		/**
		 * This constructor initializes the users ArrayList class variable as well as sets the name of the owner of the group.
		 * 
		 * @param owner The owner of the group.
		 */
		public Group(String owner)
		{
			this.users = new ArrayList<String>();
			this.owner = owner;
		}
		
		/**
		 * This method will return the list of users in the group.
		 * 
		 * @return Returns and ArrayList class variable users which represents the users in the group.
		 */
		public ArrayList<String> getUsers()
		{
			return users;
		}
		
		/**
		 * This method returns the owner of the group.
		 * 
		 * @return Returns a String value representing the owner of the group.
		 */
		public String getOwner()
		{
			return owner;
		}
		
		/**
		 * This method adds a user to the group.
		 * 
		 * @param username The name of the user to add to the group.
		 */
		public boolean addUser(String username)
		{
			return users.add(username);
		}
		
		/**
		 * This method removes a user from a group.
		 * 
		 * @param username The name of the user to remove from the group.
		 */
		public boolean removeUser(String username)
		{
//			if (users.contains(username))
//			{
			return users.remove(username);
				// users.remove(users.indexOf(username));
//			}
		}
		
		// public void addOwner(String username)
		// {
		// owners.add(username);
		// }
		
		// public void removeOwner(String username)
		// {
		// if(!owners.isEmpty())
		// {
		// if(owners.contains(username))
		// {
		// owners.remove(owners.indexOf(username));
		// }
		// }
		// }
		
	}// end class Group
}// end class GroupList
