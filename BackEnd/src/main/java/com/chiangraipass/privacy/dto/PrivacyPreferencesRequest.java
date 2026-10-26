package com.chiangraipass.privacy.dto;

import java.util.List;

import com.chiangraipass.privacy.model.PrivacyPurpose;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrivacyPreferencesRequest(
        @NotBlank(message = "The privacy notice version is required.")
        @Size(max = 80, message = "The privacy notice version is too long.")
        String noticeVersion,
        @NotEmpty(message = "Choose at least one option.")
        @Size(max = 10, message = "Too many choices were sent.")
        @Valid List<Choice> choices) {

    public record Choice(
            @NotNull(message = "The purpose is required.") PrivacyPurpose purpose,
            @NotNull(message = "The choice is required.") Boolean granted) {
    }
}