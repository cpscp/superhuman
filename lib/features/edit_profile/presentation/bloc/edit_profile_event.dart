import 'package:equatable/equatable.dart';

import '../../domain/entities/vcard_entity.dart';

abstract class EditProfileEvent extends Equatable {
  const EditProfileEvent();

  @override
  List<Object?> get props => [];
}

class EditProfileInitializationRequested extends EditProfileEvent {
  final VCardEntity initialVCard;

  const EditProfileInitializationRequested({required this.initialVCard});

  @override
  List<Object?> get props => [initialVCard];
}

class VCardFieldsUpdated extends EditProfileEvent {
  final String firstName;
  final String lastName;
  final String jobTitle;
  final String company;
  final String bio;
  final String phone;
  final String email;
  final String website;

  const VCardFieldsUpdated({
    required this.firstName,
    required this.lastName,
    required this.jobTitle,
    required this.company,
    required this.bio,
    required this.phone,
    required this.email,
    required this.website,
  });

  @override
  List<Object?> get props => [
        firstName,
        lastName,
        jobTitle,
        company,
        bio,
        phone,
        email,
        website,
      ];
}

class EditProfileSaveRequested extends EditProfileEvent {
  const EditProfileSaveRequested();
}

class SocialLinksUpdated extends EditProfileEvent {
  final List<Map<String, String>> socialLinks;

  const SocialLinksUpdated({required this.socialLinks});

  @override
  List<Object?> get props => [socialLinks];
}
