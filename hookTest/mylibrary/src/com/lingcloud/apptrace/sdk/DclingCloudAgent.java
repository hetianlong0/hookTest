package com.lingcloud.apptrace.sdk;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.lingcloud.apptrace.sdk.store.ConnectionQueue;
import com.lingcloud.apptrace.sdk.store.CrashDetails;
import com.lingcloud.apptrace.sdk.store.DeviceId;
import com.lingcloud.apptrace.sdk.store.EventQueue;
import com.lingcloud.apptrace.sdk.store.UserData;
import com.lingcloud.apptrace.sdk.store.lingAgentStore;
import com.taobao.dexposed.NetWorkHook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

public class DclingCloudAgent {

	private boolean isEnabled;

	private static String device_id;
	private static String system_name;
	private String app_version;
	private static String os_version;

	// x3C50
	private static String mobile_type = "Android";

	// device_name=="lennovo x3C50"
	private static String device_name;

	// com.example.test1;
	private String project_name;

	// 记录android相关的
	private static String VersionName;
	private static int VersionCode;
	private static String PackageName;

	// 获得传入的context
	private static Context mContext;

	private static boolean isSupport = false;
	private static boolean isLDevice = false;

	// timer； 维护与后台服务器的心跳命令；
	private ScheduledExecutorService timerService_;
	// timer执行的
	private static final long TIMER_DELAY_IN_SECONDS = 60;

	private EventQueue eventQueue_;

	// 生成的总的事件队列
	public ConnectionQueue connectionQueue_;
	// user data access
	public static UserData userData;

	// sdk的版本号
	public static final String LINGCLOUD_APPTRACE_SDK_VERSION_STRING = "1.00.00";
	// 默认的app版本号，假如不能取得app的版本号，就用该版本号
	public static final String DEFAULT_APP_VERSION = "1.0";
	// lingcloud sdk打印的tag
	public static final String TAG = "lingCloud";

	// crash的时候用户设定的参数
	private static Map<String, String> customSegments = null;

	// 是否使能日志记录功能
	private static boolean enableLogging_;

	// 线程队列，保存格式化后的字符串
	static public TaskQueue taskQueue_ = TaskQueue.getInstance();

	// 客户端的app_key,向灵云服务器注册说得
	String appKey_;
	// Server的URL
	public static String ServerUrl_;

	// 发送的命令创建工厂
	public CommandFactory commandFactory_;

	private long prevSessionDurationStartTime_;
	private int activityCount_;
	private boolean disableUpdateSessionRequests_;

	// track views
	private String lastView = null;
	private int lastViewStart = 0;
	private boolean firstView = true;
	private boolean autoViewTracker = true;

	public static List<String> publicKeyPinCertificates;

	/**
	 * Determines how many custom events can be queued locally before an attempt
	 * is made to submit them to a Count.ly server.
	 */
	private static final int EVENT_QUEUE_SIZE_THRESHOLD = 3;

