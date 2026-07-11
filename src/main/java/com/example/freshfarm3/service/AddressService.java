package com.example.freshfarm3.service;

import com.example.freshfarm3.dto.request.AddressRequest;
import com.example.freshfarm3.dto.response.AddressResponse;
import com.example.freshfarm3.entity.Address;
import com.example.freshfarm3.entity.Buyer;
import com.example.freshfarm3.exception.ResourceNotFoundException;
import com.example.freshfarm3.exception.UnauthorizedException;
import com.example.freshfarm3.repository.AddressRepository;
import com.example.freshfarm3.repository.BuyerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AddressService — fills the gap where SecurityConfig reserves
 * "/api/addresses/**" for BUYER but no controller previously implemented it.
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final BuyerRepository   buyerRepository;

    @Transactional
    public AddressResponse createAddress(String buyerEmail, AddressRequest req) {
        Buyer buyer = getBuyer(buyerEmail);

        Address address = Address.builder()
                .fullName(req.getFullName())
                .mobileNumber(req.getMobileNumber())
                .addressLine(req.getAddressLine())
                .city(req.getCity())
                .state(req.getState())
                .pincode(req.getPincode())
                .isDefault(req.isDefault())
                .buyer(buyer)
                .build();

        // First address for a buyer is always the default, regardless of what was requested.
        boolean noAddressesYet = addressRepository.countByBuyer(buyer) == 0;
        if (noAddressesYet) {
            address.setIsDefault(true);
        } else if (req.isDefault()) {
            clearExistingDefault(buyer);
        }

        return toResponse(addressRepository.save(address));
    }

    public List<AddressResponse> getMyAddresses(String buyerEmail) {
        Buyer buyer = getBuyer(buyerEmail);
        return addressRepository.findByBuyer(buyer).stream()
                .map(this::toResponse)
                .toList();
    }

    public AddressResponse getAddress(String buyerEmail, Long addressId) {
        Buyer buyer = getBuyer(buyerEmail);
        Address address = findOwned(buyer, addressId);
        return toResponse(address);
    }

    @Transactional
    public AddressResponse updateAddress(String buyerEmail, Long addressId, AddressRequest req) {
        Buyer buyer = getBuyer(buyerEmail);
        Address address = findOwned(buyer, addressId);

        address.setFullName(req.getFullName());
        address.setMobileNumber(req.getMobileNumber());
        address.setAddressLine(req.getAddressLine());
        address.setCity(req.getCity());
        address.setState(req.getState());
        address.setPincode(req.getPincode());

        if (req.isDefault() && !Boolean.TRUE.equals(address.getIsDefault())) {
            clearExistingDefault(buyer);
            address.setIsDefault(true);
        }

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(String buyerEmail, Long addressId) {
        Buyer buyer = getBuyer(buyerEmail);
        Address address = findOwned(buyer, addressId);
        addressRepository.delete(address);
    }

    // ── helpers ──────────────────────────────────────────────

    private Address findOwned(Buyer buyer, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + addressId));
        if (!address.getBuyer().getId().equals(buyer.getId())) {
            throw new UnauthorizedException("This address does not belong to you");
        }
        return address;
    }

    private void clearExistingDefault(Buyer buyer) {
        addressRepository.findByBuyerAndIsDefaultTrue(buyer)
                .ifPresent(existing -> {
                    existing.setIsDefault(false);
                    addressRepository.save(existing);
                });
    }

    private Buyer getBuyer(String email) {
        return buyerRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found for: " + email));
    }

    private AddressResponse toResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .mobileNumber(a.getMobileNumber())
                .addressLine(a.getAddressLine())
                .city(a.getCity())
                .state(a.getState())
                .pincode(a.getPincode())
                .isDefault(Boolean.TRUE.equals(a.getIsDefault()))
                .build();
    }
}
