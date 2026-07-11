package com.example.freshfarm3.controller;

import com.example.freshfarm3.dto.request.AddressRequest;
import com.example.freshfarm3.dto.response.AddressResponse;
import com.example.freshfarm3.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AddressController — buyer delivery-address management.
 *
 * NOTE: SecurityConfig already reserved "/api/addresses/**" for the BUYER
 * role, but no controller previously implemented it — checkout's
 * addressId therefore had no way to be created via the API. This fills
 * that gap using the same AppUserDetails / email-lookup pattern used
 * throughout the rest of the codebase (see CartController, CartService).
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUYER')")
public class AddressController {

    private final AddressService addressService;

    // POST /api/addresses
    @PostMapping
    public ResponseEntity<AddressResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddressRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.createAddress(userDetails.getUsername(), req));
    }

    // GET /api/addresses/my-addresses
    @GetMapping("/my-addresses")
    public ResponseEntity<List<AddressResponse>> myAddresses(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(addressService.getMyAddresses(userDetails.getUsername()));
    }

    // GET /api/addresses/{id}
    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getOne(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(addressService.getAddress(userDetails.getUsername(), id));
    }

    // PUT /api/addresses/{id}
    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest req) {
        return ResponseEntity.ok(addressService.updateAddress(userDetails.getUsername(), id, req));
    }

    // DELETE /api/addresses/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        addressService.deleteAddress(userDetails.getUsername(), id);
        return ResponseEntity.ok(Map.of("message", "Address deleted successfully"));
    }
}
