package com.zoopick.server.service;

import com.zoopick.server.dto.timetable.CreateTimetableRequest;
import com.zoopick.server.dto.timetable.TimetableCourseRecord;
import com.zoopick.server.dto.timetable.TimetableGroupResponse;
import com.zoopick.server.dto.timetable.TimetableSyncRequest;
import com.zoopick.server.entity.*;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.exception.DataNotFoundException;
import com.zoopick.server.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @InjectMocks
    private TimetableService timetableService;

    @Mock private TimetableGroupRepository groupRepository;
    @Mock private TimetableRepository timetableRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseScheduleRepository courseScheduleRepository;

    private User user;
    private TimetableGroup group;
    private Course course1;
    private Course course2;

    private final String EMAIL = "test@mju.ac.kr";

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).schoolEmail(EMAIL).build();

        group = TimetableGroup.builder()
                .id(10L)
                .user(user)
                .name("1학기 시간표")
                .year(2026)
                .semester(1)
                .isPrimary(true)
                .build();

        Building building = Building.builder().code("S1").name("함박관").build();
        Room room = Room.builder().name("S1111").building(building).build();

        course1 = Course.builder().id(101L).courseName("글쓰기").room(room).build();
        course2 = Course.builder().id(102L).courseName("영어1").room(room).build();
    }

    @Test
    @DisplayName("특정 학기 시간표 목록 조회 - 성공")
    void getTimetableGroups_Success() {
        // given
        when(userRepository.findBySchoolEmailOrThrow(EMAIL)).thenReturn(user);
        when(groupRepository.findAllByUserAndYearAndSemester(user, 2026, 1)).thenReturn(List.of(group));

        // when
        List<TimetableGroupResponse> responses = timetableService.getTimetableGroups(EMAIL, 2026, 1);

        // then
        assertEquals(1, responses.size());
        assertEquals("1학기 시간표", responses.get(0).name());
    }

    @Test
    @DisplayName("내 모든 시간표 목록 조회 - 성공")
    void getMyTimetableGroups_Success() {
        // given
        when(userRepository.findBySchoolEmailOrThrow(EMAIL)).thenReturn(user);
        when(groupRepository.findAllByUserOrderByYearDescSemesterDesc(user)).thenReturn(List.of(group));

        // when
        List<TimetableGroupResponse> responses = timetableService.getMyTimetableGroups(EMAIL);

        // then
        assertEquals(1, responses.size());
        assertEquals(2026, responses.get(0).year());
    }

    @Test
    @DisplayName("시간표 생성 - 해당 학기의 첫 시간표라면 Primary로 설정됨")
    void createTimetable_FirstGroup_BecomesPrimary() {
        // given
        CreateTimetableRequest request = new CreateTimetableRequest("내 시간표", 2026, 1);

        when(userRepository.findBySchoolEmailOrThrow(anyString())).thenReturn(user);

        when(groupRepository.findAllByUserOrderByYearDescSemesterDesc(any())).thenReturn(Collections.emptyList());
        when(groupRepository.save(any(TimetableGroup.class))).thenAnswer(i -> i.getArgument(0));

        // when
        TimetableGroupResponse response = timetableService.createTimetable(EMAIL, request);

        // then
        assertTrue(response.isPrimary());
    }

    @Test
    @DisplayName("시간표 생성 - 이미 시간표가 존재하면 Primary가 아님")
    void createTimetable_AdditionalGroup_NotPrimary() {
        // given
        CreateTimetableRequest request = new CreateTimetableRequest("서브 시간표", 2026, 1);

        when(userRepository.findBySchoolEmailOrThrow(anyString())).thenReturn(user);

        when(groupRepository.findAllByUserOrderByYearDescSemesterDesc(any())).thenReturn(List.of(group));
        when(groupRepository.save(any(TimetableGroup.class))).thenAnswer(i -> i.getArgument(0));

        // when
        TimetableGroupResponse response = timetableService.createTimetable(EMAIL, request);

        // then
        assertFalse(response.isPrimary());
    }

    @Test
    @DisplayName("기본 시간표 조회 - 없을 경우 null 반환")
    void getPrimaryTimetableGroup_NotFound() {
        // given
        when(userRepository.findBySchoolEmailOrThrow(anyString())).thenReturn(user);
        when(groupRepository.findByUserAndIsPrimaryTrue(any())).thenReturn(Optional.empty());

        // when
        TimetableGroupResponse response = timetableService.getPrimaryTimetableGroup(EMAIL);

        // then
        assertNull(response);
    }
    @Test
    @DisplayName("시간표 상세 조회 - 성공")
    void getTimetableDetails_Success() {
        // given
        Timetable t1 = Timetable.builder().course(course1).color("#FFF").build();
        CourseSchedule schedule1 = mock(CourseSchedule.class);
        when(schedule1.getCourse()).thenReturn(course1);

        when(userRepository.findBySchoolEmailOrThrow(EMAIL)).thenReturn(user);
        when(groupRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(group));
        when(timetableRepository.findAllByTimetableGroup(group)).thenReturn(List.of(t1));

        when(courseScheduleRepository.findAllByCourseIn(anyList())).thenReturn(List.of(schedule1));

        // when
        List<TimetableCourseRecord> details = timetableService.getTimetableDetails(EMAIL, 10L);

        // then
        assertEquals(1, details.size());
        assertEquals(course1.getId(), details.get(0).getCourseId());
        assertEquals("함박관", details.get(0).getBuildingName());
    }

    @Test
    @DisplayName("시간표 동기화 - 성공 (시간표 덮어쓰기 및 중복 없음)")
    void syncTimetable_Success() {
        // given
        TimetableSyncRequest.CourseColorRequest colorReq1 = new TimetableSyncRequest.CourseColorRequest(101L, "#FFFFFF");
        TimetableSyncRequest.CourseColorRequest colorReq2 = new TimetableSyncRequest.CourseColorRequest(102L, "#000000");
        TimetableSyncRequest request = new TimetableSyncRequest(List.of(colorReq1, colorReq2));

        when(userRepository.findBySchoolEmailOrThrow(EMAIL)).thenReturn(user);
        when(groupRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(group));
        when(courseRepository.findAllById(anyList())).thenReturn(List.of(course1, course2));

        CourseSchedule schedule1 = mock(CourseSchedule.class);
        when(schedule1.getCourse()).thenReturn(course1);
        CourseSchedule schedule2 = mock(CourseSchedule.class);
        when(schedule2.getCourse()).thenReturn(course2);

        lenient().when(schedule1.hasCollisionWith(any(CourseSchedule.class))).thenReturn(false);
        lenient().when(schedule2.hasCollisionWith(any(CourseSchedule.class))).thenReturn(false);

        when(courseScheduleRepository.findAllByCourseIdIn(anyList())).thenReturn(List.of(schedule1, schedule2));

        // when
        timetableService.syncTimetable(EMAIL, 10L, request);

        // then
        verify(timetableRepository, times(1)).deleteAllByTimetableGroup(group);
        verify(timetableRepository, times(1)).flush();
        verify(timetableRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("시간표 동기화 - 강의 시간 중복 시 예외 발생")
    void syncTimetable_Fail_Overlap() {
        // given
        TimetableSyncRequest.CourseColorRequest colorReq1 = new TimetableSyncRequest.CourseColorRequest(101L, "#FFFFFF");
        TimetableSyncRequest.CourseColorRequest colorReq2 = new TimetableSyncRequest.CourseColorRequest(102L, "#000000");
        TimetableSyncRequest request = new TimetableSyncRequest(List.of(colorReq1, colorReq2));

        when(userRepository.findBySchoolEmailOrThrow(EMAIL)).thenReturn(user);
        when(groupRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(group));
        when(courseRepository.findAllById(anyList())).thenReturn(List.of(course1, course2));

        CourseSchedule schedule1 = mock(CourseSchedule.class);
        when(schedule1.getCourse()).thenReturn(course1);
        CourseSchedule schedule2 = mock(CourseSchedule.class);
        when(schedule2.getCourse()).thenReturn(course2);

        when(schedule1.hasCollisionWith(any(CourseSchedule.class))).thenReturn(true);

        when(courseScheduleRepository.findAllByCourseIdIn(anyList())).thenReturn(List.of(schedule1, schedule2));

        // when & then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> timetableService.syncTimetable(EMAIL, 10L, request));
        assertTrue(exception.getMessage().contains("강의 시간이 겹칩니다"));

        verify(timetableRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("시간표 삭제 - 성공")
    void deleteTimetableGroup_Success() {
        // given
        when(userRepository.findBySchoolEmailOrThrow(EMAIL)).thenReturn(user);
        when(groupRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(group));

        // when
        timetableService.deleteTimetableGroup(EMAIL, 10L);

        // then
        verify(timetableRepository, times(1)).deleteAllByTimetableGroup(group);
        verify(groupRepository, times(1)).delete(group);
    }

    @Test
    @DisplayName("강의 검색 (페이지네이션) - 성공")
    void searchCourses_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Course> coursePage = new PageImpl<>(List.of(course1, course2), pageable, 2);

        CourseSchedule schedule1 = mock(CourseSchedule.class);
        when(schedule1.getCourse()).thenReturn(course1);

        when(courseRepository.searchCourses(2026, 1, "데이터", pageable)).thenReturn(coursePage);
        when(courseScheduleRepository.findAllByCourseIn(anyList())).thenReturn(List.of(schedule1));

        // when
        Page<TimetableCourseRecord> result = timetableService.searchCourses(2026, 1, "데이터", pageable);

        // then
        assertEquals(2, result.getContent().size());
        assertEquals(course1.getId(), result.getContent().get(0).getCourseId());
        assertEquals("함박관", result.getContent().get(0).getBuildingName());
    }
}