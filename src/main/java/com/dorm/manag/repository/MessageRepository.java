package com.dorm.manag.repository;

import com.dorm.manag.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Find by sender/recipient
    List<Message> findBySenderOrderBySentAtDesc(User sender);

    List<Message> findByRecipientOrderBySentAtDesc(User recipient);

    @Query("SELECT m FROM Message m WHERE (m.sender = :user OR m.recipient = :user) ORDER BY m.sentAt DESC")
    List<Message> findByUserOrderBySentAtDesc(@Param("user") User user);

    // Find by thread
    List<Message> findByThreadIdOrderBySentAtAsc(String threadId);

    List<Message> findByParentMessageOrderBySentAtAsc(Message parentMessage);

    // Find by status
    List<Message> findByStatusOrderBySentAtDesc(MessageStatus status);

    // Find unread messages
    @Query("SELECT m FROM Message m WHERE m.recipient = :user AND m.status IN ('SENT', 'DELIVERED') ORDER BY m.sentAt DESC")
    List<Message> findUnreadByUser(@Param("user") User user);

    // Find messages requiring response
    @Query("SELECT m FROM Message m WHERE m.requiresResponse = true AND m.status IN ('DELIVERED', 'READ') AND m.isFromAdmin = false ORDER BY m.priority DESC, m.sentAt ASC")
    List<Message> findMessagesRequiringResponse();

    // Find overdue messages
    @Query("SELECT m FROM Message m WHERE m.responseDeadline < :currentTime AND m.requiresResponse = true AND m.status NOT IN ('REPLIED', 'RESOLVED') ORDER BY m.priority DESC")
    List<Message> findOverdueMessages(@Param("currentTime") LocalDateTime currentTime);

    // Find by department
    List<Message> findByRecipientDepartmentOrderBySentAtDesc(String department);

    // Find assigned messages
    List<Message> findByAssignedToOrderBySentAtDesc(User assignedTo);

    // Statistics
    @Query("SELECT COUNT(m) FROM Message m WHERE m.status = 'SENT'")
    long countUndeliveredMessages();

    @Query("SELECT COUNT(m) FROM Message m WHERE m.requiresResponse = true AND m.status IN ('DELIVERED', 'READ')")
    long countMessagesNeedingResponse();

    @Query("SELECT m.messageType, COUNT(m) FROM Message m GROUP BY m.messageType")
    List<Object[]> getMessageCountByType();
}