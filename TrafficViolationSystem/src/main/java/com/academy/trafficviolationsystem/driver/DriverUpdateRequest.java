package com.academy.trafficviolationsystem.driver;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DriverUpdateRequest {

    @Size(max = 80)
    private String firstName;

    @Size(max = 80)
    private String lastName;

    @Pattern(
            regexp = "^$|^\\+?[1-9]\\d{6,14}$",
            message = "Phone number must be a valid international number (e.g. +14155552671)"
    )
    private String phoneNumber;

    private String Address;

    @Size(max = 20)
    private String licenseCategory;

    @PastOrPresent
    private LocalDate licenseIssuedAt;

    @FutureOrPresent
    private LocalDate licenseExpiresAt;
}
