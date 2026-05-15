package com.zoopick.server.service;

import com.zoopick.server.dto.chat.*;
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
import com.zoopick.server.service.notification.payload.ChatMessagePayload;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public CreateChatRoomResult createChatRoom(long requesterId, CreateChatRoomRequest createChatRoomRequest) {
        if (Objects.equals(requesterId, createChatRoomRequest.getCounterpartId()))
            throw new BadRequestException("잘못된 요청입니다.", "Requester and counterpart is same : " + requesterId);

        long itemId = createChatRoomRequest.getItemId();
        Item item = itemRepository.findByIdOrThrow(itemId);
        User requester = userRepository.findByIdOrThrow(requesterId);
        User counterpart = userRepository.findByIdOrThrow(createChatRoomRequest.getCounterpartId());

        Optional<ChatRoom> existingChatRoom = chatRoomRepository.findByParticipantIdAndItemIdIs(requesterId, itemId);
        if (existingChatRoom.isPresent()) {
            existingChatRoom.get().setStatus(ChatRoomStatus.OPEN);
            return new CreateChatRoomResult(false, chatRoomMapper.toChatRoomRecord(existingChatRoom.get()));
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .item(item)
                .owner(resolveOwner(item.getType(), requester, counterpart))
                .finder(resolveFinder(item.getType(), requester, counterpart))
                .build();
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        return new CreateChatRoomResult(true, chatRoomMapper.toChatRoomRecord(savedChatRoom));
    }

    public CreateChatRoomResult createChatRoomByOwner(long requesterId, long ownerId) {
        User requester = userRepository.findByIdOrThrow(requesterId);
        User owner = userRepository.findByIdOrThrow(ownerId);

        Optional<ChatRoom> existingChatRoom = chatRoomRepository.findByOwnerIdAndFinderIdIs(ownerId, requesterId);
        if (existingChatRoom.isPresent()) {
            existingChatRoom.get().setStatus(ChatRoomStatus.OPEN);
            return new CreateChatRoomResult(false, chatRoomMapper.toChatRoomRecord(existingChatRoom.get()));
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .owner(owner)
                .finder(requester)
                .build();
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        return new CreateChatRoomResult(true, chatRoomMapper.toChatRoomRecord(savedChatRoom));
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

    private void verifyParticipant(ChatRoom chatRoom, User user) {
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
        verifyParticipant(chatRoom, user);

        return chatRoomMapper.toChatRoomRecord(chatRoom);
    }

    public List<Long> getParticipants(long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        return List.of(chatRoom.getOwner().getId(), chatRoom.getFinder().getId());
    }

    public ListMessagesResult getMessages(long userId, long chatRoomId, @Nullable MessageFilter filter) {
        User user = userRepository.findByIdOrThrow(userId);
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        verifyParticipant(chatRoom, user);

        List<ChatMessage> messages = chatMessageRepository.findByRoomOrderBySentAt(chatRoom, ChatMessageRepository.applyFilter(filter));
        List<MessageRecord> messageRecords = messages.stream()
                .map(chatMessageMapper::toMessageRecord)
                .toList();
        ChatRoomRecord chatRoomRecord = chatRoomMapper.toChatRoomRecord(chatRoom);
        return new ListMessagesResult(chatRoomRecord, messageRecords);
    }

    private MessageContext sendMessage(long senderId, long chatRoomId, String message) {
        User sender = userRepository.findByIdOrThrow(senderId);
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        verifyParticipant(chatRoom, sender);
        if (chatRoom.getStatus() != ChatRoomStatus.OPEN)
            throw new BadRequestException("이미 종료된 채팅방입니다.", chatRoomId + " is closed");

        User receiver = resolveReceiver(chatRoom, sender);
        ChatMessage chatMessage = ChatMessage.builder()
                .room(chatRoom)
                .sender(sender)
                .content(message)
                .build();
        chatMessageRepository.save(chatMessage);
        return new MessageContext(sender, receiver, chatRoom);
    }

    @Transactional
    public void sendMessageWithNotification(long senderId, long chatRoomId, String message) {
        MessageContext context = sendMessage(senderId, chatRoomId, message);
        SendNotificationCommand command = new SendNotificationCommand(
                context.sender().getNickname(),
                message,
                ChatMessagePayload.of(context.chatRoom(), context.sender(), message)
        );
        notificationService.send(context.receiver(), command);
    }

    @Transactional
    public void sendMessageWithoutNotification(long senderId, long chatRoomId, String message) {
        MessageContext context = sendMessage(senderId, chatRoomId, message);
        notificationService.storeNotification(context.receiver().getId(), ChatMessagePayload.of(
                context.chatRoom(), context.sender(), message)
        );
    }

    private User resolveReceiver(ChatRoom chatRoom, User sender) {
        User owner = chatRoom.getOwner();
        User finder = chatRoom.getFinder();
        if (finder.getId().equals(sender.getId()))
            return owner;
        return finder;
    }

    @Transactional
    public void readChatMessages(long userId, long chatRoomId) {
        User user = userRepository.findByIdOrThrow(userId);
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        verifyParticipant(chatRoom, user);

        List<ChatMessage> messages = chatMessageRepository.findByRoomAndSenderIsNot(chatRoom, user);
        messages.forEach(message -> message.setReadAt(LocalDateTime.now()));
        chatMessageRepository.saveAll(messages);
        notificationService.markAllChatsAsRead(userId, chatRoomId);
    }

    @Transactional
    public void closeChatRoom(long userId, long chatRoomId, ChatRoomCloseReason reason) {
        User sender = userRepository.findByIdOrThrow(userId);
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        verifyParticipant(chatRoom, sender);

        User receiver = resolveReceiver(chatRoom, sender);
        if (chatRoom.getStatus() != ChatRoomStatus.OPEN)
            throw new BadRequestException("이미 닫힌 채팅방입니다.", chatRoomId + " is already closed.");

        chatRoom.setStatus(reason.toChatRoomStatus());
        chatRoom.setResolvedAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        SendNotificationCommand command = new SendNotificationCommand(
                "Zoopick",
                "채팅방이 닫혔습니다.",
                ChatMessagePayload.of(
                        chatRoom, sender, "채팅방이 닫혔습니다."
                )
        );
        notificationService.send(receiver, command);
    }

    @Transactional
    public void reopenChatRoom(long userId, long chatRoomId) {
        User sender = userRepository.findByIdOrThrow(userId);
        ChatRoom chatRoom = chatRoomRepository.findByIdOrThrow(chatRoomId);
        verifyParticipant(chatRoom, sender);

        User receiver = resolveReceiver(chatRoom, sender);
        if (chatRoom.getStatus() == ChatRoomStatus.OPEN)
            throw new BadRequestException("이미 열린 채팅방입니다.", chatRoomId + " is already opened.");

        chatRoom.setStatus(ChatRoomStatus.OPEN);
        chatRoom.setResolvedAt(null);
        chatRoomRepository.save(chatRoom);

        SendNotificationCommand command = new SendNotificationCommand(
                "Zoopick",
                "채팅방이 열렸습니다.",
                ChatMessagePayload.of(
                        chatRoom, sender, "채팅방이 열렸습니다."
                )
        );
        notificationService.send(receiver, command);
    }

    private record MessageContext(User sender, User receiver, ChatRoom chatRoom) {

    }
}
