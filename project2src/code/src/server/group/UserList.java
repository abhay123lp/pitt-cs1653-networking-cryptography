package server.group;

/* This list represents the users on the server */
import java.security.MessageDigest;
import java.security.SecureRandom;
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
	
	// TODO: password support
	/**
	 * list is a Hashtable class variable that connects a username (String) to a User object.
	 */
	private Hashtable<String, User> list = new Hashtable<String, User>();
	/**
	 * passwordList is a Hashtable which holds the passwords of users
	 * Retrieve the password by entering a username -- .get(username) returns null if that user
	 * is not in the table
	 */
	// TODO: SHA-1 with a salt -- need to have an integer
	private Hashtable<String, String> passwordList = new Hashtable<String, String>();
	/**
	 * saltList is a Hashtable which holds the salts for SHA-1
	 * We use the salts and concatenate them to the user's password.
	 * Then we hash the clear-text password concatenated with the salt to get the hash.
	 * That hash can be compared with the passwordList
	 */
	private Hashtable<String, String> saltList = new Hashtable<String, String>();
	
	/**
	 * This method will add a user to the class variable list.
	 * 
	 * @param username The username to add.
	 */
	public synchronized void addUser(String username, String password)
	{
		try
		{
			User newUser = new User();
			list.put(username, newUser);
			// Get a random salt
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
			byte [] salt = new byte[16];
			
			random.nextBytes(salt);
			// Concat the salt to the end of the password
			String saltedPassword = password + new String(salt);
			System.out.println("password + salt: " + saltedPassword);
			MessageDigest msgDigest = MessageDigest.getInstance("SHA1");
			msgDigest.update(saltedPassword.getBytes());
			// Now we get the salted, hashed password
			byte[] saltedHashedPassword = msgDigest.digest();
			
			
			// (Obvious) Store the hashed password -- useless to attacker with hash
			passwordList.put(username, new String(saltedHashedPassword));
			// Store the salt for future checks
			saltList.put(username, new String(salt));
			//System.out.println("UserList (checkPassword):  password: " + new String(password) + "  salted password: " + saltedPassword + "  salt: " + new String(salt) + "  hashed PW: " + new String(saltedHashedPassword));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * This method will delete a user from the class variable list.
	 * 
	 * @param username The username to delete.
	 */
	public synchronized void deleteUser(String username)
	{
		list.remove(username);
		passwordList.remove(username);
		saltList.remove(username);
	}
	
	/**
	 * This method will check if a user exists in the list.
	 * 
	 * @param username The username to check in the list.
	 * @return Returns a boolean value indicating if the user exists. If the user exists it returns true, otherwise 
	 * false.
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
	 * This method returns true if the provided password matches the hashed password in the Hashtable.
	 * 
	 * @param username The username to check in the passwordList.
	 * @param password This is the cleartext password. It will be hashed and matched against the 
	 * hashed password in passwordList.
	 * @return Returns true if the username and password match.
	 */
	// TODO: SHA-1 hashing
	// TODO: Do we need the list still? Key, value pairs already in passwordList
	// TODO: comments
	public synchronized boolean checkPassword(String username, String password)
	{
		try
		{
			String salt = "";
			if(passwordList.containsKey(username))
			{
				salt = new String(saltList.get(username));
			}
			// user not in list. Non-existent users cannot have passwords
			else
			{
				return false;
			}		
			
			// Concat the salt to the end of the password
			String saltedPassword = password + new String(salt);
			System.out.println("password + salt: " + saltedPassword);
			MessageDigest msgDigest = MessageDigest.getInstance("SHA1");
			msgDigest.update(saltedPassword.getBytes());
			// Now we get the salted, hashed password
			String saltedHashedPassword = new String(msgDigest.digest());
			//System.out.println("UserList (checkPassword):  password: " + new String(password) + "  salted password: " + saltedPassword + "  salt: " + new String(salt) + "  hashed PW: " + new String(saltedHashedPassword));	
			// Check the hashed password that was entered against the hashed password in the hashList.
			if (saltedHashedPassword.equals(passwordList.get(username)))
			{
				return true;
			}
			// Either the hash matched the
			else
			{
				return false;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return false;
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
