package com.dcling.cloud;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Process;
import android.util.Log;

public class CrashHandler implements UncaughtExceptionHandler {
	private static final String TAG = "CrashHandler";
	private static final boolean DEBUG = true;

	// private static final String PATH =
	// Environment.getExternalStorageDirectory().getPath() + "/ryg_test/log/";
	// private static final String PATH =
	// Environment.getDataDirectory().getAbsolutePath() + "/ryg_test/log/";
	private static final String FILE_NAME = "crash";

	// log文件的后缀名
	private static final String FILE_NAME_SUFFIX = ".trace";

	private static CrashHandler sInstance = new CrashHandler();

	// 系统默认的异常处理（默认情况下，系统会终止当前的异常程序）
	private UncaughtExceptionHandler mDefaultCrashHandler;

	private Context mContext;

	// 用来存储设备信息和异常信息
	private LinkedHashMap<String, String> infos = new LinkedHashMap<String, String>();

	// 构造方法私有，防止外部构造多个实例，即采用单例模式
	private CrashHandler() {
	}

	private CrashHandler(Context context) {
	      
	}

	public static CrashHandler getInstance() {
		return sInstance;
	}

	// 这里主要完成初始化工作
	public void init(Context context) {
		// 获取系统默认的异常处理器
		mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
		// 将当前实例设为系统默认的异常处理器
		Thread.setDefaultUncaughtExceptionHandler(this);
		// 获取Context，方便内部使用
		mContext = context.getApplicationContext();
	}

