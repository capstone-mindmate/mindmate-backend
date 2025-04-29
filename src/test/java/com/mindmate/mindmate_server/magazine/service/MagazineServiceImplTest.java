package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MagazineErrorCode;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import com.mindmate.mindmate_server.magazine.domain.*;
import com.mindmate.mindmate_server.magazine.dto.*;
import com.mindmate.mindmate_server.magazine.repository.MagazineContentRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineLikeRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineRepository;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MagazineServiceImplTest {
    @Mock private UserService userService;
    @Mock private MagazineImageService magazineImageService;
    @Mock private NotificationService notificationService;
    @Mock private MagazinePopularityService magazinePopularityService;
    @Mock private MagazineContentService magazineContentService;
    @Mock private MagazineRepository magazineRepository;
    @Mock private MagazineLikeRepository magazineLikeRepository;
    @Mock private MagazineContentRepository magazineContentRepository;
    @Mock private KafkaTemplate<String, MagazineEngagementEvent> kafkaTemplate;
    @Mock private SlackNotifier slackNotifier;

    @InjectMocks
    private MagazineServiceImpl magazineService;

    private Long userId;
    private Long magazineId;
    private User mockUser;
    private Magazine mockMagazine;
    private MagazineContent mockContent;

    @BeforeEach
    void setup() {
        userId = 1L;
        magazineId = 100L;

        mockUser = mock(User.class);
        mockMagazine = mock(Magazine.class);
        mockContent = mock(MagazineContent.class);

        Profile mockProfile = mock(Profile.class);
        when(mockProfile.getNickname()).thenReturn("testUser");
        when(mockUser.getId()).thenReturn(userId);
        when(mockUser.getProfile()).thenReturn(mockProfile);
        when(mockUser.getCurrentRole()).thenReturn(RoleType.ROLE_USER);

        when(mockMagazine.getId()).thenReturn(magazineId);
        when(mockMagazine.getTitle()).thenReturn("Test Magazine");
        when(mockMagazine.getAuthor()).thenReturn(mockUser);
        when(mockMagazine.getCategory()).thenReturn(MatchingCategory.CAREER);
        when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PUBLISHED);
        when(mockMagazine.getLikeCount()).thenReturn(10);
        when(mockMagazine.getContents()).thenReturn(List.of(mockContent));

        when(mockContent.getType()).thenReturn(MagazineContentType.TEXT);
        when(mockContent.getText()).thenReturn("본문 내용");

        when(magazineRepository.findById(magazineId)).thenReturn(Optional.of(mockMagazine));
        when(userService.findUserById(userId)).thenReturn(mockUser);
    }

    @Nested
    @DisplayName("매거진 생성 테스트")
    class CreateMagazineTest {
        @Test
        @DisplayName("매거진 생성 성공")
        void createMagazine_Success() {
            // given
            MagazineCreateRequest request = mock(MagazineCreateRequest.class);
            when(request.getTitle()).thenReturn("New Magazine");
            when(request.getCategory()).thenReturn(MatchingCategory.CAREER);
            when(request.getContents()).thenReturn(List.of());

            when(magazineRepository.save(any(Magazine.class))).thenReturn(mockMagazine);

            // when
            MagazineResponse response = magazineService.createMagazine(userId, request);

            // then
            assertNotNull(response);
            assertEquals("Test Magazine", response.getTitle());
            verify(magazineRepository).save(any(Magazine.class));
            verify(magazineContentService).processContents(any(), any());
            verify(slackNotifier).sendMagazineCreateAlert(any(), eq(mockUser));
        }
    }

    @Nested
    @DisplayName("매거진 수정 테스트")
    class UpdateMagazineTest {
        @Test
        @DisplayName("매거진 수정 성공")
        void updateMagazine_Success() {
            // given
            MagazineUpdateRequest request = mock(MagazineUpdateRequest.class);
            when(request.getTitle()).thenReturn("Updated Title");
            when(request.getCategory()).thenReturn(MatchingCategory.ACADEMIC);
            when(request.getContents()).thenReturn(List.of());

            // when
            MagazineResponse response = magazineService.updateMagazine(magazineId, request, userId);

            // then
            assertNotNull(response);
            verify(magazineContentRepository).deleteByMagazine(mockMagazine);
            verify(mockMagazine).update("Updated Title", MatchingCategory.ACADEMIC);
            verify(magazineContentService).processContents(mockMagazine, List.of());
            verify(slackNotifier).sendMagazineUpdateAlert(mockMagazine, mockUser);
        }

        @Test
        @DisplayName("매거진 수정 실패 - 권한 없음")
        void updateMagazine_AccessDenied() {
            // given
            Long otherUserId = 2L;
            User otherUser = mock(User.class);
            when(otherUser.getId()).thenReturn(otherUserId);
            when(userService.findUserById(otherUserId)).thenReturn(otherUser);
            MagazineUpdateRequest request = mock(MagazineUpdateRequest.class);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> magazineService.updateMagazine(magazineId, request, otherUserId));
            assertEquals(MagazineErrorCode.MAGAZINE_ACCESS_DENIED, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("매거진 삭제 테스트")
    class DeleteMagazineTest {
        @ParameterizedTest
        @CsvSource({"ROLE_ADMIN,true", "ROLE_USER,false"})
        @DisplayName("매거진 삭제 권한별")
        void deleteMagazine_RoleTest(String role, boolean isAllowed) {
            // given
            Long testUserId = 2L;
            User testUser = mock(User.class);
            when(testUser.getId()).thenReturn(testUserId);
            when(testUser.getCurrentRole()).thenReturn(RoleType.valueOf(role));
            when(userService.findUserById(testUserId)).thenReturn(testUser);

            // when & then
            if (isAllowed) {
                magazineService.deleteMagazine(magazineId, testUserId);
                verify(magazineRepository).delete(mockMagazine);
                verify(magazinePopularityService).removePopularityScores(magazineId, mockMagazine.getCategory());
            } else {
                CustomException exception = assertThrows(CustomException.class,
                        () -> magazineService.deleteMagazine(magazineId, testUserId));
                assertEquals(MagazineErrorCode.MAGAZINE_ACCESS_DENIED, exception.getErrorCode());
            }
        }
    }

    @Nested
    @DisplayName("매거진 상세 조회 테스트")
    class GetMagazineTest {
        @Test
        void getMagazine_NotPublished_Exception() {
            // given
            when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PENDING);
            User otherUser = mock(User.class);
            when(otherUser.getId()).thenReturn(2L);
            when(userService.findUserById(2L)).thenReturn(otherUser);
            when(mockMagazine.getAuthor()).thenReturn(mockUser);

            when(mockContent.getType()).thenReturn(MagazineContentType.TEXT);
            when(mockContent.getText()).thenReturn("본문 내용");

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> magazineService.getMagazine(magazineId, 2L));
            assertEquals(MagazineErrorCode.MAGAZINE_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("매거진 목록/대기 목록 조회 테스트")
    class GetMagazinesTest {
        @Test
        @DisplayName("매거진 목록 조회 성공")
        void getMagazines_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            MagazineSearchFilter filter = mock(MagazineSearchFilter.class);
            Page<MagazineResponse> mockPage = new PageImpl<>(List.of(MagazineResponse.from(mockMagazine)));

            when(magazineRepository.findMagazinesWithFilters(filter, pageable)).thenReturn(mockPage);

            // when
            Page<MagazineResponse> result = magazineService.getMagazines(userId, filter, pageable);

            // then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(magazineRepository).findMagazinesWithFilters(filter, pageable);
        }

        @Test
        @DisplayName("대기 중인 매거진 목록 조회 성공")
        void getPendingMagazines_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Magazine> mockPage = new PageImpl<>(List.of(mockMagazine));
            when(magazineRepository.findByMagazineStatus(MagazineStatus.PENDING, pageable)).thenReturn(mockPage);

            // when
            Page<MagazineResponse> result = magazineService.getPendingMagazines(pageable);

            // then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(magazineRepository).findByMagazineStatus(MagazineStatus.PENDING, pageable);
        }
    }

    @Nested
    @DisplayName("좋아요 토글 테스트")
    class ToggleLikeTest {
        @ParameterizedTest
        @CsvSource({"false,true", "true,false"})
        @DisplayName("좋아요 토글 성공")
        void toggleLike_Param(boolean alreadyLiked, boolean expectedLiked) {
            // given
            when(magazineLikeRepository.existsByMagazineAndUser(mockMagazine, mockUser)).thenReturn(alreadyLiked);

            // when
            LikeResponse response = magazineService.toggleLike(magazineId, userId);

            // then
            assertEquals(expectedLiked, response.isLiked());
            if (alreadyLiked) {
                verify(magazineLikeRepository).deleteByMagazineAndUser(mockMagazine, mockUser);
                verify(mockMagazine).removeLike(mockUser);
                verify(magazinePopularityService).updateLikeScore(mockMagazine, false);
            } else {
                verify(magazineLikeRepository).save(any(MagazineLike.class));
                verify(mockMagazine).addLike(mockUser);
                verify(magazinePopularityService).updateLikeScore(mockMagazine, true);
            }
        }
    }

    @Nested
    @DisplayName("매거진 관리 테스트")
    class ManageMagazineTest {
        @ParameterizedTest
        @CsvSource({"true,PUBLISHED", "false,REJECTED"})
        @DisplayName("매거진 승인/거절 성공")
        void manageMagazine_Param(boolean isAccepted, MagazineStatus expectedStatus) {
            // given
            when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PENDING);
            when(magazineRepository.findById(magazineId)).thenReturn(Optional.of(mockMagazine));
            when(magazineRepository.save(mockMagazine)).thenReturn(mockMagazine);

            // when
            MagazineResponse response = magazineService.manageMagazine(magazineId, isAccepted);

            // then
            verify(mockMagazine).setStatus(expectedStatus);
            verify(magazineRepository).save(mockMagazine);
        }

        @Test
        @DisplayName("이미 발행된 매거진 관리 실패")
        void manageMagazine_AlreadyPublished() {
            // given
            when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PUBLISHED);
            when(magazineRepository.findById(magazineId)).thenReturn(Optional.of(mockMagazine));

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineService.manageMagazine(magazineId, true));
            assertEquals(MagazineErrorCode.ALREADY_PUBLISHED, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("매거진 참여 및 인기 목록")
    class EngagementAndPopularityTest {
        @Test
        @DisplayName("사용자 참여 이벤트 처리 성공")
        void handleEngagement_Success() {
            // given
            MagazineEngagementRequest request = new MagazineEngagementRequest(1500L, 85.5);
            ArgumentCaptor<MagazineEngagementEvent> eventCaptor = ArgumentCaptor.forClass(MagazineEngagementEvent.class);

            // when
            magazineService.handleEngagement(userId, magazineId, request);

            // then
            verify(kafkaTemplate).send(eq("magazine-engagement-topic"), eventCaptor.capture());
            MagazineEngagementEvent capturedEvent = eventCaptor.getValue();

            assertAll(
                    () -> assertEquals(userId, capturedEvent.getUserId()),
                    () -> assertEquals(magazineId, capturedEvent.getMagazineId()),
                    () -> assertEquals(1500L, capturedEvent.getDwellTime()),
                    () -> assertEquals(85.5f, capturedEvent.getScrollPercentage()),
                    () -> assertNotNull(capturedEvent.getTimestamp())
            );
        }

    }
}
