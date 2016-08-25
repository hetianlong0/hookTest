package com.example.test1;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.dcling.cloud.DclingCloudAgent;
import com.dcling.cloud.countly2.Countly;
import com.taobao.dexposed.NetWorkHook;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

import android.view.Choreographer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";
	public static final int SHOW_RESPONSE = 0;
	
	private DclingCloudAgent lingAgent;
	
	private TextView tv_1 = null;
	private Button btCreateData = null;
	private Button btInsertData = null;
	private Button btViewData = null;
	private Button btDelOne = null;
	private Button btClearAll = null;
	
	private Button btSendHookCommand = null;

	private ClickViewHandler viewHandler = new ClickViewHandler();
	
	private MyHandler mHandler = new MyHandler();
	
	private class MyHandler extends Handler {
		 @Override
		  public void handleMessage(Message message) {
			 Toast.makeText(MainActivity.this, "handle Message!", Toast.LENGTH_LONG).show();
		    switch (message.what) {
		      case 1:
		        Log.d(TAG, "Got restart preview message");
		        break;
		    }
		 }
	}
	
	NutFrameCallback nutFrameCallback = new NutFrameCallback();
	
	//新建Handler的对象，在这里接收Message，然后更新TextView控件的内容
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case SHOW_RESPONSE:
                String response = (String) msg.obj;
    //            textView_response.setText(response);
    //            Log.d("TAG, "aaa ");
               int a = 0;
            default:
                break;
            }            
        }

    };
	
	
	
//	public static String GetNetworkType(Context context) {
//		String strNetworkType = "";
//		ConnectivityManager manager = (ConnectivityManager) context
//				.getSystemService(Context.CONNECTIVITY_SERVICE);
//		try {
//			NetworkInfo networkInfo = manager.getActiveNetworkInfo();
//			if (networkInfo != null && networkInfo.isConnected()) {
//				
//				String ip = getPhoneIp();
//				
//				if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
//					strNetworkType = "WIFI";
//				} else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
//					String _strSubTypeName = networkInfo.getSubtypeName();
//
//					Log.v(TAG, "Network getSubtypeName : " + _strSubTypeName);
//
//					// TD-SCDMA networkType is 17
//					int networkType = networkInfo.getSubtype();
//					switch (networkType) {
//					case TelephonyManager.NETWORK_TYPE_GPRS:
//					case TelephonyManager.NETWORK_TYPE_EDGE:
//					case TelephonyManager.NETWORK_TYPE_CDMA:
//					case TelephonyManager.NETWORK_TYPE_1xRTT:
//					case TelephonyManager.NETWORK_TYPE_IDEN: // api<8 : replace by 11
//						strNetworkType = "2G";
//						break;
//					case TelephonyManager.NETWORK_TYPE_UMTS:
//					case TelephonyManager.NETWORK_TYPE_EVDO_0:
//					case TelephonyManager.NETWORK_TYPE_EVDO_A:
//					case TelephonyManager.NETWORK_TYPE_HSDPA:
//					case TelephonyManager.NETWORK_TYPE_HSUPA:
//					case TelephonyManager.NETWORK_TYPE_HSPA:
//					case TelephonyManager.NETWORK_TYPE_EVDO_B: // api<9 replace by 14
//					case TelephonyManager.NETWORK_TYPE_EHRPD:  // api<11 replace by 12
//					case TelephonyManager.NETWORK_TYPE_HSPAP:  // api<13 replace by 15
//						strNetworkType = "3G";
//						break;
//					case TelephonyManager.NETWORK_TYPE_LTE:  // api<11 replace by 13
//						strNetworkType = "4G";
//						break;
//					default:
//						if (_strSubTypeName.equalsIgnoreCase("TD-SCDMA")
//								|| _strSubTypeName.equalsIgnoreCase("WCDMA")
//								|| _strSubTypeName.equalsIgnoreCase("CDMA2000")) {
//							strNetworkType = "3G";
//						} else {
//							strNetworkType = _strSubTypeName;
//						}
//						break;
//					}
//					Log.v(TAG,
//							"Network getSubtype : "
//									+ Integer.valueOf(networkType).toString());
//				}
//			}
//		} catch (Exception e) {
//		}
//
//		Log.v(TAG, "Network Type : " + strNetworkType);
//
//		return strNetworkType;
//	}
	
//	/**
//	 * 获得本机IP地址
//	 * @return
//	 */
//	public static String getPhoneIp() {
//		try {
//			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
//				NetworkInterface intf = en.nextElement();
//				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
//					InetAddress inetAddress = enumIpAddr.nextElement();
//					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
//						return inetAddress.getHostAddress().toString();
//					}
//				}
//			}
//		} catch (Exception e) {
//		}
//		return "";
//	}
	
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Choreographer.getInstance().postFrameCallback(nutFrameCallback);
		 
		lingAgent = DclingCloudAgent.getInstace(this);
		lingAgent.setCrashEnabled(true);
		String url = "http://192.168.1.48";
		String app_key = "a2bfbd3be423f5e7eeed8bbe3f71fa79c95680cc";
		lingAgent.initUploadServices(url, app_key);
		
		Countly.onCreate(this);
		
		tv_1 = (TextView)findViewById(R.id.tv_1);
		btCreateData = (Button) findViewById(R.id.Button01);
		btCreateData.setOnClickListener(viewHandler);
		btInsertData = (Button) findViewById(R.id.Button02);
		btInsertData.setOnClickListener(viewHandler);
		btDelOne = (Button) findViewById(R.id.Button03);
		btDelOne.setOnClickListener(new ClickViewHandler());
		btClearAll = (Button) findViewById(R.id.Button04);
		btClearAll.setOnClickListener(new ClickViewHandler());
		btViewData = (Button) findViewById(R.id.Button05);
		btViewData.setOnClickListener(new ClickViewHandler());
		btSendHookCommand = (Button)findViewById(R.id.Button06);
		btSendHookCommand.setOnClickListener(new ClickViewHandler());
		
