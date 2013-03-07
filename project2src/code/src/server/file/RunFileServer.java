package server.file;

/**
 * Runs the {@link FileServer}.
 * This class holds the main method to run the FileServer.
 */
public class RunFileServer
{
	/**
	 * The main method.
	 * This is the starting point for running the GroupServer.
	 * 
	 * @param args unused
	 */
	public static void main(String[] args)
	{
		if (args.length > 0)
		{
			try
			{
				FileServer server = new FileServer(Integer.parseInt(args[0]));
				server.start();
			}
			catch (NumberFormatException e)
			{
				System.out.printf("Enter a valid port number or pass no arguments to use the default port (%d)\n", FileServer.SERVER_PORT);
			}
		}
		else
		{
			FileServer server = new FileServer();
			server.start();
		}
	}// end method main
}// end class RunFileServer
