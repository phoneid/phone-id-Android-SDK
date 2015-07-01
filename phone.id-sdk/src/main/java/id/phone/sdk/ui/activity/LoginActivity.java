package id.phone.sdk.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;

import com.flurry.android.FlurryAgent;

import id.phone.sdk.Commons;
import id.phone.sdk.PhoneId;
import id.phone.sdk.R;
import id.phone.sdk.auth.AccountAuthenticatorFragmentActivity;
import id.phone.sdk.auth.IAuthActivity;
import id.phone.sdk.rest.response.TokenResponse;
import id.phone.sdk.rest.response.UserResponse;
import id.phone.sdk.ui.fragment.LoginFragment;
import id.phone.sdk.utils.LogUtils;

import static id.phone.sdk.utils.LogUtils.LOGD;

/**
 * Created by azubchenko on 4/1/15.
 * Updated by Dennis Gubsky
 */
public class LoginActivity extends AccountAuthenticatorFragmentActivity implements IAuthActivity
{
	public static final String TAG = LogUtils.makeLogTag(LoginActivity.class);

    private Handler mHandler = new Handler();

    public static final String ARG_ACCOUNT_TYPE = "ARG_ACCOUNT_TYPE";
    public static final String ARG_AUTH_TYPE = "ARG_AUTH_TYPE";
    public static final String ARG_IS_ADDING_NEW_ACCOUNT = "ARG_IS_ADDING_NEW_ACCOUNT";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phid_activity_login);

        //
        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().add(R.id.fragment_container, new LoginFragment()).commit();
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        FlurryAgent.onStartSession(this, Commons.FLURRY_ID);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

	@Override
	public Handler getHandler()
	{
		return mHandler;
	}

	@Override
	public void setAuthSucceeded(Intent intent)
	{
		try
		{
			PhoneId phoneId = PhoneId.getInstance();
			if (getIntent().hasExtra(ARG_ACCOUNT_TYPE) && phoneId != null)
			{
				AccountManager mAccountManager = AccountManager.get(getBaseContext());
				TokenResponse tokenResponse = phoneId.getAccessToken(null, null);
				UserResponse userResponse = phoneId.getUser();

				if (tokenResponse != null && userResponse != null)
				{
					String mAccountName = userResponse.getPhoneNumber();
					String mAccountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);
					String mAuthType = getIntent().getStringExtra(ARG_AUTH_TYPE);
					String accountPassword = userResponse.getPhoneNumber();

					final Account account = new Account(mAccountName, mAccountType);
					String authToken = tokenResponse.getAccessToken();

					if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false))
					{
						// Creating the account
						// Password is optional to this call, safer not to send it really.
						mAccountManager.addAccountExplicitly(account, accountPassword, null);
					}
					else
					{
						// Password change only
						mAccountManager.setPassword(account, accountPassword);
					}
					// set the auth token we got (Not setting the auth token will cause
					// another call to the server to authenticate the user)
					mAccountManager.setAuthToken(account, mAuthType, authToken);

					intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
					intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);
					intent.putExtra(AccountManager.KEY_AUTHTOKEN, tokenResponse.getAccessToken());

					// Our base class can do what Android requires with the
					// KEY_ACCOUNT_AUTHENTICATOR_RESPONSE extra that onCreate has
					// already grabbed
					setAccountAuthenticatorResult(intent.getExtras());
				}
			}
		}
		catch (Exception ex)
		{
			LOGD(TAG, "setAuthSucceeded", ex);
		}
		finally
		{
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
	}

	@Override
	public void setAuthFailed(String errorMessage)
	{
        setResult(Activity.RESULT_CANCELED, null);
        finish();
	}
}
