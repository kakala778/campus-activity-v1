package com.example.campusactivity.dto;

import com.example.campusactivity.entity.Registration;

public record StudentRegistrationView(Registration registration, boolean cancellable) {
}
