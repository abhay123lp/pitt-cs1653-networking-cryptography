package test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

public class AnotherSHATest {

	public AnotherSHATest() {
		// TODO Auto-generated constructor stub
	}
	
	public boolean createTest()
	{
		byte[] origInput = generateRandomInput(16, 3);
		byte[] hash = generateSHAHash(origInput);
		byte[] invertHash = invertHash(hash, 16, 3);
		printArray(origInput);
		printArray(invertHash);
		return (new String(origInput)).equals(new String(invertHash));
	}
	
	public byte[] invertHash(byte[] digest, int lengthOfOrig, int lengthOfRandom)
	{
		byte[] ones = generateAllOnes(lengthOfOrig-lengthOfRandom);
		byte[] randomIter = new byte[lengthOfRandom];
		for(int i = 0; i < lengthOfRandom; i++)
		{
			randomIter[i] = Byte.MIN_VALUE;
		}
		String digestString = new String(digest);
		byte[] counter = combineBytes(ones, randomIter);
		boolean same = digestString.equals(new String(generateSHAHash(counter)));
		while(!same)
		{
			randomIter[0]++;
			boolean carryOver = randomIter[0] == Byte.MIN_VALUE;
			for(int i = 1; i < lengthOfRandom; i++)
			{
				if(carryOver)
				{
					randomIter[i]++;
					carryOver = randomIter[i] == Byte.MIN_VALUE;
				}
			}
			counter = combineBytes(ones, randomIter);
			same = digestString.equals(new String(generateSHAHash(counter)));
			if(carryOver)
			{
				//Means it exhausted all possibilities, but did not find the correct thing.
				break;
			}
		}
		return counter;
	}
	
	public byte[] generateSHAHash(byte[] array)
	{
		try {
			MessageDigest md = MessageDigest.getInstance("SHA", "BC");
			md.update(array);
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public byte[] generateRandomInput(int totalSize, int lengthOfRandom)
	{
		return combineBytes(generateAllOnes(totalSize-lengthOfRandom), generateRandomBytes(lengthOfRandom));
	}
	
	public byte[] generateAllOnes(int length)
	{
		byte[] ones = new byte[length];
		byte one = Byte.MAX_VALUE;
		for(int i = 0; i < length; i++)
		{
			ones[i] = one;
		}
		return ones;
	}
	
	public byte[] generateRandomBytes(int length)
	{
		SecureRandom secRand = new SecureRandom();
		byte[] randBytes = new byte[length];
		secRand.nextBytes(randBytes);
		return randBytes;
	}
	
	public byte[] combineBytes(byte[] array1, byte[] array2)
	{
		byte[] total = new byte[array1.length+array2.length];
		System.arraycopy(array1, 0, total, 0, array1.length);
		System.arraycopy(array2, 0, total, array1.length, array2.length);
		return total;
	}
	
	private static void printArray(byte[] array)
	{
		System.out.print("[");
		for(int i = 0; i < array.length; i++)
		{
			System.out.print(array[i] + ",");
		}
		System.out.println("]");
	}
	
	public static void main(String[] args)
	{
		AnotherSHATest ashat = new AnotherSHATest();
		System.out.println(ashat.createTest());
	}
}
