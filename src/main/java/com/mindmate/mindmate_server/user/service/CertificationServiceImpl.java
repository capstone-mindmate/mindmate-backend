package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.user.domain.ListenerProfile;
import com.mindmate.mindmate_server.user.dto.AdminCertificationListResponse;
import com.mindmate.mindmate_server.user.dto.AdminCertificationResponse;
import com.mindmate.mindmate_server.user.dto.CertificationProcessRequest;
import com.mindmate.mindmate_server.user.repository.ListenerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CertificationServiceImpl implements CertificationService {
    private final ListenerRepository listenerRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminCertificationListResponse getCertificationList() {
        List<ListenerProfile> pendingProfiles = listenerRepository.findByCertificationUrlIsNotNull();

        List<AdminCertificationResponse> responses = pendingProfiles.stream()
                .map(this::buildCertificationResponse)
                .collect(Collectors.toList());

        return AdminCertificationListResponse.builder()
                .certifications(responses)
                .totalCount(responses.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCertificationResponse getCertificationDetail(Long listenerId) {
        ListenerProfile profile = listenerRepository.findById(listenerId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        return buildCertificationResponse(profile);
    }

    @Override
    public void processCertification(Long listenerId, CertificationProcessRequest request) {
        ListenerProfile profile = listenerRepository.findById(listenerId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        if (request.getIsApproved()) { // 승인
            profile.approveCertification(request.getBadgeStatus());
            listenerRepository.save(profile);

            // 승인 알림 전송
        } else { // 반려
            profile.rejectCertification();
            listenerRepository.save(profile);

            // 반려 알림 전송
        }
    }

    private AdminCertificationResponse buildCertificationResponse(ListenerProfile profile) {
        return AdminCertificationResponse.builder()
                .listenerId(profile.getId())
                .nickname(profile.getNickname())
                .certificationUrl(profile.getCertificationUrl())
                .careerDescription(profile.getCareerDescription())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}