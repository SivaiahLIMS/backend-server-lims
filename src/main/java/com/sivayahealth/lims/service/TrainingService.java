package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingMaterialRepository trainingMaterialRepository;
    private final UserTrainingRecordRepository userTrainingRecordRepository;
    private final AppUserRepository appUserRepository;

    public List<TrainingMaterial> getMaterials(Long tenantId, Long branchId) {
        return trainingMaterialRepository.findByTenantIdAndBranchIdAndActiveTrue(tenantId, branchId);
    }

    public TrainingMaterial getMaterial(Long id) {
        return trainingMaterialRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Training material not found: " + id));
    }

    @Transactional
    public TrainingMaterial createMaterial(TrainingMaterial material) {
        return trainingMaterialRepository.save(material);
    }

    @Transactional
    public TrainingMaterial updateMaterial(Long id, TrainingMaterial update) {
        TrainingMaterial existing = getMaterial(id);
        existing.setTitle(update.getTitle());
        existing.setDescription(update.getDescription());
        existing.setCategory(update.getCategory());
        existing.setDurationMins(update.getDurationMins());
        existing.setActive(update.isActive());
        return trainingMaterialRepository.save(existing);
    }

    @Transactional
    public UserTrainingRecord assignTraining(Long trainingId, Long userId, Long assignedById, Long tenantId, Long branchId) {
        TrainingMaterial training = getMaterial(trainingId);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found: " + userId));
        AppUser assignedBy = appUserRepository.findById(assignedById).orElse(null);

        UserTrainingRecord record = UserTrainingRecord.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .user(user)
                .training(training)
                .status("ASSIGNED")
                .assignedAt(LocalDateTime.now())
                .assignedBy(assignedBy)
                .build();
        return userTrainingRecordRepository.save(record);
    }

    @Transactional
    public UserTrainingRecord completeTraining(Long recordId, Integer score, String remarks) {
        UserTrainingRecord record = userTrainingRecordRepository.findById(recordId)
                .orElseThrow(() -> LimsException.notFound("Training record not found: " + recordId));
        record.setStatus("COMPLETED");
        record.setCompletedAt(LocalDateTime.now());
        record.setScore(score);
        record.setRemarks(remarks);
        return userTrainingRecordRepository.save(record);
    }

    @Transactional
    public UserTrainingRecord approveTraining(Long recordId, Long approverId) {
        UserTrainingRecord record = userTrainingRecordRepository.findById(recordId)
                .orElseThrow(() -> LimsException.notFound("Training record not found: " + recordId));
        AppUser approver = appUserRepository.findById(approverId).orElse(null);
        record.setStatus("APPROVED");
        record.setApprovedBy(approver);
        return userTrainingRecordRepository.save(record);
    }

    public List<UserTrainingRecord> getUserTrainingRecords(Long userId) {
        return userTrainingRecordRepository.findByUser_Id(userId);
    }

    public List<UserTrainingRecord> getTenantTrainingRecords(Long tenantId, Long branchId) {
        return userTrainingRecordRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, "ASSIGNED");
    }
}
