package com.esprit.esmauthms.service;

import com.esprit.esmauthms.dto.*;
import com.esprit.esmauthms.entity.User;
import com.esprit.esmauthms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private UserResponseDto toDto(User user) {
        UserResponseDto.UserResponseDtoBuilder builder = UserResponseDto.builder()
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
                .deletedAt(user.getDeletedAt());

        // 🆕 Mappe la classe si l'étudiant en a une
        if (user.getStudentClass() != null) {
            builder
                    .classId(user.getStudentClass().getId())
                    .className(user.getStudentClass().getName())
                    .classLevel(user.getStudentClass().getLevel())
                    .classSpecialty(user.getStudentClass().getSpecialty());
        }

        return builder.build();
    }

    @Override
    public UserResponseDto createUser(UserCreateRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .cin(request.getCin())
                .role(request.getRole() != null ? request.getRole() : "USER")
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .status(request.getStatus())
                .isEmailVerified(false)
                .twoFactorEnabled(false)
                .build();

        User saved = userRepository.save(user);
        return toDto(saved);
    }

    @Override
    public UserResponseDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDto(user);
    }

    @Override
    public UserResponseDto updateUser(UUID id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getCin() != null)         user.setCin(request.getCin());
        if (request.getEmail() != null)       user.setEmail(request.getEmail());
        if (request.getFirstName() != null)   user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)    user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null)     user.setAddress(request.getAddress());
        if (request.getStatus() != null)      user.setStatus(request.getStatus());
        if (request.getRole() != null)        user.setRole(request.getRole());

        User saved = userRepository.save(user);
        return toDto(saved);
    }

    @Override
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public Page<UserResponseDto> searchUsers(UserSearchCriteria criteria, Pageable pageable) {
        String email     = criteria.getEmail()       != null ? criteria.getEmail()       : "";
        String firstName = criteria.getFirstName()   != null ? criteria.getFirstName()   : "";
        String lastName  = criteria.getLastName()    != null ? criteria.getLastName()    : "";
        String cin       = criteria.getCin()         != null ? criteria.getCin()         : "";
        String phone     = criteria.getPhoneNumber() != null ? criteria.getPhoneNumber() : "";

        Page<User> page = userRepository
                .findByEmailContainingIgnoreCaseAndFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCaseAndCinContainingAndPhoneNumberContaining(
                        email, firstName, lastName, cin, phone, pageable);

        return new PageImpl<>(
                page.getContent().stream().map(this::toDto).collect(Collectors.toList()),
                pageable,
                page.getTotalElements()
        );
    }

    @Override
    public UserResponseDto updateCurrentUser(UUID currentUserId, UserSelfUpdateRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getCin() != null)         user.setCin(request.getCin());
        if (request.getEmail() != null)       user.setEmail(request.getEmail());
        if (request.getFirstName() != null)   user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)    user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null)     user.setAddress(request.getAddress());
        if (request.getStatus() != null)      user.setStatus(request.getStatus());

        User saved = userRepository.save(user);
        return toDto(saved);
    }

    @Override
    public UserResponseDto updateCurrentUserAvatar(UUID currentUserId, String avatarUrl) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAvatarUrl(avatarUrl);
        User saved = userRepository.save(user);
        return toDto(saved);
    }
}