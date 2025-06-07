package com.erp.admin.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "company_holidays")
public class CompanyHoliday {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_id")
    private Long holidayId;
    
    @Column(name = "holiday_name", nullable = false)
    private String holidayName;
    
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;
    
    @Column(name = "holiday_type")
    private String holidayType; // National, Regional, Company-specific
    
    @Column(name = "is_optional")
    private Boolean isOptional = false;
    
    @Column(name = "year")
    private Integer year;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "status", length = 1, nullable = false)
    private char status = 'A'; // A=Active, I=Inactive
}
