package id.phone.sdk.ui.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;

import com.flurry.android.FlurryAgent;

import id.phone.sdk.PhoneId;
import id.phone.sdk.R;
import id.phone.sdk.ui.activity.LoginActivity;

/**
 * Created by azubchenko on 4/2/15.
 * Updated by Dennis Gubsky
 */
public class LoginButton extends Button
{
    public static final int LOGIN_REQUEST_CODE = 10000;

    private static final String TAG = LoginButton.class.getSimpleName();
    private boolean loggedIn;

    public LoginButton(Context context) {
        super(context);
        init();
    }

    public LoginButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LoginButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LoginButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void updateLoginState()
    {
        if (getContext() instanceof Activity)
        {
            ((Activity)getContext()).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    loggedIn = PhoneId.getInstance() != null && PhoneId.getInstance().isLoggedIn();
                    setText(loggedIn ? R.string.phid_LOGGED_IN : R.string.phid_NOT_LOGGED_IN);
                }
            });
        }
    }

    private BroadcastReceiver loginEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            updateLoginState();
        }
    };

    private void init()
    {
        updateLoginState();

        setBackgroundResource(R.drawable.phid_login_btn_bg);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			setCompoundDrawablesWithIntrinsicBounds(
				getResources().getDrawable(R.drawable.phid_icon_phone, null)
				, null, null, null);
		else
			setCompoundDrawablesWithIntrinsicBounds(
				getResources().getDrawable(R.drawable.phid_icon_phone)
				, null, null, null);
        setTextColor(getResources().getColor(R.color.phid_button_text));
		setPadding((int) getResources().getDimension(R.dimen.phid_big_margin), 0
			, (int) getResources().getDimension(R.dimen.phid_padding_small), 0);
		setGravity(Gravity.CENTER);

        final IntentFilter filter = new IntentFilter(PhoneId.ACTION_LOGGED_IN);
        filter.addAction(PhoneId.ACTION_LOGGED_OUT);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(loginEventReceiver, filter);

        setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                FlurryAgent.logEvent("LOGIN BUTTON CLICK");
                PhoneId phoneId;
                loggedIn =
                    (phoneId = PhoneId.getInstance()) != null && PhoneId.getInstance().isLoggedIn();

                if (loggedIn)
                {
                    phoneId.logOut();
                    updateLoginState();
                }
                else
                {
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    ((Activity) getContext()).startActivityForResult(intent, LOGIN_REQUEST_CODE);
                }
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        updateLoginState();
    }

}
