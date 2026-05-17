package com.zoopick.server.service;

import com.zoopick.server.config.MatchConfig;
import com.zoopick.server.dto.match.CctvMatchCriteria;
import com.zoopick.server.dto.match.CreateCctvMatchEvent;
import com.zoopick.server.dto.match.SimilarItemProjection;
import com.zoopick.server.entity.*;
import com.zoopick.server.repository.CctvDetectionMatchRepository;
import com.zoopick.server.repository.CctvDetectionRepository;
import com.zoopick.server.repository.ItemPostRepository;
import com.zoopick.server.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CctvMatchServiceTest {

    @Mock MatchConfig matchConfig;
    @Mock CctvDetectionRepository cctvDetectionRepository;
    @Mock CctvDetectionMatchRepository cctvDetectionMatchRepository;
    @Mock ItemRepository itemRepository;
    @Mock ItemPostRepository itemPostRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock CctvMatchCriteriaResolver cctvMatchCriteriaResolver;

    @InjectMocks CctvMatchService cctvMatchService;

    @Nested
    @DisplayName("calculateBonusScore() — private 메서드, 리플렉션으로 검증")
    class CalculateBonusScore {

        @Test
        @DisplayName("색상이 일치하면 colorBonus를 적용한 점수를 반환한다")
        void sameColor_appliesColorBonus() {
            given(matchConfig.getColorBonus()).willReturn(1.1f);

            float result = invokeCalculateBonusScore(0.8, ItemColor.BLACK, ItemColor.BLACK);

            assertThat(result).isEqualTo(0.8f * 1.1f);
        }

        @Test
        @DisplayName("색상이 다르면 기본 점수를 그대로 반환한다")
        void differentColor_returnsBaseScore() {
            float result = invokeCalculateBonusScore(0.8, ItemColor.BLACK, ItemColor.WHITE);

            assertThat(result).isEqualTo(0.8f);
        }

        @Test
        @DisplayName("보너스 적용 후 1.0을 초과하면 1.0으로 제한한다")
        void bonusExceedsMax_capsAtOne() {
            given(matchConfig.getColorBonus()).willReturn(1.5f);

            float result = invokeCalculateBonusScore(0.9, ItemColor.BLACK, ItemColor.BLACK);

            assertThat(result).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("검출 색상이 null이면 보너스 없이 기본 점수를 반환한다")
        void detectionColorNull_returnsBaseScore() {
            float result = invokeCalculateBonusScore(0.8, ItemColor.BLACK, null);

            assertThat(result).isEqualTo(0.8f);
        }

        private float invokeCalculateBonusScore(double baseScore, ItemColor itemColor, ItemColor detectionColor) {
            return (float) ReflectionTestUtils.invokeMethod(
                    cctvMatchService, "calculateBonusScore", baseScore, itemColor, detectionColor);
        }
    }

    @Nested
    @DisplayName("matchCctvToLostItems()")
    class MatchCctvToLostItems {

        Room room;
        CctvVideo cctvVideo;
        CctvDetection cctvDetection;
        User reporter;
        Item lostItem;
        ItemPost itemPost;

        @BeforeEach
        void setUp() {
            room = Room.builder().id(10L).name("101호").build();
            cctvVideo = CctvVideo.builder()
                    .id(1L).room(room)
                    .recordedAt(LocalDateTime.now().minusHours(1))
                    .durationSeconds(300).videoUrl("cctv/videos/test.mp4")
                    .build();
            cctvDetection = CctvDetection.builder()
                    .id(1L).cctvVideo(cctvVideo)
                    .detectedAt(LocalDateTime.now())
                    .detectedCategory(ItemCategory.WALLET)
                    .detectedColor(ItemColor.BLACK)
                    .embedding(new float[512])
                    .build();
            reporter = User.builder()
                    .id(1L).schoolEmail("user@test.com").password("pw")
                    .nickname("user").role(Role.STUDENT).build();
            lostItem = Item.builder()
                    .id(100L).reporter(reporter).type(ItemType.LOST)
                    .category(ItemCategory.WALLET).color(ItemColor.BLACK)
                    .embedding(new float[512])
                    .reportedAt(LocalDateTime.now().minusHours(2))
                    .build();
            itemPost = ItemPost.builder().id(1L).item(lostItem).title("지갑 잃어버렸어요").build();
        }

        @Test
        @DisplayName("Detection을 찾지 못하면 아무 처리도 하지 않는다")
        void detectionNotFound_doesNothing() {
            given(cctvDetectionRepository.findById(1L)).willReturn(Optional.empty());

            cctvMatchService.matchCctvToLostItems(1L);

            then(cctvDetectionMatchRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("유사 분실물이 없으면 매칭을 저장하지 않는다")
        void noSimilarItems_doesNotSaveMatch() {
            given(cctvDetectionRepository.findById(1L)).willReturn(Optional.of(cctvDetection));
            given(matchConfig.getSimilarityThreshold()).willReturn(0.7f);
            given(cctvDetectionMatchRepository.findLostItems(any(), any(), any(), anyFloat()))
                    .willReturn(List.of());

            cctvMatchService.matchCctvToLostItems(1L);

            then(cctvDetectionMatchRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("이미 매칭이 존재하면 중복 저장하지 않는다")
        void matchAlreadyExists_doesNotSaveDuplicate() {
            SimilarItemProjection projection = mockProjection(100L, 0.9);
            given(cctvDetectionRepository.findById(1L)).willReturn(Optional.of(cctvDetection));
            given(matchConfig.getSimilarityThreshold()).willReturn(0.7f);
            given(cctvDetectionMatchRepository.findLostItems(any(), any(), any(), anyFloat()))
                    .willReturn(List.of(projection));
            given(itemPostRepository.findAllByItemIdsWithItem(any())).willReturn(List.of(itemPost));
            given(cctvMatchCriteriaResolver.resolve(lostItem))
                    .willReturn(new CctvMatchCriteria(List.of(10L), LocalDateTime.now().minusHours(3)));
            given(cctvDetectionMatchRepository.existsByCctvDetectionAndItem(cctvDetection, lostItem)).willReturn(true);

            cctvMatchService.matchCctvToLostItems(1L);

            then(cctvDetectionMatchRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("검출 시각이 기준 시작시간 이전이면 해당 아이템은 매칭하지 않는다")
        void detectionBeforeSearchStart_skipsItem() {
            SimilarItemProjection projection = mockProjection(100L, 0.9);
            given(cctvDetectionRepository.findById(1L)).willReturn(Optional.of(cctvDetection));
            given(matchConfig.getSimilarityThreshold()).willReturn(0.7f);
            given(cctvDetectionMatchRepository.findLostItems(any(), any(), any(), anyFloat()))
                    .willReturn(List.of(projection));
            given(itemPostRepository.findAllByItemIdsWithItem(any())).willReturn(List.of(itemPost));
            // searchStartTime을 검출 시각보다 미래로 설정 → 검출 시각이 기준 이전이 됨
            given(cctvMatchCriteriaResolver.resolve(lostItem))
                    .willReturn(new CctvMatchCriteria(List.of(10L), LocalDateTime.now().plusHours(1)));

            cctvMatchService.matchCctvToLostItems(1L);

            then(cctvDetectionMatchRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("검출된 Room이 아이템의 검색 범위에 없으면 해당 아이템을 건너뛴다")
        void detectionRoomNotInCriteria_skipsItem() {
            SimilarItemProjection projection = mockProjection(100L, 0.9);
            given(cctvDetectionRepository.findById(1L)).willReturn(Optional.of(cctvDetection));
            given(matchConfig.getSimilarityThreshold()).willReturn(0.7f);
            given(cctvDetectionMatchRepository.findLostItems(any(), any(), any(), anyFloat()))
                    .willReturn(List.of(projection));
            given(itemPostRepository.findAllByItemIdsWithItem(any())).willReturn(List.of(itemPost));
            // criteria.roomIds()에 검출 room(10L)이 포함되지 않음
            given(cctvMatchCriteriaResolver.resolve(lostItem))
                    .willReturn(new CctvMatchCriteria(List.of(99L), LocalDateTime.now().minusHours(3)));

            cctvMatchService.matchCctvToLostItems(1L);

            then(cctvDetectionMatchRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("itemPostMap에 아이템이 없으면 해당 아이템을 건너뛴다")
        void itemPostNotInMap_skipsItem() {
            SimilarItemProjection projection = mockProjection(100L, 0.9);
            given(cctvDetectionRepository.findById(1L)).willReturn(Optional.of(cctvDetection));
            given(matchConfig.getSimilarityThreshold()).willReturn(0.7f);
            given(cctvDetectionMatchRepository.findLostItems(any(), any(), any(), anyFloat()))
                    .willReturn(List.of(projection));
            // itemId=100L에 해당하는 ItemPost 없음
            given(itemPostRepository.findAllByItemIdsWithItem(any())).willReturn(List.of());

            cctvMatchService.matchCctvToLostItems(1L);

            then(cctvDetectionMatchRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("정상 케이스에서 Match를 저장하고 CreateCctvMatchEvent를 발행한다")
        void normalCase_savesMatchAndPublishesEvent() {
            SimilarItemProjection projection = mockProjection(100L, 0.9);
            CctvDetectionMatch savedMatch = CctvDetectionMatch.builder()
                    .id(1L).item(lostItem).cctvDetection(cctvDetection).score(0.9f).build();
            given(cctvDetectionRepository.findById(1L)).willReturn(Optional.of(cctvDetection));
            given(matchConfig.getSimilarityThreshold()).willReturn(0.7f);
            given(matchConfig.getColorBonus()).willReturn(1.1f);
            given(cctvDetectionMatchRepository.findLostItems(any(), any(), any(), anyFloat()))
                    .willReturn(List.of(projection));
            given(itemPostRepository.findAllByItemIdsWithItem(any())).willReturn(List.of(itemPost));
            given(cctvMatchCriteriaResolver.resolve(lostItem))
                    .willReturn(new CctvMatchCriteria(List.of(10L), LocalDateTime.now().minusHours(3)));
            given(cctvDetectionMatchRepository.existsByCctvDetectionAndItem(cctvDetection, lostItem)).willReturn(false);
            given(cctvDetectionMatchRepository.save(any())).willReturn(savedMatch);

            cctvMatchService.matchCctvToLostItems(1L);

            then(cctvDetectionMatchRepository).should().save(any(CctvDetectionMatch.class));
            then(eventPublisher).should().publishEvent(any(CreateCctvMatchEvent.class));
        }

        private SimilarItemProjection mockProjection(Long itemId, double score) {
            return new SimilarItemProjection() {
                @Override public Long getItemId() { return itemId; }
                @Override public Double getScore() { return score; }
            };
        }
    }

    @Nested
    @DisplayName("matchLostItemsToCctv()")
    class MatchLostItemsToCctv {

        Room room;
        CctvVideo cctvVideo;
        CctvDetection cctvDetection;
        User reporter;
        Item lostItem;
        ItemPost lostItemPost;

        @BeforeEach
        void setUp() {
            room = Room.builder().id(10L).name("101호").build();
            cctvVideo = CctvVideo.builder()
                    .id(1L).room(room)
                    .recordedAt(LocalDateTime.now().minusHours(1))
                    .durationSeconds(300).videoUrl("cctv/videos/test.mp4")
                    .build();
            cctvDetection = CctvDetection.builder()
                    .id(50L).cctvVideo(cctvVideo)
                    .detectedAt(LocalDateTime.now())
                    .detectedCategory(ItemCategory.WALLET)
                    .detectedColor(ItemColor.BLACK)
                    .embedding(new float[512])
                    .build();
            reporter = User.builder()
                    .id(1L).schoolEmail("user@test.com").password("pw")
                    .nickname("user").role(Role.STUDENT).build();
            lostItem = Item.builder()
                    .id(100L).reporter(reporter).type(ItemType.LOST)
                    .category(ItemCategory.WALLET).color(ItemColor.BLACK)
                    .embedding(new float[512])
                    .reportedAt(LocalDateTime.now().minusHours(2))
                    .build();
            lostItemPost = ItemPost.builder().id(1L).item(lostItem).title("지갑 잃어버렸어요").build();
        }

        @Test
        @DisplayName("criteria.roomIds()가 비어있으면 조기 return하고 findDetections()를 호출하지 않는다")
        void emptyRoomIds_returnsEarlyWithoutQuery() {
            given(itemRepository.findByIdOrThrow(100L)).willReturn(lostItem);
            given(itemPostRepository.findByItem(lostItem)).willReturn(lostItemPost);
            given(cctvMatchCriteriaResolver.resolve(lostItem))
                    .willReturn(new CctvMatchCriteria(List.of(), LocalDateTime.now().minusHours(3)));

            cctvMatchService.matchLostItemsToCctv(100L);

            then(cctvDetectionMatchRepository).should(never()).findDetections(any(), any(), any(), any(), anyFloat());
            then(cctvDetectionMatchRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("유사 검출이 없으면 Match를 저장하지 않는다")
        void noSimilarDetections_doesNotSaveMatch() {
            given(itemRepository.findByIdOrThrow(100L)).willReturn(lostItem);
            given(itemPostRepository.findByItem(lostItem)).willReturn(lostItemPost);
            given(matchConfig.getSimilarityThreshold()).willReturn(0.7f);
            given(cctvMatchCriteriaResolver.resolve(lostItem))
                    .willReturn(new CctvMatchCriteria(List.of(10L), LocalDateTime.now().minusHours(3)));
            given(cctvDetectionMatchRepository.findDetections(any(), any(), any(), any(), anyFloat()))
                    .willReturn(List.of());

            cctvMatchService.matchLostItemsToCctv(100L);

            then(cctvDetectionMatchRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("detectionMap에 해당 검출이 없으면 건너뛴다")
        void detectionNotInMap_skipsItem() {
            SimilarItemProjection projection = mockProjection(50L, 0.9);
            given(itemRepository.findByIdOrThrow(100L)).willReturn(lostItem);
            given(itemPostRepository.findByItem(lostItem)).willReturn(lostItemPost);
            given(matchConfig.getSimilarityThreshold()).willReturn(0.7f);
            given(cctvMatchCriteriaResolver.resolve(lostItem))
                    .willReturn(new CctvMatchCriteria(List.of(10L), LocalDateTime.now().minusHours(3)));
            given(cctvDetectionMatchRepository.findDetections(any(), any(), any(), any(), anyFloat()))
                    .willReturn(List.of(projection));
            // detectionId=50L에 해당하는 CctvDetection 없음
            given(cctvDetectionRepository.findAllById(any())).willReturn(List.of());

            cctvMatchService.matchLostItemsToCctv(100L);

            then(cctvDetectionMatchRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("이미 매칭이 존재하면 중복 저장하지 않는다")
        void matchAlreadyExists_doesNotSaveDuplicate() {
            SimilarItemProjection projection = mockProjection(50L, 0.9);
            given(itemRepository.findByIdOrThrow(100L)).willReturn(lostItem);
            given(itemPostRepository.findByItem(lostItem)).willReturn(lostItemPost);
            given(matchConfig.getSimilarityThreshold()).willReturn(0.7f);
            given(cctvMatchCriteriaResolver.resolve(lostItem))
                    .willReturn(new CctvMatchCriteria(List.of(10L), LocalDateTime.now().minusHours(3)));
            given(cctvDetectionMatchRepository.findDetections(any(), any(), any(), any(), anyFloat()))
                    .willReturn(List.of(projection));
            given(cctvDetectionRepository.findAllById(any())).willReturn(List.of(cctvDetection));
            given(cctvDetectionMatchRepository.existsByCctvDetectionAndItem(cctvDetection, lostItem)).willReturn(true);

            cctvMatchService.matchLostItemsToCctv(100L);

            then(cctvDetectionMatchRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("정상 케이스에서 Match를 저장하고 CreateCctvMatchEvent를 발행한다")
        void normalCase_savesMatchAndPublishesEvent() {
            SimilarItemProjection projection = mockProjection(50L, 0.9);
            CctvDetectionMatch savedMatch = CctvDetectionMatch.builder()
                    .id(1L).item(lostItem).cctvDetection(cctvDetection).score(0.9f).build();
            given(itemRepository.findByIdOrThrow(100L)).willReturn(lostItem);
            given(itemPostRepository.findByItem(lostItem)).willReturn(lostItemPost);
            given(matchConfig.getSimilarityThreshold()).willReturn(0.7f);
            given(matchConfig.getColorBonus()).willReturn(1.1f);
            given(cctvMatchCriteriaResolver.resolve(lostItem))
                    .willReturn(new CctvMatchCriteria(List.of(10L), LocalDateTime.now().minusHours(3)));
            given(cctvDetectionMatchRepository.findDetections(any(), any(), any(), any(), anyFloat()))
                    .willReturn(List.of(projection));
            given(cctvDetectionRepository.findAllById(any())).willReturn(List.of(cctvDetection));
            given(cctvDetectionMatchRepository.existsByCctvDetectionAndItem(cctvDetection, lostItem)).willReturn(false);
            given(cctvDetectionMatchRepository.save(any())).willReturn(savedMatch);

            cctvMatchService.matchLostItemsToCctv(100L);

            then(cctvDetectionMatchRepository).should().save(any(CctvDetectionMatch.class));
            then(eventPublisher).should().publishEvent(any(CreateCctvMatchEvent.class));
        }

        private SimilarItemProjection mockProjection(Long itemId, double score) {
            return new SimilarItemProjection() {
                @Override public Long getItemId() { return itemId; }
                @Override public Double getScore() { return score; }
            };
        }
    }
}
