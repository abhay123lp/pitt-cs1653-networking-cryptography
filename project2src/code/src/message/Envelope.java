package message;
import java.util.ArrayList;

/**
 * Holds messages and or objects that are sent between the client, and the file server or group server.
 */
public class Envelope implements java.io.Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7726335089122193103L;
	
	/**
	 * String data type that holds the message to store in the envelope.
	 */
	private String msg;
	
	/**
	 * ArrayList of objects stored in the envelope.
	 */
	private ArrayList<Object> objContents = new ArrayList<Object>(); // Array list of objects to store.
	
	/**
	 * Constructor that holds the message to be passed between the client, file server, and group server.
	 * 
	 * @param text The message to store in the class variable msg.
	 */
	public Envelope(String text)
	{
		msg = text;
	}
	
	/**
	 * This method will return the message in the envelope.
	 * 
	 * @return Returns a String representing the message in the envelope.
	 */
	public String getMessage()
	{
		return msg;
	}
	
	/**
	 * This method will return an ArrayList of objects that are stored in the envelope.
	 * 
	 * @return Returns an ArrayList of objects stored in the envelope.
	 */
	public ArrayList<Object> getObjContents()
	{
		return objContents;
	}
	
	/**
	 * This method will add an object to the objContents array list.
	 * 
	 * @param object The Object to add to the contents.
	 */
	public void addObject(Object object)
	{
		objContents.add(object);
	}
}// end class Envelope
