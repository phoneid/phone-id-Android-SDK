package id.phone.sdk.ui.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import id.phone.sdk.Commons;
import id.phone.sdk.PhoneId;
import id.phone.sdk.R;
import id.phone.sdk.rest.PhoneIdRestClient;
import id.phone.sdk.rest.RestCallback;
import id.phone.sdk.rest.response.ClientResponse;
import id.phone.sdk.rest.response.DefaultResponse;
import id.phone.sdk.rest.response.ErrorResponse;
import id.phone.sdk.rest.response.TokenResponse;
import id.phone.sdk.rest.response.UserResponse;
import id.phone.sdk.ui.dialog.ErrorDialog;
import id.phone.sdk.utils.IdentityHandler;
import id.phone.sdk.utils.LogUtils;
import id.phone.sdk.utils.TelephonyUtils;
import retrofit.RetrofitError;

import static id.phone.sdk.utils.LogUtils.LOGD;

/**
 * This is the fragment for entering phone number, its validation and call for SMS with code
 * Created by azubchenko on 4/1/15.
 * Updated by Dennis Gubsky
 */
public class LoginFragment extends BaseFragment implements ConnectionCallbacks, OnConnectionFailedListener
{
    public static final int ACCOUNT_CHOOSER_ACTIVITY = 109;
	private static final String TAG = LogUtils.makeLogTag(LoginFragment.class);

	private Button mCountryButton, mOkButton;
    private EditText mPhone;

    private PhoneCountryProvider mPhoneCountryProvider;
    private PhoneNumberFormattingTextWatcher mPhoneNumberFormattingTextWatcher;

    private BroadcastReceiver mReceiver;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private CountryFetcherAsyncTask mCountryFetcherAsyncTask;

    private TextView mHeader;
    private TextView mTermsAndConditions;

