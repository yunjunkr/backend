package com.zoopick.server.service;

import com.zoopick.server.dto.chat.ChatRoomCloseReason;
import com.zoopick.server.dto.chat.ChatRoomRecord;
import com.zoopick.server.dto.chat.CreateChatRoomRequest;
import com.zoopick.server.dto.chat.CreateChatRoomResult;
import com.zoopick.server.entity.*;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.mapper.ChatMessageMapper;
import com.zoopick.server.mapper.ChatRoomMapper;
import com.zoopick.server.repository.ChatMessageRepository;
import com.zoopick.server.repository.ChatRoomRepository;
import com.zoopick.server.repository.ItemRepository;
import com.zoopick.server.repository.UserRepository;
import com.zoopick.server.service.notification.NotificationService;
import com.zoopick.server.service.notification.SendNotificationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private ChatRoomMapper chatRoomMapper;

    private User requester;
    private User counterpart;
    private Item foundItem;
    private ChatRoom openChatRoom;

    @BeforeEach
    void setUp() {
        requester = User.builder().id(1L).nickname("requester").build();
        counterpart = User.builder().id(2L).nickname("counterpart").build();

        foundItem = Item.builder().id(10L).type(ItemType.FOUND).build();

        openChatRoom = ChatRoom.builder()
                .id(100L)
                .item(foundItem)
                .owner(requester)     // 1L
                .finder(counterpart)  // 2L
                .status(ChatRoomStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("채팅방 생성 - 본인에게 요청 시 BadRequestException 발생")
    void createChatRoom_fail_whenRequesterAndCounterpartAreSame() {
        // given
        CreateChatRoomRequest request = new CreateChatRoomRequest(10L, 1L);

        // when & then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> chatRoomService.createChatRoom(1L, request));

        assertEquals("잘못된 요청입니다.", exception.getClientMessage());
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("채팅방 생성 - 이미 존재하는 채팅방인 경우 기존 채팅방 반환")
    void createChatRoom_returnsExistingChatRoom() {
        // given
        CreateChatRoomRequest request = new CreateChatRoomRequest(10L, 2L);
        ChatRoomRecord mockRecord = mock(ChatRoomRecord.class);

        when(itemRepository.findByIdOrThrow(10L)).thenReturn(foundItem);
        when(userRepository.findByIdOrThrow(1L)).thenReturn(requester);
        when(userRepository.findByIdOrThrow(2L)).thenReturn(counterpart);

        when(chatRoomRepository.findOpenByParticipantAndItem(1L, 10L))
                .thenReturn(Optional.of(openChatRoom));
        when(chatRoomMapper.toChatRoomRecord(openChatRoom)).thenReturn(mockRecord);

        // when
        CreateChatRoomResult result = chatRoomService.createChatRoom(1L, request);

        // then
        assertFalse(result.isCreated()); // 생성되지 않음
        assertNotNull(result.getRoomData());
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("채팅방 생성 - FOUND 아이템에 대해 정상적으로 채팅방 생성")
    void createChatRoom_success_forFoundItem() {
        // given
        CreateChatRoomRequest request = new CreateChatRoomRequest(10L, 2L);
        ChatRoomRecord mockRecord = mock(ChatRoomRecord.class);

        when(itemRepository.findByIdOrThrow(10L)).thenReturn(foundItem);
        when(userRepository.findByIdOrThrow(1L)).thenReturn(requester);
        when(userRepository.findByIdOrThrow(2L)).thenReturn(counterpart);

        when(chatRoomRepository.findOpenByParticipantAndItem(1L, 10L)).thenReturn(Optional.empty());

        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(openChatRoom);
        when(chatRoomMapper.toChatRoomRecord(openChatRoom)).thenReturn(mockRecord);

        // when
        CreateChatRoomResult result = chatRoomService.createChatRoom(1L, request);

        // then
        assertTrue(result.isCreated());
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("메시지 전송 - 참여자가 아닌 경우 예외 발생")
    void sendMessageWithNotification_fail_whenNotParticipant() {
        // given
        User stranger = User.builder().id(3L).build();
        when(userRepository.findByIdOrThrow(3L)).thenReturn(stranger);
        when(chatRoomRepository.findByIdOrThrow(100L)).thenReturn(openChatRoom);

        // when & then
        assertThrows(BadRequestException.class,
                () -> chatRoomService.sendMessageWithNotification(3L, 100L, "Hello!"));
    }

    @Test
    @DisplayName("메시지 전송 - 닫힌 방에 전송 시 예외 발생")
    void sendMessageWithNotification_fail_whenChatRoomIsClosed() {
        // given
        openChatRoom.setStatus(ChatRoomStatus.RESOLVED_RETURNED);
        when(userRepository.findByIdOrThrow(1L)).thenReturn(requester);
        when(chatRoomRepository.findByIdOrThrow(100L)).thenReturn(openChatRoom);

        // when & then
        assertThrows(BadRequestException.class,
                () -> chatRoomService.sendMessageWithNotification(1L, 100L, "Hello!"));
    }

    @Test
    @DisplayName("메시지 전송 - 성공 시 메시지 저장 및 알림 전송 (sendMessageWithNotification)")
    void sendMessageWithNotification_success() {
        // given
        String message = "안녕하세요!";
        when(userRepository.findByIdOrThrow(1L)).thenReturn(requester);
        when(chatRoomRepository.findByIdOrThrow(100L)).thenReturn(openChatRoom);

        // when: 알림 발송이 포함된 public 메서드를 호출합니다.
        chatRoomService.sendMessageWithNotification(1L, 100L, message);

        // then
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(notificationService, times(1)).send(eq(counterpart), any(SendNotificationCommand.class));
    }

    @Test
    @DisplayName("채팅방 닫기 - 성공적으로 닫히고 알림 전송")
    void closeChatRoom_success() {
        // given
        when(userRepository.findByIdOrThrow(1L)).thenReturn(requester);
        when(chatRoomRepository.findByIdOrThrow(100L)).thenReturn(openChatRoom);

        // when
        chatRoomService.closeChatRoom(1L, 100L, ChatRoomCloseReason.RETURNED);

        // then
        assertEquals(ChatRoomStatus.RESOLVED_RETURNED, openChatRoom.getStatus());
        assertNotNull(openChatRoom.getResolvedAt());
        verify(chatRoomRepository, times(1)).save(openChatRoom);
        verify(notificationService, times(1)).send(eq(counterpart), any(SendNotificationCommand.class));
    }

    @Test
    @DisplayName("채팅방 다시 열기 - 성공적으로 열리고 알림 전송")
    void reopenChatRoom_success() {
        // given
        openChatRoom.setStatus(ChatRoomStatus.RESOLVED_RETURNED);
        openChatRoom.setResolvedAt(java.time.LocalDateTime.now());

        when(userRepository.findByIdOrThrow(1L)).thenReturn(requester);
        when(chatRoomRepository.findByIdOrThrow(100L)).thenReturn(openChatRoom);

        // when
        chatRoomService.reopenChatRoom(1L, 100L);

        // then
        assertEquals(ChatRoomStatus.OPEN, openChatRoom.getStatus());
        assertNull(openChatRoom.getResolvedAt());
        verify(chatRoomRepository, times(1)).save(openChatRoom);
        verify(notificationService, times(1)).send(eq(counterpart), any(SendNotificationCommand.class));
    }
}