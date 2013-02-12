import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The implementation of the UserToken.
 */
public class Token implements UserToken, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 843718676814078905L;
	
	/**
	 * issuer is a class variable represents the who issued the token.
	 */
	private final String issuer;
	
	/**
	 * subject is a class variable representing the name of the user the token has been awarded to.
	 */
	private final String subject;
	
	/**
	 * groups is a class variable representing the list of groups the user belongs to.
	 */
	private final List<String> groups;
	
	/**
	 * This constructor initializes the issuer, subject, and groups class variables.
	 * 
	 * @param issuer The issuer of the token.
	 * @param subject The name of the user the token was awarded to.
	 * @param groups The groups associated with the user.
	 */
	public Token(String issuer, String subject, List<String> groups)
	{
		
		this.issuer = issuer;
		this.subject = subject;
		this.groups = makeCopyOfGroups(groups);
	}
	
	/**
	 * Used to make a deep copy of groups
	 */
	private final List<String> makeCopyOfGroups(List<String> groups)
	{
		List<String> g = new ArrayList<String>();
		for (String s : groups)
		{
			g.add(s);
		}
		return g;
	}
	
	/**
	 * This method should return a string describing the issuer of
	 * this token. This string identifies the group server that
	 * created this token. For instance, if "Alice" requests a token
	 * from the group server "Server1", this method will return the
	 * string "Server1".
	 * 
	 * @return The issuer of this token
	 */
	public String getIssuer()
	{
		return issuer;
	}
	
	/**
	 * This method should return a string indicating the name of the
	 * subject of the token. For instance, if "Alice" requests a
	 * token from the group server "Server1", this method will return
	 * the string "Alice".
	 * 
	 * @return The subject of this token
	 */
	public String getSubject()
	{
		return subject;
	}
	
	/**
	 * This method extracts the list of groups that the owner of this
	 * token has access to. If "Alice" is a member of the groups "G1"
	 * and "G2" defined at the group server "Server1", this method
	 * will return ["G1", "G2"].
	 * 
	 * @return The list of group memberships encoded in this token
	 */
	public List<String> getGroups()
	{
		return groups;
	}
}
