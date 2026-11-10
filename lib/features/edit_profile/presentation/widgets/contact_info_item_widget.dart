import 'package:flutter/material.dart';

class ContactInfoItemWidget extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  final ValueChanged<String> onChanged;
  final bool isToggled;
  final ValueChanged<bool> onToggleChanged;
  final TextInputType keyboardType;

  const ContactInfoItemWidget({
    super.key,
    required this.icon,
    required this.label,
    required this.value,
    required this.onChanged,
    this.isToggled = true,
    required this.onToggleChanged,
    this.keyboardType = TextInputType.text,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(
            color: Colors.grey[200]!,
          ),
        ),
      ),
      child: Row(
        children: [
          // Icon
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: const Color(0xFF1978E5).withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(
              icon,
              color: const Color(0xFF1978E5),
              size: 20,
            ),
          ),
          const SizedBox(width: 12),
          // Input field
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label.toUpperCase(),
                  style: const TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFFA1A8B0),
                    letterSpacing: 0.5,
                  ),
                ),
                TextField(
                  controller: TextEditingController(text: value),
                  onChanged: onChanged,
                  keyboardType: keyboardType,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                  ),
                  decoration: InputDecoration(
                    border: InputBorder.none,
                    contentPadding: const EdgeInsets.only(top: 4),
                    hintStyle: TextStyle(
                      color: Colors.grey[400],
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          // Toggle switch
          GestureDetector(
            onTap: () => onToggleChanged(!isToggled),
            child: Container(
              width: 44,
              height: 24,
              decoration: BoxDecoration(
                color: isToggled ? const Color(0xFF1978E5) : Colors.grey[300],
                borderRadius: BorderRadius.circular(12),
              ),
              child: Stack(
                children: [
                  AnimatedPositioned(
                    duration: const Duration(milliseconds: 200),
                    left: isToggled ? 22 : 2,
                    top: 2,
                    child: Container(
                      width: 20,
                      height: 20,
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(10),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
