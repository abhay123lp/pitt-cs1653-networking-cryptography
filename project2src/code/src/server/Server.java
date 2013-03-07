package server;
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
	protected int port;
	
	/**
	 * The name of the server.
	 */
	public String name;
	
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
}// end class Server
