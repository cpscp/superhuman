import 'package:flutter/material.dart';

class EditTextFieldWidget extends StatelessWidget {
  final String placeholder;
  final String? label;
  final String? initialValue;
  final ValueChanged<String> onChanged;
  final TextInputType keyboardType;
  final int maxLines;
  final bool isMultiline;

  const EditTextFieldWidget({
    super.key,
    required this.placeholder,
    this.label,
    this.initialValue,
    required this.onChanged,
    this.keyboardType = TextInputType.text,
    this.maxLines = 1,
    this.isMultiline = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
        decoration: BoxDecoration(
          color: Colors.grey[100],
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (label != null)
              Padding(
                padding: const EdgeInsets.only(left: 16, top: 12, right: 16),
                child: Text(
                  label!.toUpperCase(),
                  style: const TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF1978E5),
                    letterSpacing: 0.5,
                  ),
                ),
              ),
            Padding(
              padding: EdgeInsets.fromLTRB(
                16,
                label != null ? 8 : 16,
                16,
                16,
              ),
              child: isMultiline
                  ? TextField(
                      controller: TextEditingController(text: initialValue),
                      onChanged: onChanged,
                      maxLines: maxLines,
                      keyboardType: keyboardType,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w500,
                      ),
                      decoration: InputDecoration(
                        border: InputBorder.none,
                        hintText: placeholder,
                        hintStyle: TextStyle(
                          color: Colors.grey[400],
                          fontSize: 16,
                        ),
                        contentPadding: EdgeInsets.zero,
                      ),
                    )
                  : TextField(
                      controller: TextEditingController(text: initialValue),
                      onChanged: onChanged,
                      keyboardType: keyboardType,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w500,
                      ),
                      decoration: InputDecoration(
                        border: InputBorder.none,
                        hintText: placeholder,
                        hintStyle: TextStyle(
                          color: Colors.grey[400],
                        ),
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
            ),
          ],
        ));
  }
}
