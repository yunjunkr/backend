package com.zoopick.server.repository;

import com.zoopick.server.dto.match.ItemMatchProjection;
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

    @Query(value = """
    SELECT 
    m.id as matchId,                 -- 매칭 ID
    f.id as foundItemId,             -- 매칭된 item ID
    p.id as foundPostId,             -- 매칭된 post ID
    p.title as foundPostTitle,       -- 매칭된 post 제목
    f.location_name  as locationName,-- 매칭된 post 장소
    f.image_url as foundImageUrl,    -- 매칭된 item 이미지 url
    u.nickname as foundNickname,     -- 찾은 사람 닉네임
    u.department as foundDepartment, -- 찾은 사람 과
    m.score as score,                -- 매칭 점수
    m.status as status               -- 현재 상태
    FROM items i
    JOIN item_matches m ON i.id = m.lost_item_id
    JOIN items f ON m.found_item_id = f.id
    JOIN item_posts p on f.id = p.item_id
    JOIN users u ON u.id = f.reporter_id
    WHERE i.type = 'LOST' -- 내가 잃어버린 것만
    AND m.status IN ('CANDIDATE', 'NOTIFIED') -- 아직 매칭되지 않은 것들
    AND i.reporter_id=:userId -- 내거만
    AND f.reporter_id<>:userId -- found한 아이템이 내거면 안됨
    ORDER BY m.score desc -- 내림차순으로 뽑음
    """, nativeQuery = true)
    List<ItemMatchProjection> itemMatchesByLostItem(@Param("userId") Long userId);

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

    // CONFIRMED된 매칭이 있는지 확인
    boolean existsByLostItemAndStatus(Item lostItem, MatchStatus status);

    boolean existsByFoundItemAndStatus(Item foundItem, MatchStatus status);
}
