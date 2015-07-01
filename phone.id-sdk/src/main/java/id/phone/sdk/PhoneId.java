package id.phone.sdk;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.flurry.android.FlurryAgent;
import com.google.gson.Gson;

import java.io.IOException;

import id.phone.sdk.rest.PhoneIdRestClient;
import id.phone.sdk.rest.RestCallback;
import id.phone.sdk.rest.response.ErrorResponse;
import id.phone.sdk.rest.response.TokenResponse;
import id.phone.sdk.rest.response.UserResponse;
import id.phone.sdk.service.FirstRunService;
import id.phone.sdk.telephony.SIMCardManager;
import id.phone.sdk.ui.activity.LoginActivity;
import id.phone.sdk.ui.view.LoginButton;
import id.phone.sdk.utils.IdentityHandler;
import id.phone.sdk.utils.LogUtils;
import retrofit.RetrofitError;

import static id.phone.sdk.utils.LogUtils.LOGD;


/**
 * PhoneId single-tone class should be used as Phone.Id SDK access point. Initialize this class in
 * your {@link Application#onCreate()  Application.onCreate} with corresponding ClientId value and
 * then use {@link #getInstance() getInstance} to get access to SDK.
 * Use {@link android.support.v4.content.LocalBroadcastManager LocalBroadcastManager} and your own
 * {@link android.content.BroadcastReceiver#onReceive(Context, Intent) BroadcastReceiver.onReceive}
 * to listen for SDK events.
 * @author azubchenko on 3/31/15.
 * @author Dennis Gubsky
 *
 */
public class PhoneId
{
	static private final String TAG = LogUtils.makeLogTag(PhoneId.class);

	public static final String ACCOUNT_TYPE = "phone.id";
	public static final String TOKEN_TYPE_DEFAULT = "default";

	private static PhoneId sInstance;

	private static Context mContext;
	private static String mClientId;
	private static String DeviceId = null;
	private final Gson gson = new Gson();
	private SIMCardManager simCardManager;

	/**
	 * ACTION_LOGGED_IN sent by LocalBroadcastManager when logged in to the server
	 */
	public static final String ACTION_LOGGED_IN = PhoneId.class.getName() + ".ACTION_LOGGED_IN";
	public static final String ARG_TOKEN_TYPE = "ARG_TOKEN_TYPE";	// Token type (String)
	public static final String ARG_ACCESS_TOKEN = "ARG_ACCESS_TOKEN";	// Access token (String)
	public static final String ARG_REFRESH_TOKEN = "ARG_REFRESH_TOKEN";	// Refresh Token (String)

	/**
	 * ACTION_LOGGED_OUT sent by LocalBroadcastManager when logged out from server
	 */
	public static final String ACTION_LOGGED_OUT = PhoneId.class.getName() + ".ACTION_LOGGED_OUT";
	/**
	 * ACTION_USER_PROFILE sent by LocalBroadcastManager when user profile received from server
	 */
	public static final String ACTION_USER_PROFILE = PhoneId.class.getName() + ".ACTION_USER_PROFILE";
	public static final String ARG_USER_PROFILE = "ARG_USER_PROFILE";	// User Profile JSON (String)

	/**
	 * ACTION_ERROR sent by LocalBroadcastManager when any error occurred during login/refresh
	 */
	public static final String ACTION_ERROR = PhoneId.class.getName() + ".ACTION_ERROR";
	public static final String ARG_ERROR_KIND = "ARG_ERROR_KIND";	// error kind: NETWORK, CONVERSION, HTTP, UNEXPECTED (String)
	public static final String ARG_ERROR_CODE = "ARG_ERROR_CODE";	// error code (String)
	public static final String ARG_ERROR_MESSAGE = "ARG_ERROR_MESSAGE";	// error message (String)

	/**
	 * Called when user contacts database uploaded to server
	 */
	public static final String ACTION_CONTACTS_UPLOADED = PhoneId.class.getName() + ".ACTION_CONTACTS_UPLOADED";
	public static final String ARG_RESPONSE = "ARG_RESPONSE";	// response received from server

	static public String getClientId()
	{
		return mClientId;
	}

	public Context getContext()
	{
		return mContext;
	}

	/**
	 * Get application name related to current {@link #getClientId() clientId}
	 * @return application name retrieved from server or null if it is not retrieved yet
	 */
	public String getApplicationName()
	{
		final SharedPreferences prefs = mContext.getApplicationContext()
			.getSharedPreferences(PhoneId.getClientId()
				, Context.MODE_PRIVATE);
		return prefs.getString(Commons.PREF_CLIENT_APP_NAME, getClientId());
	}

