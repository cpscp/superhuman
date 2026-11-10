import 'package:equatable/equatable.dart';

abstract class HomeEvent extends Equatable {
  const HomeEvent();

  @override
  List<Object?> get props => [];
}

class HomeInitializationRequested extends HomeEvent {
  const HomeInitializationRequested();
}

class NfcAvailabilityChecked extends HomeEvent {
  const NfcAvailabilityChecked();
}

class WriteVCardToNfcRequested extends HomeEvent {
  const WriteVCardToNfcRequested();
}

class StatusMessageCleared extends HomeEvent {
  const StatusMessageCleared();
}
