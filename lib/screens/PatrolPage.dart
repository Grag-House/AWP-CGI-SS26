import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:temi_app/Model/TemiModel.dart';

import '../Widgets/Sidebar.dart';


class PatrolPage extends StatefulWidget {

  const PatrolPage({Key? key}) : super(key: key);

  @override
  State<PatrolPage> createState() => _PatrolPage();
}

class _PatrolPage extends State<PatrolPage> {
  TemiModel temiModel = TemiModel();
  String selectedOption = "home base";
  List<String> newPatrolRoute = ["home base"];
  List<String> newPatrolTime = ["13",":", "00"];
  List<String> hours = ["13"];
  List<String> minutes = ["00"];
  String selectedHour = "13";
  String selectedMinute = "00";
  _PatrolPage(){
    initState();
  }
  @override
  void initState(){
    super.initState();
    hours = List.generate(24, (index) => index.toString().padLeft(2, '0'));
    minutes = List.generate(60, (index) => index.toString().padLeft(2, '0'));
    newPatrolRoute = temiModel.getPatrolLocations();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Main Page"),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            ConstrainedBox(
                constraints: const BoxConstraints(
                  minWidth: 450,
                  maxWidth: 500,
                  minHeight: 100,
                  maxHeight: 150,
                ),
                child: Container(
                    decoration: BoxDecoration(
                      border: Border.all(
                          color: Colors.blueGrey,
                          width: 2.0
                      ),
                    ),
                    child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: <Widget>[
                          const Text(
                            "Patrol Settings",
                          ),
                          Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: <Widget>[
                                DropdownButton(
                                  value: selectedOption,
                                  items: temiModel.getLocations().map((option) {
                                    return DropdownMenuItem(
                                      value: option,
                                      child: Text(option),
                                    );
                                  }).toList(),
                                  onChanged: (newValue) {
                                    setState(() {
                                      selectedOption = newValue!;
                                    });
                                  },
                                ),
                                ElevatedButton(
                                    onPressed: () {
                                      newPatrolRoute.add(selectedOption);
                                    }, child: const Text("add location to patrol route")
                                ),
                              ]
                          ),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: <Widget>[
                              const Text("selected: ", textScaleFactor: 1.3),
                              Text(newPatrolRoute.join(' ', ),
                                  textScaleFactor: 1.3
                              ),
                            ],
                          ),
                          Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: <Widget>[
                                ElevatedButton(
                                    onPressed: () {
                                      newPatrolRoute = ["home base"];
                                    }, child: const Text("delete patrol route")
                                ),
                                ElevatedButton(
                                  onPressed: () {
                                    temiModel.setPatrolLocations(newPatrolRoute);
                                  },
                                  child: const Text("save patrol route"),
                                ),
                              ]
                          ),
                        ]
                    )
                )
            ),
            ConstrainedBox(
              constraints: const BoxConstraints(
                minWidth: 300,
                maxWidth: 400,
                minHeight: 50,
                maxHeight: 100,
              ),
              child: Container(
                margin: const EdgeInsets.only(top: 10),
                decoration: BoxDecoration(
                  border: Border.all(
                      color: Colors.blueGrey,
                      width: 2.0
                  ),
                ),
                child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: <Widget>[
                      Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: <Widget>[
                            const Text("select new Time: "),
                            DropdownButton(
                              value: selectedHour,
                              items: hours.map((option) {
                                return DropdownMenuItem(
                                  value: option,
                                  child: Text(option),
                                );
                              }).toList(),
                              onChanged: (newValue) {
                                setState(() {
                                  selectedHour = newValue!;
                                });
                              },
                            ),
                            DropdownButton(
                              value: selectedMinute,
                              items: minutes.map((option) {
                                return DropdownMenuItem(
                                  value: option,
                                  child: Text(option),
                                );
                              }).toList(),
                              onChanged: (newValue) {
                                setState(() {
                                  selectedMinute = newValue!;
                                });
                              },
                            ),
                          ]
                      ),
                      ElevatedButton(onPressed: () {
                        temiModel.setPatrolTime(Duration(hours: int.parse(selectedHour), minutes: int.parse(selectedMinute), seconds: 0));
                      }, child: const Text("save Patrol Time"),
                      )
                    ]
                )
              ),
            ),
          ],
        ),
      ),
      drawer: const Sidebar(), // This trailing comma makes auto-formatting nicer for build methods.
    );
  }
}