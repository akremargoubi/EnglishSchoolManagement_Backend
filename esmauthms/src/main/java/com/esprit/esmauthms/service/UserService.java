package com.esprit.esmauthms.service;

import com.esprit.esmauthms.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponseDto createUser(UserCreateRequest request);

    UserResponseDto getUserById(UUID id);

    UserResponseDto updateUser(UUID id, UserUpdateRequest request);

    void deleteUser(UUID id);

    Page<UserResponseDto> searchUsers(UserSearchCriteria criteria, Pageable pageable);

    UserResponseDto updateCurrentUser(UUID currentUserId, UserSelfUpdateRequest request);

    UserResponseDto updateCurrentUserAvatar(UUID currentUserId, String avatarUrl);

    UserResponseDto topUpWallet(UUID targetUserId, Double amount);

    UserResponseDto deductWallet(UUID userId, Double amount);

    UserResponseDto setParentEmail(UUID studentId, String parentEmail);
}
