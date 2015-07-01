package id.phone.sdk.service;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.gson.Gson;

import id.phone.sdk.Commons;
import id.phone.sdk.PhoneId;
import id.phone.sdk.R;
import id.phone.sdk.data.DatabaseContract;
import id.phone.sdk.data.DatabaseProvider;
import id.phone.sdk.data.model.Country;
import id.phone.sdk.rest.PhoneIdRestClient;
import id.phone.sdk.rest.RestCallback;
import id.phone.sdk.rest.response.ContactsRefreshNeededResponse;
import id.phone.sdk.rest.response.ErrorResponse;
import id.phone.sdk.rest.response.TokenResponse;
import id.phone.sdk.rest.response.UploadContactsResponse;
import id.phone.sdk.utils.LogUtils;
import retrofit.RetrofitError;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import static id.phone.sdk.utils.LogUtils.LOGD;

/**
 * Created by azubchenko on 4/6/15.
 * Updated by Dennis Gubsky
 */

public class FirstRunService extends IntentService
{
    private static final String TAG = LogUtils.makeLogTag(FirstRunService.class);

    public static final String ACTION_IMPORT_COUNTRIES = FirstRunService.class.getName() + ".ACTION_IMPORT_COUNTRIES";
    public static final String ACTION_ADD_SMS_TO_REMOVE = FirstRunService.class.getName() + ".ACTION_ADD_SMS_TO_REMOVE";
	public static final String ACTION_REMOVE_SMS = FirstRunService.class.getName() + ".ACTION_REMOVE_SMS";
	public static final String ARG_ADDRESS = "ARG_ADDRESS";
	public static final String ARG_TIMESTAMP = "ARG_TIMESTAMP";
    public static final String ACTION_CHECK_CONTACTS = FirstRunService.class.getName() + ".ACTION_CHECK_CONTACTS";
    public static final String ACTION_UPLOAD_CONTACTS = FirstRunService.class.getName() + ".ACTION_UPLOAD_CONTACTS";

    private static class SMSProps
    {
        public final String address;
        public final long timestamp;
        public SMSProps(String address, long timestamp)
        {
            this.address = address;
            this.timestamp = timestamp;
        }
    };

	private Gson gson = new Gson();
    private static ArrayList<SMSProps> incomingSMS = new ArrayList<>();
	private PowerManager.WakeLock wl;

    public FirstRunService()
	{
        super(TAG);
    }

