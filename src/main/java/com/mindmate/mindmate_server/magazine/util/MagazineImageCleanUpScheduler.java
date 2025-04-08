package com.mindmate.mindmate_server.magazine.util;

import com.mindmate.mindmate_server.magazine.service.MagazineImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MagazineImageCleanUpScheduler {
    private final MagazineImageService magazineImageService;

    @Scheduled(cron = "0 0 3 * * ?") // 새벽 3시에 처리
    public void cleanupUnusedImages() {
        magazineImageService.deleteUnusedImages();
    }
}
