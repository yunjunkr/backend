package com.zoopick.server.service;

import com.zoopick.server.config.FastApiProperties;
import com.zoopick.server.dto.cctv.*;
import com.zoopick.server.entity.*;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.exception.DataNotFoundException;
import com.zoopick.server.repository.CctvDetectionRepository;
import com.zoopick.server.repository.CctvVideoProgressRepository;
import com.zoopick.server.repository.CctvVideoRepository;
import com.zoopick.server.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CctvService {
    private static final String DETECTION_IDEMPOTENCY_KEY_PREFIX = "cctv:detection:";
    private static final Duration DETECTION_IDEMPOTENCY_TTL = Duration.ofMinutes(5);

    private final CctvVideoRepository cctvVideoRepository;
    private final CctvVideoProgressRepository cctvVideoProgressRepository;
    private final CctvDetectionRepository cctvDetectionRepository;
    private final RoomRepository roomRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final RestClient fastApiRestClient;
    private final FastApiProperties fastApiProperties;

    @Value("${zoopick.callback-url}")
    private String callbackBaseUrl;

    @Value("${zoopick.cctv.snapshot-dir}")
    private String snapshotBasePath;

    @Value("${zoopick.cctv.storage-absolute-dir}")
    private String storageDir;

    @Transactional
    public Long createVideo(CctvVideoCreateRequest request) {
        Room room = roomRepository.findByIdOrThrow(request.getRoomId());

        CctvVideo savedVideo = cctvVideoRepository.save(
                CctvVideo.builder()
                        .room(room)
                        .recordedAt(request.getRecordedAt())
                        .durationSeconds(request.getDurationSeconds())
                        .videoUrl(request.getVideoUrl())
                        .build());

        return savedVideo.getId();
    }

    public CctvVideoCreateResponse createVideoAndEnqueue(CctvVideoCreateRequest request) {
        Long videoId = createVideo(request);

        try {
            CctvEnqueueResponse enqueueResponse = enqueueVideo(videoId);
            return CctvVideoCreateResponse.builder()
                    .videoId(videoId)
                    .queued(enqueueResponse != null && enqueueResponse.isQueued())
                    .queuePosition(enqueueResponse != null ? enqueueResponse.getQueuePosition() : null)
                    .estimatedStartAt(enqueueResponse != null ? enqueueResponse.getEstimatedStartAt() : null)
                    .build();
        } catch (BadRequestException exception) {
            log.warn("CCTV enqueue failed after video save: video_id={}, reason={}", videoId,
                    exception.getClientMessage());
            return CctvVideoCreateResponse.builder()
                    .videoId(videoId)
                    .queued(false)
                    .reason(exception.getClientMessage())
                    .build();
        }
    }

    @Transactional
    public CctvEnqueueResponse enqueueVideo(Long videoId) {
        CctvVideo video = cctvVideoRepository.findById(videoId)
                .orElseThrow(() -> new DataNotFoundException("비디오를 찾을 수 없습니다. ID: " + videoId, "VIDEO_NOT_FOUND"));

        // Progress 정보 확인 및 중복 분석 방지
        CctvVideoProgress progress = cctvVideoProgressRepository.findByCctvVideoId(videoId).orElse(null);

        if (progress != null) {
            if (progress.getStatus() == VideoAnalysisStatus.COMPLETED) {
                throw new BadRequestException("이미 분석이 완료된 비디오입니다. ID: " + videoId, "ALREADY_COMPLETED");
            } else if (progress.getStatus() == VideoAnalysisStatus.IN_PROGRESS) {
                throw new BadRequestException("이미 분석이 진행 중인 비디오입니다. ID: " + videoId, "ALREADY_PROCESSING");
            }
            // FAILED인 경우 재시도를 위해 상태 초기화
            progress.setStatus(VideoAnalysisStatus.PENDING);
            progress.setAnalyzedSeconds(0);
        } else {
            progress = CctvVideoProgress.builder()
                    .cctvVideo(video)
                    .status(VideoAnalysisStatus.PENDING)
                    .totalDurationSeconds(video.getDurationSeconds())
                    .build();
        }
        cctvVideoProgressRepository.save(progress);

        // FastAPI 요청 DTO 생성
        CctvEnqueueRequest request = CctvEnqueueRequest.builder()
                .videoId(videoId)
                .videoPath(video.getVideoUrl())
                .durationSeconds(progress.getTotalDurationSeconds())
                .recordedAt(video.getRecordedAt())
                .callbackBaseUrl(callbackBaseUrl)
                .build();

        String url = fastApiProperties.getBaseUrl() + fastApiProperties.getCctv().getEnqueuePath();

        try {
            CctvEnqueueResponse response = fastApiRestClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(CctvEnqueueResponse.class);

            if (response != null && response.isQueued()) {
                log.info("CCTV Video analysis queued successfully: video_id={}", videoId);
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to enqueue CCTV video analysis: video_id={}, error={}", videoId, e.getMessage());
            throw new BadRequestException("FastAPI 서버에 분석 요청을 보내지 못했습니다.", e.getMessage());
        }
    }

    @Transactional
    public void updateProgress(CctvProgressCallback callback) {
        CctvVideoProgress progress = cctvVideoProgressRepository.findByCctvVideoId(callback.getVideoId())
                .orElseThrow(() -> new BadRequestException("진행 정보를 찾을 수 없습니다. video_id: " + callback.getVideoId(),
                        "PROGRESS_NOT_FOUND"));

        progress.setStatus(VideoAnalysisStatus.valueOf(callback.getStatus()));
        progress.setAnalyzedSeconds(callback.getAnalyzedSeconds());
        progress.setEstimatedCompletionAt(callback.getEstimatedCompletionAt());

        if (progress.getStatus() == VideoAnalysisStatus.IN_PROGRESS && progress.getStartedAt() == null) {
            progress.setStartedAt(LocalDateTime.now());
        }

        cctvVideoProgressRepository.save(progress);

        double calculatedPercent = callback.getTotalSeconds() > 0
                ? Math.round((double) callback.getAnalyzedSeconds() / callback.getTotalSeconds() * 1000.0) / 10.0
                : 0.0;

        log.info("CCTV Progress updated: video_id={}, progress={}%, analyzed_sec={}, total_sec={}, status={}",
                callback.getVideoId(), calculatedPercent, callback.getAnalyzedSeconds(), callback.getTotalSeconds(),
                callback.getStatus());
    }

    @Transactional
    public DetectionRegisterResult registerDetection(CctvDetectionCallback callback) {
        String idempotencyKey = detectionIdempotencyKey(callback.getVideoId(), callback.getDetectionId());
        Boolean isFirstRequest = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "1", DETECTION_IDEMPOTENCY_TTL);

        if (!Boolean.TRUE.equals(isFirstRequest)) {
            log.info("Duplicate detection callback ignored: video_id={}, ai_detection_id={}",
                    callback.getVideoId(), callback.getDetectionId());
            return new DetectionRegisterResult(true, null);
        }

        CctvVideo video = cctvVideoRepository.findById(callback.getVideoId())
                .orElseThrow(() -> new DataNotFoundException("비디오를 찾을 수 없습니다. video_id: " + callback.getVideoId(),
                        "VIDEO_NOT_FOUND"));

        String itemUrl = snapshotBasePath + callback.getItemSnapshotFilename();
        String momentUrl = snapshotBasePath + callback.getMomentSnapshotFilename();

        CctvDetection detection = CctvDetection.builder()
                .cctvVideo(video)
                .detectedAt(callback.getDetectedAt())
                .detectedCategory(callback.getDetectedCategory())
                .detectedColor(callback.getDetectedColor())
                .itemSnapshotUrl(itemUrl)
                .momentSnapshotUrl(momentUrl)
                .embedding(callback.getEmbedding())
                .build();

        try {
            CctvDetection savedDetection = cctvDetectionRepository.save(detection);
            log.info("New detection registered: video_id={}, ai_detection_id={}",
                    callback.getVideoId(), callback.getDetectionId());
            return new DetectionRegisterResult(false, savedDetection.getId());
        } catch (RuntimeException e) {
            stringRedisTemplate.delete(idempotencyKey);
            throw e;
        }
    }

    @Transactional
    public void completeAnalysis(CctvCompletedCallback callback) {
        CctvVideoProgress progress = cctvVideoProgressRepository.findByCctvVideoId(callback.getVideoId())
                .orElseThrow(() -> new DataNotFoundException("진행 정보를 찾을 수 없습니다. video_id: " + callback.getVideoId(),
                        "PROGRESS_NOT_FOUND"));

        progress.setStatus(VideoAnalysisStatus.COMPLETED);
        progress.setAnalyzedSeconds(callback.getTotalSeconds());
        progress.setStartedAt(callback.getStartedAt());

        cctvVideoProgressRepository.save(progress);
        log.info("CCTV Video analysis completed: video_id={}, total_detections={}",
                callback.getVideoId(), callback.getTotalDetections());
        // TODO: 매칭 트리거 및 알림 로직 추가
    }

    @Transactional
    public void failAnalysis(CctvFailedCallback callback) {
        CctvVideoProgress progress = cctvVideoProgressRepository.findByCctvVideoId(callback.getVideoId())
                .orElseThrow(() -> new DataNotFoundException("진행 정보를 찾을 수 없습니다. video_id: " + callback.getVideoId(),
                        "PROGRESS_NOT_FOUND"));

        progress.setStatus(VideoAnalysisStatus.FAILED);
        progress.setAnalyzedSeconds(callback.getAnalyzedSeconds());

        cctvVideoProgressRepository.save(progress);
        log.error("CCTV Video analysis failed: video_id={}, error_code={}, error_message={}",
                callback.getVideoId(), callback.getErrorCode(), callback.getErrorMessage());
    }

    private String detectionIdempotencyKey(Long videoId, String detectionId) {
        return DETECTION_IDEMPOTENCY_KEY_PREFIX + videoId + ":" + detectionId;
    }

    public record DetectionRegisterResult(boolean duplicate, Long detectionDbId) {
    }

    @Transactional(readOnly = true)
    public List<GetCctvVideoResponse> getCctvVideos() {
        return cctvVideoRepository.findAllCctvVideosWithProgress();
    }

    @Transactional(readOnly = true)
    public List<GetAllDetectionResponse> getAllCctvDetection() {
        return cctvDetectionRepository.findAllByOrderByDetectedAtAsc().stream()
                .map(entity -> new GetAllDetectionResponse(
                        entity.getId(),
                        entity.getCctvVideo().getId(),
                        entity.getDetectedAt(),
                        entity.getDetectedCategory(),
                        entity.getDetectedColor(),
                        entity.getReviewStatus()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public GetDetectionByIdResponse getCctvDetectionById(Long detectionId) {
        CctvDetection entity = cctvDetectionRepository.findById(detectionId)
                .orElseThrow(() -> DataNotFoundException.from("CCTV 물품 정보", detectionId));

        return new GetDetectionByIdResponse(
                entity.getId(),
                entity.getCctvVideo().getId(),
                entity.getDetectedAt(),
                entity.getDetectedCategory(),
                entity.getDetectedColor(),
                entity.getEmbedding(),
                entity.getItemSnapshotUrl(),
                entity.getMomentSnapshotUrl(),
                entity.getReviewStatus(),
                entity.getReviewedAt(),
                entity.getCreatedAt()
        );
    }

    public String uploadVideo(MultipartFile file) throws IOException {
        Path dir = Paths.get(storageDir, "cctv", "videos");
        Files.createDirectories(dir);

        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path target = dir.resolve(filename);
        file.transferTo(target);

        //return target.toAbsolutePath().toString(); 절대경로 반환
        return "backend/storage/cctv/videos/" + filename; // 상대경로
    }
}