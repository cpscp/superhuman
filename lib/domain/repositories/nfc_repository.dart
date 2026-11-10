import '../entities/nfc_tag_entity.dart';

abstract class NfcRepository {
  /// Check if NFC is available on the device
  Future<bool> isNfcAvailable();

  /// Write vCard data to NFC tag
  /// Returns true if write was successful
  Future<bool> writeVCardToTag(String vCardData);

  /// Check NFC tag availability and get tag info
  Future<NfcTagEntity> getTagInfo();

  /// Stop any active NFC session
  Future<void> stopNfcSession();
}
