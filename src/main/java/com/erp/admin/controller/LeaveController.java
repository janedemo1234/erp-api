package com.erp.admin.controller;

import com.erp.admin.model.*;
import com.erp.admin.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leave")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class LeaveController {
    
    @Autowired
    private LeaveService leaveService;
    
    // Apply for leave - Updated to handle frontend format
    @PostMapping("/apply")
    public ResponseEntity<?> applyLeave(@RequestBody LeaveRequest leaveRequest) {
        try {
            LeaveRequest savedRequest = leaveService.applyLeave(leaveRequest);
            return ResponseEntity.ok(savedRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Get user's leave history - Updated to return frontend format
    @GetMapping("/history/{employeeSerialNumber}")
    public ResponseEntity<List<LeaveRequest>> getLeaveHistory(@PathVariable String employeeSerialNumber) {
        List<LeaveRequest> history = leaveService.getUserLeaveHistory(employeeSerialNumber);
        return ResponseEntity.ok(history);
    }
    
    // Get leave balance for user - Updated to return frontend format
    @GetMapping("/balance/{employeeSerialNumber}/{year}")
    public ResponseEntity<LeaveBalance> getLeaveBalance(@PathVariable String employeeSerialNumber, @PathVariable int year) {
        LeaveBalance balance = leaveService.getLeaveBalance(employeeSerialNumber, year);
        return ResponseEntity.ok(balance);
    }
    
    // Delete leave request
    @DeleteMapping("/delete/{requestId}")
    public ResponseEntity<?> deleteLeave(@PathVariable Long requestId) {
        try {
            LeaveBalance updatedBalance = leaveService.deleteLeaveRequest(requestId);
            return ResponseEntity.ok(updatedBalance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // The helper methods for mapping are no longer needed as the models will be serialized directly.
    // You might need to ensure your LeaveRequest and LeaveBalance models are correctly annotated for JSON serialization (e.g., with Jackson annotations if needed).
    
    // Existing methods remain the same...
  @PutMapping("/approve/{requestId}")
public ResponseEntity<?> approveLeave(@PathVariable Long requestId, @RequestBody Map<String, String> request) {
    try {
        String approvedBy = request.get("approvedBy");
        LeaveRequest approved = leaveService.approveLeave(requestId, approvedBy);

        // // ✅ Now deduct leave balance here
        // leaveService.deductLeaveBalance(
        //     approved.getUserProfile().getEmployeeSerialNumber(),
        //     approved.getLeaveType(),
        //     approved.getTotalDays()
        // );

        return ResponseEntity.ok(approved);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
  
    @PutMapping("/reject/{requestId}")
    public ResponseEntity<?> rejectLeave(@PathVariable Long requestId, @RequestBody Map<String, String> request) {
        try {
            String rejectionReason = request.get("rejectionReason");
            LeaveRequest rejected = leaveService.rejectLeave(requestId, rejectionReason);
            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<LeaveRequest>> getPendingRequests() {
        List<LeaveRequest> pending = leaveService.getPendingLeaveRequests();
        return ResponseEntity.ok(pending);
    }
    
    @GetMapping("/team/{reportingOfficer}")
    public ResponseEntity<List<LeaveRequest>> getTeamLeaveRequests(@PathVariable String reportingOfficer) {
        List<LeaveRequest> teamRequests = leaveService.getTeamLeaveRequests(reportingOfficer);
        return ResponseEntity.ok(teamRequests);
    }
    
    @GetMapping("/calendar")
    public ResponseEntity<List<LeaveRequest>> getCalendarLeaves(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<LeaveRequest> leaves = leaveService.getCalendarLeaves(startDate, endDate);
        return ResponseEntity.ok(leaves);
    }
    
    @GetMapping("/holidays/{year}")
    public ResponseEntity<List<CompanyHoliday>> getHolidays(@PathVariable Integer year) {
        List<CompanyHoliday> holidays = leaveService.getHolidays(year);
        return ResponseEntity.ok(holidays);
    }
    
}