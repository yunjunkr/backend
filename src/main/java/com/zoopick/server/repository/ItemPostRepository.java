package com.zoopick.server.repository;

import com.zoopick.server.dto.item.ItemPostFilter;
import com.zoopick.server.entity.*;
import com.zoopick.server.exception.DataNotFoundException;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemPostRepository extends JpaRepository<ItemPost, Long>, JpaSpecificationExecutor<ItemPost> {
    static Specification<ItemPost> hasStatus(ItemStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null)
                return null;
            Join<Object, Object> itemJoin = root.join("item");
            return criteriaBuilder.equal(itemJoin.get("status"), status);
        };
    }

    static Specification<ItemPost> hasCategory(ItemCategory category) {
        return (root, query, criteriaBuilder) -> {
            if (category == null)
                return null;
            Join<Object, Object> itemJoin = root.join("item");
            return criteriaBuilder.equal(itemJoin.get("category"), category);
        };
    }

    static Specification<ItemPost> hasColor(ItemColor color) {
        return (root, query, criteriaBuilder) -> {
            if (color == null)
                return null;
            Join<Object, Object> itemJoin = root.join("item");
            return criteriaBuilder.equal(itemJoin.get("color"), color);
        };
    }

    static Specification<ItemPost> applyFilter(ItemPostFilter filter) {
        if (filter == null)
            return Specification.unrestricted();
        return Specification.where(hasStatus(filter.getStatus()))
                .and(hasCategory(filter.getCategory()))
                .and(hasColor(filter.getColor()));
    }

    default ItemPost findByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> DataNotFoundException.from("게시물", id));
    }

    long countByUserId(Long userId);
    ItemPost findByItem(Item item);
}
