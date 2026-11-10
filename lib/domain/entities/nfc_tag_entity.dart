class NfcTagEntity {
  final bool isAvailable;
  final bool isWritable;
  final int maxSize;
  final int currentSize;

  NfcTagEntity({
    required this.isAvailable,
    required this.isWritable,
    required this.maxSize,
    required this.currentSize,
  });
}
