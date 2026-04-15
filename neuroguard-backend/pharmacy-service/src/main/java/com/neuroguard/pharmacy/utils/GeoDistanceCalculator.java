package com.neuroguard.pharmacy.utils;

/**
 * Utility class for geographic calculations
 * Uses Haversine formula for calculating distance between two points on Earth
 */
public class GeoDistanceCalculator {

    // Earth radius in kilometers
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculate distance between two geographic points using Haversine formula
     * 
     * @param lat1 Latitude of first point (degrees)
     * @param lon1 Longitude of first point (degrees)
     * @param lat2 Latitude of second point (degrees)
     * @param lon2 Longitude of second point (degrees)
     * @return Distance in kilometers
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine formula
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        double distance = EARTH_RADIUS_KM * c;

        // Return distance rounded to 2 decimal places
        return Math.round(distance * 100.0) / 100.0;
    }

    /**
     * Calculate bearing (angle) from one point to another
     * 
     * @param lat1 Latitude of starting point
     * @param lon1 Longitude of starting point
     * @param lat2 Latitude of ending point
     * @param lon2 Longitude of ending point
     * @return Bearing in degrees (0-360)
     */
    public static double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        double dLon = lon2Rad - lon1Rad;

        double y = Math.sin(dLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                   Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon);

        double bearing = Math.atan2(y, x);
        return (Math.toDegrees(bearing) + 360) % 360;
    }

    /**
     * Check if a point is within a certain radius from another point
     * 
     * @param lat1 Latitude of center point
     * @param lon1 Longitude of center point
     * @param lat2 Latitude of point to check
     * @param lon2 Longitude of point to check
     * @param radiusKm Radius in kilometers
     * @return true if point is within radius, false otherwise
     */
    public static boolean isWithinRadius(double lat1, double lon1, double lat2, double lon2, double radiusKm) {
        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        return distance <= radiusKm;
    }
}
