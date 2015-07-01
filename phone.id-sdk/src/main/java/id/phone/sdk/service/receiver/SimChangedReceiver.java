package id.phone.sdk.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import id.phone.sdk.Commons;
import id.phone.sdk.PhoneId;
import id.phone.sdk.rest.response.UserResponse;

import static id.phone.sdk.utils.LogUtils.LOGV;

/**
 * Created by dennis on 28.05.15.
 */
public class SimChangedReceiver extends BroadcastReceiver
{
	public static final String TAG = SimChangedReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent)
	{
		LOGV(TAG, "onReceive");
		if (intent != null)
		{
			Log.v(TAG, intent.toString());

			boolean doCheck = true;

			if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction()))
			{
				if (Commons.isAirplaneModeOn(context)) doCheck = false;
			}

			PhoneId phoneId = PhoneId.getInstance();
			if (doCheck && phoneId != null && phoneId.isLoggedIn())
			{
				UserResponse user = phoneId.getUser();
				if (phoneId.getSimCardManager().checkLogoutCondition(user.getPhoneNumber()))
					phoneId.logOut();
			}
		}
	}
}
