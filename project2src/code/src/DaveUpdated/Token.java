import java.io.Serializable;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	
	private static final String DELIMITER = "~";
	
	/**
	 * subject is a class variable representing the name of the user the token has been awarded to.
	 */
	private final String subject;
	
	/**
	 * groups is a class variable representing the list of groups the user belongs to.
	 */
	private final List<String> groups;
	
	
	
	private byte[] hashedSignedTokenData;
	
	 
	
	/**
	 * This constructor initializes the issuer, subject, and groups class variables.
	 * 
	 * @param issuer The issuer of the token.
	 * @param subject The name of the user the token was awarded to.
	 * @param groups The groups associated with the user.
	 */
	public Token(String issuer, String subject, List<String> groups, RSAPrivateKey _privKey)
	{
		
		this.issuer = issuer;
		this.subject = subject;
		this.groups = makeCopyOfGroups(groups);
		//this.privateKey = _privKey;
		
		hashedSignedTokenData = gengerateRSASignature("SHA1withRSA", "BC", _privKey, getDelimitedString());
		
		_privKey = null;
		
	}
	
	
	public byte[] gethashedSignedTokenData(){
		
		return hashedSignedTokenData;
		
	}
	
	
	private static byte[] gengerateRSASignature(String algorithm, String provider, RSAPrivateKey privKey, String clearText){

		try{

			// Change the clear text to bytes
			byte[] clearBytes = clearText.getBytes();

			// Create RSA signature
			Signature sig = Signature.getInstance(algorithm, provider);
			sig.initSign(privKey);
			sig.update(clearBytes);		
			
			privKey = null;

			return sig.sign();
			
		} catch (Exception ex){

			privKey = null;
			System.out.println(ex.toString());
		}

		return null;
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
	
	public String getDelimitedString(){
		
		// Begin creating token
		String finalToken = this.issuer + DELIMITER + this.subject;
		
		// Sort groups
		Collections.sort(this.groups);
		
		// Delimit the groups and concatenate
		for(String s : this.groups){
			
			finalToken += DELIMITER + s;
			
		}
		
		
		// Hash here and sign here
		
		
		
		
		
		return finalToken;
		
	}
	
	/**
	 * This method will verify the signature of the data.
	 * @param algorithm The algorithm to use.
	 * @param provider The security provider to use.
	 * @param pubKey The public key to use.
	 * @param decryptedData The decrypted data we want to verify.
	 * @param signature The signature data.
	 * @return boolean value indicating if the signature has been verified.
	 */
	public boolean RSAVerifySignature(String algorithm, String provider, RSAPublicKey pubKey, byte[] decryptedData){

		try{
			
			// Create new signature instance
			Signature verificationSig = Signature.getInstance(algorithm, provider);
			verificationSig.initVerify(pubKey);
			verificationSig.update(decryptedData);		
				
			return verificationSig.verify(hashedSignedTokenData);  //verificationSig.verify(signature);
				
		} catch(Exception ex){

			System.out.println(ex.toString());

		}

		return false;

	}
	
	
	
}
