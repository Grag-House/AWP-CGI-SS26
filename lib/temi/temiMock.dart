import 'dart:developer';

import 'package:temi_app/temi/Temi.dart';


class TemiMock extends TemiBase{
  /// Returns [value] plus 1.
  int addOne(int value) => value + 1;

  @override
  temiSpeak(String s) {
    print('temi speak $s');
  }

  static temiSpeakForce(String s) {
    print('temi speak force $s');
  }

  static temiSubscribeToAsrEvents() {}

  static temiFinisheConverstaion() {}

  static Map userInfo = Map();

  static temiStartTelepresence(String s, String s2) {}

  static temiShowAppList() {}

  static temiTurnKioskMode() {}

  static temiWakeup() {}

  void temiGoTo(String s,String speed) {
    log('going to $s');
  }
  void temiStopMovement() {}

  Future<bool> temiIsReady() {
    return Future.value(false);
  }

  void temiPlaySequence(String sequence) {}

  static temiSaveLocation(String s) {}

  Future<List<Object?>> temiLocations(){
    List<Object?> tmp = List.empty();
    tmp.add("home base");
    return Future.value(tmp);
  }

  void temiSetLocations(){
  }

  Stream<dynamic> temiTtsListener() {
    return const Stream.empty();
  }
  Stream<dynamic> temiAsrListener() {
    return const Stream.empty();
  }

  static temiFollowMe() {}

  static temiSkidJoy(num a, num b) {}

  static temiTiltAngle(num degree) {}

  static temiTurnBy(num degree) {}

  static temiTiltBy(num degree) {}

  static temiRepose() {}

  @override
  Stream<bool> temiSubscribeToRobotReadyEvents() {
    return const Stream.empty();
  }

  @override
  Stream<dynamic> temiSubscribeToOnLocationStatusChangeEvents() {
    return const Stream.empty();
  }
}
