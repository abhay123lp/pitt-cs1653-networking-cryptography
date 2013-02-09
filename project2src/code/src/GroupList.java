import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;

public class GroupList implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 164220729465294156L;
	private Hashtable<String, Group> list = new Hashtable<String, Group>();

	public synchronized void addGroup(String groupName, String owner)
	{
		list.put(groupName, new Group(owner));
	}

	public synchronized void deleteGroup(String groupName)
	{
		list.remove(groupName);
	}

	public synchronized boolean checkGroup(String groupName)
	{
		return list.containsKey(groupName);
	}

	public synchronized ArrayList<String> getGroupUsers(String groupName)
	{
		return list.get(groupName).getUsers();
	}

	public synchronized String getGroupOwner(String groupName)
	{
		return list.get(groupName).getOwner();
	}

	public synchronized void addUser(String group, String username)
	{
		list.get(group).addUser(username);
	}

	public synchronized void removeUser(String group, String username)
	{
		list.get(group).removeUser(username);
	}

//	public synchronized void addOwner(String group, String username)
//	{
//		list.get(group).addOwner(username);
//	}
//
//	public synchronized void removeOwner(String group, String username)
//	{
//		list.get(group).removeOwner(username);
//	}


	class Group implements Serializable{

		 /**
		 * 
		 */
		private static final long serialVersionUID = 1670533812588746958L;
		private ArrayList<String> users;
		 private final String owner;

		 public Group(String owner)
		 {
			 this.users = new ArrayList<String>();
			 this.owner = owner;
		 }

		 public ArrayList<String> getUsers()
		 {
			 return users;
		 }

		 public String getOwner()
		 {
			 return owner;
		 }

		 public void addUser(String username)
		 {
			 users.add(username);
		 }

		 public void removeUser(String username)
		 {
			 if(users.contains(username))
			 {
				 users.remove(username);
				 //users.remove(users.indexOf(username));
			 }
		 }

//		 public void addOwner(String username)
//		 {
//			 owners.add(username);
//		 }

//		 public void removeOwner(String username)
//		 {
//			 if(!owners.isEmpty())
//			 {
//				 if(owners.contains(username))
//				 {
//					 owners.remove(owners.indexOf(username));
//				 }
//			 }
//		 }

	}
}
