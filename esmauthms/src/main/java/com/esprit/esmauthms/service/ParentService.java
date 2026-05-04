package com.esprit.esmauthms.service;

import com.esprit.esmauthms.dto.ParentCreateRequest;
import com.esprit.esmauthms.dto.ParentUpdateRequest;
import com.esprit.esmauthms.dto.UserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ParentService {

    UserResponseDto createParent(ParentCreateRequest request);

    Page<UserResponseDto> listParents(Pageable pageable);

    UserResponseDto getParent(UUID parentId);

    UserResponseDto updateParent(UUID parentId, ParentUpdateRequest request);

    void deleteParent(UUID parentId);

    UserResponseDto addChild(UUID parentId, UUID childId);

    void removeChild(UUID parentId, UUID childId);

    List<UserResponseDto> getChildren(UUID parentId);
}
