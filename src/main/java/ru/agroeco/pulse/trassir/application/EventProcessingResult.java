package ru.agroeco.pulse.trassir.application;

public record EventProcessingResult(
        boolean successful,
        String error
) {

    public static EventProcessingResult success() {
        return new EventProcessingResult(true, null);
    }

    public static EventProcessingResult failure(String error) {
        return new EventProcessingResult(false, error);
    }
}
