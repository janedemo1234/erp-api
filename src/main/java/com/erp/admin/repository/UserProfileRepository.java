package com.erp.admin.repository;

import com.erp.admin.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    // Find by Employee Serial Number
    Optional<UserProfile> findByEmployeeSerialNumber(String employeeSerialNumber);
    
    // Check if employee serial number exists
    boolean existsByEmployeeSerialNumber(String employeeSerialNumber);
}