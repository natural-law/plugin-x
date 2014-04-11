/****************************************************************************
Copyright (c) 2012-2013 cocos2d-x.org

http://www.cocos2d-x.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 ****************************************************************************/
package org.cocos2dx.plugin;

import java.net.URLDecoder;
import java.util.Hashtable;

import org.cocos2dx.libSocialTwitter.R;
import org.cocos2dx.plugin.TwitterApp.TwDialogListener;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class ShareTwitter implements InterfaceShare {

	private static final String LOG_TAG = "ShareTwitter";
	private static Activity mContext = null;
	private static InterfaceShare mShareAdapter = null;
	protected static boolean bDebug = false;
	
	private static String CONSUMER_KEY = "";
	private static String CONSUMER_SECRET = "";
	private static String TWITPIC_API_KEY = ""; //support twitpic

	private static TwitterApp mTwitter = null;
	private static boolean isInitialized = false;
	private static Hashtable<String, String> mShareInfo = null;

	private final static String KEY_CONSUMER_KEY = "TwitterKey";
	private final static String KEY_CONSUMER_SECRET = "TwitterSecret";
	private final static String KEY_TWITPIC_API_KEY = "TwitPicAPIKey";

	public static String KEY_TEXT = "SharedText";
	public static String KEY_IMAGE_PATH = "SharedImagePath";

	private static boolean encode_f = false;
	
	protected static void LogE(String msg, Exception e) {
		Log.e(LOG_TAG, msg, e);
		e.printStackTrace();
	}

	protected static void LogD(String msg) {
		if (bDebug) {
			Log.d(LOG_TAG, msg);
		}
	}

	public ShareTwitter(Context context) {
		mContext = (Activity) context;
		mShareAdapter = this;
		

		Resources mResource = mContext.getResources();
		CONSUMER_KEY = mResource.getString(R.string.consumar_key);
		CONSUMER_SECRET = mResource.getString(R.string.consumar_secret);
		TWITPIC_API_KEY = mResource.getString(R.string.twitpic_api_key);
		LogD("key : " + CONSUMER_KEY);
		LogD("secret : " + CONSUMER_SECRET);
		LogD("twitpic : " + TWITPIC_API_KEY);
	}

	@Override
	public void configDeveloperInfo(Hashtable<String, String> cpInfo) {
		LogD("initDeveloperInfo invoked " + cpInfo.toString());
		try {
			if (isInitialized) {
				return;
			}
			isInitialized = true;
			
			String _CONSUMER_KEY= cpInfo.get(KEY_CONSUMER_KEY);
			if(_CONSUMER_KEY != null){
				CONSUMER_KEY = _CONSUMER_KEY;
			}
			String _CONSUMER_SECRET= cpInfo.get(KEY_CONSUMER_SECRET);
			if(_CONSUMER_SECRET != null){
				CONSUMER_SECRET = _CONSUMER_SECRET;
			}

			String _TWITPIC_API_KEY= cpInfo.get(KEY_TWITPIC_API_KEY);
			if(_TWITPIC_API_KEY != null){
				TWITPIC_API_KEY = _TWITPIC_API_KEY;
			}
			
			LogD("key : " + CONSUMER_KEY);
			LogD("secret : " + CONSUMER_SECRET);
			LogD("twitpic : " + TWITPIC_API_KEY);
			
			//support encode
			try {
				encode_f = Boolean.valueOf(cpInfo.get("encode_flag"));
			} catch (Exception e) {
				encode_f = false;
			}

			
			PluginWrapper.runOnMainThread(new Runnable() {

				@Override
				public void run() {
					mTwitter = new TwitterApp(PluginWrapper.getContext(),
							ShareTwitter.CONSUMER_KEY,
							ShareTwitter.CONSUMER_SECRET);
					mTwitter.setListener(mTwLoginDialogListener);
				}
			});
		} catch (Exception e) {
			LogE("Developer info is wrong!", e);
		}

	}

	@Override
	public void share(Hashtable<String, String> info) {
		LogD("share invoked " + info.toString());
		mShareInfo = info;
		if (!networkReachable()) {
			shareResult(ShareWrapper.SHARERESULT_FAIL, "Network error!");
			return;
		}

		if (!isInitialized) {
			shareResult(ShareWrapper.SHARERESULT_FAIL, "Initialize failed!");
			return;
		}

		// need login
		if (!mTwitter.hasAccessToken()) {
			PluginWrapper.runOnMainThread(new Runnable() {

				@Override
				public void run() {
					mTwitter.authorize();
				}
			});

			return;
		}

		PluginWrapper.runOnMainThread(new Runnable() {

			@Override
			public void run() {
				ShareTwitter.sendToTwitter();
			}
		});
	}

	@Override
	public void setDebugMode(boolean debug) {
		bDebug = debug;
	}

	@Override
	public String getSDKVersion() {
		return "4.0.1";
	}

	private boolean networkReachable() {
		boolean bRet = false;
		try {
			ConnectivityManager conn = (ConnectivityManager) mContext
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = conn.getActiveNetworkInfo();
			bRet = (null == netInfo) ? false : netInfo.isAvailable();
		} catch (Exception e) {
			LogE("Fail to check network status", e);
		}
		LogD("NetWork reachable : " + bRet);
		return bRet;
	}

	private static void shareResult(int ret, String msg) {
		ShareWrapper.onShareResult(mShareAdapter, ret, msg);
		LogD("ShareTwitter result : " + ret + " msg : " + msg);
	}

	private static final TwDialogListener mTwLoginDialogListener = new TwDialogListener() {

		@Override
		public void onError(int flag, String value) {
			LogD("Twitter connection failed!");
			shareResult(ShareWrapper.SHARERESULT_FAIL, value);
		}

		@Override
		public void onComplete(String value) {
			ShareTwitter.sendToTwitter();
		}
	};

	private static void sendToTwitter() {
		String text = mShareInfo.get(KEY_TEXT);
		String imagePath = mShareInfo.get(KEY_IMAGE_PATH);
		try {
			if(encode_f){
				try{
					text = URLDecoder.decode(text,"utf-8");
				}
				catch(Exception ex){}
			}
			
			if (imagePath != null && imagePath.length() > 0) {
				if (TWITPIC_API_KEY != null && !"".equals(TWITPIC_API_KEY)) {
					String url = mTwitter.updateStatusTwitPic(
							text,imagePath, TWITPIC_API_KEY);
					mTwitter.updateStatus(text + " " + url);
				}
				else{
					mTwitter.updateStatus(text, imagePath);
				}
			} else {
				mTwitter.updateStatus(text);
			}
			LogD("Posted to Twitter!");
			shareResult(ShareWrapper.SHARERESULT_SUCCESS, "Share succeed!");
		} catch (Exception e) {
			LogD("Post to Twitter failed!");
			shareResult(ShareWrapper.SHARERESULT_FAIL, "Unknown error!");
			e.printStackTrace();
		}
	}

	@Override
	public String getPluginVersion() {
		return "0.2.0";
	}
}
