import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/usecases/write_vcard_to_nfc_usecase.dart';
import '../../domain/usecases/check_nfc_availability_usecase.dart';
import '../../domain/usecases/get_default_vcard_usecase.dart';
import '../../domain/repositories/vcard_repository.dart';
import 'home_event.dart';
import 'home_state.dart';

class HomeBloc extends Bloc<HomeEvent, HomeState> {
  final WriteVCardToNfcUseCase writeVCardToNfcUseCase;
  final CheckNfcAvailabilityUseCase checkNfcAvailabilityUseCase;
  final GetDefaultVCardUseCase getDefaultVCardUseCase;
  final VCardRepository vCardRepository;

  HomeBloc({
    required this.writeVCardToNfcUseCase,
    required this.checkNfcAvailabilityUseCase,
    required this.getDefaultVCardUseCase,
    required this.vCardRepository,
  }) : super(const HomeInitial()) {
    on<HomeInitializationRequested>(_onInitializationRequested);
    on<NfcAvailabilityChecked>(_onNfcAvailabilityChecked);
    on<WriteVCardToNfcRequested>(_onWriteVCardToNfcRequested);
    on<StatusMessageCleared>(_onStatusMessageCleared);
  }

  Future<void> _onInitializationRequested(
    HomeInitializationRequested event,
    Emitter<HomeState> emit,
  ) async {
    emit(const HomeLoading());

    try {
      final isNfcAvailable = await checkNfcAvailabilityUseCase();
      final vcard = getDefaultVCardUseCase();

      if (!isNfcAvailable) {
        emit(
          HomeLoaded(
            vcard: vcard,
            nfcAvailable: false,
            statusMessage: 'NFC is not available on this device',
          ),
        );
        return;
      }

      emit(
        HomeLoaded(
          vcard: vcard,
          nfcAvailable: true,
        ),
      );
    } catch (e) {
      print('Error initializing home: $e');
      emit(HomeError(message: 'Failed to initialize home: $e'));
    }
  }

  Future<void> _onNfcAvailabilityChecked(
    NfcAvailabilityChecked event,
    Emitter<HomeState> emit,
  ) async {
    try {
      final isNfcAvailable = await checkNfcAvailabilityUseCase();

      if (state is HomeLoaded) {
        final currentState = state as HomeLoaded;
        emit(
          currentState.copyWith(nfcAvailable: isNfcAvailable),
        );
      }
    } catch (e) {
      print('Error checking NFC availability: $e');
      emit(HomeError(message: 'Failed to check NFC availability: $e'));
    }
  }

  Future<void> _onWriteVCardToNfcRequested(
    WriteVCardToNfcRequested event,
    Emitter<HomeState> emit,
  ) async {
    try {
      if (state is! HomeLoaded) {
        emit(const HomeError(message: 'Invalid state for writing'));
        return;
      }

      final currentState = state as HomeLoaded;
      emit(currentState.copyWith(
          isWriting: true, statusMessage: 'Tap NFC tag to write...'));

      await writeVCardToNfcUseCase(currentState.vcard);

      emit(currentState.copyWith(
        isWriting: false,
        statusMessage: 'Successfully wrote vCard to NFC tag!',
      ));

      emit(
        const VCardWriteSuccess(
          message: 'Successfully wrote vCard to NFC tag!',
        ),
      );
    } catch (e) {
      print('Error writing vCard to NFC: $e');
      if (state is HomeLoaded) {
        final currentState = state as HomeLoaded;
        emit(
          currentState.copyWith(
            isWriting: false,
            statusMessage: 'Error writing to NFC: $e',
          ),
        );
      }

      emit(VCardWriteFailure(error: e.toString()));
    }
  }

  Future<void> _onStatusMessageCleared(
    StatusMessageCleared event,
    Emitter<HomeState> emit,
  ) async {
    if (state is HomeLoaded) {
      final currentState = state as HomeLoaded;
      emit(currentState.copyWith(statusMessage: null));
    }
  }

  String getVCardString() {
    if (state is HomeLoaded) {
      final currentState = state as HomeLoaded;
      return vCardRepository.generateVCardString(currentState.vcard);
    }
    return 'No vCard data available';
  }
}
