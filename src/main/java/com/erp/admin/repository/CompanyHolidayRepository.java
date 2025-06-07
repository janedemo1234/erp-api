package com.erp.admin.repository;


import java.time.LocalDate;
import java.util.*;
import com.erp.admin.model.CompanyHoliday;

import com.erp.admin.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
    
@Repository
public interface CompanyHolidayRepository extends JpaRepository<CompanyHoliday, Long> {
    
    List<CompanyHoliday> findByYearAndStatusOrderByHolidayDate(Integer year, char status);
    
    List<CompanyHoliday> findByHolidayDateBetweenAndStatus(LocalDate startDate, LocalDate endDate, char status);
    
    @Query("SELECT ch FROM CompanyHoliday ch WHERE ch.holidayDate BETWEEN :startDate AND :endDate " +
           "AND ch.status = 'A' ORDER BY ch.holidayDate")
    List<CompanyHoliday> findHolidaysInRange(@Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);
    
    boolean existsByHolidayDateAndStatus(LocalDate holidayDate, char status);
}

