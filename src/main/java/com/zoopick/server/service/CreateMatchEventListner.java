package com.zoopick.server.service;

import com.zoopick.server.dto.match.CreateMatchEvent;
import com.zoopick.server.entity.MatchStatus;
import com.zoopick.server.entity.User;
import com.zoopick.server.repository.ItemMatchRepository;
import com.zoopick.server.repository.UserRepository;
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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateMatchEventListner {
    private final NotificationService notificationService;
    private final ItemMatchRepository itemMatchRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchCreated(CreateMatchEvent event) {
        List<CreateMatchEvent.Entry> entries = event.getEntries();
        if (entries.isEmpty()) return;

        List<Long> reporterIds = entries.stream()
                .map(CreateMatchEvent.Entry::getReporterUserId)
                .distinct()
                .toList();
        Map<Long, User> reporterMap = userRepository.findAllById(reporterIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        for (CreateMatchEvent.Entry entry : entries) {
            User reporter = reporterMap.get(entry.getReporterUserId());
            if (reporter == null) continue;

            notificationService.send(reporter, new SendNotificationCommand(
                    "분실물 발견",
                    "회원님이 등록한 %s와 유사한 물건이 %s에서 발견됐어요."
                            .formatted(entry.getItemPostTitle(), entry.getFoundLocationName()),
                    new MatchFoundPayload(entry.getLostItemId(), entry.getMatchId(), entry.getScore())));
            log.info("FCM 전송 성공 matchId: {}", entry.getMatchId());
        }

        List<Long> matchIds = entries.stream()
                .map(CreateMatchEvent.Entry::getMatchId)
                .toList();
        itemMatchRepository.updateStatusInBatch(matchIds, MatchStatus.NOTIFIED);
    }
}
