package com.zoopick.server.service;

import com.zoopick.server.entity.*;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.exception.ForbiddenException;
import com.zoopick.server.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockerServiceTest {

    @InjectMocks
    private LockerService lockerService;

    @Mock private LockerRepository lockerRepository;
    @Mock private LockerCommandRepository commandRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemMatchRepository itemMatchRepository;

    // 💡 NPE 해결: ChatRoomRepository 모킹 추가
    @Mock private ChatRoomRepository chatRoomRepository;

    private User mockUser;
    private User otherUser;
    private Locker emptyLocker;
    private Locker fullLocker;
    private Item validItem;
    private Item storedItem;

    private final Long MOCK_USER_ID = 1L;
    private final Long OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(MOCK_USER_ID).build();
        otherUser = User.builder().id(OTHER_USER_ID).build();

        emptyLocker = Locker.builder()
                .id(1L)
                .status(LockerStatus.EMPTY)
                .currentItem(null)
                .build();

        storedItem = Item.builder()
                .id(100L)
                // 💡 핵심 해결: 보관된 물품의 주인을 다시 원래대로 '나(mockUser)'로 복구!
                .reporter(mockUser)
                .status(ItemStatus.IN_LOCKER)
                .build();

        fullLocker = Locker.builder()
                .id(2L)
                .status(LockerStatus.IN_USE)
                .currentItem(storedItem)
                .build();

        validItem = Item.builder()
                .id(200L)
                .reporter(mockUser)
                .type(ItemType.FOUND)
                .status(ItemStatus.REPORTED)
                .build();
    }

    @Test
    @DisplayName("사물함 열기 요청 - 보관 (빈 사물함 + 본인이 신고한 습득물)")
    void requestUnlock_Storage_Success() {
        // given
        when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(mockUser));
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(emptyLocker));
        when(itemRepository.findById(200L)).thenReturn(Optional.of(validItem));
        when(commandRepository.save(any(LockerCommand.class))).thenAnswer(i -> i.getArgument(0));

        // when
        LockerCommand command = lockerService.requestUnlock(MOCK_USER_ID, 1L, 200L);

        // then
        assertEquals(LockerStatus.IN_USE, emptyLocker.getStatus());
        assertEquals(validItem, emptyLocker.getCurrentItem());
        assertEquals(ItemStatus.IN_LOCKER, validItem.getStatus());

        assertEquals(LockerCommandType.OPEN, command.getCommand());
        assertEquals(mockUser, command.getIssuedBy());
        verify(commandRepository, times(1)).save(any(LockerCommand.class));
    }

    @Test
    @DisplayName("사물함 열기 요청 - 보관 실패 (타인이 신고한 물품)")
    void requestUnlock_Storage_Fail_NotReporter() {
        // given
        validItem.setReporter(otherUser);
        when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(mockUser));
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(emptyLocker));
        when(itemRepository.findById(200L)).thenReturn(Optional.of(validItem));

        // when & then
        assertThrows(ForbiddenException.class,
                () -> lockerService.requestUnlock(MOCK_USER_ID, 1L, 200L));
    }

    @Test
    @DisplayName("사물함 열기 요청 - 보관 실패 (습득물이 아닌 경우)")
    void requestUnlock_Storage_Fail_NotFoundType() {
        // given
        validItem.setType(ItemType.LOST);
        when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(mockUser));
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(emptyLocker));
        when(itemRepository.findById(200L)).thenReturn(Optional.of(validItem));

        // when & then
        assertThrows(BadRequestException.class,
                () -> lockerService.requestUnlock(MOCK_USER_ID, 1L, 200L));
    }

    @Test
    @DisplayName("사물함 열기 요청 - 회수 (신고자 본인)")
    void requestUnlock_Retrieval_Success_ByReporter() {
        // given
        storedItem.setReporter(mockUser);

        when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(mockUser));
        when(lockerRepository.findById(2L)).thenReturn(Optional.of(fullLocker));

        when(itemMatchRepository.existsByFoundItemAndLostItem_Reporter_IdAndStatus(any(), anyLong(), any()))
                .thenReturn(true);

        when(commandRepository.save(any(LockerCommand.class))).thenAnswer(i -> i.getArgument(0));

        // when
        LockerCommand command = lockerService.requestUnlock(MOCK_USER_ID, 2L, null);

        // then
        assertEquals(LockerStatus.EMPTY, fullLocker.getStatus());
        assertNull(fullLocker.getCurrentItem());
        assertEquals(ItemStatus.RETURNED, storedItem.getStatus());
        assertNotNull(storedItem.getReturnedAt());

        assertEquals(LockerCommandType.OPEN, command.getCommand());
        verify(commandRepository, times(1)).save(any(LockerCommand.class));
    }

    @Test
    @DisplayName("사물함 열기 요청 - 회수 (매칭이 확정된 물품 소유자)")
    void requestUnlock_Retrieval_Success_ByMatchedOwner() {
        // given
        storedItem.setReporter(otherUser); // 이 테스트에서는 물건 주인을 다른 사람으로 변경
        when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(mockUser));
        when(lockerRepository.findById(2L)).thenReturn(Optional.of(fullLocker));

        when(itemMatchRepository.existsByFoundItemAndLostItem_Reporter_IdAndStatus(storedItem, MOCK_USER_ID, MatchStatus.CONFIRMED))
                .thenReturn(true);
        when(commandRepository.save(any(LockerCommand.class))).thenAnswer(i -> i.getArgument(0));

        // when
        LockerCommand command = lockerService.requestUnlock(MOCK_USER_ID, 2L, null);

        // then
        assertEquals(LockerStatus.EMPTY, fullLocker.getStatus());
        assertEquals(ItemStatus.RETURNED, storedItem.getStatus());
    }

    @Test
    @DisplayName("사물함 열기 요청 - 회수 실패 (권한 없음)")
    void requestUnlock_Retrieval_Fail_NoPermission() {
        // given
        storedItem.setReporter(otherUser); // 내 물건이 아님
        when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(mockUser));
        when(lockerRepository.findById(2L)).thenReturn(Optional.of(fullLocker));

        when(itemMatchRepository.existsByFoundItemAndLostItem_Reporter_IdAndStatus(storedItem, MOCK_USER_ID, MatchStatus.CONFIRMED))
                .thenReturn(false);

        // when & then
        assertThrows(ForbiddenException.class,
                () -> lockerService.requestUnlock(MOCK_USER_ID, 2L, null));
    }

    @Test
    @DisplayName("사물함 점검 중 예외 발생")
    void requestUnlock_Fail_Maintenance() {
        // given
        emptyLocker.setStatus(LockerStatus.MAINTENANCE);
        when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(mockUser));
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(emptyLocker));

        // when & then
        assertThrows(BadRequestException.class,
                () -> lockerService.requestUnlock(MOCK_USER_ID, 1L, 200L));
    }

    @Test
    @DisplayName("사물함 닫기 요청 - 성공")
    void requestLock_Success() {
        // given
        when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(mockUser));
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(emptyLocker));

        LockerCommand lastOpenCmd = LockerCommand.builder()
                .issuedBy(mockUser)
                .command(LockerCommandType.OPEN)
                .build();
        when(commandRepository.findFirstByLocker_IdAndCommandOrderByCreatedAtDesc(1L, LockerCommandType.OPEN))
                .thenReturn(Optional.of(lastOpenCmd));

        when(commandRepository.save(any(LockerCommand.class))).thenAnswer(i -> i.getArgument(0));

        // when
        LockerCommand command = lockerService.requestLock(MOCK_USER_ID, 1L);

        // then
        assertEquals(LockerCommandType.CLOSE, command.getCommand());
        verify(commandRepository, times(1)).save(any(LockerCommand.class));
    }

    @Test
    @DisplayName("사물함 닫기 요청 - 실패 (자신이 열지 않은 사물함)")
    void requestLock_Fail_NotIssuer() {
        // given
        when(userRepository.findById(MOCK_USER_ID)).thenReturn(Optional.of(mockUser));
        when(lockerRepository.findById(1L)).thenReturn(Optional.of(emptyLocker));

        LockerCommand lastOpenCmd = LockerCommand.builder()
                .issuedBy(otherUser)
                .command(LockerCommandType.OPEN)
                .build();
        when(commandRepository.findFirstByLocker_IdAndCommandOrderByCreatedAtDesc(1L, LockerCommandType.OPEN))
                .thenReturn(Optional.of(lastOpenCmd));

        // when & then
        assertThrows(ForbiddenException.class,
                () -> lockerService.requestLock(MOCK_USER_ID, 1L));
    }

    @Test
    @DisplayName("명령 폴링 (Poll) - 성공 시 CONSUMED 상태로 변경")
    void pollNextCommand_Success() {
        // given
        LockerCommand pendingCommand = LockerCommand.builder()
                .id(10L)
                .locker(emptyLocker)
                .command(LockerCommandType.OPEN)
                .status(LockerCommandStatus.PENDING)
                .build();

        when(commandRepository.findFirstByLocker_IdAndStatusOrderByCreatedAtAsc(1L, LockerCommandStatus.PENDING))
                .thenReturn(Optional.of(pendingCommand));

        // when
        Optional<LockerCommand> result = lockerService.pollNextCommand(1L);

        // then
        assertTrue(result.isPresent());
        assertEquals(LockerCommandStatus.CONSUMED, result.get().getStatus());
        assertNotNull(result.get().getConsumedAt());
    }

    @Test
    @DisplayName("명령 완료 처리 (Ack) - 성공 시 COMPLETED 상태로 변경")
    void ackCommand_Success() {
        // given
        LockerCommand consumedCommand = LockerCommand.builder()
                .id(10L)
                .locker(emptyLocker)
                .status(LockerCommandStatus.CONSUMED)
                .build();

        when(commandRepository.findById(10L)).thenReturn(Optional.of(consumedCommand));

        // when
        lockerService.ackCommand(1L, 10L);

        // then
        assertEquals(LockerCommandStatus.COMPLETED, consumedCommand.getStatus());
        assertNotNull(consumedCommand.getCompletedAt());
    }

    @Test
    @DisplayName("명령 완료 처리 (Ack) - 다른 사물함의 명령인 경우 예외 발생")
    void ackCommand_Fail_LockerMismatch() {
        // given
        LockerCommand consumedCommand = LockerCommand.builder()
                .id(10L)
                .locker(fullLocker)
                .build();

        when(commandRepository.findById(10L)).thenReturn(Optional.of(consumedCommand));

        // when & then
        assertThrows(BadRequestException.class,
                () -> lockerService.ackCommand(1L, 10L));
    }
}