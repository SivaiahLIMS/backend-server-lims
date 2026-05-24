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
public class StorageService {

    private final StorageLocationRepository storageLocationRepository;
    private final ContainerStorageRepository containerStorageRepository;
    private final ContainerStorageHistoryRepository containerStorageHistoryRepository;
    private final StorageViolationRepository storageViolationRepository;
    private final AppUserRepository appUserRepository;

    public List<StorageLocation> getLocations(Long tenantId, Long branchId) {
        return storageLocationRepository.findByTenantIdAndBranchIdAndActiveTrue(tenantId, branchId);
    }

    public StorageLocation getLocation(Long id) {
        return storageLocationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Storage location not found: " + id));
    }

    @Transactional
    public StorageLocation createLocation(StorageLocation location) {
        return storageLocationRepository.save(location);
    }

    @Transactional
    public StorageLocation updateLocation(Long id, StorageLocation update) {
        StorageLocation existing = getLocation(id);
        existing.setName(update.getName());
        existing.setTempMin(update.getTempMin());
        existing.setTempMax(update.getTempMax());
        existing.setHumidityMin(update.getHumidityMin());
        existing.setHumidityMax(update.getHumidityMax());
        existing.setCapacity(update.getCapacity());
        existing.setReservedZone(update.isReservedZone());
        return storageLocationRepository.save(existing);
    }

    @Transactional
    public ContainerStorage placeContainer(Long containerId, Long locationId, Long userId) {
        StorageLocation location = getLocation(locationId);
        AppUser user = appUserRepository.findById(userId).orElse(null);

        containerStorageRepository.findByContainerId(containerId).ifPresent(existing -> {
            ContainerStorageHistory history = ContainerStorageHistory.builder()
                    .tenantId(existing.getTenantId())
                    .branchId(existing.getBranchId())
                    .containerId(containerId)
                    .fromLocation(existing.getLocation())
                    .toLocation(location)
                    .movedBy(user)
                    .movedAt(LocalDateTime.now())
                    .reason("Container relocated")
                    .build();
            containerStorageHistoryRepository.save(history);
            containerStorageRepository.delete(existing);
        });

        ContainerStorage placement = ContainerStorage.builder()
                .tenantId(location.getTenantId())
                .branchId(location.getBranchId())
                .containerId(containerId)
                .location(location)
                .placedBy(user)
                .placedAt(LocalDateTime.now())
                .build();
        return containerStorageRepository.save(placement);
    }

    @Transactional
    public ContainerStorage moveContainer(Long containerId, Long newLocationId, Long userId, String reason) {
        ContainerStorage current = containerStorageRepository.findByContainerId(containerId)
                .orElseThrow(() -> LimsException.notFound("Container not currently placed"));
        StorageLocation newLocation = getLocation(newLocationId);
        AppUser user = appUserRepository.findById(userId).orElse(null);

        ContainerStorageHistory history = ContainerStorageHistory.builder()
                .tenantId(current.getTenantId())
                .branchId(current.getBranchId())
                .containerId(containerId)
                .fromLocation(current.getLocation())
                .toLocation(newLocation)
                .movedBy(user)
                .movedAt(LocalDateTime.now())
                .reason(reason)
                .build();
        containerStorageHistoryRepository.save(history);

        current.setLocation(newLocation);
        current.setPlacedAt(LocalDateTime.now());
        return containerStorageRepository.save(current);
    }

    public List<ContainerStorageHistory> getContainerHistory(Long containerId) {
        return containerStorageHistoryRepository.findByContainerIdOrderByMovedAtDesc(containerId);
    }

    public List<StorageViolation> getViolations(Long tenantId, Long branchId) {
        return storageViolationRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    public List<StorageViolation> getOpenViolations(Long tenantId, Long branchId) {
        return storageViolationRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, "OPEN");
    }

    @Transactional
    public StorageViolation createViolation(StorageViolation violation) {
        return storageViolationRepository.save(violation);
    }

    @Transactional
    public StorageViolation resolveViolation(Long id, Long userId) {
        StorageViolation violation = storageViolationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Violation not found: " + id));
        AppUser user = appUserRepository.findById(userId).orElse(null);
        violation.setStatus("RESOLVED");
        violation.setResolvedAt(LocalDateTime.now());
        violation.setResolvedBy(user);
        return storageViolationRepository.save(violation);
    }
}
