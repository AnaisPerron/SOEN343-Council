package com.council.tenderapp.operator.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ResetRequest(@NotNull @Pattern(regexp = "RESET") String confirmation) {}
