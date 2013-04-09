package message;

import java.io.Serializable;
import java.security.Key;
import java.util.ArrayList;


public class GroupKeysMap implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3615921813054442125L;
	String groupName;
	ArrayList<Key> keys;
	
	public GroupKeysMap(String gName, ArrayList<Key> k ){
		this.groupName = gName;
		this.keys = k;
	}
	
	public boolean checkGroupName(String gName){
		return this.groupName.equals(gName);
	}
	
	public Key getKey(int index){
		return keys.get(index);
	}
	
	public Key getLastKey(){
		return keys.get(keys.size() -1);
	}
	
	public int getEpoch(){
		return this.keys.size() -1;
	}
}
