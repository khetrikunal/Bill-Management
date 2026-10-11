package com.billing.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "number_sequence")
public class NumberSequence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String seriesKey;
    public long lastValue;
}
