package com.erp.admin.repository;


import com.erp.admin.model.LeaveStatus;
import com.erp.admin.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.erp.admin.model.LeaveRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import com.erp.admin.model.LeaveType;
import java.util.Optional;
import java.util.*;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    
    List<LeaveRequest> findByUserProfile_EmployeeSerialNumberOrderByAppliedDateDesc(String employeeSerialNumber);
    
    List<LeaveRequest> findByUserProfile_EmployeeSerialNumberAndStatus(String employeeSerialNumber, LeaveStatus status);
    
    List<LeaveRequest> findByStatusOrderByAppliedDateDesc(LeaveStatus status);
    
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.userProfile.employeeSerialNumber = :employeeSerialNumber AND " +
           "((lr.startDate BETWEEN :startDate AND :endDate) OR " +
           "(lr.endDate BETWEEN :startDate AND :endDate) OR " +
           "(lr.startDate <= :startDate AND lr.endDate >= :endDate)) AND " +
           "lr.status IN ('APPROVED', 'PENDING')")
    List<LeaveRequest> findOverlappingLeaves(@Param("employeeSerialNumber") String employeeSerialNumber,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
    
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.userProfile.employeeSerialNumber = :employeeSerialNumber AND " +
           "lr.leaveType = :leaveType AND YEAR(lr.startDate) = :year AND lr.status = 'APPROVED'")
    List<LeaveRequest> findApprovedLeavesByTypeAndYear(@Param("employeeSerialNumber") String employeeSerialNumber,
                                                       @Param("leaveType") LeaveType leaveType,
                                                       @Param("year") Integer year);
    
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.startDate BETWEEN :startDate AND :endDate " +
           "AND lr.status = 'APPROVED' ORDER BY lr.startDate")
    List<LeaveRequest> findApprovedLeavesInDateRange(@Param("startDate") LocalDate startDate, 
                                                    @Param("endDate") LocalDate endDate);
    
    // For managers to see their team's leave requests
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.userProfile.reportingOfficer = :reportingOfficer " +
           "ORDER BY lr.appliedDate DESC")
    List<LeaveRequest> findByReportingOfficer(@Param("reportingOfficer") String reportingOfficer);
}