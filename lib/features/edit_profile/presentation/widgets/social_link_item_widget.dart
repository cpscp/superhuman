import 'package:flutter/material.dart';

class SocialLinkItemWidget extends StatelessWidget {
  final String platform;
  final String handle;
  final Color platformColor;
  final VoidCallback onDelete;
  final Function(int oldIndex, int newIndex)? onReorder;
  final int index;

  const SocialLinkItemWidget({
    super.key,
    required this.platform,
    required this.handle,
    required this.platformColor,
    required this.onDelete,
    this.onReorder,
    required this.index,
  });

  static const Map<String, Map<String, dynamic>> platformConfig = {
    'linkedin': {'icon': Icons.business, 'color': Color(0xFF0077B5)},
    'twitter': {'icon': Icons.share, 'color': Color(0xFF1DA1F2)},
    'instagram': {'icon': Icons.camera_alt, 'color': Color(0xFFE1306C)},
    'github': {'icon': Icons.code, 'color': Color(0xFF333333)},
    'facebook': {'icon': Icons.people, 'color': Color(0xFF1877F2)},
    'tiktok': {'icon': Icons.music_video, 'color': Color(0xFF000000)},
    'youtube': {'icon': Icons.play_circle, 'color': Color(0xFFFF0000)},
    'website': {'icon': Icons.language, 'color': Color(0xFF1978E5)},
    'email': {'icon': Icons.mail, 'color': Color(0xFF666666)},
  };

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final config = _getPlatformConfig(platform);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: isDark ? Color(0xFF1C2127) : Colors.grey[50],
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: isDark ? Colors.grey[800]! : Colors.grey[200]!,
        ),
      ),
      child: Row(
        children: [
          // Platform icon circle
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: config['color'] as Color,
              borderRadius: BorderRadius.circular(8),
            ),
            child: Center(
              child: Icon(
                config['icon'] as IconData,
                color: Colors.white,
                size: 20,
              ),
            ),
          ),
          const SizedBox(width: 12),
          // Platform info
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  platform,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 2),
                Text(
                  handle,
                  style: TextStyle(
                    fontSize: 12,
                    color: isDark ? Colors.grey[400] : Colors.grey[600],
                    fontWeight: FontWeight.w500,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          // Action buttons
          GestureDetector(
            onTap: onDelete,
            child: Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(borderRadius: BorderRadius.circular(8)),
              child: Icon(
                Icons.delete_outline,
                size: 20,
                color: Colors.red[400],
              ),
            ),
          ),
          const SizedBox(width: 4),
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(borderRadius: BorderRadius.circular(8)),
            child: Icon(
              Icons.drag_handle,
              size: 20,
              color: isDark ? Colors.grey[600] : Colors.grey[500],
            ),
          ),
        ],
      ),
    );
  }

  Map<String, dynamic> _getPlatformConfig(String platform) {
    final key = platform.toLowerCase();
    return platformConfig[key] ??
        {'icon': Icons.link, 'color': const Color(0xFF666666)};
  }
}
