package com.zoopick.server.repository;

import com.zoopick.server.dto.match.ItemMatchResultResponse;
import com.zoopick.server.dto.match.SimilarItemProjection;
import com.zoopick.server.entity.Item;
import com.zoopick.server.entity.ItemMatch;
import com.zoopick.server.entity.MatchStatus;
import com.zoopick.server.exception.DataNotFoundException;
import org.springframework.data.domain.Vector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemMatchRepository extends JpaRepository<ItemMatch, Long> {


    default ItemMatch findByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> DataNotFoundException.from("매칭", id));
    }

    @Query(value = """
    SELECT *
    FROM (
        SELECT
            i.id AS itemId,
            1 - (i.embedding <=> CAST(:embedding AS vector)) AS score
        FROM zoopick.items i
        WHERE i.type <> CAST(:excludeType AS item_type)
          AND i.reporter_id <> :reporterId
          AND i.returned_at IS NULL
          AND i.status IN ('IN_LOCKER', 'REPORTED')
          AND i.category = CAST(:category AS item_category)
        ORDER BY i.embedding <=> CAST(:embedding AS vector)
        LIMIT 100
    ) t
    WHERE t.score >= :threshold
        LIMIT 30
    """, nativeQuery = true)
    List<SimilarItemProjection> findSimilarItems(@Param("embedding") Vector embedding,
                                                 @Param("excludeType") String excludeType,
                                                 @Param("category") String category,
                                                 @Param("reporterId") Long reporterId,
                                                 @Param("threshold") float threshold);

    // 중복 체크
    boolean existsByLostItemAndFoundItem(Item lostItem, Item foundItem);

    @Query("""
    SELECT new com.zoopick.server.dto.match.ItemMatchResultResponse(
        m.id, 
        f.id, 
        p.id, 
        p.title, 
        f.imageUrl, 
        f.locationName, 
        u.nickname, 
        u.department, 
        m.score, 
        m.status,
        f.reporter.id
    )
    FROM ItemMatch m
    JOIN m.lostItem i
    JOIN m.foundItem f
    JOIN ItemPost p ON f.id = p.item.id
    JOIN f.reporter u
    WHERE CAST(i.type AS string) = 'LOST'
      AND CAST(m.status AS string) IN ('CANDIDATE', 'NOTIFIED')
      AND i.reporter.id = :userId
      AND f.reporter.id <> :userId
    ORDER BY m.score DESC
""")
    List<ItemMatchResultResponse> findMatchResultsByUserId(@Param("userId") Long userId);

    // 매칭 컨펌된 것 제외 모든 물품을 다 rejected
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    UPDATE item_matches SET status = 'REJECTED'
    WHERE (lost_item_id = :lostItemId OR found_item_id = :foundItemId)
    AND id != :matchId
    AND status IN ('CANDIDATE', 'NOTIFIED')
""", nativeQuery = true)
    int rejectOthersByLostItem(@Param("matchId") Long matchId,
                                @Param("lostItemId") Long lostItemId,
                                @Param("foundItemId") Long foundItemId);

    // 알림 발송 후 매칭 상태를 NOTIFIED로 일괄 갱신
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ItemMatch m SET m.status = :status WHERE m.id IN :ids")
    void updateStatusInBatch(@Param("ids") List<Long> ids, @Param("status") MatchStatus status);

    // CONFIRMED된 매칭이 있는지 확인
    boolean existsByLostItemAndStatus(Item lostItem, MatchStatus status);

    boolean existsByFoundItemAndStatus(Item foundItem, MatchStatus status);

    boolean existsByFoundItemAndLostItem_Reporter_IdAndStatus(Item foundItem, Long reporterId, MatchStatus status);

    boolean existsByLostItemAndFoundItem_Reporter_IdAndStatus(Item lostItem, Long reporterId, MatchStatus status);
}
