import 'package:temi_app/Model/TemiModel.dart';
import 'package:temi_app/temi/TemiImpl.dart';
import 'dart:developer';
import 'dart:isolate';

class PatrolAction {
  bool runState = true;
  TemiModel temiModel = TemiModel();
  Temi temi = Temi();
  bool patrolThreadState = true;

  PatrolAction() {
    initStates();
  }
  void setRunState(bool state){
    runState = state;
  }
  bool getRunState(bool state){
    return runState;
  }
  void initStates() async {
    double startTime = DateTime.now().millisecondsSinceEpoch / 1000;
    // await Future.delayed(const Duration(seconds: 5));
    // Future(() => patrolThread()).then((value) => Null);
    String data = "";
    final isolate = await Isolate.spawn(patrolThread, data);
  }
  Future<String> patrolThread(String data) async {
    while(patrolThreadState){
      if(runState) {
        if(temiModel.getPatrolTime().inHours == DateTime.now().hour &&
            (temiModel.getPatrolTime().inMinutes % 60) == DateTime.now().minute  &&
            (((temiModel.getPatrolTime().inSeconds) % 60) % 60) + (DateTime.now().millisecondsSinceEpoch ~/ 1000) <=
                (DateTime.now().millisecondsSinceEpoch ~/ 1000) + (((temiModel.getPatrolTime().inSeconds) % 60) % 60) + 1 &&
            (((temiModel.getPatrolTime().inSeconds) % 60) % 60) + (DateTime.now().millisecondsSinceEpoch ~/ 1000) >=
                (DateTime.now().millisecondsSinceEpoch ~/ 1000) + (((temiModel.getPatrolTime().inSeconds) % 60) % 60) - 1
        ) {
          bool ret = patrolStart();
          // runState = false;
        }
      }
    }
    return "Test";
  }
  bool patrolStart(){
    int startTime = DateTime.now().millisecondsSinceEpoch;
    for(int i= 0; i < temiModel.getPatrolLocations().length; i++){
      if(!temiModel.getDrivingState()){
        temi.temiGoTo(temiModel.getPatrolLocations()[i], 'medium'.toUpperCase());
        while(true){
          if(!temiModel.getDrivingState()){
            i++;
            break;
          }
        }
      }
      if(((DateTime.now().millisecondsSinceEpoch - startTime) / 1000) < 500){
        return false;
      }
    }
    return true;
  }
}