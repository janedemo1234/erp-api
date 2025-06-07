package com.erp.admin.model;

import com.erp.admin.model.LeaveStatus;
import com.erp.admin.model.LeaveType;
import com.erp.admin.model.UserProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;



@Entity
@Getter
@Setter
@Table(name = "leave_request")
public class LeaveRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_serial_number", referencedColumnName = "employee_serial_number")
    private UserProfile userProfile;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "total_days", nullable = false)
    private Integer totalDays;
    
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeaveStatus status = LeaveStatus.PENDING;
    
    @Column(name = "applied_date")
    private LocalDate appliedDate = LocalDate.now();
    
    @Column(name = "approved_date")
    private LocalDate approvedDate;
    
    @Column(name = "approved_by")
    private String approvedBy; // Can be reporting officer's employee serial number
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Column(name = "emergency_contact_during_leave")
    private String emergencyContactDuringLeave;
    
    @PrePersist
    protected void onCreate() {
        appliedDate = LocalDate.now();
    }
}
