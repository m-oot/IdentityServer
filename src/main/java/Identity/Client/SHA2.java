package Identity.Client;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Caveat Emptor: Research the security of these methods before using them!
 *
 * The MD5 message digest algorithm as defined in RFC 1321.
 *
 * SHA-1 Hash algorithms defined in the FIPS PUB 180-2. SHA-256 is a
 * 256-bit hash function intended to provide 128 bits of security
 * against collision attacks, while SHA-512 is a 512-bit hash function
 * intended to provide 256 bits of security.
 */
public class SHA2
{
    /**
     * Try encoding with MD5
     *
     * @param input
     * @throws NoSuchAlgorithmException
     */
    private static void tryMD5(String input) throws NoSuchAlgorithmException {
        System.out.println();
        MessageDigest md = MessageDigest.getInstance("MD5");
        System.out.println(md);
        byte[] bytes = input.getBytes();
        md.reset();

        byte[] result = md.digest(bytes);
        System.out.println();
        System.out.print("length (bits): " + result.length * 8 + " MD5sum: ");

        for (int i = 0; i < result.length; i++)
            System.out.printf("%X", result[i]);
        System.out.println();
    }


    /**
     * Try encoding with SHA 512
     *
     * @param input
     * @throws NoSuchAlgorithmException
     */
    public static String trySHA(String input){
//        System.out.println();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
//        System.out.println(md);
        byte[] bytes = input.getBytes();
        md.reset();
        byte[] result = md.digest(bytes);
//        System.out.println();
//        System.out.print("length (bits): " + result.length * 8 + " SHA-512: ");

        String stringResult = "";
        for (int i = 0; i < result.length; i++) {
//            System.out.printf("%X", result[i]);
            stringResult += String.format("%X", result[i]);
        }

        return stringResult;
    }


    /**
     * @param args
     */
    public static void main(String args[]) {
        if (args.length == 0) {
            System.err.println("Usage: java SHA2Test <string to encode>");
            System.exit(1);
        }
        try {
            tryMD5(args[0]);
            trySHA(args[0]);
        } catch (NoSuchAlgorithmException e) {
            System.err.println(e);
        }
    }
}

