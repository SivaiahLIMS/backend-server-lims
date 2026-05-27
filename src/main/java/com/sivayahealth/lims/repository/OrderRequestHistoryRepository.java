package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.OrderRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRequestHistoryRepository extends JpaRepository<OrderRequestHistory, Long> {
    List<OrderRequestHistory> findByOrderRequestIdOrderByChangedAtAsc(Long orderRequestId);
}
