package com.zoopick.server.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.zoopick.server.dto.chat.*;
import com.zoopick.server.entity.*;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.mapper.ChatMessageMapper;
import com.zoopick.server.mapper.ChatRoomMapper;
import com.zoopick.server.repository.ChatMessageRepository;
import com.zoopick.server.repository.ChatRoomRepository;
import com.zoopick.server.repository.ItemRepository;
import com.zoopick.server.repository.UserRepository;
import com.zoopick.server.service.command.ChatMessagePayload;
import com.zoopick.server.service.command.SendNotificationCommand;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@NullMarked
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NotificationService notificationService;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatRoomMapper chatRoomMapper;

    public CreateChatRoomResult createChatRoom(long requesterId, CreateChatRoomRequest createChatRoomRequest) {
        long itemId = createChatRoomRequest.getItemId();
        Item item = itemRepository.findByIdOrThrow(itemId);
        User requester = userRepository.findByIdOrThrow(requesterId);
        User counterpart = userRepository.findByIdOrThrow(createChatRoomRequest.getCounterpartId());
        ChatRoom chatRoom = ChatRoom.builder()
                .item(item)
                .owner(resolveOwner(item.getType(), requester, counterpart))
                .finder(resolveFinder(item.getType(), requester, counterpart))
                .build();
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        return new CreateChatRoomResult(savedChatRoom.getId());
    }

    /**
     * 이 <b>요청을 보낸 사람은 게시글을 보고 요청</b>을 보낸다.<br/>
     * <p>item type이 <b>{@link ItemType#FOUND}</b>일 경우 게시글을 작성한 사람은 발견한 사람.
     * <u>이 요청을 보낸 사람이 분실물의 주인</u>이다.<p/>
     * <p>item type이 <b>{@link ItemType#LOST}</b>일 경우 게시글을 작성한 사람은 분실물의 주인.
     * <u>이 요청의 상대가 분실물의 주인</u>이다.<p/>
     *
     * @param itemType    게시글의 종류 (FOUND | LOST)
     * @param requester   게시글을 보고 채팅을 요청한 사람
     * @param counterpart 게시글 작성자
     * @return 분실물의 주인
     */
    private User resolveOwner(ItemType itemType, User requester, User counterpart) {
        if (itemType == ItemType.FOUND)
            return requester;
        return counterpart;
    }

    /**
     * 이 <b>요청을 보낸 사람은 게시글을 보고 요청</b>을 보낸다.<br/>
     * <p>item type이 <b>{@link ItemType#FOUND}</b>일 경우 게시글을 작성한 사람은 발견한 사람.
     * <u>이 요청의 상대가 발견한 사람</u>이다.<p/>
     * <p>item type이 <b>{@link ItemType#LOST}</b>일 경우 게시글을 작성한 사람은 분실물의 주인.
     * <u>이 요청을 보낸 사람이 발견한 사람</u>이다.<p/>
     *
     * @param itemType    게시글의 종류 (FOUND | LOST)
     * @param requester   게시글을 보고 채팅을 요청한 사람
     * @param counterpart 게시글 작성자
     * @return 분실물을 발견한 사람
     */
    private User resolveFinder(ItemType itemType, User requester, User counterpart) {
        if (itemType == ItemType.FOUND)
            return counterpart;
        return requester;
    }

    public FindChatRoomResult findChatRoom(long userId, long itemId) {
        Optional<Long> chatRoomId = chatRoomRepository.findByParticipantIdAndItemIdIs(userId, itemId)
                .map(ChatRoom::getId);

        return new FindChatRoomResult(chatRoomId.isPresent(), chatRoomId.orElse(0L));
    }

    private void validateParticipant(ChatRoom chatRoom, User user) {
        User owner = chatRoom.getOwner();
        User finder = chatRoom.getFinder();
        if (!(owner.getId().equals(user.getId()) || finder.getId().equals(user.getId())))
            throw new BadRequestException("사용자가 포함되지 않은 채팅방입니다.", user.getId() + " is not in chat room " + chatRoom.getId());
    }

    public ListChatRoomResult getChatRooms(long userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByParticipant(userId);
        List<Long> chatRoomIds = chatRooms.stream()
                .map(ChatRoom::getId)
                .toList();
        return new ListChatRoomResult(chatRoomIds);
    }

    public ChatRoomRecord getChatRoom(long userId, long chatRoomId) {
        User user = userRepository.findByIdOrThrow(userId);
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        validateParticipant(chatRoom, user);

        return chatRoomMapper.toChatRoomRecord(chatRoom);
    }

    public ListMessagesResult getMessages(long userId, long chatRoomId, @Nullable MessageFilter filter) {
        User user = userRepository.findByIdOrThrow(userId);
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        validateParticipant(chatRoom, user);

        List<ChatMessage> messages = chatMessageRepository.findAll(ChatMessageRepository.applyFilter(filter));
        List<MessageRecord> messageRecords = messages.stream()
                .map(chatMessageMapper::toMessageRecord)
                .toList();
        ChatRoomRecord chatRoomRecord = chatRoomMapper.toChatRoomRecord(chatRoom);
        return new ListMessagesResult(chatRoomRecord, messageRecords);
    }

    public void sendMessage(long senderId, long chatRoomId, String message) throws FirebaseMessagingException {
        User sender = userRepository.findByIdOrThrow(senderId);
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        validateParticipant(chatRoom, sender);

        User receiver = resolveReceiver(chatRoom, sender);
        ChatMessage chatMessage = ChatMessage.builder()
                .room(chatRoom)
                .sender(sender)
                .content(message)
                .build();
        chatMessageRepository.save(chatMessage);
        SendNotificationCommand command = new SendNotificationCommand(
                sender.getNickname(),
                message,
                ChatMessagePayload.of(chatRoom, sender, message)
        );
        notificationService.send(receiver, command);
    }

    private User resolveReceiver(ChatRoom chatRoom, User sender) {
        User owner = chatRoom.getOwner();
        User finder = chatRoom.getFinder();
        if (finder.getId().equals(sender.getId()))
            return owner;
        return finder;
    }

    public void closeChatRoom(long userId, long chatRoomId, ChatRoomCloseReason reason) {
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        verifyUserInChatRoom(chatRoom, userId);
        if (chatRoom.getStatus() != ChatRoomStatus.OPEN)
            throw new BadRequestException("이미 닫힌 채팅방입니다.", chatRoomId + " is already closed.");

        chatRoom.setStatus(reason.toChatRoomStatus());
        chatRoom.setResolvedAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        // TODO : send notification to counterpart
    }

    public void reopenChatRoom(long userId, long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        verifyUserInChatRoom(chatRoom, userId);
        if (chatRoom.getStatus() == ChatRoomStatus.OPEN)
            throw new BadRequestException("이미 열린 채팅방입니다.", chatRoomId + " is already opened.");

        chatRoom.setStatus(ChatRoomStatus.OPEN);
        chatRoom.setResolvedAt(null);
        chatRoomRepository.save(chatRoom);

        // TODO : send notification to counterpart
    }

    private void verifyUserInChatRoom(ChatRoom chatRoom, long userId) {
        if (!Objects.equals(chatRoom.getOwner().getId(), userId) && !Objects.equals(chatRoom.getFinder().getId(), userId))
            throw new BadRequestException("사용자가 포함되지 않은 채팅방입니다.", userId + " is not in chat room " + chatRoom.getId());
    }
}
