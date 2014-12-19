package find.service.net.diogomarques.wifioppish;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import find.service.gcm.Notifications;
import find.service.net.diogomarques.wifioppish.INetworkingFacade.OnAccessPointScanListener;
import find.service.net.diogomarques.wifioppish.networking.Message;
import find.service.net.diogomarques.wifioppish.networking.MessageGroup;
import find.service.net.diogomarques.wifioppish.networking.SoftAPDelegate;
import find.service.net.diogomarques.wifioppish.networking.UDPDelegate;
import find.service.net.diogomarques.wifioppish.networking.WiFiDelegate;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.os.Handler;
import android.util.Log;

/**
 * The Android-specific networking controller facade.
 * 
 * @author Diogo Marques <diogohomemmarques@gmail.com>
 * 
 */
public class AndroidNetworkingFacade implements INetworkingFacade {

	/*
	 * Dependencies.
	 */
	private final Context mContext;
	private final IEnvironment mEnvironment;
	private final SoftAPDelegate mSoftAP;
	private final WiFiDelegate mWiFi;
	private final UDPDelegate mUdp;

	private final String TAG = "LOST Service";

	/**
	 * Static factory that creates instances of networking controllers.
	 * 
	 * @param c
	 *            the context
	 * @param env
	 *            the state machine environment
	 * @return a new instance will all dependencies set
	 */
	public static AndroidNetworkingFacade createInstance(Context c,
			IEnvironment env) {
		return new AndroidNetworkingFacade(c, env, new SoftAPDelegate(c),
				new WiFiDelegate(c, env), new UDPDelegate(c, env));
	}

	/**
	 * Convenience method to create an {@link AndroidNetworkingFacade} instance
	 * with some default values
	 * 
	 * @param context
	 *            Android context
	 * @param environment
	 *            LOST-OppNet Environment
	 * @param softAP
	 *            Software AccessPoint controller
	 * @param wiFi
	 *            WiFi controller
	 * @param udp
	 *            UDP network manager to establish connections
	 */
	private AndroidNetworkingFacade(Context context, IEnvironment environment,
			SoftAPDelegate softAP, WiFiDelegate wiFi, UDPDelegate udp) {
		this.mContext = context;
		this.mEnvironment = environment;
		this.mSoftAP = softAP;
		this.mWiFi = wiFi;
		this.mUdp = udp;
	}

	/**
	 * Gets the current Android context
	 * 
	 * @return Android context
	 */
	protected Context getContext() {
		return mContext;
	}

	@Override
	public void startAcessPoint() {
		mSoftAP.startWifiAP(this);
	}

	@Override
	public void stopAccessPoint() {
		mUdp.releaseBroadcastSocket();
		mSoftAP.stopWifiAP(this);
	}

	/**
	 * Get the WifiConfiguration based on the SSID and password set on the
	 * domain parameters in {@link IDomainPreferences}.
	 * 
	 * @return a WifiConfiguration for a WPA access point with the SSID in
	 *         {@link IDomainPreferences#getWifiSSID()} and the password on
	 *         {@link IDomainPreferences#getWifiPassword()}.
	 */
	public WifiConfiguration getWifiSoftAPConfiguration() {
		IDomainPreferences preferences = mEnvironment.getPreferences();
		WifiConfiguration wc = new WifiConfiguration();
		wc.SSID = preferences.getWifiSSID();
		wc.preSharedKey = preferences.getWifiPassword();
		wc.allowedGroupCiphers.clear();
		wc.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
		wc.allowedPairwiseCiphers.clear();
		wc.allowedProtocols.clear();
		return wc;
	}

	@Override
	public void send(Message msg, OnSendListener listener) {
		mUdp.send(msg, listener);
	}

	@Override
	public void send(MessageGroup msgs, OnSendListener listener) {
		mUdp.send(msgs, listener);
	}

	@Override
	public void receiveFirst(int timeoutMilis, OnReceiveListener listener) {
		mUdp.receiveFirst(timeoutMilis, listener);
	}

	@Override
	public void receive(int timeoutMilis, OnReceiveListener listener) {
		mUdp.receive(timeoutMilis, listener);
	}

	@Override
	public void scanForAP(int timeoutMilis,
			final OnAccessPointScanListener listener) {
		mWiFi.scanForAP(timeoutMilis, listener, this);

	}

	@Override
	public void scanForInternet(int timeout, OnInternetConnection listener) {
		boolean connected = false;

		if (isNetworkAvailable()) {
			if (ping()) {
				Log.d(TAG, " Connected internet");
				connected = true;
				listener.onInternetConnection();
				return;
			} else {
				Log.d(TAG, " No ping internet");

			}
		}
		internetTicking(timeout, mEnvironment.getPreferences().getScanPeriod(),
				0, listener);

		/*
		 * long startTime = new Date().getTime(); while (!connected) {
		 * Log.d(TAG, " Notinternet");
		 * 
		 * long tick = new Date().getTime(); if (tick > startTime + timeout) {
		 * Log.w("", "Internet scan timeout"); listener.onScanTimeout(); break;
		 * } while (true) { if (new Date().getTime() > tick +
		 * mEnvironment.getPreferences().getScanPeriod()) { Log.d(TAG,
		 * " verify networkasas");
		 * 
		 * if(isNetworkAvailable()){ if(ping()){ Log.d(TAG,
		 * " Connected internet"); connected=true;
		 * listener.onInternetConnection(); }else{ Log.d(TAG,
		 * " No ping internet");
		 * 
		 * } }else{ Log.d(TAG, " No network failed");
		 * 
		 * }
		 * 
		 * break; }
		 * 
		 * } }
		 */

	}

	private void internetTicking(final int timeoutMilis, final int delay,
			final long totaltime, final OnInternetConnection listener) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (totaltime >= timeoutMilis) {
					Log.w("", "Internet timeout");
					listener.onScanTimeout();
				} else {
					Log.w("", "Internet tick "+delay+" " + totaltime );

					long totalTime = delay + totaltime;
					if (isNetworkAvailable()) {
						if (ping()) {
							Log.d(TAG, " Ping successfull");
							listener.onInternetConnection();
							return;
						}
						
					}
					Log.d(TAG, " No  internet");

					internetTicking(timeoutMilis, delay, totalTime,
							listener);

				}
			}
		}, delay);
	}

	/**
	 * Checks if an Internet connection is available
	 * 
	 * @return True if connection is available; false otherwise
	 */
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null
				&& activeNetworkInfo.isConnectedOrConnecting();
	}

	/**
	 * Does a network ping to a given hostname
	 * 
	 * @param url
	 *            Hostname to ping
	 * @return Ping command output
	 */
	private boolean ping() {

		try {
			URL url = new URL("http://www.google.com");
			HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
			urlc.setConnectTimeout(5000);
			urlc.connect();

			if (urlc.getResponseCode() == 200) {
				return new Boolean(true);
			}

		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

}
