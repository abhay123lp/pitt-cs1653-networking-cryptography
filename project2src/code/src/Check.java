import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class Check
{
	private static final String ALGORITHM = "rsa";
	private static final String RAND_ALG = "SHA1PRNG";
	private static final String PROVIDER = "BC";
	private static final int KEY_SIZE = 2048;
	
	public Check()
	{
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args) throws Exception
	{
		SecureRandom random = SecureRandom.getInstance(RAND_ALG, "SUN");
		
		KeyGenerator keygen = KeyGenerator.getInstance("aes", PROVIDER);
//		keygen.initialize(160, random);
		SecretKey k = keygen.generateKey();
		
		KeyPairGenerator rsaKeyGenerator = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
		rsaKeyGenerator.initialize(KEY_SIZE, random);
		KeyPair rsaKeyPair = rsaKeyGenerator.generateKeyPair();
		// Private Key
		RSAPrivateKey privateKey = (RSAPrivateKey)rsaKeyPair.getPrivate();
		// Public key
		RSAPublicKey publicKey = (RSAPublicKey)rsaKeyPair.getPublic();
		
		Cipher objCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", PROVIDER);
		objCipher.init(Cipher.ENCRYPT_MODE, publicKey);
		byte[] encrypted = objCipher.doFinal(k.getEncoded());
		
		objCipher.init(Cipher.DECRYPT_MODE, privateKey); 
		byte[] unencrypted = objCipher.doFinal(encrypted);
		
		SecretKey retrievedKey = new SecretKeySpec(unencrypted, "aes");
		System.out.println(k.equals(retrievedKey));
	}
	
}
