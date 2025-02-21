package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.user.dto.AdminCertificationListResponse;
import com.mindmate.mindmate_server.user.dto.AdminCertificationResponse;
import com.mindmate.mindmate_server.user.dto.CertificationProcessRequest;
import org.springframework.transaction.annotation.Transactional;

public interface CertificationService {

    @Transactional(readOnly = true)
    AdminCertificationListResponse getCertificationList();

    @Transactional(readOnly = true)
    AdminCertificationResponse getCertificationDetail(Long listenerId);

    void processCertification(Long listenerId, CertificationProcessRequest request);

}
