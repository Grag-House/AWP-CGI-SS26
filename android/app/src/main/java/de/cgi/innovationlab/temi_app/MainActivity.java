package de.cgi.innovationlab.temi_app;

import androidx.annotation.NonNull;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.navigation.model.SpeedLevel;

import java.util.ArrayList;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity implements FlutterPlugin, MethodChannel.MethodCallHandler {
    private static final String CHANNEL_TEMI = "flutter_temi";
    private static final String CHANNEL_TEMI_COMMANDS = CHANNEL_TEMI + "/flutter_temi";
//    private final Robot robot = Robot.getInstance();
    private Robot robot;
    private final GoToLocationStatusChangedImpl goToLocationStatusChanged = new GoToLocationStatusChangedImpl();
    private final TtsListenerImpl ttsListener = new TtsListenerImpl();
    private final AsrImpl asrImpl = new AsrImpl();
    private final String onLocationStatusChangeEventChannelName = goToLocationStatusChanged.getSTREAM_CHANNEL_NAME();
    private final String ttsListenerEventChannelName = ttsListener.getSTREAM_CHANNEL_NAME();
    private final String asrEventChannelName = asrImpl.getSTREAM_CHANNEL_NAME();


    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {

        try {
            robot = Robot.getInstance();
        } catch (Exception e) {
            robot = null;
        }

        System.out.println("configure flutter engine");
        EventChannel onLocationStatusChangeEventChannel = new EventChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), onLocationStatusChangeEventChannelName);
        onLocationStatusChangeEventChannel.setStreamHandler(this.goToLocationStatusChanged);

        EventChannel ttsListenerEventChannel = new EventChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), ttsListenerEventChannelName);
        ttsListenerEventChannel.setStreamHandler(this.ttsListener);

        EventChannel asrEventChannel = new EventChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), asrEventChannelName);
        asrEventChannel.setStreamHandler(this.asrImpl);

        if (robot != null) {
            robot.addOnGoToLocationStatusChangedListener(goToLocationStatusChanged);
            robot.addTtsListener(ttsListener);
            robot.addAsrListener(asrImpl);
            robot.requestToBeKioskApp();
        } else {
            System.out.println("temi listeners not registered because robot is null");
        }
//        robot.addAsrListener(new Robot.AsrListener() {
//            @Override
//            public void onAsrResult(@NonNull String s) {
//                System.out.println(s);
//            }
//        });

        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL_TEMI_COMMANDS)
                .setMethodCallHandler(
                        (call, result) -> {
                            System.out.println("Call: " + call.arguments.toString() + " , Method: " + call.method);
                            if (!call.method.equals("")) {
                                if (call.method.equals("temi_goto")) {
                                    ArrayList<String> arguments = call.arguments();
                                    //System.out.println("Type: " + call.arguments.getClass().toString() + ", " + call.arguments.getClass().getName());
                                    //System.out.println("Size: " + arguments.size());
                                    if (arguments.size() == 2) {
                                        if (SpeedLevel.HIGH.toString().equals(arguments.get(1)))
                                            robot.goTo(arguments.get(0), false, true, SpeedLevel.HIGH);
                                        if (SpeedLevel.MEDIUM.toString().equals(arguments.get(1)))
                                            robot.goTo(arguments.get(0), false, true, SpeedLevel.MEDIUM);
                                        if (SpeedLevel.SLOW.toString().equals(arguments.get(1)))
                                            robot.goTo(arguments.get(0), false, true, SpeedLevel.SLOW);
                                        else {
                                            System.out.println("no speed level matched");
                                        }
                                    } else {
                                        robot.goTo(arguments.get(0));
                                    }
                                    // robot.goTo(call.arguments().toString());
                                }
                                if (call.method.equals("temi_stop_movement")) {
                                    robot.stopMovement();
                                }
                                if (call.method.equals("temi_speak")) {
                                    TtsRequest ttsRequest = TtsRequest.create(call.arguments().toString(), true);
                                    robot.speak(ttsRequest);
                                }
                                if (call.method.equals("temi_get_locations")) {
                                    System.out.println(robot.getLocations());
                                    result.success(robot.getLocations());
                                }
                                if (call.method.equals("temi_get_ready_state")) {
                                    System.out.println(robot.isReady());
                                    result.success(robot.isReady());
                                }
                                if (call.method.equals("temi_playsequence")) {
                                    robot.playSequence(call.arguments().toString());
                                }
                                if (call.method.equals("temi_getAllSequences")) {
                                    //result.success(robot.getAllSequences());
                                }
                                if (call.method.equals("temi_wake_up")) {
                                    robot.wakeup();
                                }
                                if (call.method.equals("temi_follow")) {
                                    robot.beWithMe();
                                }
                            }
                        }
                );

    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        MethodChannel channel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL_TEMI);
//        FlutterTemiPlugin plugin = new FlutterTemiPlugin();
//        Context applicationContext = binding.getApplicationContext();
        channel.setMethodCallHandler(this);

        EventChannel onLocationStatusChangeEventChannel = new EventChannel(binding.getBinaryMessenger(), goToLocationStatusChanged.getSTREAM_CHANNEL_NAME());
        // goToLocationStatusChanged.onListen(null, eventSink);
        onLocationStatusChangeEventChannel.setStreamHandler(goToLocationStatusChanged);

        EventChannel asrEventChannel = new EventChannel(binding.getBinaryMessenger(), asrEventChannelName);
        // goToLocationStatusChanged.onListen(null, eventSink);
        asrEventChannel.setStreamHandler(asrImpl);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {

    }

}
