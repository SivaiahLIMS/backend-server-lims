package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.training.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/training")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Training", description = "Training material and competency management")
public class TrainingController {

    private final TrainingService trainingService;

    @GetMapping("/material")
    @PreAuthorize("hasAuthority('TRAINING_VIEW')")
    @Operation(summary = "List training materials for branch",
               description = "Requires: TRAINING_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<TrainingMaterial>> getMaterials(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(trainingService.getMaterials(u.getTenantId(), branchId));
    }

    @GetMapping("/material/{id}")
    @PreAuthorize("hasAuthority('TRAINING_VIEW')")
    @Operation(summary = "Get training material by ID",
               description = "Requires: TRAINING_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<TrainingMaterial> getMaterial(@PathVariable Long id) {
        return ResponseEntity.ok(trainingService.getMaterial(id));
    }

    @PostMapping("/material")
    @PreAuthorize("hasAuthority('TRAINING_CREATE')")
    @Operation(summary = "Create training material",
               description = "Requires: TRAINING_CREATE. tenantId and branchId are set from JWT and X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TrainingMaterial> createMaterial(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody TrainingMaterial material,
            @AuthenticationPrincipal LimsUserDetails u) {
        material.setTenantId(u.getTenantId());
        material.setBranchId(branchId);
        return ResponseEntity.status(HttpStatus.CREATED).body(trainingService.createMaterial(material));
    }

    @PutMapping("/material/{id}")
    @PreAuthorize("hasAuthority('TRAINING_CREATE')")
    @Operation(summary = "Update training material",
               description = "Requires: TRAINING_CREATE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated"),
        @ApiResponse(responseCode = "404", description = "Not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TrainingMaterial> updateMaterial(
            @PathVariable Long id,
            @RequestBody TrainingMaterial material) {
        return ResponseEntity.ok(trainingService.updateMaterial(id, material));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('TRAINING_ASSIGN')")
    @Operation(summary = "Assign training to a user",
               description = "Requires: TRAINING_ASSIGN. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Assigned"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "404", description = "Training or user not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<UserTrainingRecord> assignTraining(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody AssignTrainingRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                trainingService.assignTraining(
                        body.getTrainingId(),
                        body.getUserId(),
                        body.getAssignedById(),
                        u.getTenantId(),
                        branchId
                )
        );
    }

    @PostMapping("/records/{id}/complete")
    @PreAuthorize("hasAuthority('TRAINING_COMPLETE')")
    @Operation(summary = "Mark training record as completed",
               description = "Requires: TRAINING_COMPLETE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Completed"),
        @ApiResponse(responseCode = "404", description = "Record not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<UserTrainingRecord> completeTraining(
            @PathVariable Long id,
            @RequestBody CompleteTrainingRequest body) {
        return ResponseEntity.ok(trainingService.completeTraining(id, body.getScore(), body.getRemarks()));
    }

    @PostMapping("/records/{id}/approve")
    @PreAuthorize("hasAuthority('TRAINING_APPROVE')")
    @Operation(summary = "Approve training completion",
               description = "Requires: TRAINING_APPROVE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Approved"),
        @ApiResponse(responseCode = "404", description = "Record not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<UserTrainingRecord> approveTraining(
            @PathVariable Long id,
            @RequestBody ApproveTrainingRequest body) {
        return ResponseEntity.ok(trainingService.approveTraining(id, body.getApproverId()));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('TRAINING_VIEW')")
    @Operation(summary = "Get training records for a user",
               description = "Requires: TRAINING_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<UserTrainingRecord>> getUserTraining(@PathVariable Long userId) {
        return ResponseEntity.ok(trainingService.getUserTrainingRecords(userId));
    }
}
