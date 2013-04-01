package server.group;

import java.security.Security;

/**
 * Runs the {@link GroupServer}.
 * This class holds the main method to run the GroupServer.
 */
public class RunGroupServer
{
	/**
	 * The main method.
	 * This is the starting point for running the GroupServer.
	 * 
	 * @param args unused
	 */
	public static void main(String[] args)
	{
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		if (args.length > 0)
		{
			try
			{
				GroupServer server = new GroupServer(Integer.parseInt(args[0]));
				server.start();
			}
			catch (NumberFormatException e)
			{
				System.out.printf("Enter a valid port number or pass no arguments to use the default port (%d)\n", GroupServer.SERVER_PORT);
			}
		}
		else
		{
			GroupServer server = new GroupServer();
			server.start();
		}
	}// end method main(String[])
}// end class RunGroupServer
