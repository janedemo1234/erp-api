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
    
    // Get leave balance for user - Updated to return frontend format with better error handling
    @GetMapping("/balance/{employeeSerialNumber}/{year}")
    public ResponseEntity<?> getLeaveBalance(@PathVariable String employeeSerialNumber, @PathVariable int year) {
        try {
            LeaveBalance balance = leaveService.getLeaveBalance(employeeSerialNumber, year);
            if (balance == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Employee not found or unable to create leave balance"));
            }
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch leave balance: " + e.getMessage()));
        }
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
    
    @PutMapping("/approve/{requestId}")
    public ResponseEntity<?> approveLeave(@PathVariable Long requestId, @RequestBody Map<String, String> request) {
        try {
            String approvedBy = request.get("approvedBy");
            LeaveRequest approved = leaveService.approveLeave(requestId, approvedBy);
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