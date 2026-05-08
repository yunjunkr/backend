package com.zoopick.server.dto.timetable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTimetableRequest(
    @NotBlank(message = "시간표 이름을 입력해주세요.")
    String name,
    
    @NotNull(message = "연도를 입력해주세요.")
    Integer year,
    
    @NotNull(message = "학기를 입력해주세요.")
    Integer semester
) {}
