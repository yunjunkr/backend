package com.zoopick.server.service;

import com.zoopick.server.dto.timetable.CreateTimetableRequest;
import com.zoopick.server.dto.timetable.TimetableCourseResponse;
import com.zoopick.server.dto.timetable.TimetableGroupResponse;
import com.zoopick.server.dto.timetable.TimetableSyncRequest;
import com.zoopick.server.entity.*;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.exception.DataNotFoundException;
import com.zoopick.server.repository.CourseRepository;
import com.zoopick.server.repository.TimetableGroupRepository;
import com.zoopick.server.repository.TimetableRepository;
import com.zoopick.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableService {
    private final TimetableGroupRepository groupRepository;
    private final TimetableRepository timetableRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public List<TimetableGroupResponse> getTimetableGroups(String email, Integer year, Integer semester) {
        User user = userRepository.findBySchoolEmailOrThrow(email);
        return groupRepository.findAllByUserAndYearAndSemester(user, year, semester).stream()
                .map(g -> new TimetableGroupResponse(g.getId(), g.getName(), g.getYear(), g.getSemester(), g.getIsPrimary()))
                .collect(Collectors.toList());
    }

    public List<TimetableGroupResponse> getMyTimetableGroups(String email) {
        User user = userRepository.findBySchoolEmailOrThrow(email);
        return groupRepository.findAllByUserOrderByYearDescSemesterDesc(user).stream()
                .map(g -> new TimetableGroupResponse(g.getId(), g.getName(), g.getYear(), g.getSemester(), g.getIsPrimary()))
                .collect(Collectors.toList());
    }

    public TimetableGroupResponse getPrimaryTimetableGroup(String email) {
        User user = userRepository.findBySchoolEmailOrThrow(email);
        TimetableGroup group = groupRepository.findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> new DataNotFoundException("기본 시간표가 설정되지 않았습니다.", "PRIMARY_TIMETABLE_NOT_FOUND"));
        return new TimetableGroupResponse(group.getId(), group.getName(), group.getYear(), group.getSemester(), group.getIsPrimary());
    }

    @Transactional
    public TimetableGroupResponse createTimetable(String email, CreateTimetableRequest request) {
        User user = userRepository.findBySchoolEmailOrThrow(email);
        
        // 해당 학기의 첫 시간표라면 기본(primary)으로 설정
        boolean isFirst = groupRepository.findAllByUserAndYearAndSemester(user, request.year(), request.semester()).isEmpty();

        TimetableGroup group = TimetableGroup.builder()
                .user(user)
                .name(request.name())
                .year(request.year())
                .semester(request.semester())
                .isPrimary(isFirst)
                .build();
                
        TimetableGroup saved = groupRepository.save(group);
        return new TimetableGroupResponse(saved.getId(), saved.getName(), saved.getYear(), saved.getSemester(), saved.getIsPrimary());
    }

    public List<TimetableCourseResponse> getTimetableDetails(String email, Long timetableId) {
        User user = userRepository.findBySchoolEmailOrThrow(email);
        TimetableGroup group = groupRepository.findByIdAndUser(timetableId, user)
                .orElseThrow(() -> DataNotFoundException.from("시간표", timetableId));

        return timetableRepository.findAllByTimetableGroup(group).stream()
                .map(t -> {
                    Course c = t.getCourse();
                    Room r = c.getRoom();
                    Building b = r.getBuilding();
                    return new TimetableCourseResponse(
                            c.getId(), c.getCourseName(), c.getDayOfWeek(),
                            c.getStartTime(), c.getEndTime(),
                            r.getName(), b.getName(), b.getCode(), t.getColor()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void syncTimetable(String email, Long timetableId, TimetableSyncRequest request) {
        User user = userRepository.findBySchoolEmailOrThrow(email);
        TimetableGroup group = groupRepository.findByIdAndUser(timetableId, user)
                .orElseThrow(() -> DataNotFoundException.from("시간표", timetableId));

        // 1. 기존 내역 삭제
        timetableRepository.deleteAllByTimetableGroup(group);

        // 2. 새로운 강의 데이터 로드 및 중복 검증
        List<Timetable> newTimetables = new ArrayList<>();
        for (var req : request.courses()) {
            Course course = courseRepository.findById(req.courseId())
                    .orElseThrow(() -> DataNotFoundException.from("강의", req.courseId()));
            
            // 시간 중복 체크
            validateOverlap(newTimetables, course);

            newTimetables.add(Timetable.builder()
                    .course(course)
                    .timetableGroup(group)
                    .color(req.color())
                    .build());
        }

        timetableRepository.saveAll(newTimetables);
    }

    @Transactional
    public void deleteTimetableGroup(String email, Long timetableId) {
        User user = userRepository.findBySchoolEmailOrThrow(email);
        TimetableGroup group = groupRepository.findByIdAndUser(timetableId, user)
                .orElseThrow(() -> DataNotFoundException.from("시간표", timetableId));

        timetableRepository.deleteAllByTimetableGroup(group);
        groupRepository.delete(group);
    }

    public Page<TimetableCourseResponse> searchCourses(Integer year, Integer semester, String keyword, Pageable pageable) {
        return courseRepository.searchCourses(year, semester, keyword, pageable)
                .map(c -> {
                    Room r = c.getRoom();
                    Building b = r.getBuilding();
                    return new TimetableCourseResponse(
                            c.getId(), c.getCourseName(), c.getDayOfWeek(),
                            c.getStartTime(), c.getEndTime(),
                            r.getName(), b.getName(), b.getCode(), null
                    );
                });
    }

    private void validateOverlap(List<Timetable> existing, Course target) {
        for (Timetable t : existing) {
            Course c = t.getCourse();
            if (c.getDayOfWeek() == target.getDayOfWeek()) {
                // (StartA < EndB) && (EndA > StartB) 이면 겹침
                if (target.getStartTime().isBefore(c.getEndTime()) && 
                    target.getEndTime().isAfter(c.getStartTime())) {
                    String message = String.format("강의 시간이 겹칩니다: %s & %s", c.getCourseName(), target.getCourseName());
                    throw new BadRequestException(message, message);
                }
            }
        }
    }
}
