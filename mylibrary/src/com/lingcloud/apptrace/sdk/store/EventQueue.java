package com.lingcloud.apptrace.sdk.store;

import org.json.JSONArray;

import com.lingcloud.apptrace.sdk.Utils;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public class EventQueue {
    private final lingAgentStore lingStore_;

    /**
     * Constructs an EventQueue.
     * @param lingAgentStore backing store to be used for local event queue persistence
     */
    public EventQueue(final lingAgentStore Store) {
        lingStore_ = Store;
    }

    /**
     * Returns the number of events in the local event queue.
     * @return the number of events in the local event queue
     */
    public int size() {
        return lingStore_.events().length;
   }

    /**
     * Removes all current events from the local queue and returns them as a
     * URL-encoded JSON string that can be submitted to a ConnectionQueue.
     * @return URL-encoded JSON string of event data from the local event queue
     */
    public String events() {
        String result;

        final List<Event> events = lingStore_.eventsList();

        final JSONArray eventArray = new JSONArray();
        for (Event e : events) {
            eventArray.put(e.toJSON());
        }

        result = eventArray.toString();

        lingStore_.removeEvents(events);

        try {
           result  = java.net.URLEncoder.encode(result, "UTF-8");
            //htl add;
//            result  = java.net.URLEncoder.encode(result, "ASCII");
        } catch (UnsupportedEncodingException e) {
            // should never happen because Android guarantees UTF-8 support
        }


        return result;
    }

    /**
     * Records a custom Count.ly event to the local event queue.
     * @param key name of the custom event, required, must not be the empty string
     * @param segmentation segmentation values for the custom event, may be null
     * @param count count associated with the custom event, should be more than zero
     * @param sum sum associated with the custom event, if not used, pass zero.
     *            NaN and infinity values will be quietly ignored.
     * @throws IllegalArgumentException if key is null or empty
     */
    public void recordEvent(final String key, final Map<String, String> segmentation, final int count, final double sum, final double dur) {
        final int timestamp = Utils.currentTimestamp();
        final int hour = Utils.currentHour();
        final int dow = Utils.currentDayOfWeek();
        lingStore_.addEvent(key, segmentation, timestamp, hour, dow, count, sum, dur);
    }

    void recordEvent(final Event event) {
        event.hour = Utils.currentHour();
        event.dow = Utils.currentDayOfWeek();
        lingStore_.addEvent(event);
    }


        // for unit tests
    lingAgentStore getLingAgentStore() {
        return lingStore_;
    }
}
