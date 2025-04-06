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
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MagazineServiceImpl implements MagazineService {
    private final UserService userService;
    private final MagazineImageService magazineImageService;

    private final MagazineRepository magazineRepository;
    private final MagazineLikeRepository magazineLikeRepository;
    private final MagazineImageRepository magazineImageRepository;

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

        if (request.getImageIds() != null && !request.getImageIds().isEmpty()) {
            List<MagazineImage> images = magazineImageRepository.findAllById(request.getImageIds());
            for (MagazineImage image : images) {
                magazine.addImage(image);
            }
        }

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

        if (request.getImageIds() != null) {
            List<MagazineImage> existingImages = new ArrayList<>(magazine.getImages());
            Set<Long> newImageIds = new HashSet<>(request.getImageIds());

            // 기존 존재 이미지가 포함되지 않은 경우 -> 삭제
            for (MagazineImage image : existingImages) {
                if (!newImageIds.contains(image.getId())) {
                    magazine.removeImage(image);
                    magazineImageService.deleteImage(image.getStoredName());
                    magazineImageRepository.delete(image);
                }
            }

            // 새 이미지 연결
            List<MagazineImage> newImages = magazineImageRepository.findAllById(request.getImageIds());
            for (MagazineImage image : newImages) {
                if (image.getMagazine() == null) {
                    magazine.addImage(image);
                }
            }
        }

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

        for (MagazineImage image : magazine.getImages()) {
            magazineImageService.deleteImage(image.getStoredName());
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

        boolean isAuthor = magazine.getAuthor().equals(user);
        boolean isLiked = magazineLikeRepository.existsByMagazineAndUser(magazine, user);

        return MagazineDetailResponse.from(magazine, isAuthor, isLiked);

    }

    @Override
    public Magazine findMagazineById(Long magazineId) {
        return magazineRepository.findById(magazineId)
                .orElseThrow(() -> new CustomException(MagazineErrorCode.MAGAZINE_NOT_FOUND));
    }

    @Override
    @Transactional
    public MagazineResponse manageMagazine(Long magazineId, boolean isAccepted) {
        Magazine magazine = findMagazineById(magazineId);

        if (magazine.getMagazineStatus().equals(MagazineStatus.PUBLISHED)) {
            throw new CustomException(MagazineErrorCode.ALREADY_PUBLISHED);
        }

        if (isAccepted) {
            magazine.setStatus(MagazineStatus.PUBLISHED);
        } else {
            magazine.setStatus(MagazineStatus.REJECTED);
            // todo: 거절 시 추가 동작
        }
        return MagazineResponse.from(magazineRepository.save(magazine));
    }

    @Override
    public Page<MagazineResponse> getPendingMagazines(Pageable pageable) {
        Page<Magazine> pendingMagazines = magazineRepository.findByMagazineStatus(MagazineStatus.PENDING, pageable);
        return pendingMagazines.map(MagazineResponse::from);
    }

    // todo: 동시성 처리 고려?
    @Override
    @Transactional
    public LikeResponse toggleLike(Long magazineId, Long userId) {
        Magazine magazine = findMagazineById(magazineId);
        User user = userService.findUserById(userId);

        boolean isLiked = magazineLikeRepository.existsByMagazineAndUser(magazine, user);

        if (isLiked) {
            magazineLikeRepository.deleteByMagazineAndUser(magazine, user);
            magazine.removeLike(user);
            return LikeResponse.of(false, magazine.getLikeCount());
        } else {
            MagazineLike like = MagazineLike.builder()
                    .magazine(magazine)
                    .user(user)
                    .build();
            magazineLikeRepository.save(like);
            magazine.addLike(user);
            return LikeResponse.of(true, magazine.getLikeCount());

        }
    }
}
