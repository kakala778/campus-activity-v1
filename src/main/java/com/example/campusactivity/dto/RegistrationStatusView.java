package com.example.campusactivity.dto;

public record RegistrationStatusView(
        long registrationCount,
        boolean alreadyRegistered,
        boolean deadlinePassed,
        boolean full
) {

    public boolean canRegister() {
        return !alreadyRegistered && !deadlinePassed && !full;
    }
}
