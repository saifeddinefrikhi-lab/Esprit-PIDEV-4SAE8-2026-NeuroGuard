package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.GeoCoordinates;
import com.neuroguard.consultationservice.util.DistanceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Service d'optimisation de routes pour visites multiples (TSP simplifié).
 * Utilisé pour optimiser la tournée d'un médecin visitant plusieurs patients.
 */
@Service
public class OptimizeRouteService {

    private static final Logger logger = LoggerFactory.getLogger(OptimizeRouteService.class);

    /**
     * Résultat de l'optimisation d'un itinéraire.
     */
    public static class OptimizedRoute {
        public List<Integer> optimizedOrder; // Indices des patients dans l'ordre optimal
        public double totalDistanceKm;
        public int totalDurationSeconds;
        public List<String> route; // Chemin lisible (ex: Start → Patient 1 → Patient 3 → Patient 2)
    }

    /**
     * Optimise l'ordre de visite de plusieurs patients (Nearest Neighbor algorithm).
     * Algorithme simplifié et rapide pour N=5-20 patients (cas réel).
     *
     * @param startLocation Localisation de départ du médecin
     * @param patientLocations Localisations des patients
     * @param patientLabels Noms/identifiants des patients (optionnel)
     * @return Route optimisée avec ordre, distance totale, durée
     */
    public OptimizedRoute optimizeRoute(
            GeoCoordinates startLocation,
            List<GeoCoordinates> patientLocations,
            List<String> patientLabels) {

        long startTime = System.currentTimeMillis();
        logger.info("Optimizing route for {} patients", patientLocations.size());

        if (patientLocations.isEmpty()) {
            OptimizedRoute result = new OptimizedRoute();
            result.optimizedOrder = new ArrayList<>();
            result.totalDistanceKm = 0;
            result.totalDurationSeconds = 0;
            result.route = List.of("Start");
            return result;
        }

        // Cas spécial: 1 seul patient
        if (patientLocations.size() == 1) {
            OptimizedRoute result = new OptimizedRoute();
            result.optimizedOrder = List.of(0);
            double dist = DistanceUtils.haversineKm(
                    startLocation.getLatitude(), startLocation.getLongitude(),
                    patientLocations.get(0).getLatitude(), patientLocations.get(0).getLongitude()
            );
            result.totalDistanceKm = Math.round(dist * 100.0) / 100.0;
            result.totalDurationSeconds = estimateDurationSeconds(dist);
            result.route = List.of("Start", getPatientLabel(0, patientLabels));
            return result;
        }

        // Nearest Neighbor algorithm: constructive approach rapide et decent
        List<Integer> order = nearestNeighborAlgorithm(startLocation, patientLocations);

        // Calculer la distance totale de l'itinéraire
        double totalDistance = calculateTotalDistance(startLocation, patientLocations, order);
        int totalDuration = estimateDurationSeconds(totalDistance);

        // Construire le chemin lisible
        List<String> routePath = new ArrayList<>();
        routePath.add("Start");
        for (int idx : order) {
            routePath.add(getPatientLabel(idx, patientLabels));
        }

        long execTime = System.currentTimeMillis() - startTime;
        logger.info("Route optimized in {} ms. Order: {}, Distance: {} km, Duration: {} sec",
                execTime, order, totalDistance, totalDuration);

        OptimizedRoute result = new OptimizedRoute();
        result.optimizedOrder = order;
        result.totalDistanceKm = Math.round(totalDistance * 100.0) / 100.0;
        result.totalDurationSeconds = totalDuration;
        result.route = routePath;

        return result;
    }

    /**
     * Nearest Neighbor Algorithm (greedy):
     * Démarre au point de départ, visite toujours le point non visité le plus proche.
     * Complexité: O(n²), résultat de qualité décente pour problèmes petits-moyens (N < 100).
     */
    private List<Integer> nearestNeighborAlgorithm(
            GeoCoordinates startLocation,
            List<GeoCoordinates> patientLocations) {

        int n = patientLocations.size();
        boolean[] visited = new boolean[n];
        List<Integer> order = new ArrayList<>();

        GeoCoordinates current = startLocation;

        // Trouver le patient le plus proche du point de départ
        int nearest = findNearestUnvisited(current, patientLocations, visited);
        if (nearest >= 0) {
            order.add(nearest);
            visited[nearest] = true;
            current = patientLocations.get(nearest);
        }

        // Ensuite, toujours visiter le patient le plus proche du dernier visité
        for (int i = 1; i < n; i++) {
            nearest = findNearestUnvisited(current, patientLocations, visited);
            if (nearest >= 0) {
                order.add(nearest);
                visited[nearest] = true;
                current = patientLocations.get(nearest);
            }
        }

        return order;
    }

