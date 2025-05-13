package com.rore_int.vpp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Entity
@Table(name = "batteries")
@Data
public class Battery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotBlank(message = "Postcode is mandatory")
    @Pattern(regexp = "\\d{4}", message = "Postcode must be a 4-digit number")
    private String postcode;

    @Min(value = 1, message = "Capacity must be positive")
    private Integer capacity;
}