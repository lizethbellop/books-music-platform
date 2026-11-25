class ChatConversation {
  final String id;
  final List<String> participantIds;
  final String type;
  final String? name, ownerId;
  bool get isGroup => type == 'GROUP';
  ChatConversation({
    required this.id,
    required this.participantIds,
    this.type = 'PRIVATE',
    this.name,
    this.ownerId,
  });
  factory ChatConversation.fromJson(Map<String, dynamic> json) =>
      ChatConversation(
        id: json['id'] as String,
        type: json['type'] as String? ?? 'PRIVATE',
        name: json['name'] as String?,
        ownerId: json['ownerId'] as String?,
        participantIds: (json['participantIds'] as List).cast<String>(),
      );
}

class ChatMessage {
  final String id, conversationId, senderId, content;
  final DateTime createdAt;
  ChatMessage({
    required this.id,
    required this.conversationId,
    required this.senderId,
    required this.content,
    required this.createdAt,
  });
  factory ChatMessage.fromJson(Map<String, dynamic> json) => ChatMessage(
    id: json['id'] as String,
    conversationId: json['conversationId'] as String,
    senderId: json['senderId'] as String,
    content: json['content'] as String,
    createdAt: DateTime.parse(json['createdAt'] as String),
  );
}

class ChatHistory {
  final List<ChatMessage> items;
  final bool hasMore;
  // Conservar la cadena original: DateTime de Dart podría perder nanosegundos del cursor.
  final String? nextCreatedAt, nextId;
  ChatHistory({
    required this.items,
    required this.hasMore,
    this.nextCreatedAt,
    this.nextId,
  });
  factory ChatHistory.fromJson(Map<String, dynamic> json) => ChatHistory(
    items: (json['items'] as List)
        .map((m) => ChatMessage.fromJson(m as Map<String, dynamic>))
        .toList(),
    hasMore: json['hasMore'] as bool,
    nextCreatedAt: json['nextCreatedAt'] as String?,
    nextId: json['nextId'] as String?,
  );
}

class ChatConversationPage {
  final List<ChatConversation> items;
  final int? nextPage;
  ChatConversationPage({required this.items, this.nextPage});
  factory ChatConversationPage.fromJson(Map<String, dynamic> json) =>
      ChatConversationPage(
        items: (json['items'] as List)
            .map((c) => ChatConversation.fromJson(c as Map<String, dynamic>))
            .toList(),
        nextPage: json['nextPage'] as int?,
      );
}

class ChatRecipient {
  final String id, username;
  ChatRecipient(this.id, this.username);
}
