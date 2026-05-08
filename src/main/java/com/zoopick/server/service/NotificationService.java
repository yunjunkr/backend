package com.zoopick.server.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.zoopick.server.dto.notification.ChangeReadStatusResult;
import com.zoopick.server.dto.notification.NotificationRecord;
import com.zoopick.server.entity.User;
import com.zoopick.server.entity.ZoopickNotification;
import com.zoopick.server.exception.AccessTokenException;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.exception.DataNotFoundException;
import com.zoopick.server.mapper.NotificationMapper;
import com.zoopick.server.repository.NotificationRepository;
import com.zoopick.server.repository.UserRepository;
import com.zoopick.server.service.command.SendNotificationCommand;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@NullMarked
public class NotificationService {
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public void register(long userId, String fcmToken) throws AccessTokenException, DataNotFoundException {
        User user = userRepository.findByIdOrThrow(userId);
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    public String send(long userId, SendNotificationCommand command) throws FirebaseMessagingException {
        User user = userRepository.findByIdOrThrow(userId);
        return this.send(user, command);
    }

    public String send(User user, SendNotificationCommand command) throws FirebaseMessagingException {
        String fcmToken = user.getFcmToken();
        if (fcmToken == null)
            throw DataNotFoundException.from("FCM 토큰", user.getSchoolEmail());

        Notification notification = Notification.builder()
                .setTitle(command.title())
                .setBody(command.body())
                .build();
        Message message = Message.builder()
                .setNotification(notification)
                .putAllData(command.payload().toMap())
                .setToken(fcmToken)
                .build();

        ZoopickNotification zoopickNotification = notificationMapper.toZoopickNotification(user, command);
        notificationRepository.save(zoopickNotification);
        return FirebaseMessaging.getInstance().send(message);
    }

    public String broadcast(SendNotificationCommand command) throws FirebaseMessagingException {
        Notification notification = Notification.builder()
                .setTitle(command.title())
                .setBody(command.body())
                .build();

        List<User> users = userRepository.findAll();
        List<ZoopickNotification> zoopickNotifications = users.stream()
                .map(user -> notificationMapper.toZoopickNotification(user, command))
                .toList();
        notificationRepository.saveAll(zoopickNotifications);

        List<Message> messages = users.stream()
                .map(User::getFcmToken)
                .filter(Objects::nonNull)
                .map(fcmToken -> Message.builder()
                        .setNotification(notification)
                        .putAllData(command.payload().toMap())
                        .setToken(fcmToken)
                        .build())
                .toList();

        return FirebaseMessaging.getInstance().sendEach(messages).toString();
    }

    public List<NotificationRecord> getNotifications(long userId) {
        return findNotificationsWith(userId, notificationRepository::findByUserIdOrderByCreatedAtDesc);
    }

    public List<NotificationRecord> getUnreadNotifications(long userId) {
        return findNotificationsWith(userId, notificationRepository::findByUserIdAndReadAtIsNullOrderByCreatedAtDesc);
    }

    private List<NotificationRecord> findNotificationsWith(long userId, Function<Long, List<ZoopickNotification>> repositoryAccessor) {
        User user = userRepository.findByIdOrThrow(userId);

        List<ZoopickNotification> notifications = repositoryAccessor.apply(user.getId());
        return notifications.stream()
                .map(notificationMapper::toNotificationResponse)
                .toList();
    }

    public ChangeReadStatusResult markAsRead(long userId, List<Long> notificationIds) {
        return changeReadStatusRequest(userId, notificationIds, notification -> notification.setReadAt(LocalDateTime.now()));
    }

    public ChangeReadStatusResult markAsUnread(long userId, List<Long> notificationIds) {
        return changeReadStatusRequest(userId, notificationIds, notification -> notification.setReadAt(null));
    }

    private ChangeReadStatusResult changeReadStatusRequest(long userId, List<Long> notificationIds, Consumer<ZoopickNotification> readStatusChangeAction) {
        List<ZoopickNotification> notifications = notificationRepository.findAllById(notificationIds);
        if (notifications.isEmpty())
            throw DataNotFoundException.from("알림", notificationIds);

        List<Long> succeedIds = new ArrayList<>();
        for (ZoopickNotification notification : notifications) {
            if (notification.getUser().getId() == userId) {
                readStatusChangeAction.accept(notification);
                succeedIds.add(notification.getId());
            }
        }
        if (succeedIds.isEmpty())
            throw new BadRequestException("현재 사용자의 알림이 아닙니다.");

        notificationRepository.saveAll(notifications);
        return new ChangeReadStatusResult(succeedIds);
    }
}
