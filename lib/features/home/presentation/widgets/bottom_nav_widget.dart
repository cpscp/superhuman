import 'package:flutter/material.dart';

class BottomNavWidget extends StatelessWidget {
  final int currentIndex;
  final Function(int) onTabChanged;

  const BottomNavWidget({
    super.key,
    required this.currentIndex,
    required this.onTabChanged,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      decoration: BoxDecoration(
        color: isDark ? Color(0xFF111418) : Colors.white,
        border: Border(
          top: BorderSide(
            color: isDark ? Colors.grey[800]! : Colors.grey[300]!,
          ),
        ),
      ),
      child: SafeArea(
        top: false,
        child: SizedBox(
          height: 80,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _NavItem(
                icon: Icons.home,
                label: 'Home',
                isActive: currentIndex == 0,
                onTap: () => onTabChanged(0),
              ),
              _NavItem(
                icon: Icons.contacts,
                label: 'Contacts',
                isActive: currentIndex == 1,
                onTap: () => onTabChanged(1),
              ),
              _NavItem(
                icon: Icons.center_focus_weak,
                label: 'Scan',
                isActive: currentIndex == 2,
                onTap: () => onTabChanged(2),
              ),
              _NavItem(
                icon: Icons.person,
                label: 'Profile',
                isActive: currentIndex == 3,
                onTap: () => onTabChanged(3),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _NavItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool isActive;
  final VoidCallback onTap;

  const _NavItem({
    required this.icon,
    required this.label,
    required this.isActive,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: isActive
                    ? Color(0xFF1978E5).withValues(alpha: 0.1)
                    : Colors.transparent,
              ),
              child: Icon(
                icon,
                size: 24,
                color: isActive
                    ? Color(0xFF1978E5)
                    : (Theme.of(context).brightness == Brightness.dark
                          ? Colors.grey[500]
                          : Colors.grey[600]),
              ),
            ),
            const SizedBox(height: 4),
            Text(
              label,
              style: TextStyle(
                fontSize: 10,
                fontWeight: FontWeight.w500,
                color: isActive
                    ? Color(0xFF1978E5)
                    : (Theme.of(context).brightness == Brightness.dark
                          ? Colors.grey[500]
                          : Colors.grey[600]),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
