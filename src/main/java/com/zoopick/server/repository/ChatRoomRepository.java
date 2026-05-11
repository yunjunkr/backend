package com.zoopick.server.repository;

import com.zoopick.server.entity.ChatRoom;
import com.zoopick.server.exception.DataNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    default ChatRoom findByIdOrThrow(long id) {
        return findById(id).orElseThrow(() -> DataNotFoundException.from("채팅방", id));
    }

    List<ChatRoom> findByOwnerIdOrFinderId(long ownerId, long finderId);

    default List<ChatRoom> findByParticipant(long userId) {
        return findByOwnerIdOrFinderId(userId, userId);
    }

    Optional<ChatRoom> findByOwnerIdOrFinderIdAndItemIdIs(long ownerId, long finderId, long itemId);

    default Optional<ChatRoom> findByParticipantIdAndItemIdIs(long userId, long itemId) {
        return findByOwnerIdOrFinderIdAndItemIdIs(userId, userId, itemId);
    }

    long countByOwnerIdOrFinderId(Long ownerId, Long finderId);

    Optional<ChatRoom> findByOwnerIdAndFinderIdIs(long ownerId, long finderId);
}
