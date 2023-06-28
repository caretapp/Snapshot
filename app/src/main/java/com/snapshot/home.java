package com.snapshot;

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


public class home extends AppCompatActivity {
	public static Uri folderUri;
	private TextView accText;
	private TextView drawText;
	private TextView folderText;
	private TextView logText;
	private Context context;
	private final ArrayList<Boolean> perms = new ArrayList<>(Arrays.asList(false, false, false));
	private final ActivityResultLauncher<Intent> drawOverPermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
				if (Settings.canDrawOverlays(this)) {
					drawText.setText( context.getResources().getString( R.string.nice ) );
					drawText.setEnabled(false);
					perms.set(0, true);
					if ( ! perms.contains( false ) ) {
						utils.Echo(context, "", "all set! use the widget", 1, true);
						startService(new Intent(context, snaphotwidget.class));
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
					if ( ! perms.contains( false ) ) {
						utils.Echo(context, "", "all set! use the widget", 1, true);
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
					if ( ! perms.contains( false ) ) {
						utils.Echo(context, "", "all set! use the widget", 1, true);
						startService(new Intent(context, snaphotwidget.class));
					} else {
						utils.Echo(context, "", "enable the other permissions!", 2, true);
					}
				} else {
					utils.Echo(context, "", "no folder was selected!", 2, true);
				}
			});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = getApplicationContext();

		accText = findViewById(R.id.acc_text);
		drawText = findViewById(R.id.acc_draw);
		folderText = findViewById(R.id.acc_folder);
		logText = findViewById(R.id.log_text);

		accText.setOnClickListener(v -> {
			accPermissionLauncher.launch( new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS) );
		});
		drawText.setOnClickListener(v -> {
			drawOverPermissionLauncher.launch( new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION) );
		});
		folderText.setOnClickListener(v->{
			folderResultLauncher.launch(Uri.parse("content://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
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