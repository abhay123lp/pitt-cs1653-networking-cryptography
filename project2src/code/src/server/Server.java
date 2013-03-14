package server;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * The Server abstract class holds implementation of accepting connections from a Client, but does not hold specific methods on group client or file client demands.
 * 
 * @see GroupClient, FileClient
 */
public abstract class Server
{
	/**
	 * The port number to use.
	 */
	protected final int port;
	
	/**
	 * The name of the server.
	 */
	public final String name;
	
	protected RSAPublicKey publicKey;
	protected RSAPrivateKey privateKey;
	
	protected static final int KEY_SIZE = 1024;
	protected static final String ALGORITHM = "RSA";
	protected static final String PROVIDER = "BC";
	
	/**
	 * The method needs to be defined by any class that extends this method. This method will start the server.
	 */
	protected abstract void start();
	
	/**
	 * This constructor initializes the port of the server as well as sets the name of the server.
	 * 
	 * @param _SERVER_PORT The port of the server.
	 * @param _serverName The name of the server.
	 */
	public Server(int _SERVER_PORT, String _serverName)
	{
		port = _SERVER_PORT;
		name = _serverName;
		generateRSAKeyPair();
	}
	
	/**
	 * This method will return the port.
	 * 
	 * @return Returns an integer value specifying the port being used.
	 */
	public int getPort()
	{
		return port;
	}
	
	/**
	 * This method will return the name of the server.
	 * 
	 * @return Returns a string value specifying the name of the server.
	 */
	public String getName()
	{
		return name;
	}
	
	private final void generateRSAKeyPair()
	{
		// Generate Key Pair 
		KeyPairGenerator rsaKeyGenerator;
		
		try{
			/***** Generate Key Pair ******/
			rsaKeyGenerator = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
			rsaKeyGenerator.initialize(KEY_SIZE);
			KeyPair rsaKeyPair = rsaKeyGenerator.generateKeyPair();
			
			// Private Key
			this.privateKey = (RSAPrivateKey)rsaKeyPair.getPrivate();
			// Public key 
			this.publicKey = (RSAPublicKey)rsaKeyPair.getPublic();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}// end class Server
