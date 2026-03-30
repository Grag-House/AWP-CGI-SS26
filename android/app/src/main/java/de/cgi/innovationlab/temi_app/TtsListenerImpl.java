package de.cgi.innovationlab.temi_app;

import androidx.annotation.NonNull;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;

import java.util.HashMap;

import io.flutter.plugin.common.EventChannel;

public class TtsListenerImpl implements Robot.TtsListener, EventChannel.StreamHandler {
    private EventChannel.EventSink eventSink = null;

    public String getSTREAM_CHANNEL_NAME(){
        return "flutter_temi/tts_stream";
    }
    @Override
    public void onTtsStatusChanged(@NonNull TtsRequest ttsRequest) {
        HashMap<String, Object> ttsRequestMap = new HashMap<>(3);
        ttsRequestMap.put("status", ttsRequest.getStatus().toString().toLowerCase());
        ttsRequestMap.put("id", ttsRequest.getId().toString());
        ttsRequestMap.put("speech", ttsRequest.getSpeech());
        if(eventSink != null)
            this.eventSink.success(ttsRequestMap);
    }
    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        this.eventSink = events;
    }
    @Override
    public void onCancel(Object arguments) {
        this.eventSink = null;
    }
}
