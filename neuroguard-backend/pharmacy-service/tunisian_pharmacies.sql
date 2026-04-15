-- Données de test pour pharmacies tunisiennes
INSERT INTO pharmacies (
  name, address, phone_number, latitude, longitude,
  description, open_now, opening_time, closing_time,
  email, has_delivery, accepts24h, specialities, created_at
) VALUES

-- Pharmacie 1: Tunis Centre
(
  'Pharmacie Ibn Sina',
  'Avenue Habib Bourguiba, Tunis Centre',
  '+216 71 123 456',
  36.8065,
  10.1815,
  'Pharmacie centrale de Tunis avec service de livraison',
  TRUE,
  '08:00:00',
  '22:00:00',
  'contact@ibnsina.tn',
  TRUE,
  FALSE,
  'Médicaments, Parapharmacie, Conseils',
  NOW()
),

-- Pharmacie 2: Lac 1
(
  'Pharmacie du Lac',
  'Avenue Mohamed V, Lac 1, Tunis',
  '+216 71 234 567',
  36.8320,
  10.2380,
  'Pharmacie moderne avec parking et livraison rapide',
  TRUE,
  '09:00:00',
  '21:00:00',
  'info@pharmaciedulac.tn',
  TRUE,
  FALSE,
  'Homéopathie, Produits naturels, Vaccins',
  NOW()
),

-- Pharmacie 3: Ariana
(
  'Pharmacie Ariana Centre',
  'Rue de Palestine, Ariana',
  '+216 71 345 678',
  36.8600,
  10.1900,
  'Pharmacie familiale avec service 24h en cas d\'urgence',
  TRUE,
  '08:00:00',
  '20:00:00',
  'ariana@pharmacie.tn',
  FALSE,
  FALSE,
  'Médicaments essentiels, Premiers soins',
  NOW()
),

-- Pharmacie 4: Sousse
(
  'Pharmacie Sousse Médina',
  'Rue de la Kasbah, Sousse',
  '+216 73 123 456',
  35.8250,
  10.6370,
  'Pharmacie historique avec médicaments traditionnels',
  TRUE,
  '07:00:00',
  '23:00:00',
  'medina@sousse-pharma.tn',
  TRUE,
  FALSE,
  'Médicaments, Huiles essentielles, Plantes médicinales',
  NOW()
),

-- Pharmacie 5: Sfax
(
  'Pharmacie Sfax Centre',
  'Avenue Hedi Chaker, Sfax',
  '+216 74 123 456',
  34.7390,
  10.7600,
  'Grande pharmacie avec service de livraison express',
  TRUE,
  '08:00:00',
  '22:00:00',
  'centre@sfax-pharma.tn',
  TRUE,
  FALSE,
  'Médicaments, Parapharmacie, Cosmétique',
  NOW()
);