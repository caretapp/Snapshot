package com.snapshot;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import es.dmoral.toasty.Toasty;

public class utils {
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
	public static void Echo(String c, String m, int level ) {
		switch (level) {
			case 1:
				Log.i(c, m);
				break;
			case 2:
				Log.d(c, m);
				break;
			default:
				Log.e(c, m);
				break;
		}
	}

	public static boolean writeToUriFile(Context context, Uri f, String data){
		try {
			OutputStream outputStream = context.getContentResolver().openOutputStream( f, "wa" );
			outputStream.write(data.getBytes());
			outputStream.close();
			utils.Echo("writeToUriFile", "Wrote data" + data.length() + " to " + f.getPath(), 1);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			utils.Echo(context, "writeToUriFile", "File write failed: " + e, 3, true);
			Writer writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			String s = writer.toString();
			home.logInter.write( s );
		}
		return false;
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
			} else {
				//setting not found
				utils.Echo(context, "isAccessibilitySettingsOn", "Setting not found or supported!", 3, true);
			}
		}
		return false;
	}
}
