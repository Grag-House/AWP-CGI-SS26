import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:intl/intl.dart';
import 'package:tuple/tuple.dart';

class TemiModel {
  static bool drivingState = false;
  static bool speakState = false;
  static List<String> locations = ["home base", "platz7", "platz6", "platz5", "platz4"];
  static bool patrolActionState = false;
  static List<String> patrolLocations = ["home base", "kaffeemaschine", "platz7"];
  static Duration patrolTime = const Duration(hours: 13, minutes: 0, seconds: 0);


  Duration getPatrolTime(){
    return patrolTime;
  }
  void setPatrolTime(Duration newTime){
    // arg* hours, minutes, seconds
    patrolTime = newTime;
  }
  bool getDrivingState () {
    return drivingState;
  }
  bool getPatrolActionState () {
    return drivingState;
  }
  void setPatrolActionState(bool state){
    patrolActionState = state;
  }
  void setDrivingState (bool state) {
   drivingState = state;
  }
  List<String> getLocations(){
    return locations;
  }
  void setLocations(List<String> allLocations){
    locations = allLocations;
  }
  void setPatrolLocations(List<String> newPatrolLocations){
    patrolLocations = newPatrolLocations;
  }
  List<String> getPatrolLocations(){
    return patrolLocations;
  }
  void setSpeakState(bool state){
    speakState = state;
  }
  bool getSpeakState(){
    return speakState;
  }
}