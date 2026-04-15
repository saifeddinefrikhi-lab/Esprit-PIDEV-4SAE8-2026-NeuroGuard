-- Script SQL de test pour le service Pharmacie (NeuroGuard)
-- À exécuter dans MySQL après la création de la BD pharmacydb

-- Insérer des pharmacies de test (Paris)
INSERT INTO pharmacies (
  name, address, phone_number, latitude, longitude, 
  description, open_now, opening_time, closing_time, 
  email, has_delivery, accepts24h, specialities, created_at
) VALUES

-- Pharmacie 1: Centrale (Ouverte, Livraison, pas 24h)
(
  'Pharmacie Centrale Paris',
  '123 Rue de la Paix, 75001 Paris',
  '+33 1 23 45 67 89',
  48.8566,
  2.3522,
  'Pharmacie moderne en centre-ville avec accès facile et parking',
  TRUE,
  '08:00:00',
  '20:00:00',
  'contact@pharmacie-centrale.fr',
  TRUE,
  FALSE,
  'Homéopathie, Parapharmacy, Dispositifs médicaux',
  NOW()
),

-- Pharmacie 2: 24h/24 (Montmartre - toujours ouverte)
(
  'Pharmacie de Nuit Montmartre 24h',
  '456 Avenue Montmartre, 75002 Paris',
  '+33 1 98 76 54 32',
  48.8710,
  2.3420,
  'Pharmacie 24h/24 pour urgences nocturnes',
  TRUE,
  '00:00:00',
  '23:59:59',
  'urgence@pharmacie24.fr',
  TRUE,
  TRUE,
  'Urgences, Service rapide, Conseil pharamacien',
  NOW()
),

-- Pharmacie 3: Livraison rapide (Marais)
(
  'Pharmacie Express Livraison',
  '789 Rue du Marais, 75003 Paris',
  '+33 1 55 55 55 55',
  48.8606,
  2.3628,
  'Spécialiste en livraison rapide (1-2h) sur Paris intra-muros',
  TRUE,
  '07:00:00',
  '22:00:00',
  'livraison@pharmacie-express.fr',
  TRUE,
  FALSE,
  'Livraison express, Téléconsultation, Tests rapides',
  NOW()
),

-- Pharmacie 4: Fermée (Latin Quarter)
(
  'Pharmacie du Quartier Latin',
  '321 Rue Saint-Jacques, 75005 Paris',
  '+33 1 44 44 44 44',
  48.8495,
  2.3472,
  'Pharmacie historique du quartier latin (actuellement fermée le soir)',
  FALSE,
  '09:00:00',
  '18:00:00',
  'contact@pharmacie-latin.fr',
  FALSE,
  FALSE,
  'Medicaments classiques, Conseils',
  NOW()
),

-- Pharmacie 5: Livraison et 24h (Bastille)
(
  'Pharmacie Bastille Premium',
  '555 Avenue de la Bastille, 75004 Paris',
  '+33 1 66 66 66 66',
  48.8530,
  2.3691,
  'Premium pharmacy avec tous les services incluant consultation vidéo',
  TRUE,
  '07:00:00',
  '23:00:00',
  'premium@pharmacie-bastille.fr',
  TRUE,
  FALSE,
  'Consultation vidéo, Produits premium, Conseils nutrition',
  NOW()
),

-- Pharmacie 6: À proximité test (proche Paris)
(
  'Pharmacie Banlieue Proche',
  '999 Boulevard Central, 75006 Paris',
  '+33 1 77 77 77 77',
  48.8570,
  2.3501,
  'Pharmacie conseils avec parking gratuit',
  TRUE,
  '08:30:00',
  '19:30:00',
  'contact@pharmacie-banlieue.fr',
  TRUE,
  FALSE,
  'Parapharmacy, Tests, Vaccins',
  NOW()
),

-- Pharmacie 7: Oubervilliers 
(
  'Pharmacie Saint-Denis Express',
  '123 Avenue Lenin, 93400 Saint-Denis',
  '+33 1 88 88 88 88',
  48.9356,
  2.3569,
  'Pharmacie avec parking et laboratoire d\'analyses sur place',
  TRUE,
  '08:00:00',
  '20:30:00',
  'analyses@pharmacie-stdenise.fr',
  FALSE,
  FALSE,
  'Analyses, Vaccins, Produits génériques',
  NOW()
),

-- Pharmacie 8: Vincennes
(
  'Pharmacie Bois de Vincennes',
  '456 Avenue du Trône, 75012 Paris',
  '+33 1 99 99 99 99',
  48.8439,
  2.4363,
  'Pharmacie proche du bois avec parking gratuit',
  TRUE,
  '08:00:00',
  '20:00:00',
  'contact@pharmacie-vincennes.fr',
  TRUE,
  FALSE,
  'Homéopathie, Naturopathie, Phytothérapie',
  NOW()
),

-- Pharmacie 9: Belleville 24h
(
  'Pharmacie Nuit Belleville',
  '789 Rue de Belleville, 75020 Paris',
  '+33 1 11 11 11 11',
  48.8708,
  2.3901,
  'Pharmacie 24h/24 pour quartier Belleville',
  TRUE,
  '00:00:00',
  '23:59:59',
  'belleville@pharmacienuit.fr',
  TRUE,
  TRUE,
  'Urgences 24h, Service rapide',
  NOW()
),

-- Pharmacie 10: Île de France lointaine
(
  'Pharmacie Versailles Grand',
  '123 Rue Royale, 78000 Versailles',
  '+33 1 22 22 22 22',
  48.8013,
  2.1324,
  'Grande pharmacie à Versailles avec consultation',
  TRUE,
  '08:30:00',
  '19:30:00',
  'versailles@pharmacie-grand.fr',
  TRUE,
  FALSE,
  'Consultations, Homéopathie, Tests de santé',
  NOW()
);

-- Vérifier les données insérées
SELECT COUNT(*) as total_pharmacies FROM pharmacies;

-- Afficher toutes les pharmacies
SELECT id, name, address, latitude, longitude, open_now, has_delivery, accepts24h 
FROM pharmacies 
ORDER BY id;

-- Recherche de test: Pharmacies dans un rayon de 10km autour de Paris (48.8566, 2.3522)
SELECT name, address, 
       ROUND(
         6371 * ACOS(
           COS(RADIANS(48.8566)) * COS(RADIANS(latitude)) * COS(RADIANS(longitude) - RADIANS(2.3522)) +
           SIN(RADIANS(48.8566)) * SIN(RADIANS(latitude))
         ), 2
       ) AS distance_km
FROM pharmacies
HAVING distance_km <= 10
ORDER BY distance_km;

-- Afficher les pharmacies 24h/24
SELECT name, address, phone_number FROM pharmacies WHERE accepts24h = TRUE;

-- Afficher les pharmacies avec livraison
SELECT name, address, phone_number FROM pharmacies WHERE has_delivery = TRUE;

-- Afficher les pharmacies actuellement ouvertes
SELECT name, address, opening_time, closing_time FROM pharmacies WHERE open_now = TRUE;
