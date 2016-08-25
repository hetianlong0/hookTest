package com.dcling.cloud;

import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.JSONObject;

import com.dcling.cloud.countly2.Countly;
import com.taobao.dexposed.NetWorkHook;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class DclingCloudAgent {

	private volatile static DclingCloudAgent sLingAgent;

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

	static {
		// load xposed lib for hook.
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

	public static DclingCloudAgent getInstace(Context context) {
		if (sLingAgent == null) {
			sLingAgent = new DclingCloudAgent();

			if (context != null) {
				mContext = context.getApplicationContext();
				getVersionNameAndInit(mContext);
				CrashHandler crashHandler = CrashHandler.getInstance();
				crashHandler.init(mContext);

				isSupport = true;
				isLDevice = android.os.Build.VERSION.SDK_INT >= 20;

				// hook网络请求
				NetWorkHook.instance().start();
			}

			return sLingAgent;
		}
		return sLingAgent;
	}
	
	
	public void setCrashEnabled(boolean enabled) {
		isEnabled = enabled;
	}

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
//		NetWorkHook.instance().stop();
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
		if(enabled) {
			NetWorkHook.instance().start();
		}else {
			NetWorkHook.instance().stop();
		}
	}
	
	public void initUploadServices(String Url, String appKey) {
		 Countly.sharedInstance().setLoggingEnabled(true);
//		 Countly.sharedInstance()
//           .init(mContext, "http://192.168.1.48", "a2bfbd3be423f5e7eeed8bbe3f71fa79c95680cc");
		 
//		 Countly.sharedInstance().init(mContext, Url, appKey);
		 Countly.sharedInstance().init(mContext, Url, appKey, Utils.getDeviceHashId(mContext));
		 
		 setUserData();
		 enableCrashTracking();
		 Countly.sharedInstance().recordEvent("test", 1);
	}
	
	public void enableCrashTracking(){
        //add some custom segments, like dependency library versions
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("Facebook", "3.5");
        data.put("Admob", "6.5");
        Countly.sharedInstance().setCustomCrashSegments(data);
        Countly.sharedInstance().enableCrashReporting();
        
        Countly.sharedInstance().setLocation(44.5888300, 33.5224000);
    }
	
	public void setUserData(){
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("name", "Firstname Lastname");
        data.put("username", "nickname");
        data.put("email", "test@test.com");
        data.put("organization", "Tester");
        data.put("phone", "+123456789");
        data.put("gender", "M");
        //provide url to picture
        //data.put("picture", "http://example.com/pictures/profile_pic.png");
        //or locally from device
        //data.put("picturePath", "/mnt/sdcard/portrait.jpg");
        data.put("byear", "1987");

        //providing any custom key values to store with user
        HashMap<String, String> custom = new HashMap<String, String>();
        custom.put("country", "Turkey");
        custom.put("city", "Istanbul");
        custom.put("address", "My house 11");

        //set multiple custom properties
        Countly.userData.setUserData(data, custom);

        //set custom properties by one
        Countly.userData.setProperty("test", "test");

        //increment used value by 1
        Countly.userData.incrementBy("used", 1);

        //insert value to array of unique values
        Countly.userData.pushUniqueValue("type", "morning");

        //insert multiple values to same property
        Countly.userData.pushUniqueValue("skill", "fire");
        Countly.userData.pushUniqueValue("skill", "earth");

        Countly.userData.save();
    }
}
