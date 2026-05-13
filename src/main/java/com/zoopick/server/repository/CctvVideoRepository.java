package com.zoopick.server.repository;

import com.zoopick.server.dto.cctv.GetCctvVideoResponse;
import com.zoopick.server.entity.CctvVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CctvVideoRepository extends JpaRepository<CctvVideo, Long> {
    @Query("""
    SELECT new com.zoopick.server.dto.cctv.GetCctvVideoResponse(
        v.id, 
        v.room.id, 
        v.recordedAt, 
        v.videoUrl, 
        p.status, 
        v.durationSeconds, 
        p.analyzedSeconds, 
        p.estimatedCompletionAt, 
        p.startedAt, 
        p.lastUpdatedAt
    )
    FROM CctvVideo v
    LEFT JOIN CctvVideoProgress p ON v.id = p.cctvVideo.id
    ORDER BY v.id DESC
""")
    List<GetCctvVideoResponse> findAllCctvVideosWithProgress();
}
