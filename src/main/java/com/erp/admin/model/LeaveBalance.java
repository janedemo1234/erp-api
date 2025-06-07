package com.erp.admin.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "leave_balance")
public class LeaveBalance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "balance_id")
    private Long balanceId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_serial_number", referencedColumnName = "employee_serial_number")
    private UserProfile userProfile;
    
    @Column(name = "casual_leave_balance")
    private Integer casualLeaveBalance = 12; // Default 12 per year
    
    @Column(name = "sick_leave_balance")
    private Integer sickLeaveBalance = 12; // Default 12 per year
    
    @Column(name = "leave_with_pay_balance")
    private Integer leaveWithPayBalance = 12; // Default 12 per year
    
    @Column(name = "leave_without_pay_balance")
    private Integer leaveWithoutPayBalance = 12; // Default 12 per year
    
    @Column(name = "year")
    private Integer year; // Financial year or calendar year
    
    @Column(name = "created_date")
    private LocalDate createdDate = LocalDate.now();
    
    @Column(name = "updated_date")
    private LocalDate updatedDate = LocalDate.now();
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDate.now();
    }
}