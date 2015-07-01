package id.phone.sdk.auth;

import android.content.Intent;
import android.os.Handler;

/**
 * Created by dennis on 19.05.15.
 */
public interface IAuthActivity
{
	Handler getHandler();
	void setAuthSucceeded(Intent result);
	void setAuthFailed(String errorMessage);
}
