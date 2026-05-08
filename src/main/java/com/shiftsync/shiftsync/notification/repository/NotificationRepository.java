package com.shiftsync.shiftsync.notification.repository;

import com.shiftsync.shiftsync.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * The interface Notification repository.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find by user id order by created at desc page.
     *
     * @param userId   the user id
     * @param pageable the pageable
     * @return the page
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find by user id and read false order by created at desc page.
     *
     * @param userId   the user id
     * @param pageable the pageable
     * @return the page
     */
    Page<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Count by user id and read false long.
     *
     * @param userId the user id
     * @return the long
     */
    long countByUserIdAndReadFalse(Long userId);

    /**
     * Mark all as read.
     *
     * @param userId the user id
     * @param now    the now
     */
    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = :now where n.userId = :userId and n.read = false")
    void markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}