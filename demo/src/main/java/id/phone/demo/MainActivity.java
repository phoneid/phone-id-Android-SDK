package id.phone.demo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import id.phone.demo.ui.InfoDialog;
import id.phone.sdk.PhoneId;
import id.phone.sdk.social.User;
import id.phone.sdk.social.UserNotFoundException;
import id.phone.sdk.rest.response.TokenResponse;
import id.phone.sdk.rest.response.UserProfile;
import id.phone.sdk.ui.activity.UserProfileActivity;
import id.phone.sdk.ui.view.LoginButton;


public class MainActivity extends Activity
{
	public static final String TAG = MainActivity.class.getSimpleName();
	private static final int PICK_CONTACT_REQUEST = 122;

	Button btnLoginAccountManager;
	ViewGroup layoutButtons;
	Button btnShowAccessToken;
	Button btnShowUserInfo;
	Button btnUploadContacts;
	ViewGroup layoutContacts;
	ImageButton btnPickContact;

	private BroadcastReceiver phoneIdEventsReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent != null)
			{
				if (PhoneId.ACTION_LOGGED_IN.equals(intent.getAction()))
				{
					TokenResponse tokenResponse = intent.getParcelableExtra(TokenResponse.class.getName());
					phoneIdEventListener.onLoggedIn(tokenResponse);
				}
				else if (PhoneId.ACTION_LOGGED_OUT.equals(intent.getAction()))
					phoneIdEventListener.onLoggedOut();
				else if (PhoneId.ACTION_USER_PROFILE.equals(intent.getAction()))
				{
					UserProfile userProfile = intent.getParcelableExtra(UserProfile.class.getName());
					phoneIdEventListener.onUserProfile(userProfile);
				}
				else if (PhoneId.ACTION_CONTACTS_UPLOADED.equals(intent.getAction()))
					phoneIdEventListener.onContactsUploaded(
						intent.getStringExtra(PhoneId.ARG_RESPONSE));
				else if (PhoneId.ACTION_ERROR.equals(intent.getAction()))
					phoneIdEventListener.onError(
						intent.getStringExtra(PhoneId.ARG_ERROR_KIND)
						, intent.getStringExtra(PhoneId.ARG_ERROR_CODE)
						, intent.getStringExtra(PhoneId.ARG_ERROR_MESSAGE)
					);
				else if (PhoneId.ACTION_STATUS.equals(intent.getAction()))
					phoneIdEventListener.onStatus(
						intent.getStringExtra(PhoneId.ARG_MESSAGE)
					);
			}
		}
	};

	public MainActivity()
	{
		// Setup customer phone number if any available
		// PhoneId.setCustomerPhoneNumber("+1800555123000");
		// Setup Phone.id UI color theme
		// PhoneId.setThemeResId(R.style.Theme_PhoneIdAlternate);
	}

	@Override
    protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		btnLoginAccountManager = (Button)findViewById(R.id.btnLoginAccountManager);
		layoutButtons = (ViewGroup)findViewById(R.id.layoutButtons);
		btnShowAccessToken = (Button)findViewById(R.id.btnShowAccessToken);
		btnShowUserInfo = (Button)findViewById(R.id.btnShowUserInfo);
		btnUploadContacts = (Button)findViewById(R.id.btnUploadContacts);
		layoutContacts = (ViewGroup)findViewById(R.id.layoutContacts);
		btnPickContact = (ImageButton)findViewById(R.id.btnPickContact);

		btnLoginAccountManager.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				try
				{
					PhoneId.getInstance()
						.getAccessToken(MainActivity.this, new PhoneId.TokenResponseCallback()
						{
							@Override
							public void tokenResponseDelivered(TokenResponse tokenResponse)
							{
								InfoDialog.newInstance(R.string.msg_access_token,
									tokenResponse.toString())
									.show(getFragmentManager(), InfoDialog.TAG);
							}
						});
				}
				catch (Exception ex)
				{
					InfoDialog.newInstance(R.string.phid_error, ex.getLocalizedMessage())
						.show(MainActivity.this.getFragmentManager(), InfoDialog.TAG);
				}
			}
		});

		try
		{
			layoutButtons
				.setVisibility(PhoneId.getInstance().isLoggedIn() ? View.VISIBLE : View.GONE);
		}
		catch (Exception ex)
		{
			InfoDialog.newInstance(R.string.phid_error, ex.getLocalizedMessage())
				.show(MainActivity.this.getFragmentManager(), InfoDialog.TAG);
		}

		btnShowAccessToken.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				try
				{
					TokenResponse response;
					if ((response =
						PhoneId.getInstance().getAccessToken()) != null)
					{
						InfoDialog.newInstance(R.string.msg_access_token, response.toString())
							.show(MainActivity.this.getFragmentManager(), InfoDialog.TAG);
					}
				}
				catch (Exception ex)
				{
					InfoDialog.newInstance(R.string.phid_error, ex.getLocalizedMessage())
						.show(MainActivity.this.getFragmentManager(), InfoDialog.TAG);
				}
			}
		});

		btnShowUserInfo.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				try
				{
					PhoneId.getInstance().startUserProfileActivity(MainActivity.this);
				}
				catch (Exception ex)
				{
					InfoDialog.newInstance(R.string.phid_error, ex.getLocalizedMessage())
						.show(MainActivity.this.getFragmentManager(), InfoDialog.TAG);
				}
			}
		});

		btnUploadContacts.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				try
				{
					PhoneId.getInstance().uploadContactsToServer();
					btnUploadContacts.setText(R.string.btn_uploading_contacts);
					btnUploadContacts.setEnabled(false);
				}
				catch (Exception ex)
				{
					InfoDialog.newInstance(R.string.phid_error, ex.getLocalizedMessage())
						.show(MainActivity.this.getFragmentManager(), InfoDialog.TAG);
				}
			}
		});

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(PhoneId.ACTION_LOGGED_IN);
		intentFilter.addAction(PhoneId.ACTION_LOGGED_OUT);
		intentFilter.addAction(PhoneId.ACTION_USER_PROFILE);
		intentFilter.addAction(PhoneId.ACTION_CONTACTS_UPLOADED);
		intentFilter.addAction(PhoneId.ACTION_STATUS);
		intentFilter.addAction(PhoneId.ACTION_ERROR);
		LocalBroadcastManager.getInstance(this).registerReceiver(phoneIdEventsReceiver,
			intentFilter);
    }

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(phoneIdEventsReceiver);
	}

	public void onPickContact(View view)
	{
		Intent pickContactIntent =
			new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
		pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
		startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == LoginButton.LOGIN_REQUEST_CODE && resultCode == RESULT_OK && data != null)
		{
			UserProfile userProfile = data.getParcelableExtra(UserProfile.class.getName());
			TokenResponse tokenResponse = data.getParcelableExtra(TokenResponse.class.getName());

			showUserInfo(userProfile.toString());
		}
		else if ( requestCode == PICK_CONTACT_REQUEST )
		{

			if ( resultCode == Activity.RESULT_OK )
			{
				try
				{
					User user = User.getInstance(data.getData());

					QuickContactBadge badge = user.createQuickContactBadge(this);
					int margin = (int)getResources().getDimension(R.dimen.activity_horizontal_margin);
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.WRAP_CONTENT);
					params.setMargins(margin, margin, margin, margin);
					layoutContacts.addView(badge, params);
				}
				catch (UserNotFoundException ex)
				{
					InfoDialog.newInstance(R.string.phid_error,
						getString(R.string.err_no_user_found) + data.getData().toString())
						.show(getFragmentManager(), InfoDialog.TAG);
				}
			}
		}
		else super.onActivityResult(requestCode, resultCode, data);
	}

	public void showUserInfo(String userInfo)
	{
		((TextView)findViewById(R.id.txtUserInfo)).setText(userInfo);
	}

	private void createSampleQuickContactBadge()
	{
		final String userPhoneNumber = "+1800555333";
		try
		{
			User user1 = User.getInstance(userPhoneNumber);
			layoutContacts.removeAllViews();
			QuickContactBadge badge = user1.createQuickContactBadge(this);
			layoutContacts.addView(badge,
				new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
		}
		catch (UserNotFoundException ex)
		{
			InfoDialog.newInstance(R.string.msg_user_info, "No user found with phone: " + userPhoneNumber)
				.show(MainActivity.this.getFragmentManager(), InfoDialog.TAG);
		}
	}

	private class PhoneIdEventListener
	{
		public void onLoggedIn(TokenResponse tokenResponse)
		{
			MainActivity.this.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					layoutButtons.setVisibility(View.VISIBLE);
					btnUploadContacts.setEnabled(true);
					btnUploadContacts.setText(R.string.btn_upload_contacts);
				}
			});
		}

		public void onLoggedOut()
		{
			MainActivity.this.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					layoutButtons.setVisibility(View.GONE);
					showUserInfo(null);
				}
			});
		}

		public void onUserProfile(final UserProfile userProfile)
		{
			MainActivity.this.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					showUserInfo(userProfile.toString());
				}
			});
		}

		public void onContactsUploaded(final String response)
		{
			MainActivity.this.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					btnUploadContacts.setEnabled(true);
					btnUploadContacts.setText(R.string.btn_upload_contacts);
					InfoDialog.newInstance(R.string.msg_contacts_uploaded, response)
						.show(MainActivity.this.getFragmentManager(), InfoDialog.TAG);
				}
			});

		}

		public void onError(String code, final String message, final String kind)
		{
			MainActivity.this.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					showUserInfo(kind + ": " + message);
					InfoDialog.newInstance(R.string.phid_error, kind + ": " + message)
						.show(MainActivity.this.getFragmentManager(), InfoDialog.TAG);
				}
			});
		}
		public void onStatus(final String message)
		{
			MainActivity.this.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					showUserInfo(message);
				}
			});
		}
	};
	private PhoneIdEventListener phoneIdEventListener = new PhoneIdEventListener();

}
