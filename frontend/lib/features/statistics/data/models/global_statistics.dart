import 'book_statistics.dart';
import 'music_statistics.dart';
import 'profile_statistics.dart';

class GlobalStatistics {
  final MusicStatistics music;
  final BookStatistics books;
  final ProfileStatistics profile;

  const GlobalStatistics({
    required this.music,
    required this.books,
    required this.profile,
  });

  factory GlobalStatistics.fromJson(Map<String, dynamic> json) {
    return GlobalStatistics(
      music: MusicStatistics.fromJson(
        json['music'] as Map<String, dynamic>,
      ),
      books: BookStatistics.fromJson(
        json['books'] as Map<String, dynamic>,
      ),
      profile: ProfileStatistics.fromJson(
        json['profile'] as Map<String, dynamic>,
      ),
    );
  }
}