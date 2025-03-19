package com.cloud_storage.common.util;

public class FileSizeConverter {
    private static final String[] UNITS_OF_MEASUREMENT = {"b", "Kb", "Mb"};


    public static String convert(Long originalSize) {
        double finalSize = originalSize;
        String targetUnit = UNITS_OF_MEASUREMENT[0];
        int currentUnit = 1;

        while (currentUnit < UNITS_OF_MEASUREMENT.length && finalSize >= 1024) {
            targetUnit = UNITS_OF_MEASUREMENT[currentUnit++];
            finalSize /= 1024;
        }
        return String.format("%.2f %s", finalSize, targetUnit);
    }


    public static Long convertToBytes(String size) {
        size = size.replace(',', '.');

        String[] parts = size.trim().split(" ");

        double value;
        try {
            value = Double.parseDouble(parts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value: " + parts[0]);
        }

        String unitOfMeasurement = parts[1].toLowerCase();
        return switch (unitOfMeasurement) {
            case "b" -> (long) value;
            case "kb" -> (long) (value * 1024);
            case "mb" -> (long) (value * 1024 * 1024);
            default -> throw new IllegalArgumentException("Unknown unitOfMeasurement: " + unitOfMeasurement);
        };
    }
}
