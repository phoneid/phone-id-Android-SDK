
# Phone.Id SDK

## Phone.Id SDK library 

To include Phone.Id SDK into your project add the following dependency to your build.gradle file:

	repositories {
		maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
		mavenCentral()
	}

	dependencies {
		...
    	compile 'id.phone:library:0.9.19-SNAPSHOT'
		...
	}

## Phone.Id SDK initialization

PhoneId instance should be initialized in your android.app.Application class:

	public class DemoApplication extends Application 
	{

		private PhoneId sPhoneId;

		@Override
		public void onCreate() {
			super.onCreate();

		// Init Phone.Id SDK instance
		sPhoneId = PhoneId.getInstance(this, "<Your application ID on Phone.ID website>");
	}
	
	@Override
	public void onTerminate()
	{
		super.onTerminate();
		sPhoneId.onDestroy();
	}


## Phone.Id authorization process.

### Using 'id.phone.sdk.ui.view.LoginButton'

LoginButton is customized Button class which may be placed anywhere on your activity/fragment and
handle Phone.Id authorization process. This button indicates login status and handles click events
to login/log-out from Phone.Id server. To use LoginButton add it tou your layout xml file:

    <id.phone.sdk.ui.view.LoginButton
        android:id="@+id/btnPhoneIdLoginButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        />

### Catching Phone.Id updates

Phone.Id SDK generates updates through LocalBroadcastManager. To catch updates you should register
BroadcastReceiver in your activity/fragment class:

	private BroadcastReceiver phoneIdEventsReceiver = new BroadcastReceiver() 
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent != null)
			{
				if (PhoneId.ACTION_LOGGED_IN.equals(intent.getAction()))
				{
					String tokenType = intent.getStringExtra(PhoneId.ARG_TOKEN_TYPE)
					String accessToken = intent.getStringExtra(PhoneId.ARG_ACCESS_TOKEN)
					String refreshToken = intent.getStringExtra(PhoneId.ARG_REFRESH_TOKEN)
					// Perform actions when logged-in
					...
				}
				else if (PhoneId.ACTION_LOGGED_OUT.equals(intent.getAction()))
				{
					// Perform action on logged-out
				}
				else if (PhoneId.ACTION_USER_PROFILE.equals(intent.getAction()))
				{
					UserResponse user = PhoneId.getInstance().getUser();
					// Do what you need with user profile
					...
				}
				else if (PhoneId.ACTION_ERROR.equals(intent.getAction()))
				{
					String errorKind = intent.getStringExtra(PhoneId.ARG_ERROR_KIND);
					String errorCide = intent.getStringExtra(PhoneId.ARG_ERROR_CODE);
					String errorMessage = intent.getStringExtra(PhoneId.ARG_ERROR_MESSAGE);
					// Display error message to user
					...
				}
			}
		}
	};

Then you need to register your BroadcastReceiver:

		@Override
		public void onAttach(Activity activity)
		{
			super.onAttach(activity);

			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(PhoneId.ACTION_LOGGED_IN);
			intentFilter.addAction(PhoneId.ACTION_LOGGED_OUT);
			intentFilter.addAction(PhoneId.ACTION_USER_PROFILE);
			intentFilter.addAction(PhoneId.ACTION_ERROR);
			LocalBroadcastManager.getInstance(activity).registerReceiver(phoneIdEventsReceiver,
				intentFilter);
		}

And do not forget to unregister your BroadcastReceiver:

		@Override
		public void onDestroy()
		{
			super.onDestroy();
			if (getActivity() != null)
				LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(phoneIdEventsReceiver);
		}

### Using 'PhoneId.getInstance().getAccessToken(@Nullable Activity activity,
@Nullable TokenResponseCallback tokenResponseCallback)

