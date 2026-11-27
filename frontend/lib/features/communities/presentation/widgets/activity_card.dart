import 'package:flutter/material.dart';
import '../../data/models/activity_model.dart';

class ActivityCard extends StatelessWidget {
  final ActivityModel activity;
  final bool canManage;
  final bool isTogglingAttendance;
  final VoidCallback? onToggleAttendance;
  final VoidCallback? onCancelActivity;
  final VoidCallback? onDeleteActivity;

  const ActivityCard({
    super.key,
    required this.activity,
    this.canManage = false,
    this.isTogglingAttendance = false,
    this.onToggleAttendance,
    this.onCancelActivity,
    this.onDeleteActivity,
  });

  String _formatDateTime(DateTime date) {
    final local = date.toLocal();
    final day = local.day.toString().padLeft(2, '0');
    final month = local.month.toString().padLeft(2, '0');
    final hour = local.hour.toString().padLeft(2, '0');
    final minute = local.minute.toString().padLeft(2, '0');
    return '$day/$month/${local.year} • $hour:$minute';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final status = activity.status.toUpperCase();
    final isCancelled = status == 'CANCELADA';
    final isCompleted = status == 'COMPLETADA';
    final isScheduled = status == 'PROGRAMADA';

    final statusColor = switch (status) {
      'CANCELADA' => colorScheme.error,
      'COMPLETADA' => colorScheme.tertiary,
      _ => colorScheme.primary,
    };

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: colorScheme.outlineVariant.withValues(alpha: 0.6)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Estado + Menú de administración
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: statusColor.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    status,
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: statusColor,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                const Spacer(),
                if (canManage)
                  PopupMenuButton<String>(
                    icon: const Icon(Icons.more_vert_rounded, size: 20),
                    onSelected: (value) {
                      if (value == 'cancel' && onCancelActivity != null) {
                        onCancelActivity!();
                      } else if (value == 'delete' && onDeleteActivity != null) {
                        onDeleteActivity!();
                      }
                    },
                    itemBuilder: (_) => [
                      if (isScheduled)
                        const PopupMenuItem(
                          value: 'cancel',
                          child: Text('Cancelar actividad'),
                        ),
                      const PopupMenuItem(
                        value: 'delete',
                        child: Text('Eliminar actividad'),
                      ),
                    ],
                  ),
              ],
            ),
            const SizedBox(height: 10),

            // Título y descripción
            Text(
              activity.title,
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.bold,
                decoration: isCancelled ? TextDecoration.lineThrough : null,
              ),
            ),
            if (activity.description != null &&
                activity.description!.trim().isNotEmpty) ...[
              const SizedBox(height: 6),
              Text(
                activity.description!,
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: colorScheme.onSurfaceVariant,
                ),
              ),
            ],
            const SizedBox(height: 12),

            // Fecha y ubicación
            Row(
              children: [
                Icon(Icons.event_rounded, size: 16, color: colorScheme.primary),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    _formatDateTime(activity.startsAt),
                    style: theme.textTheme.bodySmall,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Row(
              children: [
                Icon(Icons.place_outlined, size: 16, color: colorScheme.primary),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    activity.location,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),

            // Asistentes + Botón de participación
            Row(
              children: [
                Icon(
                  Icons.how_to_reg_outlined,
                  size: 18,
                  color: colorScheme.onSurfaceVariant,
                ),
                const SizedBox(width: 6),
                Text(
                  '${activity.participantCount} ${activity.participantCount == 1 ? 'confirmado' : 'confirmados'}',
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: colorScheme.onSurfaceVariant,
                  ),
                ),
                const Spacer(),
                if (!isCancelled && !isCompleted)
                  activity.isParticipating
                      ? OutlinedButton.icon(
                          onPressed: isTogglingAttendance ? null : onToggleAttendance,
                          icon: isTogglingAttendance
                              ? const SizedBox(
                                  width: 14,
                                  height: 14,
                                  child: CircularProgressIndicator(strokeWidth: 2),
                                )
                              : const Icon(Icons.check_circle_rounded, size: 16),
                          label: const Text('Asistiré'),
                        )
                      : FilledButton.tonal(
                          onPressed: isTogglingAttendance ? null : onToggleAttendance,
                          child: isTogglingAttendance
                              ? const SizedBox(
                                  width: 14,
                                  height: 14,
                                  child: CircularProgressIndicator(strokeWidth: 2),
                                )
                              : const Text('Confirmar asistencia'),
                        ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}