    /**
     * Trouve l'indice du patient non visité le plus proche.
     */
    private int findNearestUnvisited(
            GeoCoordinates current,
            List<GeoCoordinates> locations,
            boolean[] visited) {

        int nearest = -1;
        double minDist = Double.MAX_VALUE;

        for (int i = 0; i < locations.size(); i++) {
            if (!visited[i]) {
                double dist = DistanceUtils.haversineKm(
                        current.getLatitude(), current.getLongitude(),
                        locations.get(i).getLatitude(), locations.get(i).getLongitude()
                );
                if (dist < minDist) {
                    minDist = dist;
                    nearest = i;
                }
            }
        }

        return nearest;
    }

    /**
     * Calcule la distance totale d'un itinéraire partant de startLocation
     * et visitant chaque patient dans l'ordre spécifié.
     */
    private double calculateTotalDistance(
            GeoCoordinates startLocation,
            List<GeoCoordinates> patientLocations,
            List<Integer> order) {

        double totalDist = 0;
        GeoCoordinates current = startLocation;

        for (int idx : order) {
            GeoCoordinates patient = patientLocations.get(idx);
            totalDist += DistanceUtils.haversineKm(
                    current.getLatitude(), current.getLongitude(),
                    patient.getLatitude(), patient.getLongitude()
            );
            current = patient;
        }

        return totalDist;
    }

    /**
     * Estime la durée en secondes basée sur la distance.
     * Hypothèse: vitesse moyenne 40 km/h en ville.
     */
    private int estimateDurationSeconds(double distanceKm) {
        // 40 km/h = 40000 m / 3600 s = ~11.1 m/s
        double speedMs = 40 * 1000.0 / 3600.0;
        return (int) ((distanceKm * 1000) / speedMs);
    }

    /**
     * Récupère le label d'un patient par son indice.
     */
    private String getPatientLabel(int index, List<String> labels) {
        if (labels != null && index < labels.size()) {
            return labels.get(index);
        }
        return "Patient " + (index + 1);
    }

    /**
     * 2-Opt local search improvement (optionnel pour améliorer le résultat Nearest Neighbor).
     * Échange pairs de segments si ça réduit la distance totale.
     * Complexité: O(n²), améliore généralement NN solution de 5-15%.
     */
    public OptimizedRoute optimizeRouteWith2Opt(
            GeoCoordinates startLocation,
            List<GeoCoordinates> patientLocations,
            List<String> patientLabels) {

        // Commencer avec Nearest Neighbor
        OptimizedRoute nnRoute = optimizeRoute(startLocation, patientLocations, patientLabels);

        if (patientLocations.size() <= 2) {
            return nnRoute;  // Pas d'amélioration possible pour petits cas
        }

        List<Integer> bestOrder = new ArrayList<>(nnRoute.optimizedOrder);
        double bestDistance = nnRoute.totalDistanceKm;

        // 2-Opt: essayer de "décroiser" les segments
        boolean improved = true;
        int iterations = 0;
        final int MAX_ITERATIONS = 100;  // Limite pour éviter boucle infinie

        while (improved && iterations < MAX_ITERATIONS) {
            improved = false;
            iterations++;

            for (int i = 0; i < bestOrder.size() - 2; i++) {
                for (int k = i + 2; k < bestOrder.size(); k++) {
                    // Reverser la section [i+1, k] et vérifier si c'est mieux
                    List<Integer> newOrder = apply2OptSwap(bestOrder, i, k);
                    double newDist = calculateTotalDistance(startLocation, patientLocations, newOrder);

                    if (newDist < bestDistance) {
                        bestOrder = newOrder;
                        bestDistance = newDist;
                        improved = true;
                    }
                }
            }
        }

        logger.info("2-Opt improved route from {} km to {} km in {} iterations",
                nnRoute.totalDistanceKm, bestDistance, iterations);

        nnRoute.optimizedOrder = bestOrder;
        nnRoute.totalDistanceKm = Math.round(bestDistance * 100.0) / 100.0;
        nnRoute.totalDurationSeconds = estimateDurationSeconds(bestDistance);

        return nnRoute;
    }

    /**
     * Applique un swap 2-Opt: reverser les éléments entre indices i+1 et k.
     */
    private List<Integer> apply2OptSwap(List<Integer> order, int i, int k) {
        List<Integer> newOrder = new ArrayList<>(order.subList(0, i + 1));
        for (int j = k; j > i; j--) {
            newOrder.add(order.get(j));
        }
        newOrder.addAll(order.subList(k + 1, order.size()));
        return newOrder;
    }
}
