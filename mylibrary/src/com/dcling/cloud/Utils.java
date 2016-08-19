package com.dcling.cloud;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Utils {
	private final static String TAG = "Utils";

	private static long totalMemory = 0;

	public static NetInfors GetNetworkTypeAndIP(Context context) {
		NetInfors netRet = null;
		String strNetworkType = "";
		ConnectivityManager manager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			NetworkInfo networkInfo = manager.getActiveNetworkInfo();
			if (networkInfo != null && networkInfo.isConnected()) {

				netRet = new NetInfors();
				netRet.ip = getPhoneIp();

				if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
					strNetworkType = "WIFI";
				} else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
					String _strSubTypeName = networkInfo.getSubtypeName();

					Log.v(TAG, "Network getSubtypeName : " + _strSubTypeName);

					// TD-SCDMA networkType is 17
					int networkType = networkInfo.getSubtype();
					switch (networkType) {
					case TelephonyManager.NETWORK_TYPE_GPRS:
					case TelephonyManager.NETWORK_TYPE_EDGE:
					case TelephonyManager.NETWORK_TYPE_CDMA:
					case TelephonyManager.NETWORK_TYPE_1xRTT:
					case TelephonyManager.NETWORK_TYPE_IDEN: // api<8 : replace
																// by 11
						strNetworkType = "2G";
						break;
					case TelephonyManager.NETWORK_TYPE_UMTS:
					case TelephonyManager.NETWORK_TYPE_EVDO_0:
					case TelephonyManager.NETWORK_TYPE_EVDO_A:
					case TelephonyManager.NETWORK_TYPE_HSDPA:
					case TelephonyManager.NETWORK_TYPE_HSUPA:
					case TelephonyManager.NETWORK_TYPE_HSPA:
					case TelephonyManager.NETWORK_TYPE_EVDO_B: // api<9 replace
																// by 14
					case TelephonyManager.NETWORK_TYPE_EHRPD: // api<11 replace
																// by 12
					case TelephonyManager.NETWORK_TYPE_HSPAP: // api<13 replace
																// by 15
						strNetworkType = "3G";
						break;
					case TelephonyManager.NETWORK_TYPE_LTE: // api<11 replace by
															// 13
						strNetworkType = "4G";
						break;
					default:
						if (_strSubTypeName.equalsIgnoreCase("TD-SCDMA")
								|| _strSubTypeName.equalsIgnoreCase("WCDMA")
								|| _strSubTypeName.equalsIgnoreCase("CDMA2000")) {
							strNetworkType = "3G";
						} else {
							strNetworkType = _strSubTypeName;
						}
						break;
					}
					Log.v(TAG,
							"Network getSubtype : "
									+ Integer.valueOf(networkType).toString());
				}
			}
		} catch (Exception e) {
		}

		netRet.net_type = strNetworkType;
		Log.v(TAG, "Network Type : " + strNetworkType);

		return netRet;
	}

	public static String getDeviceHashId(Context context) {

		//serialNumber
		String serialNumber = android.os.Build.SERIAL;

		//ANDROID_ID
		String android_id = android.provider.Settings.Secure.getString(
				context.getContentResolver(),
				android.provider.Settings.Secure.ANDROID_ID);

		String product = android.os.Build.PRODUCT;

		String brand = android.os.Build.BRAND;

		String fingerPrint = android.os.Build.FINGERPRINT;

		String ret = stringToMD5(serialNumber + android_id + product + brand
				+ fingerPrint);

		return ret;
	}

	private static String stringToMD5(String string) {
		byte[] hash;
		try {
			hash = MessageDigest.getInstance("MD5").digest(
					string.getBytes("UTF-8"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}

		StringBuilder hex = new StringBuilder(hash.length * 2);
		for (byte b : hash) {
			if ((b & 0xFF) < 0x10)
				hex.append("0");
			hex.append(Integer.toHexString(b & 0xFF));
		}

		return hex.toString();
	}

	public static String GetNetworkType(Context context) {
		String strNetworkType = "";
		ConnectivityManager manager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			NetworkInfo networkInfo = manager.getActiveNetworkInfo();
			if (networkInfo != null && networkInfo.isConnected()) {
				if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
					strNetworkType = "WIFI";
				} else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
					String _strSubTypeName = networkInfo.getSubtypeName();

					Log.v(TAG, "Network getSubtypeName : " + _strSubTypeName);

					// TD-SCDMA networkType is 17
					int networkType = networkInfo.getSubtype();
					switch (networkType) {
					case TelephonyManager.NETWORK_TYPE_GPRS:
					case TelephonyManager.NETWORK_TYPE_EDGE:
					case TelephonyManager.NETWORK_TYPE_CDMA:
					case TelephonyManager.NETWORK_TYPE_1xRTT:
					case TelephonyManager.NETWORK_TYPE_IDEN: // api<8 : replace
																// by 11
						strNetworkType = "2G";
						break;
					case TelephonyManager.NETWORK_TYPE_UMTS:
					case TelephonyManager.NETWORK_TYPE_EVDO_0:
					case TelephonyManager.NETWORK_TYPE_EVDO_A:
					case TelephonyManager.NETWORK_TYPE_HSDPA:
					case TelephonyManager.NETWORK_TYPE_HSUPA:
					case TelephonyManager.NETWORK_TYPE_HSPA:
					case TelephonyManager.NETWORK_TYPE_EVDO_B: // api<9 replace
																// by 14
					case TelephonyManager.NETWORK_TYPE_EHRPD: // api<11 replace
																// by 12
					case TelephonyManager.NETWORK_TYPE_HSPAP: // api<13 replace
																// by 15
						strNetworkType = "3G";
						break;
					case TelephonyManager.NETWORK_TYPE_LTE: // api<11 replace by
															// 13
						strNetworkType = "4G";
						break;
					default:
						if (_strSubTypeName.equalsIgnoreCase("TD-SCDMA")
								|| _strSubTypeName.equalsIgnoreCase("WCDMA")
								|| _strSubTypeName.equalsIgnoreCase("CDMA2000")) {
							strNetworkType = "3G";
						} else {
							strNetworkType = _strSubTypeName;
						}
						break;
					}
					Log.v(TAG,
							"Network getSubtype : "
									+ Integer.valueOf(networkType).toString());
				}
			}
		} catch (Exception e) {
		}

		Log.v(TAG, "Network Type : " + strNetworkType);

		return strNetworkType;
	}

	public static String getPhoneIp() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()
							&& inetAddress instanceof Inet4Address) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (Exception e) {
		}
		return "";
	}

	private static long getTotalRAM() {
		if (totalMemory == 0) {
			RandomAccessFile reader = null;
			String load = null;
			try {
				reader = new RandomAccessFile("/proc/meminfo", "r");
				load = reader.readLine();

				// Get the Number value from the string
				Pattern p = Pattern.compile("(\\d+)");
				Matcher m = p.matcher(load);
				String value = "";
				while (m.find()) {
					value = m.group(1);
				}
				try {
					totalMemory = Long.parseLong(value) / 1024;
				} catch (NumberFormatException ex) {
					totalMemory = 0;
				}
			} catch (Exception ex) {
				try {
					if (reader != null) {
						reader.close();
					}
				} catch (IOException exc) {
					exc.printStackTrace();
				}
				ex.printStackTrace();
			} finally {
				try {
					if (reader != null) {
						reader.close();
					}
				} catch (IOException exc) {
					exc.printStackTrace();
				}
			}
		}
		return totalMemory;
	}
	
	 /**
     * Returns the current device openGL version.
     */
    static String getOpenGL(Context context) {
        PackageManager packageManager = context.getPackageManager();
        FeatureInfo[] featureInfos = packageManager.getSystemAvailableFeatures();
        if (featureInfos != null && featureInfos.length > 0) {
            for (FeatureInfo featureInfo : featureInfos) {
                // Null feature name means this feature is the open gl es version feature.
                if (featureInfo.name == null) {
                    if (featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED) {
                        return Integer.toString((featureInfo.reqGlEsVersion & 0xffff0000) >> 16);
                    } else {
                        return "1"; // Lack of property means OpenGL ES version 1
                    }
                }
            }
        }
        return "1";
    }
    
    /**
     * Returns the current device RAM amount.
     */
    static String getRamCurrent(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return Long.toString(getTotalRAM() - (mi.availMem / 1048576L));
    }
    
    /**
     * Returns the total device RAM amount.
     */
    static String getRamTotal(Context context) {
        return Long.toString(getTotalRAM());
    }
    
    /**
     * Returns the current device disk space.
     */
    @TargetApi(18)
    static String getDiskCurrent() {
        if(android.os.Build.VERSION.SDK_INT < 18 ) {
            StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
            long   total  = ((long)statFs.getBlockCount() * (long)statFs.getBlockSize());
            long   free   = ((long)statFs.getAvailableBlocks() * (long)statFs.getBlockSize());
            return Long.toString((total - free)/ 1048576L);
        }
        else{
            StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
            long   total  = (statFs.getBlockCountLong() * statFs.getBlockSizeLong());
            long   free   = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());
            return Long.toString((total - free) / 1048576L);
        }
    }
    
    @TargetApi(18)
    static String getDiskTotal() {
        if(android.os.Build.VERSION.SDK_INT < 18 ) {
            StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
            long   total  = ((long)statFs.getBlockCount() * (long)statFs.getBlockSize());
            return Long.toString(total/ 1048576L);
        }
        else{
            StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
            long   total  = (statFs.getBlockCountLong() * statFs.getBlockSizeLong());
            return Long.toString(total/ 1048576L);
        }
    }
    
    /**
     * Returns the current device battery level.
     */
    static String getBatteryLevel(Context context) {
        try {
            Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(batteryIntent != null) {
                int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                // Error checking that probably isn't needed but I added just in case.
                if (level > -1 && scale > 0) {
                    return Float.toString(((float) level / (float) scale) * 100.0f);
                }
            }
        }
        catch(Exception e){
//            if (Countly.sharedInstance().isLoggingEnabled()) {
//                Log.i(Countly.TAG, "Can't get batter level");
//            }
        }
        return null;
    }
    
    /**
     * Get app's running time before crashing.
     */
    static String getRunningTime() {
       // return Integer.toString(Countly.currentTimestamp() - startTime);
    	return "";
    }

    /**
     * Returns the current device orientation.
     */
    static String getOrientation(Context context) {
        int orientation = context.getResources().getConfiguration().orientation;
        switch(orientation){
            case Configuration.ORIENTATION_LANDSCAPE:
                return "Landscape";
            case Configuration.ORIENTATION_PORTRAIT:
                return "Portrait";
            case Configuration.ORIENTATION_SQUARE:
                return "Square";
            case Configuration.ORIENTATION_UNDEFINED:
                return "Unknown";
            default:
                return null;
        }
    }
    
    /**
     * Checks if device is rooted.
     */
    static String isRooted() {
        String[] paths = { "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su" };
        for (String path : paths) {
            if (new File(path).exists()) return "true";
        }
        return "false";
    }

    /**
     * Checks if device is online.
     */
    static String isOnline(Context context) {
        try {
            ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (conMgr != null && conMgr.getActiveNetworkInfo() != null
                    && conMgr.getActiveNetworkInfo().isAvailable()
                    && conMgr.getActiveNetworkInfo().isConnected()) {

                return "true";
            }
            return "false";
        }
        catch(Exception e){
//            if (Countly.sharedInstance().isLoggingEnabled()) {
//                Log.w(Countly.TAG, "Got exception determining connectivity", e);
//            }
        }
        return null;
    }
}

