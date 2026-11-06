package com.council.tenderapp.ord.dtos;

import jakarta.validation.constraints.NotBlank;

/** A picked image as a data URI, e.g. data:image/jpeg;base64,... */
public record MenuPhotoUploadDto(
    @NotBlank(message = "Photo is required")
    String dataUri
) {}
