package com.dorm.manag.service;

import com.dorm.manag.entity.Message;
import com.dorm.manag.entity.MessageStatus;
import com.dorm.manag.entity.MessageType;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    @Transactional
    public Message sendMessage(User sender, String subject, String content, MessageType type, User recipient) {
        log.info("User {} sending message: {}", sender.getUsername(), subject);

        Message message = new Message(sender, subject, content, type);

        if (recipient != null) {
            message.setRecipient(recipient);
        }

        Message savedMessage = messageRepository.save(message);
        log.info("Message sent with ID: {}", savedMessage.getId());

        return savedMessage;
    }

    @Transactional
    public Message sendMessageToAdmin(User sender, String subject, String content, MessageType type) {
        log.info("User {} sending message to admin: {}", sender.getUsername(), subject);

        Message message = new Message(sender, subject, content, type);
        message.setRecipientDepartment("ADMIN");

        return messageRepository.save(message);
    }

    @Transactional
    public Message replyToMessage(Long parentMessageId, User sender, String content) {
        Message parentMessage = messageRepository.findById(parentMessageId)
                .orElseThrow(() -> new RuntimeException("Parent message not found"));

        Message reply = parentMessage.createReply(sender, content);
        Message savedReply = messageRepository.save(reply);

        // Update parent message
        messageRepository.save(parentMessage);

        log.info("Reply sent to message {} by {}", parentMessageId, sender.getUsername());
        return savedReply;
    }

    @Transactional(readOnly = true)
    public List<Message> getInboxMessages(User user) {
        return messageRepository.findByRecipientOrderBySentAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Message> getSentMessages(User user) {
        return messageRepository.findBySenderOrderBySentAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Message> getAllUserMessages(User user) {
        return messageRepository.findByUserOrderBySentAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Message> getUnreadMessages(User user) {
        return messageRepository.findUnreadByUser(user);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessageThread(String threadId) {
        return messageRepository.findByThreadIdOrderBySentAtAsc(threadId);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesRequiringResponse() {
        return messageRepository.findMessagesRequiringResponse();
    }

    @Transactional(readOnly = true)
    public List<Message> getOverdueMessages() {
        return messageRepository.findOverdueMessages(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Optional<Message> getMessageById(Long id) {
        return messageRepository.findById(id);
    }

    @Transactional
    public Message markAsRead(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Check if user is recipient
        if (message.getRecipient() != null && !message.getRecipient().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        message.markAsRead();
        return messageRepository.save(message);
    }

    @Transactional
    public Message markAsResolved(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.markAsResolved();
        return messageRepository.save(message);
    }

    @Transactional
    public Message assignMessage(Long messageId, User assignee) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.assignTo(assignee);
        return messageRepository.save(message);
    }

    @Transactional
    public void deleteMessage(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Check if user owns the message
        if (!message.getSender().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        messageRepository.delete(message);
        log.info("Message {} deleted by user {}", messageId, user.getUsername());
    }

    @Transactional(readOnly = true)
    public MessageStatsDto getMessageStatistics() {
        long undelivered = messageRepository.countUndeliveredMessages();
        long needingResponse = messageRepository.countMessagesNeedingResponse();

        MessageStatsDto stats = new MessageStatsDto();
        stats.setUndeliveredMessages(undelivered);
        stats.setMessagesNeedingResponse(needingResponse);

        return stats;
    }

    @lombok.Data
    public static class MessageStatsDto {
        private long undeliveredMessages;
        private long messagesNeedingResponse;
    }
}