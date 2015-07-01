package id.phone.sdk.telephony;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import java.util.List;

import id.phone.sdk.Commons;
import id.phone.sdk.PhoneId;
import id.phone.sdk.utils.LogUtils;

import static id.phone.sdk.utils.LogUtils.LOGD;

/**
 * Created by dennis on 28.05.15.
 * Manage information about installed SIM-cards. Detect the condition when any card was replaced
 * or card with number used for login was removed
 */
public class SIMCardManager
{
	public static final String TAG = LogUtils.makeLogTag(SIMCardManager.class);

	private Context mContext;
	private static SIMCardManager mInstance;
	private Gson gson = new Gson();

	public class SIMCardInfo
	{
		private String serialNumber;
		private String IMEI;
		private String phoneNumber;

		public String getSerialNumber() { return serialNumber; }
		public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
		public String getIMEI() { return IMEI; }
		public void setIMEI(String IMEI) { this.IMEI = IMEI; }
		public String getPhoneNumber()  { return phoneNumber; }
		public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

		@Override
		public boolean equals(Object o)
		{
			if (o != null && o instanceof SIMCardInfo)
			{
				return (
					(!TextUtils.isEmpty(phoneNumber) && phoneNumber.equals(((SIMCardInfo)o).getPhoneNumber())) ||
					(!TextUtils.isEmpty(serialNumber) && serialNumber.equals(((SIMCardInfo)o).getSerialNumber())) ||
					(!TextUtils.isEmpty(IMEI) && IMEI.equals(((SIMCardInfo)o).getIMEI())) ||
					(TextUtils.isEmpty(phoneNumber) && TextUtils.isEmpty(((SIMCardInfo)o).getPhoneNumber()) &&
						TextUtils.isEmpty(serialNumber) && TextUtils.isEmpty(((SIMCardInfo)o).getSerialNumber()) &&
						TextUtils.isEmpty(IMEI) && TextUtils.isEmpty(((SIMCardInfo)o).getIMEI()))
				);
			}
			else return super.equals(o);
		}
	}

	public class SIMCardSet
	{
		private SIMCardInfo [] cards;
		public SIMCardInfo[] getCards() { return cards; }
		public void setCards(SIMCardInfo[] cards) { this.cards = cards; }

		public SIMCardSet(SIMCardInfo [] cards)
		{
			this.cards = cards;
		}

		public boolean isCardWithPhoneNumberPresent(String phoneNumber)
		{
			boolean result = false;
			if (cards != null)
			{
				for (SIMCardInfo info: cards)
				{
					if (phoneNumber.equals(info.getPhoneNumber()))
					{
						result = true;
						break;
					}
				}
			}
			return result;
		}
	}

	private SIMCardSet simCards;

	public static SIMCardManager getInstance(Context context)
	{
		if (mInstance == null) mInstance = new SIMCardManager(context);
		return  mInstance;
	}

	private SIMCardManager(Context context)
	{
		this.mContext = context;
	}

	private SIMCardInfo [] readCardInfoFromTelephony()
	{
		SIMCardInfo [] simCards;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
		{
			SubscriptionManager sm = SubscriptionManager.from(mContext);
			List<SubscriptionInfo> list = sm == null ? null : sm.getActiveSubscriptionInfoList();
			if (list != null && !list.isEmpty())
			{
				simCards = new SIMCardInfo[list.size()];
				for (int idx = 0; idx < list.size(); idx++)
				{
					SubscriptionInfo info = list.get(idx);
					simCards[idx] = new SIMCardInfo();
					simCards[idx].phoneNumber = info.getNumber();
					simCards[idx].serialNumber = info.getIccId();
					simCards[idx].IMEI = info.getIccId();
				}
			}
			else
			{
				TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
				simCards = new SIMCardInfo[1];
				simCards[0] = new SIMCardInfo();
				simCards[0].phoneNumber = telephonyManager.getLine1Number();
				simCards[0].serialNumber = telephonyManager.getSimSerialNumber();
				simCards[0].IMEI = telephonyManager.getDeviceId();
			}
		}
		else
		{
			TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
			simCards = new SIMCardInfo[1];
			simCards[0] = new SIMCardInfo();
			simCards[0].phoneNumber = telephonyManager.getLine1Number();
			simCards[0].serialNumber = telephonyManager.getSimSerialNumber();
			simCards[0].IMEI = telephonyManager.getDeviceId();
		}
		return simCards;
	}

