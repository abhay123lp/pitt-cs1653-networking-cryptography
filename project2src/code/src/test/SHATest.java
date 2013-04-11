import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;

public class SHATest
{
	public static void main(String [] args)
	{
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		
		int keyLength = 16;
		int offset = 15;
		byte [] combinedArray = new byte[keyLength];
		/* The server gets to do this part -- the encryption phase */
		combinedArray = randomArray(keyLength, offset);
		byte [] hash = generateHash(combinedArray);
		byte [] blankOnesArray = new byte[keyLength];
		blankOnesArray = generateOnesOffset(keyLength);
		blankOnesArray = zeroBytes(offset, blankOnesArray);
//		printArray(blankOnesArray);
		/* The user gets to do this part -- the decryption phase */
		byte [] invertedHash = new byte[keyLength];
//		System.out.println(compareArrays(generateHash(combinedArray), hash));
		invertedHash = invertHash(blankOnesArray, hash, keyLength, offset);
	}
	
	/**
	 * 
	 * @param offset
	 * @return Set the bytes after the offset to zero
	 */
	private static byte[] zeroBytes(int offset, byte [] array)
	{
		for(int i = offset; i < array.length; i++)
		{
			array[i] = Byte.MIN_VALUE;
		}
		return array;
	}

	private static byte[] generateHash(byte[] combinedArray) 
	{
		try
		{
			MessageDigest msgDigest = MessageDigest.getInstance("SHA1");
			msgDigest.update(combinedArray);
			// Now we get the salted, hashed password
			byte[] hash = msgDigest.digest();
			
			return hash;
		}
		catch(Exception e)
		{
			e.printStackTrace();	
		}
		return null;
	}

	private static byte[] invertHash(byte [] baseArray, byte [] hash, int keyLength, int offset)
	{
//		printArray(baseArray);		
		int length = keyLength - offset;
		ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
		byte [] temp = new byte[4];
		System.arraycopy(baseArray, 12, temp, 0, 4);
		bb.put(temp).order(ByteOrder.BIG_ENDIAN);
//		printArray(bb.array());
//		System.out.println(bb.getInt(0));
		int byteOffset = (int) Math.pow(2, 8 * (4 - length));
		if(length == 4)
			byteOffset = 0;
		int n = 0;
		while(true)
		{
			temp = bb.order(ByteOrder.BIG_ENDIAN).array();
			System.arraycopy(temp, 0, baseArray, 12, 4);
			if(compareArrays(generateHash(baseArray), hash))
			{
				System.out.println("found the hash: at " + bb.order(ByteOrder.BIG_ENDIAN).getInt(0));
				printArray(baseArray);
				System.out.println(new String(generateHash(baseArray)) + " vs " + new String(hash));
				return baseArray;
			}
			//printArray(bb.array());
//			if(bb.getInt(0) % 10000 == 0)
//				System.out.println(bb.getInt(0) + ": ");
//			printArray(bb.order(ByteOrder.BIG_ENDIAN).array());
			bb = bb.order(ByteOrder.BIG_ENDIAN).allocate(4).putInt(bb.order(ByteOrder.BIG_ENDIAN).getInt(0) + 1 + byteOffset);
			
		}
		
//		byte [] tempArray = new byte[16];
//		while(true)
//		{
//			System.arraycopy(src, srcPos, dest, destPos, length)
//			if(compareArrays(bb.array(), two))
//		}
	}
	
	/**
	 * 
	 * @param one
	 * @param two
	 * @return Return if the two byte arrays are equal
	 */
	private static boolean compareArrays(byte[] one, byte[] two)
	{
		if(one.length != two.length)
		{
			return false;
		}
		else
		{
			for(int i = 0; i < one.length; i++)
			{
				if(one[i] != two[i])
				{
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * 
	 * @param keyLength This is the total length of the key in bytes. We
	 * plan to use 128-bit, so the keylength would be 16 bytes.
	 * @param offsetLength The offset length would be something like 13 bytes.
	 * The offset is the number of 0xFF bytes starting at position 0. The offset
	 * controls how much the user has to generate for the hash inversion. 
	 * 16 - 13 = 3 bytes. 3 bytes = 24 bits. 2^24 ~=16 million numbers to compute
	 * to find out what the hash is.
	 * @return
	 */
	/* try 16 for keylength and 13 for offsetlength */
	private static byte[] randomArray(int keyLength, int offsetLength)
	{
		byte [] onesArray = new byte[offsetLength];
		onesArray = generateOnesOffset(offsetLength);
		byte [] randomArray = new byte[keyLength - offsetLength];
		randomArray = generateRandomBytes(keyLength - offsetLength);
		byte [] combinedArray = new byte[16];
		
		/* combine the two arrays 
		 * The offset starts at byte zero and goes to byte (offsetlength - 1)
		 * The offset is just a simple padding. The bigger the offset length,'
		 * the easier it will be to invert the hash.
		 * */
		
		System.arraycopy(onesArray, 0, combinedArray, 0, onesArray.length);
		System.arraycopy(randomArray, 0, combinedArray, offsetLength, keyLength - offsetLength);
		
		return combinedArray;
	}
	
	/**
	 * Generate a random byte array based on the specified integer
	 */
	private static byte[] generateRandomBytes(int i)
	{
		SecureRandom random = new SecureRandom();
		byte [] array = new byte[i];
		random.nextBytes(array);
		return array;
	}
	
	/**
	 * 
	 * @param length The size of the ones offset byte array to generate
	 * @return Return the ones offset array
	 */
	private static byte[] generateOnesOffset(int length)
	{
		// byte = 1111-1111 (0xFF)
		byte b = 127;
		byte[] array = new byte[length];
		for(int i = 0; i < length; i++)
		{
			array[i] = b;
		}
		return array;
	}
	
	private static void printArray(byte [] array)
	{
		for(int i = 0; i < array.length; i++)
		{
			System.out.println(i + ": " + array[i]);
		}
	}
	
	private static int bbToInt(byte [] byteBarray)
	{
	    return ByteBuffer.wrap(byteBarray).getInt();
	}

	private static  byte[] intToBB(int myInteger)
	{
	    return ByteBuffer.allocate(4).putInt(myInteger).array();
	}
}
