package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
@Tag(name = "Training", description = "Training material and competency management")
public class TrainingController {

    private final TrainingService trainingService;

    @GetMapping("/material")
    @Operation(summary = "List training materials")
    public List<TrainingMaterial> getMaterials(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Branch-Id") Long branchId) {
        return trainingService.getMaterials(tenantId, branchId);
    }

    @GetMapping("/material/{id}")
    @Operation(summary = "Get training material by ID")
    public TrainingMaterial getMaterial(@PathVariable Long id) {
        return trainingService.getMaterial(id);
    }

    @PostMapping("/material")
    @Operation(summary = "Create training material")
    public ResponseEntity<TrainingMaterial> createMaterial(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody TrainingMaterial material) {
        material.setTenantId(tenantId);
        material.setBranchId(branchId);
        return ResponseEntity.status(201).body(trainingService.createMaterial(material));
    }

    @PutMapping("/material/{id}")
    @Operation(summary = "Update training material")
    public TrainingMaterial updateMaterial(@PathVariable Long id, @RequestBody TrainingMaterial material) {
        return trainingService.updateMaterial(id, material);
    }

    @PostMapping("/assign")
    @Operation(summary = "Assign training to a user")
    public ResponseEntity<UserTrainingRecord> assignTraining(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody Map<String, Object> body) {
        Long trainingId = Long.valueOf(body.get("trainingId").toString());
        Long userId = Long.valueOf(body.get("userId").toString());
        Long assignedById = Long.valueOf(body.get("assignedById").toString());
        return ResponseEntity.status(201).body(
                trainingService.assignTraining(trainingId, userId, assignedById, tenantId, branchId));
    }

    @PostMapping("/records/{id}/complete")
    @Operation(summary = "Mark training record as completed")
    public UserTrainingRecord completeTraining(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Integer score = body.containsKey("score") ? Integer.valueOf(body.get("score").toString()) : null;
        String remarks = body.containsKey("remarks") ? body.get("remarks").toString() : null;
        return trainingService.completeTraining(id, score, remarks);
    }

    @PostMapping("/records/{id}/approve")
    @Operation(summary = "Approve training completion")
    public UserTrainingRecord approveTraining(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long approverId = Long.valueOf(body.get("approverId").toString());
        return trainingService.approveTraining(id, approverId);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get training records for a user")
    public List<UserTrainingRecord> getUserTraining(@PathVariable Long userId) {
        return trainingService.getUserTrainingRecords(userId);
    }
}
