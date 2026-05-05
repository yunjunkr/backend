package com.zoopick.server.controller;

import com.zoopick.server.dto.vision.VisionAnalyzeRequest;
import com.zoopick.server.dto.vision.VisionAnalyzeResponse;
import com.zoopick.server.service.VisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vision")
@RequiredArgsConstructor
public class VisionController {
    private final VisionService visionService;

    @Operation(
            summary = "이미지 분석 — 카테고리, 색상, 임베딩 추출",
            description = """
            업로드된 이미지를 AI 서버(FastAPI)로 전달하여 다음 정보를 추출합니다.
            
            - **category**: 12개 카테고리 중 하나 (SMARTPHONE, BAG, WALLET 등)
            - **color**: 12개 색상 중 하나 (BLACK, WHITE, RED 등)
            - **embedding**: CLIP ViT-B/32 기반 512차원 시각 임베딩 벡터
            
            매칭 알고리즘이 이 정보를 사용해 LOST↔FOUND 유사도를 계산합니다.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "분석 성공"),
            @ApiResponse(responseCode = "400", description = "이미지 URL이 유효하지 않음"),
            @ApiResponse(responseCode = "502", description = "AI 서버 응답 오류"),
    })
    @PostMapping("/analyze")
    public VisionAnalyzeResponse analyzeImage(@RequestBody VisionAnalyzeRequest request) {
        return visionService.analyzeImage(request.getImageUrl());
    }
}
