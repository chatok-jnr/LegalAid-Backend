package com.legalaid.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class UpdateServiceRequest {

    @Size(max = 200)
    private String title;

    @Size(max = 100)
    private String category;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @Min(value = 1)
    @Max(value = 365)
    private Integer deliveryDays;

    @Min(value = 0)
    private Integer revisions;

    @Size(max = 5000)
    private String description;

    private Boolean isActive;

    // When provided, replaces all existing sub-items entirely
    @Valid
    private List<CreateServiceRequest.MediaItem>   media;

    @Valid
    private List<CreateServiceRequest.TextItem>    highlights;

    @Valid
    private List<CreateServiceRequest.TextItem>    features;

    @Valid
    private List<CreateServiceRequest.FaqItem>     faqs;
}