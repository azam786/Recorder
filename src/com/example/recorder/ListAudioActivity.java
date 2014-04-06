package com.example.recorder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

public class ListAudioActivity extends ListActivity {

	final static private String APP_KEY = "r8nx0zijedcb1v8";
	final static private String APP_SECRET = "kjmv29qa5gwhefl";
	// You don't need to change these, leave them alone.
	final static private String ACCOUNT_PREFS_NAME = "prefs";
	final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
	final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";

	private final String AUDIO_DIR = "/Photos/";

	DropboxAPI<AndroidAuthSession> mApi;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_list_audio);

		AndroidAuthSession session = buildSession();
		mApi = new DropboxAPI<AndroidAuthSession>(session);

		ListAudioTask lstAudioTask = new ListAudioTask(this, mApi, AUDIO_DIR);

		lstAudioTask.execute();
		
	}
	
	

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		Intent audioIntent=new Intent(this,AudioActivity.class);
		String audioPath=(String) getListAdapter().getItem(position);
		audioIntent.putExtra("audioPath", audioPath);
		
		startActivity(audioIntent);
	}



	class ListAudioTask extends AsyncTask<Void, Long, List<String>> {

		private Context mContext;
		private final ProgressDialog mDialog;
		private DropboxAPI<?> mApi;
		private String mPath;
		private FileOutputStream mFos;

		private boolean mCanceled;
		private Long mFileLen;
		private String mErrorMsg;

		public ListAudioTask(Context context, DropboxAPI<?> api,
				String dropboxPath) {
			// We set the context this way so we don't accidentally leak
			// activities
			mContext = context.getApplicationContext();

			mApi = api;
			mPath = dropboxPath;
			mDialog = new ProgressDialog(context);
			mDialog.setMessage("Downloading audio directory meta data.");
			mDialog.setButton("Cancel",
					new android.content.DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							mCanceled = true;
							mErrorMsg = "Canceled";

							// This will cancel the getThumbnail operation by
							// closing
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
		protected List<String> doInBackground(Void... params) {
			try {
				if (mCanceled) {
					return null;
				}

				// Get the metadata for a directory
				Entry dirent = mApi.metadata(mPath, 1000, null, true, null);

				if (!dirent.isDir || dirent.contents == null) {
					// It's not a directory, or there's nothing in it
					mErrorMsg = "File or empty directory";
					return null;
				}

				List<String> lstPaths = new ArrayList<String>();
				// Make a list of everything in it that we can get a thumbnail
				// for
				for (Entry ent : dirent.contents) {
					lstPaths.add(ent.path);
				}
				return lstPaths;

			}

			catch (DropboxUnlinkedException e) {
				// The AuthSession wasn't properly authenticated or user
				// unlinked.
			} catch (DropboxPartialFileException e) {
				// We canceled the operation
				mErrorMsg = "Download canceled";
			} catch (DropboxServerException e) {
				// Server-side exception. These are examples of what could
				// happen,
				// but we don't do anything special with them here.
				if (e.error == DropboxServerException._304_NOT_MODIFIED) {
					// won't happen since we don't pass in revision with
					// metadata
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
				// This gets the Dropbox error, translated into the user's
				// language
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
			return null;
		}

		@Override
		protected void onProgressUpdate(Long... progress) {
			int percent = (int) (100.0 * (double) progress[0] / mFileLen + 0.5);
			mDialog.setProgress(percent);
		}

		@Override
		protected void onPostExecute(List<String> lstAudioTask) {
			mDialog.dismiss();

			if (lstAudioTask != null) {
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(ListAudioActivity.this,
						android.R.layout.simple_list_item_1, lstAudioTask);
				setListAdapter(adapter);
			}
		}

		private void showToast(String msg) {
			Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
			error.show();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.list_audio, menu);
		return true;
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

}
