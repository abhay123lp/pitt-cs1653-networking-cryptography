
public interface ClientInterface {
    /**
     * Connect to the specified group server.  No other methods should
     * work until the client is connected to a group server.
     *
     * @param server The IP address or hostname of the group server
     * @param port The port that the group server is listening on
     *
     * @return true if the connection succeeds, false otherwise
     *
     */
    public boolean connect(final String server, final int port);

    public boolean isConnected();

    /**
     * Close down the connection to the group server.
     *
     */
    public void disconnect();
}
