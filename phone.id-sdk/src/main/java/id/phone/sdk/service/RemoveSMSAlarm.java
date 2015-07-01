package id.phone.sdk.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/**
 * Created by dennis on 18.05.15.
 */
public class RemoveSMSAlarm extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
		wl.acquire();

		final Intent serviceIntent = new Intent(context, FirstRunService.class);
		serviceIntent.setAction(FirstRunService.ACTION_REMOVE_SMS);
		context.startService(serviceIntent);

		wl.release();
	}

	public static void setAlarm(Context context, long alarmDelay)
	{
		AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, RemoveSMSAlarm.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + alarmDelay, pi);
	}

	public static void cancelAlarm(Context context)
	{
		Intent intent = new Intent(context, RemoveSMSAlarm.class);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}
}
