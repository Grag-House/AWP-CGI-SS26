package de.cgi.innovationlab.temi_app;

import androidx.annotation.NonNull;

import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;

import java.util.HashMap;

import io.flutter.plugin.common.EventChannel;

public class GoToLocationStatusChangedImpl implements OnGoToLocationStatusChangedListener, EventChannel.StreamHandler {
    private EventChannel.EventSink eventSink = null;

    @Override
    public void onGoToLocationStatusChanged(@NonNull String location, @NonNull String status, int descriptionId, @NonNull String description) {
        HashMap<String, Object> response = new HashMap<String, Object>(4);
        response.put("location", location);
        response.put("status", status);
        response.put("descriptionId", descriptionId);
        response.put("description", description);
        System.out.println(response.toString());
        if (eventSink != null)
            eventSink.success(response);
        else {
            System.out.println("eventSink is null");
        }
    }

    @Override
    public void onListen(Object arguments, @NonNull EventChannel.EventSink events) {
        this.eventSink = events;
    }

    @Override
    public void onCancel(Object arguments) {
        this.eventSink = null;
    }

    public String getSTREAM_CHANNEL_NAME() {
        return "flutter_temi/on_location_status_stream";
    }
}
