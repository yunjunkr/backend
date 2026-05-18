package com.zoopick.server.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoopick.server.dto.notification.ChangeReadStatusResult;
import com.zoopick.server.entity.NotificationType;
import com.zoopick.server.entity.User;
import com.zoopick.server.entity.ZoopickNotification;
import com.zoopick.server.mapper.notification.NotificationMapper;
import com.zoopick.server.mapper.notification.NotificationPayloadMapper;
import com.zoopick.server.repository.NotificationRepository;
import com.zoopick.server.repository.UserRepository;
import com.zoopick.server.service.notification.payload.NotificationPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationPayloadMapper notificationPayloadMapper;

    @Test
    @DisplayName("FCM 토큰 등록 - 성공")
    void register_Success() {
        // given
        long userId = 1L;
        String fcmToken = "test_fcm_token";
        User user = User.builder().id(userId).build();

        given(userRepository.findByIdOrThrow(userId)).willReturn(user);

        // when
        notificationService.register(userId, fcmToken);

        // then
        assertThat(user.getFcmToken()).isEqualTo(fcmToken);
    }

    @Test
    @DisplayName("알림 전송 - 성공 (이벤트 발행 검증)")
    void send_Success() {
        // given
        User user = User.builder().id(1L).fcmToken("valid_token").build();

        NotificationPayload payloadMock = mock(NotificationPayload.class);
        lenient().when(payloadMock.toMap()).thenReturn(Map.of());
        lenient().when(payloadMock.type()).thenReturn(NotificationType.values()[0]);

        SendNotificationCommand command = new SendNotificationCommand("제목", "내용", payloadMock);
        ZoopickNotification notification = ZoopickNotification.builder().id(100L).user(user).build();

        given(notificationMapper.toZoopickNotification(user, command)).willReturn(notification);
        given(notificationRepository.save(any(ZoopickNotification.class))).willReturn(notification);

        // when
        String result = notificationService.send(user, command);

        // then
        assertThat(result).isEqualTo("100");
        verify(notificationRepository, times(1)).save(any(ZoopickNotification.class));
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("알림 전송 - FCM 토큰이 없는 경우 예외 없이 DB에 저장되고 이벤트가 발행된다")
    void send_Success_WithoutFcmToken() {
        // given
        User user = User.builder().id(1L).schoolEmail("test@mju.ac.kr").fcmToken(null).build();

        NotificationPayload payloadMock = mock(NotificationPayload.class);
        lenient().when(payloadMock.type()).thenReturn(NotificationType.values()[0]);
        lenient().when(payloadMock.toMap()).thenReturn(Map.of());

        SendNotificationCommand command = new SendNotificationCommand("제목", "내용", payloadMock);
        ZoopickNotification mockNotification = ZoopickNotification.builder().id(100L).user(user).build();

        given(notificationMapper.toZoopickNotification(user, command)).willReturn(mockNotification);
        given(notificationRepository.save(any(ZoopickNotification.class))).willReturn(mockNotification);

        // when
        String result = notificationService.send(user, command);

        // then
        assertThat(result).isEqualTo("100");
        verify(notificationRepository, times(1)).save(any(ZoopickNotification.class));
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("알림 읽음 처리 - 성공")
    void markAsRead_Success() {
        // given
        long userId = 1L;
        User user = User.builder().id(userId).build();
        ZoopickNotification notification1 = ZoopickNotification.builder().id(10L).user(user).build();
        ZoopickNotification notification2 = ZoopickNotification.builder().id(11L).user(user).build();

        List<Long> notificationIds = List.of(10L, 11L);
        given(notificationRepository.findAllById(notificationIds)).willReturn(List.of(notification1, notification2));

        // when
        ChangeReadStatusResult result = notificationService.markAsRead(userId, notificationIds);

        // then
        assertThat(result.getSucceedIds()).containsExactly(10L, 11L);
        assertThat(notification1.getReadAt()).isNotNull();
        assertThat(notification2.getReadAt()).isNotNull();
    }
}