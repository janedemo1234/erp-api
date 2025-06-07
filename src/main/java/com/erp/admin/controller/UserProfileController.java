package com.erp.admin.controller;

import com.erp.admin.model.UserProfile;
import com.erp.admin.repository.UserProfileRepository;
import com.erp.admin.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
// import java.math.BigDecimal; // No longer directly used here
// import java.time.LocalDate; // No longer directly used here
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/user-profiles")
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private FileStorageService fileStorageService;

    private final ObjectMapper objectMapper;

    public UserProfileController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule()); // For LocalDate deserialization
    }

    // POST API - Create Employee with file uploads
    @PostMapping(value = "/save", consumes = "multipart/form-data")
    public ResponseEntity<?> createUserProfile(
            @RequestPart("userProfile") String userProfileJson,
            @RequestPart(value = "panFile", required = false) MultipartFile panFile,
            @RequestPart(value = "adhaarFile", required = false) MultipartFile adhaarFile,
            @RequestPart(value = "passbookFile", required = false) MultipartFile passbookFile,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {

        try {
            UserProfile userProfile = objectMapper.readValue(userProfileJson, UserProfile.class);
            logger.info("Creating user profile for employee: {}", userProfile.getEmployeeSerialNumber());

            // Check if employee serial number already exists
            if (userProfile.getEmployeeSerialNumber() != null && repository.existsByEmployeeSerialNumber(userProfile.getEmployeeSerialNumber())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Employee Serial Number already exists: " + userProfile.getEmployeeSerialNumber());
            }

            // Ensure srNo is null for new entities so the database can generate it.
            userProfile.setSrNo(null);
            
            // Frontend sends 'N' or 'Y', model expects char. ObjectMapper should handle this if UserProfile.status is char.
            // If UserProfile.status is String, then: userProfile.setStatus(userProfile.getStatus().charAt(0));
            // Ensure status is set, model has a default but explicit is fine.
            if (userProfile.getStatus() == '\0') { // if not set by JSON or default
                userProfile.setStatus('N');
            }


            // Handle file uploads
            if (panFile != null && !panFile.isEmpty()) {
                String panFilePath = fileStorageService.storeFile(panFile, "pan_" + userProfile.getEmployeeSerialNumber());
                userProfile.setPanFilePath(panFilePath);
                logger.info("PAN file uploaded: {}", panFilePath);
            }

            if (adhaarFile != null && !adhaarFile.isEmpty()) {
                String adhaarFilePath = fileStorageService.storeFile(adhaarFile, "adhaar_" + userProfile.getEmployeeSerialNumber());
                userProfile.setAdhaarFilePath(adhaarFilePath);
                logger.info("Aadhaar file uploaded: {}", adhaarFilePath);
            }

            if (passbookFile != null && !passbookFile.isEmpty()) {
                String passbookFilePath = fileStorageService.storeFile(passbookFile, "passbook_" + userProfile.getEmployeeSerialNumber());
                userProfile.setPassbookFilePath(passbookFilePath);
                logger.info("Passbook file uploaded: {}", passbookFilePath);
            }

            // Handle photo upload
            if (photo != null && !photo.isEmpty()) {
                userProfile.setPhoto(photo.getBytes());
                logger.info("Photo uploaded successfully for {}, size: {} bytes", userProfile.getEmployeeSerialNumber(), photo.getSize());
            }

            UserProfile savedProfile = repository.save(userProfile);
            logger.info("User profile created successfully with Sr. No: {}", savedProfile.getSrNo());
            
            // Return the saved profile, which includes the generated srNo and any other backend-set fields
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProfile);

        } catch (IOException e) {
            logger.error("Error processing/uploading files or deserializing JSON for employee", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating user profile: " + e.getMessage());
        }
    }

    // PUT API - Update Employee
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUserProfile(@PathVariable Long id, @RequestBody UserProfile updatedProfileData) {
        try {
            logger.info("Updating user profile for Sr. No: {}", id);

            Optional<UserProfile> existingProfileOptional = repository.findById(id);
            if (!existingProfileOptional.isPresent()) {
                logger.warn("User profile not found for update with Sr. No: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User profile not found with Sr. No: " + id);
            }

            UserProfile existingProfile = existingProfileOptional.get();

            // Update fields from updatedProfileData - null checks can be added for partial updates
            existingProfile.setEmployeeName(updatedProfileData.getEmployeeName());
            existingProfile.setEmergencyContactNumber(updatedProfileData.getEmergencyContactNumber());
            existingProfile.setAddress(updatedProfileData.getAddress());
            existingProfile.setEmailAddress(updatedProfileData.getEmailAddress());
            existingProfile.setQualification(updatedProfileData.getQualification());
            existingProfile.setDateOfJoining(updatedProfileData.getDateOfJoining());
            // Handle "Others" logic
            if ("Others".equalsIgnoreCase(updatedProfileData.getBankName())) {
                existingProfile.setBankName(updatedProfileData.getOtherBankName());
            } else {
                existingProfile.setBankName(updatedProfileData.getBankName());
            }

            if ("Others".equalsIgnoreCase(updatedProfileData.getDepartment())) {
                existingProfile.setDepartment(updatedProfileData.getOtherDepartment());
            } else {
                existingProfile.setDepartment(updatedProfileData.getDepartment());
            }

            if ("Others".equalsIgnoreCase(updatedProfileData.getDesignation())) {
                existingProfile.setDesignation(updatedProfileData.getOtherDesignation());
            } else {
                existingProfile.setDesignation(updatedProfileData.getDesignation());
            }

            existingProfile.setReportingOfficer(updatedProfileData.getReportingOfficer());
            existingProfile.setGrossSalary(updatedProfileData.getGrossSalary());
            existingProfile.setBankAccountNumber(updatedProfileData.getBankAccountNumber());
            existingProfile.setIfscCode(updatedProfileData.getIfscCode());
            // existingProfile.setBankName(updatedProfileData.getBankName()); // Handled by "Others" logic
            existingProfile.setMedicalBackground(updatedProfileData.getMedicalBackground());
            existingProfile.setLegalBackground(updatedProfileData.getLegalBackground());
            existingProfile.setPan(updatedProfileData.getPan());
            existingProfile.setAdhaar(updatedProfileData.getAdhaar());
            existingProfile.setPersonalFileNumber(updatedProfileData.getPersonalFileNumber());
            existingProfile.setStatus(updatedProfileData.getStatus());

            // Update byte[] document fields
            existingProfile.setPhoto(updatedProfileData.getPhoto());
            existingProfile.setPanDocument(updatedProfileData.getPanDocument());
            existingProfile.setAdhaarDocument(updatedProfileData.getAdhaarDocument());
            existingProfile.setPassbookDocument(updatedProfileData.getPassbookDocument());
            existingProfile.setQualificationDocument(updatedProfileData.getQualificationDocument());
            existingProfile.setOfferLetterDocument(updatedProfileData.getOfferLetterDocument());
            existingProfile.setAddressProofDocument(updatedProfileData.getAddressProofDocument());
            existingProfile.setMedicalBackgroundDocument(updatedProfileData.getMedicalBackgroundDocument());
            existingProfile.setLegalBackgroundDocument(updatedProfileData.getLegalBackgroundDocument());

            UserProfile savedProfile = repository.save(existingProfile);
            logger.info("User profile updated successfully for Sr. No: {}", id);
            return ResponseEntity.ok(savedProfile);

        } catch (Exception e) {
            logger.error("Error updating user profile for Sr. No: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating user profile: " + e.getMessage());
        }
    }


    // GET API - Get all employees
    @GetMapping("/all")
    public ResponseEntity<List<UserProfile>> getAllUserProfiles() {
        try {
            List<UserProfile> profiles = repository.findAll();
            logger.info("Retrieved {} user profiles", profiles.size());
            return ResponseEntity.ok(profiles);
        } catch (Exception e) {
            logger.error("Error retrieving all user profiles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // GET API - Get employee by Sr. No. (ID)
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfileById(@PathVariable Long id) {
        try {
            Optional<UserProfile> userProfile = repository.findById(id);
            if (userProfile.isPresent()) {
                logger.info("Retrieved user profile for Sr. No: {}", id);
                return ResponseEntity.ok(userProfile.get());
            } else {
                logger.warn("User profile not found for Sr. No: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User profile not found with Sr. No: " + id);
            }
        } catch (Exception e) {
            logger.error("Error retrieving user profile for Sr. No: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving user profile: " + e.getMessage());
        }
    }

    // PATCH API - Update status to 'Y' by Employee Serial Number
    @PatchMapping("/update-by-status/{employeeSerialNumber}")
    public ResponseEntity<?> activateUserStatusByEmployeeSerialNumber(
            @PathVariable String employeeSerialNumber) {
        try {
            logger.info("Activating status for employee serial number: {}", employeeSerialNumber);

            Optional<UserProfile> userProfileOptional = repository.findByEmployeeSerialNumber(employeeSerialNumber);
            if (!userProfileOptional.isPresent()) {
                logger.warn("User profile not found for employee serial number: {}", employeeSerialNumber);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User profile not found with employee serial number: " + employeeSerialNumber);
            }

            UserProfile userProfile = userProfileOptional.get();
            char currentStatus = userProfile.getStatus();

            if (currentStatus == 'N' || currentStatus == 'n') {
                userProfile.setStatus('Y');
                UserProfile savedProfile = repository.save(userProfile);
                logger.info("Status activated successfully for employee serial number: {} to Y", employeeSerialNumber);
                return ResponseEntity.ok(savedProfile);
            } else {
                logger.info("Status already active for employee serial number: {}", employeeSerialNumber);
                return ResponseEntity.ok(userProfile); // Or a specific message indicating no change
            }

        } catch (Exception e) {
            logger.error("Error activating status for employee serial number: {}", employeeSerialNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error activating status: " + e.getMessage());
        }
    }
}