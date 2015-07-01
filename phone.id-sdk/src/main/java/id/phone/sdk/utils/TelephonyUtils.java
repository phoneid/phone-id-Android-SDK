package id.phone.sdk.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.util.Locale;

/**
 * Created by azubchenko on 3/31/15.
 */
public class TelephonyUtils {

    static public final String TAG = TelephonyUtils.class.getSimpleName();

    /**
     * A call to get a phone number if possible
     *
     * @param context
     * @return
     */
    static public String getPhoneNumber(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getLine1Number();
    }


    /**
     * Trying to obtain current country
     *
     * @return
     */
    static public String getCurrentCountryIso(Context context) {
        String countryIso = "";

        //getting country code from telephonyManager
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        countryIso = TextUtils.isEmpty(telephonyManager.getSimCountryIso()) ? countryIso : telephonyManager.getSimCountryIso();

        //making sure we return at least default country
        return TextUtils.isEmpty(countryIso) ? "" : countryIso.toUpperCase();
    }

    /**
     * Checking network availability
     *
     * @param context
     * @return
     */
    static public boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isAvailable() && connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}
