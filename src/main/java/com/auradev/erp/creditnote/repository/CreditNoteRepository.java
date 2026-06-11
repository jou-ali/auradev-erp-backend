package com.auradev.erp.creditnote.repository;

import com.auradev.erp.creditnote.entity.CreditNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, UUID> {

    Page<CreditNote> findByTenantId(UUID tenantId, Pageable pageable);

    List<CreditNote> findByOriginalBillId(UUID originalBillId);
}
