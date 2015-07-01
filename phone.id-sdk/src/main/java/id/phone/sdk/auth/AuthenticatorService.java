package id.phone.sdk.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import id.phone.sdk.Commons;
import id.phone.sdk.PhoneId;
import id.phone.sdk.rest.PhoneIdRestClient;
import id.phone.sdk.rest.RestCallback;
import id.phone.sdk.rest.response.ErrorResponse;
import id.phone.sdk.rest.response.TokenResponse;
import id.phone.sdk.service.FirstRunService;
import id.phone.sdk.ui.activity.LoginActivity;
import id.phone.sdk.utils.IdentityHandler;
import id.phone.sdk.utils.LogUtils;
import retrofit.RetrofitError;

import static id.phone.sdk.utils.LogUtils.LOGD;

/**
 * Created by Dennis Gubsky on 20.05.15.
 */
public class AuthenticatorService extends Service
{
	public static final String TAG = LogUtils.makeLogTag(AuthenticatorService.class);

	private final Gson gson = new Gson();

	// Instance field that stores the authenticator object
	private PhoneIdAccountAuthenticator mAuthenticator;
	@Override
	public void onCreate() {
		// Create a new authenticator object
		mAuthenticator = new PhoneIdAccountAuthenticator(this);
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		if (intent.getAction().equals(
			android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
			return mAuthenticator.getIBinder();
		return null;
	}

	/**
	 * Created by dennis on 19.05.15.
	 */
	private class PhoneIdAccountAuthenticator extends AbstractAccountAuthenticator
	{
		public final String TAG = PhoneIdAccountAuthenticator.class.getSimpleName();

		private Context mContext;

		public PhoneIdAccountAuthenticator(Context context)
		{
			super(context);
			mContext = context;
		}

		@Override
		public Bundle editProperties(AccountAuthenticatorResponse response, String accountType)
		{
			return null;
		}

		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
			String authTokenType, String[] requiredFeatures, Bundle options)
			throws NetworkErrorException
		{
			// We absolutely cannot add an account without some information
			// from the user; so we're definitely going to return an Intent
			// via KEY_INTENT
			final Bundle bundle = new Bundle();

			// We're going to use a LoginActivity to talk to the user (mContext
			// we'll have noted on construction).
			final Intent intent = new Intent(mContext, LoginActivity.class);

			// We can configure that activity however we wish via the
			// Intent.  We'll set ARG_IS_ADDING_NEW_ACCOUNT so the Activity
			// knows to ask for the account name as well
			intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, accountType);
			intent.putExtra(LoginActivity.ARG_AUTH_TYPE, authTokenType);
			intent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
			// It will also need to know how to send its response to the
			// account manager; LoginActivity must derive from
			// AccountAuthenticatorActivity, which will want this key set
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
				response);

			// Wrap up this intent, and return it, which will cause the
			// intent to be run
			bundle.putParcelable(AccountManager.KEY_INTENT, intent);
			return bundle;
		}

		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
			Bundle options) throws NetworkErrorException
		{
			return null;
		}

		@Override
		public Bundle getAuthToken(final AccountAuthenticatorResponse response, final Account account,
			String authTokenType, Bundle options) throws NetworkErrorException
		{
			try
			{
				PhoneId phoneId = PhoneId.getInstance();
				if (phoneId != null)
				{
					TokenResponse tokenResponse = phoneId.getAccessToken(null, null);
					if (tokenResponse != null && (tokenResponse.isExpired() || tokenResponse.isExpireSoon()))
					{
						AccountManager accountManager = AccountManager.get(AuthenticatorService.this);
						if (tokenResponse != null)
							accountManager.invalidateAuthToken(PhoneId.ACCOUNT_TYPE, tokenResponse.getAccessToken());

						PhoneIdRestClient.get()
							.getRefreshToken(PhoneId.getClientId(), "refresh_token"
								, tokenResponse.getRefreshToken(), new RestCallback<TokenResponse>()
							{
								@Override
								public void success(TokenResponse tokenResponse,
									retrofit.client.Response retrofitResponse)
								{
									PhoneId phoneId = PhoneId.getInstance();
									if (phoneId != null)
									{ phoneId.saveToken(tokenResponse, null); }
									final Bundle result = new Bundle();
									result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
									result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
									result.putString(AccountManager.KEY_AUTHTOKEN,
										tokenResponse.getAccessToken());

									// Force service to check contacts checksum on server
									final Intent serviceIntent = new Intent(phoneId.getContext(), FirstRunService.class);
									serviceIntent.setAction(FirstRunService.ACTION_CHECK_CONTACTS);
									phoneId.getContext().startService(serviceIntent);

									response.onResult(result);
								}

								@Override
								public void failure(final ErrorResponse errorResponse,
									RetrofitError error)
								{
									PhoneId phoneId = PhoneId.getInstance();
									if (phoneId != null)
									{ phoneId.handleServerError(errorResponse, error); }
									response.onError(404, errorResponse.getMessage());
								}
							});
					}
					else if (tokenResponse != null)
					{
						final Bundle result = new Bundle();
						result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
						result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
						result.putString(AccountManager.KEY_AUTHTOKEN,
							tokenResponse.getAccessToken());
						return result;
					}
					else
					{
						// If we get here, then we couldn't access the user's password - so we
						// need to re-prompt them for their credentials. We do that by creating
						// an intent to display our AuthenticatorActivity.
						final Intent intent = new Intent(mContext, LoginActivity.class);
						intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
						intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, account.type);
						intent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
						intent.putExtra(LoginActivity.ARG_AUTH_TYPE, authTokenType);
						final Bundle bundle = new Bundle();
						bundle.putParcelable(AccountManager.KEY_INTENT, intent);
						return bundle;
					}
				}
			}
			catch (Exception ex)
			{
				LOGD(TAG, "getAuthToken", ex);
				throw new NetworkErrorException(ex);
			}
			return null;
		}

		@Override
		public String getAuthTokenLabel(String authTokenType)
		{
			return null;
		}

		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
			String authTokenType, Bundle options) throws NetworkErrorException
		{
			return null;
		}

		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
			String[] features) throws NetworkErrorException
		{
			return null;
		}
	}

}
