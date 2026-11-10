import 'package:equatable/equatable.dart';

import '../../domain/entities/vcard_entity.dart';

abstract class EditProfileState extends Equatable {
  const EditProfileState();

  @override
  List<Object?> get props => [];
}

class EditProfileInitial extends EditProfileState {
  const EditProfileInitial();
}

class EditProfileLoading extends EditProfileState {
  const EditProfileLoading();
}

class EditProfileLoaded extends EditProfileState {
  final VCardEntity vcard;
  final String firstName;
  final String lastName;
  final String jobTitle;
  final String company;
  final String bio;
  final String phone;
  final String email;
  final String website;
  final List<Map<String, String>> socialLinks;

  const EditProfileLoaded({
    required this.vcard,
    required this.firstName,
    required this.lastName,
    required this.jobTitle,
    required this.company,
    required this.bio,
    required this.phone,
    required this.email,
    required this.website,
    required this.socialLinks,
  });

  EditProfileLoaded copyWith({
    VCardEntity? vcard,
    String? firstName,
    String? lastName,
    String? jobTitle,
    String? company,
    String? bio,
    String? phone,
    String? email,
    String? website,
    List<Map<String, String>>? socialLinks,
  }) {
    return EditProfileLoaded(
      vcard: vcard ?? this.vcard,
      firstName: firstName ?? this.firstName,
      lastName: lastName ?? this.lastName,
      jobTitle: jobTitle ?? this.jobTitle,
      company: company ?? this.company,
      bio: bio ?? this.bio,
      phone: phone ?? this.phone,
      email: email ?? this.email,
      website: website ?? this.website,
      socialLinks: socialLinks ?? this.socialLinks,
    );
  }

  @override
  List<Object?> get props => [
        vcard,
        firstName,
        lastName,
        jobTitle,
        company,
        bio,
        phone,
        email,
        website,
        socialLinks,
      ];
}

class EditProfileSuccess extends EditProfileState {
  final VCardEntity updatedVCard;

  const EditProfileSuccess({required this.updatedVCard});

  @override
  List<Object?> get props => [updatedVCard];
}

class EditProfileError extends EditProfileState {
  final String message;

  const EditProfileError({required this.message});

  @override
  List<Object?> get props => [message];
}
