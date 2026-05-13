package com.zoopick.server.service;

import com.zoopick.server.dto.item.ItemCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ItemPostEventListner {
    private final VisionService visionService;
    //db에 commit한 이벤트를 받으면 실행
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleItemCreated(ItemCreatedEvent event) {
        visionService.analyzeImage(event.itemId());
    }
}
