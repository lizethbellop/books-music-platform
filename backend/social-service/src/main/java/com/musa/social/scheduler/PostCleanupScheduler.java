package com.musa.social.scheduler;

import com.musa.social.service.PostCleanupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostCleanupScheduler {

    private final PostCleanupService postCleanupService;

    @Scheduled(
            cron = "${musa.social.posts.cleanup-cron:0 0 3 * * *}"
    )
    public void cleanupExpiredPosts() {

        int deletedPosts =
                postCleanupService.deleteExpiredPosts();

        if (deletedPosts > 0) {
            log.info(
                    "Limpieza automática de publicaciones: {} publicaciones eliminadas permanentemente.",
                    deletedPosts
            );
        } else {
            log.debug(
                    "Limpieza automática de publicaciones: no hay publicaciones vencidas."
            );
        }
    }
}
