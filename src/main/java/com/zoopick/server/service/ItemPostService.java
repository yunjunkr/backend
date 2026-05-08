package com.zoopick.server.service;

import com.zoopick.server.dto.item.*;
import com.zoopick.server.entity.*;
import com.zoopick.server.mapper.ItemPostMapper;
import com.zoopick.server.repository.BuildingRepository;
import com.zoopick.server.repository.ItemPostRepository;
import com.zoopick.server.repository.ItemRepository;
import com.zoopick.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@NullMarked
public class ItemPostService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemPostRepository itemPostRepository;
    private final BuildingRepository buildingRepository;
    private final ItemPostMapper itemPostMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CreateItemPostResult createItemPost(long userId, CreateItemPostRequest request) {
        User user = userRepository.findByIdOrThrow(userId);
        Building building = buildingRepository.findByIdOrThrow(request.getBuildingId());
        Item item = Item.builder()
                .reporter(user)
                .type(request.getType())
                .status(ItemStatus.REPORTED)
                .category(request.getCategory())
                .color(request.getColor())
                .embedding(null)
                .reportedBuilding(building)
                .locationName(request.getDetailAddress())
                .imageUrl(request.getImageUrl())
                .reportedAt(request.getReportedAt() != null ? request.getReportedAt() : LocalDateTime.now())
                .build();

        Item savedItem = itemRepository.save(item);
        eventPublisher.publishEvent(new ItemCreatedEvent(savedItem.getId()));

        ItemPost itemPost = ItemPost.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .user(user)
                .item(savedItem)
                .build();
        ItemPost savedItemPost = itemPostRepository.save(itemPost);
        return new CreateItemPostResult(savedItemPost.getId(), savedItem.getStatus(), "등록되었습니다.");
    }

    public ListItemPostResult getItemPosts(@Nullable ItemPostFilter filter, Pageable pageable) {
        Page<ItemPost> page = itemPostRepository.findAll(ItemPostRepository.applyFilter(filter), pageable);
        List<ItemPostRecord> itemPostRecords = page.stream().map(itemPostMapper::toItemPostRecord)
                .toList();

        return new ListItemPostResult(itemPostRecords, itemPostRecords.size(), page.getNumber());
    }

    public ItemPostRecord getItemPost(long id) {
        ItemPost itemPost = itemPostRepository.findByIdOrThrow(id);
        return itemPostMapper.toItemPostRecord(itemPost);
    }
}
