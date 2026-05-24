package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BarcodeService {

    private final ChemicalContainerRepository chemicalContainerRepository;
    private final InstrumentMasterRepository instrumentMasterRepository;
    private final StorageLocationRepository storageLocationRepository;

    public ChemicalContainer scanContainer(Long tenantId, String barcodeValue) {
        return chemicalContainerRepository.findByTenantIdAndBarcodeValue(tenantId, barcodeValue)
                .orElseThrow(() -> LimsException.notFound("No container found for barcode: " + barcodeValue));
    }

    public InstrumentMaster scanInstrument(Long tenantId, String barcodeValue) {
        return instrumentMasterRepository.findByTenant_IdAndBarcodeValue(tenantId, barcodeValue)
                .orElseThrow(() -> LimsException.notFound("No instrument found for barcode: " + barcodeValue));
    }

    public StorageLocation scanLocation(Long tenantId, Long branchId, String barcodeValue) {
        return storageLocationRepository.findByTenantIdAndBranchIdAndCode(tenantId, branchId, barcodeValue)
                .orElseThrow(() -> LimsException.notFound("No storage location found for barcode: " + barcodeValue));
    }

    public Map<String, Object> scanAny(Long tenantId, Long branchId, String barcodeValue) {
        Map<String, Object> result = new HashMap<>();

        Optional<ChemicalContainer> container = chemicalContainerRepository.findByTenantIdAndBarcodeValue(tenantId, barcodeValue);
        if (container.isPresent()) {
            result.put("type", "CONTAINER");
            result.put("data", container.get());
            return result;
        }

        Optional<InstrumentMaster> instrument = instrumentMasterRepository.findByTenant_IdAndBarcodeValue(tenantId, barcodeValue);
        if (instrument.isPresent()) {
            result.put("type", "INSTRUMENT");
            result.put("data", instrument.get());
            return result;
        }

        Optional<StorageLocation> location = storageLocationRepository.findByTenantIdAndBranchIdAndCode(tenantId, branchId, barcodeValue);
        if (location.isPresent()) {
            result.put("type", "LOCATION");
            result.put("data", location.get());
            return result;
        }

        throw LimsException.notFound("No entity found for barcode: " + barcodeValue);
    }
}
