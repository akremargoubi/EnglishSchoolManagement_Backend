package com.esprit.esmauthms.controller;

import com.esprit.esmauthms.dto.ParentCreateRequest;
import com.esprit.esmauthms.dto.ParentUpdateRequest;
import com.esprit.esmauthms.dto.UserResponseDto;
import com.esprit.esmauthms.service.JwtService;
import com.esprit.esmauthms.service.ParentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin endpoints: /api/parents/**
 * Parent self-service: GET /api/parents/me/children
 */
@RestController
@RequestMapping("/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;
    private final JwtService jwtService;

    // ── Admin: create parent account ──────────────────────────────────────────
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto createParent(@Valid @RequestBody ParentCreateRequest request) {
        return parentService.createParent(request);
    }

    // ── Admin: list all parent accounts (paginated) ───────────────────────────
    @GetMapping
    public Page<UserResponseDto> listParents(Pageable pageable) {
        return parentService.listParents(pageable);
    }

    // ── Parent self: view own children (must come BEFORE /{id} to avoid conflict) ─
    @GetMapping("/me/children")
    public List<UserResponseDto> getMyChildren(HttpServletRequest httpRequest) {
        UUID currentUserId = extractUserIdFromRequest(httpRequest);
        return parentService.getChildren(currentUserId);
    }

    // ── Admin: get single parent ──────────────────────────────────────────────
    @GetMapping("/{id}")
    public UserResponseDto getParent(@PathVariable UUID id) {
        return parentService.getParent(id);
    }

    // ── Admin: update parent info ─────────────────────────────────────────────
    @PutMapping("/{id}")
    public UserResponseDto updateParent(
            @PathVariable UUID id,
            @Valid @RequestBody ParentUpdateRequest request) {
        return parentService.updateParent(id, request);
    }

    // ── Admin: soft-delete parent (unlinks children first) ───────────────────
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteParent(@PathVariable UUID id) {
        parentService.deleteParent(id);
    }

    // ── Admin: link a child to a parent ──────────────────────────────────────
    @PostMapping("/{parentId}/children/{childId}")
    public UserResponseDto addChild(
            @PathVariable UUID parentId,
            @PathVariable UUID childId) {
        return parentService.addChild(parentId, childId);
    }

    // ── Admin: unlink a child from a parent ──────────────────────────────────
    @DeleteMapping("/{parentId}/children/{childId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeChild(
            @PathVariable UUID parentId,
            @PathVariable UUID childId) {
        parentService.removeChild(parentId, childId);
    }

    // ── Admin: list children of a specific parent ─────────────────────────────
    @GetMapping("/{parentId}/children")
    public List<UserResponseDto> getChildren(@PathVariable UUID parentId) {
        return parentService.getChildren(parentId);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private UUID extractUserIdFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        return jwtService.extractUserId(header.substring(7));
    }
}
