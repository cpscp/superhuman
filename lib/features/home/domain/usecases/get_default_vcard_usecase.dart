import '../repositories/vcard_repository.dart';
import '../entities/vcard_entity.dart';

class GetDefaultVCardUseCase {
  final VCardRepository vCardRepository;

  GetDefaultVCardUseCase({required this.vCardRepository});

  VCardEntity call() {
    return vCardRepository.getDefaultVCard();
  }
}
