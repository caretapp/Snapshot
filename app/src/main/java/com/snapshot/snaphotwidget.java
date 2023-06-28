package com.snapshot;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class snaphotwidget extends Service {
	private WindowManager windowManager;
	public View mainView;
	private WindowManager.LayoutParams params;
	public Handler handler;

	public static boolean success;

	@Override
	public void onCreate() {
		super.onCreate();
		success = false;
		if (Settings.canDrawOverlays(this)) {
			handler = new Handler(Looper.getMainLooper());
			mainView = LayoutInflater.from(this).inflate(R.layout.widget, null);

			params = new WindowManager.LayoutParams(
					WindowManager.LayoutParams.WRAP_CONTENT,
					WindowManager.LayoutParams.WRAP_CONTENT,
					WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
					WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
					PixelFormat.TRANSLUCENT);
			params.x = 25;
			params.y = 25;

			windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
			windowManager.addView(mainView, params);

			TextView hintText = mainView.findViewById(R.id.widget_text);
			hintText.setOnClickListener(v->{
				v.setVisibility(View.GONE);
			});

			mainView.findViewById(R.id.widget_root).setOnTouchListener(new View.OnTouchListener() {
				private int taps = 0;
				private int initialX;
				private int initialY;
				private float initialTouchX;
				private float initialTouchY;
				@SuppressLint("ClickableViewAccessibility")
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							//remember the initial position.
							initialX = params.x;
							initialY = params.y;
							initialTouchX = event.getRawX();
							initialTouchY = event.getRawY();
							return true;
						case MotionEvent.ACTION_UP:
							if ( hintText.getVisibility() == View.GONE ) {
								int diffX = (int) (event.getRawY() - initialTouchY);
								int diffY = (int) (event.getRawX() - initialTouchX);
								utils.Echo( "", "on up: " + taps + " " + diffX + " " + diffY, 1);
								if ( diffX == 0 && diffY == 0 ) {
									taps++;
									if (taps >= 2) {
										utils.Echo("", "on up: taps >= 2 " + taps, 1);
										mainView.post(() -> {
											mainView.setVisibility(View.GONE);
										});
										utils.Echo("", "on up: start", 1);
										new Thread(() -> {
											int attempts = 0;
											int max = 30 * 1000;
											List<ArrayList<String>> queryMap = new ArrayList<>();
											while (attempts < max) {
												attempts += 10;
												try {
													if (screenReader.queryQueue.peek() != null ) {
														queryMap.addAll(
																Objects.requireNonNull(screenReader.queryQueue.poll()) );
														break;
													} else {
														try {
															Thread.sleep(10);
														} catch (Exception ignore) {
														}
													}
												} catch (Exception ignore) {
													try {
														Thread.sleep(10);
													} catch (Exception ignore1) {
													}
												}
												if (attempts == 15000) {
													utils.Echo(mainView.getContext(), "", "timing out in " + (max - attempts) / 1000 + " seconds", 2, false);
												}
											}
											if (attempts <= max) {
												if ( queryMap.size() > 0 ) {
													//write contents
													if ( home.folderUri != null ) {
														DocumentFile f = DocumentFile.fromTreeUri(mainView.getContext(), home.folderUri);
														assert f != null;
														f.createFile("*/text", "snapshot.txt");
														DocumentFile f1 = f.findFile("snapshot.txt");
														assert f1 != null;
														for ( ArrayList<String> arr : queryMap ) {
															utils.writeToUriFile( mainView.getContext(), f1.getUri(), String.join("\n", arr)+"\n---\n" );
														}
														screenReader.instance.get().takeScreenShot();
														success = true;
													}
												}
											} else {
												utils.Echo(mainView.getContext(), "", "Snapshot timed out!", 2, true);
											}
										}).start();
									}
								}
							} else {
								utils.Echo(mainView.getContext(), "", "close the text first!", 2, true);
							}
							return true;
						case MotionEvent.ACTION_MOVE:
							params.x = initialX + (int) (event.getRawX() - initialTouchX);
							params.y = initialY + (int) (event.getRawY() - initialTouchY);
							windowManager.updateViewLayout(mainView, params);
							taps = 0;
							return true;
					}
					return false;
				}
			});
		} else {
			stopSelf();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mainView != null) {
			windowManager.removeView(mainView);
		}
	}

}
