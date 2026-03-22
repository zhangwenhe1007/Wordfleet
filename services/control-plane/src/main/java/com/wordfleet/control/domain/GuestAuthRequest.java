package com.wordfleet.control.domain;

import jakarta.validation.constraints.Size;

public record GuestAuthRequest(@Size(max = 32) String displayName) {
}
