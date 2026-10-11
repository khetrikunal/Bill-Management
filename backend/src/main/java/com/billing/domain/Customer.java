package com.billing.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "customer")
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name;
    public String address;
    public String gstin;
    public String email;
    public String phone;
}
