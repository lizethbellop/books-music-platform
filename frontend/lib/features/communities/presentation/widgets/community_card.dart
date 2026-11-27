import 'package:flutter/material.dart';
import '../../data/models/community_model.dart';

class CommunityCard extends StatelessWidget {
  final CommunityModel community;
  final VoidCallback onTap;
  final VoidCallback? onJoinTap;
  final bool isJoining;

  const CommunityCard({
    super.key,
    required this.community,
    required this.onTap,
    this.onJoinTap,
    this.isJoining = false,
  });

  IconData _typeIcon(String type) => switch (type.toUpperCase()) {
    'LIBROS' => Icons.menu_book_rounded,
    'MUSICA' => Icons.music_note_rounded,
    _ => Icons.auto_awesome_rounded, // MIXTO
  };

  String _typeLabel(String type) => switch (type.toUpperCase()) {
    'LIBROS' => 'Libros',
    'MUSICA' => 'Música',
    _ => 'Mixto',
  };

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: colorScheme.outlineVariant.withValues(alpha: 0.6)),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Fila superior: Categoría + Visibilidad
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(
                      color: colorScheme.primaryContainer,
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          _typeIcon(community.type),
                          size: 15,
                          color: colorScheme.onPrimaryContainer,
                        ),
                        const SizedBox(width: 6),
                        Text(
                          _typeLabel(community.type),
                          style: theme.textTheme.labelMedium?.copyWith(
                            color: colorScheme.onPrimaryContainer,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Spacer(),
                  Icon(
                    community.isPrivate ? Icons.lock_outline_rounded : Icons.public_rounded,
                    size: 16,
                    color: colorScheme.onSurfaceVariant,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    community.isPrivate ? 'Privada' : 'Pública',
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),

              // Nombre de la comunidad
              Text(
                community.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 6),

              // Descripción
              Text(
                (community.description != null && community.description!.trim().isNotEmpty)
                    ? community.description!
                    : 'Sin descripción disponible.',
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 14),

              // Pie de tarjeta: Contador de miembros + Acción / Rol
              Row(
                children: [
                  Icon(
                    Icons.people_outline_rounded,
                    size: 18,
                    color: colorScheme.onSurfaceVariant,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    '${community.memberCount} ${community.memberCount == 1 ? 'miembro' : 'miembros'}',
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: colorScheme.onSurfaceVariant,
                    ),
                  ),
                  const Spacer(),
                  if (community.isMember)
                    Chip(
                      label: Text(community.isAdmin ? 'Admin' : 'Miembro'),
                      visualDensity: VisualDensity.compact,
                      backgroundColor: community.isAdmin
                          ? colorScheme.secondaryContainer
                          : colorScheme.surfaceContainerHighest,
                      labelStyle: theme.textTheme.labelSmall?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    )
                  else if (community.isPending)
                    Chip(
                      avatar: const Icon(Icons.hourglass_top_rounded, size: 14),
                      label: const Text('Pendiente'),
                      visualDensity: VisualDensity.compact,
                      backgroundColor: colorScheme.tertiaryContainer,
                      labelStyle: theme.textTheme.labelSmall?.copyWith(
                        color: colorScheme.onTertiaryContainer,
                        fontWeight: FontWeight.w600,
                      ),
                    )
                  else
                    FilledButton.tonal(
                      onPressed: isJoining ? null : onJoinTap,
                      style: FilledButton.styleFrom(
                        visualDensity: VisualDensity.compact,
                        padding: const EdgeInsets.symmetric(horizontal: 14),
                      ),
                      child: isJoining
                          ? const SizedBox(
                              width: 14,
                              height: 14,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : Text(community.isPrivate ? 'Solicitar' : 'Unirse'),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}