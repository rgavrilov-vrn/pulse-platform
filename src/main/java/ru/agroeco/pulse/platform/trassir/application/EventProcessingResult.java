package ru.agroeco.pulse.platform.trassir.application;

public record EventProcessingResult(
        boolean success,
        String error
) {

    public static EventProcessingResult success() {
        return new EventProcessingResult(true, null);
    }

    public static EventProcessingResult failure(String error) {
        return new EventProcessingResult(false, error);
    }
}
