import 'package:flutter/material.dart';

import '../Model/ConsoleModel.dart';


class ConsoleWidget extends StatefulWidget {
  const ConsoleWidget({super.key});

  @override
  _ConsoleWidgetState createState() => _ConsoleWidgetState();
}

class _ConsoleWidgetState extends State<ConsoleWidget> {
  ConsoleModel consoleModel = ConsoleModel();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Console"),
      ),
      body: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Column(
          children: [
            Expanded(
              child: SingleChildScrollView(
                controller: consoleModel.getScrollController(),
                child: TextField(
                  controller: consoleModel.getConsoleController(),
                  maxLines: null,
                  readOnly: true,
                  decoration: const InputDecoration(
                    border: InputBorder.none,
                    hintText: "Console output...",
                  ),
                ),
              ),
            ),
            TextField(
              onSubmitted: (text) {
                consoleModel.addToConsole(text);
                // _consoleController.clear();
              },
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                hintText: "Enter command...",
              ),
            ),
          ],
        ),
      ),
    );
  }
}