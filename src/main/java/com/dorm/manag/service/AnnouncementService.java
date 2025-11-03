package com.dorm.manag.service;

import com.dorm.manag.entity.Announcement;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Transactional
    public Announcement createAnnouncement(User author, String title, String content,
            Announcement.AnnouncementType type, Announcement.AnnouncementPriority priority,
            String targetAudience, boolean isPinned, boolean isUrgent) {
        log.info("Creating announcement: {} by {}", title, author.getUsername());

        Announcement announcement = new Announcement(author, title, content, type);
        announcement.setPriority(priority);
        announcement.setTargetAudience(targetAudience != null ? targetAudience : "ALL");
        announcement.setIsPinned(isPinned);
        announcement.setIsUrgent(isUrgent);
        announcement.publish();

        Announcement saved = announcementRepository.save(announcement);
        log.info("Announcement created with ID: {}", saved.getId());

        return saved;
    }

    @Transactional
    public Announcement scheduleAnnouncement(User author, String title, String content,
            Announcement.AnnouncementType type, LocalDateTime scheduledFor) {
        log.info("Scheduling announcement: {} for {}", title, scheduledFor);

        Announcement announcement = new Announcement(author, title, content, type);
        announcement.schedule(scheduledFor);

        return announcementRepository.save(announcement);
    }

    @Transactional(readOnly = true)
    public List<Announcement> getPublishedAnnouncements(User user) {
        List<Announcement> allPublished = announcementRepository.findPublishedAnnouncements(LocalDateTime.now());

        // Filter by target audience
        return allPublished.stream()
                .filter(a -> a.isTargetedTo(user))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Announcement> getPinnedAnnouncements() {
        return announcementRepository.findPinnedAnnouncements();
    }

    @Transactional(readOnly = true)
    public List<Announcement> getAnnouncementsByType(Announcement.AnnouncementType type) {
        return announcementRepository.findByTypeOrderByPublishedAtDesc(type);
    }

    @Transactional(readOnly = true)
    public List<Announcement> getUrgentAnnouncements() {
        return announcementRepository.findByIsUrgentAndIsActiveOrderByPublishedAtDesc(true, true);
    }

    @Transactional(readOnly = true)
    public List<Announcement> getScheduledAnnouncements() {
        return announcementRepository.findScheduledAnnouncements(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<Announcement> searchAnnouncements(String searchTerm) {
        return announcementRepository.searchAnnouncements(searchTerm);
    }

    @Transactional(readOnly = true)
    public Optional<Announcement> getAnnouncementById(Long id) {
        return announcementRepository.findById(id);
    }

    @Transactional
    public Announcement updateAnnouncement(Long id, String title, String content,
            Announcement.AnnouncementType type, Announcement.AnnouncementPriority priority,
            String targetAudience, boolean isPinned, boolean isUrgent, User modifier) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setType(type);
        announcement.setPriority(priority);
        announcement.setTargetAudience(targetAudience);
        announcement.setIsPinned(isPinned);
        announcement.setIsUrgent(isUrgent);
        announcement.setLastModifiedById(modifier.getId());

        log.info("Announcement {} updated by {}", id, modifier.getUsername());
        return announcementRepository.save(announcement);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        announcementRepository.findById(id).ifPresent(announcement -> {
            announcement.incrementViewCount();
            announcementRepository.save(announcement);
        });
    }

    @Transactional
    public void acknowledgeAnnouncement(Long id, User user) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        announcement.incrementAcknowledgmentCount();
        announcementRepository.save(announcement);

        log.info("User {} acknowledged announcement {}", user.getUsername(), id);
    }

    @Transactional
    public void pinAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        announcement.pin();
        announcementRepository.save(announcement);
    }

    @Transactional
    public void unpinAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        announcement.unpin();
        announcementRepository.save(announcement);
    }

    @Transactional
    public void archiveAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        announcement.archive();
        announcementRepository.save(announcement);

        log.info("Announcement {} archived", id);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        announcementRepository.deleteById(id);
        log.info("Announcement {} deleted", id);
    }

    @Transactional(readOnly = true)
    public AnnouncementStatsDto getAnnouncementStatistics() {
        long total = announcementRepository.countActiveAnnouncements();
        long pinned = announcementRepository.countPinnedAnnouncements();

        AnnouncementStatsDto stats = new AnnouncementStatsDto();
        stats.setTotalActive(total);
        stats.setPinned(pinned);

        return stats;
    }

    @lombok.Data
    public static class AnnouncementStatsDto {
        private long totalActive;
        private long pinned;
    }
}