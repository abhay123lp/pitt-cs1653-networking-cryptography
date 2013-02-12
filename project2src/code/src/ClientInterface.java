/**
 * Interface that contains methods relating to all forms of clients to be used by the group-based file sharing application.
 * This interface came from the splitting of the FileClientInterface and the GroupClientInterface.
 */
public interface ClientInterface
{
	/**
	 * Connect to the specified group server. No other methods should
	 * work until the client is connected to a group server.
	 * 
	 * @param server The IP address or hostname of the server
	 * @param port The port that the server is listening on
	 * @return true if the connection succeeds, false otherwise
	 */
	public boolean connect(final String server, final int port);
	
	/**
	 * Determines whether or not the client is connected to the server.
	 * 
	 * @return Returns whether or not the client is connected.
	 *         Returns false if there is no valid connection between the client and the server.
	 */
	public boolean isConnected();
	
	/**
	 * Close down the connection to the server.
	 */
	public void disconnect();
}// end interface ClientInterface
