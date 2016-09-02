package com.lingcloud.apptrace.sdk;

import com.lingcloud.apptrace.sdk.store.CrashDetails;
import com.lingcloud.apptrace.sdk.store.DeviceInfo;

public class CommandFactory {
	private DclingCloudAgent lingCloudAgent_;

	public CommandFactory(DclingCloudAgent lingAgent) {
		lingCloudAgent_ = lingAgent;
	}

	public void beginSession() {
		final String data = getHttpHeader() + "&sdk_version="
				+ lingCloudAgent_.LINGCLOUD_APPTRACE_SDK_VERSION_STRING
				+ "&begin_session=1" + "&metrics="
				+ DeviceInfo.getMetrics(lingCloudAgent_.getContext());
		
		lingCloudAgent_.taskQueue_.addTask(data);
	}

	public void updateSession(final int duration) {
		if (duration > 0) {
			final String data = getHttpHeader() + "&session_duration="
					+ duration + "&location=";
//					+ lingCloudAgent_.cgetLingAgentStore().getAndRemoveLocation();

			lingCloudAgent_.taskQueue_.addTask(data);
		}
	}
	
    public void endSession(final int duration) {
        String data = getHttpHeader() + "&end_session=1";
        if (duration > 0) {
            data += "&session_duration=" + duration;
        }

        lingCloudAgent_.taskQueue_.addTask(data);
    }
    
    public void sendUserData() {
        String userdata = lingCloudAgent_.userData.getDataForRequest();

        if(!userdata.equals("")){
            String data = getHttpHeader() + userdata;
            
            lingCloudAgent_.taskQueue_.addTask(data);
        }
    }

   public void recordEvents(final String events) {
        final String data = getHttpHeader() + "&events=" + events;
        lingCloudAgent_.taskQueue_.addTask(data);
    }

    void recordLocation(final String events) {
        final String data = getHttpHeader() + "&events=" + events;
        lingCloudAgent_.taskQueue_.addTask(data);
    }
    
    public void sendCrashReport(String error, boolean nonfatal) {
		final String crashString = getHttpHeader() + "&sdk_version="
				+ lingCloudAgent_.LINGCLOUD_APPTRACE_SDK_VERSION_STRING
				+ "&crash="
				+ CrashDetails.getCrashData(lingCloudAgent_.getContext(), error, nonfatal);

		lingCloudAgent_.taskQueue_.addTask(crashString);
	}
    
	public String getHttpHeader() {
		final String data = "app_key=" + lingCloudAgent_.appKey_
				+ "&timestamp=" + Utils.currentTimestamp() + "&hour="
				+ Utils.currentHour() + "&dow=" + Utils.currentDayOfWeek()
				+ "&device_id="
				+ Utils.getDeviceHashId(lingCloudAgent_.getContext());

		return data;
	}
}
