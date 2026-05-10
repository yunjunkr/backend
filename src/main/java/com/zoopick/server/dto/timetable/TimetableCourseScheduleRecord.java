package com.zoopick.server.dto.timetable;

import com.zoopick.server.entity.DayOfWeek;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
public class TimetableCourseScheduleRecord {
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}
