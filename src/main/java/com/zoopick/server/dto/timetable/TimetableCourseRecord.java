package com.zoopick.server.dto.timetable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class TimetableCourseRecord {
    private Long courseId;
    private String courseName;
    private String roomName;
    private String buildingName;
    private String buildingCode;
    private String color;
    private List<TimetableCourseScheduleRecord> schedules;
}
