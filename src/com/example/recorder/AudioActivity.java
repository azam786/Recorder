package com.example.recorder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxInputStream;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.example.recorder.R.string;

public class AudioActivity extends Activity {

	final static private String APP_KEY = "r8nx0zijedcb1v8";
	final static private String APP_SECRET = "kjmv29qa5gwhefl";
	// You don't need to change these, leave them alone.
	final static private String ACCOUNT_PREFS_NAME = "prefs";
	final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
	final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
	private final String AUDIO_DIR = "/Photos/";
	private static final String TAG = LoginActivity.class.getName();
	
	//private final static String AUDIO_FILE_NAME ="audio.3gp";

	// DropboxAPI<AndroidAuthSession> mApi;
	private Context mContext;
	private ProgressDialog mDialog = null;
	private String mPath;
	private boolean mCanceled;
	private Long mFileLen;
	private String mErrorMsg;
	private FileOutputStream mFos;
	private String audioPath;
	//private DropboxAPI<AndroidAuthSession> mApi = null;
	DropboxAPI<AndroidAuthSession> mApi;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 setContentView(R.layout.activity_downloadaudiofile);
		audioPath = getIntent().getStringExtra("audioPath");/*getStringExtra(audioPath);*///getStringExtra("audioPath");

		// Get DropBox API.
		//mContext = this;
		//mApi = DropBoxSingleton.get(this).getApi();
		AndroidAuthSession session = buildSession();
		mApi = new DropboxAPI<AndroidAuthSession>(session);
		final DownloadAudioTask lstAudioTask = new DownloadAudioTask(this, mApi, audioPath);

		lstAudioTask.execute();
		final Button play = (Button) findViewById(R.id.button1);
        play.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            	lstAudioTask.startPlaying();
            	
            }
        });
	}
	
	
	//
	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// // Inflate the menu; this adds items to the action bar if it is present.
	// getMenuInflater().inflate(R.menu.downloadaudiofile, menu);
	// return true;
	// }

	public AudioActivity() {

		// We set the context this way so we don't accidentally leak activities
	}

	private AndroidAuthSession buildSession() {
		AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

		AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
		loadAuth(session);
		return session;
	}

	private void loadAuth(AndroidAuthSession session) {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
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
	
	 class DownloadAudioTask extends AsyncTask<Void, Long, Boolean> {

		private Context mContext;
		private final ProgressDialog mDialog;
		private DropboxAPI<?> mApi;
		private String mPath;

		private FileOutputStream mFos;

		private boolean mCanceled;
		private Long mFileLen;
		private String mErrorMsg;

		// Note that, since we use a single file name here for simplicity, you
		// won't be able to use this code for two simultaneous downloads.
		private final static String AUDIO_FILE_NAME = "audio.mp3";
		
		private void startPlaying() {
		     MediaPlayer mPlayer = new MediaPlayer();
		        try {
		            mPlayer.setDataSource(AUDIO_FILE_NAME);
		            mPlayer.prepare();
		            mPlayer.start();
		        } catch (IOException e) {
		            Log.d("LOG_TAG", "prepare() failed");
		        }
		    }
		public DownloadAudioTask(Context context, DropboxAPI<?> api,
				String dropboxPath) {
			// We set the context this way so we don't accidentally leak activities
			mContext = context.getApplicationContext();

			mApi = api;
			mPath = dropboxPath;

			mDialog = new ProgressDialog(context);
			mDialog.setMessage("Downloading Audio");
			mDialog.setButton("Cancel", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mCanceled = true;
					mErrorMsg = "Canceled";

					// This will cancel the getThumbnail operation by closing
					// its stream
					if (mFos != null) {
						try {
							mFos.close();
						} catch (IOException e) {
						}
					}
				}
			});

			mDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				if (mCanceled) {
					return false;
				}
				//File root =android.os.Environment.getExternalStorageDirectory();

				String cachePath = mContext.getExternalCacheDir().getAbsolutePath() + "/"
						+ AUDIO_FILE_NAME;
				Log.d("dadfaf", "download audio file path: " + cachePath);
				
				BufferedOutputStream bw = null;
				try {
					mFos = new FileOutputStream(cachePath);
					bw = new BufferedOutputStream(mFos);
					//mFos.write(string.getBytes());
					
				}
				catch (FileNotFoundException e) {
					mErrorMsg = "Couldn't create a local file to store the image";
					return false;
				}
				

				// mApi.getFile(mPath, rev, mFos, null);
				DropboxInputStream din = mApi.getFileStream(mPath, null);
				BufferedInputStream br = new BufferedInputStream(din);

				byte[] buf = new byte[1024];
				int read = 0;
				try {
					while ((read = br.read(buf, 0, buf.length)) != -1) {
						bw.write(buf, 0, read);
					}
					//mFos.flush();
					//mFos.close();
					br.close();
					bw.close();
					//mFos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				return true;

			} catch (DropboxUnlinkedException e) {
				// The AuthSession wasn't properly authenticated or user unlinked.
			} catch (DropboxPartialFileException e) {
				// We canceled the operation
				mErrorMsg = "Download canceled";
			} catch (DropboxServerException e) {
				// Server-side exception. These are examples of what could happen,
				// but we don't do anything special with them here.
				if (e.error == DropboxServerException._304_NOT_MODIFIED) {
					// won't happen since we don't pass in revision with metadata
				} else if (e.error == DropboxServerException._401_UNAUTHORIZED) {
					// Unauthorized, so we should unlink them. You may want to
					// automatically log the user out in this case.
				} else if (e.error == DropboxServerException._403_FORBIDDEN) {
					// Not allowed to access this
				} else if (e.error == DropboxServerException._404_NOT_FOUND) {
					// path not found (or if it was the thumbnail, can't be
					// thumbnailed)
				} else if (e.error == DropboxServerException._406_NOT_ACCEPTABLE) {
					// too many entries to return
				} else if (e.error == DropboxServerException._415_UNSUPPORTED_MEDIA) {
					// can't be thumbnailed
				} else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
					// user is over quota
				} else {
					// Something else
				}
				// This gets the Dropbox error, translated into the user's language
				mErrorMsg = e.body.userError;
				if (mErrorMsg == null) {
					mErrorMsg = e.body.error;
				}
			} catch (DropboxIOException e) {
				// Happens all the time, probably want to retry automatically.
				mErrorMsg = "Network error.  Try again.";
			} catch (DropboxParseException e) {
				// Probably due to Dropbox server restarting, should retry
				mErrorMsg = "Dropbox error.  Try again.";
			} catch (DropboxException e) {
				// Unknown error
				mErrorMsg = "Unknown error.  Try again.";
			}
			return false;
			
			
		}
		

		@Override
		protected void onPostExecute(Boolean result) {
			mDialog.dismiss();
			if(!result){
				showToast(mErrorMsg);
			}
		}

		private void showToast(String msg) {
			Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
			error.show();
		}
		

	}


}
