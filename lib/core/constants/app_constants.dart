class AppConstants {
  // vCard data
  static const String defaultVCardData = '''BEGIN:VCARD
VERSION:3.0
FN:Mazen Alhomsi
N:Mazen;Alhomsi;;;
ORG:Bytes Of Future;CEO & Co-Founder
ANNIVERSARY:04-01
TITLE:Chief Executive Officer
TEL;TYPE=Mobile: +4915222481512
ADR;TYPE=Work:;;Weihnzellerstr.11;Nuremberg;90449;Bavaria;Germany
ROLE:CEO & Co-Founder
EMAIL;TYPE=CEO:ceo@bytesoffuture.com
EMAIL;TYPE=Info:info@bytesoffuture.com
EMAIL;TYPE=Support:support@bytesoffuture.com
URL;TYPE=Website:https://bytesoffuture.com
NOTE:Follow us on social media!
URL;Type=GitHub: https://github.com/bytesoffuture
URL;Type=X: https://x.com/bytesoffuture
URL;Type=LinkedIn: https://linkedin.com/company/bytesoffuture
URL;Type=YouTube: https://youtube.com/@bytesoffuture
URL;Type=Facebook: https://facebook.com/bytesoffuture
URL;Type=Instagram: https://instagram.com/bytesoffuture
URL;Type=Threads: https://threads.net/@bytesoffuture
URL;Type=TikTok: https://tiktok.com/@bytesoffuture
END:VCARD''';

  // Error messages
  static const String nfcNotAvailableError =
      'NFC is not available on this device';
  static const String tagNotWritableError = 'Tag is not writable';
  static const String nfcWriteSuccessMessage =
      'Successfully wrote vCard to NFC tag!';

  // UI strings
  static const String appTitle = 'NFC vCard Writer';
  static const String writeToNfcButton = 'Write to NFC';
  static const String tapNfcTagMessage = 'Tap NFC tag to write...';
  static const String writingToTagMessage = 'Writing to tag...';
  static const String vCardDataLabel = 'vCard Data to Write:';
  static const String copyVCardButton = 'Copy vCard';
  static const String vCardCopiedMessage = 'vCard copied to clipboard!';
}
