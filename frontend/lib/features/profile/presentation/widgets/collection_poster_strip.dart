import 'package:flutter/material.dart';

import '../../../../core/theme/app_colors.dart';
import '../../data/models/list_element_model.dart';

class CollectionPosterStrip extends StatelessWidget {
  final List<ListElementModel> elements;
  final ValueChanged<ListElementModel>? onRemove;
  final bool busy;
  final String removeLabel;
  const CollectionPosterStrip({
    super.key,
    required this.elements,
    this.onRemove,
    this.busy = false,
    this.removeLabel = 'Quitar de la lista',
  });

  @override
  Widget build(BuildContext context) => SizedBox(
    height: onRemove == null ? 282 : 322,
    child: ListView.separated(
      scrollDirection: Axis.horizontal,
      itemCount: elements.length,
      separatorBuilder: (_, index) => const SizedBox(width: 16),
      itemBuilder: (context, index) {
        final element = elements[index];
        final book = element.elementType == 'BOOK';
        final artist = element.elementType == 'ARTIST';
        final placeholder = ColoredBox(
          color: AppColors.lilac,
          child: Center(
            child: Icon(
              book
                  ? Icons.menu_book_outlined
                  : artist
                  ? Icons.person_outline
                  : Icons.music_note,
              size: 40,
              color: AppColors.ink,
            ),
          ),
        );
        final image = element.imageUrl == null
            ? placeholder
            : Image.network(
                element.imageUrl!,
                fit: BoxFit.contain,
                errorBuilder: (_, error, stack) => placeholder,
              );
        return SizedBox(
          width: 132,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(
                height: 198,
                width: 132,
                child: Center(
                  child: SizedBox(
                    width: 132,
                    height: book ? 198 : 132,
                    child: artist
                        ? ClipOval(child: image)
                        : ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: image,
                          ),
                  ),
                ),
              ),
              const SizedBox(height: 10),
              SizedBox(
                height: 40,
                child: Text(
                  element.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: AppColors.ink,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              const SizedBox(height: 4),
              Text(
                element.statusMessage ??
                    element.subtitle ??
                    (book
                        ? 'Libro'
                        : artist
                        ? 'Artista'
                        : 'Canción'),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 12, color: AppColors.ink),
              ),
              if (onRemove != null)
                Align(
                  alignment: Alignment.centerRight,
                  child: IconButton(
                    tooltip: removeLabel,
                    icon: const Icon(Icons.remove_circle_outline),
                    onPressed: busy ? null : () => onRemove!(element),
                  ),
                ),
            ],
          ),
        );
      },
    ),
  );
}
