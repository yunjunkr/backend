package com.zoopick.server.controller;

import com.zoopick.server.dto.CommonResponse;
import com.zoopick.server.dto.timetable.CreateTimetableRequest;
import com.zoopick.server.dto.timetable.TimetableCourseResponse;
import com.zoopick.server.dto.timetable.TimetableGroupResponse;
import com.zoopick.server.dto.timetable.TimetableSyncRequest;
import com.zoopick.server.security.UserPrincipal;
import com.zoopick.server.service.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Timetable API", description = "시간표 관리 및 강의 조회 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TimetableController {
    private final TimetableService timetableService;

    @Operation(summary = "내 시간표 목록 조회", description = "특정 연도/학기의 시간표 그룹 목록을 가져옵니다.")
    @GetMapping("/timetables")
    public CommonResponse<List<TimetableGroupResponse>> getTimetableGroups(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Integer year,
            @RequestParam Integer semester) {
        return CommonResponse.success(timetableService.getTimetableGroups(principal.email(), year, semester));
    }

    @Operation(summary = "새 시간표 만들기", description = "특정 연도/학기에 새로운 시간표 그룹을 생성합니다.")
    @PostMapping("/timetables")
    public CommonResponse<TimetableGroupResponse> createTimetable(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTimetableRequest request) {
        return CommonResponse.success(timetableService.createTimetable(principal.email(), request));
    }

    @Operation(summary = "시간표 상세 조회", description = "특정 시간표의 상세 강의 목록을 가져옵니다.")
    @GetMapping("/timetables/{id}")
    public CommonResponse<List<TimetableCourseResponse>> getTimetableDetails(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return CommonResponse.success(timetableService.getTimetableDetails(principal.email(), id));
    }

    @Operation(summary = "시간표 동기화", description = "현재 시간표의 강의 목록과 색상을 일괄 저장합니다.")
    @PostMapping("/timetables/{id}/sync")
    public CommonResponse<String> syncTimetable(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody TimetableSyncRequest request) {
        timetableService.syncTimetable(principal.email(), id, request);
        return CommonResponse.success("Successfully synced");
    }

    @Operation(summary = "강의 검색", description = "시스템에 등록된 전체 강의 마스터 데이터를 검색합니다.")
    @GetMapping("/courses")
    public CommonResponse<Page<TimetableCourseResponse>> searchCourses(
            @RequestParam Integer year,
            @RequestParam Integer semester,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return CommonResponse.success(timetableService.searchCourses(year, semester, keyword, pageable));
    }
}
