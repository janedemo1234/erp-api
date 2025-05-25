package com.erp.admin.controller;

import com.erp.admin.model.UserProfile;
import com.erp.admin.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/user-profiles")
public class UserProfileController {

  private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

  @Autowired
  private UserProfileRepository repository;

//   @PostMapping("/submit")
//   public UserProfile createUserProfile(@RequestBody UserProfile userProfile) {
//     logger.info("Received user profile: {}", userProfile);
//     logger.info("Blood group from request: {}", userProfile.getBloodGroup());
//     return repository.save(userProfile);
//   }
 @PostMapping(value = "/submit", consumes = {"multipart/form-data"})
public ResponseEntity<UserProfile> createUserProfile(
        @RequestPart("userProfile") UserProfile userProfile,
        @RequestPart(value = "photo", required = false) MultipartFile photo) {

    logger.info("Received user profile: {}", userProfile);
    
    if (photo != null && !photo.isEmpty()) {
        try {
            userProfile.setPhoto(photo.getBytes()); // if storing bytes
            // OR
            // String filePath = saveFile(photo); // if storing path
            // userProfile.setPhotoPath(filePath);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    UserProfile saved = repository.save(userProfile);
    return ResponseEntity.ok(saved);
}

  @GetMapping("/all")
  public List<UserProfile> getAllUserProfiles() {
    return repository.findAll();
  }
  
  @GetMapping("/{id}")
  public ResponseEntity<UserProfile> getUserProfileById(@PathVariable String id) {
    return repository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }
  
  @PutMapping("update/{id}")
  public ResponseEntity<UserProfile> updateUserProfile(@PathVariable String id, @RequestBody UserProfile updatedProfile) {
    return repository.findById(id)
            .map(existingProfile -> {
                // Update fields from updatedProfile
                if (updatedProfile.getName() != null) {
                    existingProfile.setName(updatedProfile.getName());
                }
                if (updatedProfile.getEmail() != null) {
                    existingProfile.setEmail(updatedProfile.getEmail());
                }
                if (updatedProfile.getUsername() != null) {
                    existingProfile.setUsername(updatedProfile.getUsername());
                }
                if (updatedProfile.getPassword() != null) {
                    existingProfile.setPassword(updatedProfile.getPassword());
                }
                if (updatedProfile.getDateOfBirth() != null) {
                    existingProfile.setDateOfBirth(updatedProfile.getDateOfBirth());
                }
                if (updatedProfile.getBloodGroup() != null) {
                    existingProfile.setBloodGroup(updatedProfile.getBloodGroup());
                }
                if (updatedProfile.getAddress() != null) {
                    existingProfile.setAddress(updatedProfile.getAddress());
                }
                if (updatedProfile.getPhone() != null) {
                    existingProfile.setPhone(updatedProfile.getPhone());
                }
                
                // Save the updated entity
                UserProfile savedProfile = repository.save(existingProfile);
                return ResponseEntity.ok(savedProfile);
            })
            .orElse(ResponseEntity.notFound().build());
  }
} 