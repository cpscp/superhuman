import 'package:flutter/material.dart';

class ActionAreaWidget extends StatelessWidget {
  final VoidCallback onWriteNfc;
  final VoidCallback onEditProfile;
  final VoidCallback onShareQr;
  final bool isWriting;

  const ActionAreaWidget({
    super.key,
    required this.onWriteNfc,
    required this.onEditProfile,
    required this.onShareQr,
    this.isWriting = false,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Primary Action: Write to NFC
          Container(
            width: double.infinity,
            height: 56,
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFF1978E5), Color(0xFF1563BB)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(12),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF1978E5).withValues(alpha: .3),
                  blurRadius: 12,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Material(
              color: Colors.transparent,
              child: InkWell(
                onTap: isWriting ? null : onWriteNfc,
                borderRadius: BorderRadius.circular(12),
                child: Stack(
                  children: [
                    // Shine effect on hover
                    Container(
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(12),
                        gradient: LinearGradient(
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                          colors: [
                            Colors.white.withValues(alpha: 0),
                            Colors.white.withValues(alpha: 0.1),
                            Colors.white.withValues(alpha: 0),
                          ],
                        ),
                      ),
                    ),
                    // Content
                    Center(
                      child: isWriting
                          ? SizedBox(
                              width: 24,
                              height: 24,
                              child: CircularProgressIndicator(
                                strokeWidth: 2.5,
                                valueColor: AlwaysStoppedAnimation<Color>(
                                  Colors.white.withValues(alpha: 0.9),
                                ),
                              ),
                            )
                          : Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(
                                  Icons.sensors,
                                  color: Colors.white,
                                  size: 24,
                                ),
                                const SizedBox(width: 12),
                                const Text(
                                  'Write to NFC Card',
                                  style: TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.bold,
                                    fontSize: 16,
                                    letterSpacing: 0.3,
                                  ),
                                ),
                              ],
                            ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),
          // Secondary Actions
          Row(
            children: [
              // Edit Profile Button
              Expanded(
                child: _SecondaryActionButton(
                  icon: Icons.edit,
                  label: 'Edit Profile',
                  onTap: onEditProfile,
                ),
              ),
              const SizedBox(width: 16),
              // Share QR Button
              Expanded(
                child: _SecondaryActionButton(
                  icon: Icons.qr_code_2,
                  label: 'Share QR',
                  onTap: onShareQr,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          // Helper Text
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                width: 8,
                height: 8,
                decoration: BoxDecoration(
                  color: Colors.green,
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 8),
              Text(
                'Ready to tap on a supported device',
                style: TextStyle(
                  fontSize: 11,
                  color: isDark ? Colors.grey[500] : Colors.grey[600],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _SecondaryActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _SecondaryActionButton({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      height: 48,
      decoration: BoxDecoration(
        color: isDark ? Color(0xFF283039) : Colors.white,
        border: Border.all(
          color: isDark ? Colors.grey[700]! : Colors.grey[300]!,
        ),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                color: isDark ? Colors.white : Colors.grey[800],
                size: 20,
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: isDark ? Colors.white : Colors.grey[800],
                    fontWeight: FontWeight.w600,
                    fontSize: 14,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
