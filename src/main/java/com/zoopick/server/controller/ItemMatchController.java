package com.zoopick.server.controller;

import com.zoopick.server.dto.CommonResponse;
import com.zoopick.server.dto.match.ItemMatchResultResponse;
import com.zoopick.server.dto.match.MatchManualRequest;
import com.zoopick.server.dto.match.MatchManualResponse;
import com.zoopick.server.security.UserPrincipal;
import com.zoopick.server.service.ItemMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Item Matching API", description = "아이템 매칭용 API")
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor

public class ItemMatchController {
    private final ItemMatchService itemMatchService;

    @Operation(summary = "매칭 조회", description = "유저의 잃어버린 아이템과 매칭된 아이템을 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매칭 조회 성공"),
            @ApiResponse(responseCode = "404", description = "매칭을 찾을 수 없음")
    })
    @GetMapping("/me")
    public CommonResponse<List<ItemMatchResultResponse>> itemMatchResult(
            @Parameter(description = "조회할 유저")
            @AuthenticationPrincipal UserPrincipal principal) {
        return CommonResponse.success(itemMatchService.getItemMatchResult(principal.id()));
    }

    @Operation(summary = "매칭 성사", description = "유저가 아이템의 매칭을 성사시킵니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매칭 성공"),
            @ApiResponse(responseCode = "404", description = "매칭을 찾을 수 없음")
    })
    @PostMapping("/{matchId}/confirm")
    public CommonResponse<Long> itemMatchConfirm(
            @Parameter(description = "매치 id")
            @PathVariable Long matchId) {
        itemMatchService.confirmMatch(matchId);
        return CommonResponse.success(matchId);
    }

    @Operation(summary = "매칭 거절", description = "유저가 아이템의 매칭을 거절합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매칭 거절"),
            @ApiResponse(responseCode = "404", description = "매칭을 찾을 수 없음")
    })
    @PostMapping("/{matchId}/reject")
    public CommonResponse<Long> itemMatchReject(
            @Parameter(description = "매치 id")
            @PathVariable Long matchId) {
        itemMatchService.rejectMatch(matchId);
        return CommonResponse.success(matchId);
    }
    
    @Operation(summary = "매칭 수동 적용", description = "유저가 아이템의 매칭을 수동 성사시킵니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매칭 성공"),
            @ApiResponse(responseCode = "404", description = "매칭을 찾을 수 없음")
    })
    @PostMapping("/manual")
    public CommonResponse<MatchManualResponse> itemMatchManual(@RequestBody MatchManualRequest request) {
        MatchManualResponse matchManualResponse = itemMatchService.matchManual(request);
        return CommonResponse.success(matchManualResponse);
    }
}
