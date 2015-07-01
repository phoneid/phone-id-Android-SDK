package id.phone.sdk.ui.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import id.phone.sdk.Commons;
import id.phone.sdk.PhoneId;
import id.phone.sdk.R;
import id.phone.sdk.rest.PhoneIdRestClient;
import id.phone.sdk.rest.RestCallback;
import id.phone.sdk.rest.response.ErrorResponse;
import id.phone.sdk.rest.response.TokenResponse;
import id.phone.sdk.rest.response.UserResponse;
import id.phone.sdk.service.FirstRunService;
import id.phone.sdk.auth.IAuthActivity;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Fragment with the field for the code entering receive from SMS
 * Created by azubchenko on 4/1/15.
 * Updated by Dennis Gubsky
 */
public class CodeFragment extends BaseFragment {

    private String mPhone;

    private EditText mCode;
    private ProgressBar mSpinner;
    private View okButton;
	private ImageView mStatusIcon;
	private TextView mHeader;
	private TextView mErrorBanner;
    private TextView mMessage;
    private TextView mBtnCallMeBack;

    private BroadcastReceiver mReceiver;
    private final Gson gson = new Gson();

    static public CodeFragment getInstance(String phone)
    {
        CodeFragment fragment = new CodeFragment();
        Bundle args = new Bundle();
        args.putString(Commons.PARAM_PHONE, phone);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.phid_fragment_code, container, false);

