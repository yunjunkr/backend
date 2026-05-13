package com.zoopick.server.service.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoopick.server.dto.notification.ChangeReadStatusResult;
import com.zoopick.server.dto.notification.NotificationRecord;
import com.zoopick.server.entity.NotificationType;
import com.zoopick.server.entity.User;
import com.zoopick.server.entity.ZoopickNotification;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.exception.DataNotFoundException;
import com.zoopick.server.mapper.notification.NotificationMapper;
import com.zoopick.server.mapper.notification.NotificationPayloadMapper;
import com.zoopick.server.repository.NotificationRepository;
import com.zoopick.server.repository.UserRepository;
import com.zoopick.server.service.notification.event.FcmMessageRequest;
import com.zoopick.server.service.notification.event.NotificationDispatchRequestedEvent;
import com.zoopick.server.service.notification.payload.ChatMessagePayload;
import com.zoopick.server.service.notification.payload.NotificationPayload;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@NullMarked
public class NotificationService {
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationPayloadMapper notificationPayloadMapper;

    public void register(long userId, String fcmToken) {
        User user = userRepository.findByIdOrThrow(userId);
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    @Transactional
    public String send(long userId, SendNotificationCommand command) {
        User user = userRepository.findByIdOrThrow(userId);
        return this.send(user, command);
    }

    @Transactional
    public String send(User user, SendNotificationCommand command) {
        String fcmToken = user.getFcmToken();

        ZoopickNotification zoopickNotification = notificationMapper.toZoopickNotification(user, command);
        ZoopickNotification savedNotification = notificationRepository.save(zoopickNotification);
        eventPublisher.publishEvent(new NotificationDispatchRequestedEvent(List.of(
                buildFcmMessageRequest(savedNotification, command, fcmToken)
        )));
        return String.valueOf(savedNotification.getId());
    }

    @Transactional
    public String send(List<User> users, SendNotificationCommand command) {
        List<ZoopickNotification> zoopickNotifications = users.stream()
                .map(user -> notificationMapper.toZoopickNotification(user, command))
                .toList();
        List<ZoopickNotification> savedNotifications = notificationRepository.saveAll(zoopickNotifications);

        List<FcmMessageRequest> messages = savedNotifications.stream()
                .filter(zoopickNotification -> zoopickNotification.getUser().getFcmToken() != null)
                .map(zoopickNotification -> buildFcmMessageRequest(
                        zoopickNotification,
                        command,
                        zoopickNotification.getUser().getFcmToken()
                ))
                .toList();
        eventPublisher.publishEvent(new NotificationDispatchRequestedEvent(messages));
        return String.valueOf(messages.size());
    }

    @Transactional
    public String broadcast(SendNotificationCommand command) {
        List<User> users = userRepository.findAll();
        return send(users, command);
    }

    public void storeNotification(long userId, NotificationPayload payload) {
        User user = userRepository.findByIdOrThrow(userId);
        ZoopickNotification zoopickNotification = ZoopickNotification.builder()
                .user(user)
                .type(payload.type())
                .payload(objectMapper.convertValue(payload, JsonNode.class))
                .build();
        notificationRepository.save(zoopickNotification);
    }

    public void storeNotifications(List<Long> userIds, NotificationPayload payload) {
        JsonNode convertedPayload = objectMapper.convertValue(payload, JsonNode.class);
        List<ZoopickNotification> zoopickNotifications = userRepository.findAllById(userIds).stream()
                .map(user -> ZoopickNotification.builder()
                        .user(user)
                        .type(payload.type())
                        .payload(convertedPayload)
                        .build())
                .toList();
        notificationRepository.saveAll(zoopickNotifications);
    }

    public List<NotificationRecord> getNotifications(long userId) {
        return findNotificationsWith(userId, notificationRepository::findByUserIdOrderByCreatedAtDesc);
    }

    public List<NotificationRecord> getUnreadNotifications(long userId) {
        return findNotificationsWith(userId, notificationRepository::findByUserIdAndReadAtIsNullOrderByCreatedAtDesc);
    }

    @Transactional
    public void markAllChatsAsRead(long userId, long roomId) {
        List<ZoopickNotification> notifications = notificationRepository.findAllByUserIdAndType(userId, NotificationType.CHAT_MESSAGE);
        for (ZoopickNotification notification : notifications) {
            if (notificationFromChatRoom(notification, roomId)) {
                notification.setReadAt(LocalDateTime.now());
            }
        }
        notificationRepository.saveAll(notifications);
    }

    private boolean notificationFromChatRoom(ZoopickNotification notification, long roomId) {
        ChatMessagePayload payload = notificationPayloadMapper.toNotificationPayload(notification.getPayload(), ChatMessagePayload.class);
        return payload.roomId() == roomId;
    }

    private List<NotificationRecord> findNotificationsWith(long userId, Function<Long, List<ZoopickNotification>> repositoryAccessor) {
        User user = userRepository.findByIdOrThrow(userId);

        List<ZoopickNotification> notifications = repositoryAccessor.apply(user.getId());
        return notifications.stream()
                .map(notificationMapper::toNotificationResponse)
                .toList();
    }

    @Transactional
    public ChangeReadStatusResult markAsRead(long userId, List<Long> notificationIds) {
        return changeReadStatusRequest(userId, notificationIds, notification -> notification.setReadAt(LocalDateTime.now()));
    }

    @Transactional
    public ChangeReadStatusResult markAsUnread(long userId, List<Long> notificationIds) {
        return changeReadStatusRequest(userId, notificationIds, notification -> notification.setReadAt(null));
    }

    private ChangeReadStatusResult changeReadStatusRequest(long userId, List<Long> notificationIds, Consumer<ZoopickNotification> readStatusChangeAction) {
        List<ZoopickNotification> notifications = notificationRepository.findAllById(notificationIds);
        if (notifications.isEmpty())
            throw DataNotFoundException.from("알림", notificationIds);

        List<Long> succeedIds = new ArrayList<>();
        for (ZoopickNotification notification : notifications) {
            if (notification.getUser().getId().equals(userId)) {
                readStatusChangeAction.accept(notification);
                succeedIds.add(notification.getId());
            }
        }
        if (succeedIds.isEmpty())
            throw new BadRequestException("현재 사용자의 알림이 아닙니다.");

        notificationRepository.saveAll(notifications);
        return new ChangeReadStatusResult(succeedIds);
    }

    private FcmMessageRequest buildFcmMessageRequest(
            ZoopickNotification notification,
            SendNotificationCommand command,
            @Nullable String fcmToken
    ) {
        Map<String, String> data = new HashMap<>(command.payload().toMap());
        data.put("type", command.payload().type().name());
        data.put("id", String.valueOf(notification.getId()));
        return new FcmMessageRequest(Optional.ofNullable(fcmToken), command.title(), command.body(), data);
    }
}