	/**
	 * Get device ID
	 * @return current device ID
	 */
	public static String getDeviceId()
	{
		if (DeviceId == null)
		{
			DeviceId = Settings.Secure
				.getString(mContext.getApplicationContext().getContentResolver(),
					Settings.Secure.ANDROID_ID);
		}
		return DeviceId;
	}

	/**
	 * Get {@link id.phone.sdk.PhoneId PhoneId} instance. Instance must be initialized in
	 * your {@link Application#onCreate()  Application.onCreate}
	 * @return {@link id.phone.sdk.PhoneId PhoneId} instance
	 */
	static public PhoneId getInstance()
	{
		return sInstance;
	}

	/**
	 * This method must be called in your {@link Application#onCreate()  Application.onCreate}
	 * to initialize {@link id.phone.sdk.PhoneId PhoneId}
	 * @param context Application {@link android.content.Context Context}
	 * @param clientId Your application PhoneId registered ClinetId
	 * @return {@link id.phone.sdk.PhoneId PhoneId} instance
	 */
	static public PhoneId getInstance(Context context, String clientId)
	{
		if (sInstance == null)
		{
			sInstance = new PhoneId(context, clientId);
		}

		return sInstance;
	}

	private PhoneId(Context context, String clientId)
	{
		mContext = context;
		mClientId = clientId;
		simCardManager = SIMCardManager.getInstance(context);

		//
		init();
	}

	private void init()
	{

		DeviceId = getDeviceId();

		//generating salt for encryption of the important data
		IdentityHandler.initSalt(mContext);

		//init Flurry
		FlurryAgent.setCaptureUncaughtExceptions(true);
		FlurryAgent.setLogEnabled(BuildConfig.DEBUG);
		FlurryAgent.setLogEvents(BuildConfig.DEBUG);
		FlurryAgent.setLogLevel(Log.DEBUG);
		FlurryAgent.setVersionName(BuildConfig.VERSION_NAME);
		FlurryAgent.init(mContext.getApplicationContext(), Commons.FLURRY_ID);

		//starting service that makes sure we have all needed data in db
		final Intent intent = new Intent(mContext, FirstRunService.class);
		intent.setAction(FirstRunService.ACTION_IMPORT_COUNTRIES);
		mContext.startService(intent);

		mContext.getContentResolver()
			.registerContentObserver(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, true,
				phonesDatabaseContentObserver);
	}

	public void onDestroy()
	{
		try
		{
			if (mContext != null)
			{
				mContext.getContentResolver().unregisterContentObserver(phonesDatabaseContentObserver);
			}
		}
		catch (Exception ex) {}
		finally
		{
			mContext = null;
		}
	}

	@Override
	protected void finalize() throws Throwable
	{
		onDestroy();
		super.finalize();
	}

	//

	/**
	 * Get stored logged in user information in {@link id.phone.sdk.rest.response.UserResponse UserResponse}.
	 * @return logged-in user information
	 */
	public UserResponse getUser()
	{
		UserResponse userResponse = null;
		String user = IdentityHandler.getUser(mContext);
		if (!TextUtils.isEmpty(user))
		{
			userResponse = gson.fromJson(user, UserResponse.class);
		}
		return userResponse;
	}

	public void saveToken(TokenResponse tokenResponse, @Nullable String phoneNumber)
	{
		tokenResponse.setCreate_time(System.currentTimeMillis());
		String tokenJson = gson.toJson(tokenResponse);
		IdentityHandler.saveToken(mContext, tokenJson);

		mAccountManager = AccountManager.get(mContext);
		mAccounts = mAccountManager.getAccountsByType(PhoneId.ACCOUNT_TYPE);

		if (!TextUtils.isEmpty(phoneNumber))
		{
			mAccount = null;
			if (mAccounts != null && mAccounts.length > 0)
			{
				for (Account acc : mAccounts)
				{
					if (phoneNumber.equals(acc.name))
					{
						mAccount = acc;
						break;
					}
				}
			}

			// If account is not found, then we have to add it
			if (mAccount == null)
			{
				Account account = new Account(phoneNumber, PhoneId.ACCOUNT_TYPE);
				String authToken = tokenResponse.getAccessToken();

				mAccountManager.addAccountExplicitly(account, phoneNumber, null);
				// set the auth token we got (Not setting the auth token will cause
				// another call to the server to authenticate the user)
				mAccountManager.setAuthToken(account, tokenResponse.getTokenType(), authToken); // TODO: CHECK TOKEN TYPE
			}
		}

		Intent intent = new Intent(ACTION_LOGGED_IN);
		intent.putExtra(ARG_TOKEN_TYPE, tokenResponse.getTokenType());
		intent.putExtra(ARG_ACCESS_TOKEN, tokenResponse.getAccessToken());
		intent.putExtra(ARG_REFRESH_TOKEN, tokenResponse.getRefreshToken());
		LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
	}

