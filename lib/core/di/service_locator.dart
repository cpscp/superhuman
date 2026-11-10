import 'package:get_it/get_it.dart';

import '../../features/home/home_injection_container.dart' as home;
import '../../features/edit_profile/edit_profile_injection_container.dart'
    as edit_profile;

final getIt = GetIt.instance;

void setupServiceLocator() {
  // Setup feature-specific injection containers
  home.setupHomeInjectionContainer();
  edit_profile.setupEditProfileInjectionContainer();
}
