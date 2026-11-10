abstract class NfcRepository {
  /// Check if NFC is available on the device
  Future<bool> isNfcAvailable();

  /// Write vCard data to NFC tag
  /// Returns true if write was successful
  Future<bool> writeVCardToTag(String vCardData);

  /// Stop any active NFC session
  Future<void> stopNfcSession();
}
