abstract class TemiBase{
  temiGoTo(String location, String speed) {}
  temiStopMovement(){}
  temiSpeak(String speakText){}
  temiWakeUp(){}
  temiFollow(){}
  Future<List<Object?>> temiLocations();
  void temiSetLocations();
  Stream<bool> temiSubscribeToRobotReadyEvents();
  Future<bool> temiIsReady();
  Stream<dynamic> temiSubscribeToOnLocationStatusChangeEvents();
  Stream<dynamic> temiTtsListener();
  Stream<dynamic> temiAsrListener();
  temiPlaySequence(String sequence){}
}