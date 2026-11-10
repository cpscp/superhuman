import 'package:vcard_maintained/vcard_maintained.dart';

import '../../domain/repositories/vcard_repository.dart';
import '../../domain/entities/vcard_entity.dart';

class VCardRepositoryImpl implements VCardRepository {
  @override
  String generateVCardString(VCardEntity vcard) {
    try {
      final vcardLib = VCard();

      // Set basic information
      vcardLib.firstName = vcard.firstName;
      vcardLib.lastName = vcard.lastName;

      // Set organization info
      if (vcard.organization != null) {
        vcardLib.organization = vcard.organization;
      }
      if (vcard.title != null || vcard.organizationRole != null) {
        vcardLib.role = vcard.title ?? vcard.organizationRole ?? '';
      }

      // Set phone
      if (vcard.mobilePhone != null) {
        vcardLib.cellPhone = vcard.mobilePhone;
      }

      // Set emails
      if (vcard.emails.isNotEmpty) {
        vcardLib.email = vcard.emails.map((e) => e.value).toList().join(', ');
      }

      // Set URLs
      if (vcard.urls.isNotEmpty) {
        vcardLib.url = vcard.urls.first.value;
        if (vcard.urls.length > 1) {
          final socials = vcard.urls
              .skip(1)
              .map((url) => '${url.type}: ${url.value}')
              .join('; ');
          if ((vcard.note ?? '').isNotEmpty) {
            vcardLib.note = '${vcard.note}\n$socials';
          } else {
            vcardLib.note = socials;
          }
        }
      }

      // Set address
      if (vcard.workAddress != null ||
          vcard.workCity != null ||
          vcard.workPostalCode != null ||
          vcard.workState != null ||
          vcard.workCountry != null) {
        final addressParts = [
          if (vcard.workAddress != null) vcard.workAddress,
          if (vcard.workCity != null) vcard.workCity,
          if (vcard.workPostalCode != null) vcard.workPostalCode,
          if (vcard.workState != null) vcard.workState,
          if (vcard.workCountry != null) vcard.workCountry,
        ];
        final addressString = addressParts.whereType<String>().join(', ');
        if ((vcard.note ?? '').isNotEmpty) {
          vcardLib.note = 'Address: $addressString\n${vcard.note}';
        } else {
          vcardLib.note = 'Address: $addressString';
        }
      }

      // Set anniversary
      if (vcard.anniversary != null) {
        try {
          final parts = vcard.anniversary!.split('-');
          if (parts.length == 2) {
            final month = int.parse(parts[0]);
            final day = int.parse(parts[1]);
            vcardLib.anniversary = DateTime(DateTime.now().year, month, day);
          }
        } catch (e) {
          print('Warning: Invalid anniversary format: ${vcard.anniversary}');
        }
      }

      // Set note
      if (vcard.note != null) {
        vcardLib.note = vcard.note;
      }

      return vcardLib.getFormattedString();
    } catch (e) {
      print('Error generating vCard string: $e');
      rethrow;
    }
  }

  @override
  VCardEntity getDefaultVCard() {
    return VCardEntity.createDefault();
  }

  @override
  bool validateVCard(VCardEntity vcard) {
    // Basic validation
    if (vcard.firstName.isEmpty && vcard.lastName.isEmpty) {
      print('Warning: vCard must have at least a first or last name');
      return false;
    }
    return true;
  }
}
