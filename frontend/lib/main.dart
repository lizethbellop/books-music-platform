import 'package:flutter/material.dart';

import 'core/routes/app_shell.dart';
import 'core/theme/app_theme.dart';

void main() {
  runApp(const MusaApp());
}

class MusaApp extends StatelessWidget {
  const MusaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'musa.',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      home: const AppShell(),
    );
  }
}