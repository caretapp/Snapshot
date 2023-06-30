package com.snapshot;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
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

			CardView cardView = mainView.findViewById( R.id.widget_card );

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
							if ( mainView.getAlpha() != 0.5f ) {
								mainView.post(() -> {
									mainView.setAlpha(0.5f);
								});
							}
							return true;
						case MotionEvent.ACTION_UP:
							if ( mainView.getAlpha() != 1f ) {
								mainView.post(() -> {
									mainView.setAlpha(1f);
								});
							}
							if ( hintText.getVisibility() == View.GONE ) {
								int diffX = (int) (event.getRawY() - initialTouchY);
								int diffY = (int) (event.getRawX() - initialTouchX);
								utils.Echo( "", "on up: " + taps + " " + diffX + " " + diffY, 1);
								if ( diffX == 0 && diffY == 0 ) {
									taps++;
									if (taps >= 2) {
										Animation rotation = AnimationUtils.loadAnimation(mainView.getContext(), R.anim.rotate);
										rotation.setFillAfter(true);

										utils.Echo(mainView.getContext(), "", "Running! Please wait...", 1, true);
										home.logInter.write( "On double tap of widget" );
										mainView.post(() -> {
											mainView.setEnabled(false);
											cardView.setCardBackgroundColor(
													ContextCompat.getColor(mainView.getContext(), R.color.grey));
											mainView.clearAnimation();
											mainView.startAnimation(rotation);
										});
										new Thread(() -> {
											int attempts = 0;
											int max = 10 * 1000;
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
												if (attempts == 5000) {
													utils.Echo(mainView.getContext(), "", "Timing out in " + (max - attempts) / 1000 + " seconds", 2, true);
												}
											}
											if (attempts <= max) {
												if ( queryMap.size() > 0 ) {
													home.logInter.write( "Found " + queryMap.size() + " elements" );
													//write contents
													if ( home.folderUri != null ) {
														DocumentFile f = DocumentFile.fromTreeUri(mainView.getContext(), home.folderUri);
														assert f != null;
														f.createFile("*/text", "snapshot.txt");
														DocumentFile f1 = f.findFile("snapshot.txt");
														assert f1 != null;
														for ( ArrayList<String> arr : queryMap ) {
															if ( ! utils.writeToUriFile( mainView.getContext(), f1.getUri(), String.join("\n", arr)+"\n---\n" ) ) {
																utils.Echo(mainView.getContext(), "", "An error occurred writing the elements!", 3, true);
																home.logInter.write( "Write errors" );
																break;
															}
														}
														if ( screenReader.instance.get().takeScreenShot() && queryMap.size() > 0 ) {
															success = true;
															mainView.post(() -> {
																cardView.setCardBackgroundColor(
																		ContextCompat.getColor(mainView.getContext(), R.color.optional));
															});
															mainView.postDelayed(() -> mainView.setVisibility(View.GONE), 2000);
															utils.Echo(mainView.getContext(), "", "Success!", 1, true);
															home.logInter.write( "Captured screenshot!" );
														} else {
															utils.Echo(mainView.getContext(), "", "Failed to take a screenshot!", 2, true);
															home.logInter.write( "Screenshot failed!" );
														}
													}
												} else {
													mainView.post(() -> {
														cardView.setCardBackgroundColor(
																ContextCompat.getColor(mainView.getContext(), R.color.yellow_100));
													});
													utils.Echo(mainView.getContext(), "", "Elements was empty!", 2, true);
													home.logInter.write( "Snapshot was empty!" );
												}
											} else {
												utils.Echo(mainView.getContext(), "", "Snapshot timed out!", 2, true);
												mainView.post(() -> {
													cardView.setCardBackgroundColor(
															ContextCompat.getColor(mainView.getContext(), R.color.required));
												});
												home.logInter.write( "Snapshot timed out!" );
											}
											if ( ! success ) {
												mainView.post(() -> {
													cardView.setCardBackgroundColor(
															ContextCompat.getColor(mainView.getContext(), R.color.black));
													mainView.setEnabled(true);
												});
											}
										}).start();
									} else {
										mainView.post(() -> {
											cardView.setCardBackgroundColor(
													ContextCompat.getColor(mainView.getContext(), R.color.black));
										});
									}
									mainView.post(() -> {
										mainView.clearAnimation();
									});
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
