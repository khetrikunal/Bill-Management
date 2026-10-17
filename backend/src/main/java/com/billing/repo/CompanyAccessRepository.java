package com.billing.repo;

import com.billing.domain.UserCompanyAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CompanyAccessRepository extends JpaRepository<UserCompanyAccess, Long> {
    @Query(value = "select company_id from app_user_company where user_id = "
            + "(select id from app_user where lower(username) = lower(:username)) order by company_id",
            nativeQuery = true)
    List<Long> findCompanyIdsByUsername(String username);

    @Query(value = "select exists(select 1 from app_user_company a join app_user u on u.id = a.user_id "
            + "where lower(u.username) = lower(:username) and a.company_id = :companyId)", nativeQuery = true)
    boolean hasAccess(String username, Long companyId);

    void deleteByUserId(Long userId);

    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    List<UserCompanyAccess> findByUserIdOrderByCompanyId(Long userId);
}
