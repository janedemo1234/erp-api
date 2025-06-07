package com.erp.admin.controller;

import com.erp.admin.model.CompanyHoliday;
import com.erp.admin.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Holiday management controller
@RestController
@RequestMapping("/api/holidays")
@CrossOrigin(origins = "*")
public class HolidayController {
    
    @Autowired
    private LeaveService leaveService;
    
    // Add new holiday (admin only)
    @PostMapping("/add")
    public ResponseEntity<?> addHoliday(@RequestBody CompanyHoliday holiday) {
        try {
            // Add logic to save holiday
            return ResponseEntity.ok(holiday);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Get all holidays for a year
    @GetMapping("/{year}")
    public ResponseEntity<List<CompanyHoliday>> getHolidays(@PathVariable Integer year) {
        List<CompanyHoliday> holidays = leaveService.getHolidays(year);
        return ResponseEntity.ok(holidays);
    }
}
