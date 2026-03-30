import 'package:flutter/material.dart';
import 'package:temi_app/Model/ConsoleModel.dart';
import 'package:temi_app/screens/ConsoleScreen.dart';
import 'package:temi_app/screens/MainPage.dart';
import 'package:temi_app/screens/PatrolPage.dart';

class Sidebar extends StatelessWidget {
  const Sidebar({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Drawer(
      //backgroundColor: Colors.black ,
      child: ListView(
        padding: EdgeInsets.zero,
        children: <Widget>[
          ListTile(
            leading: const Icon(Icons.home),
            title: const Text("Home"),
            onTap: () {
              Navigator.push(context, MaterialPageRoute(builder: (context) => const MainPage()));
            },
          ),
          ListTile(
            leading: const Icon(Icons.terminal),
            title: const Text("Console"),
            onTap: () {
              Navigator.push(context, MaterialPageRoute(builder: (context) => ConsoleWidget()));
            },
          ),
          ListTile(
            leading: const Icon(Icons.settings),
            title: const Text("Patrol Settings"),
            onTap: () {
              Navigator.push(context, MaterialPageRoute(builder: (context) => const PatrolPage()));
            },
          )
        ],
      ),
    );
  }
}