	@Override
	public void onCreate()
	{
		super.onCreate();
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	@Override
    protected void onHandleIntent(Intent intent)
    {
		boolean releaseWakeLock = true;
		if (!wl.isHeld())
			wl.acquire();
		else
			releaseWakeLock = false;	// this lock already requested by other call and no need to release it here
		try
		{
			if (intent != null)
			{
				if (ACTION_IMPORT_COUNTRIES.equals(intent.getAction()))
					importCountries();
				else if (ACTION_ADD_SMS_TO_REMOVE.equals(intent.getAction()))
				{
					synchronized (incomingSMS)
					{
						incomingSMS.add(new SMSProps(intent.getStringExtra(ARG_ADDRESS),
							intent.getLongExtra(ARG_TIMESTAMP, 0)));
					}
					RemoveSMSAlarm.setAlarm(this, 2500);
				}
				else if (ACTION_REMOVE_SMS.equals(intent.getAction()))
				{
					ArrayList<SMSProps> incomingSMScopy;
					synchronized (incomingSMS)
					{
						incomingSMScopy = new ArrayList<>(incomingSMS);
						incomingSMS.clear();
					}
					try
					{
						ContentResolver resolver = getContentResolver();
						Uri deleteUri = Uri.parse("content://sms");
						for (SMSProps props : incomingSMScopy)
						{
							resolver.delete(deleteUri, "address=? and date=?"
								, new String[]
								{props.address, String.valueOf(props.timestamp)});
						}
					}
					catch (Exception ex)
					{
						LOGD(TAG, "delete SMS", ex);
					}
				}
				else if (ACTION_CHECK_CONTACTS.equals(intent.getAction()))
				{
					if (checkContacts())
						releaseWakeLock = false;
				}
				else if (ACTION_UPLOAD_CONTACTS.equals(intent.getAction()))
				{
					if (uploadContacts())
						releaseWakeLock = false;
				}
			}
		}
		catch (Exception ex)
		{
			LOGD(TAG, "onHandleIntent", ex);
		}
		finally
		{
			if (releaseWakeLock && wl.isHeld())
				wl.release();
		}
    }

    /**
     *
     *
     */
    private void importCountries() {

        //if no phid_countries found in the database then take them from local storage
        try {
            if (DatabaseProvider.checkCount(this, DatabaseContract.CountryEntry.CONTENT_URI, null, null) == 0) {
                InputStream is = getResources().openRawResource(R.raw.phid_countries);
                Writer writer = new StringWriter();
                char[] buffer = new char[1024];
                //reading phid_countries from resource file
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, n);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                String jsonString = writer.toString();
                JSONArray json = new JSONArray();
                try {
                    json = new JSONArray(jsonString);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //ok, we have json with phid_countries, now we can insert them in db
                if (json.length() > 0)
				{
                    Country country;
                    Locale locale;
                    ContentValues[] values = new ContentValues[json.length()];
                    Locale defaultLocale = Locale.getDefault();
                    for (int i = 0; i < json.length(); i++) {
                        country = gson.fromJson(json.get(i).toString(), Country.class);
                        locale = new Locale(defaultLocale.getISO3Country(), country.getIso());
                        country.setName(locale.getDisplayCountry().trim());
                        values[i] = country.getContentValues();
                    }
                    getContentResolver().bulkInsert(DatabaseContract.CountryEntry.CONTENT_URI, values);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	@SuppressLint("InlinedApi")
	private static final String[] PROJECTION_CHECKSUM =
	{
		ContactsContract.Contacts._ID,
	};

	/**
	 * Generate the checksum this way: get all unique phone numbers in E164 format,
	 * hash each one of them with SHA1, sort the results alphabetically and finally concatenate
	 * them in a string coma separated. The SHA1 hash of that string is the address book hash.
	 * @return
	 */
	private String calculateContactsChecksum() throws Exception
	{
		String resultHash = null;
		Cursor cursor = null;
		ArrayList<String> numberHashes = new ArrayList<>();
		MessageDigest crypt = MessageDigest.getInstance("SHA-1");
		try
		{
			cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI
				, PROJECTION_CHECKSUM
				, ContactsContract.Contacts.HAS_PHONE_NUMBER + "='1'"
				, null
				, null);
			if (cursor.moveToFirst())
			{
				do
				{
					Cursor phoneCursor = null;
					try
					{
						phoneCursor = getContentResolver().query(
							ContactsContract.CommonDataKinds.Phone.CONTENT_URI
							, new String [] { ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER  }
							, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?"
							, new String[] { cursor.getString(0) }
							, null);
						if (phoneCursor.moveToFirst())
						{
							do
							{
								String number = phoneCursor.getString(0);
								if (!TextUtils.isEmpty(number))
								{
									crypt.reset();
									crypt.update(number.getBytes("UTF8"));
									//numbers.add(Base64.encodeToString(crypt.digest(), Base64.NO_WRAP));
									numberHashes.add(Commons.bytesToHex(crypt.digest()));
								}
							}
							while (phoneCursor.moveToNext());
						}
					}
					catch (Exception ex)
					{
						LOGD(TAG, "calculateContactsChecksum:numbers", ex);
					}
					finally
					{
						if (phoneCursor != null)
							phoneCursor.close();
					}
				}
				while (cursor.moveToNext());
			}
		}
		catch (Exception ex)
		{
			LOGD(TAG, "calculateContactsChecksum", ex);
			throw ex;
		}
		finally
		{
			if (cursor != null)
				cursor.close();
		}
		if (!numberHashes.isEmpty())
		{
			//if (BuildConfig.DEBUG)
			{
				numberHashes = new ArrayList<String>(numberHashes.subList(0, 5));
				/*ArrayList<String> temp = new ArrayList<String>(numberHashes);
				for (int i = 0; i < 199; i++)
					numberHashes.addAll(temp);*/
			}
			Collections.sort(numberHashes);
			crypt.reset();
			boolean isFirst = true;
			final byte [] commaBytes = ",".getBytes("UTF8");
			for (String hash: numberHashes)
			{
				if (!isFirst) crypt.update(commaBytes); isFirst = false;
				crypt.update(hash.getBytes("UTF8"));
			}
			resultHash = Commons.bytesToHex(crypt.digest());
		}

		return resultHash;
	}

	/**
	 * Calculate contacts checksum and send it to server. Initiate contacts uploading in case
	 * checksum does not match
	 * @return true if server request initiated and wake lock should be kept and will be released on server response
	 */
    private boolean checkContacts()
    {
		boolean result = false;
		try
		{
			String contactsHash;
			final PhoneId phoneId = PhoneId.getInstance();
			TokenResponse tokenResponse;
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			boolean syncContacts = sharedPrefs.getBoolean(Commons.PREF_SYNC_CONTACTS, true);

			if (phoneId != null &&
				syncContacts &&
				(tokenResponse = phoneId.getAccessToken(null, null)) != null &&
				!TokenResponse.TOKEN_TYPE_ACCESS_MANAGER.equals(tokenResponse.getTokenType()) &&
				!TextUtils.isEmpty(contactsHash = calculateContactsChecksum()))
			{
				PhoneIdRestClient.get().getContactsRefreshNeeded(
					tokenResponse.getAccessToken()
					, contactsHash
					, new RestCallback<ContactsRefreshNeededResponse>()
				{
					@Override
					public void success(ContactsRefreshNeededResponse contactsRefreshNeededResponse,
						retrofit.client.Response response)
					{
						if (contactsRefreshNeededResponse.isRefreshNeeded())
						{
							final Intent intent = new Intent(phoneId.getContext(), FirstRunService.class);
							intent.setAction(FirstRunService.ACTION_UPLOAD_CONTACTS);
							phoneId.getContext().startService(intent);
						}
						if (wl.isHeld()) wl.release();
					}

					@Override
					public void failure(final ErrorResponse errorResponse, RetrofitError error)
					{
						PhoneId phoneId = PhoneId.getInstance();
						if (phoneId != null)
						{ phoneId.handleServerError(errorResponse, error); }
						if (wl.isHeld()) wl.release();
					}
				});
			}
		}
		catch (Exception ex)
		{
			LOGD(TAG, "checkContacts", ex);
		}
		return result;
    }

	@SuppressLint("InlinedApi")
	private static final String[] PROJECTION_CONTACTS =
		{
			ContactsContract.Contacts._ID
			, ContactsContract.Contacts.LOOKUP_KEY
			, Build.VERSION.SDK_INT
				>= Build.VERSION_CODES.HONEYCOMB ?
				ContactsContract.Contacts.DISPLAY_NAME_PRIMARY :
				ContactsContract.Contacts.DISPLAY_NAME
		};
	public static final int CONTACT_ID = 0;
	public static final int CONTACT_LOOKUP_KEY = 1;
	public static final int CONTACT_DISPLAY_NAME = 2;

	/**
	 * Scan contacts database and create list of all phone numbers with user names
	 * @return array of phone number contacts
	 */
	private ContactInfo[] createContactsList()
	{
		ArrayList<ContactInfo> list = new ArrayList<>();
		Cursor cursor = null;
		try
		{
			cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI
				, PROJECTION_CONTACTS
				, ContactsContract.Contacts.HAS_PHONE_NUMBER + "='1'"
				, null
				, null);
			if (cursor.moveToFirst())
			{
				do
				{
					Cursor phoneCursor = null;
					Cursor companyCursor = null;
					try
					{
						companyCursor = getContentResolver().query(
							ContactsContract.Data.CONTENT_URI
							, new String [] {
								ContactsContract.CommonDataKinds.Organization.COMPANY
							}
							, ContactsContract.Data.CONTACT_ID + "=? and " +
								ContactsContract.Data.MIMETYPE + " = ?"
							, new String[] { cursor.getString(CONTACT_ID)
								, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE }
							, null);
						String companyName = null;
						if (companyCursor.moveToFirst())
							companyName = companyCursor.getString(0);

						phoneCursor = getContentResolver().query(
							ContactsContract.CommonDataKinds.Phone.CONTENT_URI
							, new String [] {
								ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
								, ContactsContract.CommonDataKinds.Phone.TYPE
								, ContactsContract.CommonDataKinds.Phone.LABEL
							}
							, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?"
							, new String[] { cursor.getString(CONTACT_ID) }
							, null);
						if (phoneCursor.moveToFirst())
						{
							do
							{
								String number = phoneCursor.getString(0);
								if (!TextUtils.isEmpty(number))
								{
									int type = phoneCursor.getInt(1);
									String customLabel = phoneCursor.getString(2);
									String kind = ContactsContract.CommonDataKinds.Phone
										.getTypeLabel(getResources(), type, customLabel).toString();
									ContactInfo ci = new ContactInfo(cursor.getString(
										CONTACT_DISPLAY_NAME)
										, number
										, kind);
									if (companyName != null)
										ci.setCompany(companyName);
									list.add(ci);
								}
							}
							while (phoneCursor.moveToNext());
						}
					}
					catch (Exception ex)
					{
						LOGD(TAG, "createContactsList:numbers", ex);
					}
					finally
					{
						if (companyCursor != null)
							companyCursor.close();
						if (phoneCursor != null)
							phoneCursor.close();
					}
				}
				while (cursor.moveToNext());
			}
		}
		catch (Exception ex)
		{
			LOGD(TAG, "createContactsList", ex);
			throw ex;
		}
		finally
		{
			if (cursor != null)
				cursor.close();
		}
		//if (BuildConfig.DEBUG)
		{
			list = new ArrayList<>(list.subList(0, 5));
			/*ArrayList<ContactInfo> temp = new ArrayList<ContactInfo>(list);
			for (int i = 0; i < 199; i++)
				list.addAll(temp);*/
		}
		return list.toArray(new ContactInfo[list.size()]);
	}

	private static class ContactInfoArray
	{
		ContactInfo[] contacts;
		ContactInfoArray(ContactInfo[] contacts)
		{
			this.contacts = contacts;
		}
	}

	/**
	 * Upload array of contacts to the server
	 * @return true if upload started and no need to release wake lock
	 */
	private boolean uploadContacts()
	{
		boolean result = false;
		try
		{
			ContactInfoArray contacts = new ContactInfoArray(createContactsList());
			String contactsJSON = gson.toJson(contacts);
			final PhoneId phoneId = PhoneId.getInstance();
			TokenResponse tokenResponse;
			if (phoneId != null &&
				(tokenResponse = phoneId.getAccessToken(null, null)) != null &&
				contacts != null)
			{
				PhoneIdRestClient.get().uploadContacts(
					tokenResponse.getAccessToken()
					, contactsJSON
					, new RestCallback<UploadContactsResponse>()
					{
						@Override
						public void success(
							UploadContactsResponse uploadContactsResponse,
							retrofit.client.Response response)
						{
							if (uploadContactsResponse.getReceived() > 0)
							{
								SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(FirstRunService.this);
								if (sharedPrefs != null)
								{
									sharedPrefs.edit().putString(Commons.PREF_CONTACTS_LAST_SYNC,
										DateUtils.formatDateTime(FirstRunService.this, System.currentTimeMillis()
											, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NUMERIC_DATE))
										.apply();
								}
							}
							PhoneId phoneId = PhoneId.getInstance();
							if (phoneId != null)
							{ phoneId.onContactsUploaded(uploadContactsResponse.toString()); }

							if (wl.isHeld()) wl.release();
						}

						@Override
						public void failure(final ErrorResponse errorResponse, RetrofitError error)
						{
							PhoneId phoneId = PhoneId.getInstance();
							if (phoneId != null)
							{ phoneId.handleServerError(errorResponse, error); }
							if (wl.isHeld()) wl.release();
						}
					});
			}
		}
		catch (Exception ex)
		{
			LOGD(TAG, "checkContacts", ex);
		}
		return result;
	}
}
