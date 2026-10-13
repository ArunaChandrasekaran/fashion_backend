package com.fashion.controller.admin;

import com.fashion.dto.PageResponse;
import com.fashion.dto.ProfileDtos.CustomerStatusRequest;
import com.fashion.dto.ProfileDtos.CustomerSummary;
import com.fashion.dto.ProfileDtos.ProfileResponse;
import com.fashion.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

    private final ProfileService profileService;

    public AdminCustomerController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public PageResponse<CustomerSummary> search(@RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "12") int size) {
        return profileService.adminCustomers(q, page, size);
    }

    @GetMapping("/{id}")
    public ProfileResponse detail(@PathVariable Long id) {
        return profileService.adminCustomer(id);
    }

    @PatchMapping("/{id}/status")
    public CustomerSummary status(@PathVariable Long id, @Valid @RequestBody CustomerStatusRequest request) {
        return profileService.updateCustomerStatus(id, request.enabled());
    }
}
