package com.zoopick.server.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoopick.server.exception.DataNotFoundException;
import com.zoopick.server.exception.InternalServerException;
import com.zoopick.server.service.ChatRoomService;
import com.zoopick.server.websocket.message.ChatErrorPayload;
import com.zoopick.server.websocket.message.ChatRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 사용자 인증에 실패한 경우에만 연결을 종료합니다. <br/>
 * 요청을 처리하는 과정에서 발생한 오류는 요청자에게 알립니다.
 *
 * @see AuthHandshakeInterceptor
 * @see ChatEventSender
 * @see WebSocketSessionManager
 * @see ChatWebSocketBroadcaster
 */
@Component
@RequiredArgsConstructor
@NullMarked
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final WebSocketSessionManager webSocketSessionManager;
    private final ObjectMapper objectMapper;
    private final ChatWebSocketBroadcaster chatWebSocketBroadcaster;
    private final ChatRoomService chatRoomService;
    private final ChatEventSender chatEventSender;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (session.getAttributes().get(AuthHandshakeInterceptor.USER_ID_ATTRIBUTE) == null)
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized websocket session"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            ChatRequestMessage request = objectMapper.readValue(message.getPayload(), ChatRequestMessage.class);
            if (!validateChatRoomAccess(session, request))
                return;

            if (!webSocketSessionManager.getSessionsByRoom(request.roomId()).contains(session))
                webSocketSessionManager.join(request.roomId(), session);
            if (request.type() == ChatRequestMessage.Type.MESSAGE)
                chatWebSocketBroadcaster.broadcastChat(request.roomId(), session, request.message());
            if (request.type() == ChatRequestMessage.Type.READ)
                chatWebSocketBroadcaster.broadcastRead(request.roomId(), session);
        } catch (InternalServerException exception) {
            chatEventSender.sendErrorSafely(session, ChatErrorPayload.Reason.INTERNAL_SERVER_ERROR, exception.getClientMessage());
            log.error("Failed to handle websocket message. sessionId={}", session.getId(), exception);
        } catch (DataNotFoundException exception) {
            chatEventSender.sendErrorSafely(session, ChatErrorPayload.Reason.NOT_FOUND, exception.getClientMessage());
        } catch (JsonProcessingException exception) {
            chatEventSender.sendErrorSafely(session, ChatErrorPayload.Reason.BAD_REQUEST, "잘못된 형식의 요청입니다.");
            log.warn("Failed to parse request. sessionId={}", session.getId(), exception);
        } catch (Exception exception) {
            chatEventSender.sendErrorSafely(session, ChatErrorPayload.Reason.INTERNAL_SERVER_ERROR, "서버에 장애가 발생했습니다.");
            log.error("Failed to handle websocket message. sessionId={}", session.getId(), exception);
        }
    }

    private boolean validateChatRoomAccess(WebSocketSession session, ChatRequestMessage request) {
        long userId = WebSocketSessionUtils.getUserId(session);
        if (!chatRoomService.getParticipants(request.roomId()).contains(userId)) {
            chatEventSender.sendErrorSafely(session, ChatErrorPayload.Reason.NOT_PERMITTED, "채팅방에 참여할 수 없습니다.");
            log.warn("sessionId={} is rejected to join chat room {}", session.getId(), request.roomId());
            return false;
        }

        return true;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        webSocketSessionManager.leave(session);
    }
}
