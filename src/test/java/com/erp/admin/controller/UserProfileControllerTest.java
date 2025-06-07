package com.erp.admin.controller;

import com.erp.admin.model.UserProfile;
import com.erp.admin.repository.UserProfileRepository;
import com.erp.admin.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
public class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileRepository userProfileRepository;

    @MockBean
    private FileStorageService fileStorageService; // Mocked even if not directly used by tested methods

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Ensure ObjectMapper is configured with JavaTimeModule, similar to the controller
        // Spring Boot typically autoconfigures this, but explicit configuration is safer for tests.
        if (!objectMapper.getRegisteredModuleIds().contains(JavaTimeModule.class.getName())) {
            objectMapper.registerModule(new JavaTimeModule());
        }
    }

    private UserProfile createSampleUserProfile(Long id, String employeeName) {
        UserProfile profile = new UserProfile();
        profile.setSrNo(id);
        profile.setEmployeeSerialNumber("EMP" + id);
        profile.setEmployeeName(employeeName);
        profile.setEmailAddress("test" + id + "@example.com");
        profile.setDateOfJoining(LocalDate.of(2023, 1, 1));
        profile.setGrossSalary(new BigDecimal("50000.00"));
        profile.setPan("ABCDE1234F");
        profile.setAdhaar("123456789012");
        // Initialize byte[] fields to avoid NullPointerExceptions if accessed
        profile.setPhoto(new byte[0]);
        profile.setPanDocument(new byte[0]);
        profile.setAdhaarDocument(new byte[0]);
        profile.setPassbookDocument(new byte[0]);
        profile.setQualificationDocument(new byte[0]);
        profile.setOfferLetterDocument(new byte[0]);
        profile.setAddressProofDocument(new byte[0]);
        profile.setMedicalBackgroundDocument(new byte[0]);
        profile.setLegalBackgroundDocument(new byte[0]);
        return profile;
    }

    // --- Tests for updateUserProfile ---

    @Test
    void testUpdateUserProfile_Success_BasicFields() throws Exception {
        Long userId = 1L;
        UserProfile existingProfile = createSampleUserProfile(userId, "Old Name");
        existingProfile.setDepartment("Old Department");

        UserProfile updatedDto = new UserProfile();
        updatedDto.setEmployeeName("New Name");
        updatedDto.setEmailAddress("newemail@example.com");
        updatedDto.setDepartment("New Department"); // Basic field, not using "Others"

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(existingProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/user-profiles/update/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeName", is("New Name")))
                .andExpect(jsonPath("$.emailAddress", is("newemail@example.com")))
                .andExpect(jsonPath("$.department", is("New Department")));

        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(userProfileCaptor.capture());
        UserProfile savedProfile = userProfileCaptor.getValue();

        assertEquals("New Name", savedProfile.getEmployeeName());
        assertEquals("newemail@example.com", savedProfile.getEmailAddress());
        assertEquals("New Department", savedProfile.getDepartment());
        // Ensure other fields from existingProfile were preserved if not updated
        assertEquals(existingProfile.getEmployeeSerialNumber(), savedProfile.getEmployeeSerialNumber());
    }

    @Test
    void testUpdateUserProfile_Success_WithOthersLogic() throws Exception {
        Long userId = 2L;
        UserProfile existingProfile = createSampleUserProfile(userId, "Original Name");

        UserProfile updatedDto = new UserProfile();
        updatedDto.setBankName("Others");
        updatedDto.setOtherBankName("My Custom Bank");
        updatedDto.setDepartment("Others");
        updatedDto.setOtherDepartment("My Custom Department");
        updatedDto.setDesignation("Others");
        updatedDto.setOtherDesignation("My Custom Designation");
        // Set a non-Others field to ensure it's also updated
        updatedDto.setEmployeeName("Updated Name For Others Test");


        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(existingProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/user-profiles/update/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankName", is("My Custom Bank")))
                .andExpect(jsonPath("$.department", is("My Custom Department")))
                .andExpect(jsonPath("$.designation", is("My Custom Designation")))
                .andExpect(jsonPath("$.employeeName", is("Updated Name For Others Test")));

        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(userProfileCaptor.capture());
        UserProfile savedProfile = userProfileCaptor.getValue();

        assertEquals("My Custom Bank", savedProfile.getBankName());
        assertEquals("My Custom Department", savedProfile.getDepartment());
        assertEquals("My Custom Designation", savedProfile.getDesignation());
        assertEquals("Updated Name For Others Test", savedProfile.getEmployeeName());
    }

    @Test
    void testUpdateUserProfile_Success_WithBase64FileFields() throws Exception {
        Long userId = 3L;
        UserProfile existingProfile = createSampleUserProfile(userId, "File User");

        String photoBase64 = Base64.getEncoder().encodeToString("testPhotoData".getBytes());
        String panDocBase64 = Base64.getEncoder().encodeToString("testPanData".getBytes());
        String qualDocBase64 = Base64.getEncoder().encodeToString("testQualData".getBytes());

        UserProfile updatedDto = new UserProfile();
        updatedDto.setPhoto(photoBase64.getBytes()); // Jackson expects byte[] for direct mapping if source is already byte[]
                                                    // If JSON source is base64 string, it will convert.
                                                    // For testing, we simulate the DTO state after JSON deserialization.
        updatedDto.setPanDocument(panDocBase64.getBytes());
        updatedDto.setQualificationDocument(qualDocBase64.getBytes());
        // To simulate incoming JSON as base64 strings, we'd pass strings in the JSON content:
        // String jsonContent = String.format("{\"photo\":\"%s\", \"panDocument\":\"%s\", ...}", photoBase64, panDocBase64);
        // However, objectMapper.writeValueAsString(updatedDto) will re-encode byte[] to base64.
        // So, setting byte[] in DTO is fine here.

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(existingProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Create a temporary DTO with String fields for base64 content to ensure Jackson deserializes from string
        // This more accurately simulates the incoming JSON payload.
        UserProfile dtoForJson = new UserProfile(); // This is what would be represented in the JSON request body
        // We can't directly set String to byte[] fields in UserProfile.
        // So we construct the JSON string manually for this test case to be precise.

        String jsonContent = "{"
            + "\"photo\":\"" + photoBase64 + "\","
            + "\"panDocument\":\"" + panDocBase64 + "\","
            + "\"qualificationDocument\":\"" + qualDocBase64 + "\""
            // Add other necessary fields for the DTO to be valid if needed, or ensure UserProfile allows partial updates
            // For this test, we focus on byte[] fields.
            + "}";


        mockMvc.perform(put("/api/user-profiles/update/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)) // Use the manually crafted JSON string
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photo", is(photoBase64)))
                .andExpect(jsonPath("$.panDocument", is(panDocBase64)))
                .andExpect(jsonPath("$.qualificationDocument", is(qualDocBase64)));

        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(userProfileCaptor.capture());
        UserProfile savedProfile = userProfileCaptor.getValue();

        assertArrayEquals("testPhotoData".getBytes(), savedProfile.getPhoto());
        assertArrayEquals("testPanData".getBytes(), savedProfile.getPanDocument());
        assertArrayEquals("testQualData".getBytes(), savedProfile.getQualificationDocument());
    }

    @Test
    void testUpdateUserProfile_NotFound() throws Exception {
        Long userId = 4L;
        UserProfile updatedDto = new UserProfile();
        updatedDto.setEmployeeName("Any Name");

        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/user-profiles/update/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isNotFound());
    }

    // --- Test for getUserProfileById ---

    @Test
    void testGetUserProfileById_Success_WithFileFields() throws Exception {
        Long userId = 5L;
        UserProfile profile = createSampleUserProfile(userId, "User With Files");

        byte[] photoBytes = "samplePhotoData".getBytes();
        byte[] panDocumentBytes = "samplePanDocumentData".getBytes();
        profile.setPhoto(photoBytes);
        profile.setPanDocument(panDocumentBytes);

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/user-profiles/" + userId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.srNo", is(userId.intValue())))
                .andExpect(jsonPath("$.employeeName", is("User With Files")))
                .andExpect(jsonPath("$.photo", is(Base64.getEncoder().encodeToString(photoBytes))))
                .andExpect(jsonPath("$.panDocument", is(Base64.getEncoder().encodeToString(panDocumentBytes))));
    }
}
