package com.auradev.erp.procurement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "purchase_sequences")
public class PurchaseSequence {

    @EmbeddedId
    private PurchaseSequenceId id;

    @Column(name = "last_no", nullable = false)
    private int lastNo;
}
