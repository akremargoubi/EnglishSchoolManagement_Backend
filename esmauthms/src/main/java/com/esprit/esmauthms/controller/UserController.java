// src/main/java/com/esprit/esmauthms/controller/UserController.java
package com.esprit.esmauthms.controller;

import com.esprit.esmauthms.dto.*;
import com.esprit.esmauthms.service.FileStorageService;
import com.esprit.esmauthms.service.JwtService;
import com.esprit.esmauthms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final JwtService jwtService;

    @PostMapping
    public UserResponseDto createUser(@Valid @RequestBody UserCreateRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/{id}")
    public UserResponseDto getUser(@PathVariable UUID id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public UserResponseDto updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
    }

    // List + pagination + filtering
    @GetMapping
    public Page<UserResponseDto> searchUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String cin,
            @RequestParam(required = false) String phoneNumber,
            Pageable pageable
    ) {
        UserSearchCriteria criteria = new UserSearchCriteria();
        criteria.setRole(role);
        criteria.setEmail(email);
        criteria.setFirstName(firstName);
        criteria.setLastName(lastName);
        criteria.setCin(cin);
        criteria.setPhoneNumber(phoneNumber);

        return userService.searchUsers(criteria, pageable);
    }

    // NEW: Get current user profile
    @GetMapping("/me")
    public UserResponseDto getCurrentUser(HttpServletRequest httpRequest) {
        UUID currentUserId = extractUserIdFromRequest(httpRequest);
        return userService.getUserById(currentUserId);
    }

    // Current user info update (no role)
    @PutMapping("/me")
    public UserResponseDto updateCurrentUser(
            @Valid @RequestBody UserSelfUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID currentUserId = extractUserIdFromRequest(httpRequest);
        return userService.updateCurrentUser(currentUserId, request);
    }

    // Avatar upload for current user
    @PostMapping("/me/avatar")
    public UserResponseDto uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest
    ) {
        UUID currentUserId = extractUserIdFromRequest(httpRequest);
        String url = fileStorageService.storeAvatar(file);
        return userService.updateCurrentUserAvatar(currentUserId, url);
    }

    @PutMapping("/{id}/wallet/topup")
    public UserResponseDto topUpWallet(
            @PathVariable UUID id,
            @Valid @RequestBody WalletRequest request) {
        return userService.topUpWallet(id, request.getAmount());
    }

    @PutMapping("/{id}/wallet/deduct")
    public UserResponseDto deductWallet(
            @PathVariable UUID id,
            @Valid @RequestBody WalletRequest request) {
        return userService.deductWallet(id, request.getAmount());
    }

    @GetMapping("/{id}/wallet")
    public Map<String, Object> getWallet(@PathVariable UUID id) {
        UserResponseDto dto = userService.getUserById(id);
        return Map.of("userId", id.toString(), "walletBalance", dto.getWalletBalance());
    }

    @PutMapping("/{id}/parent-email")
    public UserResponseDto setParentEmail(
            @PathVariable UUID id,
            @Valid @RequestBody ParentLinkRequest request) {
        return userService.setParentEmail(id, request.getParentEmail());
    }

    private UUID extractUserIdFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = header.substring(7);
        return jwtService.extractUserId(token);
    }
}
