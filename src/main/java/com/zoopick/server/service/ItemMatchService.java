package com.zoopick.server.service;

import com.zoopick.server.config.MatchConfig;
import com.zoopick.server.dto.match.*;
import com.zoopick.server.entity.*;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.repository.ItemMatchRepository;
import com.zoopick.server.repository.ItemPostRepository;
import com.zoopick.server.repository.ItemRepository;
import com.zoopick.server.repository.LockerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Vector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemMatchService {
    private final ItemRepository itemRepository;
    private final ItemMatchRepository itemMatchRepository;
    private final ItemPostRepository itemPostRepository;
    private final LockerRepository lockerRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MatchConfig matchConfig;

    @Transactional
    public void createMatch(Long itemId) {
        log.info("매칭 시작 ID: {}", itemId);
        Item targetItem = itemRepository.findByIdOrThrow(itemId); // 게시글이 올라간 아이템
        Vector embedding = Vector.of(targetItem.getEmbedding());
        List<SimilarItemResult> similarItems = itemMatchRepository.findSimilarItems(
                        embedding,
                        targetItem.getType().name(),
                        targetItem.getCategory().name(),
                        targetItem.getReporter().getId(),
                        matchConfig.getSimilarityThreshold())
                .stream()
                .map(p -> new SimilarItemResult(p.getItemId(), p.getScore()))
                .toList();

        if (similarItems.isEmpty()) {
            log.warn("매칭된 아이템이 없습니다.");
            return;
        }

        Map<Long, Item> itemMap = itemRepository.findAllById(
                        similarItems.stream().map(SimilarItemResult::getItemId).toList())
                .stream()
                .collect(Collectors.toMap(Item::getId, i -> i));

        List<SimilarItemResult> top5 = similarItems.stream()
                .map(s -> {
                    Item found = itemMap.get(s.getItemId());
                    float score = (float) s.getScore();
                    if (targetItem.getColor() == found.getColor()) score *= matchConfig.getColorBonus();
                    return new SimilarItemResult(s.getItemId(), Math.min(score, 1.0f));
                })
                .sorted(Comparator.comparingDouble(SimilarItemResult::getScore).reversed())
                .limit(5)
                .toList();

        List<CreateMatchEvent.Entry> entries = new ArrayList<>();
        Map<Long, String> titleCache = new HashMap<>();

        for (SimilarItemResult s : top5) {
            Item foundItemInDb = itemMap.get(s.getItemId());
            // 게시글에 올라온 아이템이 LOST라면 lostItem에, FOUND라면 foundItem에
            Item lostItem = targetItem.getType() == ItemType.LOST ? targetItem : foundItemInDb;
            Item foundItem = targetItem.getType() == ItemType.LOST ? foundItemInDb : targetItem;

            // 중복 저장 방지
            if (!itemMatchRepository.existsByLostItemAndFoundItem(lostItem, foundItem)) {
                ItemMatch savedMatch = itemMatchRepository.save(ItemMatch.builder()
                        .score((float) s.getScore())
                        .lostItem(lostItem)
                        .foundItem(foundItem)
                        .status(MatchStatus.CANDIDATE)
                        .build());
                log.info("매칭된 아이템 ID: {}", foundItemInDb.getId());

                String title = titleCache.computeIfAbsent(lostItem.getId(),
                        id -> itemPostRepository.findByItem(lostItem).getTitle());
                entries.add(new CreateMatchEvent.Entry(
                        savedMatch.getId(),
                        savedMatch.getScore(),
                        lostItem.getId(),
                        lostItem.getReporter().getId(),
                        title,
                        foundItem.getLocationName()));
            }
        }

        if (!entries.isEmpty()) {
            eventPublisher.publishEvent(new CreateMatchEvent(entries));
        }
        log.info("매칭 종료 ID: {}", targetItem.getId());
    }

    @Transactional
    public List<ItemMatchResultResponse> getItemMatchResult(Long userId) {
        return itemMatchRepository.findMatchResultsByUserId(userId);
    }

    @Transactional
    public MatchConfirmResponse confirmMatch(Long matchId, Long userId) {
        ItemMatch itemMatch = itemMatchRepository.findByIdOrThrow(matchId);

        //유저 검증
        validateMatchOwner(itemMatch, userId);

        Item lostItem = itemMatch.getLostItem();
        Item foundItem = itemMatch.getFoundItem();

        validateMatchStatus(itemMatch);

        boolean wasInLocker = foundItem.getStatus() == ItemStatus.IN_LOCKER;

        lostItem.setStatus(ItemStatus.MATCHED);
        foundItem.setStatus(ItemStatus.MATCHED);
        itemMatch.setStatus(MatchStatus.CONFIRMED);

        itemMatchRepository.rejectOthersByLostItem(matchId, lostItem.getId(), foundItem.getId());
        log.info("매칭 CONFIRMED ID: {}", matchId);

        var responseBuilder = MatchConfirmResponse.builder()
                .matchId(itemMatch.getId())
                .foundItemId(foundItem.getId())
                .counterpartId(foundItem.getReporter().getId());

        if (wasInLocker) {
            Locker locker = lockerRepository.findLockerByCurrentItem(foundItem);
            log.info("매칭 LOCKER {} <-> {}", lostItem.getId(), foundItem.getId());
            return responseBuilder
                    .matchType(MatchManualType.LOCKER)
                    .lockerId(locker.getId())
                    .build();
        }

        log.info("매칭 CHAT {} <-> {}", lostItem.getId(), foundItem.getId());
        return responseBuilder
                .matchType(MatchManualType.CHAT)
                .build();
    }

    @Transactional
    public void rejectMatch(Long matchId, Long userId) {
        ItemMatch itemMatch = itemMatchRepository.findByIdOrThrow(matchId);

        //유저 검증
        validateMatchOwner(itemMatch, userId);

        validateMatchStatus(itemMatch);
        itemMatch.setStatus(MatchStatus.REJECTED);
        log.info("매칭 REJECTED ID: {}", matchId);
    }

    @Transactional
    public MatchManualResponse matchManual(MatchManualRequest request) {
        log.info("수동 매칭 시작: {} <-> {}", request.getLostItemId(), request.getFoundItemId());

        Item lostItem = itemRepository.findByIdOrThrow(request.getLostItemId());
        Item foundItem = itemRepository.findByIdOrThrow(request.getFoundItemId());

        if (itemMatchRepository.existsByLostItemAndFoundItem(lostItem, foundItem)) {
            throw new BadRequestException("이미 진행중인 매칭입니다.");
        }
        validateItemsNotMatched(lostItem, foundItem);

        // 분기 처리
        boolean isLockerType = (foundItem.getStatus() == ItemStatus.IN_LOCKER);

        ItemMatch savedMatch = itemMatchRepository.save(ItemMatch.builder() // 새로운 매칭
                .lostItem(lostItem)
                .foundItem(foundItem)
                .score(1.0f)
                .status(MatchStatus.CONFIRMED)
                .build());

        // 수동 매칭 확정
        lostItem.setStatus(ItemStatus.MATCHED);
        foundItem.setStatus(ItemStatus.MATCHED);

        itemMatchRepository.rejectOthersByLostItem(savedMatch.getId(), lostItem.getId(), foundItem.getId());
        log.info("매칭 저장 완료 ID: {}", savedMatch.getId());

        // 공통 응답
        var responseBuilder = MatchManualResponse.builder()
                .matchId(savedMatch.getId());

        // LOCKER, CHAT 분기
        if (isLockerType) {
            Locker locker = lockerRepository.findLockerByCurrentItem(foundItem);
            log.info("매칭 LOCKER {} <-> {}", request.getLostItemId(), request.getFoundItemId());
            return responseBuilder
                    .matchManualType(MatchManualType.LOCKER)
                    .lockerId(locker.getId())
                    .build();
        }

        log.info("매칭 CHAT {} <-> {}", request.getLostItemId(), request.getFoundItemId());
        return responseBuilder
                .matchManualType(MatchManualType.CHAT)
                .build();
    }

    // 유효 검증
    private void validateMatchStatus(ItemMatch itemMatch) {
        // 이미 처리된 매칭인지 확인
        if (itemMatch.getStatus() == MatchStatus.CONFIRMED) {
            throw new BadRequestException("이미 확정된 매칭입니다.");
        }
        if (itemMatch.getStatus() == MatchStatus.REJECTED) {
            throw new BadRequestException("이미 거절된 매칭입니다.");
        }

        validateItemsNotMatched(itemMatch.getLostItem(), itemMatch.getFoundItem());
    }

    // 이미 확정된 매칭이 있는지 확인
    private void validateItemsNotMatched(Item lostItem, Item foundItem) {
        if (itemMatchRepository.existsByLostItemAndStatus(lostItem, MatchStatus.CONFIRMED) ||
                itemMatchRepository.existsByFoundItemAndStatus(foundItem, MatchStatus.CONFIRMED)) {
            throw new BadRequestException("이미 주인을 찾은 물품이 포함되어 있습니다.");
        }
    }

    //유저 검증
    private void validateMatchOwner(ItemMatch itemMatch, Long userId) {
        if (!itemMatch.getLostItem().getReporter().getId().equals(userId)) {
            throw new BadRequestException("본인의 매칭만 처리할 수 있습니다.");
        }
    }
}