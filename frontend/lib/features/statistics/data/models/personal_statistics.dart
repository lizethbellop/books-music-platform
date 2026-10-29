import 'book_statistics.dart';
import 'music_statistics.dart';
import 'profile_statistics.dart';

class PersonalStatistics {
  final String userId;
  final MusicStatistics music;
  final BookStatistics books;
  final ProfileStatistics profile;

  const PersonalStatistics({
    required this.userId,
    required this.music,
    required this.books,
    required this.profile,
  });

  factory PersonalStatistics.fromJson(Map<String, dynamic> json) {
    return PersonalStatistics(
      userId: json['userId'] as String,
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