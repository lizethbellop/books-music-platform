import 'dart:async';

import 'package:flutter/material.dart';

import '../../auth/data/services/auth_session_manager.dart';
import '../data/notification_api.dart';
import 'notifications_page.dart';

class NotificationBell extends StatefulWidget {
  const NotificationBell({super.key});
  @override
  State<NotificationBell> createState() => _NotificationBellState();
}

class _NotificationBellState extends State<NotificationBell>
    with WidgetsBindingObserver {
  final NotificationApi _api = NotificationApi();
  int _count = 0;
  String? _user;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    AuthSessionManager.instance.addListener(_sessionChanged);
    _sessionChanged();
  }

  void _sessionChanged() {
    final user = AuthSessionManager.instance.session?.userId;
    if (_user != user) {
      _user = user;
      _count = 0;
      unawaited(_load());
    }
  }

  Future<void> _load() async {
    final user = _user;
    if (user == null) return;
    try {
      final count = await _api.unreadCount();
      if (mounted && _user == user) setState(() => _count = count);
    } catch (_) {
      /* Un fallo de notificaciones no bloquea las otras pantallas. */
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) unawaited(_load());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    AuthSessionManager.instance.removeListener(_sessionChanged);
    _api.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => FloatingActionButton.small(
    heroTag: 'notifications-bell',
    tooltip: 'Notificaciones',
    onPressed: () async {
      await Navigator.of(context).push(
        MaterialPageRoute<void>(builder: (_) => const NotificationsPage()),
      );
      if (mounted) await _load();
    },
    child: Badge(
      isLabelVisible: _count > 0,
      label: Text(_count > 99 ? '99+' : '$_count'),
      child: const Icon(Icons.notifications_outlined),
    ),
  );
}
