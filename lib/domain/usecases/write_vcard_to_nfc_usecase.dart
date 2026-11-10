import '../repositories/vcard_repository.dart';
import '../repositories/nfc_repository.dart';
import '../entities/vcard_entity.dart';

class WriteVCardToNfcUseCase {
  final NfcRepository nfcRepository;
  final VCardRepository vCardRepository;

  WriteVCardToNfcUseCase({
    required this.nfcRepository,
    required this.vCardRepository,
  });

  Future<bool> call(VCardEntity vCard) async {
    // Validate vCard
    if (!vCardRepository.validateVCard(vCard)) {
      throw Exception('Invalid vCard data');
    }

    // Generate vCard string
    final vCardString = vCardRepository.generateVCardString(vCard);

    // Write to NFC tag
    return await nfcRepository.writeVCardToTag(vCardString);
  }
}
