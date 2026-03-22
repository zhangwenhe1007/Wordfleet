package com.wordfleet.control.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MagicLinkRequest(@NotBlank @Email String email) {
}
