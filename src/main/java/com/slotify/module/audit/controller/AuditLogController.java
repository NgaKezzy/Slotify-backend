package com.slotify.module.audit.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.common.api.PageResponse;
import com.slotify.module.audit.dto.AuditLogResponse;
import com.slotify.module.audit.entity.AuditAction;
import com.slotify.module.audit.service.AuditService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only access to the audit trail for salon owners and the platform admin. */
@Tag(name = "Audit log", description = "Who changed what, per salon or platform-wide")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AuditLogController {

  private final AuditService auditService;

  @Operation(summary = "Audit trail of one of my salons")
  @GetMapping("/salons/{salonId}/audit-logs")
  @PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
  public ApiResponse<PageResponse<AuditLogResponse>> salonLogs(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String entity,
      @RequestParam(required = false) AuditAction action) {
    return ApiResponse.ok(
        auditService.listForSalon(
            salonId, principal, entity, action, AuditService.pageable(page, size)));
  }

  @Operation(summary = "Platform-wide audit trail")
  @GetMapping("/platform/audit-logs")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<PageResponse<AuditLogResponse>> platformLogs(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String entity,
      @RequestParam(required = false) AuditAction action) {
    return ApiResponse.ok(auditService.listAll(entity, action, AuditService.pageable(page, size)));
  }
}
