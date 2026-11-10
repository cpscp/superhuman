import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:smart_business_cards/core/utils/logger.dart';

import '../../domain/repositories/vcard_repository.dart';
import 'edit_profile_event.dart';
import 'edit_profile_state.dart';

class EditProfileBloc extends Bloc<EditProfileEvent, EditProfileState> {
  final VCardRepository vCardRepository;

  EditProfileBloc({required this.vCardRepository})
    : super(const EditProfileInitial()) {
    on<EditProfileInitializationRequested>(_onInitializationRequested);
    on<VCardFieldsUpdated>(_onVCardFieldsUpdated);
    on<EditProfileSaveRequested>(_onEditProfileSaveRequested);
    on<SocialLinksUpdated>(_onSocialLinksUpdated);
  }

  Future<void> _onInitializationRequested(
    EditProfileInitializationRequested event,
    Emitter<EditProfileState> emit,
  ) async {
    emit(const EditProfileLoading());

    try {
      final vcard = event.initialVCard;

      emit(
        EditProfileLoaded(
          vcard: vcard,
          firstName: vcard.firstName,
          lastName: vcard.lastName,
          jobTitle: vcard.title ?? '',
          company: vcard.organization ?? '',
          bio: vcard.bio ?? '',
          phone: vcard.mobilePhone ?? '',
          email: vcard.emails.isNotEmpty ? vcard.emails.first.value : '',
          website: vcard.urls.isNotEmpty ? vcard.urls.first.value : '',
          socialLinks: vcard.urls
              .map((url) => {'type': url.type, 'value': url.value})
              .toList(),
        ),
      );
    } catch (e) {
      Logger.info('Error initializing edit profile: $e');
      emit(EditProfileError(message: 'Failed to initialize edit profile: $e'));
    }
  }

  Future<void> _onVCardFieldsUpdated(
    VCardFieldsUpdated event,
    Emitter<EditProfileState> emit,
  ) async {
    if (state is! EditProfileLoaded) return;

    final currentState = state as EditProfileLoaded;

    emit(
      currentState.copyWith(
        firstName: event.firstName,
        lastName: event.lastName,
        jobTitle: event.jobTitle,
        company: event.company,
        bio: event.bio,
        phone: event.phone,
        email: event.email,
        website: event.website,
      ),
    );
  }

  Future<void> _onEditProfileSaveRequested(
    EditProfileSaveRequested event,
    Emitter<EditProfileState> emit,
  ) async {
    if (state is! EditProfileLoaded) return;

    try {
      final currentState = state as EditProfileLoaded;

      final updatedVCard = currentState.vcard.copyWith(
        firstName: currentState.firstName,
        lastName: currentState.lastName,
        title: currentState.jobTitle,
        organization: currentState.company,
        bio: currentState.bio,
        mobilePhone: currentState.phone,
      );

      emit(EditProfileSuccess(updatedVCard: updatedVCard));
    } catch (e) {
      print('Error saving edit profile: $e');
      emit(EditProfileError(message: 'Failed to save profile: $e'));
    }
  }

  Future<void> _onSocialLinksUpdated(
    SocialLinksUpdated event,
    Emitter<EditProfileState> emit,
  ) async {
    if (state is! EditProfileLoaded) return;

    final currentState = state as EditProfileLoaded;

    emit(currentState.copyWith(socialLinks: event.socialLinks));
  }
}
