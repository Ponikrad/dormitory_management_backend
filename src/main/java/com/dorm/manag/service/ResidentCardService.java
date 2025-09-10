package com.dorm.manag.service;

import com.dorm.manag.dto.ResidentCardDto;
import com.dorm.manag.entity.ResidentCard;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.ResidentCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResidentCardService {

    private final ResidentCardRepository residentCardRepository;
    private final UserService userService;

    @Transactional
    public ResidentCardDto generateCard(User user) {
        log.info("Generating resident card for user: {}", user.getUsername());

        // Check if user already has an active card
        Optional<ResidentCard> existingCard = residentCardRepository.findByUser(user);
        if (existingCard.isPresent() && existingCard.get().isActive()) {
            // Deactivate the old card
            ResidentCard oldCard = existingCard.get();
            oldCard.setActive(false);
            residentCardRepository.save(oldCard);
            log.info("Deactivated old card for user: {}", user.getUsername());
        }

        // Generate new QR code
        String qrCode = generateUniqueQrCode();

        // Create new card
        ResidentCard residentCard = new ResidentCard();
        residentCard.setUser(user);
        residentCard.setQrCode(qrCode);
        residentCard.setIssuedDate(LocalDateTime.now());
        residentCard.setExpirationDate(LocalDateTime.now().plusYears(1)); // Card valid for 1 year
        residentCard.setActive(true);

        ResidentCard savedCard = residentCardRepository.save(residentCard);

        return convertToDto(savedCard);
    }

    @Transactional(readOnly = true)
    public Optional<ResidentCardDto> getCardByUser(User user) {
        return residentCardRepository.findByUser(user)
                .filter(ResidentCard::isActive)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Optional<ResidentCardDto> getCardByUserId(Long userId) {
        return residentCardRepository.findByUserId(userId)
                .filter(ResidentCard::isActive)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Optional<ResidentCardDto> verifyQrCode(String qrCode) {
        log.info("Verifying QR code: {}", qrCode);

        return residentCardRepository.findByQrCode(qrCode)
                .filter(card -> card.isActive() && !isExpired(card))
                .map(this::convertToDto);
    }

    @Transactional
    public void deactivateCard(Long userId) {
        residentCardRepository.findByUserId(userId)
                .ifPresent(card -> {
                    card.setActive(false);
                    residentCardRepository.save(card);
                    log.info("Deactivated card for user ID: {}", userId);
                });
    }

    @Transactional(readOnly = true)
    public List<ResidentCardDto> getExpiredCards() {
        return residentCardRepository.findExpiredCards(LocalDateTime.now())
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResidentCardDto> getActiveCards() {
        return residentCardRepository.findActiveCards()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countActiveCards() {
        return residentCardRepository.countActiveCards();
    }

    private String generateUniqueQrCode() {
        String qrCode;
        do {
            qrCode = "RC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (residentCardRepository.existsByQrCode(qrCode));

        return qrCode;
    }

    private boolean isExpired(ResidentCard card) {
        return card.getExpirationDate().isBefore(LocalDateTime.now());
    }

    private ResidentCardDto convertToDto(ResidentCard card) {
        ResidentCardDto dto = new ResidentCardDto();
        dto.setId(card.getId());
        dto.setUserId(card.getUser().getId());
        dto.setUserName(card.getUser().getFirstName() + " " + card.getUser().getLastName());
        dto.setUserName(card.getUser().getUsername());
        dto.setRoomNumber(card.getUser().getRoomNumber());
        dto.setQrCode(card.getQrCode());
        dto.setIssuedDate(card.getIssuedDate());
        dto.setExpirationDate(card.getExpirationDate());
        dto.setActive(card.isActive());

        return dto;
    }
}