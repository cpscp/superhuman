abstract class NfcDataSource {
  /// Check if NFC is available on the device
  Future<bool> isNfcAvailable();

  /// Write data to NFC tag
  /// Returns true if successful
  Future<bool> writeToTag(String data);

  /// Stop the NFC session
  Future<void> stopSession();

  /// Get NFC tag information (size, writable status)
  Future<Map<String, dynamic>> getTagInfo();
}