        View backButton = view.findViewById(R.id.back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //hiding keyboard
                hideSoftwareKeyboard();

                getActivity().onBackPressed();
            }
        });

        okButton = view.findViewById(R.id.ok);
        okButton.setVisibility(View.INVISIBLE);
        okButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                requestToken(mCode.getText().toString());
            }
        });

		mStatusIcon = (ImageView)view.findViewById(R.id.status_icon);
		mStatusIcon.setVisibility(View.GONE);

        mCode = (EditText) view.findViewById(R.id.code);
        mCode.setOnEditorActionListener(new TextView.OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_DONE)
				{
					requestToken(mCode.getText().toString());
					handled = true;
				}
				return handled;
			}
		});
        mCode.addTextChangedListener(codeTextWatcher);

        mSpinner = (ProgressBar)view.findViewById(R.id.spinner);
        mSpinner.setVisibility(View.GONE);

        mBtnCallMeBack = (TextView)view.findViewById(R.id.btnCallMeBack);
        mBtnCallMeBack.setText(Html.fromHtml(getString(R.string.phid_btn_call_me_back)));
		mBtnCallMeBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)
			{
				// TODO: initiate callback sequence
			}
		});

        mMessage = (TextView)view.findViewById(R.id.message);
		mHeader = (TextView)view.findViewById(R.id.header);
		mErrorBanner = (TextView)view.findViewById(R.id.error_banner);

        return view;
    }

    private final TextWatcher codeTextWatcher = new TextWatcher()
    {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count)
        {

        }

        @Override
        public void afterTextChanged(Editable s)
        {
            mSpinner.setVisibility(View.GONE);
			mStatusIcon.setVisibility(View.GONE);
			mErrorBanner.setVisibility(View.GONE);
			mHeader.setVisibility(View.VISIBLE);
            mMessage.setText(null);
            mBtnCallMeBack.setVisibility(View.GONE);

            if (getActivity() != null)
                ((IAuthActivity)getActivity()).getHandler().removeCallbacks(
					codeReceivingWatchdog);

            //
            if (s.toString().length() == 6)
            {
                requestToken(mCode.getText().toString());
                //okButton.setVisibility(View.VISIBLE);
            }
            else
            {
                okButton.setVisibility(View.INVISIBLE);
            }
        }
    };

    @Override
    protected void saveState(Bundle outState)
    {
        outState.putString(Commons.PARAM_PHONE, mPhone);
    }

    @Override
    protected void restoreState(Bundle savedInstanceState) {
        mPhone = savedInstanceState.getString(Commons.PARAM_PHONE);
    }

    @Override
    protected void populateArguments(Bundle savedInstanceState) {
        restoreState(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getExtras() != null) {
                    try {
                        Object[] smsExtra = (Object[]) intent.getExtras().get("pdus");
                        SmsMessage sms = SmsMessage.createFromPdu((byte[]) smsExtra[0]);
                        String address = sms.getOriginatingAddress();
						long timestamp = sms.getTimestampMillis();

                        //getting body and trying to find code with regexp
                        String body = sms.getMessageBody();
                        Pattern pattern = Pattern.compile("^phone\\.id: ([0-9]{3}) ([0-9]{3}) .+?$");
                        Matcher matcher = pattern.matcher(body);
                        if (matcher.find())
                        {
                            final String receivedCode = matcher.group(1) + matcher.group(2);

                            //code is found!
                            if (getActivity() != null)
                            {
								Intent serviceIntent = new Intent(getActivity(), FirstRunService.class);
								serviceIntent.setAction(FirstRunService.ACTION_ADD_SMS_TO_REMOVE);
								serviceIntent.putExtra(FirstRunService.ARG_ADDRESS, address);
								serviceIntent.putExtra(FirstRunService.ARG_TIMESTAMP, timestamp);
								getActivity().startService(serviceIntent);

                                getActivity().runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        mCode.removeTextChangedListener(codeTextWatcher);
                                        mCode.setText(receivedCode);
                                        mCode.addTextChangedListener(codeTextWatcher);
                                    }
                                });
                            }
                            requestToken(receivedCode);
                            abortBroadcast();
                        }
                    } catch (Exception e) {
                        //
                    }
                }
            }
        };
        //registering our receiver
        IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        intentFilter.setPriority(500);
        getActivity().registerReceiver(mReceiver, intentFilter);

        mCode.requestFocus();
        showSoftwareKeyboard(mCode);

        if (getActivity() != null)
			((IAuthActivity)getActivity()).getHandler().postDelayed(codeReceivingWatchdog, 30000);
        mSpinner.setVisibility(View.VISIBLE);
		mStatusIcon.setVisibility(View.GONE);
		mErrorBanner.setVisibility(View.GONE);
		mHeader.setVisibility(View.VISIBLE);
	}

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    //private methods
    private Runnable codeReceivingWatchdog = new Runnable()
    {
        @Override
        public void run()
        {
            mSpinner.setVisibility(View.GONE);
			mStatusIcon.setVisibility(View.GONE);
			mErrorBanner.setVisibility(View.GONE);
			mHeader.setVisibility(View.VISIBLE);
            hideSoftwareKeyboard();

            PhoneId phoneId = PhoneId.getInstance();
            if (phoneId != null)
                phoneId.handleServerError(new ErrorResponse(getString(R.string.phid_error_havent_received_code_in_time))
                    , null);

            mMessage.setText(R.string.phid_error_havent_received_code_in_time);
            mBtnCallMeBack.setVisibility(View.VISIBLE);

            /*ErrorDialog.newInstance(0, getString(R.string.error_no_code_received))
                .setClickListener(new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        if (getActivity() != null)
                        {
                            getActivity().onBackPressed();
                        }
                    }
                })
                .show(
                    getActivity().getSupportFragmentManager(), ErrorDialog.TAG);*/
        }
    };

    /**
     * Method that calls REST API to retrieve token
     */
    private void requestToken(String code)
    {
        //hiding keyboard
        hideSoftwareKeyboard();
        mSpinner.setVisibility(View.VISIBLE);
		mStatusIcon.setVisibility(View.GONE);
		mErrorBanner.setVisibility(View.GONE);
		mHeader.setVisibility(View.VISIBLE);
        mMessage.setText(R.string.phid_msg_logging_in);
        mBtnCallMeBack.setVisibility(View.GONE);
        if (getActivity() != null)
			((IAuthActivity)getActivity()).getHandler().removeCallbacks(codeReceivingWatchdog);

        mCode.setEnabled(false);
        PhoneIdRestClient.get().getToken(PhoneId.getClientId(), "authorization_code", code + "/" + mPhone, new RestCallback<TokenResponse>() {
            @Override
            public void success(TokenResponse tokenResponse, retrofit.client.Response response) {

                //making sure fragment is still there
                if (getActivity() != null)
                {
                    //lets save our token response in the private prefs
                    PhoneId phoneId = PhoneId.getInstance();
                    if (phoneId != null)
                        phoneId.saveToken(tokenResponse, mPhone);

                    //now we need to get user info
                    requestUser(tokenResponse.getAccessToken());

					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run()
						{
							mSpinner.setVisibility(View.GONE);
							mStatusIcon.setImageResource(
								R.drawable.phid_icon_check_mark);
							mStatusIcon.setVisibility(View.VISIBLE);

							mMessage.setText(R.string.phid_msg_logged_in);
						}
					});
                }
            }

            @Override
            public void failure(final ErrorResponse errorResponse, RetrofitError error)
            {
                PhoneId phoneId = PhoneId.getInstance();
                if (phoneId != null)
                    phoneId.handleServerError(errorResponse, error);

                if (getActivity() != null)
                {
                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mCode.setEnabled(true);
                            mSpinner.setVisibility(View.GONE);

							mStatusIcon.setImageResource(
								android.R.drawable.ic_notification_clear_all);
							mStatusIcon.setVisibility(View.VISIBLE);
							mErrorBanner.setVisibility(View.VISIBLE);
							mHeader.setVisibility(View.INVISIBLE);

							if ("InvalidContent".equalsIgnoreCase(errorResponse.getCode()) ||
								"InvalidCredentials".equalsIgnoreCase(errorResponse.getCode()))
                            {
                                mMessage.setText(
                                    Html.fromHtml(getString(R.string.phid_error_invalid_code)));
								mErrorBanner.setText(R.string.phid_error_invalid_code_short);
                            }
							else
                            {
                                mMessage.setText(errorResponse.getMessage());
								mErrorBanner.setText(R.string.phid_error);
							}

							showSoftwareKeyboard(mCode);

                            /*
							hideSoftwareKeyboard();
                            ErrorDialog.newInstance(0, errorResponse.getMessage())
                                .setClickListener(new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        showSoftwareKeyboard(mCode);
                                    }
                                })
                                .show(
                                    getActivity().getSupportFragmentManager(), ErrorDialog.TAG);*/
                        }
                    });
                }
            }
        });
    }

    /**
     * Method that calls REST API to retrieve user with token newly obtained
     *
     * @param token
     */
    private void requestUser(String token) {
        PhoneIdRestClient.get().getUser(token, new RestCallback<UserResponse>() {

            @Override
            public void success(UserResponse userResponse, Response response)
            {
                PhoneId phoneId = PhoneId.getInstance();
                if (phoneId != null)
                    phoneId.saveUser(userResponse);


                //making sure fragment is still there
                if (getActivity() != null)
                {
                    Intent result = new Intent();
                    result.putExtra(Intent.EXTRA_TEXT, gson.toJson(userResponse));
					((IAuthActivity)getActivity()).setAuthSucceeded(result);
                }
            }

            @Override
            public void failure(final ErrorResponse errorResponse, RetrofitError error)
            {
                if (getActivity() != null)
                {
                    PhoneId phoneId = PhoneId.getInstance();
                    if (phoneId != null)
                        phoneId.handleServerError(errorResponse, error);

                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mSpinner.setVisibility(View.GONE);
							mStatusIcon.setImageResource(android.R.drawable.ic_notification_clear_all);
							mStatusIcon.setVisibility(View.VISIBLE);
							mErrorBanner.setVisibility(View.GONE);
							mHeader.setVisibility(View.VISIBLE);
							mErrorBanner.setText(R.string.phid_error);
                            hideSoftwareKeyboard();
							mMessage.setText(errorResponse.getMessage());
                            /*ErrorDialog.newInstance(0, errorResponse.getMessage()).show(
                                getActivity().getSupportFragmentManager(), ErrorDialog.TAG);*/
                        }
                    });

                }
            }

        });
    }
}
