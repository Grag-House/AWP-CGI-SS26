import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:temi_app/Model/TemiModel.dart';

import 'Temi.dart';

class Temi extends TemiBase {
  final MethodChannel _channel = const MethodChannel('flutter_temi/flutter_temi');
  final emptyString = "";
  final TemiModel temiModel = TemiModel();

  final EventChannel _onRobotReadyEventChannel = const EventChannel('flutter_temi/on_robot_ready_stream');
  final EventChannel _onLocationStatusChangeEventChannel = const EventChannel('flutter_temi/on_location_status_stream');
  final _ttsListener = const EventChannel('flutter_temi/tts_stream');
  final _asrListener = const EventChannel('flutter_temi/asr_stream');

  @override
  Future<bool> temiGoTo(String location, String speed) async {
    return await _channel.invokeMethod('temi_goto', [location, speed]);
  }
  @override
  temiStopMovement() async {
    await _channel.invokeMethod('temi_stop_movement', emptyString);
  }
  @override
  temiSpeak(String speakText) async {
    await _channel.invokeMethod('temi_speak', speakText);
  }
  @override
  temiWakeUp() async {
    await _channel.invokeMethod('temi_wake_up', emptyString);
  }
  @override
  temiFollow() async {
    await _channel.invokeMethod('temi_follow', emptyString);
  }
  @override
  Future<List<Object?>> temiLocations() async {
    return await _channel.invokeMethod('temi_get_locations', emptyString);
  }

  @override
  void temiSetLocations(){
    List<Object?> locations = _channel.invokeMethod('temi_get_locations', emptyString) as List<Object?>;
    temiModel.setLocations(jsonEncode(locations) as List<String>);
  }

  @override
  Stream<bool> temiSubscribeToRobotReadyEvents() {
    return _onRobotReadyEventChannel.receiveBroadcastStream() as Stream<bool>;
  }
  // Doesn't work
  @override
  Future<bool> temiIsReady() async {
    return await _channel.invokeMethod('temi_get_ready_state', emptyString);
  }
  @override
  Stream<dynamic> temiSubscribeToOnLocationStatusChangeEvents() {
    return _onLocationStatusChangeEventChannel.receiveBroadcastStream();
  }
  @override
  Stream<dynamic> temiTtsListener() {
    return _ttsListener.receiveBroadcastStream();
  }
  @override
  Stream<dynamic> temiAsrListener() {
    return _asrListener.receiveBroadcastStream();
  }
  @override
  temiPlaySequence(String sequence){
    return _channel.invokeMethod('temi_playsequence', sequence);
  }
}