package id.phone.sdk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.flurry.android.FlurryAgent;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import id.phone.sdk.Commons;
import id.phone.sdk.PhoneId;

import static id.phone.sdk.utils.LogUtils.LOGD;
import static id.phone.sdk.utils.LogUtils.LOGW;

/**
 * Created by azubchenko on 04/19/15.
 * TODO: hide log records from user
 */
public class IdentityHandler {

    static private final String TAG = LogUtils.makeLogTag(IdentityHandler.class);

    static private byte[] tokenSalt = null;

    /**
     * Saving encoded token into private preference
     *
     * @param context
     * @param token
     */
    static public void saveToken(Context context, String token)
    {
        //encoding
        DecryptorEncryptor decryptorEncryptor = new DecryptorEncryptor(PhoneId.getDeviceId(), tokenSalt);
        String encodedToken = decryptorEncryptor.encrypt(token);

        //saving into preferences
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PhoneId.getClientId(), Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(Commons.PREF_TOKEN, encodedToken);
        edit.apply();
    }

    /**
     * Retrieving token from private preferences
     *
     * @param context
     * @return
     */
    static public String getToken(Context context)
    {
        String token = null;
        try
		{
			DecryptorEncryptor decryptorEncryptor =
				new DecryptorEncryptor(PhoneId.getDeviceId(), tokenSalt);

			//reading encoded token from preferences
			SharedPreferences prefs = context.getApplicationContext()
				.getSharedPreferences(PhoneId.getClientId(), Context.MODE_PRIVATE);
			String encodedToken = prefs.getString(Commons.PREF_TOKEN, "");

			//decoding token
			token = decryptorEncryptor.decrypt(encodedToken);
		}
		catch (Exception ex)
		{
			LOGD(TAG, "getToken", ex);
		}
        return token;
    }

    /**
     * Clearing token - say it's logout action
     *
     * @param context
     */
    static public void clearToken(Context context) {

        //saving into preferences
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PhoneId.getClientId(), Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.remove(Commons.PREF_TOKEN);
        edit.apply();
    }

    /**
     * Saving encoded user into private preference
     *
     * @param context
     * @param user
     */
    static public void saveUser(Context context, String user) {

        //encoding
        DecryptorEncryptor decryptorEncryptor = new DecryptorEncryptor(PhoneId.getDeviceId(), tokenSalt);
        String encodedUser = decryptorEncryptor.encrypt(user);

        //saving into preferences
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PhoneId.getClientId(), Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(Commons.PREF_USER, encodedUser);
        edit.apply();
    }

    /**
     * Retrieving user from private preferences
     *
     * @param context
     * @return
     */
    static public String getUser(Context context)
	{
        String user = null;
		try
		{
			DecryptorEncryptor decryptorEncryptor =
				new DecryptorEncryptor(PhoneId.getDeviceId(), tokenSalt);

			//reading encoded token from preferences
			SharedPreferences prefs = context.getApplicationContext()
				.getSharedPreferences(PhoneId.getClientId(), Context.MODE_PRIVATE);
			String encodedUser = prefs.getString(Commons.PREF_USER, "");

			//decoding user data
			user = decryptorEncryptor.decrypt(encodedUser);
		}
		catch (Exception ex)
		{
			LOGD(TAG, "getUser", ex);
		}

        return user;
    }

    /**
     * Clearing user - say it's logout action
     *
     * @param context
     */
    static public void clearUser(Context context) {

        //saving into preferences
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PhoneId.getClientId(), Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.remove(Commons.PREF_USER);
        edit.apply();
    }

    /**
     * We need this method to generate salt to encode our private data
     *
     * @param context
     */
    static public void initSalt(Context context) {
        try {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PhoneId.getClientId(), Context.MODE_PRIVATE);
            String base64Salt = prefs.getString(Commons.PREF_SALT, null);
            //no salt generated, lets create one
            if (base64Salt == null) {
                SecretKey secretKey = DecryptorEncryptor.generateSalt();
                base64Salt = Base64.encodeToString(secretKey.getEncoded(), Base64.NO_WRAP);
                tokenSalt = secretKey.getEncoded();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Commons.PREF_SALT, base64Salt);
                editor.apply();
            } else {
                tokenSalt = Base64.decode(base64Salt, Base64.NO_WRAP);
            }
        } catch (NoSuchAlgorithmException e)
		{
            FlurryAgent.onError(TAG, "Cannot find the algorithm: ", e);
        }
    }

    /**
     * This is the encryptor/decryptor for our salt and token and key generator
     */
    static private class DecryptorEncryptor {
        static private final String ALGORITHM = "AES";
        private Cipher ecipher;
        private Cipher dcipher;
        private SecretKey key;
        private String deviceId;
        private byte[] salt;

        public DecryptorEncryptor(String deviceId, byte[] salt) {

            this.deviceId = deviceId;
            this.salt = salt;
            try {

                key = generateKey();

                ecipher = Cipher.getInstance(ALGORITHM);
                ecipher.init(Cipher.ENCRYPT_MODE, key);

                dcipher = Cipher.getInstance(ALGORITHM);
                dcipher.init(Cipher.DECRYPT_MODE, key);
            } catch (javax.crypto.NoSuchPaddingException | NoSuchAlgorithmException | java.security.InvalidKeyException e) {
                LOGD(TAG, "failed to create DecriptorEncriptor: ", e);

            } catch (Exception e) {
                LOGD(TAG, "failed to create DecriptorEncriptor: ", e);
            }
        }

        public static SecretKey generateSalt() throws NoSuchAlgorithmException {
            // Generate a 256-bit key
            final int outputKeyLength = 256;

            SecureRandom secureRandom = new SecureRandom();
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(outputKeyLength, secureRandom);
            SecretKey key = keyGenerator.generateKey();
            return key;
        }

        public SecretKey generateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
            // Number of PBKDF2 hardening rounds to use. Larger values increase
            // computation time. You should select a value that causes computation
            // to take >100ms.
            final int iterations = 1000;

            // Generate a 256-bit key
            final int outputKeyLength = 256;

            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec keySpec = new PBEKeySpec(deviceId.toCharArray(), salt, iterations, outputKeyLength);
            SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
            return secretKey;
        }

        public String decrypt(String str) {
            try {
                // Decode base64 to get bytes
                byte[] dec = Base64.decode(str, Base64.NO_WRAP);

                // Decrypt
                byte[] utf8 = dcipher.doFinal(dec);

                // Decode using utf-8
                return new String(utf8, "UTF8");
            } catch (javax.crypto.BadPaddingException | IllegalBlockSizeException | UnsupportedEncodingException e) {
                LOGW(TAG, "failed to decrypt: ", e);
            } catch (Exception e) {
                LOGW(TAG, "failed to decrypt: ", e);
            }
            return "";
        }

        public String encrypt(String str) {
            try {
                // Encode the string into bytes using utf-8
                byte[] utf8 = str.getBytes("UTF8");

                // Encrypt
                byte[] enc = ecipher.doFinal(utf8);

                // Encode bytes to base64 to get a string
                String result = Base64.encodeToString(enc, Base64.NO_WRAP);

                return result;
            } catch (javax.crypto.BadPaddingException | IllegalBlockSizeException | UnsupportedEncodingException e) {
                LOGW(TAG, "failed to encrypt: ", e);
            } catch (Exception e) {
                LOGW(TAG, "failed to encrypt: ", e);
            }
            return "";
        }
    }
}
