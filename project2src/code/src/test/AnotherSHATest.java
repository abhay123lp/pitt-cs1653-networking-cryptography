package test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

public class AnotherSHATest {
	
	private MessageDigest md;

	public AnotherSHATest()
	{
		this.md = null;
		try
		{
			this.md = MessageDigest.getInstance("SHA", "BC");
			this.md.reset();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchProviderException e)
		{
			e.printStackTrace();
		}
	}
	
	public void createTest(int numInstances)
	{
		long sumOfTimes = 0;
		byte[] origInput = null;
		byte[] hash = null;
		long startTime = 0;
		byte[] invertHash = null;
		long endTime = 0;
		for(int i = 0; i < numInstances; i++)
		{
			origInput = generateRandomInput(16, 2);
			hash = generateSHAHash(origInput);
			startTime = System.currentTimeMillis();
			invertHash = invertHash(hash, 16, 2);
			endTime = System.currentTimeMillis();
			sumOfTimes += (endTime - startTime);
			System.out.println("It took the last one " + (endTime-startTime) + "ms to invert the hash");
		}
		printArray(origInput);
		printArray(invertHash);
		System.out.println("It took an average of " + ((double)sumOfTimes/numInstances) + "ms to convert");
//		return (new String(origInput)).equals(new String(invertHash));
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
		String counterString = new String(generateSHAHash(counter));
		boolean same = digestString.hashCode() == counterString.hashCode() && digestString.equals(counterString);
		while(!same)
		{
			randomIter[0]++;
			boolean carryOver = randomIter[0] == Byte.MIN_VALUE;
			for(int i = 1; i < lengthOfRandom; i++)
			{
				if(!carryOver)
				{
					break;
				}
				randomIter[i]++;
				carryOver = randomIter[i] == Byte.MIN_VALUE;
			}
			counter = combineBytes(ones, randomIter);
			counterString = new String(generateSHAHash(counter));
			same = digestString.hashCode() == counterString.hashCode() && digestString.equals(counterString);
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
//		this.md.reset();
		this.md.update(array);
		return md.digest();
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
//		System.out.println(ashat.createTest());
		ashat.createTest(100);
	}
}
