package com.esprit.esmauthms.service;

import com.esprit.esmauthms.dto.ParentCreateRequest;
import com.esprit.esmauthms.dto.ParentUpdateRequest;
import com.esprit.esmauthms.dto.UserResponseDto;
import com.esprit.esmauthms.entity.User;
import com.esprit.esmauthms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ParentServiceImpl implements ParentService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailClient emailClient;

    private static final String PARENT_ROLE = "PARENT";
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    // ===================== CRUD =====================

    @Override
    public UserResponseDto createParent(ParentCreateRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("A user with this email already exists");
        }

        String rawPassword = generatePassword(12);
        String verificationToken = UUID.randomUUID().toString();

        User parent = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(rawPassword))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(PARENT_ROLE)
                .status("ACTIVE")
                .isEmailVerified(false)
                .twoFactorEnabled(false)
                .emailVerificationToken(verificationToken)
                .emailVerificationExpiresAt(LocalDateTime.now().plusDays(1))
                .build();

        User saved = userRepository.save(parent);

        sendParentWelcomeEmail(saved, rawPassword, verificationToken);

        return toDto(saved);
    }

    @Override
    public Page<UserResponseDto> listParents(Pageable pageable) {
        Page<User> page = userRepository.findByRoleAndDeletedAtIsNull(PARENT_ROLE, pageable);
        List<UserResponseDto> dtos = page.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    public UserResponseDto getParent(UUID parentId) {
        User parent = findParentOrThrow(parentId);
        return toDto(parent);
    }

    @Override
    public UserResponseDto updateParent(UUID parentId, ParentUpdateRequest request) {
        User parent = findParentOrThrow(parentId);

        if (request.getFirstName() != null)   parent.setFirstName(request.getFirstName());
        if (request.getLastName() != null)    parent.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) parent.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null)     parent.setAddress(request.getAddress());

        return toDto(userRepository.save(parent));
    }

    @Override
    public void deleteParent(UUID parentId) {
        User parent = findParentOrThrow(parentId);

        // Unlink all children before soft-deleting
        List<User> children = userRepository.findByParentId(parentId);
        children.forEach(child -> {
            child.setParentId(null);
            userRepository.save(child);
        });

        parent.setDeletedAt(LocalDateTime.now());
        userRepository.save(parent);
    }

    // ===================== CHILDREN =====================

    @Override
    public UserResponseDto addChild(UUID parentId, UUID childId) {
        User parent = findParentOrThrow(parentId);
        User child = userRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child user not found: " + childId));

        if (PARENT_ROLE.equals(child.getRole())) {
            throw new RuntimeException("Cannot link a PARENT account as a child");
        }

        child.setParentId(parent.getId());
        // Keep parentEmail in sync for backward compatibility
        child.setParentEmail(parent.getEmail());

        return toDto(userRepository.save(child));
    }

    @Override
    public void removeChild(UUID parentId, UUID childId) {
        findParentOrThrow(parentId);
        User child = userRepository.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child user not found: " + childId));

        if (!parentId.equals(child.getParentId())) {
            throw new RuntimeException("This child is not linked to the specified parent");
        }

        child.setParentId(null);
        child.setParentEmail(null);
        userRepository.save(child);
    }

    @Override
    public List<UserResponseDto> getChildren(UUID parentId) {
        findParentOrThrow(parentId);
        return userRepository.findByParentId(parentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ===================== HELPERS =====================

    private User findParentOrThrow(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parent not found: " + id));
        if (!PARENT_ROLE.equals(user.getRole())) {
            throw new RuntimeException("User " + id + " is not a PARENT account");
        }
        if (user.getDeletedAt() != null) {
            throw new RuntimeException("Parent account has been deleted");
        }
        return user;
    }

    private void sendParentWelcomeEmail(User parent, String rawPassword, String token) {
        String verifyLink = "http://localhost:4200/verify-email?token=" + token;
        String subject = "Welcome to ESM Platform — Your Parent Account";
        String text = "Hello " + parent.getFirstName() + " " + parent.getLastName() + ",\n\n"
                + "An administrator has created a parent account for you on the ESM Platform.\n\n"
                + "Your login credentials:\n"
                + "  Email:    " + parent.getEmail() + "\n"
                + "  Password: " + rawPassword + "\n\n"
                + "Please verify your email address to activate your account:\n"
                + verifyLink + "\n\n"
                + "This activation link is valid for 24 hours.\n"
                + "We strongly recommend changing your password after your first login.\n\n"
                + "Best regards,\nESM Platform Team";

        try {
            emailClient.sendEmail(parent.getEmail(), subject, text);
        } catch (Exception e) {
            log.error("Failed to send welcome email to parent {}: {}", parent.getEmail(), e.getMessage());
        }
    }

    private String generatePassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .cin(user.getCin())
                .email(user.getEmail())
                .role(user.getRole())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .deletedAt(user.getDeletedAt())
                .walletBalance(user.getWalletBalance())
                .parentEmail(user.getParentEmail())
                .parentId(user.getParentId())
                .build();
    }
}
