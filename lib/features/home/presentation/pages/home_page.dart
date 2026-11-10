import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:smart_business_cards/core/di/service_locator.dart';

import 'package:smart_business_cards/features/edit_profile/presentation/pages/edit_profile_page.dart';
import 'package:smart_business_cards/features/edit_profile/presentation/bloc/edit_profile_bloc.dart';
import 'package:smart_business_cards/features/edit_profile/presentation/bloc/edit_profile_event.dart';
import '../bloc/home_bloc.dart';
import '../bloc/home_event.dart';
import '../bloc/home_state.dart';
import '../widgets/top_bar_widget.dart';
import '../widgets/stats_section_widget.dart';
import '../widgets/card_preview_widget.dart';
import '../widgets/action_area_widget.dart';
import '../widgets/bottom_nav_widget.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _currentNavIndex = 0;

  @override
  Widget build(BuildContext context) {
    return BlocListener<HomeBloc, HomeState>(
      listener: (context, state) {
        if (state is VCardWriteSuccess) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(state.message),
              backgroundColor: Colors.green,
            ),
          );
        } else if (state is VCardWriteFailure) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(state.error), backgroundColor: Colors.red),
          );
        }
      },
      child: Scaffold(
        body: BlocBuilder<HomeBloc, HomeState>(
          builder: (context, state) {
            if (state is HomeInitial || state is HomeLoading) {
              return const Center(child: CircularProgressIndicator());
            }

            if (state is HomeError || state is NfcNotAvailable) {
              return Center(
                child: Text(
                  state is HomeError
                      ? state.message
                      : 'NFC is not available on this device',
                  style: const TextStyle(color: Colors.red),
                  textAlign: TextAlign.center,
                ),
              );
            }

            if (state is HomeLoaded) {
              final homeBloc = context.read<HomeBloc>();
              return Stack(
                children: [
                  // Main Content
                  CustomScrollView(
                    slivers: [
                      // Top Bar
                      SliverToBoxAdapter(
                        child: TopBarWidget(
                          userName: 'Alex Johnson',
                          userAvatarUrl:
                              'https://lh3.googleusercontent.com/aida-public/AB6AXuA17wFGmugNN8HSeGGkqttQiHc1mp-3GXfQ0DSr1rCGB-_yU3-yvXVwun-Xo3kXBpQwAnSTZ-tNdOdJ520zAWgYverxfvKHY6VOqWIYepH3CoSmB2exeue9SNxTOmlqgnscjt1MP-4gnLN0YbfmmkSari9cSeocAudnOpgkoyMp4Qm8loTKvQGcwyZEZq7TT_EhDn9vec8Zzc7PyZ0LUt4MnXuauukfIPbX8hY3Y5iNWCA_VCZsZO0X7RbvbKTiwdQ9lhvEpxCq19o',
                          onSettingsTap: () {
                            if (state is HomeLoaded) {
                              Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (context) => BlocProvider(
                                    create: (context) =>
                                        getIt<EditProfileBloc>()..add(
                                          EditProfileInitializationRequested(
                                            initialVCard: state.vcard,
                                          ),
                                        ),
                                    child: const EditProfilePage(),
                                  ),
                                ),
                              );
                            }
                          },
                        ),
                      ),
                      // Stats Section
                      SliverToBoxAdapter(
                        child: Padding(
                          padding: const EdgeInsets.only(top: 8, bottom: 24),
                          child: StatsSectionWidget(
                            viewCount: 124,
                            lastUpdated: 'Oct 24',
                          ),
                        ),
                      ),
                      // Card Preview
                      SliverToBoxAdapter(
                        child: Padding(
                          padding: const EdgeInsets.only(bottom: 24),
                          child: CardPreviewWidget(
                            cardholderName: 'Alex Johnson',
                            cardholderTitle: 'Product Designer',
                            companyBrand: 'TechFlow',
                            qrImageUrl:
                                'https://lh3.googleusercontent.com/aida-public/AB6AXuAfIJpJjX9vW-wOJXYc7dh6_6XwoHS44DB1qKJHyXBsstLnMYQkvqjL5Byw2KO1SuhatxsNoCZbHktNvpAgS6ZPhXNNzJegW9YfJGgHOQUo60MVcLAStf19mT6k8GOGEtQwqMbrJRpscBLR7yyKPy2_D2lV3pZKk7teJwVZRdaPbW7osjoQjOsq5qXgyex7_ScLOwryMFFx4Fy8UOLaAiR53PkVUCK8UELdfE0Kwa0CPeDSz_2Tw1a-icqjEiYgqDgLITYWoyXWs5I',
                          ),
                        ),
                      ),
                      // Action Area
                      SliverToBoxAdapter(
                        child: Padding(
                          padding: const EdgeInsets.only(bottom: 32),
                          child: ActionAreaWidget(
                            onWriteNfc: () {
                              if (!state.isWriting) {
                                context.read<HomeBloc>().add(
                                  WriteVCardToNfcRequested(),
                                );
                              }
                            },
                            onEditProfile: () {
                              if (state is HomeLoaded) {
                                final homeState = state as HomeLoaded;
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (context) => BlocProvider(
                                      create: (context) =>
                                          getIt<EditProfileBloc>()..add(
                                            EditProfileInitializationRequested(
                                              initialVCard: homeState.vcard,
                                            ),
                                          ),
                                      child: const EditProfilePage(),
                                    ),
                                  ),
                                );
                              }
                            },
                            onShareQr: () {
                              // TODO: Implement share QR functionality
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                  content: Text('Share QR feature coming soon'),
                                ),
                              );
                            },
                            isWriting: state.isWriting,
                          ),
                        ),
                      ),
                      // Extra spacing for bottom nav
                      const SliverToBoxAdapter(child: SizedBox(height: 20)),
                    ],
                  ),
                  // Bottom Navigation
                  Positioned(
                    bottom: 0,
                    left: 0,
                    right: 0,
                    child: BottomNavWidget(
                      currentIndex: _currentNavIndex,
                      onTabChanged: (index) {
                        setState(() {
                          _currentNavIndex = index;
                        });
                        // TODO: Navigate to different pages based on index
                      },
                    ),
                  ),
                ],
              );
            }

            return const SizedBox.shrink();
          },
        ),
      ),
    );
  }
}
