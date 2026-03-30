import 'package:flutter/material.dart';

import '../Widgets/Sidebar.dart';


class MainPage extends StatefulWidget {

  const MainPage({Key? key}) : super(key: key);

  @override
  State<MainPage> createState() => _HomeScreen();
}

class _HomeScreen extends State<MainPage> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Main Page"),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: const <Widget>[
            Text(
              "Main Page",
            ),
          ],
        ),
      ),
      drawer: Sidebar(), // This trailing comma makes auto-formatting nicer for build methods.
    );
  }
}