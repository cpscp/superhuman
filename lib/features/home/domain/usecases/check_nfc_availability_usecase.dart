import '../repositories/nfc_repository.dart';

class CheckNfcAvailabilityUseCase {
  final NfcRepository nfcRepository;

  CheckNfcAvailabilityUseCase({required this.nfcRepository});

  Future<bool> call() async {
    return await nfcRepository.isNfcAvailable();
  }
}
