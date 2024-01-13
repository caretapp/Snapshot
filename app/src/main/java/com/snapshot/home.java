package com.snapshot;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.joanfuentes.hintcase.HintCase;
import com.joanfuentes.hintcase.RectangularShape;
import com.joanfuentes.hintcaseassets.hintcontentholders.SimpleHintContentHolder;
import com.joanfuentes.hintcaseassets.shapeanimators.RevealRectangularShapeAnimator;
import com.joanfuentes.hintcaseassets.shapeanimators.UnrevealRectangularShapeAnimator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class home extends AppCompatActivity {
	private TextView accText;
	private TextView logText;
	private Context context;

	public interface LogInter {
		void write(String msg);
	}
	public interface SnapInter {
		void add(List<ArrayList<String>> snapContainer);
	}
	public static SnapInter snapInter;
	public static LogInter logInter;
	private final ActivityResultLauncher<Intent> accPermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
				//doesn't return a code
				if ( utils.isAccessibilitySettingsOn( context )){
					accText.setText( context.getResources().getString( R.string.well_done ) );
					accText.setEnabled(false);
					home.logInter.write( "granted accessibility permissions" );
					startForegroundService(new Intent(context, screenReader.class));
				} else {
					stopService(new Intent(context, screenReader.class));
				}
			});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = getApplicationContext();
		utils.basicNotification = new utils.BasicNotification();

		accText = findViewById(R.id.acc_text);
		logText = findViewById(R.id.log_text);
		EditText findThisText = findViewById(R.id.find_this_edit_text);
		LinearLayout snapContainer = findViewById(R.id.snap_container);
		ArrayList<String> logData = new ArrayList<>();
		logInter = msg -> {
			logData.add( new Date() + ": " + msg );
			logText.post(()->{
				logText.setText( String.join("\n", logData) );
			});
		};
		accText.setOnClickListener(v -> {
			accPermissionLauncher.launch( new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS) );
		});
		logInter.write("This is a log. Text will be written here as Snapshot runs to explain things. Tap this text to copy it to your clipboard if needed for pasting.");
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
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		LinearLayout rowOne = new LinearLayout(context);
		rowOne.setLayoutParams( params );
		rowOne.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout rowTwo = new LinearLayout(context);
		rowTwo.setLayoutParams( params );
		rowTwo.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout rowThree = new LinearLayout(context);
		rowThree.setLayoutParams( params );
		rowThree.setOrientation(LinearLayout.HORIZONTAL);

		snapContainer.addView(rowOne);
		snapContainer.addView(rowTwo);
		snapContainer.addView(rowThree);

		LinearLayout.LayoutParams snapParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 128, 1f);
		snapParams.setMargins(8,8,8,8);
		ArrayList<CardView> snapViews = new ArrayList<>();
		int[] colors = new int[]{Color.BLUE, Color.GREEN, Color.GRAY, Color.LTGRAY, Color.RED, Color.CYAN};
		snapInter = new SnapInter() {
			@Override
			public void add(List<ArrayList<String>> snapContainer) {
				if ( snapViews.size() > 12 ) {
					snapViews.remove(snapViews.size()-1);
				}
				CardView newSnapView = new CardView(context);
				newSnapView.setLayoutParams( snapParams );
				newSnapView.setCardBackgroundColor(utils.rngInt(0, colors[colors.length-1]));
				int matches = 0;
				StringBuilder snapTextBuilder = new StringBuilder();
				snapTextBuilder.append( "~start" );
				if ( findThisText.getText() != null && ! findThisText.getText().toString().isEmpty() ) {
					String findStr = findThisText.getText().toString().toLowerCase();
					for ( ArrayList<String> aStr : snapContainer ) {
						for ( int x=0; x<aStr.size(); x++ ) {
							String s = aStr.get(x);
							if ( s.toLowerCase().contains( findStr ) ) {
								matches++;
							}
							if ( x < aStr.size() - 1 ) {
								snapTextBuilder.append(s).append("~");
							} else {
								snapTextBuilder.append( s );
							}
						}
						snapTextBuilder.append("\n");
					}
				}
				snapTextBuilder.append( "~end" );
				TextView matchCountView = new TextView( context );
				matchCountView.setTextSize(20f);
				matchCountView.setTextColor( Color.WHITE );
				matchCountView.setText(String.valueOf( matches ));
				newSnapView.addView( matchCountView, LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT );
				newSnapView.setRadius(16f);
				newSnapView.setElevation(0f);
				newSnapView.setOnClickListener(v->{
					ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("snap data", snapTextBuilder.toString());
					clipboard.setPrimaryClip(clip);
					utils.Echo(context, "", "Snap copied to your clipboard!", 1, true);
				});
				snapViews.add(0, newSnapView);
				//
				new Handler(getMainLooper()).post(() -> {
					rowOne.removeAllViews();
					rowTwo.removeAllViews();
					rowThree.removeAllViews();
					for (int i = 0; i < snapViews.size(); i++) {
						LinearLayout targetLayout;
						if (rowOne.getChildCount() < 4) {
							targetLayout = rowOne;
						} else if (rowTwo.getChildCount() < 4) {
							targetLayout = rowTwo;
						} else {
							targetLayout = rowThree;
						}
						targetLayout.addView(snapViews.get(i));
					}
				});
			}
		};

		new Handler(getMainLooper()).postDelayed(() -> {
			SimpleHintContentHolder hintBlock1 = new SimpleHintContentHolder.Builder(context)
					.setContentTitle(String.format(Locale.getDefault(), "%s", "Hints: Step #1"))
					.setContentText(String.format(Locale.getDefault(), "%s", "Enable the Accessibility Service.\n\n" +
							"If you're running Android 13+ do App Info (for this app) > Tap 3 dots in the top right > Enable Restricted Settings > Allow"))
					.setTitleStyle(R.style.hints_title)
					.setContentStyle(R.style.hints_content)
					.build();
			new HintCase(accText.getRootView())
					.setTarget(accText, new RectangularShape(), HintCase.TARGET_IS_NOT_CLICKABLE)
					.setShapeAnimators(new RevealRectangularShapeAnimator(),
							new UnrevealRectangularShapeAnimator())
					.setHintBlock(hintBlock1)
					.setOnClosedListener(() -> {
						SimpleHintContentHolder hintBlock2 = new SimpleHintContentHolder.Builder(context)
								.setContentTitle(String.format(Locale.getDefault(), "%s", "Step #2"))
								.setContentText(String.format(Locale.getDefault(), "%s", "Enter some text you wish to find on screen here.\n i.e \"2 shop\" to find multiple batch offers etc."))
								.setTitleStyle(R.style.hints_title)
								.setContentStyle(R.style.hints_content)
								.build();
						new HintCase(findThisText.getRootView())
								.setTarget(findThisText, new RectangularShape(), HintCase.TARGET_IS_NOT_CLICKABLE)
								.setShapeAnimators(new RevealRectangularShapeAnimator(),
										new UnrevealRectangularShapeAnimator())
								.setHintBlock(hintBlock2)
								.setOnClosedListener(() -> {
									SimpleHintContentHolder hintBlock3 = new SimpleHintContentHolder.Builder(context)
											.setContentTitle(String.format(Locale.getDefault(), "%s", "Step #3"))
											.setContentText(String.format(Locale.getDefault(), "%s", "Open the delivery app and find what you're looking for. Then return to SnapShot."))
											.setTitleStyle(R.style.hints_title)
											.setContentStyle(R.style.hints_content)
											.build();
									new HintCase(snapContainer.getRootView())
											.setShapeAnimators(new RevealRectangularShapeAnimator(),
													new UnrevealRectangularShapeAnimator())
											.setHintBlock(hintBlock3)
											.setOnClosedListener(() -> {
												SimpleHintContentHolder hintBlock4 = new SimpleHintContentHolder.Builder(context)
														.setContentTitle(String.format(Locale.getDefault(), "%s", "Afterwards..."))
														.setContentText(String.format(Locale.getDefault(), "%s", "Snaps will appear here. Tap one to copy it to your clipboard for pasting.\n\nThe number shown is the number of text matches found in the Snap given the text you have entered above to find."))
														.setTitleStyle(R.style.hints_title)
														.setContentStyle(R.style.hints_content)
														.build();
												new HintCase(snapContainer.getRootView())
														.setTarget(snapContainer, new RectangularShape(), HintCase.TARGET_IS_NOT_CLICKABLE)
														.setShapeAnimators(new RevealRectangularShapeAnimator(),
																new UnrevealRectangularShapeAnimator())
														.setHintBlock(hintBlock4)
														.setOnClosedListener(() -> {

														})
														.show();
											})
											.show();
								})
								.show();
					})
					.show();
		}, 1000);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		screenReader.stopThis();
	}
}