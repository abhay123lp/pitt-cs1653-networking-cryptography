import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Token implements UserToken, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 843718676814078905L;
	private final String issuer;
	private final String subject;
	private final List<String> groups;
	
	public Token(String issuer, String subject, List<String> groups){
		
		this.issuer = issuer;
		this.subject = subject;
		this.groups = makeCopyOfGroups(groups);
	}
	
	/**
	 *  Used to make a deep copy of groups
	 */
	private final List<String> makeCopyOfGroups(List<String> groups){
		
		List<String> g = new ArrayList<String>();
		
		for(String s : groups){
			
			g.add(s);
						
		}
		
		return g;
		
	}
	
	/**
     * This method should return a string describing the issuer of
     * this token.  This string identifies the group server that
     * created this token.  For instance, if "Alice" requests a token
     * from the group server "Server1", this method will return the
     * string "Server1".
     *
     * @return The issuer of this token
     *
     */
	public String getIssuer()
	{
		
		return issuer;
	
	}
	
	 /**
     * This method should return a string indicating the name of the
     * subject of the token.  For instance, if "Alice" requests a
     * token from the group server "Server1", this method will return
     * the string "Alice".
     *
     * @return The subject of this token
     *
     */
	 public String getSubject(){
		 
		 return subject;
		 
	 }
	 
	 /**
	     * This method extracts the list of groups that the owner of this
	     * token has access to.  If "Alice" is a member of the groups "G1"
	     * and "G2" defined at the group server "Server1", this method
	     * will return ["G1", "G2"].
	     *
	     * @return The list of group memberships encoded in this token
	     *
	     */
	    public List<String> getGroups(){
	    	
	    	return groups;
	    	
	    }
	    
	    
	    
	    
	
	

}
