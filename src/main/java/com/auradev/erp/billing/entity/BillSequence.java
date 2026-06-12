package com.auradev.erp.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bill_sequences")
public class BillSequence {

    @EmbeddedId
    private BillSequenceId id;

    @Column(name = "last_no", nullable = false)
    private int lastNo;
}
