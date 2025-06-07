package com.erp.admin.service;

import com.erp.admin.model.*;
import com.erp.admin.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class LeaveService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveService.class);

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private CompanyHolidayRepository companyHolidayRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    
    @Transactional
    public LeaveRequest applyLeave(LeaveRequest leaveRequest) throws Exception {
        // Validate user exists
        UserProfile user = userProfileRepository.findByEmployeeSerialNumber(leaveRequest.getUserProfile().getEmployeeSerialNumber())
                .orElseThrow(() -> new Exception("User not found"));

        // Check for overlapping leaves
        List<LeaveRequest> overlappingLeaves = leaveRequestRepository.findOverlappingLeaves(
                user.getEmployeeSerialNumber(), leaveRequest.getStartDate(), leaveRequest.getEndDate());

        if (!overlappingLeaves.isEmpty()) {
            throw new Exception("Leave dates overlap with existing leave application");
        }

        // Calculate working days (excluding weekends and holidays)
        int workingDays = calculateWorkingDays(leaveRequest.getStartDate(), leaveRequest.getEndDate());
        leaveRequest.setTotalDays(workingDays);

        // Check leave balance
        if (!hasSufficientBalance(user.getEmployeeSerialNumber(), leaveRequest.getLeaveType(), workingDays)) {
            throw new Exception("Insufficient leave balance");
        }

        leaveRequest.setUserProfile(user);
        return leaveRequestRepository.save(leaveRequest);
    }

 
    @Transactional
public LeaveRequest approveLeave(Long requestId, String approvedBy) throws Exception {
    LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
            .orElseThrow(() -> new Exception("Leave request not found"));

    if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
        throw new Exception("Leave request is not in pending status");
    }

    // Deduct balance FIRST, before updating status
    LeaveBalance updatedBalance = deductLeaveBalance(
        leaveRequest.getUserProfile().getEmployeeSerialNumber(),
        leaveRequest.getLeaveType(),
        leaveRequest.getTotalDays()
    );
    
    if (updatedBalance == null) {
        throw new Exception("Failed to deduct leave balance");
    }

    // Only update status if deduction was successful
    leaveRequest.setStatus(LeaveStatus.APPROVED);
    leaveRequest.setApprovedBy(approvedBy);
    leaveRequest.setApprovedDate(LocalDate.now());

    return leaveRequestRepository.save(leaveRequest);
}

// public LeaveRequest approveLeave(Long requestId, String approvedBy) throws Exception {
//     LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
//             .orElseThrow(() -> new Exception("Leave request not found"));

//     if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
//         throw new Exception("Leave request is not in pending status");
//     }

//     leaveRequest.setStatus(LeaveStatus.APPROVED);
//     leaveRequest.setApprovedBy(approvedBy);
//     leaveRequest.setApprovedDate(LocalDate.now());

//     // *** Deduct balance here ***
//     String employeeSerial = leaveRequest.getUserProfile().getEmployeeSerialNumber();
//     LeaveType leaveType = leaveRequest.getLeaveType();
//     int days = leaveRequest.getTotalDays();
//     deductLeaveBalance(employeeSerial, leaveType, days);

//     return leaveRequestRepository.save(leaveRequest);
// }


    @Transactional
    public LeaveRequest rejectLeave(Long requestId, String rejectionReason) throws Exception {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Leave request not found"));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new Exception("Leave request is not in pending status");
        }

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setRejectionReason(rejectionReason);

        return leaveRequestRepository.save(leaveRequest);
    }

    public List<LeaveRequest> getUserLeaveHistory(String employeeSerialNumber) {
        return leaveRequestRepository.findByUserProfile_EmployeeSerialNumberOrderByAppliedDateDesc(employeeSerialNumber);
    }

    public List<LeaveRequest> getPendingLeaveRequests() {
        return leaveRequestRepository.findByStatusOrderByAppliedDateDesc(LeaveStatus.PENDING);
    }

    public List<LeaveRequest> getTeamLeaveRequests(String reportingOfficer) {
        return leaveRequestRepository.findByReportingOfficer(reportingOfficer);
    }

    public LeaveBalance getLeaveBalance(String employeeSerialNumber, Integer year) {
        Optional<LeaveBalance> balance = leaveBalanceRepository.findByUserProfile_EmployeeSerialNumberAndYear(employeeSerialNumber, year);

        if (balance.isEmpty()) {
            // Create new balance for the year
            UserProfile user = userProfileRepository.findByEmployeeSerialNumber(employeeSerialNumber).orElse(null);
            if (user != null) {
                LeaveBalance newBalance = new LeaveBalance();
                newBalance.setUserProfile(user);
                newBalance.setYear(year);
                return leaveBalanceRepository.save(newBalance);
            }
        }

        return balance.orElse(null);
    }

    public List<LeaveRequest> getCalendarLeaves(LocalDate startDate, LocalDate endDate) {
        return leaveRequestRepository.findApprovedLeavesInDateRange(startDate, endDate);
    }

    public List<CompanyHoliday> getHolidays(Integer year) {
        return companyHolidayRepository.findByYearAndStatusOrderByHolidayDate(year, 'A');
    }

    private int calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        int workingDays = 0;
        LocalDate current = startDate;

        // Get holidays in the date range
        List<CompanyHoliday> holidays = companyHolidayRepository.findHolidaysInRange(startDate, endDate);

        while (!current.isAfter(endDate)) {
            // Skip weekends (Saturday and Sunday)
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY &&
                    current.getDayOfWeek() != DayOfWeek.SUNDAY) {

                // Check if it's not a holiday
                final LocalDate currentDate = current;
                boolean isHoliday = holidays.stream()
                        .anyMatch(holiday -> holiday.getHolidayDate().equals(currentDate));

                if (!isHoliday) {
                    workingDays++;
                }
            }
            current = current.plusDays(1);
        }

        return workingDays;
    }

    private boolean hasSufficientBalance(String employeeSerialNumber, LeaveType leaveType, int requestedDays) {
        LeaveBalance balance = getLeaveBalance(employeeSerialNumber, LocalDate.now().getYear());
        if (balance == null) return false;

        return switch (leaveType) {
            case CASUAL -> balance.getCasualLeaveBalance() >= requestedDays;
            case SICK -> balance.getSickLeaveBalance() >= requestedDays;
            case LEAVE_WITH_PAY -> balance.getLeaveWithPayBalance() >= requestedDays;
            case LEAVE_WITHOUT_PAY -> balance.getLeaveWithoutPayBalance() >= requestedDays;
            default -> false;
        };
    }

    // Change private to public and add return type
