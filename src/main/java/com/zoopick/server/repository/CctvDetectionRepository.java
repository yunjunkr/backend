package com.zoopick.server.repository;

import com.zoopick.server.entity.CctvDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CctvDetectionRepository extends JpaRepository<CctvDetection, Long> {
    List<CctvDetection> findAllByOrderByDetectedAtAsc();
}
