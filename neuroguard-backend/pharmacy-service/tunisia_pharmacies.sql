-- Script SQL de données de test pour le service Pharmacie (NeuroGuard)
-- Pharmacies en Tunisie - À exécuter dans MySQL après la création de la BD pharmacydb

-- Supprimer les anciennes données
DELETE FROM pharmacies;

-- Insérer des pharmacies de test (Tunisie)
INSERT INTO pharmacies (
  name, address, phone_number, latitude, longitude,
  description, open_now, opening_time, closing_time,
  email, has_delivery, accepts24h, specialities, created_at
) VALUES

-- Tunis
(
  'Pharmacie Centrale Tunis',
  'Avenue Habib Bourguiba, Tunis 1000',
  '+216 71 123 456',
  36.8065,
  10.1815,
  'Pharmacie centrale en plein centre-ville de Tunis',
  TRUE,
  '08:00:00',
  '22:00:00',
  'contact@pharmacie-tunis.tn',
  TRUE,
  FALSE,
  'Médicaments, Parapharmacie, Conseils médicaux',
  NOW()
),

(
  'Pharmacie de Nuit El Manar',
  'Rue de la République, El Manar 2092',
  '+216 71 987 654',
  36.8438,
  10.1658,
  'Pharmacie ouverte 24h/24 pour urgences nocturnes',
  TRUE,
  '00:00:00',
  '23:59:59',
  'urgence@pharmacie-manar.tn',
  TRUE,
  TRUE,
  'Urgences, Service rapide, Garde de nuit',
  NOW()
),

(
  'Pharmacie Express Carthage',
  'Avenue de Carthage, Carthage 2016',
  '+216 71 555 777',
  36.8529,
  10.3250,
  'Pharmacie moderne avec livraison express',
  TRUE,
  '07:30:00',
  '21:30:00',
  'express@pharmacie-carthage.tn',
  TRUE,
  FALSE,
  'Livraison rapide, Produits bio, Conseils personnalisés',
  NOW()
),

-- Sfax
(
  'Pharmacie du Centre Sfax',
  'Avenue Hedi Chaker, Sfax 3000',
  '+216 74 123 789',
  34.7398,
  10.7600,
  'Pharmacie historique au centre de Sfax',
  TRUE,
  '08:30:00',
  '20:00:00',
  'centre@pharmacie-sfax.tn',
  FALSE,
  FALSE,
  'Médicaments traditionnels, Herboristerie',
  NOW()
),

(
  'Pharmacie Moderne Sfax',
  'Rue de la Liberté, Sfax 3000',
  '+216 74 456 123',
  34.7325,
  10.7674,
  'Pharmacie moderne avec équipements dernier cri',
  TRUE,
  '09:00:00',
  '22:00:00',
  'moderne@pharmacie-sfax.tn',
  TRUE,
  FALSE,
  'Équipements modernes, Analyses médicales, Vaccins',
  NOW()
),

-- Sousse
(
  'Pharmacie de la Corniche',
  'Boulevard du 14 Janvier, Sousse 4000',
  '+216 73 123 456',
  35.8256,
  10.6369,
  'Pharmacie avec vue sur mer, spécialisée en dermatologie',
  TRUE,
  '08:00:00',
  '21:00:00',
  'corniche@pharmacie-sousse.tn',
  TRUE,
  FALSE,
  'Dermatologie, Cosmétiques, Produits solaires',
  NOW()
),

(
  'Pharmacie Familiale Sousse',
  'Rue de Palestine, Sousse 4000',
  '+216 73 789 012',
  35.8245,
  10.6347,
  'Pharmacie familiale depuis 3 générations',
  TRUE,
  '07:00:00',
  '23:00:00',
  'familiale@pharmacie-sousse.tn',
  TRUE,
  TRUE,
  'Service familial, Urgences, Conseils traditionnels',
  NOW()
),

-- Monastir
(
  'Pharmacie Touristique Monastir',
  'Avenue Habib Bourguiba, Monastir 5000',
  '+216 73 456 789',
  35.7780,
  10.8262,
  'Pharmacie spécialisée pour touristes et expatriés',
  TRUE,
  '08:00:00',
  '20:00:00',
  'touristique@pharmacie-monastir.tn',
  TRUE,
  FALSE,
  'Médicaments internationaux, Traduction, Service touristique',
  NOW()
),

