import 'package:flutter/material.dart';

class SocialPlatformConfig {
  final String name;
  final IconData icon;
  final Color color;
  final String placeholder;

  const SocialPlatformConfig({
    required this.name,
    required this.icon,
    required this.color,
    required this.placeholder,
  });
}

class SocialPlatformSelector {
  static const List<SocialPlatformConfig> platforms = [
    SocialPlatformConfig(
      name: 'LinkedIn',
      icon: Icons.business,
      color: Color(0xFF0077B5),
      placeholder: 'linkedin.com/in/yourprofile',
    ),
    SocialPlatformConfig(
      name: 'Twitter',
      icon: Icons.share,
      color: Color(0xFF1DA1F2),
      placeholder: '@yourhandle',
    ),
    SocialPlatformConfig(
      name: 'Instagram',
      icon: Icons.camera_alt,
      color: Color(0xFFE1306C),
      placeholder: '@yourhandle',
    ),
    SocialPlatformConfig(
      name: 'GitHub',
      icon: Icons.code,
      color: Color(0xFF333333),
      placeholder: 'github.com/yourprofile',
    ),
    SocialPlatformConfig(
      name: 'Facebook',
      icon: Icons.people,
      color: Color(0xFF1877F2),
      placeholder: 'facebook.com/yourprofile',
    ),
    SocialPlatformConfig(
      name: 'TikTok',
      icon: Icons.music_video,
      color: Color(0xFF000000),
      placeholder: '@yourhandle',
    ),
    SocialPlatformConfig(
      name: 'YouTube',
      icon: Icons.play_circle,
      color: Color(0xFFFF0000),
      placeholder: 'youtube.com/@yourchannel',
    ),
    SocialPlatformConfig(
      name: 'Website',
      icon: Icons.language,
      color: Color(0xFF1978E5),
      placeholder: 'yourwebsite.com',
    ),
  ];

  static Future<SocialPlatformConfig?> showPlatformPicker(
    BuildContext context,
  ) async {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return showModalBottomSheet<SocialPlatformConfig?>(
      context: context,
      backgroundColor: isDark ? Color(0xFF111821) : Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(20),
          topRight: Radius.circular(20),
        ),
      ),
      builder: (context) => Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Header
            Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: Text(
                'Select Platform',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: isDark ? Colors.white : Colors.black,
                ),
              ),
            ),
            // Platform grid
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
              ),
              itemCount: platforms.length,
              itemBuilder: (context, index) {
                final platform = platforms[index];
                return _PlatformButton(
                  platform: platform,
                  onTap: () => Navigator.pop(context, platform),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _PlatformButton extends StatelessWidget {
  final SocialPlatformConfig platform;
  final VoidCallback onTap;

  const _PlatformButton({required this.platform, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return GestureDetector(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: platform.color.withOpacity(0.1),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: platform.color.withOpacity(0.3),
            width: 1.5,
          ),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: platform.color,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(platform.icon, color: Colors.white, size: 24),
            ),
            const SizedBox(height: 8),
            Expanded(
              child: Center(
                child: Text(
                  platform.name,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: isDark ? Colors.white : Colors.black,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class AddSocialLinkDialog extends StatefulWidget {
  final SocialPlatformConfig platform;

  const AddSocialLinkDialog({super.key, required this.platform});

  @override
  State<AddSocialLinkDialog> createState() => _AddSocialLinkDialogState();
}

class _AddSocialLinkDialogState extends State<AddSocialLinkDialog> {
  late TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return AlertDialog(
      backgroundColor: isDark ? Color(0xFF1C2127) : Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      title: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: widget.platform.color,
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(widget.platform.icon, color: Colors.white, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              'Add ${widget.platform.name}',
              style: TextStyle(color: isDark ? Colors.white : Colors.black),
            ),
          ),
        ],
      ),
      content: TextField(
        controller: _controller,
        decoration: InputDecoration(
          hintText: widget.platform.placeholder,
          hintStyle: TextStyle(
            color: isDark ? Colors.grey[600] : Colors.grey[400],
          ),
          filled: true,
          fillColor: isDark ? Color(0xFF111821) : Colors.grey[100],
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(
              color: isDark ? Colors.grey[800]! : Colors.grey[300]!,
            ),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(
              color: isDark ? Colors.grey[800]! : Colors.grey[300]!,
            ),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(color: widget.platform.color, width: 2),
          ),
        ),
        style: TextStyle(color: isDark ? Colors.white : Colors.black),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text(
            'Cancel',
            style: TextStyle(
              color: isDark ? Colors.grey[400] : Colors.grey[600],
            ),
          ),
        ),
        ElevatedButton(
          style: ElevatedButton.styleFrom(
            backgroundColor: widget.platform.color,
          ),
          onPressed: () {
            if (_controller.text.isNotEmpty) {
              Navigator.pop(context, _controller.text);
            }
          },
          child: const Text('Add', style: TextStyle(color: Colors.white)),
        ),
      ],
    );
  }
}
