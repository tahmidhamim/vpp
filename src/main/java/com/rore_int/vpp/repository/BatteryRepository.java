package com.rore_int.vpp.repository;

import com.rore_int.vpp.entity.Battery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BatteryRepository extends JpaRepository<Battery, Long> {
    @Query("SELECT b FROM Battery b WHERE b.postcode BETWEEN :minPostcode AND :maxPostcode " +
            "AND (:minCapacity IS NULL OR b.capacity >= :minCapacity) " +
            "AND (:maxCapacity IS NULL OR b.capacity <= :maxCapacity) " +
            "ORDER BY b.name")
    List<Battery> findByPostcodeRangeAndCapacity(
            @Param("minPostcode") String minPostcode,
            @Param("maxPostcode") String maxPostcode,
            @Param("minCapacity") Integer minCapacity,
            @Param("maxCapacity") Integer maxCapacity
    );
}