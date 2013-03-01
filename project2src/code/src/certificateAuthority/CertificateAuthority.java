package certificateAuthority;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Assumes this is up 24/7, so we do not deal with writing out upon shut down.
 *
 */
public class CertificateAuthority extends Server
{
	protected ArrayList<String> serverList;
	protected HashMap<String, String> serverPublicKeyPairs; //server name --> public key
	
	private RSAPrivateKey privateKey;
	private RSAPublicKey publicKey;
	
	private static final String ALGORITHM = "rsa";
	private static final String RAND_ALG = "SHA1PRNG";
	private static final String PROVIDER = "bc";
	private static final int KEY_SIZE = 1024;
	
	private static final int DEF_PORT = 34567;
	private static final String NAME = "Certificate Authority";
	
	public CertificateAuthority(int port)
	{
		super(DEF_PORT, NAME);
		this.serverList = new ArrayList<>();
		this.serverPublicKeyPairs = new HashMap<>();
		
		SecureRandom random = SecureRandom.getInstance(RAND_ALG, PROVIDER);
		
		KeyPairGenerator rsaKeyGenerator = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
		rsaKeyGenerator.initialize(KEY_SIZE, random);
		KeyPair rsaKeyPair = rsaKeyGenerator.generateKeyPair();
		
		// Private Key
		this.privateKey = (RSAPrivateKey)rsaKeyPair.getPrivate();
		// Public key
		this.publicKey = (RSAPublicKey)rsaKeyPair.getPublic();
	}
	
	public void start()
	{
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutDownListenerCA()));
		ScheduledExecutorService timer = new ScheduledThreadPoolExecutor(1);
		timer.scheduleAtFixedRate(new AutoSaveCA(this), 0, 3, TimeUnit.SECONDS);
//		timer.schedule(new AutoSaveCA(this), 30000);
//		timer.start();
	}
	
}

/**
 * This thread saves the user and group lists upon shutdown.
 */
class ShutDownListenerCA extends Thread
{
	/**
	 * The group server to write out.
	 */
//	public CertificateAuthority certificateAuthority;
	
	private static final String TEMP_FILE_NAME = "caTemp.tmp";
	
	/**
	 * The constructor.
	 * Takes in the GroupServer to write out.
	 * 
	 * @param certificateAuthority The GroupServer to write out.
	 */
//	public ShutDownListenerCA(CertificateAuthority certificateAuthority)
//	{
//		this.certificateAuthority = certificateAuthority;
//	}
	
	public ShutDownListenerCA()
	{
		super();
	}
	
	public void run()
	{
		System.out.println("Shutting down server");
		File f = new File(TEMP_FILE_NAME);
		f.delete();
	}// end method run()
}// end class ShutDownListener

/**
 * This thread automatically saves the user and group lists every five minutes.
 */
class AutoSaveCA extends Thread
{
	/**
	 * The GroupServer to write out.
	 */
	private final CertificateAuthority certificateAuthority;
	
	private static final String TEMP_FILE_NAME = "caTemp.tmp";
	
	/**
	 * The constructor.
	 * Takes in the GroupServer to write out.
	 * 
	 * @param certificateAuthority The GroupServer to write out.
	 */
	public AutoSaveCA(CertificateAuthority certificateAuthority)
	{
		this.certificateAuthority = certificateAuthority;
	}
	
	public void run()
	{
		do
		{
			try
			{
//				Thread.sleep(300000); // Save group and user lists every 5 minutes
				System.out.println("Autosave group and user lists...");
				ObjectOutputStream outStream;
				try
				{
					outStream = new ObjectOutputStream(new FileOutputStream(TEMP_FILE_NAME));
					outStream.writeObject(this.certificateAuthority.serverList);
					outStream.writeObject(this.certificateAuthority.serverPublicKeyPairs);
					outStream.flush();
					outStream.close();
				}
				catch (Exception e)
				{
					System.err.println("Error: " + e.getMessage());
					e.printStackTrace(System.err);
				}
			}// end try block
			catch (Exception e)
			{
				System.out.println("Autosave Interrupted");
			}
		} while (true); // end do while
	}// end method run()
}//end class AutoSave
