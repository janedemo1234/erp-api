package com.erp.admin.repository;

import com.erp.admin.model.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    
    Optional<LeaveBalance> findByUserProfile_EmployeeSerialNumberAndYear(String employeeSerialNumber, Integer year);
    
    List<LeaveBalance> findByUserProfile_EmployeeSerialNumber(String employeeSerialNumber);
    
    // Add these update methods that your service is trying to call
    @Modifying
    @Transactional
    @Query("UPDATE LeaveBalance lb SET lb.casualLeaveBalance = :balance WHERE lb.userProfile.employeeSerialNumber = :employeeSerialNumber AND lb.year = :year")
    int updateCasualLeaveBalance(@Param("employeeSerialNumber") String employeeSerialNumber, 
                                @Param("year") Integer year, 
                                @Param("balance") Integer balance);
    
    @Modifying
    @Transactional
    @Query("UPDATE LeaveBalance lb SET lb.sickLeaveBalance = :balance WHERE lb.userProfile.employeeSerialNumber = :employeeSerialNumber AND lb.year = :year")
    int updateSickLeaveBalance(@Param("employeeSerialNumber") String employeeSerialNumber, 
                              @Param("year") Integer year, 
                              @Param("balance") Integer balance);
    
    @Modifying
    @Transactional
    @Query("UPDATE LeaveBalance lb SET lb.leaveWithPayBalance = :balance WHERE lb.userProfile.employeeSerialNumber = :employeeSerialNumber AND lb.year = :year")
    int updateLeaveWithPayBalance(@Param("employeeSerialNumber") String employeeSerialNumber, 
                                 @Param("year") Integer year, 
                                 @Param("balance") Integer balance);
    
    @Modifying
    @Transactional
    @Query("UPDATE LeaveBalance lb SET lb.leaveWithoutPayBalance = :balance WHERE lb.userProfile.employeeSerialNumber = :employeeSerialNumber AND lb.year = :year")
    int updateLeaveWithoutPayBalance(@Param("employeeSerialNumber") String employeeSerialNumber, 
                                    @Param("year") Integer year, 
                                    @Param("balance") Integer balance);
}
