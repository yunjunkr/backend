package com.zoopick.server.dto.timetable;

public record TimetableGroupResponse(
    Long timetableId,
    String name,
    Integer year,
    Integer semester,
    Boolean isPrimary
) {}
