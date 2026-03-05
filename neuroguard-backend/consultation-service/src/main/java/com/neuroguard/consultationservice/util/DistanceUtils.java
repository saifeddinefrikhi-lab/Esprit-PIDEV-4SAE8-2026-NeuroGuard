package com.neuroguard.consultationservice.util;

/**
 * Calcul de la distance à vol d'oiseau (formule de Haversine) entre deux points sur la sphère terrestre.
 * La distance est retournée en kilomètres.
 */
public final class DistanceUtils {

    /** Rayon moyen de la Terre en kilomètres */
    private static final double EARTH_RADIUS_KM = 6371.0;

    private DistanceUtils() {}

    /**
     * Calcule la distance orthodromique (à vol d'oiseau) entre deux points en kilomètres.
     *
     * @param lat1 Latitude du premier point (degrés)
     * @param lon1 Longitude du premier point (degrés)
     * @param lat2 Latitude du second point (degrés)
     * @param lon2 Longitude du second point (degrés)
     * @return Distance en kilomètres
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
