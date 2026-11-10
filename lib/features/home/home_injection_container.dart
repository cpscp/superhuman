import 'package:get_it/get_it.dart';

import 'presentation/bloc/home_bloc.dart';
import 'domain/usecases/write_vcard_to_nfc_usecase.dart';
import 'domain/usecases/check_nfc_availability_usecase.dart';
import 'domain/usecases/get_default_vcard_usecase.dart';
import 'domain/repositories/nfc_repository.dart';
import 'domain/repositories/vcard_repository.dart';
import 'data/datasources/nfc_data_source.dart';
import 'data/datasources/nfc_data_source_impl.dart';
import 'data/repositories/nfc_repository_impl.dart';
import 'data/repositories/vcard_repository_impl.dart';

final getIt = GetIt.instance;

void setupHomeInjectionContainer() {
  // Register data sources
  getIt.registerSingleton<NfcDataSource>(NfcDataSourceImpl());

  // Register repositories
  getIt.registerSingleton<NfcRepository>(
    NfcRepositoryImpl(dataSource: getIt<NfcDataSource>()),
  );
  getIt.registerSingleton<VCardRepository>(VCardRepositoryImpl());

  // Register use cases
  getIt.registerSingleton<WriteVCardToNfcUseCase>(
    WriteVCardToNfcUseCase(
      nfcRepository: getIt<NfcRepository>(),
      vCardRepository: getIt<VCardRepository>(),
    ),
  );
  getIt.registerSingleton<CheckNfcAvailabilityUseCase>(
    CheckNfcAvailabilityUseCase(nfcRepository: getIt<NfcRepository>()),
  );
  getIt.registerSingleton<GetDefaultVCardUseCase>(
    GetDefaultVCardUseCase(vCardRepository: getIt<VCardRepository>()),
  );

  // Register BLoC
  getIt.registerSingleton<HomeBloc>(
    HomeBloc(
      writeVCardToNfcUseCase: getIt<WriteVCardToNfcUseCase>(),
      checkNfcAvailabilityUseCase: getIt<CheckNfcAvailabilityUseCase>(),
      getDefaultVCardUseCase: getIt<GetDefaultVCardUseCase>(),
      vCardRepository: getIt<VCardRepository>(),
    ),
  );
}
