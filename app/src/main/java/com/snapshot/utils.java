package com.snapshot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import es.dmoral.toasty.Toasty;

public class utils {
	public static BasicNotification basicNotification;
	public static void Echo(Context context, String c, String m, int level, boolean t ) {
		switch (level) {
			case 1:
				Log.i(c, m);
				if ( t ) {
					try {
						new Handler(Looper.getMainLooper()).post(() -> {
							Toasty.custom(context, m, R.drawable.baseline_launch_24, R.color.optional, Toast.LENGTH_LONG, true, true).show();
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				break;
			case 2:
				Log.d(c, m);
				if ( t ) {
					try {
						new Handler(Looper.getMainLooper()).post(()-> {
							Toasty.custom(context, m, R.drawable.baseline_launch_24, R.color.toast_error, Toast.LENGTH_LONG, true, true).show();
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				break;
			default:
				Log.e(c, m);
				if ( t ) {
					try {
						new Handler(Looper.getMainLooper()).post(()-> {
							Toasty.custom(context, m, R.drawable.baseline_launch_24, R.color.toast_error, Toast.LENGTH_LONG, true, true).show();
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				break;
		}
	}
	public static void Echo(String m, int level ) {
		switch (level) {
			case 1:
				Log.i("Snapshot", m);
				break;
			case 2:
				Log.d("Snapshot", m);
				break;
			default:
				Log.e("Snapshot", m);
				break;
		}
	}

	public static int rngInt(int min, int max) {
		return (int) ((Math.random() * (max - min)) + min);
	}

	public static class BasicNotification {
		public NotificationManager mNotificationManager;
		public NotificationCompat.Builder mBuilder;

		public BasicNotification() {
		}

		public void cancel( int id ) {
			if ( mNotificationManager != null ) {
				mNotificationManager.cancel( id );
			}
		}

		public void cancelAll() {
			if ( mNotificationManager != null ) {
				mNotificationManager.cancelAll();
			}
		}

		public Notification getNotification(){
			return mBuilder.build();
		}

		public void build(Context context, String title, String subTitle, String text, String channel, boolean toGroup, boolean foreground, int importance) {
			try {
				mBuilder = new NotificationCompat.Builder(context, channel);
				if (foreground) {
					mBuilder.setOngoing(true);
				} else {
					mBuilder.setAutoCancel(true);
				}
				NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
				bigText.setSummaryText(title);
				bigText.setBigContentTitle(subTitle);
				bigText.bigText(text);
				mBuilder.setStyle(bigText);
				mBuilder.setSmallIcon(R.drawable.baseline_launch_24);
				mBuilder.setContentTitle(title);
				if (toGroup) {
					mBuilder.setGroup(channel);
				}
				mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				NotificationChannel mChannel = new NotificationChannel(channel, String.format(Locale.getDefault(), "SnapShot-%s", channel), NotificationManager.IMPORTANCE_HIGH);
				mChannel.setImportance(importance); //NotificationManager.IMPORTANCE_HIGH
				mNotificationManager.createNotificationChannel(mChannel);
				mBuilder.setChannelId(channel);

				Intent notificationIntent = new Intent(context, home.class);
				notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_SINGLE_TOP);
				PendingIntent intent = PendingIntent.getActivity(context, 0,
						notificationIntent, PendingIntent.FLAG_IMMUTABLE);
				mBuilder.setContentIntent( intent );
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		public void notify( Context context, int id ) {
			try {
				mNotificationManager.notify(id, mBuilder.build());
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
	}

	public static boolean isAccessibilitySettingsOn(Context context) {
		int accessibilityEnabled = 0;
		final String service = context.getPackageName() + "/" + screenReader.class.getCanonicalName();
		try {
			accessibilityEnabled = Settings.Secure.getInt(
					context.getContentResolver(),
					android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
		} catch (Settings.SettingNotFoundException e) {
			e.printStackTrace();
			Writer writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			String s = writer.toString();
			home.logInter.write( s );
		}
		TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
		if ( accessibilityEnabled == 1 ) {
			//is enabled
			String settingValue = Settings.Secure.getString(
					context.getContentResolver(),
					Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
			if (settingValue != null) {
				mStringColonSplitter.setString(settingValue);
				while (mStringColonSplitter.hasNext()) {
					String accessibilityService = mStringColonSplitter.next();
					if (accessibilityService.equalsIgnoreCase(service)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
