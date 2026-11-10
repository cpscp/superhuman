import 'package:flutter/foundation.dart';

class Logger {
  static void log(String message, [dynamic error, StackTrace? stackTrace]) {
    if (kDebugMode) {
      print('[NFC vCard] $message');
      if (error != null) {
        print('[ERROR] $error');
      }
      if (stackTrace != null) {
        print('[STACK TRACE] $stackTrace');
      }
    }
  }

  static void info(String message) {
    log('[INFO] $message');
  }

  static void warning(String message) {
    log('[WARNING] $message');
  }

  static void error(String message, [dynamic error, StackTrace? stackTrace]) {
    log('[ERROR] $message', error, stackTrace);
  }
}
