package com.spendmart.group_service.controller;

import com.spendmart.group_service.config.SecurityConfig;
import com.spendmart.group_service.dto.GroupDetailResponse;
import com.spendmart.group_service.dto.GroupListResponse;
import com.spendmart.group_service.dto.GroupResponse;
import com.spendmart.group_service.dto.GroupSummary;
import com.spendmart.group_service.service.GroupService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@Import(SecurityConfig.class)
class GroupControllerTest {

    private static final String SUB = "00000000-0000-0000-0000-000000000001";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private GroupService groupService;

    @Test
    void createGroup_returns200_withSnakeCaseBody() throws Exception {
        when(groupService.createGroup(eq(SUB), eq("Trip")))
                .thenReturn(new GroupResponse("g-1", "Trip", SUB, Instant.parse("2026-06-01T12:00:00Z")));

        mockMvc.perform(post("/api/v1/groups")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Trip\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value("g-1"))
                .andExpect(jsonPath("$.name").value("Trip"))
                .andExpect(jsonPath("$.createdBy").value(SUB))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createGroup_returns400_whenNameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGroup_returns401_withoutJwt() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Trip\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listGroups_returns200_withGroupNames() throws Exception {
        when(groupService.listGroups(SUB)).thenReturn(new GroupListResponse(
                List.of(new GroupSummary("g-1", "Trip"), new GroupSummary("g-2", "Flat"))));

        mockMvc.perform(get("/api/v1/groups").with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[0].groupId").value("g-1"))
                .andExpect(jsonPath("$.groups[0].name").value("Trip"))
                .andExpect(jsonPath("$.groups[1].name").value("Flat"));
    }

    @Test
    void getGroup_returns200_withMembers() throws Exception {
        when(groupService.getGroup(eq("g-1"), eq(SUB)))
                .thenReturn(new GroupDetailResponse("Trip", List.of("alice", "bob")));

        mockMvc.perform(get("/api/v1/groups/g-1").with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Trip"))
                .andExpect(jsonPath("$.members[1]").value("bob"));
    }

    @Test
    void getGroup_returns403_whenCallerNotMember() throws Exception {
        when(groupService.getGroup(eq("g-1"), eq(SUB)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not an active member of this group"));

        mockMvc.perform(get("/api/v1/groups/g-1").with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isForbidden());
    }

    @Test
    void addMember_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/groups/g-1/members")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user\":\"" + SUB + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void addMember_returns404_whenUserNotFound() throws Exception {
        doThrow(new EntityNotFoundException("User not found"))
                .when(groupService).addMember(eq("g-1"), eq(SUB), any());

        mockMvc.perform(post("/api/v1/groups/g-1/members")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user\":\"missing\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeMember_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/g-1/members")
                        .with(jwt().jwt(j -> j.subject(SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user\":\"" + SUB + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteGroup_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/g-1")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteGroup_returns403_whenNotOwner() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group owner can delete the group"))
                .when(groupService).deleteGroup(eq("g-1"), eq(SUB));

        mockMvc.perform(delete("/api/v1/groups/g-1")
                        .with(jwt().jwt(j -> j.subject(SUB))))
                .andExpect(status().isForbidden());
    }

    @Test
    void health_returns200_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/groups/health"))
                .andExpect(status().isOk());
    }
}
