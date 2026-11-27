import 'package:flutter/material.dart';

import '../../data/models/activity_model.dart';
import '../../data/models/community_model.dart';
import '../../data/services/activity_api_service.dart';
import '../widgets/activity_card.dart';
import '../widgets/create_activity_dialog.dart';

class CommunityActivitiesPage extends StatefulWidget {
  final CommunityModel community;
  final String currentUserId;
  final bool embedded;

  const CommunityActivitiesPage({
    super.key,
    required this.community,
    required this.currentUserId,
    this.embedded = false,
  });

  @override
  State<CommunityActivitiesPage> createState() => _CommunityActivitiesPageState();
}

class _CommunityActivitiesPageState extends State<CommunityActivitiesPage> {
  final ActivityApiService _activityService = ActivityApiService();

  List<ActivityModel> _activities = [];
  bool _isLoading = true;
  String? _errorMessage;
  final Set<String> _busyActivityIds = {};

  bool get _isAdmin => widget.community.isAdmin;

  @override
  void initState() {
    super.initState();
    if (!widget.community.isPrivate || widget.community.isMember) {
      _loadActivities();
    } else {
      _isLoading = false;
    }
  }

  Future<void> _loadActivities() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final list = await _activityService.getCommunityActivities(widget.community.id);
      if (!mounted) return;
      setState(() {
        _activities = list;
        _isLoading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _errorMessage = e.toString();
        _isLoading = false;
      });
    }
  }

  Future<void> _openCreateActivityDialog() async {
    final request = await showDialog<ActivityCreateRequestModel>(
      context: context,
      builder: (_) => const CreateActivityDialog(),
    );
    if (request == null || !mounted) return;

    try {
      await _activityService.createActivity(widget.community.id, request);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Actividad creada con éxito.')),
      );
      await _loadActivities();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString())),
      );
    }
  }

  Future<void> _toggleAttendance(ActivityModel activity) async {
    if (!widget.community.isMember) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Únete a la comunidad para confirmar asistencia.')),
      );
      return;
    }

    setState(() => _busyActivityIds.add(activity.id));
    try {
      if (activity.isParticipating) {
        await _activityService.cancelAttendance(widget.community.id, activity.id);
      } else {
        await _activityService.confirmAttendance(widget.community.id, activity.id);
      }
      await _loadActivities();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString())),
      );
    } finally {
      if (mounted) {
        setState(() => _busyActivityIds.remove(activity.id));
      }
    }
  }

  Future<void> _cancelActivity(ActivityModel activity) async {
    try {
      await _activityService.updateActivity(
        widget.community.id,
        activity.id,
        const ActivityUpdateRequestModel(status: 'CANCELADA'),
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Actividad cancelada.')),
      );
      await _loadActivities();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString())),
      );
    }
  }

  Future<void> _deleteActivity(ActivityModel activity) async {
    try {
      await _activityService.deleteActivity(widget.community.id, activity.id);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Actividad eliminada.')),
      );
      await _loadActivities();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString())),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final content = _buildBody(context);

    if (widget.embedded) {
      return Stack(
        children: [
          content,
          if (_isAdmin)
            Positioned(
              right: 16,
              bottom: 16,
              child: FloatingActionButton.extended(
                heroTag: 'create_activity_fab',
                onPressed: _openCreateActivityDialog,
                icon: const Icon(Icons.add_rounded),
                label: const Text('Actividad'),
              ),
            ),
        ],
      );
    }

    return Scaffold(
      appBar: AppBar(title: Text('Actividades • ${widget.community.name}')),
      floatingActionButton: _isAdmin
          ? FloatingActionButton.extended(
              onPressed: _openCreateActivityDialog,
              icon: const Icon(Icons.add_rounded),
              label: const Text('Nueva actividad'),
            )
          : null,
      body: content,
    );
  }

  Widget _buildBody(BuildContext context) {
    
    if (widget.community.isPrivate && !widget.community.isMember) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.lock_outline_rounded, size: 48),
              SizedBox(height: 12),
              Text(
                'Esta comunidad es privada. Solicita unirte para ver sus actividades.',
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      );
    }

    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(_errorMessage!, textAlign: TextAlign.center),
              const SizedBox(height: 12),
              FilledButton(
                onPressed: _loadActivities,
                child: const Text('Reintentar'),
              ),
            ],
          ),
        ),
      );
    }

    if (_activities.isEmpty) {
      return const Center(
        child: Text('Aún no hay actividades programadas en esta comunidad.'),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadActivities,
      child: ListView.separated(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 88),
        itemCount: _activities.length,
        separatorBuilder: (_, _) => const SizedBox(height: 12),
        itemBuilder: (context, index) {
          final activity = _activities[index];
          final canManage = _isAdmin || activity.createdBy == widget.currentUserId;

          return ActivityCard(
            activity: activity,
            canManage: canManage,
            isTogglingAttendance: _busyActivityIds.contains(activity.id),
            onToggleAttendance: () => _toggleAttendance(activity),
            onCancelActivity: () => _cancelActivity(activity),
            onDeleteActivity: () => _deleteActivity(activity),
          );
        },
      ),
    );
  }
}