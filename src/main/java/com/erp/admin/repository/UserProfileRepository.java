package com.erp.admin.repository;

import com.erp.admin.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
} 