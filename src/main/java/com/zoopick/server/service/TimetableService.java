package com.zoopick.server.service;

import com.zoopick.server.dto.timetable.*;
import com.zoopick.server.entity.*;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.exception.DataNotFoundException;
import com.zoopick.server.repository.*;
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
    private final CourseScheduleRepository courseScheduleRepository;

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

    public List<TimetableCourseRecord> getTimetableDetails(String email, Long timetableId) {
        User user = userRepository.findBySchoolEmailOrThrow(email);
        TimetableGroup group = groupRepository.findByIdAndUser(timetableId, user)
                .orElseThrow(() -> DataNotFoundException.from("시간표", timetableId));

        List<Timetable> timetables = timetableRepository.findAllByTimetableGroup(group);
        List<Course> courses = timetables.stream()
                .map(Timetable::getCourse)
                .toList();
        List<CourseSchedule> courseSchedules = courseScheduleRepository.findAllByCourseIn(courses);
        return timetableRepository.findAllByTimetableGroup(group).stream()
                .map(t -> {
                    Course c = t.getCourse();
                    Room r = c.getRoom();
                    Building b = r.getBuilding();
                    return new TimetableCourseRecord(
                            c.getId(), c.getCourseName(),
                            r.getName(), b.getName(), b.getCode(), t.getColor(),
                            collectCourseScheduleRecord(c, courseSchedules)
                    );
                })
                .collect(Collectors.toList());
    }

    private List<TimetableCourseScheduleRecord> collectCourseScheduleRecord(Course course, List<CourseSchedule> allCourseSchedules) {
        return allCourseSchedules.stream()
                .filter(schedule -> schedule.getCourse().getId().equals(course.getId()))
                .map(schedule -> new TimetableCourseScheduleRecord(
                        schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime()
                ))
                .toList();
    }

    private List<CourseSchedule> filterCourseSchedules(Course course, List<CourseSchedule> allCourseSchedules) {
        return allCourseSchedules.stream()
                .filter(schedule -> schedule.getCourse().getId().equals(course.getId()))
                .toList();
    }

    @Transactional
    public void syncTimetable(String email, Long timetableId, TimetableSyncRequest request) {
        User user = userRepository.findBySchoolEmailOrThrow(email);
        TimetableGroup group = groupRepository.findByIdAndUser(timetableId, user)
                .orElseThrow(() -> DataNotFoundException.from("시간표", timetableId));

        // 1. 기존 내역 삭제 후 즉시 플러시하여 DB에 반영
        timetableRepository.deleteAllByTimetableGroup(group);
        timetableRepository.flush();

        // 2. 새로운 강의 데이터 로드 및 중복 검증
        List<Timetable> newTimetables = new ArrayList<>();
        List<Long> courseIds = request.courses().stream()
                .map(TimetableSyncRequest.CourseColorRequest::courseId)
                .toList();
        List<CourseSchedule> schedules = courseScheduleRepository.findAllByCourseIdIn(courseIds);
        for (var req : request.courses()) {
            Course course = courseRepository.findById(req.courseId())
                    .orElseThrow(() -> DataNotFoundException.from("강의", req.courseId()));

            // 시간 중복 체크
            validateOverlap(newTimetables, course, schedules);

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

    public Page<TimetableCourseRecord> searchCourses(Integer year, Integer semester, String keyword, Pageable pageable) {
        Page<Course> courses = courseRepository.searchCourses(year, semester, keyword, pageable);
        List<CourseSchedule> schedules = courseScheduleRepository.findAllByCourseIn(courses.getContent());

        return courseRepository.searchCourses(year, semester, keyword, pageable)
                .map(c -> {
                    Room r = c.getRoom();
                    Building b = r.getBuilding();
                    return new TimetableCourseRecord(
                            c.getId(), c.getCourseName(),
                            r.getName(), b.getName(), b.getCode(), null,
                            collectCourseScheduleRecord(c, schedules)
                    );
                });
    }

    public void validateOverlap(List<Timetable> existing, Course target, List<CourseSchedule> allCourseSchedules) {
        for (Timetable t : existing) {
            Course c = t.getCourse();
            List<CourseSchedule> comparingSchedules = filterCourseSchedules(c, allCourseSchedules);
            List<CourseSchedule> targetSchedules = filterCourseSchedules(target, allCourseSchedules);
            if (hasOverlap(comparingSchedules, targetSchedules)) {
                String message = String.format("강의 시간이 겹칩니다: %s & %s", c.getCourseName(), target.getCourseName());
                throw new BadRequestException(message, message);
            }
        }
    }

    private boolean hasOverlap(List<CourseSchedule> schedules1, List<CourseSchedule> schedules2) {
        for (CourseSchedule current : schedules1) {
            for (CourseSchedule comparing : schedules2) {
                if (current.hasCollisionWith(comparing))
                    return true;
            }
        }
        return false;
    }
}
