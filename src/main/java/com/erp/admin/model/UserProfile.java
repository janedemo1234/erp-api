package com.erp.admin.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
public class UserProfile {
  @Id
  private String id;

  @Embedded
  private Name name;

  private String email;
  private String username;
  private String password;
  private LocalDate dateOfBirth;
  @Lob
@Column(name = "photo", columnDefinition = "MEDIUMBLOB")
private byte[] photo;

  @Column(name = "blood_group")
  private String bloodGroup;

  @Embedded
  private Address address;

  private String phone;
  
  @Column(updatable = false)
  private OffsetDateTime accountCreated;
  
  @PrePersist
  protected void onCreate() {
    accountCreated = OffsetDateTime.now();
  }
}

@Embeddable
@Getter
@Setter
class Name {
  private String first;
  private String last;
}

@Embeddable
@Getter
@Setter
class Address {
  private String street;
  private String city;
  private String state;
  private String postalCode;
  private String country;
} 