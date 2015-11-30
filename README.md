
# Phone.id SDK (rev #37)

Phone.id SDK is handy android library for easy user login in your application by phone number. 
Library provides easy-to-use API and UI which you may include into your application UI and customize
it with your application style.
Phone.id SDK library may work with or without AppCompat android library with corresponding themes.

## Phone.id SDK library quick start guide

Include Phone.id SDK into your project by adding dependency to your project build.gradle file:

	repositories {
		maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
		mavenCentral()
	}

	dependencies {
		...
    	compile 'id.phone:library:0.9.39-SNAPSHOT'
		...
	}

### Phone.id SDK initialization

PhoneId instance should be initialized in your android.app.Application class:

	public class DemoApplication extends Application 
	{

		private PhoneId sPhoneId;

		@Override
		public void onCreate() {
			super.onCreate();

		// Init Phone.id SDK instance
		sPhoneId = PhoneId.getInstance(this, "<Your application ID on Phone.ID website>");
	}
	
	@Override
	public void onTerminate()
	{
		super.onTerminate();
		sPhoneId.onDestroy();
	}


### Phone.id authorization process.

Phone.id Library contains two UI interfaces:
- Login button + Full-screen interface which performs login in it's own activity;
- Compact button based on android fragment which performs login in-place in your activity;

#### Login using LoginButton and Full-Screen UI

LoginButton is customizable Button class which may be placed anywhere on your activity/fragment and
initiates Phone.id authorization process. This button indicates login status and handles click events
to login/log-out from Phone.id server. To use LoginButton add it tou your layout xml file like this:

    <id.phone.sdk.ui.view.LoginButton
        android:id="@+id/btnPhoneIdLoginButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        />

When LoginButton clicked the Phone.id login activity started by startActivityForResult on
behalf of your activity. When Phone.id login activity completed you can get results in your activity
onActivityResult function:

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == LoginButton.LOGIN_REQUEST_CODE && resultCode == RESULT_OK && data != null)
		{
			UserProfile userProfile = data.getParcelableExtra(UserProfile.class.getName());
			TokenResponse tokenResponse = data.getParcelableExtra(TokenResponse.class.getName());

			....
		}		
		else super.onActivityResult(requestCode, resultCode, data);
	}

TokenResponse contains access token which may be used for secure data access on your server. 
UserProfile contains user-specific information retrieved from server during login.

#### Login using in-place Compact UI
You can insert Phone.id Compact UI fragment into your UI layout. Compact UI has a size of
the LoginButton, but does not open separate activity for login and does everything "in-place". 
Compact UI fragment may be used only inside Activity (no nested fragments allowed).
To insert Compact UI into your layout use the following snippet:

	<fragment
		android:id="@+id/phoneid_login_fragment"
		android:layout_width="wrap_content"
		android:layout_height="wrap_content"
		android:name="id.phone.sdk.PhoneIdFragment"
		tools:layout="@layout/phid_fragment_compact_ui" />

You can change fragment width when needed as:

	<fragment
		android:id="@+id/phoneid_login_fragment"
		android:layout_width="350dp"
		...
		/>

For example. 
Fragment height is fixed and can't be changed.
To follow user login process you need to catch broadcasts generated by Phone.id using 
LocalBroadcastReceiver (see next chapter).

#### Monitoring Phone.id activity

Phone.id SDK generates updates through LocalBroadcastManager. To catch updates you should register
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
					TokenResponse tokenResponse = intent.getParcelableExtra(TokenResponse.class.getName());
					// Perform actions when logged-in
					...
				}
				else if (PhoneId.ACTION_LOGGED_OUT.equals(intent.getAction()))
				{
					// Perform action on logged-out
				}
				else if (PhoneId.ACTION_STATUS.equals(intent.getAction()))
				{
					String message = intent.getStringExtra(PhoneId.ARG_MESSAGE)
					// Display current login process stage/status in your UI
				}
				else if (PhoneId.ACTION_USER_PROFILE.equals(intent.getAction()))
				{
					UserProfile userProfile = intent.getParcelableExtra(UserProfile.class.getName());
					// Do what you need with user profile
					...
				}
				else if (PhoneId.ACTION_ERROR.equals(intent.getAction()))
				{
					String errorKind = intent.getStringExtra(PhoneId.ARG_ERROR_KIND);
					String errorCode = intent.getStringExtra(PhoneId.ARG_ERROR_CODE);
					String errorMessage = intent.getStringExtra(PhoneId.ARG_ERROR_MESSAGE);
					// Display error message to user. 
					...
				}
			}
		}
	};

Then you need to register your BroadcastReceiver:

	@Override
    protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		...
	
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(PhoneId.ACTION_LOGGED_IN);
		intentFilter.addAction(PhoneId.ACTION_LOGGED_OUT);
		intentFilter.addAction(PhoneId.ACTION_USER_PROFILE);
		intentFilter.addAction(PhoneId.ACTION_USER_PROFILE);
		intentFilter.addAction(PhoneId.ACTION_STATUS);
		LocalBroadcastManager.getInstance(activity).registerReceiver(phoneIdEventsReceiver,
			intentFilter);	
					
		...
	}

And do not forget to unregister it:

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(phoneIdEventsReceiver);
	}

If you already have customer phone number and want phone.id to start from this number un UI, then pass
it to PhoneId object like this:

	public MainActivity()
	{
		// Setup customer phone number if any available
		PhoneId.setCustomerPhoneNumber("+1800555123000");
	}

Make sure you made this call BEFORE UI initialized (before onCreate call for the login Activity).

## Phone.id UI Customization

