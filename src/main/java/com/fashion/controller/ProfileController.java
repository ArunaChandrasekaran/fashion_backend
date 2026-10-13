package com.fashion.controller;

import com.fashion.dto.AuthDtos.MessageResponse;
import com.fashion.dto.ProfileDtos.AddressRequest;
import com.fashion.dto.ProfileDtos.AddressResponse;
import com.fashion.dto.ProfileDtos.ProfileResponse;
import com.fashion.dto.ProfileDtos.UpdateProfileRequest;
import com.fashion.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/api/profile")
    public ProfileResponse profile() {
        return profileService.myProfile();
    }

    @PatchMapping("/api/profile")
    public ProfileResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(request);
    }

    @GetMapping("/api/addresses")
    public List<AddressResponse> addresses() {
        return profileService.myAddresses();
    }

    @PostMapping("/api/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@Valid @RequestBody AddressRequest request) {
        return profileService.createAddress(request);
    }

    @PatchMapping("/api/addresses/{addressId}")
    public AddressResponse updateAddress(@PathVariable Long addressId, @Valid @RequestBody AddressRequest request) {
        return profileService.updateAddress(addressId, request);
    }

    @DeleteMapping("/api/addresses/{addressId}")
    public MessageResponse deleteAddress(@PathVariable Long addressId) {
        profileService.deleteAddress(addressId);
        return new MessageResponse("Address removed");
    }
}
