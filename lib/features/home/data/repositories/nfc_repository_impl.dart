import '../../domain/repositories/nfc_repository.dart';
import '../datasources/nfc_data_source.dart';
import '../../../../core/utils/logger.dart';

class NfcRepositoryImpl implements NfcRepository {
  final NfcDataSource dataSource;

  NfcRepositoryImpl({required this.dataSource});

  @override
  Future<bool> isNfcAvailable() async {
    try {
      return await dataSource.isNfcAvailable();
    } catch (e) {
      Logger.error('Error checking NFC availability', e);
      return false;
    }
  }

  @override
  Future<bool> writeVCardToTag(String vCardData) async {
    try {
      return await dataSource.writeToTag(vCardData);
    } catch (e) {
      Logger.error('Error writing vCard to tag', e);
      rethrow;
    }
  }

  @override
  Future<void> stopNfcSession() async {
    try {
      await dataSource.stopSession();
    } catch (e) {
      Logger.error('Error stopping NFC session', e);
    }
  }
}
