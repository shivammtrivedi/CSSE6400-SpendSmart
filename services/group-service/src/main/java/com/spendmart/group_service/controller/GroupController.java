package com.spendmart.group_service.controller;

import com.spendmart.group_service.dto.GroupDetailResponse;
import com.spendmart.group_service.dto.GroupListResponse;
import com.spendmart.group_service.dto.GroupRequest;
import com.spendmart.group_service.dto.GroupResponse;
import com.spendmart.group_service.dto.MemberRequest;
import com.spendmart.group_service.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GroupRequest request) {
        return ResponseEntity.ok(groupService.createGroup(jwt.getSubject(), request.getName()));
    }

    @GetMapping
    public ResponseEntity<GroupListResponse> listGroups(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(groupService.listGroups(jwt.getSubject()));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailResponse> getGroup(
            @PathVariable String groupId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(groupService.getGroup(groupId, jwt.getSubject()));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<Map<String, Object>> addMember(
            @PathVariable String groupId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MemberRequest request) {
        groupService.addMember(groupId, jwt.getSubject(), request.getUser());
        return ResponseEntity.ok(Map.of());
    }

    @DeleteMapping("/{groupId}/members")
    public ResponseEntity<Map<String, Object>> removeMember(
            @PathVariable String groupId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MemberRequest request) {
        groupService.deactivateMember(groupId, jwt.getSubject(), request.getUser());
        return ResponseEntity.ok(Map.of());
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable String groupId,
            @AuthenticationPrincipal Jwt jwt) {
        groupService.deleteGroup(groupId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
