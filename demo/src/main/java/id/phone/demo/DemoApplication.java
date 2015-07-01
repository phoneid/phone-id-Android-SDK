package id.phone.demo;

import android.app.Application;

import id.phone.sdk.PhoneId;


/**
 * Created by azubchenko on 4/6/15.
 */
public class DemoApplication extends Application {

    private PhoneId sPhoneId;

    @Override
    public void onCreate() {
        super.onCreate();

        //init of our sdk
        sPhoneId = PhoneId.getInstance(this, "TestPhoneId");
    }

    @Override
    public void onTerminate()
    {
        super.onTerminate();
        sPhoneId.onDestroy();
    }
}
