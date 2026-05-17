package com.zoopick.server.service;

import com.zoopick.server.config.FastApiProperties;
import com.zoopick.server.dto.cctv.*;
import com.zoopick.server.dto.match.SaveCctvDetectionEvent;
import com.zoopick.server.entity.*;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.exception.DataNotFoundException;
import com.zoopick.server.exception.ForbiddenException;
import com.zoopick.server.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CctvServiceTest {

    @Mock CctvVideoRepository cctvVideoRepository;
    @Mock CctvVideoProgressRepository cctvVideoProgressRepository;
    @Mock CctvDetectionRepository cctvDetectionRepository;
    @Mock CctvDetectionMatchRepository cctvDetectionMatchRepository;
    @Mock RoomRepository roomRepository;
    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock RestClient fastApiRestClient;
    @Mock FastApiProperties fastApiProperties;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks CctvService cctvService;

    @BeforeEach
    void setValueFields() {
        ReflectionTestUtils.setField(cctvService, "snapshotBasePath", "/snapshots/");
        ReflectionTestUtils.setField(cctvService, "callbackBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(cctvService, "storageDir", "/storage");
    }

    @Nested
    @DisplayName("registerDetection()")
    class RegisterDetection {

        CctvDetectionCallback callback;
        CctvVideo cctvVideo;

        @BeforeEach
        void setUp() {
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            cctvVideo = CctvVideo.builder()
                    .id(1L)
                    .room(Room.builder().id(10L).name("101호").build())
                    .recordedAt(LocalDateTime.now())
                    .durationSeconds(300)
                    .videoUrl("cctv/videos/test.mp4")
                    .build();
            callback = CctvDetectionCallback.builder()
                    .videoId(1L)
                    .detectionId("det-001")
                    .detectedAt(LocalDateTime.now())
                    .detectedCategory(ItemCategory.WALLET)
                    .detectedColor(ItemColor.BLACK)
                    .itemSnapshotFilename("item.jpg")
                    .momentSnapshotFilename("moment.jpg")
                    .embedding(new float[512])
                    .build();
        }

        @Test
        @DisplayName("최초 호출이면 Detection을 저장하고 SaveCctvDetectionEvent를 발행한다")
        void firstCall_savesDetectionAndPublishesEvent() {
            given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(true);
            CctvDetection savedDetection = CctvDetection.builder()
                    .id(99L)
                    .cctvVideo(cctvVideo)
                    .detectedAt(LocalDateTime.now())
                    .detectedCategory(ItemCategory.WALLET)
                    .build();
            given(cctvVideoRepository.findById(1L)).willReturn(Optional.of(cctvVideo));
            given(cctvDetectionRepository.save(any())).willReturn(savedDetection);

            CctvService.DetectionRegisterResult result = cctvService.registerDetection(callback);

            assertThat(result.duplicate()).isFalse();
            assertThat(result.detectionDbId()).isEqualTo(99L);
            then(cctvDetectionRepository).should().save(any(CctvDetection.class));
            then(eventPublisher).should().publishEvent(any(SaveCctvDetectionEvent.class));
        }

        @Test
        @DisplayName("중복 호출이면 DB 저장 없이 duplicate=true를 반환한다")
        void duplicateCall_returnsDuplicateWithoutSaving() {
            given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(false);

            CctvService.DetectionRegisterResult result = cctvService.registerDetection(callback);

            assertThat(result.duplicate()).isTrue();
            assertThat(result.detectionDbId()).isNull();
            then(cctvDetectionRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("DB 저장 중 예외 발생 시 Redis 키를 삭제하고 예외를 재전파한다")
        void dbError_deletesRedisKeyAndRethrows() {
            given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(true);
            given(cctvVideoRepository.findById(1L)).willReturn(Optional.of(cctvVideo));
            given(cctvDetectionRepository.save(any())).willThrow(new RuntimeException("DB 오류"));

            assertThatThrownBy(() -> cctvService.registerDetection(callback))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB 오류");
            then(stringRedisTemplate).should().delete(anyString());
        }

        @Test
        @DisplayName("Redis 연결 문제로 setIfAbsent가 null을 반환하면 중복으로 처리한다")
        void redisReturnsNull_treatedAsDuplicate() {
            given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(null);

            CctvService.DetectionRegisterResult result = cctvService.registerDetection(callback);

            assertThat(result.duplicate()).isTrue();
            then(cctvDetectionRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("reviewMatch()")
    class ReviewMatch {

        User reporter;
        User otherUser;
        Item item;
        CctvDetectionMatch match;

        @BeforeEach
        void setUp() {
            reporter = User.builder()
                    .id(1L).schoolEmail("reporter@test.com").password("pw")
                    .nickname("reporter").role(Role.STUDENT).build();
            otherUser = User.builder()
                    .id(2L).schoolEmail("other@test.com").password("pw")
                    .nickname("other").role(Role.STUDENT).build();
            item = Item.builder()
                    .id(10L).reporter(reporter).type(ItemType.LOST).build();
            match = CctvDetectionMatch.builder()
                    .id(100L).item(item).reviewStatus(DetectionReviewStatus.PENDING).build();
        }

        @Test
        @DisplayName("존재하지 않는 matchId로 요청 시 DataNotFoundException을 던진다")
        void matchNotFound_throwsDataNotFoundException() {
            given(cctvDetectionMatchRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> cctvService.reviewMatch(
                    reporter.getId(), 999L,
                    new CctvDetectionReviewRequest(DetectionReviewStatus.CONFIRMED_SELF)))
                    .isInstanceOf(DataNotFoundException.class);
        }

        @Test
        @DisplayName("다른 사용자가 리뷰 시도 시 ForbiddenException을 던진다")
        void otherUser_throwsForbiddenException() {
            given(cctvDetectionMatchRepository.findById(100L)).willReturn(Optional.of(match));

            assertThatThrownBy(() -> cctvService.reviewMatch(
                    otherUser.getId(), 100L,
                    new CctvDetectionReviewRequest(DetectionReviewStatus.CONFIRMED_SELF)))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("PENDING을 review_status로 전달하면 BadRequestException을 던진다")
        void pendingStatus_throwsBadRequestException() {
            given(cctvDetectionMatchRepository.findById(100L)).willReturn(Optional.of(match));

            assertThatThrownBy(() -> cctvService.reviewMatch(
                    reporter.getId(), 100L,
                    new CctvDetectionReviewRequest(DetectionReviewStatus.PENDING)))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("UNCERTAIN을 review_status로 전달하면 BadRequestException을 던진다")
        void uncertainStatus_throwsBadRequestException() {
            given(cctvDetectionMatchRepository.findById(100L)).willReturn(Optional.of(match));

            assertThatThrownBy(() -> cctvService.reviewMatch(
                    reporter.getId(), 100L,
                    new CctvDetectionReviewRequest(DetectionReviewStatus.UNCERTAIN)))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("이미 처리된 매칭에 재요청하면 아무 처리 없이 리턴한다")
        void alreadyProcessed_noOp() {
            match = CctvDetectionMatch.builder()
                    .id(100L).item(item)
                    .reviewStatus(DetectionReviewStatus.CONFIRMED_SELF).build();
            given(cctvDetectionMatchRepository.findById(100L)).willReturn(Optional.of(match));

            cctvService.reviewMatch(reporter.getId(), 100L,
                    new CctvDetectionReviewRequest(DetectionReviewStatus.CONFIRMED_SELF));

            then(cctvDetectionMatchRepository).should(never())
                    .rejectOtherPendingMatches(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("CONFIRMED_SELF 전달 시 도난 의심 처리 및 다른 PENDING 매칭을 거절한다")
        void confirmedSelf_marksTheftAndRejectsOthers() {
            given(cctvDetectionMatchRepository.findById(100L)).willReturn(Optional.of(match));

            cctvService.reviewMatch(reporter.getId(), 100L,
                    new CctvDetectionReviewRequest(DetectionReviewStatus.CONFIRMED_SELF));

            assertThat(item.getTheftSuspectedAt()).isNotNull();
            then(cctvDetectionMatchRepository).should().rejectOtherPendingMatches(
                    eq(item.getId()), eq(100L),
                    eq(DetectionReviewStatus.REJECTED_SELF),
                    eq(DetectionReviewStatus.PENDING),
                    any(LocalDateTime.class));
            assertThat(match.getReviewStatus()).isEqualTo(DetectionReviewStatus.CONFIRMED_SELF);
        }

        @Test
        @DisplayName("REJECTED_SELF 전달 시 도난 의심 처리 없이 상태만 변경된다")
        void rejectedSelf_onlyStatusChangedWithoutTheft() {
            given(cctvDetectionMatchRepository.findById(100L)).willReturn(Optional.of(match));

            cctvService.reviewMatch(reporter.getId(), 100L,
                    new CctvDetectionReviewRequest(DetectionReviewStatus.REJECTED_SELF));

            assertThat(item.getTheftSuspectedAt()).isNull();
            then(cctvDetectionMatchRepository).should(never())
                    .rejectOtherPendingMatches(any(), any(), any(), any(), any());
            assertThat(match.getReviewStatus()).isEqualTo(DetectionReviewStatus.REJECTED_SELF);
        }
    }

    @Nested
    @DisplayName("enqueueVideo()")
    class EnqueueVideo {

        CctvVideo cctvVideo;
        RestClient deepRestClient;

        @BeforeEach
        void setUp() {
            cctvVideo = CctvVideo.builder()
                    .id(1L)
                    .room(Room.builder().id(10L).name("101호").build())
                    .recordedAt(LocalDateTime.now().minusHours(1))
                    .durationSeconds(300)
                    .videoUrl("cctv/videos/test.mp4")
                    .build();
            given(cctvVideoRepository.findById(1L)).willReturn(Optional.of(cctvVideo));

            deepRestClient = mock(RestClient.class, Answers.RETURNS_DEEP_STUBS);
            ReflectionTestUtils.setField(cctvService, "fastApiRestClient", deepRestClient);
        }

        @Test
        @DisplayName("이미 분석 완료된 영상은 BadRequestException을 던진다")
        void completedVideo_throwsBadRequestException() {
            CctvVideoProgress progress = CctvVideoProgress.builder()
                    .id(1L).totalDurationSeconds(300)
                    .status(VideoAnalysisStatus.COMPLETED).build();
            given(cctvVideoProgressRepository.findByCctvVideoId(1L)).willReturn(Optional.of(progress));

            assertThatThrownBy(() -> cctvService.enqueueVideo(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("ALREADY_COMPLETED");
        }

        @Test
        @DisplayName("분석 진행 중인 영상은 BadRequestException을 던진다")
        void inProgressVideo_throwsBadRequestException() {
            CctvVideoProgress progress = CctvVideoProgress.builder()
                    .id(1L).totalDurationSeconds(300)
                    .status(VideoAnalysisStatus.IN_PROGRESS).build();
            given(cctvVideoProgressRepository.findByCctvVideoId(1L)).willReturn(Optional.of(progress));

            assertThatThrownBy(() -> cctvService.enqueueVideo(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("ALREADY_PROCESSING");
        }

        @Test
        @DisplayName("분석 실패한 영상은 상태를 PENDING으로 초기화하고 FastAPI에 재요청한다")
        void failedVideo_resetsToPendingAndCallsFastApi() {
            CctvVideoProgress progress = CctvVideoProgress.builder()
                    .id(1L).totalDurationSeconds(300)
                    .status(VideoAnalysisStatus.FAILED).build();
            given(cctvVideoProgressRepository.findByCctvVideoId(1L)).willReturn(Optional.of(progress));

            FastApiProperties.Cctv cctvProps = mock(FastApiProperties.Cctv.class);
            given(fastApiProperties.getBaseUrl()).willReturn("http://fastapi");
            given(fastApiProperties.getCctv()).willReturn(cctvProps);
            given(cctvProps.getEnqueuePath()).willReturn("/cctv/enqueue");

            cctvService.enqueueVideo(1L);

            assertThat(progress.getStatus()).isEqualTo(VideoAnalysisStatus.PENDING);
            assertThat(progress.getAnalyzedSeconds()).isEqualTo(0);
            then(deepRestClient).should(atLeastOnce()).post();
        }
    }

    @Nested
    @DisplayName("completeAnalysis() / failAnalysis()")
    class AnalysisStatusTransition {

        CctvVideoProgress progress;

        @BeforeEach
        void setUp() {
            progress = CctvVideoProgress.builder()
                    .id(1L)
                    .totalDurationSeconds(300)
                    .status(VideoAnalysisStatus.IN_PROGRESS)
                    .build();
        }

        @Test
        @DisplayName("completeAnalysis() 호출 시 Progress 상태가 COMPLETED로 변경된다")
        void completeAnalysis_setsStatusCompleted() {
            CctvCompletedCallback callback = CctvCompletedCallback.builder()
                    .videoId(1L).totalSeconds(300).totalDetections(5)
                    .startedAt(LocalDateTime.now()).build();
            given(cctvVideoProgressRepository.findByCctvVideoId(1L)).willReturn(Optional.of(progress));

            cctvService.completeAnalysis(callback);

            assertThat(progress.getStatus()).isEqualTo(VideoAnalysisStatus.COMPLETED);
            assertThat(progress.getAnalyzedSeconds()).isEqualTo(300);
        }

        @Test
        @DisplayName("failAnalysis() 호출 시 Progress 상태가 FAILED로 변경된다")
        void failAnalysis_setsStatusFailed() {
            CctvFailedCallback callback = CctvFailedCallback.builder()
                    .videoId(1L).errorCode("TIMEOUT").errorMessage("분석 타임아웃")
                    .analyzedSeconds(100).build();
            given(cctvVideoProgressRepository.findByCctvVideoId(1L)).willReturn(Optional.of(progress));

            cctvService.failAnalysis(callback);

            assertThat(progress.getStatus()).isEqualTo(VideoAnalysisStatus.FAILED);
            assertThat(progress.getAnalyzedSeconds()).isEqualTo(100);
        }
    }
}
