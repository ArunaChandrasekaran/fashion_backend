package com.fashion.service;

import com.fashion.dto.PageResponse;
import com.fashion.dto.ProfileDtos.AddressRequest;
import com.fashion.dto.ProfileDtos.AddressResponse;
import com.fashion.dto.ProfileDtos.CustomerSummary;
import com.fashion.dto.ProfileDtos.ProfileResponse;
import com.fashion.dto.ProfileDtos.UpdateProfileRequest;
import com.fashion.entity.Address;
import com.fashion.entity.AppUser;
import com.fashion.entity.RoleName;
import com.fashion.exception.ApiException;
import com.fashion.repository.AddressRepository;
import com.fashion.repository.OrderRepository;
import com.fashion.repository.UserRepository;
import com.fashion.security.SecurityUtils;
import com.fashion.util.Money;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final SecurityUtils securityUtils;
    private final DtoMapper mapper;

    public ProfileService(UserRepository userRepository,
                          AddressRepository addressRepository,
                          OrderRepository orderRepository,
                          SecurityUtils securityUtils,
                          DtoMapper mapper) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.orderRepository = orderRepository;
        this.securityUtils = securityUtils;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ProfileResponse myProfile() {
        return toProfile(securityUtils.requireUser());
    }

    @Transactional
    public ProfileResponse updateProfile(UpdateProfileRequest request) {
        AppUser user = securityUtils.requireUser();
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone().trim());
        return toProfile(user);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> myAddresses() {
        return addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(securityUtils.requireUserId())
                .stream().map(mapper::toAddress).toList();
    }

    @Transactional
    public AddressResponse createAddress(AddressRequest request) {
        AppUser user = securityUtils.requireUser();
        Address address = new Address();
        address.setUser(user);
        apply(address, request, user.getId());
        return mapper.toAddress(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(Long id, AddressRequest request) {
        Long userId = securityUtils.requireUserId();
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Address not found"));
        apply(address, request, userId);
        return mapper.toAddress(address);
    }

    @Transactional
    public void deleteAddress(Long id) {
        Long userId = securityUtils.requireUserId();
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Address not found"));
        addressRepository.delete(address);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerSummary> adminCustomers(String q, int page, int size) {
        String query = StringUtils.hasText(q) ? q.trim() : "";
        return PageResponse.from(userRepository.searchCustomers(
                        RoleName.USER,
                        query,
                        PageRequest.of(page, Math.min(size, 40), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toCustomer));
    }

    @Transactional(readOnly = true)
    public ProfileResponse adminCustomer(Long id) {
        AppUser user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Customer not found"));
        if (!user.hasRole(RoleName.USER)) {
            throw ApiException.notFound("Customer not found");
        }
        return toProfile(user);
    }

    @Transactional
    public CustomerSummary updateCustomerStatus(Long id, boolean enabled) {
        AppUser user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Customer not found"));
        if (user.hasRole(RoleName.ADMIN)) {
            throw ApiException.forbidden("Admin accounts cannot be changed here");
        }
        user.setEnabled(enabled);
        return toCustomer(user);
    }

    private void apply(Address address, AddressRequest request, Long userId) {
        if (request.defaultAddress()) {
            addressRepository.clearDefault(userId);
        }
        address.setFullName(request.fullName().trim());
        address.setPhone(request.phone().trim());
        address.setLine1(request.line1().trim());
        address.setLine2(request.line2());
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPostalCode(request.postalCode().trim());
        address.setCountry(StringUtils.hasText(request.country()) ? request.country().trim() : "India");
        address.setDefaultAddress(request.defaultAddress());
    }

    private ProfileResponse toProfile(AppUser user) {
        return new ProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getCreatedAt(),
                orderRepository.countByUserId(user.getId()),
                Money.scale(orderRepository.sumSpentByUser(user.getId())),
                addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(user.getId()).stream().map(mapper::toAddress).toList()
        );
    }

    private CustomerSummary toCustomer(AppUser user) {
        return new CustomerSummary(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.isEnabled(),
                user.getCreatedAt(),
                orderRepository.countByUserId(user.getId()),
                Money.scale(orderRepository.sumSpentByUser(user.getId()))
        );
    }
}
