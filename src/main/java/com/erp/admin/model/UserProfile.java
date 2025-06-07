package com.erp.admin.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "user_profile")
public class UserProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sr_no")
    private Long srNo; // Sr. No. (Auto-generated)

    @Column(name = "employee_serial_number", unique = true, nullable = false)
    private String employeeSerialNumber;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(name = "emergency_contact_number")
    private String emergencyContactNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "email_address")
    private String emailAddress;

    private String qualification;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    private String designation;

    private String department;

    @Column(name = "reporting_officer")
    private String reportingOfficer;

    @Column(name = "gross_salary", precision = 15, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "bank_name")
    private String bankName;

    @Lob
    @Column(name = "medical_background", columnDefinition = "TEXT")
    private String medicalBackground;

    @Lob
    @Column(name = "legal_background", columnDefinition = "TEXT")
    private String legalBackground;

    @Column(name = "pan", length = 10)
    private String pan; // PAN as string

    @Column(name = "adhaar", length = 12)
    private String adhaar; // Aadhaar as string

    @Column(name = "personal_file_number")
    private String personalFileNumber;

    // File paths for documents
    @Column(name = "pan_file_path")
    private String panFilePath;

    @Column(name = "adhaar_file_path")
    private String adhaarFilePath;

    @Column(name = "passbook_file_path")
    private String passbookFilePath;

    // Photo field
    @Lob
    @Column(name = "photo", columnDefinition = "MEDIUMBLOB")
    private byte[] photo;

    @Column(name = "status", length = 1, nullable = false)
    private char status = 'N'; // Default status is 'N'

    @PrePersist
    protected void onCreate() {
        if (status == '\0') {
            status = 'N';
        }
    }
}