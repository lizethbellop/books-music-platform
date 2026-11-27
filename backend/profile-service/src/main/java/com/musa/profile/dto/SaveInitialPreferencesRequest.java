package com.musa.profile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveInitialPreferencesRequest(
        @NotEmpty @Size(max = 20) List<@jakarta.validation.constraints.NotNull @Valid AddPreferenceElementRequest> elements
) {}
