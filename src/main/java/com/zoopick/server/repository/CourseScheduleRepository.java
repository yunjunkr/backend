package com.zoopick.server.repository;

import com.zoopick.server.entity.Course;
import com.zoopick.server.entity.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {
    List<CourseSchedule> findAllByCourseIn(List<Course> courses);

    List<CourseSchedule> findAllByCourseIdIn(List<Long> courseIds);
}
