import 'package:flutter/material.dart';

import '../../data/models/community_member_model.dart';
import '../../data/models/community_model.dart';
import '../../data/services/community_api_service.dart';
import '../widgets/member_list_tile.dart';
import 'community_activities_page.dart';
import '../widgets/edit_community_dialog.dart';

class CommunityDetailPage extends StatefulWidget {
  final CommunityModel initialCommunity;
  final String currentUserId;

  const CommunityDetailPage({
    super.key,
    required this.initialCommunity,
    required this.currentUserId,
  });

  @override
  State<CommunityDetailPage> createState() => _CommunityDetailPageState();
}

class _CommunityDetailPageState extends State<CommunityDetailPage> {
  final CommunityApiService _communityService = CommunityApiService();

  late CommunityModel _community = widget.initialCommunity;
  List<CommunityMemberModel> _members = [];
  bool _loadingMembers = true;
  bool _processingMembership = false;

  Widget _buildMembershipButton() {
    if (_community.isMember) {
      return OutlinedButton(
        onPressed: _processingMembership ? null : _handleMembershipAction,
        child: const Text('Salir'),
      );
    }
    if (_community.isPending) {
      return OutlinedButton.icon(
        onPressed: _processingMembership ? null : _handleMembershipAction,
        icon: const Icon(Icons.hourglass_top_rounded, size: 16),
        label: const Text('Cancelar solicitud'),
      );
    }
    return FilledButton(
      onPressed: _processingMembership ? null : _handleMembershipAction,
      child: Text(_community.isPrivate ? 'Solicitar unirse' : 'Unirse'),
    );
  }

  bool get _canViewPrivateContent =>
      !_community.isPrivate || _community.isMember;

  @override
  void initState() {
    super.initState();
    _refreshAll();
  }

