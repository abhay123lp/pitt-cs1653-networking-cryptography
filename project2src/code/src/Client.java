import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public abstract class Client {

	/* protected keyword is like private but subclasses have access
	 * Socket and input/output streams
	 */
	protected Socket sock;				//NOTE: this requires blocking access
	protected ObjectOutputStream output;
	protected ObjectInputStream input;

	//this is really easy
	public boolean connect(final String server, final int port) {
		System.out.println("attempting to connect");

		/* TODO: Write this method */

	}

	//I don't really know why we need this.
	//Plus it's easy to condense
	public boolean isConnected() {
		if (sock == null || !sock.isConnected()) {
			return false;
		}
		else {
			return true;
		}
	}

	//really bad coding practice.........
	public void disconnect()	 {
		if (isConnected()) {
			try
			{
				Envelope message = new Envelope("DISCONNECT");
				output.writeObject(message);
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
			}
		}
	}
}
