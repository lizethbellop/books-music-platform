import 'dart:async';
import 'dart:convert';

import 'package:web_socket_channel/web_socket_channel.dart';

import '../../auth/data/services/auth_session_manager.dart';
import 'chat_api.dart';

class ChatConnection {
  final ChatApi api;
  final _events = StreamController<Map<String, dynamic>>.broadcast();
  Stream<Map<String, dynamic>> get events => _events.stream;
  WebSocketChannel? _channel;
  Timer? _retry, _heartbeat;
  bool _closed = false, _connecting = false;
  bool _connected = false;
  bool get isConnected => _connected;
  DateTime _lastPong = DateTime.now();
  int _attempt = 0;
  String? _userId;

  ChatConnection(this.api) {
    _userId = AuthSessionManager.instance.session?.userId;
    AuthSessionManager.instance.addListener(_sessionChanged);
  }
  void _sessionChanged() {
    if (AuthSessionManager.instance.session?.userId != _userId) close();
  }

  Future<void> connect() async {
    if (_closed || _connecting) return;
    _connecting = true;
    try {
      final ticket = await api.ticket();
      if (_closed) return;
      final httpUri = Uri.parse('${api.baseUrl}/ws');
      final channel = WebSocketChannel.connect(
        httpUri.replace(scheme: httpUri.scheme == 'https' ? 'wss' : 'ws'),
      );
      _channel = channel;
      channel.stream.listen(
        (frame) {
          if (_closed || !identical(channel, _channel)) return;
          try {
            final event = jsonDecode(frame as String) as Map<String, dynamic>;
            if (event['type'] == 'ready' || event['type'] == 'pong') {
              _lastPong = DateTime.now();
            }
            if (event['type'] == 'ready') {
              _attempt = 0;
              _connected = true;
            }
            if (event['type'] != 'pong') _events.add(event);
          } on FormatException {
            /* Ignorar frames malformados; el historial sigue en REST. */
          }
        },
        onDone: () => _disconnected(channel),
        onError: (Object _) => _disconnected(channel),
      );
      await channel.ready.timeout(const Duration(seconds: 8));
      if (_closed || !identical(channel, _channel)) {
        unawaited(channel.sink.close());
        return;
      }
      channel.sink.add(jsonEncode({'ticket': ticket}));
      _heartbeat?.cancel();
      _heartbeat = Timer.periodic(const Duration(seconds: 20), (_) {
        if (!_closed && identical(channel, _channel)) {
          if (DateTime.now().difference(_lastPong).inSeconds > 45) {
            _disconnected(channel);
            unawaited(channel.sink.close());
            return;
          }
          try {
            channel.sink.add(jsonEncode({'type': 'ping'}));
          } catch (_) {
            _disconnected(channel);
          }
        }
      });
    } catch (_) {
      final channel = _channel;
      if (channel != null) unawaited(channel.sink.close());
      _scheduleRetry();
    } finally {
      _connecting = false;
    }
  }

  void _disconnected(WebSocketChannel channel) {
    if (!identical(channel, _channel)) return;
    _connected = false;
    _channel = null;
    _heartbeat?.cancel();
    _scheduleRetry();
  }

  void _scheduleRetry() {
    if (_closed || (_retry?.isActive ?? false)) return;
    _events.add({'type': 'disconnected'});
    _attempt = (_attempt + 1).clamp(1, 5);
    _retry = Timer(Duration(seconds: _attempt * 2), () {
      unawaited(connect());
    });
  }

  void close() {
    if (_closed) return;
    _closed = true;
    AuthSessionManager.instance.removeListener(_sessionChanged);
    _retry?.cancel();
    _heartbeat?.cancel();
    unawaited(_channel?.sink.close());
    unawaited(_events.close());
  }
}
