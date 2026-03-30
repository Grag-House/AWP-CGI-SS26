package de.cgi.innovationlab.temi_app;

import com.robotemi.sdk.listeners.OnRobotReadyListener;

import io.flutter.plugin.common.EventChannel;

public class OnRobotReadyListenerImpl implements com.robotemi.sdk.listeners.OnRobotReadyListener, EventChannel.StreamHandler {
    // private var eventSink: EventChannel.EventSink? = null
    private EventChannel.EventSink eventSink;
    private static final String CHANNNEL_TEMI_READY_STATE_LISTENER = "flutter_temi/on_robot_ready_stream";

    @Override
    public void onRobotReady(boolean isReady) {
        eventSink.success(isReady);
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        this.eventSink = events;
    }

    @Override
    public void onCancel(Object arguments) {

    }
}


