package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MagazineErrorCode;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineImage;
import com.mindmate.mindmate_server.magazine.domain.MagazineLike;
import com.mindmate.mindmate_server.magazine.domain.MagazineStatus;
import com.mindmate.mindmate_server.magazine.dto.*;
import com.mindmate.mindmate_server.magazine.repository.MagazineImageRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineLikeRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineRepository;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mindmate.mindmate_server.magazine.service.MagazineServiceImpl.MAX_IMAGE_SIZE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MagazineServiceImplTest {
    @Mock private UserService userService;
    @Mock private MagazineImageService magazineImageService;
    @Mock private MagazineRepository magazineRepository;
    @Mock private MagazineLikeRepository magazineLikeRepository;
    @Mock private MagazineImageRepository magazineImageRepository;

    @InjectMocks
    private MagazineServiceImpl magazineService;

    private Long userId;
    private Long magazineId;
    private User mockUser;
    private Magazine mockMagazine;
    private MagazineImage mockImage;

    @BeforeEach
    void setup() {
        userId = 1L;
        magazineId = 100L;

        // Mock 객체 생성
        mockUser = mock(User.class);
        mockMagazine = mock(Magazine.class);
        mockImage = mock(MagazineImage.class);

        // User 모킹
        Profile mockProfile = mock(Profile.class);
        when(mockProfile.getNickname()).thenReturn("testUser");
        when(mockUser.getId()).thenReturn(userId);
        when(mockUser.getProfile()).thenReturn(mockProfile);
        when(mockUser.getCurrentRole()).thenReturn(RoleType.ROLE_USER);

        // Magazine 모킹
        when(mockMagazine.getId()).thenReturn(magazineId);
        when(mockMagazine.getTitle()).thenReturn("Test Magazine");
        when(mockMagazine.getContent()).thenReturn("Test Content");
        when(mockMagazine.getAuthor()).thenReturn(mockUser);
        when(mockMagazine.getCategory()).thenReturn(MatchingCategory.CAREER);
        when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PUBLISHED);
        when(mockMagazine.getLikeCount()).thenReturn(10);
        when(mockMagazine.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(mockMagazine.getModifiedAt()).thenReturn(LocalDateTime.now());
        when(mockMagazine.getImages()).thenReturn(List.of(mockImage));

        // MagazineImage 모킹
        when(mockImage.getId()).thenReturn(1L);
        when(mockImage.getImageUrl()).thenReturn("test/images/uuid.webp");

        // Repository 모킹
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
            Long userId = 1L;
            MagazineCreateRequest request = mock(MagazineCreateRequest.class);
            when(request.getTitle()).thenReturn("New Magazine");
            when(request.getContent()).thenReturn("New Content");
            when(request.getCategory()).thenReturn(MatchingCategory.CAREER);
            when(request.getImageIds()).thenReturn(List.of(1L));

            when(magazineImageRepository.findAllById(any())).thenReturn(List.of(mockImage));
            when(magazineRepository.save(any(Magazine.class))).thenReturn(mockMagazine);

            // when
            MagazineResponse response = magazineService.createMagazine(userId, request);

            // then
            assertNotNull(response);
            assertEquals(magazineId, response.getId());
            assertEquals("Test Magazine", response.getTitle());
            assertEquals(MatchingCategory.CAREER, response.getCategory());
            verify(magazineRepository).save(any(Magazine.class));
        }

        @Test
        @DisplayName("매거진 생성 싶래 - 이미지 개수 제한")
        void createMagazine_TooManyImages() {
            // given
            MagazineCreateRequest request = mock(MagazineCreateRequest.class);
            when(request.getTitle()).thenReturn("New Magazine");
            when(request.getContent()).thenReturn("New Content");
            when(request.getCategory()).thenReturn(MatchingCategory.CAREER);

            List<Long> imageIds = IntStream.rangeClosed(1, (int) (MAX_IMAGE_SIZE + 1))
                    .mapToObj(i -> (long)i)
                    .collect(Collectors.toList());
            when(request.getImageIds()).thenReturn(imageIds);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineService.createMagazine(userId, request));
            assertEquals(MagazineErrorCode.TOO_MANY_IMAGES, exception.getErrorCode());
        }

        @Test
        @DisplayName("매거진 생성 실패 - 해당 이미지 이미 사용 중")
        void createMagazine_ImageAlreadyInUse() {
            // given
            MagazineCreateRequest request = mock(MagazineCreateRequest.class);
            when(request.getTitle()).thenReturn("New Magazine");
            when(request.getContent()).thenReturn("New Content");
            when(request.getCategory()).thenReturn(MatchingCategory.CAREER);
            when(request.getImageIds()).thenReturn(List.of(1L));

            when(mockImage.getMagazine()).thenReturn(mock(Magazine.class));
            when(magazineImageRepository.findAllById(any())).thenReturn(List.of(mockImage));

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineService.createMagazine(userId, request));
            assertEquals(MagazineErrorCode.MAGAZINE_IMAGE_ALREADY_IN_USE, exception.getErrorCode());
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
            when(request.getContent()).thenReturn("Updated Content");
            when(request.getCategory()).thenReturn(MatchingCategory.ACADEMIC);
            when(request.getImageIds()).thenReturn(List.of(1L));

            when(magazineImageRepository.findAllById(any())).thenReturn(List.of(mockImage));

            // when
            MagazineResponse response = magazineService.updateMagazine(magazineId, request, userId);

            // then
            assertNotNull(response);
            verify(mockMagazine).update(request.getTitle(), request.getContent(), request.getCategory());
        }

        @Test
        @DisplayName("매거진 수정 실패 - 권한 없음")
        void updateMagazine_AccessDenied() {
            // given
            Long unauthorizedUserId = 2L;
            User unauthorizedUser = mock(User.class);
            when(unauthorizedUser.getId()).thenReturn(unauthorizedUserId);
            when(userService.findUserById(unauthorizedUserId)).thenReturn(unauthorizedUser);
            MagazineUpdateRequest request = mock(MagazineUpdateRequest.class);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineService.updateMagazine(magazineId, request, unauthorizedUserId));
            assertEquals(MagazineErrorCode.MAGAZINE_ACCESS_DENIED, exception.getErrorCode());
        }

        @Test
        @DisplayName("매거진 수정 시 이미지 삭제 처리 성공")
        void updateMagazine_RemoveImages_Success() {
            // given
            MagazineUpdateRequest request = mock(MagazineUpdateRequest.class);
            when(request.getTitle()).thenReturn("Updated Title");
            when(request.getContent()).thenReturn("Updated Content");
            when(request.getCategory()).thenReturn(MatchingCategory.ACADEMIC);
            when(request.getImageIds()).thenReturn(List.of(2L)); // 기존 이미지 빼고 새로운 이미지

            MagazineImage existingImage = mockImage;
            List<MagazineImage> existingImages = new ArrayList<>();
            existingImages.add(existingImage);

            when(mockMagazine.getImages()).thenReturn(existingImages);
            MagazineImage newImage = mock(MagazineImage.class);
            when(newImage.getId()).thenReturn(2L);
            when(magazineImageRepository.findAllById(any())).thenReturn(List.of(newImage));

            // when
            MagazineResponse response = magazineService.updateMagazine(magazineId, request, userId);

            // then
            verify(mockMagazine).removeImage(existingImage);
            verify(magazineImageService).deleteImage(any());
            verify(magazineImageRepository).delete(existingImage);
        }

        @Test
        @DisplayName("이미 다른 매거진에서 사용 중인 이미지 추가 실패")
        void updateMagazine_ImageAlreadyInUse_Failure() {
            // given
            MagazineUpdateRequest request = mock(MagazineUpdateRequest.class);
            when(request.getTitle()).thenReturn("Updated Title");
            when(request.getContent()).thenReturn("Updated Content");
            when(request.getCategory()).thenReturn(MatchingCategory.ACADEMIC);
            when(request.getImageIds()).thenReturn(List.of(2L));

            when(mockMagazine.getImages()).thenReturn(new ArrayList<>());

            MagazineImage newImage = mock(MagazineImage.class);
            when(newImage.getId()).thenReturn(2L);
            Magazine otherMagazine = mock(Magazine.class);
            when(otherMagazine.getId()).thenReturn(999L);
            when(newImage.getMagazine()).thenReturn(otherMagazine);
            when(magazineImageRepository.findAllById(any())).thenReturn(List.of(newImage));

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineService.updateMagazine(magazineId, request, userId));
            assertEquals(MagazineErrorCode.MAGAZINE_IMAGE_ALREADY_IN_USE, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("매거진 삭제 테스트")
    class DeleteMagazineTest {
        @Test
        @DisplayName("매거진 삭제 성공 - 작성자")
        void deleteMagazine_SuccessAuthor() {
            // when
            magazineService.deleteMagazine(magazineId, userId);

            // then
            verify(magazineRepository).delete(mockMagazine);
        }

        @Test
        @DisplayName("매거진 삭제 성공 - 관리자")
        void deleteMagazine_SuccessAdmin() {
            //given
            Long adminUserId = 2L;
            User adminUser = mock(User.class);
            when(adminUser.getId()).thenReturn(adminUserId);
            when(adminUser.getCurrentRole()).thenReturn(RoleType.ROLE_ADMIN);
            when(userService.findUserById(adminUserId)).thenReturn(adminUser);

            // when
            magazineService.deleteMagazine(magazineId, adminUserId);

            // then
            verify(magazineRepository).delete(mockMagazine);
        }

        @Test
        @DisplayName("매거진 삭제 실패 - 권한 없는 사용자")
        void deleteMagazine_AccessDenied() {
            //given
            Long unauthorizedUserId = 2L;
            User unauthorizedUser = mock(User.class);
            when(unauthorizedUser.getId()).thenReturn(unauthorizedUserId);
            when(unauthorizedUser.getCurrentRole()).thenReturn(RoleType.ROLE_USER);
            when(userService.findUserById(unauthorizedUserId)).thenReturn(unauthorizedUser);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineService.deleteMagazine(magazineId, unauthorizedUserId));
            assertEquals(MagazineErrorCode.MAGAZINE_ACCESS_DENIED, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("매거진 조회 테스트")
    class GetMagazineTest {
        @Test
        @DisplayName("매거진 상세 조회 성공")
        void getMagazine_Success() {
            // given
            when(magazineLikeRepository.existsByMagazineAndUser(mockMagazine, mockUser)).thenReturn(true);

            // when
            MagazineDetailResponse response = magazineService.getMagazine(magazineId, userId);

            // then
            assertNotNull(response);
            assertEquals(magazineId, response.getId());
            assertEquals("Test Magazine", response.getTitle());
            assertTrue(response.isAuthor());
            assertTrue(response.isLiked());
        }

        @Test
        @DisplayName("매거진 상세 조회 실패 - 아직 공개 되지 않은 매거진 + 다른 사용자")
        void getMagazine_AccessDenied() {
            // given
            Long otherUserId = 2L;
            User otherUser = mock(User.class);
            when(otherUser.getId()).thenReturn(otherUserId);
            when(userService.findUserById(otherUserId)).thenReturn(otherUser);
            when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PENDING);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> magazineService.getMagazine(magazineId, otherUserId));
            assertEquals(MagazineErrorCode.MAGAZINE_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("매거진 목록 조회 테스트")
    class GetMagazinesTest {
        @Test
        @DisplayName("필터를 적용한 매거진 목록 조회 성공")
        void getMagazines_WithFilters_Success() {
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
    @DisplayName("매거진 조항요 테스트")
    class ToggleLikeTest {
        @Test
        @DisplayName("좋아요 추가 성공")
        void toggleLike_Add() {
            // given
            when(magazineLikeRepository.existsByMagazineAndUser(mockMagazine, mockUser)).thenReturn(false);

            // when
            LikeResponse response = magazineService.toggleLike(magazineId, userId);

            // then
            assertTrue(response.isLiked());
            verify(magazineLikeRepository).save(any(MagazineLike.class));
            verify(mockMagazine).addLike(mockUser);
        }

        @Test
        @DisplayName("좋아요 취소 성공")
        void toggleLike_Remove() {
            // given
            when(magazineLikeRepository.existsByMagazineAndUser(mockMagazine, mockUser)).thenReturn(true);

            // when
            LikeResponse response = magazineService.toggleLike(magazineId, userId);

            // then
            assertFalse(response.isLiked());
            verify(magazineLikeRepository).deleteByMagazineAndUser(mockMagazine, mockUser);
            verify(mockMagazine).removeLike(mockUser);
        }
    }

    @Nested
    @DisplayName("매거진 관리 테스트")
    class ManageMagazineTest {
        @Test
        @DisplayName("매거진 승인 성공")
        void manageMagazine_Approve() {
            // given
            when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PENDING);
            when(magazineRepository.findById(magazineId)).thenReturn(Optional.of(mockMagazine));
            when(magazineRepository.save(mockMagazine)).thenReturn(mockMagazine);

            // when
            MagazineResponse response = magazineService.manageMagazine(magazineId, true);

            // then
            verify(mockMagazine).setStatus(MagazineStatus.PUBLISHED);
            verify(magazineRepository).save(mockMagazine);
        }

        @Test
        @DisplayName("매거진 거절 성공")
        void manageMagazine_Reject() {
            // given
            when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PENDING);
            when(magazineRepository.findById(magazineId)).thenReturn(Optional.of(mockMagazine));
            when(magazineRepository.save(mockMagazine)).thenReturn(mockMagazine);

            // when
            MagazineResponse response = magazineService.manageMagazine(magazineId, false);

            // then
            verify(mockMagazine).setStatus(MagazineStatus.REJECTED);
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
}