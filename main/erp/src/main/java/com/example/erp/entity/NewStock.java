package com.example.erp.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "new_stock")
@Getter
@NoArgsConstructor
public class NewStock {

    @Builder
    public NewStock(Long stockId, Storage storage, Product product, int count, Date stockDate) {
        this.stockId = stockId;
        this.storage = storage;
        this.product = product;
        this.count = count;
        this.stockDate = stockDate;
    }

    @Id
    @Column(name = "stock_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_id")
    private Storage storage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "count")
    private int count;

    @Column(name = "date")
    private Date stockDate;

}
