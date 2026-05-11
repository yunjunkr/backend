package com.zoopick.server.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.zoopick.server.dto.match.CreateMatchEvent;
import com.zoopick.server.entity.Item;
import com.zoopick.server.entity.ItemMatch;
import com.zoopick.server.entity.MatchStatus;
import com.zoopick.server.repository.ItemMatchRepository;
import com.zoopick.server.repository.ItemPostRepository;
import com.zoopick.server.repository.ItemRepository;
import com.zoopick.server.service.notification.NotificationService;
import com.zoopick.server.service.notification.SendNotificationCommand;
import com.zoopick.server.service.notification.payload.MatchFoundPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateMatchEventListner {
    private final NotificationService notificationService;
    private final ItemMatchRepository itemMatchRepository;
    private final ItemPostRepository itemPostRepository;
    private final ItemRepository itemRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchCreated(CreateMatchEvent event) {
        ItemMatch match = itemMatchRepository.findByIdOrThrow(event.matchId());
        Item lostItem = itemRepository.findByIdOrThrow(event.lostItemId());
        Item foundItem = itemRepository.findByIdOrThrow(event.foundItemId());
        String title = itemPostRepository.findByItem(lostItem).getTitle();
        String location = foundItem.getLocationName();
        try {
            notificationService.send(lostItem.getReporter(), new SendNotificationCommand(
                    "분실물 발견",
                    "회원님이 등록한 %s와 유사한 물건이 %s에서 발견됐어요.".formatted(title, location),
                    MatchFoundPayload.of(lostItem, match)));
            match.setStatus(MatchStatus.NOTIFIED);
            log.info("FCM 전송 성공 matchId: {}", match.getId());
        } catch (FirebaseMessagingException e) {
            log.error("FCM 전송 실패 (matchId: {}): {}", match.getId(), e.getMessage());
        }
    }
}
