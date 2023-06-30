package com.snapshot;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;


public class home extends AppCompatActivity {
	public static Uri folderUri;
	private TextView accText;
	private TextView drawText;
	private TextView folderText;
	private TextView logText;
	private Context context;
	public interface LogInter {
		void write(String msg);
	}
	public static LogInter logInter;
	private final ArrayList<Boolean> perms = new ArrayList<>(Arrays.asList(false, false, false));
	private final ActivityResultLauncher<Intent> drawOverPermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
				if (Settings.canDrawOverlays(this)) {
					drawText.setText( context.getResources().getString( R.string.nice ) );
					drawText.setEnabled(false);
					perms.set(0, true);
					home.logInter.write( "granted draw over permissions" );
					if ( ! perms.contains( false ) ) {
						utils.Echo(context, "", "all set! use the widget", 1, true);
						startService(new Intent(context, snaphotwidget.class));
						home.logInter.write( "showing widget" );
					} else {
						utils.Echo(context, "", "enable the other permissions!", 2, true);
					}
				}
			});
	private final ActivityResultLauncher<Intent> accPermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
				//doesn't return a code
				if ( utils.isAccessibilitySettingsOn( context )){
					accText.setText( context.getResources().getString( R.string.well_done ) );
					accText.setEnabled(false);
					perms.set(1, true);
					home.logInter.write( "granted accessibility permissions" );
					if ( ! perms.contains( false ) ) {
						utils.Echo(context, "", "all set! use the widget", 1, true);
						home.logInter.write( "showing widget" );
						startService(new Intent(context, snaphotwidget.class));
					} else {
						utils.Echo(context, "", "enable the other permissions!", 2, true);
					}
				} else {
					stopService(new Intent(context, screenReader.class));
				}
			});
	ActivityResultLauncher<Uri> folderResultLauncher = registerForActivityResult(
			new ActivityResultContracts.OpenDocumentTree(),
			uri -> {
				if (uri != null) {
					ContentResolver resolver = context.getContentResolver();
					resolver.takePersistableUriPermission(uri, (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
					folderUri = uri;
					folderText.setText( context.getResources().getString( R.string.nice ) );
					folderText.setEnabled(false);
					perms.set(2, true);
					home.logInter.write( "granted storage permissions" );
					if ( ! perms.contains( false ) ) {
						utils.Echo(context, "", "all set! use the widget", 1, true);
						startService(new Intent(context, snaphotwidget.class));
						home.logInter.write( "showing widget" );
					} else {
						utils.Echo(context, "", "enable the other permissions!", 2, true);
					}
				} else {
					utils.Echo(context, "", "no folder was selected!", 2, true);
				}
			});

	@SuppressLint("SetTextI18n")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = getApplicationContext();

		accText = findViewById(R.id.acc_text);
		drawText = findViewById(R.id.acc_draw);
		folderText = findViewById(R.id.acc_folder);
		logText = findViewById(R.id.log_text);
		ArrayList<String> logData = new ArrayList<>();
		logInter = msg -> {
			logData.add( new Date() + ": " + msg );
			logText.post(()->{
				logText.setText( String.join("\n", logData) );
			});
		};
		logInter.write("This is a log. Text will be written here as Snapshot runs to explain things. Tap this text to copy it to your clipboard for pasting.");
		logText.setOnClickListener(v->{
			if ( logText.getText() != null ) {
				ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("log data", logText.getText().toString());
				clipboard.setPrimaryClip(clip);
				utils.Echo(context, "", "Log copied to clipboard!", 1, true);
			} else {
				utils.Echo(context, "", "Failed to copy to clipboard!", 3, true);
			}
		});

		accText.setOnClickListener(v -> {
			accPermissionLauncher.launch( new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS) );
		});
		drawText.setOnClickListener(v -> {
			drawOverPermissionLauncher.launch( new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION) );
		});
		folderText.setOnClickListener(v->{
			folderResultLauncher.launch(Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		if ( snaphotwidget.success ) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(this)
					.setTitle(context.getResources().getString(R.string.alert_title))
					.setMessage(context.getResources().getString(R.string.alert_msg))
					.setNegativeButton("Cancel", (dialog2, which) -> dialog2.dismiss());
			dialog.create();
			dialog.show();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		screenReader.stopThis();
	}
}