import 'package:flutter/material.dart';
import '../../data/models/community_member_model.dart';

class MemberListTile extends StatelessWidget {
  final CommunityMemberModel member;
  final bool isCurrentUser;
  final VoidCallback? onApprove;
  final VoidCallback? onReject;

  const MemberListTile({
    super.key,
    required this.member,
    this.isCurrentUser = false,
    this.onApprove,
    this.onReject,
  });

  String _formatDate(DateTime date) {
    final local = date.toLocal();
    final day = local.day.toString().padLeft(2, '0');
    final month = local.month.toString().padLeft(2, '0');
    return '$day/$month/${local.year}';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final shortId = member.userId.length >= 8
        ? member.userId.substring(0, 8)
        : member.userId;

    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      leading: CircleAvatar(
        backgroundColor: member.isAdmin
            ? colorScheme.primaryContainer
            : member.isPending
                ? colorScheme.tertiaryContainer
                : colorScheme.surfaceContainerHighest,
        child: Icon(
          member.isAdmin
              ? Icons.shield_outlined
              : member.isPending
                  ? Icons.hourglass_top_rounded
                  : Icons.person_outline_rounded,
          color: member.isAdmin
              ? colorScheme.onPrimaryContainer
              : member.isPending
                  ? colorScheme.onTertiaryContainer
                  : colorScheme.onSurfaceVariant,
        ),
      ),
      title: Text(
        isCurrentUser ? 'Tú ($shortId...)' : 'Usuario $shortId...',
        style: theme.textTheme.bodyLarge?.copyWith(
          fontWeight: isCurrentUser ? FontWeight.bold : FontWeight.w500,
        ),
      ),
      subtitle: Text(
        member.isPending
            ? 'Solicitud pendiente • ${_formatDate(member.joinedAt)}'
            : 'Miembro desde ${_formatDate(member.joinedAt)}',
        style: theme.textTheme.bodySmall?.copyWith(
          color: colorScheme.onSurfaceVariant,
        ),
      ),
      trailing: member.isPending
          ? Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  tooltip: 'Aprobar solicitud',
                  icon: Icon(Icons.check_circle_rounded, color: colorScheme.primary),
                  onPressed: onApprove,
                ),
                IconButton(
                  tooltip: 'Rechazar solicitud',
                  icon: Icon(Icons.cancel_rounded, color: colorScheme.error),
                  onPressed: onReject,
                ),
              ],
            )
          : Chip(
              label: Text(member.isAdmin ? 'Admin' : 'Miembro'),
              visualDensity: VisualDensity.compact,
              backgroundColor: member.isAdmin
                  ? colorScheme.secondaryContainer
                  : colorScheme.surfaceContainerHighest,
              labelStyle: theme.textTheme.labelSmall?.copyWith(
                fontWeight: FontWeight.w600,
              ),
            ),
    );
  }
}