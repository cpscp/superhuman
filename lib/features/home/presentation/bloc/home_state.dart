import 'package:equatable/equatable.dart';

import '../../domain/entities/vcard_entity.dart';

abstract class HomeState extends Equatable {
  const HomeState();

  @override
  List<Object?> get props => [];
}

class HomeInitial extends HomeState {
  const HomeInitial();
}

class HomeLoading extends HomeState {
  const HomeLoading();
}

class HomeLoaded extends HomeState {
  final VCardEntity vcard;
  final bool nfcAvailable;
  final String? statusMessage;
  final bool isWriting;

  const HomeLoaded({
    required this.vcard,
    required this.nfcAvailable,
    this.statusMessage,
    this.isWriting = false,
  });

  HomeLoaded copyWith({
    VCardEntity? vcard,
    bool? nfcAvailable,
    String? statusMessage,
    bool? isWriting,
  }) {
    return HomeLoaded(
      vcard: vcard ?? this.vcard,
      nfcAvailable: nfcAvailable ?? this.nfcAvailable,
      statusMessage: statusMessage ?? this.statusMessage,
      isWriting: isWriting ?? this.isWriting,
    );
  }

  @override
  List<Object?> get props => [vcard, nfcAvailable, statusMessage, isWriting];
}

class HomeError extends HomeState {
  final String message;

  const HomeError({required this.message});

  @override
  List<Object?> get props => [message];
}

class NfcNotAvailable extends HomeState {
  const NfcNotAvailable();
}

class VCardWriteSuccess extends HomeState {
  final String message;

  const VCardWriteSuccess({required this.message});

  @override
  List<Object?> get props => [message];
}

class VCardWriteFailure extends HomeState {
  final String error;

  const VCardWriteFailure({required this.error});

  @override
  List<Object?> get props => [error];
}
