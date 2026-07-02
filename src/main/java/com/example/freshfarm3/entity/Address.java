package com.example.freshfarm3.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @NotBlank(message = "Address line is required")
    @Size(max = 300)
    @Column(name = "address_line", nullable = false, length = 300)
    private String addressLine;

    @NotBlank(message = "City is required")
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Enter a valid 6-digit pincode")
    @Column(name = "pincode", nullable = false, length = 6)
    private String pincode;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;
}