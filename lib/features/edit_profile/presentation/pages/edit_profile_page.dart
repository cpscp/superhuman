import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:smart_business_cards/core/utils/logger.dart';

import '../bloc/edit_profile_bloc.dart';
import '../bloc/edit_profile_event.dart';
import '../bloc/edit_profile_state.dart';
import '../widgets/edit_text_field_widget.dart';
import '../widgets/contact_info_item_widget.dart';
import '../widgets/social_link_item_widget.dart';
import '../widgets/social_platform_selector.dart';

class EditProfilePage extends StatefulWidget {
  const EditProfilePage({super.key});

  @override
  State<EditProfilePage> createState() => _EditProfilePageState();
}

class _EditProfilePageState extends State<EditProfilePage> {
  bool phoneToggled = true;
  bool emailToggled = true;
  bool websiteToggled = false;

  @override
  Widget build(BuildContext context) {
    return BlocListener<EditProfileBloc, EditProfileState>(
      listener: (context, state) {
        if (state is EditProfileSuccess) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Profile saved successfully!')),
          );
          Navigator.pop(context, state.updatedVCard);
        } else if (state is EditProfileError) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(state.message), backgroundColor: Colors.red),
          );
        }
      },
      child: Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.close),
            onPressed: () => Navigator.pop(context),
          ),
          title: const Text(
            'Edit Profile',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          centerTitle: true,
          actions: [
            TextButton(
              onPressed: () {
                context.read<EditProfileBloc>().add(
                  const EditProfileSaveRequested(),
                );
              },
              child: const Text(
                'Save',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1978E5),
                ),
              ),
            ),
          ],
        ),
        body: BlocBuilder<EditProfileBloc, EditProfileState>(
          builder: (context, state) {
            if (state is EditProfileInitial || state is EditProfileLoading) {
              return const Center(child: CircularProgressIndicator());
            }

            if (state is EditProfileError) {
              return Center(
                child: Text(
                  state.message,
                  style: const TextStyle(color: Colors.red),
                  textAlign: TextAlign.center,
                ),
              );
            }

            if (state is EditProfileLoaded) {
              return SingleChildScrollView(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Profile Photo Section
                    Center(
                      child: Column(
                        children: [
                          Stack(
                            alignment: Alignment.bottomRight,
                            children: [
                              Container(
                                width: 128,
                                height: 128,
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(64),
                                  border: Border.all(
                                    color: Colors.white,
                                    width: 4,
                                  ),
                                  image: DecorationImage(
                                    image: NetworkImage(
                                      state.vcard.profilePhotoUrl ??
                                          'https://via.placeholder.com/128',
                                    ),
                                    fit: BoxFit.cover,
                                  ),
                                ),
                              ),
                              Container(
                                width: 40,
                                height: 40,
                                decoration: BoxDecoration(
                                  color: const Color(0xFF1978E5),
                                  borderRadius: BorderRadius.circular(20),
                                  border: Border.all(
                                    color: Colors.white,
                                    width: 3,
                                  ),
                                ),
                                child: const Icon(
                                  Icons.edit,
                                  color: Colors.white,
                                  size: 20,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                          const Text(
                            'Change Photo',
                            style: TextStyle(
                              fontSize: 14,
                              color: Color(0xFF1978E5),
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          Text(
                            'Tap to upload a new avatar',
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.grey[500],
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 32),

                    // Identity Section
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Padding(
                          padding: const EdgeInsets.only(left: 4, bottom: 12),
                          child: Text(
                            'IDENTITY',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                              color: Colors.grey[600],
                              letterSpacing: 0.5,
                            ),
                          ),
                        ),
                        EditTextFieldWidget(
                          placeholder: 'Full Name',
                          label: 'Full Name',
                          initialValue: state.firstName,
                          onChanged: (value) {
                            context.read<EditProfileBloc>().add(
                              VCardFieldsUpdated(
                                firstName: value,
                                lastName: state.lastName,
                                jobTitle: state.jobTitle,
                                company: state.company,
                                bio: state.bio,
                                phone: state.phone,
                                email: state.email,
                                website: state.website,
                              ),
                            );
                          },
                        ),
                        const SizedBox(height: 12),
                        EditTextFieldWidget(
                          placeholder: 'Job Title',
                          label: 'Job Title',
                          initialValue: state.jobTitle,
                          onChanged: (value) {
                            context.read<EditProfileBloc>().add(
                              VCardFieldsUpdated(
                                firstName: state.firstName,
                                lastName: state.lastName,
                                jobTitle: value,
                                company: state.company,
                                bio: state.bio,
                                phone: state.phone,
                                email: state.email,
                                website: state.website,
                              ),
                            );
                          },
                        ),
                        const SizedBox(height: 12),
                        EditTextFieldWidget(
                          placeholder: 'Company Name',
                          label: 'Company',
                          initialValue: state.company,
                          onChanged: (value) {
                            context.read<EditProfileBloc>().add(
                              VCardFieldsUpdated(
                                firstName: state.firstName,
                                lastName: state.lastName,
                                jobTitle: state.jobTitle,
                                company: value,
                                bio: state.bio,
                                phone: state.phone,
                                email: state.email,
                                website: state.website,
                              ),
                            );
                          },
                        ),
                        const SizedBox(height: 12),
                        EditTextFieldWidget(
                          placeholder: 'Write a short bio...',
                          label: 'Bio',
                          initialValue: state.bio,
                          onChanged: (value) {
                            context.read<EditProfileBloc>().add(
                              VCardFieldsUpdated(
                                firstName: state.firstName,
                                lastName: state.lastName,
                                jobTitle: state.jobTitle,
                                company: state.company,
                                bio: value,
                                phone: state.phone,
                                email: state.email,
                                website: state.website,
                              ),
                            );
                          },
                          maxLines: 3,
                          isMultiline: true,
                        ),
                      ],
                    ),
                    const SizedBox(height: 32),

                    // Contact Details Section
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Padding(
                          padding: const EdgeInsets.only(left: 4, bottom: 12),
                          child: Text(
                            'CONTACT DETAILS',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                              color: Colors.grey[600],
                              letterSpacing: 0.5,
                            ),
                          ),
                        ),
                        Container(
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(color: Colors.grey[200]!),
                          ),
                          child: Column(
                            children: [
                              ContactInfoItemWidget(
                                icon: Icons.phone,
                                label: 'Mobile',
                                value: state.phone,
                                onChanged: (value) {
                                  context.read<EditProfileBloc>().add(
                                    VCardFieldsUpdated(
                                      firstName: state.firstName,
                                      lastName: state.lastName,
                                      jobTitle: state.jobTitle,
                                      company: state.company,
                                      bio: state.bio,
                                      phone: value,
                                      email: state.email,
                                      website: state.website,
                                    ),
                                  );
                                },
                                isToggled: phoneToggled,
                                onToggleChanged: (value) {
                                  setState(() {
                                    phoneToggled = value;
                                  });
                                },
                                keyboardType: TextInputType.phone,
                              ),
                              ContactInfoItemWidget(
                                icon: Icons.mail,
                                label: 'Work Email',
                                value: state.email,
                                onChanged: (value) {
                                  context.read<EditProfileBloc>().add(
                                    VCardFieldsUpdated(
                                      firstName: state.firstName,
                                      lastName: state.lastName,
                                      jobTitle: state.jobTitle,
                                      company: state.company,
                                      bio: state.bio,
                                      phone: state.phone,
                                      email: value,
                                      website: state.website,
                                    ),
                                  );
                                },
                                isToggled: emailToggled,
                                onToggleChanged: (value) {
                                  setState(() {
                                    emailToggled = value;
                                  });
                                },
                                keyboardType: TextInputType.emailAddress,
                              ),
                              ContactInfoItemWidget(
                                icon: Icons.language,
                                label: 'Website',
                                value: state.website,
                                onChanged: (value) {
                                  context.read<EditProfileBloc>().add(
                                    VCardFieldsUpdated(
                                      firstName: state.firstName,
                                      lastName: state.lastName,
                                      jobTitle: state.jobTitle,
                                      company: state.company,
                                      bio: state.bio,
                                      phone: state.phone,
                                      email: state.email,
                                      website: value,
                                    ),
                                  );
                                },
                                isToggled: websiteToggled,
                                onToggleChanged: (value) {
                                  setState(() {
                                    websiteToggled = value;
                                  });
                                },
                                keyboardType: TextInputType.url,
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 32),

                    // Social Links Section
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Padding(
                          padding: const EdgeInsets.only(left: 4, bottom: 12),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                'SOCIAL PRESENCE',
                                style: TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.grey[600],
                                  letterSpacing: 0.5,
                                ),
                              ),
                              GestureDetector(
                                onTap: () async {
                                  // Show platform selector
                                  final selectedPlatform =
                                      await SocialPlatformSelector.showPlatformPicker(
                                        context,
                                      );

                                  if (selectedPlatform != null && mounted) {
                                    // Show input dialog for the handle/URL
                                    final result = await showDialog<String>(
                                      context: context,
                                      builder: (context) => AddSocialLinkDialog(
                                        platform: selectedPlatform,
                                      ),
                                    );

                                    if (result != null && mounted) {
                                      Logger.info(
                                        'Added social link: ${selectedPlatform.name} = $result',
                                      );
                                      // TODO: Add to state via BLoC event
                                    }
                                  }
                                },
                                child: const Row(
                                  children: [
                                    Icon(
                                      Icons.add_circle,
                                      color: Color(0xFF1978E5),
                                      size: 18,
                                    ),
                                    SizedBox(width: 4),
                                    Text(
                                      'Add Link',
                                      style: TextStyle(
                                        fontSize: 12,
                                        fontWeight: FontWeight.bold,
                                        color: Color(0xFF1978E5),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                        if (state.socialLinks.isNotEmpty)
                          Column(
                            children: List.generate(state.socialLinks.length, (
                              index,
                            ) {
                              final link = state.socialLinks[index];
                              return Padding(
                                padding: const EdgeInsets.only(bottom: 12),
                                child: SocialLinkItemWidget(
                                  platform: link['type'] ?? 'Social Link',
                                  handle: link['value'] ?? '',
                                  platformColor: const Color(0xFF0077B5),
                                  onDelete: () {
                                    Logger.info(
                                      'Delete social link: ${link['type']}',
                                    );
                                  },
                                  index: index,
                                ),
                              );
                            }),
                          ),
                      ],
                    ),
                    const SizedBox(height: 100),
                  ],
                ),
              );
            }

            return const SizedBox.shrink();
          },
        ),
        bottomSheet: Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: MediaQuery.of(context).padding.bottom + 16,
          ),
          child: SizedBox(
            width: double.infinity,
            height: 56,
            child: ElevatedButton.icon(
              onPressed: () {
                context.read<EditProfileBloc>().add(
                  const EditProfileSaveRequested(),
                );
              },
              icon: const Icon(Icons.nfc, size: 24),
              label: const Text(
                'Write to NFC Card',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 0.5,
                ),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF1978E5),
                foregroundColor: Colors.white,
                elevation: 4,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
