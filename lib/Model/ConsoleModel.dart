

import 'package:flutter/cupertino.dart';

class ConsoleModel{
  static final TextEditingController _consoleController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  TextEditingController getConsoleController(){
    return _consoleController;
  }
  ScrollController getScrollController(){
    return _scrollController;
  }
  void resetConsole(){
    _consoleController.clear();
  }
  void addToConsole(String text) {
      _consoleController.text += "$text\n";
      // Scroll to the bottom of the console
      _scrollController.jumpTo(_scrollController.position.maxScrollExtent);
  }
}