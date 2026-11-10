class VCardEntity {
  final String firstName;
  final String lastName;
  final String? organization;
  final String? organizationRole;
  final String? title;
  final String? mobilePhone;
  final String? workAddress;
  final String? workCity;
  final String? workPostalCode;
  final String? workState;
  final String? workCountry;
  final String? anniversary;
  final List<EmailEntity> emails;
  final List<UrlEntity> urls;
  final String? note;
  final String? bio;
  final String? profilePhotoUrl;

  VCardEntity({
    required this.firstName,
    required this.lastName,
    this.organization,
    this.organizationRole,
    this.title,
    this.mobilePhone,
    this.workAddress,
    this.workCity,
    this.workPostalCode,
    this.workState,
    this.workCountry,
    this.anniversary,
    this.emails = const [],
    this.urls = const [],
    this.note,
    this.bio,
    this.profilePhotoUrl,
  });

  factory VCardEntity.createDefault() {
    return VCardEntity(
      firstName: 'Mazen',
      lastName: 'Alhomsi',
      organization: 'Bytes Of Future',
      organizationRole: 'CEO & Co-Founder',
      title: 'Managing Director',
      mobilePhone: '+4915222481512',
      workAddress: 'Weihnzellerstr.11',
      workCity: 'Nueremberg',
      workPostalCode: '90449',
      workState: 'Bavaria',
      workCountry: 'Germany',
      anniversary: '04-01',
      emails: [
        EmailEntity(type: 'CEO', value: 'ceo@bytesoffuture.com'),
        EmailEntity(type: 'Info', value: 'info@bytesoffuture.com'),
      ],
      urls: [
        UrlEntity(type: 'Website', value: 'https://bytesoffuture.com'),
        UrlEntity(type: 'GitHub', value: 'https://github.com/bytesoffuture'),
        UrlEntity(type: 'X', value: 'https://x.com/bytesoffuture'),
        UrlEntity(
            type: 'LinkedIn',
            value: 'https://linkedin.com/company/bytesoffuture'),
        UrlEntity(type: 'YouTube', value: 'https://youtube.com/@bytesoffuture'),
        UrlEntity(
            type: 'Facebook', value: 'https://facebook.com/bytesoffuture'),
        UrlEntity(
            type: 'Instagram', value: 'https://instagram.com/bytesoffuture'),
        UrlEntity(type: 'Threads', value: 'https://threads.net/@bytesoffuture'),
        UrlEntity(type: 'TikTok', value: 'https://tiktok.com/@bytesoffuture'),
      ],
      note: 'Follow us on social media!',
    );
  }

  VCardEntity copyWith({
    String? firstName,
    String? lastName,
    String? organization,
    String? organizationRole,
    String? title,
    String? mobilePhone,
    String? workAddress,
    String? workCity,
    String? workPostalCode,
    String? workState,
    String? workCountry,
    String? anniversary,
    List<EmailEntity>? emails,
    List<UrlEntity>? urls,
    String? note,
    String? bio,
    String? profilePhotoUrl,
  }) {
    return VCardEntity(
      firstName: firstName ?? this.firstName,
      lastName: lastName ?? this.lastName,
      organization: organization ?? this.organization,
      organizationRole: organizationRole ?? this.organizationRole,
      title: title ?? this.title,
      mobilePhone: mobilePhone ?? this.mobilePhone,
      workAddress: workAddress ?? this.workAddress,
      workCity: workCity ?? this.workCity,
      workPostalCode: workPostalCode ?? this.workPostalCode,
      workState: workState ?? this.workState,
      workCountry: workCountry ?? this.workCountry,
      anniversary: anniversary ?? this.anniversary,
      emails: emails ?? this.emails,
      urls: urls ?? this.urls,
      note: note ?? this.note,
      bio: bio ?? this.bio,
      profilePhotoUrl: profilePhotoUrl ?? this.profilePhotoUrl,
    );
  }
}

class EmailEntity {
  final String type;
  final String value;

  EmailEntity({
    required this.type,
    required this.value,
  });
}

class UrlEntity {
  final String type;
  final String value;

  UrlEntity({
    required this.type,
    required this.value,
  });
}

class SocialLinkEntity {
  final String platform;
  final String handle;
  final String? url;
  final String? iconColor;

  SocialLinkEntity({
    required this.platform,
    required this.handle,
    this.url,
    this.iconColor,
  });
}
