import 'dart:collection';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:mqtt_client/mqtt_client.dart';
import '../temi/Temi.dart';
import 'MqttClientBase.dart';
import 'dart:developer';
import 'package:temi_app/temi/TemiImpl.dart';
import 'package:temi_app/temi/temiMock.dart';
// import 'package:mqtt_client/mqtt_browser_client.dart';
import 'package:mqtt_client/mqtt_client.dart';
import 'package:mqtt_client/mqtt_server_client.dart';
import 'package:temi_app/Model/TemiModel.dart';

class MqttClientTemi extends MqttClientBase{
  late TemiBase temi;
  TemiModel temiModel = TemiModel();

  Map<String, String> topicList = {
    'innovation_lab/karlsruhe/temi/temi_goto/set': 'temi_goto',
    'innovation_lab/karlsruhe/temi/temi_speak/set': 'temi_speak',
    'innovation_lab/karlsruhe/temi/temi_wake_up/set': 'temi_wake_up',
    'innovation_lab/karlsruhe/temi/temi_follow/set': 'temi_follow',
    'innovation_lab/karlsruhe/temi/temi_stop_movement/set' : 'temi_stop_movement',
    'innovation_lab/karlsruhe/temi/temi_get_locations' : 'temi_get_locations',
    'innovation_lab/karlsruhe/temi/temi_get_ready_state' : 'temi_get_ready_state',
    'innovation_lab/karlsruhe/temi/temi_playsequence/set' : 'temi_playsequence',
  };
  MqttClientTemi() : super(path: 'configs/config_mqtt_temi.json') {
    configure();
  }
  Future configure() async {
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
    if (kIsWeb) {
      temi = TemiMock();
    } else {
      temi = Temi();
    }
    tmpClient.logging(on: true);
    tmpClient.autoReconnect = true;
    tmpClient.keepAlivePeriod = 30;
    tmpClient.connectTimeoutPeriod = 5000;
    tmpClient.onConnected = onConnected;
    tmpClient.onDisconnected = onDisconnected;
    tmpClient.onSubscribed = onSubscribed;
    tmpClient.pongCallback = pong;
    final connMess = MqttConnectMessage()
        .withClientIdentifier('${mqttConfig["clientId"]}')
        .withWillTopic('innovation_lab/karlsruhe/temi/greeting')
        .withWillMessage('Temi is connected')
        .startClean()
        .withWillQos(MqttQos.atLeastOnce)
        .authenticateAs(mqttConfig["username"], mqttConfig["password"]);
    tmpClient.connectionMessage = connMess;
    try {
      await tmpClient.connect();
      client = tmpClient;
      receive('#', handleMessage);
      temiListener();
      List<Object?> temiLocationsList = await temi.temiLocations();
      List<dynamic> jsonList = jsonDecode(jsonEncode(temiLocationsList));
      temiModel.setLocations(jsonList.cast<String>().toList());
    } on Exception catch (e) {
      debugPrint(e.toString());
      tmpClient.disconnect();
    }
  }
  Future<void> handleMessage(MqttPublishMessage message, String action, String topic) async {
    // var payload = String.fromCharCodes(message.payload.message);
    log("Incoming Message: ${String.fromCharCodes(message.payload.message)}");
    String stringData = String.fromCharCodes(message.payload.message);
    final data = jsonDecode(stringData);

    switch(action) {
      case "temi_goto":
        if (data["speed"] != null) {
          await temi.temiGoTo(
              data["payloadObject"], data["speed"].toString().toUpperCase());
        } else {
          await temi.temiGoTo(data["payloadObject"], 'high'.toUpperCase());
        }
        break;
      case "temi_speak":
        temi.temiSpeak(data["payloadObject"]);
        break;
      case "temi_wake_up":
        temi.temiWakeUp();
        break;
      case "temi_follow":
        temi.temiFollow();
        break;
      case "temi_stop_movement": {
        temi.temiStopMovement();
      }
      break;
      case "temi_get_locations":
        {
          List<Object?> temiLocationsList = await temi.temiLocations();
          //log(temiLocationsList.toString);
          publishMessage("$topic/return",
              '{"payloadObject":${jsonEncode(temiLocationsList)}}');
          List<dynamic> jsonList = jsonDecode(jsonEncode(temiLocationsList));
          temiModel.setLocations(jsonList.cast<String>().toList());
        }
        break;
      case "temi_get_ready_state":
        {
          var tmpState = await temi.temiIsReady();
          log(temi.temiIsReady().toString());
          publishMessage("$topic/return", '{"payloadObject":$tmpState)');
        }
        break;
      case "temi_playsequence":
        {
          temi.temiPlaySequence(data["payloadObject"]);
        }
        break;
      default:
        log("No action mapped correctly");
        break;
    }
  }
  void temiListener() async {
    subOnLocationStatusChange = temi.temiSubscribeToOnLocationStatusChangeEvents().listen((event) {
      eventListenerBuilder(4, 'innovation_lab/karlsruhe/temi/onlocationsstatuschangevents', event);
    });
    subOnTtsListener = temi.temiTtsListener().listen((event) {
      eventListenerBuilder(3, 'innovation_lab/karlsruhe/temi/ttsListener', event);
    });
    subOnAsrListener = temi.temiAsrListener().listen((event) {
      eventListenerBuilder(1, 'innovation_lab/karlsruhe/temi/asrListener', event);
    });
  }
  Future receive(String topicName, Function(MqttPublishMessage, String action, String topic) callback) async {
    if(client?.connectionStatus?.state == MqttConnectionState.connected) {
      client?.subscribe('#', MqttQos.atLeastOnce);
      client?.updates?.listen((event) {
        log("message recieved message from topic: ${event[0].topic}");
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
  void eventListenerBuilder(int atributsAmount, String pubTopic, event){
    LinkedHashMap<Object?, Object?> tmp = event;
    String eventJsonForm = '{';
    for(int i = 0; i < atributsAmount; i++) {
      if(i == atributsAmount - 1){
        eventJsonForm += '"${tmp.keys.first}":"${tmp.values.elementAt(0)}"';
      } else {
        eventJsonForm += '"${tmp.keys.first}":"${tmp.values.elementAt(0)}", ';
        tmp.remove(tmp.keys.first);
      }
    }
    eventJsonForm += '}';
    log(eventJsonForm);
    Map<String, dynamic> events = jsonDecode(eventJsonForm);
    publishMessage(pubTopic, eventJsonForm);
    if(pubTopic == 'innovation_lab/karlsruhe/temi/ttsListener'){
      if(events['status'] == 'processing' || events['status'] == 'started'){
        temiModel.setSpeakState(true);
      } else if(events['status'] == 'completed') {
        temiModel.setSpeakState(false);
      }
    }
    if(pubTopic == 'innovation_lab/karlsruhe/temi/onlocationsstatuschangevents'){
      if(events['status'] == 'start' || events['status'] == 'going' || events['status'] == 'calculating'){
        temiModel.setDrivingState(true);
      } else if(events['status'] == 'complete'){
        temiModel.setDrivingState(false);
      }
    }
  }
}