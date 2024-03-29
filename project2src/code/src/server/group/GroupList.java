package server.group;

import java.io.Serializable;
import java.security.Key;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.crypto.KeyGenerator;

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
		list.get(group).addKey();
		return list.get(group).removeUser(username);
	}
	
	public int getEpochOfGroup(String group)
	{
		return list.get(group).getEpoch();
	}
	
	public ArrayList<Key> getKeysForGroup(String group)
	{
		return list.get(group).getKeys();
	}
		
	/**
	 * This class represents a group object within the {@link GroupList}.
	 */
	class Group implements Serializable
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1670533812588746958L;
		private static final String SYM_ALGORITHM = "AES";
		private static final String PROVIDER = "BC";
		
		/**
		 * users is a class variable that holds a list of users in a group.
		 */
		private final ArrayList<String> users;
		
		/**
		 * onwer is a class variable that holds the name of the owner of the group.
		 */
		private final String owner;
		
		
		/**
		 * New for phase 4: use this array list to look up arrays of keys for the given group.
		 * GroupClient needs to store this information for UserCommands.
		 * The index of the an array for a given group in the hash table is the epoch number (used to mitigate file leakage).
		 */
		private final ArrayList<Key> keys;
		
		/**
		 * This constructor initializes the users ArrayList class variable as well as sets the name of the owner of the group.
		 * 
		 * @param owner The owner of the group.
		 */
		public Group(String owner)
		{
			this.users = new ArrayList<String>();
			this.keys = new ArrayList<Key>();
			this.keys.add(this.genterateSymmetricKey());
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
		
		public ArrayList<Key> getKeys()
		{
			return keys;
		}
		
		public int getEpoch()
		{
			return keys.size()-1;
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
			this.addKey();
			return users.remove(username);
		}
		
		private boolean addKey()
		{
			return this.keys.add(this.genterateSymmetricKey());
		}
		
		/**
		 * This method will generate a symmetric key for use.
		 */
		private final Key genterateSymmetricKey()
		{
			try
			{
				// Random Number used to generate key
				SecureRandom randomNumber = new SecureRandom();	

				// Generate a 128-bit AES Key with Bouncy Castle provider
				KeyGenerator keyGenerator = KeyGenerator.getInstance(SYM_ALGORITHM, PROVIDER);
				keyGenerator.init(128, randomNumber);
				Key key = keyGenerator.generateKey();

				return key;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			return null;
		}
	}// end class Group
}// end class GroupList