This function retrieves Phone.Id access token for your application from AccountManager or creates
new account if no any account available yet. You can call this function when user clicks your
customized PhoneId login control or anytime when you need PhoneId access token in your application.
This is asynchronous function and it returns result in tokenResponseCallback.
If tokenResponseCallback is null, then locally stored access token will be returned immediately (if any).
When there is no PhoneId account available in AccountManager this function will return null and
will not call tokenResponseCallback. PhoneId login activity will be started instead and you should
catch it result in your activity onActivityResult function:

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);

		//ok we've got some data from the PhoneId SDK
		if (resultCode == RESULT_OK && requestCode == LoginButton.LOGIN_REQUEST_CODE) 
		{
			TokenResponse tokenResponse = PhoneId.getInstance().getAccessToken();
			UserResponse user = PhoneId.getInstance().getUser();
			// Do what you need to do with access token and user profile
			...
		}
	}

Or you can use BroadcastReceiver as described in "Catching Phone.Id updates"

## Using AccountManager to get Phone.Id access token

Phone.Id when logged-in once registers account in Android AccountManager. Thus AccountManager
default functionality may be used to retrieve access token by your application

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container
	, @Nullable Bundle savedInstanceState) 
	{
		...
		mAccountManager = AccountManager.get(getActivity());
		mAccounts = mAccountManager.getAccountsByType(PhoneId.ACCOUNT_TYPE);

		if (mAccounts != null && mAccounts.length > 0)
		{
			Intent accountsIntent = AccountManager.newChooseAccountIntent(
				null
				, null
				, new String[]{ "phone.id" }	// PhoneId.ACCOUNT_TYPE
				, false
				, null
				, "default"	// PhoneId.TOKEN_TYPE_DEFAULT
				, null
				, null);
			startActivityForResult(accountsIntent, ACCOUNT_CHOOSER_ACTIVITY);
		}		
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
				"ARG_TOKEN_TYPE"	// PhoneId.ARG_TOKEN_TYPE,
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
			// Here you have the Access Token
			mAccessToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
		}
	}

## Converting contact phone number to display name

If you need to display user/contact in your UI having phone number PhoneId has handy wrapper around
ContactsContract.PhoneLookup system content provider:

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
		, boolean allowMultipleEntries);

You can use it in the following way for example:

	((TextView)view.findViewById(R.id.textView_UserName)).setText(PhoneId.getUserDisplayName(
		getActivity(), phoneNumber, true);

## UI Customizing

Phone.Id library UI is fully customizable. You can change appearance for both LoginButton and for
Phone.Id UI.

### LoginButton customization

LoginButton has built-in defaults which may be overridden by your code. For example:

    <id.phone.sdk.ui.view.LoginButton
        android:id="@+id/btnPhoneIdLoginButtonStyled"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:minEms="17"
        android:layout_centerHorizontal="true"
        android:text ="Logout (styled)"
        app:textLoggedOut="Login Phone.Id (styled)"
        android:background="@drawable/login_btn_styled_bg"
        android:textColor="@android:color/white"
		android:gravity="center_vertical|left"
		android:drawablePadding="10dp"
        />

### Phone.Id UI customization

You can override any or all UI resources provided by Phone.id-sdk library. All Phone.id-sdk resource
names are started from "phid_" prefix.

## Social functions
### User object

id.phone.sdk.social.User object provides handful information about one of your contacts. You can create
 instance of id.phone.sdk.social.User object from E164 formatted phone number.

	  public static User getInstance(String phoneNumber) throws UserNotFoundException

User will be looked up in local contacts database. If nothing found, then UserNotFoundException thrown.
You can get user information from server API using

	public static void getRemoteInstance(@NonNull String phoneNumber
		, @NonNull UserInstanceCreatedCallback callback)

UserInstanceCreatedCallback callback will return a User instance or an error.

#### QuickContactBadge for your UI

id.phone.sdk.social.User class instance may create and setup the QuickContactBadge view object for
 the selected user. Below is sample code:

	private void createSampleQuickContactBadge(String userPhoneNumber)
	{
		try
		{
			User user = User.getInstance(userPhoneNumber);
			layoutContacts.removeAllViews();
			QuickContactBadge badge = user.createQuickContactBadge(getActivity());
			layoutContacts.addView(badge,
				new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
		}
		catch (UserNotFoundException ex)
		{
			InfoDialog.newInstance(R.string.msg_user_info, "No user found with phone: " + userPhoneNumber)
				.show(getActivity().getSupportFragmentManager(), InfoDialog.TAG);
		}
	}

