package com.wordfleet.control.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MagicLinkVerifyRequest(@NotBlank String token, @Size(max = 32) String displayName) {
}
