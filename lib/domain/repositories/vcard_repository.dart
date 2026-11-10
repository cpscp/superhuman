import '../entities/vcard_entity.dart';

abstract class VCardRepository {
  /// Generate vCard string from entity
  String generateVCardString(VCardEntity vcard);

  /// Get default vCard
  VCardEntity getDefaultVCard();

  /// Validate vCard data
  bool validateVCard(VCardEntity vcard);
}
