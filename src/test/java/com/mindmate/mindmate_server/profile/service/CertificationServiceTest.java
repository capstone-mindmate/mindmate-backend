package com.mindmate.mindmate_server.profile.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.user.domain.ListenerProfile;
import com.mindmate.mindmate_server.user.domain.Badge;
import com.mindmate.mindmate_server.user.dto.AdminCertificationListResponse;
import com.mindmate.mindmate_server.user.dto.AdminCertificationResponse;
import com.mindmate.mindmate_server.user.dto.CertificationProcessRequest;
import com.mindmate.mindmate_server.user.service.CertificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificationServiceTest {

    @Mock private ListenerRepository listenerRepository;
    @InjectMocks private CertificationServiceImpl certificationService;

    private ListenerProfile listenerProfile;

    @BeforeEach
    void setup() {
        listenerProfile = ListenerProfile.builder()
                .user(null) //test라 필없
                .nickname("listenerProfile")
                .profileImage("jiwon.png")
                .counselingStyle(null)
                .availableTime(LocalDateTime.now())
                .build();
        listenerProfile.updateCertificationDetails("http://certification.url", "경력 설명입니다~");
    }

    @Nested
    @DisplayName("자격 목록 조회 테스트")
    class GetCertificationListTest {
        @Test
        @DisplayName("자격 목록 조회 성공")
        void getCertificationList_Success() {
            // given
            when(listenerRepository.findByCertificationUrlIsNotNull()).thenReturn(List.of(listenerProfile));

            // when
            AdminCertificationListResponse response = certificationService.getCertificationList();

            // then
            assertNotNull(response);
            assertEquals(1, response.getTotalCount());
            List<AdminCertificationResponse> certs = response.getCertifications();
            assertFalse(certs.isEmpty());
            assertEquals("listenerProfile", certs.get(0).getNickname());
        }
    }

    @Nested
    @DisplayName("자격 상세 조회 테스트")
    class GetCertificationDetailTest {
        @Test
        @DisplayName("자격 상세 조회 성공")
        void getCertificationDetail_Success() {
            // given
            when(listenerRepository.findById(anyLong())).thenReturn(Optional.of(listenerProfile));

            // when
            AdminCertificationResponse response = certificationService.getCertificationDetail(1L);

            // then
            assertNotNull(response);
            assertEquals("listenerProfile", response.getNickname());
        }

        @Test
        @DisplayName("자격 상세 조회 실패 - 프로필 미존재")
        void getCertificationDetail_NotFound() {
            // given
            when(listenerRepository.findById(anyLong())).thenReturn(Optional.empty());

            // when & then
            CustomException ex = assertThrows(CustomException.class, () -> certificationService.getCertificationDetail(1L));
            assertEquals(ProfileErrorCode.PROFILE_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("자격 처리 테스트")
    class ProcessCertificationTest {
        @Test
        @DisplayName("자격 승인 처리")
        void processCertification_Approve() {
            // given
            when(listenerRepository.findById(anyLong())).thenReturn(Optional.of(listenerProfile));
            CertificationProcessRequest request = CertificationProcessRequest.builder()
                    .isApproved(true)
                    .build();

            // when
            certificationService.processCertification(1L, request);

            // then
            assertEquals(Badge.EXPERT, listenerProfile.getBadgeStatus());
            assertNull(listenerProfile.getCertificationUrl());
            verify(listenerRepository).save(listenerProfile);
        }

        @Test
        @DisplayName("자격 반려 처리")
        void processCertification_Reject() {
            // given
            listenerProfile.updateCertificationDetails("http://cert.url", "경력 설명");
            when(listenerRepository.findById(anyLong())).thenReturn(Optional.of(listenerProfile));
            CertificationProcessRequest request = CertificationProcessRequest.builder()
                    .isApproved(false)
                    .build();

            // when
            certificationService.processCertification(1L, request);

            // then
            assertNull(listenerProfile.getCertificationUrl());
            verify(listenerRepository).save(listenerProfile);
        }

        @Test
        @DisplayName("자격 처리 실패 - 프로필 미존재")
        void processCertification_NotFound() {
            // given
            when(listenerRepository.findById(anyLong())).thenReturn(Optional.empty());
            CertificationProcessRequest request = CertificationProcessRequest.builder()
                    .isApproved(true)
                    .build();

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> certificationService.processCertification(1L, request));
            assertEquals(ProfileErrorCode.PROFILE_NOT_FOUND, ex.getErrorCode());
        }
    }
}
