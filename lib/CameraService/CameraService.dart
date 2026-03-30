import 'dart:convert';

import 'package:camera/camera.dart';
import 'package:flutter/foundation.dart';
import 'package:temi_app/MqttService/MqttClientTemi.dart';
import 'package:temi_app/main.dart';
import 'dart:developer';
import 'package:image/image.dart' as imglib;
import 'package:mqtt_client/mqtt_client.dart';
import 'package:uuid/uuid.dart';

import '../MqttService/MqttClientAFR.dart';

class CameraService{
  late List<CameraDescription> cameras = [];
  late CameraDescription selectedCamera;
  late CameraController _cameraController;
  bool cameraInitialized = false;
  bool streamState = true;
  late MqttClientAFR mqttClientAFR;
  bool captureState = false;

  CameraService(this.mqttClientAFR){
    initCamera();
  }
  Future<void> initCamera() async {
    cameras = await availableCameras();
    selectedCamera = cameras[0];
    debugPrint(selectedCamera.toString());
    _cameraController = CameraController(cameras[0], ResolutionPreset.high, enableAudio: false);
    _cameraController.initialize().then((_) async {
      while(false) {
        var rawPicture = await _cameraController.takePicture();
        try {
          if (mqttClientAFR.client?.connectionStatus?.state ==
              MqttConnectionState.connected) {
            List<int> currentByteList = await rawPicture.readAsBytes();
            imglib.Image? preImage = imglib.decodeImage(currentByteList);
            var image = imglib.encodeJpg(preImage!);
            /*
                imglib.copyRotate(preImage!,
                    _cameraController.description.lensDirection ==
                        CameraLensDirection.back ? 90 : 270),

                quality: 30);
             */
            // debugPrint("payload: ${base64.encode(image)}");
            mqttClientAFR.publishMessage("image/input/${mqttClientAFR.imageRequestId}", base64.encode(image));
            // mqttClientTemi.publishMessage("allthoseimages", "frame iterration");
          }
        } catch (e){}
        await Future.delayed(const Duration(seconds: 6));
      }
      cameraInitialized = true;
      // runRecord();
    });
  }
  Future<void> initCameraAndroid() async {
    cameras = await availableCameras();
  }
  void runRecord() {
    while(true){
      log("iterration");
      if(captureState) {
        captureState = false;
      }
    }
  }
}