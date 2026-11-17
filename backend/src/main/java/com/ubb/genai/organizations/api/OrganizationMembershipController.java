package com.ubb.genai.organizations.api;

import com.ubb.genai.organizations.application.OrganizationMembershipService;
import com.ubb.genai.organizations.domain.OrganizationRole;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationMembershipController {

    private final OrganizationMembershipService service;

    public OrganizationMembershipController(OrganizationMembershipService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrganizationCard> browseOrganizations() {
        return service.browseOrganizations();
    }

    @PostMapping("/{organizationId}/join-requests")
    public ResponseEntity<JoinRequestView> requestToJoin(@PathVariable UUID organizationId) {
        JoinRequestView created = service.requestToJoin(organizationId);
        return ResponseEntity.created(URI.create("/api/organizations/" + organizationId + "/join-requests/" + created.id()))
                .body(created);
    }

    @DeleteMapping("/{organizationId}/join-requests/me")
    public ResponseEntity<Void> withdrawRequest(@PathVariable UUID organizationId) {
        service.withdrawRequest(organizationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{organizationId}/join-requests")
    public List<JoinRequestView> listPendingRequests(@PathVariable UUID organizationId) {
        return service.listPendingRequests(organizationId);
    }

    @PostMapping("/{organizationId}/join-requests/{requestId}/approve")
    public ResponseEntity<Void> approveRequest(
            @PathVariable UUID organizationId,
            @PathVariable UUID requestId) {
        service.approveRequest(organizationId, requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{organizationId}/join-requests/{requestId}/reject")
    public ResponseEntity<Void> rejectRequest(
            @PathVariable UUID organizationId,
            @PathVariable UUID requestId) {
        service.rejectRequest(organizationId, requestId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{organizationId}/members")
    public List<MemberView> listMembers(@PathVariable UUID organizationId) {
        return service.listMembers(organizationId);
    }

    @PatchMapping("/{organizationId}/members/{userId}")
    public MemberView changeRole(
            @PathVariable UUID organizationId,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeRoleRequest request) {
        OrganizationRole role = request.role();
        return service.changeRole(organizationId, userId, role);
    }

    @DeleteMapping("/{organizationId}/members/me")
    public ResponseEntity<Void> leaveOrganization(@PathVariable UUID organizationId) {
        service.leaveOrganization(organizationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{organizationId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID organizationId,
            @PathVariable UUID userId) {
        service.removeMember(organizationId, userId);
        return ResponseEntity.noContent().build();
    }
}
