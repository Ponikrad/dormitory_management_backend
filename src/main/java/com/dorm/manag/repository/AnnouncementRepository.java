package com.dorm.manag.repository;

import com.dorm.manag.entity.Announcement;
import com.dorm.manag.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // Find published announcements
    @Query("SELECT a FROM Announcement a WHERE a.isActive = true AND " +
            "(a.publishedAt IS NULL OR a.publishedAt <= :now) AND " +
            "(a.expiresAt IS NULL OR a.expiresAt > :now) AND " +
            "(a.scheduledFor IS NULL OR a.scheduledFor <= :now) " +
            "ORDER BY a.isPinned DESC, a.priority DESC, a.publishedAt DESC")
    List<Announcement> findPublishedAnnouncements(@Param("now") LocalDateTime now);

    // Find pinned announcements
    @Query("SELECT a FROM Announcement a WHERE a.isPinned = true AND a.isActive = true ORDER BY a.priority DESC, a.publishedAt DESC")
    List<Announcement> findPinnedAnnouncements();

    // Find by type
    List<Announcement> findByTypeOrderByPublishedAtDesc(Announcement.AnnouncementType type);

    // Find by priority
    List<Announcement> findByPriorityOrderByPublishedAtDesc(Announcement.AnnouncementPriority priority);

    // Find urgent announcements
    List<Announcement> findByIsUrgentAndIsActiveOrderByPublishedAtDesc(Boolean isUrgent, Boolean isActive);

    // Find by author
    List<Announcement> findByAuthorOrderByCreatedAtDesc(User author);

    // Find scheduled announcements
    @Query("SELECT a FROM Announcement a WHERE a.scheduledFor > :now AND a.isActive = true ORDER BY a.scheduledFor ASC")
    List<Announcement> findScheduledAnnouncements(@Param("now") LocalDateTime now);

    // Find expired announcements
    @Query("SELECT a FROM Announcement a WHERE a.expiresAt < :now ORDER BY a.expiresAt DESC")
    List<Announcement> findExpiredAnnouncements(@Param("now") LocalDateTime now);

    // Find by target audience
    List<Announcement> findByTargetAudienceAndIsActiveOrderByPublishedAtDesc(String targetAudience, Boolean isActive);

    // Search
    @Query("SELECT a FROM Announcement a WHERE (a.title LIKE %:searchTerm% OR a.content LIKE %:searchTerm%) AND a.isActive = true ORDER BY a.publishedAt DESC")
    List<Announcement> searchAnnouncements(@Param("searchTerm") String searchTerm);

    // Statistics
    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.isActive = true")
    long countActiveAnnouncements();

    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.isPinned = true AND a.isActive = true")
    long countPinnedAnnouncements();
}