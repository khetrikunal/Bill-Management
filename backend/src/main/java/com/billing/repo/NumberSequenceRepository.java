package com.billing.repo;

import com.billing.domain.NumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface NumberSequenceRepository extends JpaRepository<NumberSequence, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from NumberSequence s where s.seriesKey = :key")
    Optional<NumberSequence> lockByKey(String key);

    @Modifying
    @Query(value = "insert into number_sequence(series_key, last_value) values (:key, 0) on conflict (series_key) do nothing",
            nativeQuery = true)
    void insertIfAbsent(String key);
}
