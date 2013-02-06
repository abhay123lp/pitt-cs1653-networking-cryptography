
import java.io.BufferedReader;  // Buffered Reader used for reading input
import java.io.IOException; // In the case that the group or file server times out
// or doesn't respond, we import IOException.
import java.io.InputStreamReader; // Used for Buffered Reader 
import java.util.Arrays; // Test only: use to print out String array with Arrays.toString()

public class UserCommands {
/**
 * 
 * Instantiates either file client or group client.
 * The main() method will read in input from the user.
 * The user will specify what server (group or file) they want
 * to talk to and what command they wish to execute.
 */
	
	// want file or group client
	// get the user name from the user
	// 
	
	// both throw IOException
	// BR br = new BR(new InputStreamerReader(System.in))
	// br.readline() // returns a string 
	/**
	 *  
	 * @throws IOException possible with readline() method of BufferedReader
	 * in the case that either the server didn't respond. The user asks for either
	 * the group or file server, and either server could time out, which might
	 * be caused by something like the server dying.
	 */
	public static void main(String [] args)
	{		
		try
		{
			String userInput = "";
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			
			do
			{
				// We need to connect the user to the group server
				GroupClient user = new GroupClient();
				// Ask the user for their username
				System.out.printf("Please enter your user name: ");
				userInput = br.readLine();
				String userToken = user.getToken(userInput);
				// TODO: Finish connecting user to the server and add the commands			
				userInput = br.readLine();
				System.out.printf("You entered: \"%s\"\n", userInput);
				// use split() on s to get array
				String [] userCommands = userInput.split(" "); 
				System.out.printf("Test: %s", Arrays.toString(userCommands));
				
				
				// TODO:
			} while(!userInput.equals("quit")); // Quit when the user tells us to quit
			
			
		} 
		catch(IOException e) 
		{
			e.printStackTrace();		
		} 
		
		// we have the client
		// they want to manage group or file server
		// get their tokens 
		// instantiate a file client or group client
		// ask them for commands
		// this loop doesn't end
		
		// Depending on where they are connecting, parse the commands
		// by calling either the file clients or the group clients methods
		// Then wait for a response from one of the two servers depending
		// on which one they asked for.
	}
	

}
