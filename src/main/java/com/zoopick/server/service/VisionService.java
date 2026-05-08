package com.zoopick.server.service;

import com.zoopick.server.config.FastApiProperties;
import com.zoopick.server.dto.item.ItemCreatedEvent;
import com.zoopick.server.dto.vision.VisionAnalyzeRequest;
import com.zoopick.server.dto.vision.VisionAnalyzeResponse;
import com.zoopick.server.entity.Item;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.exception.DataNotFoundException;
import com.zoopick.server.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisionService {
    private final RestClient fastApiRestClient;
    private final FastApiProperties fastApiProperties;
    private final ItemRepository itemRepository;
    private final ItemMatchService itemMatchService;

    //db에 commit한 이벤트를 받으면 실행
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleItemCreated(ItemCreatedEvent event) {
        analyzeImage(event.itemId());
    }

    public void analyzeImage(Long itemId) {
        log.info("전달된 아이템 ID: {}", itemId);
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new EntityNotFoundException("아이템을 찾을 수 없습니다."));
        VisionAnalyzeRequest request = new VisionAnalyzeRequest(item.getImageUrl());
        String imageUrl = request.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BadRequestException("이미지 URL이 올바르지 않습니다.", "imageUrl is null or blank");
        }
        String url = fastApiProperties.getBaseUrl() + fastApiProperties.getVision().getAnalyzePath();

        try {
            VisionAnalyzeResponse response = fastApiRestClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(VisionAnalyzeResponse.class);
            if (response == null) {
                throw new DataNotFoundException("분석 결과", imageUrl);
            }
            item.setCategory(response.getCategory());
            item.setColor(response.getColor());
            item.setEmbedding(response.getEmbedding());
            itemRepository.save(item);
            itemMatchService.createMatch(item.getId());
        } catch (BadRequestException | DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("FastAPI 요청 중 오류가 발생했습니다.", e.getMessage());
        }
    }
}