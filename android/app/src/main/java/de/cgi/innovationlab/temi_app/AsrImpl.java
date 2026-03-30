package de.cgi.innovationlab.temi_app;

import androidx.annotation.NonNull;

import com.robotemi.sdk.Robot;

import java.util.HashMap;

import io.flutter.plugin.common.EventChannel;

public class AsrImpl implements Robot.AsrListener, EventChannel.StreamHandler {
    private EventChannel.EventSink eventSink = null;

    public String getSTREAM_CHANNEL_NAME(){
        return "flutter_temi/asr_stream";
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        this.eventSink = events;
    }
    @Override
    public void onCancel(Object arguments) {
        this.eventSink = null;
    }

    @Override
    public void onAsrResult(@NonNull String asrResult) {
        HashMap<String, Object> response = new HashMap<>(4);
        response.put("asrResult", asrResult);
        if (eventSink != null) {
            eventSink.success(response);
            System.out.println("ASR-Result: " + asrResult);
        }
        else {
            System.out.println("eventSink is null");
        }
    }
}