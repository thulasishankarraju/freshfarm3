package com.example.freshfarm3.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {

    private Long id;
    private String fullName;
    private String mobileNumber;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;
    private boolean isDefault;

    // Computed, not stored: distance from the farm (Tirupati) in km, and
    // the delivery charge that would apply if this address is selected at
    // checkout. distanceKm is null when the address has no saved
    // coordinates — deliveryCharge still has a flat-rate fallback value.
    private Double distanceKm;
    private java.math.BigDecimal deliveryCharge;
}
