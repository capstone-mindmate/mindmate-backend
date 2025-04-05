package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MagazineErrorCode;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineStatus;
import com.mindmate.mindmate_server.magazine.dto.*;
import com.mindmate.mindmate_server.magazine.repository.MagazineRepository;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MagazineServiceImpl implements MagazineService {
    private final UserService userService;

    private final MagazineRepository magazineRepository;

    @Override
    @Transactional
    public MagazineResponse createMagazine(Long userId, MagazineCreateRequest request) {
        User user = userService.findUserById(userId);

        Magazine magazine = Magazine.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .author(user)
                .build();

        magazine.setCategory(request.getCategory());
        magazine.setStatus(MagazineStatus.PENDING);

        Magazine savedMagazine = magazineRepository.save(magazine);
        return MagazineResponse.from(savedMagazine);
    }

    @Override
    @Transactional
    public MagazineResponse updateMagazine(Long magazineId, MagazineUpdateRequest request, Long userId) {
        Magazine magazine = findMagazineById(magazineId);
        User user = userService.findUserById(userId);

        if (!magazine.getAuthor().equals(user)) {
            throw new CustomException(MagazineErrorCode.MAGAZINE_ACCESS_DENIED);
        }

        magazine.update(request.getTitle(), request.getContent(), request.getCategory());

        return MagazineResponse.from(magazine);
    }

    @Override
    @Transactional
    public void deleteMagazine(Long magazineId, Long userId) {
        Magazine magazine = findMagazineById(magazineId);
        User user = userService.findUserById(userId);

        if (!magazine.getAuthor().equals(user) || !user.getCurrentRole().equals(RoleType.ROLE_ADMIN)) {
            throw new CustomException(MagazineErrorCode.MAGAZINE_ACCESS_DENIED);
        }

        magazineRepository.delete(magazine);
    }

    @Override
    public Page<MagazineResponse> getMagazines(Long userId, MagazineSearchFilter filter, Pageable pageable) {
        return magazineRepository.findMagazinesWithFilters(filter, pageable);
    }

    @Override
    public MagazineDetailResponse getMagazine(Long magazineId, Long userId) {
        Magazine magazine = findMagazineById(magazineId);
        User user = userService.findUserById(userId);

//        if (magazine.getMagazineStatus() != MagazineStatus.PUBLISHED) {
//            throw new CustomException(MagazineErrorCode.MAGAZINE_NOT_FOUND);
//        }

        return MagazineDetailResponse.from(magazine, magazine.getAuthor().equals(user));
    }

    @Override
    public Magazine findMagazineById(Long magazineId) {
        return magazineRepository.findById(magazineId)
                .orElseThrow(() -> new CustomException(MagazineErrorCode.MAGAZINE_NOT_FOUND));
    }

    @Override
    @Transactional
    public MagazineResponse publishMagazine(Long magazineId) {
        Magazine magazine = findMagazineById(magazineId);
        magazine.setStatus(MagazineStatus.PUBLISHED);

        return MagazineResponse.from(magazineRepository.save(magazine));
    }

    @Override
    @Transactional
    public MagazineResponse rejectMagazine(Long magazineId) {
        Magazine magazine = findMagazineById(magazineId);
        magazine.setStatus(MagazineStatus.REJECTED);

        // todo: 추가 동작 고려

        return MagazineResponse.from(magazineRepository.save(magazine));
    }

    @Override
    public Page<MagazineResponse> getPendingMagazines(Pageable pageable) {
        Page<Magazine> pendingMagazines = magazineRepository.findByMagazineStatus(MagazineStatus.PENDING, pageable);
        return pendingMagazines.map(MagazineResponse::from);
    }
}