  Future<void> _refreshAll() async {
    setState(() => _loadingMembers = true);
    try {
      final updated = await _communityService.getCommunityById(_community.id);
      if (!mounted) return;

      List<CommunityMemberModel> members = [];
      if (!updated.isPrivate || updated.isMember) {
        members = await _communityService.getCommunityMembers(_community.id);
      }

      if (!mounted) return;
      setState(() {
        _community = updated;
        _members = members;
        _loadingMembers = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _loadingMembers = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString())),
      );
    }
  }

    Future<void> _openEditCommunityDialog() async {
    final request = await showDialog<CommunityUpdateRequestModel>(
      context: context,
      builder: (_) => EditCommunityDialog(community: _community),
    );
    if (request == null || !mounted) return;

    try {
      await _communityService.updateCommunity(_community.id, request);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Comunidad actualizada.')),
      );
      await _refreshAll();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString())),
      );
    }
}

  Future<void> _handleMembershipAction() async {
    setState(() => _processingMembership = true);
    try {
      if (_community.isMember || _community.isPending) {
        final wasPending = _community.isPending;
        await _communityService.leaveCommunity(_community.id);
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              wasPending
                  ? 'Solicitud cancelada.'
                  : 'Has salido de la comunidad.',
            ),
          ),
        );
      } else {
        await _communityService.joinCommunity(_community.id);
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              _community.isPrivate
                  ? 'Solicitud enviada al administrador.'
                  : 'Te has unido a la comunidad.',
            ),
          ),
        );
      }
      await _refreshAll();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString())),
      );
    } finally {
      if (mounted) {
        setState(() => _processingMembership = false);
      }
    }
  }

  Future<void> _handleApproveMember(CommunityMemberModel member) async {
    try {
      await _communityService.approveMember(_community.id, member.userId);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Solicitud aprobada.')),
      );
      await _refreshAll();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString())),
      );
    }
  }

  Future<void> _handleRejectMember(CommunityMemberModel member) async {
    try {
      await _communityService.rejectOrRemoveMember(_community.id, member.userId);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Solicitud rechazada.')),
      );
      await _refreshAll();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString())),
      );
    }
  }

  Future<void> _confirmDeleteCommunity() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Eliminar comunidad'),
        content: Text(
          '¿Seguro que deseas eliminar "${_community.name}"? Esta acción no se puede deshacer.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Eliminar'),
          ),
        ],
      ),
    );

    if (confirmed != true || !mounted) return;

    try {
      await _communityService.deleteCommunity(_community.id);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Comunidad eliminada.')),
      );
      Navigator.of(context).pop(true);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString())),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: Text(_community.name),
          actions: [
            if (_community.isAdmin) ...[
              IconButton(
                tooltip: 'Editar comunidad',
                icon: const Icon(Icons.edit_outlined),
                onPressed: _openEditCommunityDialog,
              ),
              IconButton(
                tooltip: 'Eliminar comunidad',
                icon: const Icon(Icons.delete_outline_rounded),
                onPressed: _confirmDeleteCommunity,
              ),
            ],
          ],
        ),
        body: Column(
          children: [
            // Cabecera con información y botón Unirse / Salir
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: colorScheme.surfaceContainerLow,
                border: Border(
                  bottom: BorderSide(
                    color: colorScheme.outlineVariant.withValues(alpha: 0.5),
                  ),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    (_community.description != null &&
                            _community.description!.trim().isNotEmpty)
                        ? _community.description!
                        : 'Sin descripción disponible.',
                    style: theme.textTheme.bodyMedium,
                  ),
                  const SizedBox(height: 14),
                  Row(
                    children: [
                      Chip(
                        avatar: const Icon(Icons.category_outlined, size: 16),
                        label: Text(_community.type),
                        visualDensity: VisualDensity.compact,
                      ),
                      const SizedBox(width: 8),
                      Chip(
                        avatar: const Icon(Icons.people_outline_rounded, size: 16),
                        label: Text('${_community.memberCount} miembros'),
                        visualDensity: VisualDensity.compact,
                      ),
                      const Spacer(),
                      _buildMembershipButton(),
                    ],
                  ),
                ],
              ),
            ),

            // Pestañas: Actividades y Miembros
            const TabBar(
              tabs: [
                Tab(text: 'Actividades'),
                Tab(text: 'Miembros'),
              ],
            ),

            Expanded(
              child: TabBarView(
                children: [
                  CommunityActivitiesPage(
                    community: _community,
                    currentUserId: widget.currentUserId,
                    embedded: true,
                  ),
                  _buildMembersTab(),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMembersTab() {
    if (!_canViewPrivateContent) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.lock_outline_rounded, size: 48),
              SizedBox(height: 12),
              Text(
                'Esta comunidad es privada. Solicita unirte para ver sus integrantes.',
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      );
    }

    if (_loadingMembers) {
      return const Center(child: CircularProgressIndicator());
    }

    final pendingMembers = _members.where((m) => m.isPending).toList();
    final activeMembers = _members.where((m) => m.isActive).toList();

    if (pendingMembers.isEmpty && activeMembers.isEmpty) {
      return const Center(child: Text('No hay miembros activos.'));
    }

    return RefreshIndicator(
      onRefresh: _refreshAll,
      child: ListView(
        padding: const EdgeInsets.symmetric(vertical: 8),
        children: [
          if (_community.isAdmin && pendingMembers.isNotEmpty) ...[
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
              child: Text(
                'Solicitudes pendientes (${pendingMembers.length})',
                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
              ),
            ),
            ...pendingMembers.map(
              (member) => MemberListTile(
                member: member,
                isCurrentUser: false,
                onApprove: () => _handleApproveMember(member),
                onReject: () => _handleRejectMember(member),
              ),
            ),
            const Divider(height: 24),
          ],
          ...activeMembers.map(
            (member) => MemberListTile(
              member: member,
              isCurrentUser: member.userId == widget.currentUserId,
            ),
          ),
        ],
      ),
    );
  }
}