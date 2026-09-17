import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import 'app_colors.dart';

class AppTextStyles {
  AppTextStyles._();

  static final logo = GoogleFonts.fraunces(
    fontSize: 38,
    fontWeight: FontWeight.w600,
    color: AppColors.warmWhite,
  );

  static final pageTitle = GoogleFonts.fraunces(
    fontSize: 44,
    fontWeight: FontWeight.w600,
    color: AppColors.ink,
  );

  static final sectionTitle = GoogleFonts.fraunces(
    fontSize: 27,
    fontWeight: FontWeight.w600,
    color: AppColors.ink,
  );

  static final cardTitle = GoogleFonts.manrope(
    fontSize: 16,
    fontWeight: FontWeight.w700,
    color: AppColors.ink,
  );

  static final body = GoogleFonts.manrope(
    fontSize: 15,
    color: AppColors.ink,
  );

  static final secondary = GoogleFonts.manrope(
    fontSize: 13,
    color: AppColors.textSecondary,
  );

  static final navigation = GoogleFonts.manrope(
    fontSize: 15,
    fontWeight: FontWeight.w600,
    color: AppColors.warmWhite,
  );

  static final button = GoogleFonts.manrope(
    fontSize: 14,
    fontWeight: FontWeight.w700,
    color: AppColors.ink,
  );
}