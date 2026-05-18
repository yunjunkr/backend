package com.zoopick.server.service;

import com.zoopick.server.dto.item.*;
import com.zoopick.server.entity.*;
import com.zoopick.server.mapper.ItemPostMapper;
import com.zoopick.server.repository.BuildingRepository;
import com.zoopick.server.repository.ItemPostRepository;
import com.zoopick.server.repository.ItemRepository;
import com.zoopick.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemPostServiceTest {

    @InjectMocks
    private ItemPostService itemPostService;

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemPostRepository itemPostRepository;
    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private ItemPostMapper itemPostMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private User user;
    private Building building;
    private Item item;
    private ItemPost itemPost;

    @BeforeEach
    void setUp() {
        // 공통적으로 사용될 테스트 데이터 세팅
        user = User.builder().id(1L).nickname("테스트유저").build();
        building = Building.builder().id(10L).name("명진당").build();

        item = Item.builder()
                .id(100L)
                .reporter(user)
                //.type(ItemType.FOUND) // 실제 Entity에 정의된 Enum 활용
                .status(ItemStatus.REPORTED)
                .reportedBuilding(building)
                .build();

        itemPost = ItemPost.builder()
                .id(1000L)
                .title("지갑 찾습니다")
                .description("검은색 가죽 지갑입니다.")
                .user(user)
                .item(item)
                .build();
    }

    @Test
    @DisplayName("게시글 생성 - 정상 처리 및 이벤트 발행 검증")
    void createItemPost_success() {
        // given
        CreateItemPostRequest request = mock(CreateItemPostRequest.class);
        when(request.getBuildingId()).thenReturn(10L);
        when(request.getTitle()).thenReturn("지갑 찾습니다");
        when(request.getDescription()).thenReturn("검은색 가죽 지갑입니다.");
        when(request.getReportedAt()).thenReturn(LocalDateTime.now());

        when(userRepository.findByIdOrThrow(1L)).thenReturn(user);
        when(buildingRepository.findByIdOrThrow(10L)).thenReturn(building);

        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(itemPostRepository.save(any(ItemPost.class))).thenReturn(itemPost);

        // when
        CreateItemPostResult result = itemPostService.createItemPost(1L, request);

        // then
        assertNotNull(result);

        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);

        verify(itemRepository, times(1)).save(itemCaptor.capture());

        Item savedItem = itemCaptor.getValue();
        assertEquals(ItemStatus.REPORTED, savedItem.getStatus());

        // 나머지 검증
        verify(itemPostRepository, times(1)).save(any(ItemPost.class));
        verify(eventPublisher, times(1)).publishEvent(any(ItemCreatedEvent.class));
    }

    @Test
    @DisplayName("게시글 목록 조회 - 페이지네이션 적용 성공")
    void getItemPosts_success() {
        // given
        ItemPostFilter filter = mock(ItemPostFilter.class);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ItemPost> itemPostPage = new PageImpl<>(List.of(itemPost), pageable, 1);
        ItemPostRecord recordMock = mock(ItemPostRecord.class);

        // Specification 파라미터 매칭을 위해 any() 사용
        when(itemPostRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(itemPostPage);
        when(itemPostMapper.toItemPostRecord(any(ItemPost.class))).thenReturn(recordMock);

        // when
        ListItemPostResult result = itemPostService.getItemPosts(filter, pageable);

        // then
        assertNotNull(result);

        verify(itemPostRepository, times(1)).findAll(any(Specification.class), eq(pageable));
        verify(itemPostMapper, times(1)).toItemPostRecord(itemPost);
    }

    @Test
    @DisplayName("단일 게시글 조회 - 성공")
    void getItemPost_success() {
        // given
        ItemPostRecord recordMock = mock(ItemPostRecord.class);

        when(itemPostRepository.findByIdOrThrow(1000L)).thenReturn(itemPost);
        when(itemPostMapper.toItemPostRecord(itemPost)).thenReturn(recordMock);

        // when
        ItemPostRecord result = itemPostService.getItemPost(1000L);

        // then
        assertNotNull(result);
        assertEquals(recordMock, result);

        verify(itemPostRepository, times(1)).findByIdOrThrow(1000L);
        verify(itemPostMapper, times(1)).toItemPostRecord(itemPost);
    }
}