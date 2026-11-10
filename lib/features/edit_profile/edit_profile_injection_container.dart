import 'package:get_it/get_it.dart';

import 'presentation/bloc/edit_profile_bloc.dart';
import 'domain/repositories/vcard_repository.dart';
import '../home/data/repositories/vcard_repository_impl.dart';

final getIt = GetIt.instance;

void setupEditProfileInjectionContainer() {
  // Register repositories (reuse from home if not already registered)
  if (!getIt.isRegistered<VCardRepository>()) {
    getIt.registerSingleton<VCardRepository>(VCardRepositoryImpl());
  }

  // Register BLoC
  getIt.registerSingleton<EditProfileBloc>(
    EditProfileBloc(vCardRepository: getIt<VCardRepository>()),
  );
}
