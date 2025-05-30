package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MagazineErrorCode;
import com.mindmate.mindmate_server.global.service.ResilientEventPublisher;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import com.mindmate.mindmate_server.magazine.domain.*;
import com.mindmate.mindmate_server.magazine.dto.*;
import com.mindmate.mindmate_server.magazine.repository.MagazineContentRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineLikeRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineRepository;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.ProfileImage;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock private SlackNotifier slackNotifier;
    @Mock private ResilientEventPublisher eventPublisher;

    @InjectMocks
    private MagazineServiceImpl magazineService;

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_USER_ID = 2L;
    private static final Long OTHER_USER_ID = 3L;
    private static final Long MAGAZINE_ID = 100L;

    private User mockUser;
    private User mockAdminUser;
    private User mockOtherUser;
    private Magazine mockMagazine;
    private MagazineContent mockTextContent;
    private MagazineContent mockImageContent;
    private MagazineImage mockImage;
    private Profile mockProfile;
    private ProfileImage mockProfileImage;

    @BeforeEach
    void setup() {
        setupMockUsers();
        setupMockMagazine();
        setupMockContent();
        setupRepositoryMocks();
    }

    private void setupMockUsers() {
        mockUser = createMockUser(USER_ID, "testUser", RoleType.ROLE_USER);
        mockAdminUser = createMockUser(ADMIN_USER_ID, "adminUser", RoleType.ROLE_ADMIN);
        mockOtherUser = createMockUser(OTHER_USER_ID, "otherUser", RoleType.ROLE_USER);
    }

    private User createMockUser(Long id, String nickname, RoleType role) {
        User user = mock(User.class);
        Profile profile = mock(Profile.class);
        ProfileImage profileImage = mock(ProfileImage.class);

        when(user.getId()).thenReturn(id);
        when(user.getProfile()).thenReturn(profile);
        when(user.getCurrentRole()).thenReturn(role);
        when(profile.getNickname()).thenReturn(nickname);
        when(profile.getProfileImage()).thenReturn(profileImage);
        when(profileImage.getImageUrl()).thenReturn("https://example.com/profile.jpg");

        return user;
    }

    private void setupMockMagazine() {
        mockMagazine = mock(Magazine.class);
        when(mockMagazine.getId()).thenReturn(MAGAZINE_ID);
        when(mockMagazine.getTitle()).thenReturn("Test Magazine");
        when(mockMagazine.getSubtitle()).thenReturn("Test Subtitle");
        when(mockMagazine.getAuthor()).thenReturn(mockUser);
        when(mockMagazine.getCategory()).thenReturn(MatchingCategory.CAREER);
        when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PUBLISHED);
        when(mockMagazine.getLikeCount()).thenReturn(10);
    }

    private void setupMockContent() {
        mockTextContent = mock(MagazineContent.class);
        when(mockTextContent.getType()).thenReturn(MagazineContentType.TEXT);
        when(mockTextContent.getText()).thenReturn("본문 내용");
        when(mockTextContent.getId()).thenReturn(1L);
        when(mockTextContent.getContentOrder()).thenReturn(1);

        mockImageContent = mock(MagazineContent.class);
        mockImage = mock(MagazineImage.class);
        when(mockImageContent.getType()).thenReturn(MagazineContentType.IMAGE);
        when(mockImageContent.getImage()).thenReturn(mockImage);
        when(mockImageContent.getId()).thenReturn(2L);
        when(mockImageContent.getContentOrder()).thenReturn(2);
        when(mockImage.getStoredName()).thenReturn("test-image.webp");

        when(mockMagazine.getContents()).thenReturn(List.of(mockTextContent, mockImageContent));
    }

    private void setupRepositoryMocks() {
        when(magazineRepository.findById(MAGAZINE_ID)).thenReturn(Optional.of(mockMagazine));
        when(userService.findUserById(USER_ID)).thenReturn(mockUser);
        when(userService.findUserById(ADMIN_USER_ID)).thenReturn(mockAdminUser);
        when(userService.findUserById(OTHER_USER_ID)).thenReturn(mockOtherUser);
    }


    @Nested
    @DisplayName("매거진 생성 테스트")
    class CreateMagazineTest {
        @Test
        @DisplayName("매거진 생성 성공")
        void createMagazine_Success() {
            // given
            MagazineCreateRequest request = createMagazineCreateRequest();
            when(magazineRepository.save(any(Magazine.class))).thenReturn(mockMagazine);

            // when
            MagazineResponse response = magazineService.createMagazine(USER_ID, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("Test Magazine");

            verify(magazineRepository).save(any(Magazine.class));
            verify(magazineContentService).processContents(any(Magazine.class), eq(request.getContents()));
            verify(slackNotifier).sendMagazineCreateAlert(any(Magazine.class), eq(mockUser));
        }

        @Test
        @DisplayName("이미지가 없는 매거진 생성 실패")
        void createMagazine_NoImage_ThrowsException() {
            // given
            MagazineCreateRequest request = createMagazineCreateRequestWithoutImage();

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineService.createMagazine(USER_ID, request));
            assertThat(exception.getErrorCode()).isEqualTo(MagazineErrorCode.MAGAZINE_IMAGE_REQUIRED);
        }


        private MagazineCreateRequest createMagazineCreateRequest() {
            List<MagazineContentDTO> contents = List.of(
                    MagazineContentDTO.builder()
                            .type(MagazineContentType.TEXT)
                            .text("본문 내용")
                            .build(),
                    MagazineContentDTO.builder()
                            .type(MagazineContentType.IMAGE)
                            .imageId(1L)
                            .build()
            );

            return MagazineCreateRequest.builder()
                    .title("New Magazine")
                    .subtitle("New Subtitle")
                    .category(MatchingCategory.CAREER)
                    .contents(contents)
                    .build();
        }
        private MagazineCreateRequest createMagazineCreateRequestWithoutImage() {
            List<MagazineContentDTO> contents = List.of(
                    MagazineContentDTO.builder()
                            .type(MagazineContentType.TEXT)
                            .text("본문 내용")
                            .build()
            );

            return MagazineCreateRequest.builder()
                    .title("New Magazine")
                    .subtitle("New Subtitle")
                    .category(MatchingCategory.CAREER)
                    .contents(contents)
                    .build();
        }
    }

    @Nested
    @DisplayName("매거진 수정 테스트")
    class UpdateMagazineTest {
        @Test
        @DisplayName("매거진 수정 성공")
        void updateMagazine_Success() {
            // given
            MagazineUpdateRequest request = createMagazineUpdateRequest();

            // when
            MagazineResponse response = magazineService.updateMagazine(MAGAZINE_ID, request, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(mockMagazine).update("Updated Title", "Updated Subtitle", MatchingCategory.ACADEMIC);
            verify(magazineContentRepository).deleteByMagazine(mockMagazine);
            verify(mockMagazine).clearContents();
            verify(magazineContentService).processContents(mockMagazine, request.getContents());
            verify(slackNotifier).sendMagazineUpdateAlert(mockMagazine, mockUser);
        }

        @Test
        @DisplayName("매거진 수정 실패 - 권한 없음")
        void updateMagazine_AccessDenied() {
            // given
            MagazineUpdateRequest request = createMagazineUpdateRequest();

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> magazineService.updateMagazine(MAGAZINE_ID, request, OTHER_USER_ID));
            assertThat(exception.getErrorCode()).isEqualTo(MagazineErrorCode.MAGAZINE_ACCESS_DENIED);
        }

        private MagazineUpdateRequest createMagazineUpdateRequest() {
            List<MagazineContentDTO> contents = List.of(
                    MagazineContentDTO.builder()
                            .type(MagazineContentType.TEXT)
                            .text("수정된 본문")
                            .build(),
                    MagazineContentDTO.builder()
                            .type(MagazineContentType.IMAGE)
                            .imageId(1L)
                            .build()
            );

            return MagazineUpdateRequest.builder()
                    .title("Updated Title")
                    .subtitle("Updated Subtitle")
                    .category(MatchingCategory.ACADEMIC)
                    .contents(contents)
                    .build();
        }
    }

    @Nested
    @DisplayName("매거진 삭제")
    class DeleteMagazineTest {

        @Test
        @DisplayName("관리자의 매거진 삭제 성공")
        void deleteMagazine_AdminUser_Success() {
            // when
            magazineService.deleteMagazine(MAGAZINE_ID, ADMIN_USER_ID);

            // then
            verify(magazineImageService).deleteImage(mockImage.getStoredName());
            verify(magazinePopularityService).removePopularityScores(MAGAZINE_ID, MatchingCategory.CAREER);
            verify(magazineRepository).delete(mockMagazine);
        }

        @Test
        @DisplayName("일반 사용자의 매거진 삭제 실패")
        void deleteMagazine_RegularUser_ThrowsException() {
            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> magazineService.deleteMagazine(MAGAZINE_ID, USER_ID));
            assertThat(exception.getErrorCode()).isEqualTo(MagazineErrorCode.MAGAZINE_ACCESS_DENIED);
        }

        @Test
        @DisplayName("이미지가 포함된 매거진 삭제")
        void deleteMagazine_WithImages_DeletesImages() {
            // when
            magazineService.deleteMagazine(MAGAZINE_ID, ADMIN_USER_ID);

            // then
            verify(magazineImageService).deleteImage(mockImage.getStoredName());
            verify(magazineRepository).delete(mockMagazine);
        }
    }

    @Nested
    @DisplayName("매거진 상세 조회 테스트")
    class GetMagazineTest {
        @Test
        @DisplayName("발행된 매거진 조회 성공")
        void getMagazine_PublishedMagazine_Success() {
            // given
            when(magazineRepository.findWIthAllDetailsById(MAGAZINE_ID)).thenReturn(Optional.of(mockMagazine));
            when(magazineLikeRepository.existsByMagazineAndUser(mockMagazine, mockUser)).thenReturn(true);

            // when
            MagazineDetailResponse response = magazineService.getMagazine(MAGAZINE_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(magazinePopularityService).incrementViewCount(mockMagazine, USER_ID);
        }

        @Test
        @DisplayName("미발행 매거진을 다른 사용자가 조회 시 실패")
        void getMagazine_UnpublishedByOtherUser_ThrowsException() {
            // given
            when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PENDING);
            when(magazineRepository.findWIthAllDetailsById(MAGAZINE_ID)).thenReturn(Optional.of(mockMagazine));

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> magazineService.getMagazine(MAGAZINE_ID, OTHER_USER_ID));
            assertThat(exception.getErrorCode()).isEqualTo(MagazineErrorCode.MAGAZINE_NOT_FOUND);
        }

        @Test
        @DisplayName("미발행 매거진 작성자 조회 성공")
        void getMagazine_UnpublishedByAuthor_Success() {
            // given
            when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PENDING);
            when(magazineRepository.findWIthAllDetailsById(MAGAZINE_ID)).thenReturn(Optional.of(mockMagazine));
            when(magazineLikeRepository.existsByMagazineAndUser(mockMagazine, mockUser)).thenReturn(false);

            // when
            MagazineDetailResponse response = magazineService.getMagazine(MAGAZINE_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(magazinePopularityService).incrementViewCount(mockMagazine, USER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 매거진 조회 시 예외 발생")
        void getMagazine_NotFound_ThrowsException() {
            // given
            Long nonExistentId = 999L;
            when(magazineRepository.findWIthAllDetailsById(nonExistentId)).thenReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineService.getMagazine(nonExistentId, USER_ID));
            assertThat(exception.getErrorCode()).isEqualTo(MagazineErrorCode.MAGAZINE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("매거진 목록/대기 목록 조회 테스트")
    class GetMagazinesTest {
        @Test
        @DisplayName("필터링된 매거진 목록 조회")
        void getMagazines_WithFilter_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            MagazineSearchFilter filter = MagazineSearchFilter.builder()
                    .category(MatchingCategory.CAREER)
                    .keyword("test")
                    .build();

            Page<MagazineResponse> mockPage = new PageImpl<>(List.of(MagazineResponse.from(mockMagazine)));
            when(magazineRepository.findMagazinesWithFilters(filter, pageable)).thenReturn(mockPage);

            // when
            Page<MagazineResponse> result = magazineService.getMagazines(USER_ID, filter, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(magazineRepository).findMagazinesWithFilters(filter, pageable);
        }

        @Test
        @DisplayName("사용자의 매거진 목록 조회")
        void getMyMagazines_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Magazine> mockPage = new PageImpl<>(List.of(mockMagazine));
            when(magazineRepository.findByAuthorIdOrderByCreatedAtDesc(USER_ID, pageable)).thenReturn(mockPage);

            // when
            Page<MagazineResponse> result = magazineService.getMyMagazines(USER_ID, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(magazineRepository).findByAuthorIdOrderByCreatedAtDesc(USER_ID, pageable);
        }

        @Test
        @DisplayName("좋아요한 매거진 목록 조회")
        void getLikedMagazines_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Magazine> mockPage = new PageImpl<>(List.of(mockMagazine));
            when(magazineRepository.findByLikesUserIdOrderByCreatedAtDesc(USER_ID, pageable))
                    .thenReturn(mockPage);

            // when
            Page<MagazineResponse> result = magazineService.getLikedMagazines(USER_ID, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(magazineRepository).findByLikesUserIdOrderByCreatedAtDesc(USER_ID, pageable);
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
        @DisplayName("좋아요 토글")
        @MethodSource("likeToggleScenarios")
        void toggleLike_Param(boolean alreadyLiked, boolean expectedLiked) {
            // given
            when(magazineLikeRepository.existsByMagazineAndUser(mockMagazine, mockUser)).thenReturn(alreadyLiked);

            // when
            LikeResponse response = magazineService.toggleLike(MAGAZINE_ID, USER_ID);

            // then
            assertThat(response.isLiked()).isEqualTo(expectedLiked);
            assertThat(response.getLikeCount()).isEqualTo(10);

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

        static Stream<Arguments> likeToggleScenarios() {
            return Stream.of(
                    Arguments.of(false, true),   // 좋아요 추가
                    Arguments.of(true, false)    // 좋아요 제거
            );
        }
    }

    @Nested
    @DisplayName("매거진 관리 테스트")
    class ManageMagazineTest {
        @ParameterizedTest
        @DisplayName("매거진 승인/거절 성공")
        @MethodSource("managementScenarios")
        void manageMagazine_ApprovalAndRejection(boolean isAccepted, MagazineStatus expectedStatus) {
            // given
            when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PENDING);
            when(magazineRepository.save(mockMagazine)).thenReturn(mockMagazine);

            // when
            MagazineResponse response = magazineService.manageMagazine(MAGAZINE_ID, isAccepted);

            // then
            assertThat(response).isNotNull();
            verify(mockMagazine).setStatus(expectedStatus);
            verify(magazineRepository).save(mockMagazine);
            verify(notificationService).processNotification(any());

            if (isAccepted) {
                verify(magazinePopularityService).initializePopularityScore(mockMagazine);
            }
        }

        static Stream<Arguments> managementScenarios() {
            return Stream.of(
                    Arguments.of(true, MagazineStatus.PUBLISHED),
                    Arguments.of(false, MagazineStatus.REJECTED)
            );
        }

        @Test
        @DisplayName("이미 발행된 매거진 관리 실패")
        void manageMagazine_AlreadyPublished() {
            // given
            when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PUBLISHED);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineService.manageMagazine(MAGAZINE_ID, true));
            assertThat(exception.getErrorCode()).isEqualTo(MagazineErrorCode.ALREADY_PUBLISHED);
        }
    }

    @Nested
    @DisplayName("인기 매거진 조회")
    class PopularMagazineTest {
        @Test
        @DisplayName("전체 인기 매거진 조회")
        void getPopularMagazines_Success() {
            // given
            int limit = 5;
            List<MagazineResponse> popularMagazines = List.of(MagazineResponse.from(mockMagazine));
            when(magazinePopularityService.getPopularMagazines(limit)).thenReturn(popularMagazines);

            // when
            List<MagazineResponse> result = magazineService.getPopularMagazines(limit);

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            verify(magazinePopularityService).getPopularMagazines(limit);
        }

        @Test
        @DisplayName("카테고리별 인기 매거진 조회")
        void getPopularMagazinesByCategory_Success() {
            // given
            MatchingCategory category = MatchingCategory.CAREER;
            int limit = 3;
            List<MagazineResponse> popularMagazines = List.of(MagazineResponse.from(mockMagazine));
            when(magazinePopularityService.getPopularMagazinesByCategory(category, limit))
                    .thenReturn(popularMagazines);

            // when
            List<MagazineResponse> result = magazineService.getPopularMagazinesByCategory(category, limit);

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            verify(magazinePopularityService).getPopularMagazinesByCategory(category, limit);
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

            // when
            magazineService.handleEngagement(USER_ID, MAGAZINE_ID, request);

            // then
            ArgumentCaptor<MagazineEngagementEvent> eventCaptor = ArgumentCaptor.forClass(MagazineEngagementEvent.class);
            verify(eventPublisher).publishEvent(eq("magazine-engagement-topic"), eventCaptor.capture());

            MagazineEngagementEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getUserId()).isEqualTo(USER_ID);
            assertThat(capturedEvent.getMagazineId()).isEqualTo(MAGAZINE_ID);
            assertThat(capturedEvent.getDwellTime()).isEqualTo(1500L);
            assertThat(capturedEvent.getScrollPercentage()).isEqualTo(85.5);
            assertThat(capturedEvent.getTimestamp()).isNotNull();
        }
    }
}
