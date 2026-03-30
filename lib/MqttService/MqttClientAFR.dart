import 'dart:convert';
import 'package:intl/intl.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:mqtt_client/mqtt_client.dart';
import 'package:temi_app/Model/ConsoleModel.dart';
import 'package:tuple/tuple.dart';
import 'package:uuid/uuid.dart';
import 'dart:developer';
import 'MqttClientBase.dart';
// import 'package:mqtt_client/mqtt_browser_client.dart';
import 'package:mqtt_client/mqtt_server_client.dart';

class MqttClientAFR extends MqttClientBase{
  String imageRequestId = Uuid().v1();
  final ConsoleModel consoleModel = ConsoleModel();
  Map<String, String> topicList = {};
  var navigatorKey = GlobalKey<NavigatorState>();
  MqttClientAFR() : super(path: 'configs/config_mqtt_afr.json') {
    configure();
  }
  Future configure() async {
    topicList = {'image/output/$imageRequestId': 'action'};
    mqttConfig = await readConfig(configPath);
    MqttClient tmpClient;
    if (kIsWeb) {
      /*
      log('on web, starting ws mqtt');
      tmpClient = MqttBrowserClient('ws://${mqttConfig["address"]}/', '${mqttConfig["clientId"]}');
      tmpClient.port = 8080;
      tmpClient.websocketProtocols = MqttClientConstants.protocolsSingleDefault;
      tmpClient.setProtocolV311();
       */
      // For Temi Deployment uncomment
      tmpClient  = MqttServerClient('${mqttConfig["address"]}', '');
    } else {
      log('not on web, starting default mqtt');
      tmpClient  = MqttServerClient('${mqttConfig["address"]}', '');
      tmpClient.port = 1883;
    }
    tmpClient.logging(on: false);
    tmpClient.autoReconnect = true;
    tmpClient.keepAlivePeriod = 30;
    tmpClient.connectTimeoutPeriod = 5000;
    tmpClient.onConnected = onConnected;
    tmpClient.onDisconnected = onDisconnected;
    tmpClient.onSubscribed = onSubscribed;
    final connMess = MqttConnectMessage()
        .withClientIdentifier('${mqttConfig["clientId"]}')
        .withWillTopic('innovation_lab/karlsruhe/temi/greeting')
        .withWillMessage('Temi is connected')
        .startClean()
        .withWillQos(MqttQos.atLeastOnce);
        //.authenticateAs(mqttConfig["username"], mqttConfig["password"]);1
    tmpClient.connectionMessage = connMess;
    try {
      await tmpClient.connect();
      client = tmpClient;
      receive('#', handleMessage);
    } on Exception catch (e) {
      debugPrint(e.toString());
      tmpClient.disconnect();
    }
  }
  Future<void> handleMessage(MqttPublishMessage message, String action, String topic) async {
    // var payload = String.fromCharCodes(message.payload.message);
    // log("Incoming Message: ${String.fromCharCodes(message.payload.message)}");
    var payload = String.fromCharCodes(message.payload.message);
    if (payload == "No Person found") {
      log("Received no Person found");
    } else {
      // log("Received Image Payload: $payload");
      var stringresults = payload.split(';');
      if(stringresults.length > 1){
        DateTime now = DateTime.now();
        DateFormat formatter = DateFormat('yyyy-MM-dd HH:mm:ss');
        String formattedDate = formatter.format(now);
        if(stringresults[1] == 'sitting'){
          consoleModel.addToConsole('${stringresults[1]} $formattedDate');
        } else if(stringresults[1] == 'standing') {
          consoleModel.addToConsole('${stringresults[1]} $formattedDate');
        } else if(stringresults[1] == 'lying risk') {
          consoleModel.addToConsole('${stringresults[1]} $formattedDate');
        }
      }
      var response = Tuple2(Image.memory(base64Decode(stringresults[0])), stringresults[1]);
    }
  }
  Future receive(String topicName, Function(MqttPublishMessage, String action, String topic) callback) async {
    if(client?.connectionStatus?.state == MqttConnectionState.connected) {
      client?.subscribe('#', MqttQos.atLeastOnce);
      client?.updates?.listen((event) {
        if (event[0].payload is MqttPublishMessage) {
          if (topicList.containsKey(event[0].topic)) {
            log("topicList contains event topic successful");
            callback(event[0].payload as MqttPublishMessage,
                topicList[event[0].topic]!, event[0].topic);
          }
        }
      });
    } else{
    }
  }
}