	private AccountManager mAccountManager;
	private String mAccessToken;
	private Account[] mAccounts;
	private Account mAccount;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.phid_fragment_login, container, false);

        buildGoogleApiClient();

        //setting provider
        mPhoneCountryProvider = mPhoneCountryProvider == null ? new PhoneCountryProvider(getActivity().getApplicationContext()) : mPhoneCountryProvider;

        //restoring phone number and country previously entered
        if (savedInstanceState != null && savedInstanceState.getString(Commons.PARAM_PHONE) != null) {
            mPhoneCountryProvider.setFullPhoneNumber(savedInstanceState.getString(Commons.PARAM_PHONE));
        }

        //as phone number is not valid and it's first time we're here, we can try to call for location
        //and get country from it
        if (savedInstanceState == null && TextUtils.isEmpty(mPhoneCountryProvider.getCountryIso())) {
            mGoogleApiClient.connect();
        }

        //
        mCountryButton = (Button) view.findViewById(R.id.country);
        //for the country in provider let's try to find MCC code
        mCountryButton.setText(mPhoneCountryProvider.getCountryMcc());
        mCountryButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//TODO: move analytics events to separate helper class
				FlurryAgent.logEvent("COUNTRY LIST BUTTON CLICK");

				//stopping country fetching service, if it's running
				if (mCountryFetcherAsyncTask != null) mCountryFetcherAsyncTask.cancel(true);

				//hiding keyboard
				hideSoftwareKeyboard();

				//showing phid_countries list
				FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.beginTransaction()
					.add(((View) getView().getParent()).getId(), new CountryListFragment())
					.addToBackStack(null).commit();
			}
		});

        mOkButton = (Button) view.findViewById(R.id.ok);
        mOkButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				processNumber();
			}
		});
        //showing/hiding our button in case phone is valid/invalid
        mOkButton.setVisibility(
			mPhoneCountryProvider.isPhoneValid() ? View.VISIBLE : View.INVISIBLE);

        //phone field and we need textwatcher bind to the country selected
        mPhone = (EditText) view.findViewById(R.id.number);
        //showing phone available number
        mPhone.setText(mPhoneCountryProvider.getPhoneNumber());
        mPhone.addTextChangedListener(createPhoneNumberWatcher());
		mPhone.setOnEditorActionListener(new TextView.OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_ACTION_GO
					&& (event == null || event.getAction() == KeyEvent.ACTION_DOWN))
				{
					processNumber();
					return true;
				}
				return false;
			}
		});


        //listener for the picked country
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //updating our view with new country selected by user
                if (intent.getAction().equals(Commons.COUNTRY_PICKED_ACTION)) {
                    setCountryIso(intent.getStringExtra(Intent.EXTRA_TEXT));
                }
            }
        };

        mTermsAndConditions = (TextView) view.findViewById(R.id.termsAndConditions);
		mTermsAndConditions.setLinksClickable(true);
		mTermsAndConditions.setMovementMethod(LinkMovementMethod.getInstance());
        mTermsAndConditions.setText(Html.fromHtml(getString(R.string.phid_label_terms_and_policy)));

        mHeader = (TextView) view.findViewById(R.id.header);

        requestAppName();

		mAccountManager = AccountManager.get(getActivity());
		mAccounts = mAccountManager.getAccountsByType(PhoneId.ACCOUNT_TYPE);

		if (mAccounts != null && mAccounts.length > 0)
		{
			Intent accountsIntent = AccountManager.newChooseAccountIntent(
				null
				, null
				, new String[]{ PhoneId.ACCOUNT_TYPE }
				, false
				, null
				, PhoneId.TOKEN_TYPE_DEFAULT
				, null
				, null);
			startActivityForResult(accountsIntent, ACCOUNT_CHOOSER_ACTIVITY);
		}

        return view;
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if( resultCode == Activity.RESULT_CANCELED)
			return;
		if( requestCode == ACCOUNT_CHOOSER_ACTIVITY )
		{
			Bundle bundle = data.getExtras();
			mAccount = new Account(
				bundle.getString(AccountManager.KEY_ACCOUNT_NAME),
				bundle.getString(AccountManager.KEY_ACCOUNT_TYPE)
			);
			LOGD(TAG, "Selected account " + mAccount.name + ", fetching");
			startAuthTokenFetch(mAccount);
		}
	}

	private void startAuthTokenFetch(Account account)
	{
		try
		{
			Bundle options = new Bundle();
			mAccountManager.getAuthToken(
				account,
				PhoneId.ARG_TOKEN_TYPE,
				options,
				getActivity(),
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

			PhoneId phoneId = PhoneId.getInstance();
			if (phoneId != null)
			{
				phoneId.saveToken(new TokenResponse(TokenResponse.TOKEN_TYPE_ACCESS_MANAGER, mAccessToken, null), null);
				phoneId.saveUser(new UserResponse(null, mAccount.type, mAccount.name));
			}
			if (getActivity() != null)
			{
				getActivity().onBackPressed();
			}
		}
	}

    @Override
    public void onResume() {
        super.onResume();

        //registering our receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Commons.COUNTRY_PICKED_ACTION);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        localBroadcastManager.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        //cancelling task if running
        if (mCountryFetcherAsyncTask != null) {
            mCountryFetcherAsyncTask.cancel(true);
            mCountryFetcherAsyncTask = null;
        }

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        //unregistering receiver
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        localBroadcastManager.unregisterReceiver(mReceiver);
    }

    @Override
    protected void saveState(Bundle outState) {
        outState.putString(Commons.PARAM_PHONE,
			mPhoneCountryProvider.getCountryMcc() + " " + mPhoneCountryProvider.getPhoneNumber());
    }

    @Override
    protected void restoreState(Bundle savedInstanceState) {
    }

    @Override
    protected void populateArguments(Bundle savedInstanceState) {

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        //ok we have location, lets get country from it
        if (mLastLocation != null && Geocoder.isPresent() && TelephonyUtils.isNetworkConnected(getActivity().getApplicationContext())) {
            mCountryFetcherAsyncTask = new CountryFetcherAsyncTask();
            mCountryFetcherAsyncTask.execute(mLastLocation);
        } else {
            setCountryIso(Locale.getDefault().getCountry());
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        setCountryIso(Locale.getDefault().getCountry());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }



    //private methods
	private void processNumber()
	{
		if (mPhoneCountryProvider.isPhoneValid())
		{
			//hiding keyboard
			hideSoftwareKeyboard();

			PhoneIdRestClient.get().getCode(PhoneId.getClientId()
				, mPhoneCountryProvider.getFormattedPhoneNumber(PhoneNumberUtil.PhoneNumberFormat.E164)
				, new RestCallback<DefaultResponse>()
			{
				@Override
				public void success(DefaultResponse codeResponse, retrofit.client.Response response2) {
					FragmentManager fragmentManager = getFragmentManager();
					fragmentManager.beginTransaction().add(((View) getView().getParent()).getId()
						, CodeFragment.getInstance(mPhoneCountryProvider
							.getFormattedPhoneNumber(PhoneNumberUtil.PhoneNumberFormat.E164)))
							.addToBackStack(null).commit();
				}

				@Override
				public void failure(final ErrorResponse errorResponse, RetrofitError error)
				{
					PhoneId phoneId = PhoneId.getInstance();
					if (phoneId != null)
						phoneId.handleServerError(errorResponse, error);

					getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							hideSoftwareKeyboard();
							ErrorDialog.newInstance(0, errorResponse.getMessage()).show(
								getActivity().getSupportFragmentManager(), ErrorDialog.TAG);
						}
					});
				}

			});
		}

	}


    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity().getApplicationContext()).
                addConnectionCallbacks(this).addOnConnectionFailedListener(this).
                addApi(LocationServices.API).build();
    }

    /**
     * Creating text watcher for the phone input field
     *
     * @return
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private PhoneNumberFormattingTextWatcher createPhoneNumberWatcher() {
        //funny it says that it's available only in Lollipop, when in sources it's already in 15th SDK
        mPhoneNumberFormattingTextWatcher = new PhoneNumberFormattingTextWatcher(mPhoneCountryProvider.getCountryIso()) {
            private boolean mIsFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (mIsFormatting) return;
                super.beforeTextChanged(s, start, count, after);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mIsFormatting) return;
                super.onTextChanged(s, start, before, count);
            }

            @Override
            public synchronized void afterTextChanged(Editable s) {
                if (!mIsFormatting) {
                    mIsFormatting = true;

					mPhoneCountryProvider.setPhoneNumber(s.toString());
					//s.insert(0, mPhoneCountryProvider.getCountryMcc());
					super.afterTextChanged(s);
					/*if (s.length() >= mPhoneCountryProvider.getCountryMcc().length())
						s.replace(0, mPhoneCountryProvider.getCountryMcc().length(), "");*/
					//removing leading space
					/*if (s.length() > 0 && s.charAt(0) == ' ')
					{
						s.replace(0, 1, "");
					}*/

                    //showing OK button if number is valid
                    mOkButton.setVisibility(mPhoneCountryProvider.isPhoneValid() ? View.VISIBLE : View.INVISIBLE);
                    mIsFormatting = false;
                }
            }
        };
        return mPhoneNumberFormattingTextWatcher;
    }

    /**
     * Setting country iso from outside
     *
     * @param iso
     */
    private void setCountryIso(String iso) {
        //saving new iso in the provider
        //this involves phone validation and mcc calculation
        mPhoneCountryProvider.setCountryIso(iso);

        //updating phone number with new listener
        mPhone.removeTextChangedListener(mPhoneNumberFormattingTextWatcher);
        mPhone.addTextChangedListener(createPhoneNumberWatcher());
        //little hack to make phonenumber be reformatted with new formatter
        mPhone.getText().append('1');
        mPhone.getText().delete(mPhone.getText().length() - 1, mPhone.getText().length());

        //for the country in provider let's try to find MCC code
        mCountryButton.setText(mPhoneCountryProvider.getCountryMcc());
    }

    //

    static private class PhoneCountryProvider {

        //
        protected boolean mIsPhoneValid;

        //phone/country stuff
        protected String mPhone;
        protected String mCountryIso;
        protected String mCountryMcc;

        protected final PhoneNumberUtil mPhoneNumberUtil = PhoneNumberUtil.getInstance();

        public PhoneCountryProvider(Context context) {
            mIsPhoneValid = false;
            mCountryMcc = "+";

            String user = IdentityHandler.getUser(context);

            if (TextUtils.isEmpty(user)) {
                //trying to get phone number from telephony
                this.mPhone = TelephonyUtils.getPhoneNumber(context);
            } else {
                try {
                    Gson gson = new Gson();
                    UserResponse userResponse = gson.fromJson(user, UserResponse.class);
                    this.mPhone = userResponse.getPhoneNumber();
                } catch (Exception e) {
                    //TODO: logging
                }
            }

            guessCountry(context);
        }

        private PhoneCountryProvider() {}

        //

        /**
         * Method to validate phone number
         * It involves constructing instance of PhoneNumber from phone/country and validating it
         */
        private void validatePhone() {
            mIsPhoneValid = false;
            Phonenumber.PhoneNumber parsedPhone = null;
            try {
                //parsing number
                parsedPhone = mPhoneNumberUtil.parse(mPhone, mCountryIso);
            } catch (NumberParseException e) {
            }
            //parsing was ok and we can validate number
            mIsPhoneValid = parsedPhone != null && mPhoneNumberUtil.isValidNumber(parsedPhone);
        }

        //protected

        /**
         * Obtaining country iso from phone number if it's in INTERNATIONAL format
         */
        protected void guessCountryFromPhone() {
            Phonenumber.PhoneNumber parsedPhone = null;
            try {
                //parsing number
                parsedPhone = mPhoneNumberUtil.parse(mPhone, null);
            } catch (NumberParseException e) {
            }
            //parsing was ok and we can retrieve something from parsedPhone
            if (parsedPhone != null) {
                this.mPhone = mPhoneNumberUtil.format(parsedPhone, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL).replace("+" + parsedPhone.getCountryCode(), "").trim();
                setCountryIso(mPhoneNumberUtil.getRegionCodeForCountryCode(parsedPhone.getCountryCode()));
            }
        }

        /**
         * Method that tries to get country values at first from phone number, then from telephony and from locale
         *
         * @param context
         */
        protected void guessCountry(Context context) {

            //trying to obtain countryIso from phone number if available
            guessCountryFromPhone();

            //if country is still empty
            if (TextUtils.isEmpty(mCountryIso)) {
                setCountryIso(TelephonyUtils.getCurrentCountryIso(context));
            }
        }

        //public methods

        public String getPhoneNumber() {
            return mPhone;
        }

        public String getFormattedPhoneNumber(PhoneNumberUtil.PhoneNumberFormat format) {

            String phoneStr = mPhone == null ? "" : mPhone;
            Phonenumber.PhoneNumber parsedPhone;
            try {
                //parsing number
                parsedPhone = mPhoneNumberUtil.parse(phoneStr, mCountryIso);
                phoneStr = mPhoneNumberUtil.format(parsedPhone, format);
            } catch (NumberParseException e) {
            }
            return phoneStr;
        }

        public String getCountryIso() {
            return mCountryIso;
        }

        public String getCountryMcc() {
            return mCountryMcc;
        }

        public void setPhoneNumber(String phone) {
            this.mPhone = phone;
            validatePhone();
        }

        public void setFullPhoneNumber(String phone) {
            this.mPhone = phone;
            guessCountryFromPhone();
            validatePhone();
        }

        public void setCountryIso(String iso) {
            this.mCountryIso = iso;

            //for the country in provider let's try to find MCC code
            int mcc = mPhoneNumberUtil.getCountryCodeForRegion(mCountryIso);
            mCountryMcc = mcc == 0 ? "+" : "+" + mcc;

            validatePhone();
        }

        public boolean isPhoneValid() {
            return mIsPhoneValid;
        }
    }

    //dummy class to supply testing data to views
    static private class DummyPhoneCountryProvider extends PhoneCountryProvider {

        public DummyPhoneCountryProvider() {
            this.mPhone = "+380995695852";
            guessCountryFromPhone();
        }

    }

    //class that uses geocoder to retrieve country from location
    private class CountryFetcherAsyncTask extends AsyncTask<Location, Void, String> {

        @Override
        protected String doInBackground(Location... params) {
            String iso = Locale.getDefault().getCountry();
            if (!isCancelled()) {
                try {
                    Address address = null;
                    Geocoder geocoder = new Geocoder(getActivity().getApplicationContext(), Locale.getDefault());
                    List<Address> addressList = geocoder.getFromLocation(params[0].getLatitude(), params[0].getLongitude(), 10);
                    if (addressList != null && addressList.size() > 0) {
                        address = addressList.get(0);
                        iso = address.getCountryCode();
                    }
                } catch (Exception e) {
                }
            }
            return iso;
        }

        @Override
        protected void onPostExecute(String iso) {
            if (!isCancelled() && !TextUtils.isEmpty(iso)) setCountryIso(iso);
        }
    }

    private void requestAppName()
    {
		final SharedPreferences prefs = getActivity().getApplicationContext()
			.getSharedPreferences(PhoneId.getClientId()
				, Context.MODE_PRIVATE);
		String appName = prefs.getString(Commons.PREF_CLIENT_APP_NAME, null);
		if (!TextUtils.isEmpty(appName))
		{
			mHeader.setText(Html.fromHtml(
				String.format(getString(R.string.phid_label_access_app_with_phone)
					, appName)));
			mHeader.setVisibility(View.VISIBLE);
		}

        PhoneIdRestClient.get().getClients(PhoneId.getClientId(), new RestCallback<ClientResponse>()
        {
            @Override
            public void success(final ClientResponse clientResponse, retrofit.client.Response response)
            {
                if (getActivity() != null)
                {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            if (!TextUtils.isEmpty(clientResponse.getAppName()))
                            {
                                mHeader.setText(Html.fromHtml(
                                    String.format(getString(R.string.phid_label_access_app_with_phone)
                                        , clientResponse.getAppName())));
                                mHeader.setVisibility(View.VISIBLE);

                                SharedPreferences.Editor edit = prefs.edit();
                                edit.putString(Commons.PREF_CLIENT_APP_NAME, clientResponse.getAppName());
                                edit.apply();
                            }
                        }
                    });
                }
            }

            @Override
            public void failure(final ErrorResponse errorResponse, RetrofitError error)
            {
                PhoneId phoneId = PhoneId.getInstance();
                if (phoneId != null) { phoneId.handleServerError(errorResponse, error); }
            }
        });
    }
}
