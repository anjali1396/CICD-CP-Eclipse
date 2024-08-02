package utils;

import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyBearer {
	
	private SecretKeySpec secretKey = null;
	private Cipher cipher = null;
	
	public KeyBearer(String key, String salt) {
		
		if (null == cipher || null == secretKey) {
			
			try {
				
				SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");			
		        KeySpec spec = new PBEKeySpec(key.toCharArray(), salt.getBytes(), 65536, 256);
		        SecretKey tmp = factory.generateSecret(spec);
		        secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
		        
		        cipher = Cipher.getInstance("AES");
	        
			} catch (Exception e) {
				LoggerUtils.log("Error while initiating KeyBearer object: " + e.getMessage());
				e.printStackTrace();
			}
			
		}
		
	}
	
	public String encrypt(String plainText) throws Exception {
	 
        byte[] plainTextByte = plainText.getBytes();
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedByte = cipher.doFinal(plainTextByte);
        Base64.Encoder encoder = Base64.getEncoder();
        String encryptedText = encoder.encodeToString(encryptedByte);
        
        return encryptedText;
        
	}

	public String decrypt(String encryptedText) throws Exception {
	 
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] encryptedTextByte = decoder.decode(encryptedText);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedByte = cipher.doFinal(encryptedTextByte);
        String decryptedText = new String(decryptedByte);
        
        return decryptedText;
        
    }

}
