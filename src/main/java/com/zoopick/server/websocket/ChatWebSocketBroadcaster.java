package com.zoopick.server.websocket;

import com.zoopick.server.service.ChatRoomService;
import com.zoopick.server.websocket.message.ChatBroadcastPayload;
import com.zoopick.server.websocket.message.ChatEventPayload;
import com.zoopick.server.websocket.message.ChatInformationPayload;
import com.zoopick.server.websocket.message.ChatReadPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@NullMarked
public class ChatWebSocketBroadcaster {
    private final WebSocketSessionManager webSocketSessionManager;
    private final ChatRoomService chatRoomService;
    private final ChatEventSender chatEventSender;

    /**
     * 채팅방의 다른 사용자들에게 메시지를 전송합니다.
     * 웹소켓를 통한 메시지 전송에 실패한 경우 FCM을 통해 알림을 보냅니다.
     *
     * @param roomId        채팅방 Id
     * @param senderSession 보내는 사람의 세션
     * @param message       메시지
     */
    public void broadcastChat(long roomId, WebSocketSession senderSession, String message) {
        long senderId = WebSocketSessionUtils.getUserId(senderSession);
        String senderNickname = WebSocketSessionUtils.getNickname(senderSession);
        ChatBroadcastPayload payload = new ChatBroadcastPayload(senderNickname, message);

        Set<WebSocketSession> sessions = webSocketSessionManager.getSessionsByRoom(roomId);
        List<Long> receiverInWebSocketIds = sendMessageToOthers(senderSession, sessions, payload);

        chatRoomService.getParticipants(roomId).stream()
                .filter(participantId -> participantId != senderId)
                .filter(participantId -> !receiverInWebSocketIds.contains(participantId))
                .forEach(participantId -> chatRoomService.sendMessageWithNotification(senderId, roomId, message));
        receiverInWebSocketIds.forEach(receiverId -> chatRoomService.sendMessageWithoutNotification(senderId, roomId, message));
        chatEventSender.sendMessageSafely(senderSession, new ChatInformationPayload(receiverInWebSocketIds.size() + "개의 세션으로 메시지가 전송되었습니다."));
    }

    public void broadcastRead(long roomId, WebSocketSession senderSession) {
        long senderId = WebSocketSessionUtils.getUserId(senderSession);
        String senderNickname = WebSocketSessionUtils.getNickname(senderSession);
        ChatReadPayload payload = new ChatReadPayload(senderNickname);
        chatRoomService.readChatMessages(senderId, roomId);

        Set<WebSocketSession> sessions = webSocketSessionManager.getSessionsByRoom(roomId);
        List<Long> receiverInWebSocketIds = sendMessageToOthers(senderSession, sessions, payload);
        chatEventSender.sendMessageSafely(senderSession, new ChatInformationPayload(receiverInWebSocketIds.size() + "개의 세션으로 읽음 상태가 전송되었습니다."));
    }

    private List<Long> sendMessageToOthers(WebSocketSession senderSession, Set<WebSocketSession> sessions, ChatEventPayload payload) {
        List<Long> receiverInWebSocketIds = new ArrayList<>();

        for (WebSocketSession session : sessions) {
            if (!session.isOpen() || session.getId().equals(senderSession.getId()))
                continue;

            long id = WebSocketSessionUtils.getUserId(session);
            if (chatEventSender.sendMessageSafely(session, payload))
                receiverInWebSocketIds.add(id);
        }

        return receiverInWebSocketIds;
    }
}