	static {
		try {
			if (android.os.Build.VERSION.SDK_INT == 22) {
				System.loadLibrary("dexposed_l51");
			} else if (android.os.Build.VERSION.SDK_INT > 19
					&& android.os.Build.VERSION.SDK_INT <= 21) {
				System.loadLibrary("dexposed_l");
			} else if (android.os.Build.VERSION.SDK_INT > 14) {
				System.loadLibrary("dexposed");
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	// see
	// http://stackoverflow.com/questions/7048198/thread-safe-singletons-in-java
	private static class SingletonHolder {
		static final DclingCloudAgent instance = new DclingCloudAgent();
	}

	/**
	 * Returns the DclingCloudAgent singleton.
	 */
	public static DclingCloudAgent getInstance() {
		return SingletonHolder.instance;
	}

	public void init(Context context, String url, String appKey) {
		if (context != null) {
			mContext = context.getApplicationContext();
			getVersionNameAndInit(mContext);
			// CrashHandler crashHandler = CrashHandler.getInstance();
			// crashHandler.init(mContext);

			isSupport = true;
			isLDevice = android.os.Build.VERSION.SDK_INT >= 20;

			appKey_ = appKey;
			ServerUrl_ = url;

			commandFactory_ = new CommandFactory(this);

			// hook网络请求
			NetWorkHook.instance().start();

			// 设置本地存储文件和timer
			setStoreAndTimer();

			initConnectQueue(mContext, url, appKey,
					Utils.getDeviceHashId(mContext));

			// setUserData();

			// 设置后台服务器信息，URL, app_key
			// initUploadServices(url, appKey);

			// 启动接收线程，接收一些命令
		//	HttpSendThread.start();
		}
	}

	// public void setCrashEnabled(boolean enabled) {
	// isEnabled = enabled;
	// }

	public void sendDeviceInfo() {
		new Thread() {
			@Override
			public void run() {
				String url = "http://192.168.1.49:8081/sdk/mobile/device";
				try {
					JSONObject object = new JSONObject();
					object.put("device_id", device_id);
					object.put("system_name", system_name);
					object.put("app_version", VersionName);
					object.put("os_version", os_version);
					object.put("mobile_type", mobile_type);
					object.put("device_name", device_name);
					object.put("project_name", PackageName);
					HttpUtils.submitPostJSONData(url, object);
				} catch (Exception e) {
				}
			}
		}.start();
	}

	public void sendHookHttpCommand() {
		// NetWorkHook.instance().stop();
		new Thread() {
			@Override
			public void run() {
				String url = "http://192.168.1.49:8081/sdk/mobile/device";
				try {
					JSONObject object = new JSONObject();
					object.put("device_id", device_id);
					object.put("system_name", system_name);
					object.put("url", NetWorkHook.instance().executeUrl);
					object.put("method", NetWorkHook.instance().executeMethod);
					object.put("startTime",
							NetWorkHook.instance().executeStartTime);
					object.put("endTime", NetWorkHook.instance().executeEndTime);
					object.put("ret code", NetWorkHook.instance().urlRetCode);
					HttpUtils.submitPostJSONData(url, object);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public NetInfors getCurrentNetInfors() {
		// 获得网络信息
		NetInfors netRet = Utils.GetNetworkTypeAndIP(mContext);
		return netRet;
	}

	// 获得App的VersionCode
	public static String getVersionNameAndInit(Context context) {
		// 获得包名
		PackageName = context.getPackageName();
		// 应用的版本名称和版本号
		PackageManager pm = context.getPackageManager();
		PackageInfo pi;
		try {
			pi = pm.getPackageInfo(context.getPackageName(),
					PackageManager.GET_ACTIVITIES);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}

		VersionName = pi.versionName;
		VersionCode = pi.versionCode;

		device_id = Utils.getDeviceHashId(mContext);
		os_version = android.os.Build.VERSION.RELEASE;
		device_name = android.os.Build.MODEL;
		system_name = android.os.Build.PRODUCT;

		return VersionName;
	}

	//
	public void hookNetWorkEnable(Boolean enabled) {
		if (enabled) {
			NetWorkHook.instance().start();
		} else {
			NetWorkHook.instance().stop();
		}
	}

	// private void setLoggingEnabled(String Url, String appKey) {
	// MainBridge.sharedInstance().setLoggingEnabled(true);
	//
	// MainBridge.sharedInstance().init(mContext, Url, appKey,
	// Utils.getDeviceHashId(mContext));
	//
	// setUserData();
	//
	// enableCrashTracking();
	// MainBridge.sharedInstance().recordEvent("test", 1);
	// }

	// public void enableCrashTracking() {
	// // add some custom segments, like dependency library versions
	// HashMap<String, String> data = new HashMap<String, String>();
	// data.put("Facebook", "3.5");
	// data.put("Admob", "6.5");
	// MainBridge.sharedInstance().setCustomCrashSegments(data);
	// MainBridge.sharedInstance().enableCrashReporting();
	//
	// MainBridge.sharedInstance().setLocation(44.5888300, 33.5224000);
	// }

	// public void setUserData() {
	// HashMap<String, String> data = new HashMap<String, String>();
	// data.put("name", "Firstname Lastname");
	// data.put("username", "nickname");
	// data.put("email", "test@test.com");
	// data.put("organization", "Tester");
	// data.put("phone", "+123456789");
	// data.put("gender", "M");
	// // provide url to picture
	// // data.put("picture", "http://example.com/pictures/profile_pic.png");
	// // or locally from device
	// // data.put("picturePath", "/mnt/sdcard/portrait.jpg");
	// data.put("byear", "1987");
	//
	// // providing any custom key values to store with user
	// HashMap<String, String> custom = new HashMap<String, String>();
	// custom.put("country", "Turkey");
	// custom.put("city", "Istanbul");
	// custom.put("address", "My house 11");
	//
	// // set multiple custom properties
	// userData.setUserData(data, custom);
	//
	// // set custom properties by one
	// userData.setProperty("test", "test");
	//
	// // increment used value by 1
	// userData.incrementBy("used", 1);
	//
	// // insert value to array of unique values
	// userData.pushUniqueValue("type", "morning");
	//
	// // insert multiple values to same property
	// userData.pushUniqueValue("skill", "fire");
	// userData.pushUniqueValue("skill", "earth");
	//
	// // userData.save();
	// }

	public synchronized void onStart(Activity activity) {
		// appLaunchDeepLink = false;
		if (eventQueue_ == null) {
			throw new IllegalStateException(
					"init must be called before onStart");
		}

		++activityCount_;
		if (activityCount_ == 1) {
			onStartHelper();
		}

		// if(referrer != null){
		// connectionQueue_.sendReferrerData(referrer);
		// ReferrerReceiver.deleteReferrer(context_);
		// }

		CrashDetails.inForeground();
		//
		if (autoViewTracker) {
			recordView(activity.getClass().getName());
		}
	}

	/**
	 * Called when the first Activity is started. Sends a begin session event to
	 * the server and initializes application session tracking.
	 */
	void onStartHelper() {
		prevSessionDurationStartTime_ = System.nanoTime();
		commandFactory_.beginSession();
	}

	/**
	 * Tells the lingcloud SDK that an Activity has stopped. Since Android does
	 * not have an easy way to determine when an application instance starts and
	 * stops, you must call this method from every one of your Activity's onStop
	 * methods for accurate application session tracking.
	 * 
	 * @throws IllegalStateException
	 *             if lingcloud SDK has not been initialized, or if unbalanced
	 *             calls to onStart/onStop are detected
	 */
	public synchronized void onStop() {
		if (eventQueue_ == null) {
			throw new IllegalStateException("init must be called before onStop");
		}
		if (activityCount_ == 0) {
			throw new IllegalStateException("must call onStart before onStop");
		}

		--activityCount_;
		if (activityCount_ == 0) {
			onStopHelper();
		}

		CrashDetails.inBackground();

		// report current view duration
		reportViewDuration();
	}

	private void setStoreAndTimer() {
		connectionQueue_ = new ConnectionQueue();
		userData = new UserData(connectionQueue_);
		timerService_ = Executors.newSingleThreadScheduledExecutor();
		timerService_.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				onTimer();
			}
		}, TIMER_DELAY_IN_SECONDS, TIMER_DELAY_IN_SECONDS, TimeUnit.SECONDS);
	}

	/**
	 * Called every 60 seconds to send a session heartbeat to the server. Does
	 * nothing if there is not an active application session.
	 */
	synchronized void onTimer() {
		final boolean hasActiveSession = activityCount_ > 0;
		if (hasActiveSession) {
			if (!disableUpdateSessionRequests_) {
				commandFactory_
						.updateSession(roundedSecondsSinceLastSessionDurationUpdate());
			}
		}
	}

	/**
	 * Calculates the unsent session duration in seconds, rounded to the nearest
	 * int.
	 */
	int roundedSecondsSinceLastSessionDurationUpdate() {
		final long currentTimestampInNanoseconds = System.nanoTime();
		final long unsentSessionLengthInNanoseconds = currentTimestampInNanoseconds
				- prevSessionDurationStartTime_;
		prevSessionDurationStartTime_ = currentTimestampInNanoseconds;
		return (int) Math
				.round(unsentSessionLengthInNanoseconds / 1000000000.0d);
	}

	/**
	 * Called when final Activity is stopped. Sends an end session event to the
	 * server, also sends any unsent custom events.
	 */
	void onStopHelper() {
		commandFactory_
				.endSession(roundedSecondsSinceLastSessionDurationUpdate());
		prevSessionDurationStartTime_ = 0;

		if (eventQueue_.size() > 0) {
			commandFactory_.recordEvents(eventQueue_.events());
		}
	}

	/**
	 * Immediately disables session &amp; event tracking and clears any stored
	 * session &amp; event data. This API is useful if your app has a tracking
	 * opt-out switch, and you want to immediately disable tracking when a user
	 * opts out. The onStart/onStop/recordEvent methods will throw
	 * IllegalStateException after calling this until lingcloud is reinitialized
	 * by calling init again.
	 */
	public synchronized void halt() {
		eventQueue_ = null;
		final lingAgentStore lingStore = connectionQueue_.getLingAgentStore();
		if (lingStore != null) {
			lingStore.clear();
		}

		connectionQueue_.setContext(null);
		connectionQueue_.setServerURL(null);
		connectionQueue_.setAppKey(null);
		connectionQueue_.setLingAgentStore(null);
		prevSessionDurationStartTime_ = 0;
		activityCount_ = 0;
	}

	/**
	 * Sets custom segments to be reported with crash reports In custom segments
	 * you can provide any string key values to segments crashes by
	 * 
	 * @param segments
	 *            Map&lt;String, String&gt; key segments and their values
	 */
	public synchronized DclingCloudAgent setCustomCrashSegments(
			Map<String, String> segments) {
		if (segments != null)
			CrashDetails.setCustomSegments(segments);
		return this;
	}

	void initConnectQueue(Context context, String serverURL, String appKey,
			String deviceID) {
		// if we get here and eventQueue_ != null, init is being called again
		// with the same values,
		// so there is nothing to do, because we are already initialized with
		// those values
		if (eventQueue_ == null) {
			DeviceId deviceIdInstance = null;
			if (deviceID != null) {
				deviceIdInstance = new DeviceId(deviceID);
			}

			final lingAgentStore lingStore = new lingAgentStore(context);

			deviceIdInstance.init(context, lingStore, true);

			connectionQueue_.setServerURL(serverURL);
			connectionQueue_.setAppKey(appKey);
			connectionQueue_.setLingAgentStore(lingStore);
			connectionQueue_.setDeviceId(deviceIdInstance);

			eventQueue_ = new EventQueue(lingStore);
		}
		// context is allowed to be changed on the second init call
		connectionQueue_.setContext(context);
	}

	public void sendCrashReport(String error, boolean nonfatal) {
		commandFactory_.sendCrashReport(error, nonfatal);
	}

	public void sendUserData() {
		commandFactory_.sendUserData();
	}

	public static Context getContext() {
		return mContext;
	}

	public static void onCreate(Activity activity) {
		Intent launchIntent = activity.getPackageManager()
				.getLaunchIntentForPackage(activity.getPackageName());

		if (isLoggingEnabled()) {
			Log.d(TAG, "Activity created: " + activity.getClass().getName()
					+ " ( main is "
					+ launchIntent.getComponent().getClassName() + ")");
		}

		Intent intent = activity.getIntent();
		if (intent != null) {
			Uri data = intent.getData();
			if (data != null) {
				// if (sharedInstance().isLoggingEnabled()) {
				// Log.d(MainBridge.TAG, "Data in activity created intent: " +
				// data + " (appLaunchDeepLink " +
				// sharedInstance().appLaunchDeepLink + ") " );
				// }
				// if (sharedInstance().appLaunchDeepLink) {
				// DeviceInfo.deepLink = data.toString();
				// }
			}
		}
	}

	public synchronized void setLocation(double lat, double lon) {
		try {
			connectionQueue_.getLingAgentStore().setLocation(lat, lon);
			if (disableUpdateSessionRequests_) {
				// connectionQueue_.updateSession(roundedSecondsSinceLastSessionDurationUpdate());
				commandFactory_
						.updateSession(roundedSecondsSinceLastSessionDurationUpdate());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Enable crash reporting to send unhandled crash reports to server
	 */
	public synchronized DclingCloudAgent enableCrashReporting() {
		// get default handler
		final Thread.UncaughtExceptionHandler oldHandler = Thread
				.getDefaultUncaughtExceptionHandler();

		Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				DclingCloudAgent.getInstance().commandFactory_.sendCrashReport(
						sw.toString(), false);

				// if there was another handler before
				if (oldHandler != null) {
					// notify it also
					oldHandler.uncaughtException(t, e);
				}
			}
		};

		Thread.setDefaultUncaughtExceptionHandler(handler);
		return this;
	}

	/**
	 * Reports duration of last view
	 */
	void reportViewDuration() {
		if (lastView != null) {
			HashMap<String, String> segments = new HashMap<String, String>();
			segments.put("name", lastView);
			segments.put("dur",
					String.valueOf(Utils.currentTimestamp() - lastViewStart));
			segments.put("segment", "Android");
			recordEvent("[CLY]_view", segments, 1);
			lastView = null;
			lastViewStart = 0;
		}
	}

	public synchronized void recordView(String viewName) {
		reportViewDuration();
		lastView = viewName;
		lastViewStart = Utils.currentTimestamp();
		HashMap<String, String> segments = new HashMap<String, String>();
		segments.put("name", viewName);
		segments.put("visit", "1");
		segments.put("segment", "Android");
		if (firstView) {
			firstView = false;
			segments.put("start", "1");
		}
		recordEvent("[CLY]_view", segments, 1);
		return;
	}

	public void recordEvent(String key, int count, double sum) {
		this.recordEvent(key, (Map) null, count, sum);
	}

	public void recordEvent(final String key,
			final Map<String, String> segmentation, final int count) {
		recordEvent(key, segmentation, count, 0);
	}

	public synchronized void setViewTracking(boolean enable) {
		autoViewTracker = enable;
	}

	public synchronized boolean isViewTrackingEnabled() {
		return autoViewTracker;
	}

	public void recordEvent(final String key, final int count) {
		recordEvent(key, null, count, 0);
	}

	public synchronized void recordEvent(final String key,
			final Map<String, String> segmentation, final int count,
			final double sum) {
		recordEvent(key, segmentation, count, sum, 0);
	}

	public synchronized void recordEvent(final String key,
			final Map<String, String> segmentation, final int count,
			final double sum, final double dur) {
		if (!isInitialized()) {
			throw new IllegalStateException(
					"DclingCloud.getInstance().init must be called before recordEvent");
		}
		if (key == null || key.length() == 0) {
			throw new IllegalArgumentException(
					"Valid lingcloud event key is required");
		}
		if (count < 1) {
			throw new IllegalArgumentException(
					"lingcloud event count should be greater than zero");
		}
		if (segmentation != null) {
			for (String k : segmentation.keySet()) {
				if (k == null || k.length() == 0) {
					throw new IllegalArgumentException(
							"lingcloud event segmentation key cannot be null or empty");
				}
				if (segmentation.get(k) == null
						|| segmentation.get(k).length() == 0) {
					throw new IllegalArgumentException(
							"lingcloud event segmentation value cannot be null or empty");
				}
			}
		}

		eventQueue_.recordEvent(key, segmentation, count, sum, dur);
		sendEventsIfNeeded();
	}

	public synchronized boolean isInitialized() {
		return eventQueue_ != null;
	}

	/**
	 * Submits all of the locally queued events to the server if there are more
	 * than 10 of them.
	 */
	void sendEventsIfNeeded() {
		if (eventQueue_.size() >= EVENT_QUEUE_SIZE_THRESHOLD) {
			// connectionQueue_.recordEvents(eventQueue_.events());
			commandFactory_.recordEvents(eventQueue_.events());
		}
	}

	/**
	 * Sets information about user. Possible keys are:
	 * <ul>
	 * <li>
	 * name - (String) providing user's full name</li>
	 * <li>
	 * username - (String) providing user's nickname</li>
	 * <li>
	 * email - (String) providing user's email address</li>
	 * <li>
	 * organization - (String) providing user's organization's name where user
	 * works</li>
	 * <li>
	 * phone - (String) providing user's phone number</li>
	 * <li>
	 * picture - (String) providing WWW URL to user's avatar or profile picture</li>
	 * <li>
	 * picturePath - (String) providing local path to user's avatar or profile
	 * picture</li>
	 * <li>
	 * gender - (String) providing user's gender as M for male and F for female</li>
	 * <li>
	 * byear - (int) providing user's year of birth as integer</li>
	 * </ul>
	 * 
	 * @param data
	 *            Map&lt;String, String&gt; with user data
	 * @deprecated use {@link UserData#setUserData(Map)} to set data and
	 *             {@link UserData#save()} to send it to server.
	 */
	public synchronized void setUserData(Map<String, String> data) {
		setUserData(data, null);
	}

	/**
	 * Sets information about user with custom properties. In custom properties
	 * you can provide any string key values to be stored with user Possible
	 * keys are:
	 * <ul>
	 * <li>
	 * name - (String) providing user's full name</li>
	 * <li>
	 * username - (String) providing user's nickname</li>
	 * <li>
	 * email - (String) providing user's email address</li>
	 * <li>
	 * organization - (String) providing user's organization's name where user
	 * works</li>
	 * <li>
	 * phone - (String) providing user's phone number</li>
	 * <li>
	 * picture - (String) providing WWW URL to user's avatar or profile picture</li>
	 * <li>
	 * picturePath - (String) providing local path to user's avatar or profile
	 * picture</li>
	 * <li>
	 * gender - (String) providing user's gender as M for male and F for female</li>
	 * <li>
	 * byear - (int) providing user's year of birth as integer</li>
	 * </ul>
	 * 
	 * @param data
	 *            Map&lt;String, String&gt; with user data
	 * @param customdata
	 *            Map&lt;String, String&gt; with custom key values for this user
	 */
	public void setUserData(Map<String, String> data,
			Map<String, String> customdata) {
		UserData.setData(data);
		if (customdata != null)
			UserData.setCustomData(customdata);
	}

	/**
	 * Sets custom properties. In custom properties you can provide any string
	 * key values to be stored with user
	 * 
	 * @param customdata
	 *            Map&lt;String, String&gt; with custom key values for this user
	 */
	public void setCustomUserData(Map<String, String> customdata) {
		if (customdata != null)
			UserData.setCustomData(customdata);
	}

	/**
	 * Sets custom provide key/value as custom property.
	 * 
	 * @param key
	 *            String with key for the property
	 * @param value
	 *            String with value for the property
	 */
	public void setProperty(String key, String value) {
		UserData.setCustomProperty(key, value);
	}

	/**
	 * Sets whether debug logging is turned on or off. Logging is disabled by
	 * default.
	 * 
	 * @param enableLogging
	 *            true to enable logging, false to disable logging
	 * @return lingcloud instance for easy method chaining
	 */
	public synchronized void setLoggingEnabled(final boolean enableLogging) {
		enableLogging_ = enableLogging;
	}

	public synchronized static boolean isLoggingEnabled() {
		return enableLogging_;
	}

	public synchronized void addCrashLog(String record) {
		CrashDetails.addLog(record);
	}

	/**
	 * Utility method for testing validity of a URL.
	 */
	public static boolean isValidURL(final String urlStr) {
		boolean validURL = false;
		if (urlStr != null && urlStr.length() > 0) {
			try {
				new URL(urlStr);
				validURL = true;
			} catch (MalformedURLException e) {
				validURL = false;
			}
		}
		return validURL;
	}
}