	/**
	 * Called by PhoneId when logged-in
	 * Stores currently installed cards information
	 * @param phoneNumber used for login
	 */
	public void onLoggedIn(String phoneNumber)
	{
		SIMCardInfo [] simCards = readCardInfoFromTelephony();
		// Look for card with phone number
		boolean numberFound = false;
		for (SIMCardInfo info: simCards)
		{
			if (phoneNumber.equals(info.getPhoneNumber()))
				numberFound = true;
		}
		if (!numberFound)
			for (SIMCardInfo info: simCards)
			{
				if (TextUtils.isEmpty(info.phoneNumber))
				{
					info.phoneNumber = phoneNumber;
					break;
				}
			}
		SIMCardSet set = new SIMCardSet(simCards);
		try
		{
			SharedPreferences prefs = mContext.getApplicationContext()
				.getSharedPreferences(PhoneId.getClientId(), Context.MODE_PRIVATE);
			String json = gson.toJson(set);
			prefs.edit()
				.putString(Commons.PREF_SIM_CARDS, json)
				.putBoolean(Commons.PREF_SIM_LOGOUT_REQUIRED, false)
				.apply();
		}
		catch (Exception ex)
		{
			LOGD(TAG, "onLoggedIn", ex);
		}
	}

	/**
	 * Check stored logout required condition
	 * @return true if card was previously replaced and logout required
	 */
	public boolean isLogoutRequired()
	{
		boolean result = false;
		try
		{
			SharedPreferences prefs = mContext.getApplicationContext()
				.getSharedPreferences(PhoneId.getClientId(), Context.MODE_PRIVATE);
			result = prefs.getBoolean(Commons.PREF_SIM_LOGOUT_REQUIRED, false);
		}
		catch (Exception ex)
		{
			LOGD(TAG, "isLogoutRequired", ex);
		}
		return result;
	}

	/**
	 * Check installed SIM-cards against previously saved information.
	 * If no card associated with phoneNumber present or card replacement detected, then
	 * set Commons.PREF_SIM_LOGOUT_REQUIRED flag and return true
	 * @param phoneNumber phone numbed used for login
	 * @return true if card was replaced and logout required
	 */
	public boolean checkLogoutCondition(String phoneNumber)
	{
		boolean result = false;
		try
		{
			SharedPreferences prefs = mContext.getApplicationContext()
				.getSharedPreferences(PhoneId.getClientId(), Context.MODE_PRIVATE);
			String json = prefs.getString(Commons.PREF_SIM_CARDS, "{\"cards\":[]}");
			SIMCardSet storedSIMCards = gson.fromJson(json, SIMCardSet.class);
			SIMCardSet installedSIMCards = new SIMCardSet(readCardInfoFromTelephony());

			// If card with desired number present, then nothing to do.
			if (!TextUtils.isEmpty(phoneNumber) && installedSIMCards.isCardWithPhoneNumberPresent(phoneNumber))
			{
				prefs.edit()
					.putBoolean(Commons.PREF_SIM_LOGOUT_REQUIRED, false)
					.apply();
				return false;
			}

			// Try to find any card replacements. Return true if any
			boolean anyAbsent = false;
			for (SIMCardInfo stored: storedSIMCards.getCards())
			{
				boolean found = false;
				for (SIMCardInfo installed: installedSIMCards.getCards())
				{
					if (stored.equals(installed))
					{
						found = true;
						break;
					}
				}
				if (!found) anyAbsent = true;
			}
			// Any card was replaced. Logging out
			if (anyAbsent)
			{
				prefs.edit()
					.putBoolean(Commons.PREF_SIM_LOGOUT_REQUIRED, true)
					.apply();
				result = true;
			}
		}
		catch (Exception ex)
		{
			LOGD(TAG, "isLogoutRequired", ex);
		}
		return result;
	}
}
