package com.zoopick.server.service;

import com.zoopick.server.dto.profile.ProfileSummaryResponse;
import com.zoopick.server.dto.profile.ProfileUpdateRequest;
import com.zoopick.server.entity.User;
import com.zoopick.server.repository.ChatRoomRepository;
import com.zoopick.server.repository.ItemPostRepository;
import com.zoopick.server.repository.NotificationRepository;
import com.zoopick.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @InjectMocks
    private ProfileService profileService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemPostRepository itemPostRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private NotificationRepository notificationRepository;

    private User user;
    private final String EMAIL = "test@mju.ac.kr";
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // User 엔티티 모킹 (실제 엔티티의 @Builder 사용 가정)
        user = User.builder()
                .id(USER_ID)
                .schoolEmail(EMAIL)
                .nickname("기존닉네임")
                .department("컴퓨터공학과")
                .build();
    }

    @Test
    @DisplayName("프로필 요약 조회 - 성공 (각종 카운트 정상 집계)")
    void getProfileSummary_Success() {
        // given
        when(userRepository.findBySchoolEmailOrThrow(EMAIL)).thenReturn(user);
        when(itemPostRepository.countByUserId(USER_ID)).thenReturn(5L);
        when(chatRoomRepository.countByOwnerIdOrFinderId(USER_ID, USER_ID)).thenReturn(3L);
        when(notificationRepository.countByUserIdAndReadAtIsNull(USER_ID)).thenReturn(2L);

        // when
        ProfileSummaryResponse response = profileService.getProfileSummary(EMAIL);

        // then
        assertNotNull(response);
        assertEquals("기존닉네임", response.nickname());
        assertEquals("컴퓨터공학과", response.department());
        assertEquals(5L, response.postCount());
        assertEquals(3L, response.chatRoomCount());
        assertEquals(2L, response.unreadNotificationCount());

        verify(itemPostRepository, times(1)).countByUserId(USER_ID);
        verify(chatRoomRepository, times(1)).countByOwnerIdOrFinderId(USER_ID, USER_ID);
        verify(notificationRepository, times(1)).countByUserIdAndReadAtIsNull(USER_ID);
    }

    @Test
    @DisplayName("프로필 업데이트 - 성공 (닉네임 변경 없음)")
    void updateProfile_Success_SameNickname() {
        // given
        ProfileUpdateRequest request = new ProfileUpdateRequest("기존닉네임", "소프트웨어학과");
        when(userRepository.findBySchoolEmailOrThrow(EMAIL)).thenReturn(user);

        // when
        profileService.updateProfile(EMAIL, request);

        // then
        // 닉네임이 같으므로 중복 체크 로직(existsByNickname)이 호출되지 않아야 함
        verify(userRepository, never()).existsByNickname(anyString());
        assertEquals("소프트웨어학과", user.getDepartment());
    }

    @Test
    @DisplayName("프로필 업데이트 - 성공 (닉네임 변경 및 중복 없음)")
    void updateProfile_Success_NewNickname() {
        // given
        ProfileUpdateRequest request = new ProfileUpdateRequest("새로운닉네임", "소프트웨어학과");
        when(userRepository.findBySchoolEmailOrThrow(EMAIL)).thenReturn(user);
        when(userRepository.existsByNickname("새로운닉네임")).thenReturn(false);

        // when
        profileService.updateProfile(EMAIL, request);

        // then
        verify(userRepository, times(1)).existsByNickname("새로운닉네임");
        assertEquals("새로운닉네임", user.getNickname());
        assertEquals("소프트웨어학과", user.getDepartment());
    }

    @Test
    @DisplayName("프로필 업데이트 - 실패 (변경하려는 닉네임이 이미 존재함)")
    void updateProfile_Fail_NicknameAlreadyExists() {
        // given
        ProfileUpdateRequest request = new ProfileUpdateRequest("중복닉네임", "컴퓨터공학과");
        when(userRepository.findBySchoolEmailOrThrow(EMAIL)).thenReturn(user);
        when(userRepository.existsByNickname("중복닉네임")).thenReturn(true);

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> profileService.updateProfile(EMAIL, request));

        assertEquals("이미 존재하는 닉네임입니다.", exception.getMessage());

        // 정보가 업데이트 되지 않았는지 검증
        assertEquals("기존닉네임", user.getNickname());
    }
}