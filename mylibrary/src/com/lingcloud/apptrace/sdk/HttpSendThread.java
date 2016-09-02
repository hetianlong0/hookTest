package com.lingcloud.apptrace.sdk;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

import android.util.Log;

public class HttpSendThread {

	public static boolean sIsExit = true;

	private static final int CONNECT_TIMEOUT_IN_MILLISECONDS = 30000;
	private static final int READ_TIMEOUT_IN_MILLISECONDS = 30000;

	// private static ExecutorService executor_;
	//
	// static public ExecutorService getInstance() {
	// if (executor_ == null) {
	// executor_ = Executors.newSingleThreadExecutor();
	// }
	// return executor_;
	// }
	//
	// static public void start() {
	// ExecutorService exeServer = getInstance();
	// exeServer.execute(new Runnable() {
	// @Override
	// public void run() {
	// try {
	//
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	// });
	// }

	private static Thread sSendHttpThread;

	public static void start() {
		if (sSendHttpThread == null) {
			sSendHttpThread = new Thread(new Runnable() {
				@Override
				public void run() {
					//
					while (true) {
						try {
							String taskStr = DclingCloudAgent.taskQueue_
									.takeTask(); // 取得要传输的字符串信息
							sendCommand(taskStr);
						} catch (Exception e) { // 线程退出异常
							if (sIsExit) {
								break;
							} else {
								continue;
							}
						}
					}
				}
			});
			sSendHttpThread.start(); // 启动线程
		}
	}

	static URLConnection urlConnectionForEventData(final String eventData)
			throws IOException {
		String urlStr = DclingCloudAgent.ServerUrl_ + "/i?";
		if (!eventData.contains("&crash=") && eventData.length() < 2048)
			urlStr += eventData;

		final URL url = new URL(urlStr);
		final HttpURLConnection conn;

		conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(CONNECT_TIMEOUT_IN_MILLISECONDS);
		conn.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
		conn.setUseCaches(false);
		conn.setDoInput(true);

		if (eventData.contains("&crash=") || eventData.length() >= 2048) {
			if (DclingCloudAgent.isLoggingEnabled()) {
				Log.d(DclingCloudAgent.TAG, "Using post because of crash");
			}
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			OutputStream os = conn.getOutputStream();
			// htl add; comment
			// BufferedWriter writer = new BufferedWriter(new
			// OutputStreamWriter(os));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					os, "UTF-8"));
			writer.write(eventData);
			writer.flush();
			writer.close();
			os.close();
		} else {
			conn.setDoOutput(false);
		}
		return conn;
	}

	static void sendCommand(String src) {
		URLConnection conn = null;
		BufferedInputStream responseStream = null;
		try {
			// initialize and open connection
			conn = urlConnectionForEventData(src);
			conn.connect();

			// consume response stream
			responseStream = new BufferedInputStream(conn.getInputStream());
			final ByteArrayOutputStream responseData = new ByteArrayOutputStream(
					256); // big enough to handle success response without
							// reallocating
			int c;
			while ((c = responseStream.read()) != -1) {
				responseData.write(c);
			}

			// response code has to be 2xx to be considered a success
			boolean success = true;
			if (conn instanceof HttpURLConnection) {
				final HttpURLConnection httpConn = (HttpURLConnection) conn;
				final int responseCode = httpConn.getResponseCode();
				success = responseCode >= 200 && responseCode < 300;
				 if (!success && DclingCloudAgent.isLoggingEnabled()) {
				 Log.w(DclingCloudAgent.TAG, "HTTP error response code was " +
				 responseCode + " from submitting event data: ");
				 }
			}

			// HTTP response code was good, check response JSON contains
			// {"result":"Success"}
			if (success) {
				final JSONObject responseDict = new JSONObject(
						responseData.toString("UTF-8"));
				success = responseDict.optString("result").equalsIgnoreCase(
						"success");
				 if (!success && DclingCloudAgent.isLoggingEnabled()) {
				 Log.w(DclingCloudAgent.TAG,
				 "Response from Countly server did not report success, it was: "
				 + responseData.toString("UTF-8"));
				 }
			}

			if (success) {
//				// if (MainBridge.sharedInstance().isLoggingEnabled()) {
//				// Log.d(MainBridge.TAG, "ok ->" + eventData);
//				// }
//
//				// successfully submitted event data to Count.ly server, so
//				// remove
//				// this one from the stored events collection
//				 store_.removeConnection(storedEvents[0]);
			} else {
				// warning was logged above, stop processing, let next tick take
				// care of retrying
				// break;
			}
		} catch (Exception e) {
			// if (MainBridge.sharedInstance().isLoggingEnabled()) {
			// Log.w(MainBridge.TAG,
			// "Got exception while trying to submit event data: " + eventData,
			// e);
			// }
			// if exception occurred, stop processing, let next tick take care
			// of retrying
			// break;
			e.printStackTrace();
		} finally {
			// free connection resources
			if (responseStream != null) {
				try {
					responseStream.close();
				} catch (IOException ignored) {
				}
			}
			if (conn != null && conn instanceof HttpURLConnection) {
				((HttpURLConnection) conn).disconnect();
			}
		}
	}
}
