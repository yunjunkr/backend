package com.zoopick.server.service;

import com.zoopick.server.config.MatchConfig;
import com.zoopick.server.dto.match.*;
import com.zoopick.server.entity.*;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.repository.ItemMatchRepository;
import com.zoopick.server.repository.ItemRepository;
import com.zoopick.server.repository.LockerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Vector;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemMatchServiceTest {

    @InjectMocks
    private ItemMatchService itemMatchService;

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ItemMatchRepository itemMatchRepository;
    @Mock
    private LockerRepository lockerRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MatchConfig matchConfig;

    private User user;
    private Item lostItem;
    private Item foundItem;
    private ItemMatch itemMatch;

    @BeforeEach
    void setUp() {

        user = User.builder().id(1L).build();

        lostItem = Item.builder()
                .id(100L)
                .reporter(user)
                .type(ItemType.LOST)
                .category(ItemCategory.BAG)
                .color(ItemColor.BLACK)
                .embedding(new float[]{0.1f, 0.2f})
                .status(ItemStatus.REPORTED)
                .build();

        foundItem = Item.builder()
                .id(200L)
                .reporter(user)
                .type(ItemType.FOUND)
                .category(ItemCategory.BAG)
                .color(ItemColor.BLACK)
                .embedding(new float[]{0.1f, 0.2f})
                .status(ItemStatus.REPORTED)
                .build();

        itemMatch = ItemMatch.builder()
                .id(10L)
                .lostItem(lostItem)
                .foundItem(foundItem)
                .status(MatchStatus.CANDIDATE)
                .score(0.95f)
                .build();
    }

    @Test
    @DisplayName("자동 매칭 생성 - 성공 (이벤트 발행 포함)")
    void createMatch_success() {
        // given
        SimilarItemProjection projectionMock = mock(SimilarItemProjection.class);
        when(projectionMock.getItemId()).thenReturn(200L);
        when(projectionMock.getScore()).thenReturn(0.95);

        when(itemRepository.findByIdOrThrow(100L)).thenReturn(lostItem);

        when(itemMatchRepository.findSimilarItems(
                any(Vector.class),
                eq("LOST"),
                eq("BAG"),
                eq(1L),
                anyFloat()
        )).thenReturn(List.of(projectionMock));

        when(itemRepository.findAllById(anyList())).thenReturn(List.of(foundItem));
        when(itemMatchRepository.existsByLostItemAndFoundItem(lostItem, foundItem)).thenReturn(false);
        when(itemMatchRepository.save(any(ItemMatch.class))).thenReturn(itemMatch);

        // when
        itemMatchService.createMatch(100L);

        // then
        verify(itemMatchRepository, times(1)).save(any(ItemMatch.class));
        verify(eventPublisher, times(1)).publishEvent(any(CreateMatchEvent.class));
    }

    @Test
    @DisplayName("자동 매칭 생성 - 유사 아이템이 없는 경우 그대로 종료")
    void createMatch_noSimilarItems() {
        // given
        when(itemRepository.findByIdOrThrow(100L)).thenReturn(lostItem);

        when(itemMatchRepository.findSimilarItems(
                any(Vector.class),
                anyString(),
                anyString(),
                anyLong(),
                anyFloat()
        )).thenReturn(List.of());

        // when
        itemMatchService.createMatch(100L);

        // then
        verify(itemRepository, never()).findAllById(anyList());
        verify(itemMatchRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("매칭 결과 목록 조회 - 성공")
    void getItemMatchResult_success() {
        // given
        Long userId = 1L;

        ItemMatchResultResponse mockResponse = new ItemMatchResultResponse(
                10L,
                200L,
                1000L,
                "분실물 게시글 제목",
                "http://example.com/image.jpg",
                "도서관",
                "착한발견자",
                "컴퓨터공학과",
                0.95f,
                MatchStatus.CANDIDATE,
                2L
        );

        when(itemMatchRepository.findMatchResultsByUserId(userId)).thenReturn(List.of(mockResponse));

        // when
        List<ItemMatchResultResponse> responses = itemMatchService.getItemMatchResult(userId);

        // then
        assertFalse(responses.isEmpty());
        assertEquals(10L, responses.get(0).getMatchId());
        assertEquals(MatchStatus.CANDIDATE, responses.get(0).getStatus());
        assertEquals("분실물 게시글 제목", responses.get(0).getFoundPostTitle());
    }

    @Test
    @DisplayName("매칭 확정 - 성공")
    void confirmMatch_success() {
        // given
        when(itemMatchRepository.findByIdOrThrow(10L)).thenReturn(itemMatch);
        when(itemMatchRepository.existsByLostItemAndStatus(lostItem, MatchStatus.CONFIRMED)).thenReturn(false);
        when(itemMatchRepository.existsByFoundItemAndStatus(foundItem, MatchStatus.CONFIRMED)).thenReturn(false);

        // when
        itemMatchService.confirmMatch(10L, 1L);

        // then
        assertEquals(MatchStatus.CONFIRMED, itemMatch.getStatus());
        assertEquals(ItemStatus.MATCHED, lostItem.getStatus());
        assertEquals(ItemStatus.MATCHED, foundItem.getStatus());
        verify(itemMatchRepository, times(1)).rejectOthersByLostItem(10L, 100L, 200L);
    }

    @Test
    @DisplayName("매칭 확정 실패 - 이미 주인을 찾은 물품 존재")
    void confirmMatch_fail_alreadyConfirmed() {
        // given
        when(itemMatchRepository.findByIdOrThrow(10L)).thenReturn(itemMatch);
        when(itemMatchRepository.existsByLostItemAndStatus(lostItem, MatchStatus.CONFIRMED)).thenReturn(true);

        // when & then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> itemMatchService.confirmMatch(10L, 1L));
        assertEquals("이미 주인을 찾은 물품이 포함되어 있습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("매칭 거절 - 성공")
    void rejectMatch_success() {
        // given
        when(itemMatchRepository.findByIdOrThrow(10L)).thenReturn(itemMatch);

        // when
        itemMatchService.rejectMatch(10L, 1L);

        // then
        assertEquals(MatchStatus.REJECTED, itemMatch.getStatus());
    }

    @Test
    @DisplayName("수동 매칭 - 보관함(Locker) 아이템 성공")
    void matchManual_locker_success() {
        // given
        MatchManualRequest request = mock(MatchManualRequest.class);
        when(request.getLostItemId()).thenReturn(100L);
        when(request.getFoundItemId()).thenReturn(200L);

        foundItem.setStatus(ItemStatus.IN_LOCKER);
        Locker locker = Locker.builder().id(5L).build();

        when(itemRepository.findByIdOrThrow(100L)).thenReturn(lostItem);
        when(itemRepository.findByIdOrThrow(200L)).thenReturn(foundItem);

        when(itemMatchRepository.existsByLostItemAndFoundItem(lostItem, foundItem)).thenReturn(false);
        when(itemMatchRepository.existsByLostItemAndStatus(lostItem, MatchStatus.CONFIRMED)).thenReturn(false);
        when(itemMatchRepository.existsByFoundItemAndStatus(foundItem, MatchStatus.CONFIRMED)).thenReturn(false);

        when(itemMatchRepository.save(any(ItemMatch.class))).thenReturn(itemMatch);
        when(lockerRepository.findLockerByCurrentItem(foundItem)).thenReturn(locker);

        // when
        MatchManualResponse response = itemMatchService.matchManual(request);

        // then
        assertEquals(MatchManualType.LOCKER, response.getMatchManualType());
        assertEquals(5L, response.getLockerId());
        verify(itemMatchRepository, times(1)).rejectOthersByLostItem(itemMatch.getId(), 100L, 200L);
    }

    @Test
    @DisplayName("수동 매칭 - 채팅(Chat) 아이템 성공")
    void matchManual_chat_success() {
        // given
        MatchManualRequest request = mock(MatchManualRequest.class);
        when(request.getLostItemId()).thenReturn(100L);
        when(request.getFoundItemId()).thenReturn(200L);

        when(itemRepository.findByIdOrThrow(100L)).thenReturn(lostItem);
        when(itemRepository.findByIdOrThrow(200L)).thenReturn(foundItem);

        when(itemMatchRepository.existsByLostItemAndFoundItem(lostItem, foundItem)).thenReturn(false);
        when(itemMatchRepository.existsByLostItemAndStatus(lostItem, MatchStatus.CONFIRMED)).thenReturn(false);
        when(itemMatchRepository.existsByFoundItemAndStatus(foundItem, MatchStatus.CONFIRMED)).thenReturn(false);

        when(itemMatchRepository.save(any(ItemMatch.class))).thenReturn(itemMatch);

        // when
        MatchManualResponse response = itemMatchService.matchManual(request);

        // then
        assertEquals(MatchManualType.CHAT, response.getMatchManualType());
        assertNull(response.getLockerId());
    }
}