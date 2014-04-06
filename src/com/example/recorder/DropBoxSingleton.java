package com.example.recorder;

import android.content.Context;
import android.content.SharedPreferences;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;


public class DropBoxSingleton {
	
	final static private String APP_KEY = "r8nx0zijedcb1v8";
	final static private String APP_SECRET = "kjmv29qa5gwhefl";
	// You don't need to change these, leave them alone.
	final static private String ACCOUNT_PREFS_NAME = "prefs";
	final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
	final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";

	private final String AUDIO_DIR = "/Photos/";

	private DropboxAPI<AndroidAuthSession> mApi = null;
	private Context mContext = null;

	private static DropBoxSingleton instance = null;
	private DropBoxSingleton(Context mContext){
		// Create DropBox API.
		AndroidAuthSession session = buildSession();
		mApi = new DropboxAPI<AndroidAuthSession>(session);
		
		this.mContext = mContext;
	}
	
	
	public DropboxAPI<AndroidAuthSession> getApi(){
		return mApi;
	}
	
	public static DropBoxSingleton get(Context mContext){
		if(instance == null ){
			synchronized(DropBoxSingleton.class){
				if(instance == null){
					instance = new DropBoxSingleton(mContext);
				}
			}
		}
		return instance;
	}
	
	private AndroidAuthSession buildSession() {
		AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

		AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
		loadAuth(session);
		return session;
	}

	private void loadAuth(AndroidAuthSession session) {
		SharedPreferences prefs = mContext.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		String key = prefs.getString(ACCESS_KEY_NAME, null);
		String secret = prefs.getString(ACCESS_SECRET_NAME, null);
		if (key == null || secret == null || key.length() == 0
				|| secret.length() == 0)
			return;

		if (key.equals("oauth2:")) {
			// If the key is set to "oauth2:", then we can assume the token is
			// for OAuth 2.
			session.setOAuth2AccessToken(secret);
		} else {
			// Still support using old OAuth 1 tokens.
			session.setAccessTokenPair(new AccessTokenPair(key, secret));
		}
	}

}
