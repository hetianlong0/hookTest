package com.lingcloud.apptrace.sdk.store;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import com.lingcloud.apptrace.sdk.DclingCloudAgent;
import com.lingcloud.apptrace.sdk.Utils;

public class ConnectionQueue {
    private lingAgentStore store_;
    private ExecutorService executor_;
    private String appKey_;
    private Context context_;
    private String serverURL_;
    private Future<?> connectionProcessorFuture_;
    private DeviceId deviceId_;
    private SSLContext sslContext_;

    // Getters are for unit testing
    String getAppKey() {
        return appKey_;
    }

    public void setAppKey(final String appKey) {
        appKey_ = appKey;
    }

    Context getContext() {
        return context_;
    }

    public void setContext(final Context context) {
        context_ = context;
    }

    String getServerURL() {
        return serverURL_;
    }

    public void setServerURL(final String serverURL) {
        serverURL_ = serverURL;

        if (DclingCloudAgent.publicKeyPinCertificates == null) {
            sslContext_ = null;
        } else {
            try {
                TrustManager tm[] = { new CertificateTrustManager(DclingCloudAgent.publicKeyPinCertificates) };
                sslContext_ = SSLContext.getInstance("TLS");
                sslContext_.init(null, tm, null);
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public lingAgentStore getLingAgentStore() {
        return store_;
    }

    public void setLingAgentStore(final lingAgentStore agentStore) {
        store_ = agentStore;
    }

    DeviceId getDeviceId() { return deviceId_; }

    public void setDeviceId(DeviceId deviceId) {
        this.deviceId_ = deviceId;
    }

    /**
     * Checks internal state and throws IllegalStateException if state is invalid to begin use.
     * @throws IllegalStateException if context, app key, store, or server URL have not been set
     */
    void checkInternalState() {
        if (context_ == null) {
            throw new IllegalStateException("context has not been set");
        }
        if (appKey_ == null || appKey_.length() == 0) {
            throw new IllegalStateException("app key has not been set");
        }
        if (store_ == null) {
            throw new IllegalStateException("countly store has not been set");
        }
        if (serverURL_ == null || !DclingCloudAgent.isValidURL(serverURL_)) {
            throw new IllegalStateException("server URL is not valid");
        }
        if (DclingCloudAgent.publicKeyPinCertificates != null && !serverURL_.startsWith("https")) {
            throw new IllegalStateException("server must start with https once you specified public keys");
        }
    }

    /**
     * Records a session start event for the app and sends it to the server.
     * @throws IllegalStateException if context, app key, store, or server URL have not been set
     */
    void beginSession() {
        checkInternalState();
        final String data = "app_key=" + appKey_
                          + "&timestamp=" + Utils.currentTimestamp()
                          + "&hour=" + Utils.currentHour()
                          + "&dow=" + Utils.currentDayOfWeek()
                          + "&sdk_version=" + DclingCloudAgent.LINGCLOUD_APPTRACE_SDK_VERSION_STRING
                          + "&begin_session=1"
                          + "&metrics=" + DeviceInfo.getMetrics(context_);

        store_.addConnection(data);

        tick();
    }

    /**
     * Records a session duration event for the app and sends it to the server. This method does nothing
     * if passed a negative or zero duration.
     * @param duration duration in seconds to extend the current app session, should be more than zero
     * @throws IllegalStateException if context, app key, store, or server URL have not been set
     */
    public void updateSession(final int duration) {
//        checkInternalState();
//        if (duration > 0) {
//            final String data = "app_key=" + appKey_
//                              + "&timestamp=" + MainBridge.currentTimestamp()
//                              + "&hour=" + MainBridge.currentHour()
//                              + "&dow=" + MainBridge.currentDayOfWeek()
//                              + "&session_duration=" + duration
//                              + "&location=" + this.getLingAgentStore().getAndRemoveLocation();
//
//            store_.addConnection(data);
//
//            tick();
//        }
    }

//    public void tokenSession(String token, MainBridge.CountlyMessagingMode mode) {
//        checkInternalState();
//
//        final String data = "app_key=" + appKey_
//                + "&" + "timestamp=" + MainBridge.currentTimestamp()
//                + "&hour=" + MainBridge.currentHour()
//                + "&dow=" + MainBridge.currentDayOfWeek()
//                + "&" + "token_session=1"
//                + "&" + "android_token=" + token
//                + "&" + "test_mode=" + (mode == MainBridge.CountlyMessagingMode.TEST ? 2 : 0)
//                + "&" + "locale=" + DeviceInfo.getLocale();
//
//        // To ensure begin_session will be fully processed by the server before token_session
//        final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
//        worker.schedule(new Runnable() {
//            @Override
//            public void run() {
//                store_.addConnection(data);
//                tick();
//            }
//        }, 10, TimeUnit.SECONDS);
//    }

    /**
     * Records a session end event for the app and sends it to the server. Duration is only included in
     * the session end event if it is more than zero.
     * @param duration duration in seconds to extend the current app session
     * @throws IllegalStateException if context, app key, store, or server URL have not been set
     */
    void endSession(final int duration) {
//        checkInternalState();
//        String data = "app_key=" + appKey_
//                    + "&timestamp=" + MainBridge.currentTimestamp()
//                    + "&hour=" + MainBridge.currentHour()
//                    + "&dow=" + MainBridge.currentDayOfWeek()
//                    + "&end_session=1";
//        if (duration > 0) {
//            data += "&session_duration=" + duration;
//        }
//
//        store_.addConnection(data);
//
//        tick();
    }

    /**
     * Send user data to the server.
     * @throws java.lang.IllegalStateException if context, app key, store, or server URL have not been set
     */
    void sendUserData() {
        checkInternalState();
//        String userdata = UserData.getDataForRequest();
//
//        if(!userdata.equals("")){
//            String data = "app_key=" + appKey_
//                    + "&timestamp=" + MainBridge.currentTimestamp()
//                    + "&hour=" + MainBridge.currentHour()
//                    + "&dow=" + MainBridge.currentDayOfWeek()
//                    + userdata;
//            store_.addConnection(data);
//
//            tick();
//        }
    }

    /**
     * Attribute installation to Countly server.
     * @param referrer query parameters
     * @throws java.lang.IllegalStateException if context, app key, store, or server URL have not been set
     */
    void sendReferrerData(String referrer) {
//        checkInternalState();
//
//        if(referrer != null){
//            String data = "app_key=" + appKey_
//                    + "&timestamp=" + MainBridge.currentTimestamp()
//                    + "&hour=" + MainBridge.currentHour()
//                    + "&dow=" + MainBridge.currentDayOfWeek()
//                    + referrer;
//            store_.addConnection(data);
//
//            tick();
//        }
    }

    /**
     * Reports a crash with device data to the server.
     * @throws IllegalStateException if context, app key, store, or server URL have not been set
     */
    void sendCrashReport(String error, boolean nonfatal) {
//        checkInternalState();
//        final String data = "app_key=" + appKey_
//                + "&timestamp=" + MainBridge.currentTimestamp()
//                + "&hour=" + MainBridge.currentHour()
//                + "&dow=" + MainBridge.currentDayOfWeek()
//                + "&sdk_version=" + MainBridge.COUNTLY_SDK_VERSION_STRING
//                + "&crash=" + CrashDetails.getCrashData(context_, error, nonfatal);
//
//        store_.addConnection(data);
//
//        tick();
    }

    /**
     * Records the specified events and sends them to the server.
     * @param events URL-encoded JSON string of event data
     * @throws IllegalStateException if context, app key, store, or server URL have not been set
     */
    public void recordEvents(final String events) {
//        checkInternalState();
//        final String data = "app_key=" + appKey_
//                          + "&timestamp=" + MainBridge.currentTimestamp()
//                          + "&hour=" + MainBridge.currentHour()
//                          + "&dow=" + MainBridge.currentDayOfWeek()
//                          + "&events=" + events;
//
//        store_.addConnection(data);
//
//        tick();
    }

    /**
     * Records the specified events and sends them to the server.
     * @param events URL-encoded JSON string of event data
     * @throws IllegalStateException if context, app key, store, or server URL have not been set
     */
    void recordLocation(final String events) {
//        checkInternalState();
//        final String data = "app_key=" + appKey_
//                          + "&timestamp=" + MainBridge.currentTimestamp()
//                          + "&hour=" + MainBridge.currentHour()
//                          + "&dow=" + MainBridge.currentDayOfWeek()
//                          + "&events=" + events;
//
//        store_.addConnection(data);
//
//        tick();
    }

    /**
     * Ensures that an executor has been created for ConnectionProcessor instances to be submitted to.
     */
    void ensureExecutor() {
        if (executor_ == null) {
            executor_ = Executors.newSingleThreadExecutor();
        }
    }

    /**
     * Starts ConnectionProcessor instances running in the background to
     * process the local connection queue data.
     * Does nothing if there is connection queue data or if a ConnectionProcessor
     * is already running.
     */
    void tick() {
        if (!store_.isEmptyConnections() && (connectionProcessorFuture_ == null || connectionProcessorFuture_.isDone())) {
            ensureExecutor();
            connectionProcessorFuture_ = executor_.submit(new ConnectionProcessor(serverURL_, store_, deviceId_, sslContext_));
        }
    }

    // for unit testing
    ExecutorService getExecutor() { return executor_; }
    void setExecutor(final ExecutorService executor) { executor_ = executor; }
    Future<?> getConnectionProcessorFuture() { return connectionProcessorFuture_; }
    void setConnectionProcessorFuture(final Future<?> connectionProcessorFuture) { connectionProcessorFuture_ = connectionProcessorFuture; }

}
