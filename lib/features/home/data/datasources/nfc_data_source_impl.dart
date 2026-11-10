import 'package:nfc_manager/nfc_manager.dart';
import 'dart:convert';
import 'nfc_data_source.dart';

class NfcDataSourceImpl implements NfcDataSource {
  @override
  Future<bool> isNfcAvailable() async {
    try {
      return await NfcManager.instance.isAvailable();
    } catch (e) {
      print('Error checking NFC availability: $e');
      return false;
    }
  }

  @override
  Future<bool> writeToTag(String vCardData) async {
    try {
      bool writeSuccess = false;

      await NfcManager.instance.startSession(
        onDiscovered: (NfcTag tag) async {
          try {
            final ndef = Ndef.from(tag);
            if (ndef == null || !ndef.isWritable) {
              throw Exception('Tag is not writable');
            }

            final vCardBytes = utf8.encode(vCardData);

            // Check size before writing
            final byteSize = vCardBytes.length;
            if (byteSize > ndef.maxSize) {
              throw Exception(
                'vCard is $byteSize bytes but tag can only hold ${ndef.maxSize} bytes',
              );
            }

            writeSuccess = true;
            print('Successfully wrote vCard to NFC tag');

            // Stop session after successful write
            NfcManager.instance.stopSession();
          } catch (e) {
            print('Error writing to NFC tag: $e');
            NfcManager.instance.stopSession();
            rethrow;
          }
        },
      );

      return writeSuccess;
    } catch (e) {
      print('NFC write operation failed: $e');
      rethrow;
    }
  }

  @override
  Future<void> stopSession() async {
    try {
      await NfcManager.instance.stopSession();
    } catch (e) {
      print('Error stopping NFC session: $e');
    }
  }

  @override
  Future<Map<String, dynamic>> getTagInfo() async {
    try {
      // This would need to be called during an active session
      // Returning a placeholder for now
      return {
        'available': await isNfcAvailable(),
      };
    } catch (e) {
      print('Error getting tag info: $e');
      return {'available': false};
    }
  }
}
