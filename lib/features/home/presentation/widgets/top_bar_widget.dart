import 'package:flutter/material.dart';

class TopBarWidget extends StatelessWidget {
  final String userName;
  final String userAvatarUrl;
  final VoidCallback onSettingsTap;
  final bool isOnline;

  const TopBarWidget({
    super.key,
    required this.userName,
    required this.userAvatarUrl,
    required this.onSettingsTap,
    this.isOnline = true,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      padding: const EdgeInsets.fromLTRB(24, 32, 24, 20),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          // User Info Section
          Expanded(
            child: Row(
              children: [
                // Avatar with Online Indicator
                Stack(
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: isDark ? Colors.grey[700]! : Colors.grey[300]!,
                          width: 2,
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.1),
                            blurRadius: 4,
                          ),
                        ],
                      ),
                      child: CircleAvatar(
                        backgroundImage: NetworkImage(userAvatarUrl),
                      ),
                    ),
                    if (isOnline)
                      Positioned(
                        bottom: 0,
                        right: 0,
                        child: Container(
                          width: 12,
                          height: 12,
                          decoration: BoxDecoration(
                            color: Colors.green,
                            shape: BoxShape.circle,
                            border: Border.all(
                              color: isDark
                                  ? Color(0xFF111821)
                                  : Color(0xFFF6F7F8),
                              width: 2,
                            ),
                          ),
                        ),
                      ),
                  ],
                ),
                const SizedBox(width: 12),
                // User Name and Greeting
                Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Welcome back',
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.w500,
                        color: isDark ? Colors.grey[500] : Colors.grey[600],
                        letterSpacing: 0.5,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      userName,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        letterSpacing: -0.5,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          // Settings Button
          Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: onSettingsTap,
              borderRadius: BorderRadius.circular(20),
              child: Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: isDark ? Color(0xFF1C2127) : Colors.grey[200],
                ),
                child: Icon(
                  Icons.settings,
                  color: isDark ? Colors.grey[400] : Colors.grey[700],
                  size: 20,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
