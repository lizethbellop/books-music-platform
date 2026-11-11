class ProfileStatistics {
  final int lists;
  final int listElements;
  final int preferenceElements;

  const ProfileStatistics({
    required this.lists,
    required this.listElements,
    required this.preferenceElements,
  });

  factory ProfileStatistics.fromJson(Map<String, dynamic> json) {
    return ProfileStatistics(
      lists: (json['lists'] as num?)?.toInt() ?? 0,
      listElements: (json['listElements'] as num?)?.toInt() ?? 0,
      preferenceElements:
          (json['preferenceElements'] as num?)?.toInt() ?? 0,
    );
  }
}