	/**
	 * 这个是最关键的函数，当程序中有未被捕获的异常，系统将会自动调用#uncaughtException方法
	 * thread为出现未捕获异常的线程，ex为未捕获的异常，有了这个ex，我们就可以得到异常信息。
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		try {
			infos.clear();

			// 获取系统时间
			long current = System.currentTimeMillis();
			String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.format(new Date(current));
			infos.put("date", time);

			// serious number号
			infos.put("Serial Number", android.os.Build.SERIAL);

			// ANDROID_ID
			String android_id = android.provider.Settings.Secure.getString(
					mContext.getContentResolver(),
					android.provider.Settings.Secure.ANDROID_ID);
			infos.put("ANDROID_ID", android_id);

			// 收集版本号到infos结构中
//			collectDeviceInfo(mContext);
			
			// 收集其他的信息到infos结构中
			dumpPhoneInfo();
			
			// 收集动态信息到infos结构中
			

			// 导出异常信息到SD卡中
			dumpExceptionToSDCard(ex);

			// 将堆栈奔溃信息写入infos
			if (ex != null) {
				infos.put("CrashHash", Integer.toBinaryString(ex.hashCode()));

				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				ex.printStackTrace(pw);

				infos.put("CrashStack", sw.toString());
			}

			// 这里可以通过网络上传异常信息到服务器，便于开发人员分析日志从而解决bug
			uploadExceptionToServer();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// 打印出当前调用栈信息
		ex.printStackTrace();
		
		// 如果系统提供了默认的异常处理器，则交给系统去结束我们的程序，否则就由我们自己结束自己
		if (mDefaultCrashHandler != null) {
			mDefaultCrashHandler.uncaughtException(thread, ex);
		} else {
			Process.killProcess(Process.myPid());
		}

	}

	/**
	 * 收集设备参数信息
	 * 
	 * @param ctx
	 */
	public void collectDeviceInfo(Context ctx) {
		try {
			PackageManager pm = ctx.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
					PackageManager.GET_ACTIVITIES);

			if (pi != null) {
				String versionName = pi.versionName == null ? "null"
						: pi.versionName;
				String versionCode = pi.versionCode + "";
				infos.put("versionName", versionName);
				infos.put("versionCode", versionCode);
			}
		} catch (NameNotFoundException e) {
			Log.e(TAG, "an error occured when collect package info", e);
		}
		Field[] fields = Build.class.getDeclaredFields();
		for (Field field : fields) {
			try {
				field.setAccessible(true);
				infos.put(field.getName(), field.get(null).toString());
				Log.d(TAG, field.getName() + " : " + field.get(null));
			} catch (Exception e) {
				Log.e(TAG, "an error occured when collect crash info", e);
			}
		}
	}

	private void dumpExceptionToSDCard(Throwable ex) throws IOException {
		// 创建本地文件 /data/data/com.example.test/files/ryg_test/log/
		String PATH = mContext.getFilesDir().getAbsolutePath()
				+ "/ryg_test/log/";

		File dir = new File(PATH);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		long current = System.currentTimeMillis();
		String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
				.format(new Date(current));
		// 以当前时间创建log文件
		File file = new File(PATH + FILE_NAME + time + FILE_NAME_SUFFIX);

		try {
			StringBuffer sb = new StringBuffer();
			for (Map.Entry<String, String> entry : infos.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				sb.append(key + "=" + value + "\n");
			}

			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(
					file)));
			// 导出发生异常的时间
			pw.println(time);

			// 导出手机信息
			dumpPhoneInfo(pw);

			pw.println();

			// 导出infos结构
			pw.print(sb);

			// 导出异常的调用栈信息
			ex.printStackTrace(pw);

			pw.close();
		} catch (Exception e) {
			Log.e(TAG, "dump crash info failed");
		}
	}

	private void dumpPhoneInfo(PrintWriter pw) throws NameNotFoundException {
		// 应用的版本名称和版本号
		PackageManager pm = mContext.getPackageManager();
		PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(),
				PackageManager.GET_ACTIVITIES);
		pw.print("App Version: ");
		pw.print(pi.versionName);
		pw.print('_');
		pw.println(pi.versionCode);

		// android版本号
		pw.print("OS Version: ");
		pw.print(Build.VERSION.RELEASE);
		pw.print("_");
		pw.println(Build.VERSION.SDK_INT);

		// 手机制造商
		pw.print("Vendor: ");
		pw.println(Build.MANUFACTURER);

		// 手机型号
		pw.print("Model: ");
		pw.println(Build.MODEL);

		// cpu架构
		pw.print("CPU ABI: ");
		pw.println(Build.CPU_ABI);
	}

	private void dumpPhoneInfo() {
		// 应用的版本名称和版本号
		PackageManager pm = mContext.getPackageManager();
		PackageInfo pi;
		try {
			pi = pm.getPackageInfo(mContext.getPackageName(),
					PackageManager.GET_ACTIVITIES);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		infos.put("VersionName", pi.versionName);
		infos.put("VersionCode", pi.versionCode +"");
		
		//获取设备基板名称
		infos.put("BOARD", android.os.Build.BOARD);
		
		//获取设备引导程序版本号
		infos.put("BOOTLOADER", android.os.Build.BOOTLOADER);
		
		//获取设备品牌
		infos.put("BRAND", android.os.Build.BRAND);
		
		//获取设备指令集名称
		infos.put("CPU_ABI", android.os.Build.CPU_ABI);
		
		//获取设备第二指令集名称
		infos.put("CPU_ABI2", android.os.Build.CPU_ABI2);
		
		//获取设备驱动名称
		infos.put("DEVICE", android.os.Build.DEVICE);
		
		//设备的唯一标识。由设备的多个信息拼接合成
		infos.put("FINGERPRINT", android.os.Build.FINGERPRINT);
		
		//设备硬件名称,一般和基板名称一样（BOARD）
		infos.put("HARDWARE", android.os.Build.HARDWARE);
		
		//设备主机地址ַ
		infos.put("HOST", android.os.Build.HOST);
		
		//设备版本号
		infos.put("ID", android.os.Build.ID);
		
		//获取手机的型号设备名称
		infos.put("MODEL", android.os.Build.MODEL);
		
		//获取手机设备制造商
		infos.put("MANUFACTURER", android.os.Build.MANUFACTURER);
		
		//获取整个产品的名称
		infos.put("PRODUCT", android.os.Build.PRODUCT);
		
		//时间
		infos.put("TIME", android.os.Build.TIME + "");
		
		//设备版本类型  主要为获取系统版本字符串.如4.1.2 或2.2 或2.3等
		infos.put("RELEASE", android.os.Build.VERSION.RELEASE);
		
		//统的API级别 数字表示
		infos.put("SDK_INT", android.os.Build.VERSION.SDK);
	}

	private void dumpDynamicInfos() {
		
		//获得CPU占用率
//		String cpu = Utils.
//		infos.put("VersionName", pi.versionName);
//		infos.put("VersionCode", pi.versionCode +"");
	}
	
	private void uploadExceptionToServer() {
		new Thread() {
			@Override
			public void run() {
				// String strUrlPath = "http://192.168.1.107:3000/sdk/android";
				//String strUrlPath = "http://192.168.1.49:8081/sdk/android";
				String strUrlPath = "http://192.168.1.49:8081/sdk/mobile/device";
				HttpUtils.submitPostData(strUrlPath, infos, "utf-8");
			}
		}.start();
	}
}
