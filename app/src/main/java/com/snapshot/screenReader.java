package com.snapshot;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class screenReader extends AccessibilityService {
	public static WeakReference<screenReader> instance;

	private final ArrayList<String> allowedPkgs = new ArrayList<>(
			Arrays.asList("com.doordash.driverapp", "com.grubhub.driver",
					"com.ubercab.driver", "com.instacart.shopper"));
	public static ConcurrentLinkedQueue<List<ArrayList<String>>> queryQueue = new ConcurrentLinkedQueue<>();
	private final List<ArrayList<String>> queryMap = new ArrayList<>();
	private CompletableFuture<Void> processFuture = new CompletableFuture<>();

	public static void stopThis(){
		if ( screenReader.instance != null ) {
			screenReader.instance.get().stopSelf();
		}
	}

	public void takeScreenShot() {
		performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
	}

	public void nodeToArray(AccessibilityNodeInfo node) {
		int count = node.getChildCount();
		for (int i = 0; i < count; i++) {
			AccessibilityNodeInfo child = node.getChild(i);
			if (child != null) {
				ArrayList<String> childAttrs = new ArrayList<>();
				if ( child.getPackageName() != null ) { childAttrs.add( child.getPackageName().toString() ); } else { childAttrs.add(""); } //package
				if ( child.getClassName() != null ) { childAttrs.add( child.getClassName().toString() ); } else { childAttrs.add(""); } //class
				if ( child.getViewIdResourceName() != null ) {
					childAttrs.add( child.getViewIdResourceName() );
				} else {
					childAttrs.add( "unknown" );
				} //res
				if ( child.getText() != null ) { childAttrs.add( child.getText().toString() ); } else { childAttrs.add(""); } //text
				if ( child.getContentDescription() != null ) { childAttrs.add( child.getContentDescription().toString() ); } else { childAttrs.add(""); } //desc
				Rect r = new Rect();
				child.getBoundsInScreen(r);
				childAttrs.add( r.left + "/" + r.top + "/" + r.right + "/" + r.bottom ); //bounds
				queryMap.add( childAttrs );
				nodeToArray(child);
				child.recycle();
			}
		}
	}

	private void processQuery( AccessibilityEvent event ) {
		int e = event.getEventType();
		if ( e == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || e == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ) {
			AccessibilityNodeInfo finalNode = getRootInActiveWindow();
			if (finalNode != null) {
				CharSequence pkg = finalNode.getPackageName();
				if ( pkg != null && allowedPkgs.contains( pkg.toString() ) ) {
					if ( processFuture.isDone() ) {
						processFuture = CompletableFuture.runAsync(() -> {
							queryMap.clear();
							nodeToArray(finalNode);
							if ( queryQueue.peek() != null ) {
								queryQueue.poll();
							}
							queryQueue.offer( queryMap );
							finalNode.recycle();
						});
					}
				}
			}
		}
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		if (event != null) {
			AccessibilityNodeInfo source = event.getSource();
			if (source != null) {
				CharSequence sourcePackage = source.getPackageName();
				if (sourcePackage != null && !sourcePackage.equals("null")) {
					processQuery( event );
				}
			}
		}
	}

	@Override
	public void onInterrupt() {
	}

	@Override
	public boolean onGesture(int gestureId) {
		return false;
	}

	@Override
	public void onServiceConnected() {
		super.onServiceConnected();
		instance = new WeakReference<>(this);
		processFuture = CompletableFuture.runAsync(() -> {});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public int onStartCommand (Intent intent, int flags, int startId) {
		return START_NOT_STICKY;
	}
}

