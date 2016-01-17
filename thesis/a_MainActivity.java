package edu.arizona.jbv.thesis.main;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.SerializableEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import edu.arizona.jbv.thesis.crypto.AES256Encryptor;
import edu.arizona.jbv.thesis.crypto.CMSSignedDataEncryptor;
import edu.arizona.jbv.thesis.crypto.Encryptor;
import edu.arizona.jbv.thesis.data.IdentityToken;
import edu.arizona.jbv.thesis.networking.SSLClient;
import edu.arizona.jbv.thesis.utils.ThesisLog;
import edu.arizona.jbv.thesis.utils.ThesisTimer;

/**
 * There is only one activity in the prototype app, and this is it. It has a
 * dropdown for selecting which records will be requested, and three buttons to
 * choose which trust server will be queried for the identity token. In this
 * prototype, the client is known by all the trust servers, so querying any
 * trust server results in an IdentityToken being returned. In a real
 * application, only the trust server corresponding to the user's district would
 * be likely to reply to identity token request.
 * 
 * Asking for different health records results in different routes being taken
 * when finding the trust server responsible for the correct health record
 * server, but this goes on behind the scenes on the server.
 * 
 * @author Jorge Vergara
 * 
 */
public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findViewById(R.id.button1).setOnClickListener(new ButtonClick());
		findViewById(R.id.button2).setOnClickListener(new ButtonClick());
		findViewById(R.id.button3).setOnClickListener(new ButtonClick());
	}

	/**
	 * The NetworkThread makes the network requests to the trust server
	 * 
	 * @author Jorge Vergara
	 * 
	 */
	private class NetworkThread extends Thread {

		private char server;
		private String userID;

		/**
		 * Sets up the NetworkThread. Start still needs to be called for any
		 * real work to be done.
		 * 
		 * @param server
		 *            The trust server to query. 'A', 'B', or 'C', currently.
		 * @param userID
		 *            The userID to ask for. The userIDs are currently tied to
		 *            the buttons
		 */
		public NetworkThread(char server, String userID) {
			this.server = server;
			this.userID = userID;
			ThesisLog
					.l("Asking TrustServer" + server + " for userID " + userID);
		}

		public void run() {
			InputStream truststore = getResources().openRawResource(
					R.raw.truststore);
			InputStream clientstore = getResources().openRawResource(
					R.raw.android);
			SSLClient cli = new SSLClient(truststore, clientstore);
			List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			postParameters.add(new BasicNameValuePair("userID", userID));
			String paramString = URLEncodedUtils
					.format(postParameters, "utf-8");
			HttpGet post = new HttpGet(
					"https://dmft.cs.arizona.edu:8082/MobileTracker/Thesis/SSL/TrustServer"
							+ server + "?" + paramString);
			try {

				ThesisLog.l("Requesting identity token. Starting timer");
				ThesisTimer.startTimer();

				HttpResponse resp = cli.execute(post);

				long time = ThesisTimer.stopTimer();
				ThesisLog.l("Finished. Total time was: " + time
						+ " milliseconds");

				InputStream is = resp.getEntity().getContent();
				ObjectInputStream ois = new ObjectInputStream(is);
				Object encodedCMSData = ois.readObject();
				InputStream keystore = getResources().openRawResource(
						R.raw.trustserverakeystore);
				InputStream key = getResources().openRawResource(
						R.raw.trustserverakey);
				Encryptor enc = new CMSSignedDataEncryptor(keystore, key);
				IdentityToken token = (IdentityToken) enc.byteToObject(enc
						.decrypt(encodedCMSData));

				HttpClient recordRequester = new DefaultHttpClient();
				HttpPost post2 = new HttpPost(token.urlOfHealthRecordServer);
				Encryptor aesEnc = new AES256Encryptor();
				Object aesEncryptedCMSData = aesEnc.encrypt(encodedCMSData);
				HttpEntity ent = new SerializableEntity(
						(Serializable) aesEncryptedCMSData, false);
				post2.setEntity(ent);

				ThesisLog.l("Requesting health records. Starting timer");
				ThesisTimer.startTimer();

				HttpResponse resp2 = recordRequester.execute(post2);

				time = ThesisTimer.stopTimer();
				ThesisLog.l("Finished. Total time was: " + time
						+ " milliseconds");

				ObjectInputStream ois2 = new ObjectInputStream(resp2
						.getEntity().getContent());
				byte[] aesEncodedReply = (byte[]) ois2.readObject();
				final String s2 = (String) aesEnc.byteToObject(aesEnc
						.decrypt(aesEncodedReply));

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(MainActivity.this,
								"Request Completed: " + s2, Toast.LENGTH_SHORT)
								.show();
					}
				});

				ThesisLog.l(s2);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		new AlertDialog.Builder(MainActivity.this).setTitle("Help")
				.setMessage(getResources().getString(R.string.menu_text))
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						;
					}
				}).show();
		return true;
	}

	/**
	 * Listens to which trust server you are trying to query. Pulls the userID
	 * and send the request by starting up a NetworkThread.
	 * 
	 * @author Jorge Vergara
	 * 
	 */
	private class ButtonClick implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			Spinner spinner = (Spinner) findViewById(R.id.spinner);
			String name = (String) spinner.getSelectedItem();

			Button serverAButton = (Button) findViewById(R.id.button1);
			Button serverBButton = (Button) findViewById(R.id.button2);

			if (arg0 == serverAButton) {
				new NetworkThread('A', name).start();
			} else if (arg0 == serverBButton) {
				new NetworkThread('B', name).start();
			} else { // server C button
				new NetworkThread('C', name).start();
			}
		}

	}
}
