package id.phone.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

/**
 * Created by azubchenko on 04/01/15.
 */
public class Commons {

    //Connection
    static public final int DEFAULT_CONNECTION_TIMEOUT = 5000;
    static public final int DEFAULT_READ_TIMEOUT = 10000;

    //PREFERENCES
    static public final String PREF_SALT = "prf_salt";
    static public final String PREF_TOKEN = "prf_token";
    static public final String PREF_USER = "prf_user";
    static public final String PREF_CLIENT_APP_NAME = "client_app_name";
    static public final String PREF_SIM_CARDS = "sim_cards";
    static public final String PREF_SIM_LOGOUT_REQUIRED = "sim_card_logout_required";
    static public final String PREF_SYNC_CONTACTS = "sync_contacts";
    static public final String PREF_CONTACTS_LAST_SYNC = "contacts_last_sync";

    //INTENT/FRAGMENT ARGUMENTS
    static public final String PARAM_PHONE = "phone";

    static public final String PARAM_CODE = "code";
    //LOCAL BROADCASTSS
    static public final String COUNTRY_PICKED_ACTION = "country.picked.action";

    //LOADERS IDS
    static public final int COUNTRIES_LOADER = 10000;

    //MISC
    static public final String DEFAULT_COUNTRY_CODE = "US";
    static public final String FLURRY_ID = "6HTX3RG5CJVQZW89DXKQ";
    public static final String AUTH_TOKEN_TYPE = "AUTH_TOKEN_TYPE";

    /**
     * Gets the state of Airplane Mode.
     *
     * @param context
     * @return true if enabled.
     */
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isAirplaneModeOn(Context context)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            return Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

    final protected static char[] hexArray = "0123456789abcdef".toCharArray();
    public static String bytesToHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
