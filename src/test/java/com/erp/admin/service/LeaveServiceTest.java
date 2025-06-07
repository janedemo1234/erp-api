package com.erp.admin.service;

import com.erp.admin.model.*;
import com.erp.admin.repository.CompanyHolidayRepository;
import com.erp.admin.repository.LeaveBalanceRepository;
import com.erp.admin.repository.LeaveRequestRepository;
import com.erp.admin.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private CompanyHolidayRepository companyHolidayRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private LeaveService leaveService;

    private UserProfile createSampleUserProfile(String employeeSerialNumber) {
        UserProfile user = new UserProfile();
        user.setEmployeeSerialNumber(employeeSerialNumber);
        user.setEmployeeName("Test User");
        return user;
    }

    private LeaveRequest createSampleLeaveRequest(UserProfile user, LocalDate startDate, LocalDate endDate, LeaveType type) {
        LeaveRequest request = new LeaveRequest();
        request.setUserProfile(user);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setLeaveType(type);
        request.setReason("Test Reason");
        request.setStatus(LeaveStatus.PENDING);
        request.setAppliedDate(LocalDate.now());
        return request;
    }

    private LeaveBalance createSampleLeaveBalance(UserProfile user, int year, int casualLeave, int sickLeave) {
        LeaveBalance balance = new LeaveBalance();
        balance.setUserProfile(user);
        balance.setYear(year);
        balance.setCasualLeaveBalance(casualLeave);
        balance.setSickLeaveBalance(sickLeave);
        balance.setLeaveWithPayBalance(10); // Default for other types
        balance.setLeaveWithoutPayBalance(10); // Default for other types
        return balance;
    }

    // --- Tests for applyLeave ---

    @Test
    void testApplyLeave_Success() throws Exception {
        UserProfile user = createSampleUserProfile("EMP001");
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(2); // 2 working days if no holidays/weekends
        LeaveRequest request = createSampleLeaveRequest(user, startDate, endDate, LeaveType.CASUAL);

        when(userProfileRepository.findByEmployeeSerialNumber("EMP001")).thenReturn(Optional.of(user));
        when(leaveRequestRepository.findOverlappingLeaves("EMP001", startDate, endDate)).thenReturn(Collections.emptyList());
        when(companyHolidayRepository.findHolidaysInRange(startDate, endDate)).thenReturn(Collections.emptyList()); // Ensures workingDays > 0

        LeaveBalance balance = createSampleLeaveBalance(user, LocalDate.now().getYear(), 10, 10);
        when(leaveBalanceRepository.findByUserProfile_EmployeeSerialNumberAndYear("EMP001", LocalDate.now().getYear()))
                .thenReturn(Optional.of(balance));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequest savedRequest = leaveService.applyLeave(request);

        assertNotNull(savedRequest);
        // Assuming startDate and endDate are weekdays and no holidays, workingDays would be 2.
        // This depends on the actual logic of calculateWorkingDays which is private.
        // For this test, we are more focused on the flow.
        // If calculateWorkingDays is complex, it might need its own tests or a spy if we need to control its output directly.
        // Here, we control it by ensuring no holidays and selecting weekdays.
        assertTrue(savedRequest.getTotalDays() > 0); // General check
        assertEquals(user, savedRequest.getUserProfile());
        verify(leaveRequestRepository).save(request);
    }

    @Test
    void testApplyLeave_Failure_UserNotFound() {
        UserProfile user = createSampleUserProfile("EMP001");
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(2);
        LeaveRequest request = createSampleLeaveRequest(user, startDate, endDate, LeaveType.CASUAL);

        when(userProfileRepository.findByEmployeeSerialNumber("EMP001")).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> leaveService.applyLeave(request));
        assertEquals("User not found", exception.getMessage());
    }


    @Test
    void testApplyLeave_Failure_ZeroWorkingDays() {
        UserProfile user = createSampleUserProfile("EMP002");
        // Example: Start and end date are the same, and it's a mocked holiday
        LocalDate holidayDate = LocalDate.now().plusDays(1);
        // Ensure the date is a weekday to avoid weekend logic interference for this specific test focus
        if (holidayDate.getDayOfWeek() == DayOfWeek.SATURDAY || holidayDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            holidayDate = holidayDate.plusDays(holidayDate.getDayOfWeek() == DayOfWeek.SATURDAY ? 2 : 1);
        }
        LocalDate startDate = holidayDate;
        LocalDate endDate = holidayDate;

        LeaveRequest request = createSampleLeaveRequest(user, startDate, endDate, LeaveType.CASUAL);
        CompanyHoliday holiday = new CompanyHoliday();
        holiday.setHolidayDate(holidayDate);

        when(userProfileRepository.findByEmployeeSerialNumber("EMP002")).thenReturn(Optional.of(user));
        when(leaveRequestRepository.findOverlappingLeaves("EMP002", startDate, endDate)).thenReturn(Collections.emptyList());
        // Mocking findHolidaysInRange to make calculateWorkingDays return 0
        when(companyHolidayRepository.findHolidaysInRange(startDate, endDate)).thenReturn(List.of(holiday));


        Exception exception = assertThrows(Exception.class, () -> leaveService.applyLeave(request));
        assertEquals("Leave application must be for at least one working day. Please select a valid date range.", exception.getMessage());
    }

    @Test
    void testApplyLeave_Failure_ZeroWorkingDays_Weekend() {
        UserProfile user = createSampleUserProfile("EMP002");
        LocalDate saturday = LocalDate.now().with(DayOfWeek.SATURDAY);
        LocalDate sunday = LocalDate.now().with(DayOfWeek.SUNDAY);
         // If today is Sunday, saturday would be in the past, so advance by a week.
        if (saturday.isAfter(sunday)) { // e.g. if today is sunday, .with(SATURDAY) goes to yesterday.
            saturday = saturday.plusWeeks(1);
            sunday = sunday.plusWeeks(1);
        }

        LeaveRequest request = createSampleLeaveRequest(user, saturday, sunday, LeaveType.CASUAL);

        when(userProfileRepository.findByEmployeeSerialNumber("EMP002")).thenReturn(Optional.of(user));
        when(leaveRequestRepository.findOverlappingLeaves("EMP002", saturday, sunday)).thenReturn(Collections.emptyList());
        when(companyHolidayRepository.findHolidaysInRange(saturday, sunday)).thenReturn(Collections.emptyList());


        Exception exception = assertThrows(Exception.class, () -> leaveService.applyLeave(request));
        assertEquals("Leave application must be for at least one working day. Please select a valid date range.", exception.getMessage());
    }


    @Test
    void testApplyLeave_Failure_InsufficientBalance() {
        UserProfile user = createSampleUserProfile("EMP003");
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(2); // Assume 2 working days
        LeaveRequest request = createSampleLeaveRequest(user, startDate, endDate, LeaveType.CASUAL);
        // Ensure start and end are weekdays
         if (startDate.getDayOfWeek() == DayOfWeek.SATURDAY || startDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            startDate = startDate.plusDays(startDate.getDayOfWeek() == DayOfWeek.SATURDAY ? 2 : 1);
        }
        endDate = startDate.plusDays(1); // 2 working days
         if (endDate.getDayOfWeek() == DayOfWeek.SATURDAY || endDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            endDate = endDate.plusDays(endDate.getDayOfWeek() == DayOfWeek.SATURDAY ? 2 : 1);
        }
        request.setStartDate(startDate);
        request.setEndDate(endDate);


        when(userProfileRepository.findByEmployeeSerialNumber("EMP003")).thenReturn(Optional.of(user));
        when(leaveRequestRepository.findOverlappingLeaves("EMP003", startDate, endDate)).thenReturn(Collections.emptyList());
        when(companyHolidayRepository.findHolidaysInRange(startDate, endDate)).thenReturn(Collections.emptyList());

        LeaveBalance balance = createSampleLeaveBalance(user, LocalDate.now().getYear(), 1, 10); // Only 1 casual leave day
        when(leaveBalanceRepository.findByUserProfile_EmployeeSerialNumberAndYear("EMP003", LocalDate.now().getYear()))
                .thenReturn(Optional.of(balance));

        Exception exception = assertThrows(Exception.class, () -> leaveService.applyLeave(request));
        assertEquals("Insufficient leave balance", exception.getMessage());
    }

    @Test
    void testApplyLeave_Failure_OverlappingLeave() {
        UserProfile user = createSampleUserProfile("EMP004");
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(2);
        LeaveRequest request = createSampleLeaveRequest(user, startDate, endDate, LeaveType.CASUAL);

        when(userProfileRepository.findByEmployeeSerialNumber("EMP004")).thenReturn(Optional.of(user));
        List<LeaveRequest> overlapping = new ArrayList<>();
        overlapping.add(new LeaveRequest()); // Add a dummy request to simulate overlap
        when(leaveRequestRepository.findOverlappingLeaves("EMP004", startDate, endDate)).thenReturn(overlapping);

        Exception exception = assertThrows(Exception.class, () -> leaveService.applyLeave(request));
        assertEquals("Leave dates overlap with existing leave application", exception.getMessage());
    }

    // --- Tests for approveLeave ---

    @Test
    void testApproveLeave_Success_DeductsBalance() throws Exception {
        Long requestId = 1L;
        UserProfile user = createSampleUserProfile("EMP005");
        LeaveRequest pendingRequest = createSampleLeaveRequest(user, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), LeaveType.CASUAL);
        pendingRequest.setSrNo(requestId);
        pendingRequest.setStatus(LeaveStatus.PENDING);
        pendingRequest.setTotalDays(2); // Explicitly set for test clarity

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));

        LeaveBalance balance = createSampleLeaveBalance(user, LocalDate.now().getYear(), 10, 10);
        when(leaveBalanceRepository.findByUserProfile_EmployeeSerialNumberAndYear("EMP005", LocalDate.now().getYear()))
                .thenReturn(Optional.of(balance));
        when(leaveBalanceRepository.saveAndFlush(any(LeaveBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequest approvedRequest = leaveService.approveLeave(requestId, "ApproverX");

        assertNotNull(approvedRequest);
        assertEquals(LeaveStatus.APPROVED, approvedRequest.getStatus());
        assertEquals("ApproverX", approvedRequest.getApprovedBy());
        assertNotNull(approvedRequest.getApprovedDate());

        ArgumentCaptor<LeaveBalance> balanceCaptor = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(leaveBalanceRepository).saveAndFlush(balanceCaptor.capture());
        assertEquals(8, balanceCaptor.getValue().getCasualLeaveBalance()); // 10 - 2

        verify(leaveRequestRepository).save(approvedRequest);
    }

    @Test
    void testApproveLeave_Failure_RequestNotFound() {
        Long requestId = 99L;
        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> leaveService.approveLeave(requestId, "ApproverX"));
        assertEquals("Leave request not found", exception.getMessage());
    }

    @Test
    void testApproveLeave_Failure_NotInPendingStatus() {
        Long requestId = 1L;
        UserProfile user = createSampleUserProfile("EMP005");
        LeaveRequest nonPendingRequest = createSampleLeaveRequest(user, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), LeaveType.CASUAL);
        nonPendingRequest.setSrNo(requestId);
        nonPendingRequest.setStatus(LeaveStatus.APPROVED); // Not PENDING

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(nonPendingRequest));

        Exception exception = assertThrows(Exception.class, () -> leaveService.approveLeave(requestId, "ApproverX"));
        assertEquals("Leave request is not in pending status", exception.getMessage());
         verify(leaveBalanceRepository, never()).saveAndFlush(any()); // Ensure balance not deducted
    }


    @Test
    void testApproveLeave_Failure_DeductBalanceFails_NoBalanceRecord() {
        Long requestId = 2L;
        UserProfile user = createSampleUserProfile("EMP006");
        LeaveRequest pendingRequest = createSampleLeaveRequest(user, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), LeaveType.SICK);
        pendingRequest.setSrNo(requestId);
        pendingRequest.setStatus(LeaveStatus.PENDING);
        pendingRequest.setTotalDays(3);

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        // Mocking getLeaveBalance (which findBy... calls) to return null effectively by returning empty Optional
        // This mock ensures that the call within deductLeaveBalance returns empty.
        when(leaveBalanceRepository.findByUserProfile_EmployeeSerialNumberAndYear(user.getEmployeeSerialNumber(), LocalDate.now().getYear()))
                .thenReturn(Optional.empty());

        Exception exception = assertThrows(Exception.class, () -> leaveService.approveLeave(requestId, "ApproverY"));
        assertEquals("Failed to deduct leave balance", exception.getMessage());

        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class)); // Status should not change
    }
}
