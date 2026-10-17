package com.billing.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user_company")
public class UserCompanyAccess {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "user_id", nullable = false) public Long userId;
    @Column(name = "company_id", nullable = false) public Long companyId;
}