-- Nabeul
(
  'Pharmacie du Cap Bon',
  'Rue Habib Thameur, Nabeul 8000',
  '+216 72 123 456',
  36.4525,
  10.7376,
  'Pharmacie régionale du Cap Bon',
  TRUE,
  '08:30:00',
  '19:30:00',
  'capbon@pharmacie-nabeul.tn',
  FALSE,
  FALSE,
  'Médicaments régionaux, Produits locaux',
  NOW()
),

-- Bizerte
(
  'Pharmacie Maritime Bizerte',
  'Port de Bizerte, Bizerte 7000',
  '+216 72 456 789',
  37.2744,
  9.8739,
  'Pharmacie portuaire spécialisée en médecine maritime',
  TRUE,
  '07:00:00',
  '22:00:00',
  'maritime@pharmacie-bizerte.tn',
  TRUE,
  TRUE,
  'Médecine maritime, Urgences portuaires, Vaccins voyage',
  NOW()
),

-- Gabès
(
  'Pharmacie du Sud Gabès',
  'Avenue de la République, Gabès 6000',
  '+216 75 123 456',
  33.8815,
  10.0982,
  'Pharmacie du sud tunisien',
  TRUE,
  '08:00:00',
  '20:00:00',
  'sud@pharmacie-gabes.tn',
  TRUE,
  FALSE,
  'Médicaments désertiques, Protection solaire extrême',
  NOW()
),

-- Gafsa
(
  'Pharmacie Oasis Gafsa',
  'Rue de l\'Oasis, Gafsa 2100',
  '+216 76 123 456',
  34.4250,
  8.7842,
  'Pharmacie dans la région des oasis',
  TRUE,
  '08:30:00',
  '19:00:00',
  'oasis@pharmacie-gafsa.tn',
  FALSE,
  FALSE,
  'Médicaments régionaux, Produits naturels',
  NOW()
),

-- Kairouan
(
  'Pharmacie Historique Kairouan',
  'Rue de la Grande Mosquée, Kairouan 3100',
  '+216 77 123 456',
  35.6781,
  10.0963,
  'Pharmacie historique près de la Grande Mosquée',
  TRUE,
  '08:00:00',
  '20:00:00',
  'historique@pharmacie-kairouan.tn',
  TRUE,
  FALSE,
  'Médicaments traditionnels, Herbes médicinales',
  NOW()
),

-- Tozeur
(
  'Pharmacie du Désert Tozeur',
  'Avenue de la République, Tozeur 2200',
  '+216 76 456 789',
  33.9197,
  8.1335,
  'Pharmacie spécialisée pour le climat désertique',
  TRUE,
  '08:00:00',
  '21:00:00',
  'desert@pharmacie-tozeur.tn',
  TRUE,
  TRUE,
  'Protection désertique, Médicaments climatiques, Urgences désertiques',
  NOW()
),

-- Djerba
(
  'Pharmacie Touristique Djerba',
  'Zone Touristique, Djerba 4180',
  '+216 75 654 321',
  33.8076,
  10.8451,
  'Pharmacie touristique de Djerba',
  TRUE,
  '07:30:00',
  '23:00:00',
  'touristique@pharmacie-djerba.tn',
  TRUE,
  TRUE,
  'Tourisme médical, Vaccins, Produits solaires',
  NOW()
);

-- Afficher toutes les pharmacies
SELECT id, name, address, latitude, longitude, open_now, has_delivery, accepts24h
FROM pharmacies
ORDER BY id;

-- Afficher les pharmacies 24h/24
SELECT name, address, phone_number FROM pharmacies WHERE accepts24h = TRUE;

-- Afficher les pharmacies avec livraison
SELECT name, address, phone_number FROM pharmacies WHERE has_delivery = TRUE;

-- Calculer les distances depuis Tunis (exemple)
SELECT name, address,
       ROUND(
         6371 * ACOS(
           COS(RADIANS(36.8065)) * COS(RADIANS(latitude)) * COS(RADIANS(longitude) - RADIANS(10.1815)) +
           SIN(RADIANS(36.8065)) * SIN(RADIANS(latitude))
         ), 2
       ) AS distance_km
FROM pharmacies
HAVING distance_km <= 500
ORDER BY distance_km;