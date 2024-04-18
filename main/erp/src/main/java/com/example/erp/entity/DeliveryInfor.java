package com.example.erp.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "delivery_infor")
@Getter
@NoArgsConstructor
public class DeliveryInfor {

    @Builder
    public DeliveryInfor(Long deliveryInForId, Date eta, int count, ArrivalCity arrivalCity, Client client, Product product, String remark) {
        this.deliveryInForId = deliveryInForId;
        this.client = client;
        this.product = product;
        this.arrivalCity = arrivalCity;
        this.count = count;
        this.remark = remark;
        this.eta = eta;

    }

    @Id
    @Column(name = "delivery_infor_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryInForId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_city_id")
    private ArrivalCity arrivalCity;

    @Column(name = "count")
    private int count;

    @Column(name = "eta")
    private Date eta;

    @Column(name = "remark")
    @Nullable
    private String remark;


    @OneToMany(fetch = FetchType.LAZY, mappedBy = "deliveryInfor")
    private List<Shipment> shipmentList;
}
