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

        String employeeSerialNumber = leaveRequest.getUserProfile().getEmployeeSerialNumber();
        LeaveType leaveType = leaveRequest.getLeaveType();
        int totalDays = leaveRequest.getTotalDays();
        int currentYear = LocalDate.now().getYear();

        logger.info("Approving leave for employee: {}, Type: {}, Days: {}", 
                employeeSerialNumber, leaveType, totalDays);

        // Get current balance
        LeaveBalance currentBalance = getLeaveBalance(employeeSerialNumber, currentYear);
        if (currentBalance == null) {
            throw new Exception("Leave balance not found for employee");
        }

        // Check if sufficient balance exists
        int availableBalance = getCurrentBalance(currentBalance, leaveType);
        if (availableBalance < totalDays) {
            throw new Exception("Insufficient leave balance. Available: " + availableBalance + ", Required: " + totalDays);
        }

        // Deduct the balance
        boolean balanceDeducted = deductLeaveBalanceDirectly(currentBalance, leaveType, totalDays);
        if (!balanceDeducted) {
            throw new Exception("Failed to deduct leave balance");
        }

        // Update leave request status
        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setApprovedBy(approvedBy);
        leaveRequest.setApprovedDate(LocalDate.now());

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        
        logger.info("Leave approved successfully for employee: {}", employeeSerialNumber);
        return savedRequest;
    }

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

    @Transactional
    public LeaveBalance getLeaveBalance(String employeeSerialNumber, Integer year) {
        try {
            logger.info("=== Starting getLeaveBalance ===");
            logger.info("Employee Serial Number: {}", employeeSerialNumber);
            logger.info("Year: {}", year);
            
            // First check if balance exists
            logger.info("Checking if leave balance already exists...");
            Optional<LeaveBalance> existingBalance = leaveBalanceRepository.findByUserProfile_EmployeeSerialNumberAndYear(employeeSerialNumber, year);
            
            if (existingBalance.isPresent()) {
                logger.info("Found existing leave balance with ID: {}", existingBalance.get().getBalanceId());
                return existingBalance.get();
            }

            logger.info("No existing balance found, checking if user exists...");
            // If no balance exists, check if user exists first
            Optional<UserProfile> userOpt = userProfileRepository.findByEmployeeSerialNumber(employeeSerialNumber);
            if (userOpt.isEmpty()) {
                logger.error("❌ User not found with employee serial number: {}", employeeSerialNumber);
                return null;
            }

            UserProfile user = userOpt.get();
            logger.info("✅ User found: {} (ID: {})", user.getEmployeeName(), user.getSrNo());

            // Create new balance for the year
            logger.info("Creating new leave balance...");
            LeaveBalance newBalance = new LeaveBalance();
            newBalance.setUserProfile(user);
            newBalance.setYear(year);
            newBalance.setCasualLeaveBalance(12);
            newBalance.setSickLeaveBalance(12);
            newBalance.setLeaveWithPayBalance(12);
            newBalance.setLeaveWithoutPayBalance(12);
            newBalance.setCreatedDate(LocalDate.now());
            newBalance.setUpdatedDate(LocalDate.now());

            logger.info("Saving new leave balance to database...");
            LeaveBalance savedBalance = leaveBalanceRepository.save(newBalance);
            logger.info("✅ Successfully created leave balance with ID: {} for employee: {}", savedBalance.getBalanceId(), employeeSerialNumber);
            
            return savedBalance;

        } catch (Exception e) {
            logger.error("❌ Exception in getLeaveBalance for employee: {} and year: {}", employeeSerialNumber, year, e);
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            logger.error("Exception message: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("Root cause: {}", e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to get/create leave balance: " + e.getMessage(), e);
        }
    }

    // Add method to check if employee exists
    public boolean checkEmployeeExists(String employeeSerialNumber) {
        logger.info("Checking if employee exists: {}", employeeSerialNumber);
        boolean exists = userProfileRepository.existsByEmployeeSerialNumber(employeeSerialNumber);
        logger.info("Employee {} exists: {}", employeeSerialNumber, exists);
        return exists;
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

    // New method to directly deduct balance from the entity
    @Transactional
    private boolean deductLeaveBalanceDirectly(LeaveBalance balance, LeaveType leaveType, int days) {
        try {
            int currentBalance = getCurrentBalance(balance, leaveType);
            
            logger.info("Before deduction - Type: {}, Current Balance: {}, Days to deduct: {}", 
                    leaveType, currentBalance, days);
            
            if (currentBalance < days) {
                logger.error("Insufficient balance. Current: {}, Required: {}", currentBalance, days);
                return false;
            }

            switch (leaveType) {
                case CASUAL -> balance.setCasualLeaveBalance(balance.getCasualLeaveBalance() - days);
                case SICK -> balance.setSickLeaveBalance(balance.getSickLeaveBalance() - days);
                case LEAVE_WITH_PAY -> balance.setLeaveWithPayBalance(balance.getLeaveWithPayBalance() - days);
                case LEAVE_WITHOUT_PAY -> balance.setLeaveWithoutPayBalance(balance.getLeaveWithoutPayBalance() - days);
                default -> {
                    logger.error("Unknown leave type: {}", leaveType);
                    return false;
                }
            }
            
            // Save and flush to ensure immediate persistence
            LeaveBalance savedBalance = leaveBalanceRepository.saveAndFlush(balance);
            
            int newBalance = getCurrentBalance(savedBalance, leaveType);
            logger.info("After deduction - Type: {}, New Balance: {}", leaveType, newBalance);
            
            return true;
        } catch (Exception e) {
            logger.error("Error deducting leave balance", e);
            return false;
        }
    }

    // Keep the old method for backward compatibility but make it use the new logic
    @Transactional
    public LeaveBalance deductLeaveBalance(String employeeSerialNumber, LeaveType leaveType, int days) {
        Optional<LeaveBalance> balanceOpt = leaveBalanceRepository.findByUserProfile_EmployeeSerialNumberAndYear(
            employeeSerialNumber, LocalDate.now().getYear());
        
        if (balanceOpt.isPresent()) {
            LeaveBalance balance = balanceOpt.get();
            boolean success = deductLeaveBalanceDirectly(balance, leaveType, days);
            return success ? balance : null;
        }
        
        logger.warn("No leave balance found for employee: {} and year: {}", employeeSerialNumber, LocalDate.now().getYear());
        return null;
    }

    // Helper method to get current balance for a specific leave type
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