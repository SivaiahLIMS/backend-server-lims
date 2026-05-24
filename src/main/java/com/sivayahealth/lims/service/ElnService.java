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
public class ElnService {

    private final ElnEntryRepository elnEntryRepository;

    public List<ElnEntry> getEntries(Long tenantId, Long branchId) {
        return elnEntryRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    public List<ElnEntry> getEntriesByWorksheet(Long worksheetId) {
        return elnEntryRepository.findByWorksheetExecution_Id(worksheetId);
    }

    public ElnEntry getEntry(Long id) {
        return elnEntryRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("ELN entry not found: " + id));
    }

    @Transactional
    public ElnEntry createEntry(ElnEntry entry) {
        entry.setCreatedAt(LocalDateTime.now());
        return elnEntryRepository.save(entry);
    }

    @Transactional
    public ElnEntry updateEntry(Long id, ElnEntry update) {
        ElnEntry existing = getEntry(id);
        existing.setTitle(update.getTitle());
        existing.setNotes(update.getNotes());
        existing.setAttachments(update.getAttachments());
        existing.setTags(update.getTags());
        existing.setUpdatedAt(LocalDateTime.now());
        return elnEntryRepository.save(existing);
    }

    @Transactional
    public void deleteEntry(Long id) {
        elnEntryRepository.deleteById(id);
    }
}
