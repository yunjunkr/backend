package com.zoopick.server.controller;

import com.zoopick.server.dto.CommonResponse;
import com.zoopick.server.dto.cctv.*;
import com.zoopick.server.service.CctvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "CCTV API", description = "CCTV 영상 등록 및 분석 큐 관리")
@RestController
@RequestMapping("/api/cctv")
@RequiredArgsConstructor
public class CctvController {
    private final CctvService cctvService;

    @Operation(
            summary = "CCTV 영상 등록 + 분석 큐 등록",
            description = """
            CCTV 영상 메타데이터를 등록하고 즉시 분석 큐에 추가합니다.
            
            등록 후 백그라운드 워커가 큐 순서대로 영상을 분석하며,
            검출 결과는 cctv_detections 테이블에 캐싱됩니다.
            
            응답에는 큐에서의 위치와 예상 시작 시각이 포함됩니다.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "등록 + 큐 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (room_id 없음, video_url 형식 오류 등)"),
    })
    @PostMapping("/videos")
    public ResponseEntity<CommonResponse<CctvVideoCreateResponse>> createVideoAndEnqueue(@Valid @RequestBody CctvVideoCreateRequest request) {
        CctvVideoCreateResponse response = cctvService.createVideoAndEnqueue(request);
        return ResponseEntity.accepted().body(CommonResponse.success(response));
    }

    @Operation(
            summary = "기존 영상 재큐잉",
            description = """
            이미 등록된 CCTV 영상을 다시 분석 큐에 등록합니다.
            
            분석 실패(FAILED) 후 재시도하거나, 우선순위 변경이 필요할 때 사용합니다.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "재큐잉 성공"),
            @ApiResponse(responseCode = "404", description = "영상을 찾을 수 없음"),
    })
    @PostMapping("/enqueue/{videoId}")
    public ResponseEntity<CommonResponse<CctvEnqueueResponse>> enqueueVideo(@PathVariable Long videoId) {
        CctvEnqueueResponse response = cctvService.enqueueVideo(videoId);
        return ResponseEntity.accepted().body(CommonResponse.success(response));
    }

    @Operation(
            summary = "CCTV 분석 결과 전체 조회",
            description = """
            관리자가 CCTV 분석 결과를 조회합니다.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공")
    })
    @GetMapping("/videos")
    public ResponseEntity<CommonResponse<List<GetCctvVideoResponse>>> getVideos() {
        List<GetCctvVideoResponse> response = cctvService.getCctvVideos();
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(
            summary = "CCTV 분석 물품 전체 조회",
            description = """
            관리자가 CCTV 분석 물품 리스트를 조회합니다.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공")
    })
    @GetMapping("/detections")
    public ResponseEntity<CommonResponse<List<GetAllDetectionResponse>>> getAllDetection() {
        List<GetAllDetectionResponse> response = cctvService.getAllCctvDetection();
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(
            summary = "CCTV 분석 물품 상세 조회",
            description = """
            관리자가 CCTV 분석 물품을 상세 조회합니다.
            embedding은 조회하지 않습니다.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "물품을 찾을 수 없음")
    })
    @GetMapping("/detections/{id}")
    public ResponseEntity<CommonResponse<GetDetectionByIdResponse>> getDetection(@PathVariable Long id) {
        GetDetectionByIdResponse response = cctvService.getCctvDetectionById(id);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
