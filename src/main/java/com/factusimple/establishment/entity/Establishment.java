package com.factusimple.establishment.entity;

import com.factusimple.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Establecimiento de comercio: es el "tenant" de la plataforma. */
@Getter
@Setter
@Entity
@Table(name = "establishments")
public class Establishment extends BaseEntity {

    @Column(nullable = false, length = 180)
    private String name;

    /** NIT/CC sin dígito de verificación. */
    @Column(nullable = false, length = 40)
    private String identification;

    @Column(length = 2)
    private String dv;

    @Column(length = 255)
    private String address;

    @Column(length = 40)
    private String phone;

    @Column(length = 180)
    private String email;

    @Column(name = "municipality_code", length = 10)
    private String municipalityCode;

    /** ID del rango de numeración DIAN configurado en Factus. */
    @Column(name = "numbering_range_id")
    private Integer numberingRangeId;
}
