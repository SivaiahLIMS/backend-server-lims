package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.GoodsReceiptLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GoodsReceiptLineRepository extends JpaRepository<GoodsReceiptLine, Long> {
    List<GoodsReceiptLine> findByGoodsReceiptId(Long grnId);
}