Phone.id library UI is fully customizable. You can change appearance for LoginButton, for
Phone.id full-screen UI and for Phone.id Compact button UI.

### LoginButton customization

LoginButton has built-in defaults which may be overridden by your code. For example:

    <id.phone.sdk.ui.view.LoginButton
        android:id="@+id/btnPhoneIdLoginButtonStyled"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:minEms="17"
        android:layout_centerHorizontal="true"
        android:text ="Logout (styled)"
        app:textLoggedOut="Login Phone.id (styled)"
        android:background="@drawable/login_btn_styled_bg"
        android:textColor="@android:color/white"
		android:gravity="center_vertical|left"
		android:drawablePadding="10dp"
        />

### Phone.id full-screen and Compact Button UI theme

Phone.id UI theme is fully customizable by your general project brand colors. 
You can replace theme style for the whole Phone.id UI. For example if your brand color is tomato:
 
	<!-- Sample accent colors for alternate Phone.id theme -->
	<color name="tomato">#ffe85025</color>
	<color name="tomato_dark">#ffbf360c</color>

you may redefine the theme for the Phone.id UI controls like this for pre-21 API:

	<!-- Sample theme for changing Phone.id controls accent colors  -->
    <style name="Theme_PhoneIdAlternate" parent="@style/phid_Theme">

		<!-- Make sure to replace action bar style if you change it -->
        <item name="android:actionBarStyle">@style/Theme_PhoneIdActionBarAlternate</item>

		<!--   color for highlighted texts on light background -->
		<item name="android:textColorHighlight">@color/tomato</item>
		<!--   panels background used panels with darker background -->
		<item name="android:panelColorBackground">@color/tomato</item>

    </style>

	<!-- Sample theme for changing Phone.id action bar accent colors  -->
    <style name="Theme_PhoneIdActionBarAlternate" parent="@style/phid_ActionBar">
		<!-- Action bar background color -->
        <item name="android:background">@color/tomato</item>
    </style>

And like this for API 21 and above:

    <!-- Sample theme for changing Phone.id controls accent colors  -->
    <style name="Theme_PhoneIdAlternate" parent="@style/phid_Theme">

        <!-- your app branding color for the app bar -->
        <item name="android:colorPrimary">@color/tomato</item>
        <!--   darker variant for the status bar and contextual app bars -->
        <item name="android:colorPrimaryDark">@color/tomato_dark</item>
        <!--   theme UI controls like checkboxes and text fields -->
        <item name="android:colorAccent">@color/tomato</item>
        <!--   bottom navigation bar color -->
        <item name="android:navigationBarColor">@color/tomato_dark</item>

        <!--   color for highlighted texts on light background -->
        <item name="android:textColorHighlight">@color/tomato</item>
        <!--   panels background used panels with darker background -->
        <item name="android:panelColorBackground">@color/tomato</item>

    </style>

The following AppCompat theme should be overridden in case your project using AppCompat library:

	<!-- Sample accent colors for alternate Phone.Id theme -->
	<color name="tomato">#ffe85025</color>
	<color name="tomato_dark">#ffbf360c</color>

	<!-- Sample theme for changing Phone.Id controls accent colors  -->
    <style name="Theme_PhoneIdAlternate" parent="@style/phid_ThemeAppCompat">

		<!-- Make sure to replace action bar style if you change it -->
        <item name="actionBarStyle">@style/Theme_PhoneIdActionBarAlternate</item>

		<!--   color for highlighted texts on light background -->
		<item name="android:textColorHighlight">@color/tomato</item>
		<!--   panels background used panels with darker background -->
		<item name="android:panelColorBackground">@color/tomato</item>

    </style>

	<!-- Sample theme for changing Phone.Id action bar accent colors  -->
    <style name="Theme_PhoneIdActionBarAlternate" parent="@style/phid_ActionBarAppCompat">
		<!-- Action bar background color -->
		<item name="background">@color/tomato</item>
		<item name="colorPrimary">@color/tomato</item>
		<item name="colorPrimaryDark">@color/tomato_dark</item>
    </style>


To apply this new theme to Phone.id controls call 

	PhoneId.setThemeResId(R.style.Theme_PhoneIdAlternate);

in your activity/application constructor before any of Phone.id view being inflated.  

	public MainActivity()
	{
		// Setup Phone.id UI color theme
		PhoneId.setThemeResId(R.style.Theme_PhoneIdAlternate);
	}

You also can redefine any resource with name starting from "phid_" prefix in your resources. 

## Social functions
### User object

Phone.id library contains id.phone.sdk.social.User object which provides handful information about 
one of contacts. You can create instance of id.phone.sdk.social.User object from E164 formatted 
phone number.

	  public static User getInstance(String phoneNumber) throws UserNotFoundException

Phone numbers are never transmitted to server in clear form - just as SHA-hashes. In case you have 
user phone number SHA-hash retrieved from server you can use it to get user profile information 
like this:

	  public static User getInstance(String phoneNumberSHAHash) throws UserNotFoundException

User will be looked up in local contacts database. If nothing found, then UserNotFoundException 
thrown. User lookup function is highly optimized and takes minimal time.

You can get user information from server API using

	public static void getRemoteInstance(@NonNull String phoneNumber
		, @NonNull UserInstanceCreatedCallback callback)

	public static void getRemoteInstance(@NonNull String phoneNumberSHAHash
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

### Converting contact phone number to display name

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

### User profile UI

User profile UI provided by Phone.id library allows to setup/edit user properties like name, 
avatar and birthday. User profile data stored on Phone.id server and provided only for users's 
friends who have his number in contacts. 
To open User Profile UI activity use the following Intent:

	PhoneId.getInstance().startUserProfileActivity(Context context);

