import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import 'app_colors.dart';

class AppTheme {
  AppTheme._();

  static ThemeData get light {
    return ThemeData(
      useMaterial3: true,
      scaffoldBackgroundColor: AppColors.cream,

      colorScheme: const ColorScheme.light(
        primary: AppColors.lavender,
        secondary: AppColors.sage,
        surface: AppColors.warmWhite,
        onPrimary: AppColors.ink,
        onSecondary: AppColors.ink,
        onSurface: AppColors.ink,
      ),

      textTheme: GoogleFonts.manropeTextTheme(),

      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.warmWhite,

        hintStyle: GoogleFonts.manrope(
          color: AppColors.textSecondary,
          fontSize: 14,
        ),

        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(
            color: AppColors.border,
          ),
        ),

        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(
            color: AppColors.border,
          ),
        ),

        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(
            color: AppColors.lavender,
            width: 2,
          ),
        ),
      ),
    );
  }
}