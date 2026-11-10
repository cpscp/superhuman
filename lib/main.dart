import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'core/di/service_locator.dart';
import 'features/home/presentation/bloc/home_bloc.dart';
import 'features/home/presentation/bloc/home_event.dart';
import 'features/home/presentation/pages/home_page.dart';

void main() {
  // Setup dependency injection for all features

  setupServiceLocator();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider<HomeBloc>(
          create: (context) =>
              getIt<HomeBloc>()..add(const HomeInitializationRequested()),
        ),
      ],
      child: MaterialApp(
        debugShowCheckedModeBanner: false,
        title: 'NFC vCard',
        theme: ThemeData(primarySwatch: Colors.blue, useMaterial3: true),
        home: const HomePage(),
      ),
    );
  }
}
