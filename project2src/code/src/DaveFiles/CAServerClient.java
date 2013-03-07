import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.interfaces.RSAPublicKey;


public class CAServerClient extends Client {
	
		
	private String serverName;
	private RSAPublicKey publicKey;
	
	
	public CAServerClient(){
						
	}
	
	public String getServerName(){
		return this.serverName;
	}
	
	
	public RSAPublicKey getPublicKey(){
		return this.publicKey;
	}
	
	public String getPublicKey(String serverName){
		
		return null;
		
	}
	
	public boolean passAuthorizationData(String server, RSAPublicKey pubKey ){
		
		this.serverName = server;
		this.publicKey = pubKey;
		
		try{
						
			Envelope response = new Envelope("CAREQUEST");
			response.addObject(server);
			response.addObject(this.publicKey);
			output.writeObject(response);
			boolean proceed = true;
			
			do
			{				
				Envelope message = (Envelope)input.readObject();
				System.out.println("Request received: " + message.getMessage());
				
				if (message.getMessage().equals("OK")) // Everything worked with CA
				{
				    
					disconnect();
					proceed = false; // End this communication loop
					
					return true;
					
				} else {
					
					return false;
					
				}
				
				
			} while(proceed);
			
								
			
		} catch(Exception ex){
			
			System.out.println(ex.toString());
			
		}
		
		return false;
				
	}
	
	
	
	

}