@Transactional
public LeaveBalance deductLeaveBalance(String employeeSerialNumber, LeaveType leaveType, int days) {
    Optional<LeaveBalance> balanceOpt = leaveBalanceRepository.findByUserProfile_EmployeeSerialNumberAndYear(
        employeeSerialNumber, LocalDate.now().getYear());
    
    if (balanceOpt.isPresent()) {
        LeaveBalance balance = balanceOpt.get();
        
        logger.info("Before deduction - Employee: {}, Type: {}, Current Balance: {}", 
                employeeSerialNumber, leaveType, getCurrentBalance(balance, leaveType));
        
        switch (leaveType) {
            case CASUAL -> balance.setCasualLeaveBalance(balance.getCasualLeaveBalance() - days);
            case SICK -> balance.setSickLeaveBalance(balance.getSickLeaveBalance() - days);
            case LEAVE_WITH_PAY -> balance.setLeaveWithPayBalance(balance.getLeaveWithPayBalance() - days);
            case LEAVE_WITHOUT_PAY -> balance.setLeaveWithoutPayBalance(balance.getLeaveWithoutPayBalance() - days);
        }
        
        // Force flush to ensure the change is persisted
        LeaveBalance savedBalance = leaveBalanceRepository.saveAndFlush(balance);
        
        logger.info("After deduction - Employee: {}, Type: {}, New Balance: {}", 
                employeeSerialNumber, leaveType, getCurrentBalance(savedBalance, leaveType));
        
        return savedBalance;
    }
    logger.warn("No leave balance found for employee: {} and year: {}", employeeSerialNumber, LocalDate.now().getYear());
    return null;
}
    // Add these methods to your existing LeaveService class

// Add this helper method to your LeaveService class
private int getCurrentBalance(LeaveBalance balance, LeaveType leaveType) {
    return switch (leaveType) {
        case CASUAL -> balance.getCasualLeaveBalance();
        case SICK -> balance.getSickLeaveBalance();
        case LEAVE_WITH_PAY -> balance.getLeaveWithPayBalance();
        case LEAVE_WITHOUT_PAY -> balance.getLeaveWithoutPayBalance();
        default -> 0;
    };
}


@Transactional
public LeaveBalance deleteLeaveRequest(Long requestId) throws Exception {
    LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
            .orElseThrow(() -> new Exception("Leave request not found"));

    String employeeSerialNumber = leaveRequest.getUserProfile().getEmployeeSerialNumber();
    int year = leaveRequest.getAppliedDate().getYear();

    // If the leave was approved, restore the balance
    if (leaveRequest.getStatus() == LeaveStatus.APPROVED) {
        restoreLeaveBalance(employeeSerialNumber,
                leaveRequest.getLeaveType(),
                leaveRequest.getTotalDays());
    }

    leaveRequestRepository.delete(leaveRequest);

    // Return the updated balance
    return getLeaveBalance(employeeSerialNumber, year);
}

public LeaveRequest getLeaveRequestById(Long requestId) {
    return leaveRequestRepository.findById(requestId).orElse(null);
}

private void restoreLeaveBalance(String employeeSerialNumber, LeaveType leaveType, int days) {
    LeaveBalance balance = getLeaveBalance(employeeSerialNumber, LocalDate.now().getYear());
    if (balance != null) {
        switch (leaveType) {
            case CASUAL -> balance.setCasualLeaveBalance(Math.min(12, balance.getCasualLeaveBalance() + days));
            case SICK -> balance.setSickLeaveBalance(Math.min(12, balance.getSickLeaveBalance() + days));
            case LEAVE_WITH_PAY -> balance.setLeaveWithPayBalance(Math.min(12, balance.getLeaveWithPayBalance() + days));
            case LEAVE_WITHOUT_PAY -> balance.setLeaveWithoutPayBalance(Math.min(12, balance.getLeaveWithoutPayBalance() + days));
        }
        leaveBalanceRepository.save(balance);
    }
}
}