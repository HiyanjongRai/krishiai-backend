package com.krishiai.expert.dto;

public record ExpertScheduleSlotDto(
        Long id,
        String dayOfWeek,
        String timeRange,
        String slotType, // "VIDEO", "AUDIO", "CHAT"
        String status,   // "AVAILABLE", "BOOKED"
        String bookedFarmerName
) {}
