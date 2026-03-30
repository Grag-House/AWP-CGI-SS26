import 'dart:async';
import 'dart:convert';
import 'dart:developer';

import 'package:flutter/foundation.dart' show debugPrint, kIsWeb;
import 'package:flutter/services.dart';
// import 'package:mqtt_client/mqtt_browser_client.dart';
import 'package:mqtt_client/mqtt_client.dart';
import 'package:mqtt_client/mqtt_server_client.dart';
import 'package:temi_app/temi/Temi.dart';
import '../temi/TemiImpl.dart';
import '../temi/temiMock.dart';

class MqttClientBase{
  late MqttClient? client;
  late final Map<String, dynamic> mqttConfig;
  late String configPath;
  bool connectedState = false;
  StreamSubscription? subOnLocationStatusChange;
  StreamSubscription? subOnTtsListener;
  StreamSubscription? subOnAsrListener;

  MqttClientBase({required String path}) {
    configPath = path;
  }

  Future publishMessage(String topicName, String payload) async {
    // For MQTTv5:
    // MqttPayloadBuilder tmp = MqttPayloadBuilder();
    // tmp.addString(payload);
    client?.publishMessage(topicName, MqttQos.exactlyOnce, MqttClientPayloadBuilder().addString(payload).payload!);
  }
  Future<Map<String, dynamic>> readConfig(String path) async {
    // 'configs/config_mqtt_client.json
    final data = await rootBundle.loadString(path);
    var data_ = json.decode(data) as Map<String, dynamic>;
    log(data_.toString());
    return data_;
  }
  void onConnected(){
    print("${mqttConfig["clientId"]}::onConnected successfully to ${mqttConfig["address"]}");
  }
  void onDisconnected() {
    print(
        "${mqttConfig["clientId"]}::onDisconnected client disconnecting from ${mqttConfig["address"]}");
    if (client?.connectionStatus!.disconnectionOrigin ==
        MqttDisconnectionOrigin.solicited) {
      print(
          "${mqttConfig["clientId"]}::OnDisconnected callback is solicited, this is correct");
    }
  }
  void onSubscribed(String topic) {
    print('EXAMPLE::Subscription confirmed for topic $topic');
  }
  void pong() {
    print('EXAMPLE::Ping response client callback invoked');
  }
}