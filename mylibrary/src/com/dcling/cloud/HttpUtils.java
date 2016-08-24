package com.dcling.cloud;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Map;
import java.io.IOException;
import java.net.URLEncoder;
import java.io.ByteArrayOutputStream;

import org.json.JSONException;
import org.json.JSONObject;

public class HttpUtils {
	
	public static String submitPostData(String strUrlPath,
			Map<String, String> params, String encode) {

//		byte[] data = getRequestData(params, encode).toString().getBytes();
		StringBuffer buffer = getRequestData(params, encode);
		byte[] data = buffer.toString().getBytes();
		try {
			
			URL url = new URL(strUrlPath);

			HttpURLConnection httpURLConnection = (HttpURLConnection) url
					.openConnection();
			httpURLConnection.setConnectTimeout(3000);
			httpURLConnection.setDoInput(true);
			httpURLConnection.setDoOutput(true);
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setUseCaches(false);
			
			httpURLConnection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			
			httpURLConnection.setRequestProperty("Content-Length",
					String.valueOf(data.length));
			
			OutputStream outputStream = httpURLConnection.getOutputStream();
			outputStream.write(data);

			int response = httpURLConnection.getResponseCode();
			if (response == HttpURLConnection.HTTP_OK) {
				InputStream inptStream = httpURLConnection.getInputStream();
				String result = dealResponseResult(inptStream);
				
				try {
					JSONObject ret = new JSONObject(result);
					int code = ret.optInt("result");
					
					//1 成功
					if( code == 1) {
						return "1";
					}
					return "0";
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// e.printStackTrace();
			return "err: " + e.getMessage().toString();
		}
		return "-1";
	}
	
	public static String submitPostJSONData(String strUrlPath,
			JSONObject param) {
		byte[] data = param.toString().getBytes();
		try {
			
			URL url = new URL(strUrlPath);

			HttpURLConnection httpURLConnection = (HttpURLConnection) url
					.openConnection();
			httpURLConnection.setConnectTimeout(3000); 
			httpURLConnection.setDoInput(true);
			httpURLConnection.setDoOutput(true);
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setUseCaches(false);
			
			httpURLConnection.setRequestProperty("Content-Type", "application/json");
			
			httpURLConnection.connect();
			
			DataOutputStream outputStream = new DataOutputStream(httpURLConnection
				     .getOutputStream());
						   
			outputStream.write(data, 0, data.length);
			outputStream.flush();
			outputStream.close(); // flush and close

			int response = httpURLConnection.getResponseCode();
			if (response == HttpURLConnection.HTTP_OK) {
				InputStream inptStream = httpURLConnection.getInputStream();
				String result = dealResponseResult(inptStream);
				
				try {
					JSONObject ret = new JSONObject(result);
					int code = ret.optInt("result");
					
					//1 成功
					if( code == 1) {
						return "1";
					}
					return "0";
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			// e.printStackTrace();
			return "err: " + e.getMessage().toString();
		}
		return "-1";
	}

	public static StringBuffer getRequestData(Map<String, String> params,
			String encode) {
		StringBuffer stringBuffer = new StringBuffer();
		try {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				stringBuffer.append(entry.getKey()).append("=")
						.append(URLEncoder.encode(entry.getValue(), encode))
						.append("&");
			}
			stringBuffer.deleteCharAt(stringBuffer.length() - 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stringBuffer;
	}

	public static String dealResponseResult(InputStream inputStream) {
		String resultData = null;
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] data = new byte[1024*16];
		int len = 0;
		try {
			while ((len = inputStream.read(data)) != -1) {
				byteArrayOutputStream.write(data, 0, len);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		resultData = new String(byteArrayOutputStream.toByteArray());
		return resultData;
	}
}