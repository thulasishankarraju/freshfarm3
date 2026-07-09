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
}
