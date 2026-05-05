package com.zoopick.server.controller;

import com.zoopick.server.dto.cctv.*;
import com.zoopick.server.service.CctvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(
        name = "CCTV Internal API",
        description = "AI 서버 → WAS 콜백 전용 API (외부 호출 금지)"
)
@RestController
@RequestMapping("/api/internal/cctv")
@RequiredArgsConstructor
public class CctvInternalController {
    private final CctvService cctvService;

    @Operation(
            summary = "[Internal] 분석 진행률 콜백",
            description = """
            AI 서버가 영상 분석 도중 진행률을 보고할 때 호출하는 엔드포인트입니다.
            
            cctv_video_progress 테이블의 analyzed_seconds, status, 
            estimated_completion_at을 업데이트합니다.
            
            호출 주체: FastAPI 백그라운드 워커
            인증: 내부 통신 (외부 차단 권장)
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "진행률 업데이트 성공"),
            @ApiResponse(responseCode = "404", description = "video_id를 찾을 수 없음"),
    })
    @PostMapping("/progress")
    public ResponseEntity<?> updateProgress(@RequestBody CctvProgressCallback callback) {
        cctvService.updateProgress(callback);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @Operation(
            summary = "[Internal] 검출 결과 등록 콜백",
            description = """
            AI 서버가 영상에서 의심 장면을 검출했을 때 호출하는 엔드포인트입니다.
            
            cctv_detections 테이블에 검출 정보를 INSERT합니다.
            - detection_id: AI 서버가 발급한 멱등성 키 (중복 호출 방어)
            - embedding: 검출 장면의 CLIP 임베딩
            - 스냅샷 파일명 (item / moment 두 종류)
            
            호출 주체: FastAPI 백그라운드 워커
            멱등성 보장: 같은 detection_id 재호출 시 무시
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검출 등록 성공"),
            @ApiResponse(responseCode = "409", description = "이미 처리된 detection_id (멱등성)"),
    })
    @PostMapping("/detection")
    public ResponseEntity<?> registerDetection(@RequestBody CctvDetectionCallback callback) {
        CctvService.DetectionRegisterResult result = cctvService.registerDetection(callback);

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("duplicate", result.duplicate());
        response.put("detection_db_id", result.detectionDbId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "[Internal] 분석 완료 콜백",
            description = """
            AI 서버가 영상 분석을 완료했을 때 호출하는 엔드포인트입니다.
            
            cctv_video_progress.status를 COMPLETED로 전환하고,
            관련된 LOST 신고자에게 분석 완료 알림(FCM)을 발송합니다.
            
            호출 주체: FastAPI 백그라운드 워커
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "완료 처리 성공"),
            @ApiResponse(responseCode = "404", description = "video_id를 찾을 수 없음"),
    })
    @PostMapping("/completed")
    public ResponseEntity<?> completeAnalysis(@RequestBody CctvCompletedCallback callback) {
        cctvService.completeAnalysis(callback);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @Operation(
            summary = "[Internal] 분석 실패 콜백",
            description = """
            AI 서버가 영상 분석에 실패했을 때 호출하는 엔드포인트입니다.
            
            cctv_video_progress.status를 FAILED로 전환하고
            error_code, error_message를 기록합니다.
            
            관리자가 재큐잉(POST /api/cctv/enqueue/{videoId})으로 재시도 가능합니다.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "실패 처리 성공"),
            @ApiResponse(responseCode = "404", description = "video_id를 찾을 수 없음"),
    })
    @PostMapping("/failed")
    public ResponseEntity<?> failAnalysis(@RequestBody CctvFailedCallback callback) {
        cctvService.failAnalysis(callback);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
