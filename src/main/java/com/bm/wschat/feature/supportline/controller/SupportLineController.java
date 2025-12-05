package com.bm.wschat.feature.supportline.controller;

import com.bm.wschat.feature.supportline.dto.request.CreateSupportLineRequest;
import com.bm.wschat.feature.supportline.dto.request.UpdateSupportLineRequest;
import com.bm.wschat.feature.supportline.dto.response.SpecialistResponse;
import com.bm.wschat.feature.supportline.dto.response.SupportLineListResponse;
import com.bm.wschat.feature.supportline.dto.response.SupportLineResponse;
import com.bm.wschat.feature.supportline.service.SupportLineService;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support-lines")
@RequiredArgsConstructor
public class SupportLineController {

    private final SupportLineService supportLineService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupportLineResponse>> createLine(
            @Valid @RequestBody CreateSupportLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Support line created successfully",
                        supportLineService.createLine(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupportLineResponse>> getLine(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(supportLineService.getLineById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupportLineListResponse>>> getAllLines() {
        return ResponseEntity.ok(ApiResponse.success(supportLineService.getAllLines()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupportLineResponse>> updateLine(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupportLineRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Support line updated successfully",
                supportLineService.updateLine(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteLine(@PathVariable Long id) {
        supportLineService.deleteLine(id);
        return ResponseEntity.ok(ApiResponse.success("Support line deleted successfully"));
    }

    @PostMapping("/{lineId}/specialists/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupportLineResponse>> addSpecialist(
            @PathVariable Long lineId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Specialist added successfully",
                supportLineService.addSpecialist(lineId, userId)));
    }

    @DeleteMapping("/{lineId}/specialists/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupportLineResponse>> removeSpecialist(
            @PathVariable Long lineId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Specialist removed successfully",
                supportLineService.removeSpecialist(lineId, userId)));
    }

    @GetMapping("/{lineId}/specialists")
    public ResponseEntity<ApiResponse<List<SpecialistResponse>>> getSpecialists(@PathVariable Long lineId) {
        return ResponseEntity.ok(ApiResponse.success(supportLineService.getLineSpecialists(lineId)));
    }

    @GetMapping("/my-lines")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<SupportLineListResponse>>> getMyLines(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(supportLineService.getLinesBySpecialist(user.getId())));
    }
}
