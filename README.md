# Smart Business Cards

A modern Flutter application for managing and sharing NFC-enabled business cards with an intuitive, responsive UI.

## Features

- **NFC Card Management**: Write and read vCard data to/from NFC-enabled devices
- **Beautiful Card Preview**: Interactive business card display with glassmorphic design
- **Profile Management**: Edit and manage business card information
- **QR Code Sharing**: Generate and share QR codes for easy contact distribution
- **Dark Mode Support**: Full dark theme implementation across all screens
- **Quick Stats**: View engagement metrics (views, last updated)
- **Bottom Navigation**: Easy navigation between Home, Contacts, Scan, and Profile sections
- **State Management**: BLoC pattern for robust state management

## Project Structure

```
lib/
├── main.dart
├── core/
│   ├── constants/
│   ├── di/ (Dependency Injection)
│   └── utils/
├── domain/
│   ├── entities/
│   ├── repositories/
│   └── usecases/
└── features/
    ├── home/
    │   ├── presentation/
    │   │   ├── pages/
    │   │   │   └── home_page.dart
    │   │   ├── widgets/
    │   │   │   ├── top_bar_widget.dart
    │   │   │   ├── stats_section_widget.dart
    │   │   │   ├── card_preview_widget.dart
    │   │   │   ├── action_area_widget.dart
    │   │   │   └── bottom_nav_widget.dart
    │   │   └── bloc/
    │   │       ├── home_bloc.dart
    │   │       ├── home_event.dart
    │   │       └── home_state.dart
    └── edit_profile/
        └── presentation/
            └── pages/
                └── edit_profile_page.dart
```

## Architecture

This project follows **Clean Architecture** principles with:

- **Presentation Layer**: Widgets and BLoC for UI logic
- **Domain Layer**: Entities, repositories, and use cases
- **Data Layer**: External API calls and local storage

## UI Components

### Home Page

- **Top Bar**: User profile with online status and settings access
- **Stats Section**: Grid display of views and last updated date
- **Card Preview**: Interactive business card with gradient background and QR code
- **Action Area**: Primary NFC write button and secondary action buttons
- **Bottom Navigation**: 4-tab bottom navigation bar

### Key Widgets

- `TopBarWidget`: User profile header with avatar and settings
- `StatsSectionWidget`: Quick stats grid display
- `CardPreviewWidget`: Interactive business card preview with hover effects
- `ActionAreaWidget`: Primary and secondary action buttons with loading states
- `BottomNavWidget`: Material Design bottom navigation

## Getting Started

### Prerequisites

- Flutter SDK (latest version)
- Dart SDK
- Android Studio/VS Code with Flutter extensions
- Android device/emulator with NFC support (for NFC features)

### Installation

1. Clone the repository

```bash
git clone <repository-url>
cd smart_business_cards
```

2. Get dependencies

```bash
flutter pub get
```

3. Generate BLoC boilerplate (if using build_runner)

```bash
flutter pub run build_runner build
```

4. Run the app

```bash
flutter run
```

## Usage

### Writing to NFC Card

1. Navigate to the home page
2. Tap "Write to NFC Card"
3. Hold your device near an NFC card
4. Wait for the write operation to complete

### Editing Profile

1. Tap the settings icon or "Edit Profile" button
2. Update your information
3. Save changes

### Sharing QR Code

1. Tap "Share QR" button
2. Select sharing method
3. Share with contacts

## Dependencies

Key packages used:

- `flutter_bloc`: State management
- `get_it`: Dependency injection
- `dio`: HTTP client
- `nfc_manager`: NFC operations
- `vcard`: vCard parsing and formatting

## Theme & Design

- **Primary Color**: `#1978E5` (Blue)
- **Dark Background**: `#111821`
- **Light Background**: `#F6F7F8`
- **Font Family**: Inter
- **Material Icons**: Material Symbols Outlined

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues or feature requests, please open an issue on the repository.