//		GetNetworkType(this.getBaseContext());
		
		float cpuDo = getProcessCpuRate();
		long time = getTotalCpuTime();
		long app = getAppCpuTime();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private class ClickViewHandler implements OnClickListener {
		
		private void DelAllPeople() {
			Log.d(TAG, "ret = ");
		}

		private void DelOne() {
			
		}

		private void ViewRecords() {

		}
		private void InsertSomeRecords() {
			
		}

		@Override
		public void onClick(View v) {
			if (v == btInsertData) {
				String array[] = {"one", "two", "three"};
				String crashHere = array[5];
			} else if (v == btDelOne) {
				lingAgent.sendDeviceInfo();
			} else if (v == btClearAll) {	//发送设备信息
				lingAgent.sendDeviceInfo();
			} else if (v == btViewData) {	//hook http 请求
				String url = "http://www.sina.com.cn";
				hookHttpClientStart(url);
			} else if (v == btSendHookCommand) {
				lingAgent.hookNetWorkEnable(true);
				lingAgent.sendHookHttpCommand();
			}
		}
	}

	public static float getProcessCpuRate(){
        float totalCpuTime1 = getTotalCpuTime();
        float processCpuTime1 = getAppCpuTime();
        try{
            Thread.sleep(360);
        }
        catch (Exception e){
        }

        float totalCpuTime2 = getTotalCpuTime();
        float processCpuTime2 = getAppCpuTime();

        float cpuRate = 100 * (processCpuTime2 - processCpuTime1)
                / (totalCpuTime2 - totalCpuTime1);
        
        return cpuRate;
    }

    public static long getTotalCpuTime(){
    	//
        String[] cpuInfos = null;
        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        long totalCpu = Long.parseLong(cpuInfos[2])
                + Long.parseLong(cpuInfos[3]) + Long.parseLong(cpuInfos[4])
                + Long.parseLong(cpuInfos[6]) + Long.parseLong(cpuInfos[5])
                + Long.parseLong(cpuInfos[7]) + Long.parseLong(cpuInfos[8]);
        return totalCpu;
    }

    public static long getAppCpuTime(){   
    	// 获得App Cpu 占用率
        String[] cpuInfos = null;
        try{
            int pid = android.os.Process.myPid();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/" + pid + "/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        long appCpuTime = Long.parseLong(cpuInfos[13])
                + Long.parseLong(cpuInfos[14]) + Long.parseLong(cpuInfos[15])
                + Long.parseLong(cpuInfos[16]);
        return appCpuTime;
    }
    
    public void hookHttpClientStart(final String url) {
    	lingAgent.hookNetWorkEnable(true);
		Log.d("hook_http_client", "runPatchApk button clicked.");
		  new Thread(new Runnable() {
	            @Override
	            public void run() {
	                //用HttpClient发送请求，分为五步
	                //第一步：创建HttpClient对象
	                HttpClient httpCient = new DefaultHttpClient();
	                //第二步：创建代表请求的对象,参数是访问的服务器地址
	                HttpGet httpGet = new HttpGet(url);
	                
	                try {
	                    //第三步：执行请求，获取服务器发还的相应对象
	                    HttpResponse httpResponse = httpCient.execute(httpGet);
	                    //第四步：检查相应的状态是否正常：检查状态码的值是200表示正常
	                    if (httpResponse.getStatusLine().getStatusCode() == 200) {
	                        //第五步：从相应对象当中取出数据，放到entity当中
	                        HttpEntity entity = httpResponse.getEntity();
	                        String response = EntityUtils.toString(entity,"utf-8");//将entity当中的数据转换为字符串
	                        
	                        //在子线程中将Message对象发出去
	                        Message message = new Message();
	                        message.what = SHOW_RESPONSE;
	                        message.obj = response.toString();
	                        handler.sendMessage(message);
	                    }
	                } catch (Exception e) {
	                    // TODO Auto-generated catch block
	                    e.printStackTrace();
	                }
	            }
	        }).start();//这个start()方法不要忘记了           
	}
    
    @SuppressLint("NewApi")
	private static  class NutFrameCallback implements Choreographer.FrameCallback {

        static final NutFrameCallback callback = new NutFrameCallback();
        private long mLastFrameTimeNanos = 0;
        private long mFrameIntervalNanos = (long)(500000000) - 1;
        @Override
        public void doFrame(long frameTimeNanos) {
            if (mLastFrameTimeNanos != 0) {
                final long jitterNanos = frameTimeNanos - mLastFrameTimeNanos;
                if (jitterNanos > mFrameIntervalNanos) {
                  int a = 0;
                  a++;
                }
            }
            mLastFrameTimeNanos = frameTimeNanos;

            Choreographer.getInstance().postFrameCallback(NutFrameCallback.callback);
        }
    }
    
    @Override
    public void onStart()
    {
        super.onStart();
        Countly.sharedInstance().onStart(this);
    }

    @Override
    public void onStop()
    {
        Countly.sharedInstance().onStop();
        super.onStop();
    }
}
