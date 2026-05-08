package com.zoopick.server.repository;

import com.zoopick.server.entity.TimetableGroup;
import com.zoopick.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimetableGroupRepository extends JpaRepository<TimetableGroup, Long> {
    List<TimetableGroup> findAllByUserAndYearAndSemester(User user, Integer year, Integer semester);
    List<TimetableGroup> findAllByUserOrderByYearDescSemesterDesc(User user);
    Optional<TimetableGroup> findByIdAndUser(Long id, User user);
    Optional<TimetableGroup> findByUserAndIsPrimaryTrue(User user);
}