	public void saveUser(UserResponse userResponse)
	{
		//setting userId
		FlurryAgent.setUserId(userResponse.getId());
		String userJson = gson.toJson(userResponse);

		//saving user info into private storage
		IdentityHandler.saveUser(mContext, userJson);

		Intent intent = new Intent(ACTION_USER_PROFILE);
		intent.putExtra(ARG_USER_PROFILE, userJson);
		LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);

		// Force service to check contacts checksum on server
		final Intent serviceIntent = new Intent(getContext(), FirstRunService.class);
		serviceIntent.setAction(FirstRunService.ACTION_CHECK_CONTACTS);
		getContext().startService(serviceIntent);
	}

	/**
	 * Callback to receive login token response asynchronously
	 */
	public interface TokenResponseCallback
	{
		/**
		 * Deliver {@link id.phone.sdk.rest.response.TokenResponse TokenResponse} to calling thread
		 * @param tokenResponse {@link id.phone.sdk.rest.response.TokenResponse TokenResponse} received from server
		 */
		void tokenResponseDelivered(TokenResponse tokenResponse);
	};

	private AccountManager mAccountManager;
	private Account[] mAccounts;
	private Account mAccount;
	private String mAccessToken;
	private TokenResponseCallback tokenResponseCallback;

	private void startAuthTokenFetch(@NonNull Account account, @Nullable TokenResponseCallback tokenResponseCallback)
	{
		try
		{
			this.tokenResponseCallback = tokenResponseCallback;
			Bundle options = new Bundle();
			mAccountManager.getAuthToken(
				account,
				PhoneId.ARG_TOKEN_TYPE,
				options,
				false,
				new OnAccountManagerComplete(),
				new android.os.Handler(errorCallback)
			);
		}
		catch (Exception ex)
		{
			LOGD(TAG, "startAuthTokenFetch", ex);
		}
	}

	private android.os.Handler.Callback errorCallback = new android.os.Handler.Callback()
	{
		@Override
		public boolean handleMessage(Message msg)
		{
			return false;
		}
	};

	private class OnAccountManagerComplete implements AccountManagerCallback<Bundle>
	{
		@Override
		public void run(AccountManagerFuture<Bundle> result)
		{
			Bundle bundle;
			try
			{
				bundle = result.getResult();
			}
			catch (OperationCanceledException e)
			{
				LOGD(TAG, "OnAccountManagerComplete", e);
				return;
			}
			catch (AuthenticatorException e)
			{
				LOGD(TAG, "OnAccountManagerComplete", e);
				return;
			}
			catch (IOException e)
			{
				LOGD(TAG, "OnAccountManagerComplete", e);
				return;
			}
			mAccessToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
			LOGD(TAG, "Received authentication token " + mAccessToken);

			TokenResponse tokenResponse;
			saveToken(
				tokenResponse = new TokenResponse(TokenResponse.TOKEN_TYPE_ACCESS_MANAGER, mAccessToken, null), null);
			saveUser(new UserResponse(null, mAccount.type, mAccount.name));

			if (PhoneId.this.tokenResponseCallback != null)
			{
				try
				{
					PhoneId.this.tokenResponseCallback.tokenResponseDelivered(tokenResponse);
				}
				catch (Exception ex) {}
			}
		}
	}

	private class OnAccountAddComplete implements AccountManagerCallback<Bundle>
	{
		@Override
		public void run(AccountManagerFuture<Bundle> result) {
			Bundle bundle;
			try
			{
				bundle = result.getResult();
				Log.d(TAG, "OnAccountAddComplete result=" + bundle.toString());
			}
			catch (OperationCanceledException e)
			{
				Log.d(TAG, "OnAccountAddComplete", e);
				return;
			}
			catch (AuthenticatorException e)
			{
				Log.d(TAG, "OnAccountAddComplete", e);
				return;
			}
			catch (IOException e)
			{
				Log.d(TAG, "OnAccountAddComplete", e);
				return;
			}
			mAccount = new Account(
				bundle.getString(AccountManager.KEY_ACCOUNT_NAME)
				, bundle.getString(AccountManager.KEY_ACCOUNT_TYPE)
			);
			Log.d(TAG, "Added account " + mAccount.name + ", fetching");
			startAuthTokenFetch(mAccount, PhoneId.this.tokenResponseCallback);
		}
	}

	/**
	 * Get access token as {@link id.phone.sdk.rest.response.TokenResponse TokenResponse} object
	 */
	public TokenResponse getAccessToken()
	{
		return this.getAccessToken(null, null);
	}

	/**
	 * Get access token as {@link id.phone.sdk.rest.response.TokenResponse TokenResponse} object
	 *
	 * @param tokenResponseCallback {@link PhoneId.TokenResponseCallback TokenResponseCallback} async callback to deliver access token to the caller thread
	 * @return {@link id.phone.sdk.rest.response.TokenResponse TokenResponse} object or null if access token is not available at the moment. If tokenResponseCallback != null token will be delivered to it
	 */
	public TokenResponse getAccessToken(@Nullable Activity activity, @Nullable TokenResponseCallback tokenResponseCallback)
	{
		TokenResponse tokenResponse = null;
		if (simCardManager.isLogoutRequired())
		{
			logOut();
			return null;
		}
		try
		{
			String token = IdentityHandler.getToken(mContext);
			if (!TextUtils.isEmpty(token))
			{
				tokenResponse = gson.fromJson(token, TokenResponse.class);

				if (TokenResponse.TOKEN_TYPE_ACCESS_MANAGER.equals(tokenResponse.getTokenType())
					&& tokenResponseCallback != null)
				{
					UserResponse userResponse = getUser();
					mAccountManager = AccountManager.get(mContext);
					mAccounts = mAccountManager.getAccountsByType(PhoneId.ACCOUNT_TYPE);

					mAccount = null;
					if (mAccounts != null && mAccounts.length > 0)
					{
						if (userResponse != null)
							for (Account acc : mAccounts)
							{
								if (userResponse.getPhoneNumber().equals(acc.name))
								{
									mAccount = acc;
									break;
								}
							}
						if (mAccount == null)
							mAccount = mAccounts[0];
					}

					if (mAccount != null)
					{
						startAuthTokenFetch(mAccount, tokenResponseCallback);
						return null;	// Signal that result will be delivered through tokenResponseCallback
					}
				}
				else
				{
					if (tokenResponse.isExpired() || tokenResponse.isExpireSoon())
					{
						refreshAccessToken(tokenResponse, tokenResponseCallback);
						if (tokenResponseCallback != null)    // prepare app to receive token by callback
							tokenResponse = null;
					}
					else if (tokenResponseCallback != null)
					{
						try
						{
							tokenResponseCallback.tokenResponseDelivered(tokenResponse);
						}
						catch (Exception ex) {}
					}
				}
			}
			else if (tokenResponseCallback != null)
			{
				if (activity != null)
				{
					Intent intent = new Intent(activity, LoginActivity.class);
					activity.startActivityForResult(intent, LoginButton.LOGIN_REQUEST_CODE);
				}
				else
				{
					this.tokenResponseCallback = tokenResponseCallback;
					mAccountManager.addAccount(
						PhoneId.ACCOUNT_TYPE
						, PhoneId.TOKEN_TYPE_DEFAULT
						, null
						, new Bundle()
						, activity
						, new OnAccountAddComplete()
						, new android.os.Handler(errorCallback));
				}

				return null;	// Signal that result will be delivered through tokenResponseCallback
			}
		}
		catch (Exception ex)
		{
			tokenResponse = null;
			LOGD(TAG, "getAccessToken", ex);
		}
		return tokenResponse;
	}

	/**
	 * Check if user is currently logged-in
	 * @return true if user is currently logged-in
	 */
	public boolean isLoggedIn()
	{
		TokenResponse token = getAccessToken(null, null);
		return token != null;
	}

	public void handleServerError(ErrorResponse errorResponse, RetrofitError error)
	{
		if (TextUtils.isEmpty(errorResponse.getMessage()) ||
			errorResponse.getMessage().contains("No address associated with hostname"))
			errorResponse.setMessage(mContext.getString(R.string.phid_error_no_connection));

		Intent intent = new Intent(ACTION_ERROR);
		intent.putExtra(ARG_ERROR_KIND, error == null ? "NETWORK" : error.getKind().toString());
		intent.putExtra(ARG_ERROR_CODE, errorResponse.getCode());
		intent.putExtra(ARG_ERROR_MESSAGE, errorResponse.getMessage());
		LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
	}

	private void refreshAccessToken(TokenResponse token, @Nullable final TokenResponseCallback tokenResponseCallback)
	{
		PhoneIdRestClient.get().getRefreshToken(PhoneId.getClientId(), "refresh_token"
			, token.getRefreshToken(), new RestCallback<TokenResponse>()
		{
			@Override
			public void success(TokenResponse tokenResponse, retrofit.client.Response response)
			{
				PhoneId phoneId = PhoneId.getInstance();
				if (tokenResponseCallback != null)
				{
					try
					{
						tokenResponseCallback.tokenResponseDelivered(tokenResponse);
					}
					catch (Exception ex) {}
				}
				if (phoneId != null)
				{
					phoneId.saveToken(tokenResponse, null);
				}

				// Force service to check contacts checksum on server
				final Intent serviceIntent = new Intent(getContext(), FirstRunService.class);
				serviceIntent.setAction(FirstRunService.ACTION_CHECK_CONTACTS);
				getContext().startService(serviceIntent);
			}

			@Override
			public void failure(final ErrorResponse errorResponse, RetrofitError error)
			{
				PhoneId phoneId = PhoneId.getInstance();
				if (phoneId != null)
				{ phoneId.handleServerError(errorResponse, error); }
			}
		});
	}

	public void removeAccountInAccountManager(String accountName)
	{
		AccountManager mAccountManager = AccountManager.get(mContext);
		Account[] mAccounts = mAccountManager.getAccountsByType(PhoneId.ACCOUNT_TYPE);
		if (mAccounts != null && mAccounts.length > 0)
		{
			for (Account account: mAccounts)
			{
				if (TextUtils.isEmpty(accountName) || accountName.equals(account.name))
				{
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
						mAccountManager.removeAccountExplicitly(account);
					else
						mAccountManager.removeAccount(account, null, null);
				}
			}
		}
	}

	/**
	 * Logout from server
	 */
	public void logOut()
	{
		String phoneNumber = null;
		UserResponse user = getUser();
		if (user != null)
			phoneNumber = user.getPhoneNumber();
		removeAccountInAccountManager(phoneNumber);

		IdentityHandler.clearUser(mContext);
		IdentityHandler.clearToken(mContext);

		//resetting userId
		FlurryAgent.setUserId("");

		Intent intent = new Intent(ACTION_LOGGED_OUT);
		LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
	}

	public SIMCardManager getSimCardManager()
	{
		return simCardManager;
	}

	public void onContactsUploaded(String response)
	{
		Intent intent = new Intent(ACTION_CONTACTS_UPLOADED);
		intent.putExtra(ARG_RESPONSE, response);
		LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
	}

	/**
	 * Start contacts uploading to the server.
	 */
	public void uploadContactsToServer()
	{
		// Force service to upload contacts to server
		final Intent serviceIntent = new Intent(getContext(), FirstRunService.class);
		serviceIntent.setAction(FirstRunService.ACTION_UPLOAD_CONTACTS);
		getContext().startService(serviceIntent);
	}

	private class PhonesDatabaseContentObserver extends ContentObserver
	{
		public PhonesDatabaseContentObserver()
		{
			super(null);
		}

		@Override
		public void onChange(boolean selfChange)
		{
			super.onChange(selfChange);
			// Force service to check contacts checksum on server
			final Intent serviceIntent = new Intent(getContext(), FirstRunService.class);
			serviceIntent.setAction(FirstRunService.ACTION_CHECK_CONTACTS);
			getContext().startService(serviceIntent);
		}
	}
	private PhonesDatabaseContentObserver phonesDatabaseContentObserver = new PhonesDatabaseContentObserver();

	/**
	 * Lookup user name by phone number. Only local contacts database queried. If allowMultipleEntries is true
	 * and more than one contact related to given phone number, then user names are concatenated and separated
	 * by comma. If no any contact related to phone number, then the phone number returned.
	 * @param context Context
	 * @param phoneNumber phone number to lookup
	 * @param allowMultipleEntries if true possible multiple contacts are listed, otherwise only first contact
	 * @return contact display name or phoneNumber if no contact found
	 */
	public static String getUserDisplayName(@NonNull Context context, @NonNull String phoneNumber
		, boolean allowMultipleEntries)
	{
		String result = phoneNumber;
		Cursor cursor = null;
		try
		{
			Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
			cursor = context.getContentResolver().query(uri
				, new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME,
					ContactsContract.PhoneLookup._ID }
				, null, null, null);
			if (cursor.moveToFirst())
			{
				StringBuilder sb = new StringBuilder();
				do
				{
					if (sb.length() > 0) sb.append(", ");
					sb.append(cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)));
					if (!allowMultipleEntries) break;
 				} while (cursor.moveToNext());
				if (sb.length() > 0) result = sb.toString();
			}
		}
		catch (Exception ex)
		{
			Log.d(TAG, "getUserDisplayName", ex);
		}
		finally
		{
			if (cursor != null) cursor.close();
		}
		return result;
	}

}
