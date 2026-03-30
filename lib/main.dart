import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:temi_app/CameraService/CameraService.dart';
import 'package:temi_app/MqttService/MqttClientAFR.dart';
import 'package:temi_app/MqttService/MqttClientTemi.dart';
import 'package:temi_app/screens/MainPage.dart';
import 'package:temi_app/temi/TemiPatrolAction.dart';

List<CameraDescription> _cameras = <CameraDescription>[];

Future<void> main() async {
  try {
    /*
    WidgetsFlutterBinding.ensureInitialized();
    _cameras = await availableCameras();
    print("cameras found $_cameras");
    */

  } on CameraException catch (e) {
    _logError(e.code, e.description);
  }
  runApp(const MyApp());

}

void _logError(String code, String? message) {
  if (message != null) {
    print('Error: $code\nError Message: $message');
  } else {
    print('Error: $code');
  }
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Innovation Lab',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'Innovation Lab'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});
  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _counter = 0;
 // MqttClientTemi? mqttClientTemi;

  _MyHomePageState(){
    PatrolAction patrolAction = PatrolAction();
    MqttClientTemi mqttClientTemi = MqttClientTemi();
    MqttClientAFR mqttClientAFR = MqttClientAFR();
    CameraService cameraService = CameraService(mqttClientAFR);
  }

  @override
  Widget build(BuildContext context) {
    return const MainPage();
  }
}
