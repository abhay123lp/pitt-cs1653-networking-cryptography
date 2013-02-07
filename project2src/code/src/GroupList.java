import java.util.ArrayList;
import java.util.Hashtable;

public class GroupList {
	private Hashtable<String, Group> list = new Hashtable<String, Group>();

	public synchronized void addGroup(String groupName)
	{
		Group newUser = new Group();
		list.put(groupName, newUser);
	}

	public synchronized void deleteGroup(String groupName)
	{
		list.remove(groupName);
	}

	public synchronized boolean checkGroup(String groupName)
	{
		if(list.containsKey(groupName))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public synchronized ArrayList<String> getGroupUsers(String groupName)
	{
		return list.get(groupName).getUsers();
	}

	public synchronized ArrayList<String> getGroupOwners(String groupName)
	{
		return list.get(groupName).getOwners();
	}

	public synchronized void addUser(String group, String username)
	{
		list.get(group).addUser(username);
	}

	public synchronized void removeUser(String group, String username)
	{
		list.get(group).removeUser(username);
	}

	public synchronized void addOwner(String group, String username)
	{
		list.get(group).addOwner(username);
	}

	public synchronized void removeOwner(String group, String username)
	{
		list.get(group).removeOwner(username);
	}


	class Group {

		 private ArrayList<String> users;
		 private ArrayList<String> owners;

		 public Group()
		 {
			 users = new ArrayList<String>();
			 owners = new ArrayList<String>();
		 }

		 public ArrayList<String> getUsers()
		 {
			 return users;
		 }

		 public ArrayList<String> getOwners()
		 {
			 return owners;
		 }

		 public void addUser(String username)
		 {
			 users.add(username);
		 }

		 public void removeUser(String username)
		 {
			 if(!users.isEmpty())
			 {
				 if(users.contains(username))
				 {
					 users.remove(users.indexOf(username));
				 }
			 }
		 }

		 public void addOwner(String username)
		 {
			 owners.add(username);
		 }

		 public void removeOwner(String username)
		 {
			 if(!owners.isEmpty())
			 {
				 if(owners.contains(username))
				 {
					 owners.remove(owners.indexOf(username));
				 }
			 }
		 }

	}
}
