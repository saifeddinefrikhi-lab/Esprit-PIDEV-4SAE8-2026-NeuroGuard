import { Component, OnInit, ViewChild, ElementRef, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DistanceService } from '../../../services/distance.service';
import { User } from '../../../core/models/user.model';
import { Consultation } from '../../../core/models/consultation.model';
import { HttpClient } from '@angular/common/http';

// Déclaration Google Maps API
declare let google: any;

/**
 * Composant pour trouver et afficher les médecins les plus proches sur une carte.
 * Features:
 * - Utilise la position actuelle du patient (géolocalisation)
 * - Affiche les médecins sur carte Google Maps
 * - Montre distance réelle + ETA pour chaque médecin
 * - Filtre par mode de transport (voiture, pied, transports)
 * - Permet sélection et réservation de consultation
 */
@Component({
  selector: 'app-find-nearby-doctors',
  templateUrl: './find-nearby-doctors.component.html',
  styleUrls: ['./find-nearby-doctors.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class FindNearbyDoctorsComponent implements OnInit {

  @ViewChild('googleMap', { static: false }) mapElement!: ElementRef;

  // Position du patient
  patientLocation: any = null;
  patientLocationError: string = '';

  // Paramètres de recherche
  selectedTransportMode: string = 'DRIVING';
  searchRadius: number = 50;
  maxResults: number = 10;

  // Résultats
  closestProviders: any[] = [];
  filteredProviders: any[] = [];
  selectedProvider: any = null;

  // États
  isSearching: boolean = false;
  isLoadingLocation: boolean = false;
  resultCount: number = 0;
  
  // Debounce timer pour éviter les recherches multiples
  private debounceTimer: any = null;
  private lastSearchTime: number = 0;

  // Modes de transport disponibles
  transportModes = [
    { value: 'DRIVING', label: 'Voiture' },
    { value: 'WALKING', label: 'Pied' },
    { value: 'TRANSIT', label: 'Transports en commun' },
    { value: 'BICYCLING', label: 'Vélo' }
  ];

  // Carte Google
  map: any;
  markers: any[] = [];
  userMarker: any;

  // Toast notifications
  successMessage: string = '';
  errorMessage: string = '';

  // Disponibilité des médecins (mock data pour demo)
  providerAvailability: Map<number, boolean> = new Map();

  // Injection des services
  private distanceService = inject(DistanceService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.initializeMap();
    this.getCurrentLocation();
  }

  /**
   * Initialise la carte Google Maps.
   */
  private initializeMap(): void {
    // La vraie initialisation se fera quand le composant est rendu
    // Google Maps API doit être chargée dans index.html avec votre clé API
    setTimeout(() => {
      if (this.mapElement && typeof google !== 'undefined') {
        const defaultLocation = { lat: 48.8566, lng: 2.3522 };  // Paris
        this.map = new google.maps.Map(this.mapElement.nativeElement, {
          zoom: 12,
          center: defaultLocation,
          styles: [
            {
              featureType: 'poi',
              stylers: [{ visibility: 'off' }]
            }
          ]
        });
      }
    }, 500);
  }

  /**
   * Récupère la position GPS actuelle du patient.
   */
  public getCurrentLocation(): void {
    this.isLoadingLocation = true;
    this.patientLocationError = '';
    this.successMessage = '';

    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          this.patientLocation = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy
          };

          this.isLoadingLocation = false;
          this.successMessage = 'Position obtenue avec succès. Cliquez sur "Rechercher" pour trouver les médecins.';

          // Placer le marker du patient sur la carte
          if (this.map) {
            this.updateMapWithPatientLocation();
          }

          // NE PAS appeler searchClosestProviders automatiquement
          // L'utilisateur doit cliquer sur "Rechercher"
        },
        (error) => {
          this.isLoadingLocation = false;
          this.patientLocationError = this.getGeolocationErrorMessage(error.code);
          
          // Fallback: utiliser localisation de the localStorage si existe, sinon Paris par défaut
          const storedLat = localStorage.getItem('patientLat');
          const storedLon = localStorage.getItem('patientLon');
          
          if (storedLat && storedLon) {
             this.patientLocation = {
                latitude: parseFloat(storedLat),
                longitude: parseFloat(storedLon)
             };
             this.patientLocationError = 'Position obtenue depuis vos paramètres.';
             this.successMessage = 'Position connue. Cliquez sur "Rechercher".';
          } else {
             this.patientLocation = {
               latitude: 48.8566,
               longitude: 2.3522
             };
          }
          this.updateMapWithPatientLocation();
          console.warn('Geolocation error, using fallback location:', error);
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 0
        }
      );
    } else {
      this.isLoadingLocation = false;
      this.patientLocationError = 'Géolocalisation non supportée par votre navigateur';
    }
  }

  /**
   * Met à jour la carte avec la position du patient.
   */
  private updateMapWithPatientLocation(): void {
    if (!this.map || !this.patientLocation) return;

    // Ajouter/update marker du patient
    if (this.userMarker) {
      this.userMarker.setPosition({
        lat: this.patientLocation.latitude,
        lng: this.patientLocation.longitude
      });
    } else {
      this.userMarker = new google.maps.Marker({
        position: {
          lat: this.patientLocation.latitude,
          lng: this.patientLocation.longitude
        },
        map: this.map,
        title: 'Votre position',
        icon: 'http://maps.google.com/mapfiles/ms/icons/blue-dot.png'
      });
    }

    // Centrer la carte
    this.map.setCenter({
      lat: this.patientLocation.latitude,
      lng: this.patientLocation.longitude
    });
  }

  /**
   * Recherche les médecins les plus proches avec debouncing.
   * Évite les appels API trop rapides en succession.
   */
  public searchClosestProviders(): void {
    // Debounce: vérifier si suffisamment de temps s'est écoulé
    const now = Date.now();
    if (now - this.lastSearchTime < 1000) {
      console.log('[Debounce] Search called too soon, skipping API call');
      return;
    }

    if (!this.patientLocation) {
      this.errorMessage = 'Position du patient non disponible';
      return;
    }

    // Mettre à jour le timestamp du dernier appel
    this.lastSearchTime = now;

    // Arrêter tout timer de debounce précédent
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }

    this.isSearching = true;
    this.errorMessage = '';
    this.successMessage = '';

    // Appel direct au backend qui gère les providers sans avoir besoin de passer de liste
    this.distanceService.getNearestProviders(
      this.patientLocation.latitude,
      this.patientLocation.longitude,
      this.selectedTransportMode,
      this.searchRadius,
      this.maxResults
    ).subscribe(
      (response) => {
        this.isSearching = false;
        this.closestProviders = response.providers || [];
        this.filteredProviders = this.closestProviders;
        this.resultCount = response.resultCount || 0;

        if (this.closestProviders.length > 0) {
          this.successMessage = `${this.resultCount} médecin(s) trouvé(s) à proximité`;
          this.updateMapWithProviders();
        } else {
          this.errorMessage = `Aucun médecin trouvé dans un rayon de ${this.searchRadius} km`;
        }
        this.cdr.markForCheck();
      },
      (error) => {
        this.isSearching = false;
        this.errorMessage = 'Erreur lors de la recherche: ' + (error.error?.message || error.message);
        this.cdr.markForCheck();
        console.error('Search error:', error);
      }
    );
  }

  /**
   * Met à jour la carte avec les marqueurs des médecins.
   */
  private updateMapWithProviders(): void {
    if (!this.map) return;

    // Nettoyer les anciens marqueurs
    this.markers.forEach(marker => marker.setMap(null));
    this.markers = [];

    // Ajouter nouveaux marqueurs pour chaque médecin
    this.closestProviders.forEach((provider, index) => {
      const marker = new google.maps.Marker({
        position: {
          lat: provider.location.latitude,
          lng: provider.location.longitude
        },
        map: this.map,
        title: provider.providerName || `Médecin ${index + 1}`,
        label: (index + 1).toString()
      });

      // Ajouter info window avec détails
      const infoContent = this.getProviderInfoWindowContent(provider);
      const infoWindow = new google.maps.InfoWindow({
        content: infoContent
      });

      marker.addListener('click', () => {
        infoWindow.open(this.map, marker);
        this.selectProvider(provider);
      });

      this.markers.push(marker);
    });

    // Ajuster les limites de la carte pour afficher tous les marqueurs
    if (this.markers.length > 0) {
      const bounds = new google.maps.LatLngBounds();
      if (this.userMarker) {
        bounds.extend(this.userMarker.getPosition());
      }
      this.markers.forEach(marker => bounds.extend(marker.getPosition()));
      this.map.fitBounds(bounds);
    }
  }

  /**
   * Génère le contenu de l'info window pour un médecin.
   */
  private getProviderInfoWindowContent(provider: any): string {
    const duration = provider.estimatedDurationHuman || 'N/A';
    const distance = provider.roadDistanceKm || provider.distanceKm;

    return `<div style="max-width: 300px;">
      <h4>${provider.providerName || 'Médecin'}</h4>
      <p><strong>Spécialité:</strong> ${provider.specialty || 'Généraliste'}</p>
      <p><strong>Distance:</strong> ${distance} km</p>
      <p><strong>Durée:</strong> ${duration}</p>
      <p><strong>Notation:</strong> ${provider.rating ? provider.rating + '/5' : 'N/A'}</p>
      <button class="btn btn-sm btn-primary" onclick="alert('Réservation: disponible dans l\\'interface complète')">
        Réserver
      </button>
    </div>`;
  }

  /**
   * Sélectionne un médecin.
   */
  public selectProvider(provider: any): void {
    this.selectedProvider = provider;
  }

  /**
   * Affiche les directions vers un médecin sur Google Maps.
   */
  public showDirections(provider: any): void {
    if (!this.patientLocation) return;

    const from = `${this.patientLocation.latitude},${this.patientLocation.longitude}`;
    const to = `${provider.location.latitude},${provider.location.longitude}`;
    const modeMap: {[key: string]: string} = {
      'DRIVING': 'd',
      'WALKING': 'w',
      'TRANSIT': 'r',
      'BICYCLING': 'b'
    };
    const mode = modeMap[this.selectedTransportMode] || 'd';

    const url = `https://www.google.com/maps/dir/?api=1&origin=${from}&destination=${to}&travelmode=${mode}`;
    window.open(url, '_blank');
  }

  /**
   * Change le mode de transport et relance la recherche.
   */
  public onTransportModeChange(): void {
    this.searchClosestProviders();
  }

  /**
   * Change le rayon de recherche et relance la recherche.
   */
  public onRadiusChange(): void {
    this.searchClosestProviders();
  }

  /**
   * Obtient le message d'erreur de géolocalisation approprié.
   */
  private getGeolocationErrorMessage(code: number): string {
    switch (code) {
      case 1:
        return 'Permission de géolocalisation refusée';
      case 2:
        return 'Position non disponible';
      case 3:
        return 'Délai d\'attente dépassé';
      default:
        return 'Erreur de géolocalisation';
    }
  }
}
