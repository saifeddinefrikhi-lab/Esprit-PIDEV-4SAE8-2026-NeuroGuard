# Pharmacy Service - NeuroGuard

## 📱 Overview

A professional pharmacy localization microservice that allows patients to find nearby pharmacies with advanced filtering and distance calculation.

**Features:**
- 🗺️ Geolocation-based pharmacy search
- 📏 Distance calculation using Haversine formula
- 🔍 Search filtering (name, services, hours)
- 🚚 Delivery service detection
- ⏰ Opening hours and 24h pharmacy support
- 📞 Contact & directions integration

---

## 🏗️ Architecture

### Backend Stack
- **Framework:** Spring Boot 3.2.4
- **Database:** MySQL (pharmacydb)
- **Service Discovery:** Eureka Client
- **Security:** JWT + Spring Security
- **Java:** 17
- **Build Tool:** Maven

### Frontend Integration
- **Framework:** Angular 21
- **Standalone Component:** PharmacyLocatorComponent
- **Geolocation API:** Browser's native geolocation
- **HTTP Client:** Angular HttpClient

---

## 📦 Installation

### 1. Database Setup

```bash
# No manual setup required - Hibernate will create tables automatically
# But if needed, create database:
# CREATE DATABASE pharmacydb;
```

### 2. Start the Service

```bash
cd pharmacy-service

# Build
mvn clean install

# Run
mvn spring-boot:run
```

**Expected output:**
```
... Started PharmacyServiceApplication in X seconds
... Registered instance PHARMACY-SERVICE with status UP
```

**Port:** 8085
**Eureka Status:** http://localhost:8761

### 3. Load Test Data

```bash
# Connect to MySQL
mysql -u root -p

# Use pharmacy database
use pharmacydb;

# Load test data from test_data.sql
source test_data.sql;

# Verify data loaded
SELECT COUNT(*) FROM pharmacies;  -- Should return 10
```

---

## 🔌 API Endpoints

### Base URL
```
http://localhost:8083/api/pharmacies  (via Gateway)
or
http://localhost:8085/api/pharmacies  (direct)
```

### Endpoints

#### 1. Get All Pharmacies
```
GET /api/pharmacies
```
Response: Array of all pharmacies

#### 2. Get Pharmacy by ID
```
GET /api/pharmacies/{id}
```
Response: Single pharmacy details

#### 3. Find Nearby Pharmacies ⭐ MAIN ENDPOINT
```
POST /api/pharmacies/nearby

Body:
{
  "patientLatitude": 48.8566,
  "patientLongitude": 2.3522,
  "radiusKm": 10,
  "openNowOnly": false
}

Response: Array of pharmacies sorted by distance
[
  {
    "id": 1,
    "name": "Pharmacie Centrale",
    "address": "123 Rue de la Paix",
    "phoneNumber": "+33 1 23 45 67 89",
    "latitude": 48.8566,
    "longitude": 2.3522,
    "distance": 0.67,
    "openNow": true,
    "hasDelivery": true,
    "accepts24h": false,
    ...
  }
]
```

#### 4. Search by Name
```
GET /api/pharmacies/search?name=Centrale
```

#### 5. Get Open Pharmacies
```
GET /api/pharmacies/open
```

#### 6. Get Pharmacies with Delivery
```
GET /api/pharmacies/delivery
```

#### 7. Get 24-Hour Pharmacies
```
GET /api/pharmacies/24hours
```

#### 8. Create Pharmacy (Admin)
```
POST /api/pharmacies

Body:
{
  "name": "Pharmacie Test",
  "address": "999 Rue Test",
  "phoneNumber": "+33 1 00 00 00 00",
  "latitude": 48.8566,
  "longitude": 2.3522,
  "hasDelivery": true,
  "accepts24h": false,
  ...
}
```

#### 9. Update Pharmacy (Admin)
```
PUT /api/pharmacies/{id}
Body: {...pharmacy data...}
```

#### 10. Delete Pharmacy (Admin)
```
DELETE /api/pharmacies/{id}
```

---

## 🧮 Distance Calculation Algorithm

### Haversine Formula
Used to calculate the great-circle distance between two points on a sphere.

**Formula:**
```
a = sin²(Δlat/2) + cos(lat1) × cos(lat2) × sin²(Δlon/2)
c = 2 × asin(√a)
d = R × c

Where:
- R = 6371 km (Earth's radius)
- lat, lon in radians
```

### Frontend Implementation
```typescript
calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const earthRadiusKm = 6371;
  const dLat = this.degreesToRadians(lat2 - lat1);
  const dLon = this.degreesToRadians(lon2 - lon1);
  
  const a = Math.sin(dLat/2)*Math.sin(dLat/2) + 
            Math.cos(this.degreesToRadians(lat1))*
            Math.cos(this.degreesToRadians(lat2))*
            Math.sin(dLon/2)*Math.sin(dLon/2);
  
  const c = 2 * Math.asin(Math.sqrt(a));
  return earthRadiusKm * c;
}
```

### Backend Implementation (SQL)
```sql
SELECT * FROM pharmacies WHERE 
(6371 * acos(
  cos(radians(?)) * cos(radians(latitude)) * 
  cos(radians(longitude) - radians(?)) + 
  sin(radians(?)) * sin(radians(latitude))
)) <= ?
```

---

## 🎯 Project Structure

```
pharmacy-service/
├── pom.xml
├── test_data.sql
├── src/main/
│   ├── java/com/neuroguard/pharmacy/
│   │   ├── PharmacyServiceApplication.java
│   │   ├── controllers/
│   │   │   └── PharmacyController.java
│   │   ├── services/
│   │   │   └── PharmacyService.java
│   │   ├── repositories/
│   │   │   └── PharmacyRepository.java
│   │   ├── entities/
│   │   │   └── Pharmacy.java
│   │   ├── dto/
│   │   │   ├── PharmacyDto.java
│   │   │   └── PharmacyLocationRequest.java
│   │   └── utils/
│   │       └── GeoDistanceCalculator.java
│   └── resources/
│       └── application.yml
└── src/test/
    └── (tests to be added)
```

---

## 📊 Database Schema

### Table: pharmacies
```sql
Column              | Type          | Notes
--------------------|--------------|----------------------
id                 | BIGINT        | Primary Key, Auto-increment
name               | VARCHAR(100)  | Pharmacy name
address            | VARCHAR(255)  | Full address
phone_number       | VARCHAR(20)   | Contact number
latitude           | DECIMAL(10,8) | GPS latitude
longitude          | DECIMAL(11,8) | GPS longitude
description        | TEXT          | Pharmacy description
open_now           | BOOLEAN       | Currently open
opening_time       | TIME          | Opening hour
closing_time       | TIME          | Closing hour
email              | VARCHAR(255)  | Email address
has_delivery       | BOOLEAN       | Delivery available
accepts_24h        | BOOLEAN       | Open 24 hours
specialities       | TEXT          | Specialties list
image_url          | TEXT          | Image URL
created_at         | TIMESTAMP     | Creation date

Indexes:
- PRIMARY KEY (id)
- INDEX idx_lat_lon (latitude, longitude)
```

---

## 🧪 Testing

### Using curl

**Test 1: Get all pharmacies**
```bash
curl http://localhost:8083/api/pharmacies
```

**Test 2: Find nearby pharmacies**
```bash
curl -X POST http://localhost:8083/api/pharmacies/nearby \
  -H "Content-Type: application/json" \
  -d '{
    "patientLatitude": 48.8566,
    "patientLongitude": 2.3522,
    "radiusKm": 10,
    "openNowOnly": false
  }'
```

**Test 3: Search by name**
```bash
curl "http://localhost:8083/api/pharmacies/search?name=Centrale"
```

**Test 4: Get 24h pharmacies**
```bash
curl http://localhost:8083/api/pharmacies/24hours
```

### Using Postman

1. Import the API collection (to be created)
2. Set `{{baseUrl}}` to `http://localhost:8083`
3. Execute requests with examples provided

---

## 🔐 Security

### JWT Authentication
- All endpoints protected by JWT filter
- Token validation automatic via `JwtAuthenticationFilter`
- Default user: `admin/admin123` (configurable)

### CORS
Configured at Gateway level:
- Allows: `http://localhost:4200`
- Methods: GET, POST, PUT, DELETE, OPTIONS
- Credentials: Allowed

### Admin-Only Operations
- POST (create pharmacy)
- PUT (update pharmacy)
- DELETE (delete pharmacy)

To restrict these, add `@PreAuthorize("hasRole('ADMIN')")` to controller methods.

---

## 🚀 Integration with NeuroGuard

### Frontend Routes
```typescript
// In app-routing.module.ts
{
  path: 'patient/pharmacy',
  loadComponent: () => import('./Front-office/patient/pharmacy/pharmacy-locator/pharmacy-locator.component')
    .then((c) => c.PharmacyLocatorComponent)
}
```

### Navigation Menu
```typescript
// In patient-layout/navigation/navigation.ts
{
  id: 'pharmacy',
  title: 'Localiser Pharmacies',
  type: 'item',
  url: '/patient/pharmacy',
  icon: 'shop',
  breadcrumbs: false
}
```

### Gateway Routes
```yaml
# In gateway/application.yaml
- id: pharmacies
  uri: lb://pharmacy-service
  predicates:
    - Path=/api/pharmacies/**
```

---

## 📱 Frontend Usage

### Basic Usage
```typescript
import { PharmacyService } from '@app/core/services/pharmacy.service';

constructor(private pharmacyService: PharmacyService) {}

// Get current location
this.pharmacyService.getCurrentLocation()
  .then(location => {
    this.userLatitude = location.latitude;
    this.userLongitude = location.longitude;
  });

// Find nearby pharmacies
this.pharmacyService.findNearbyPharmacies(
  48.8566,  // latitude
  2.3522,   // longitude
  10,       // radius in km
  false     // openNowOnly
).subscribe(pharmacies => {
  this.pharmacies = pharmacies;
});
```

### Component Integration
The `PharmacyLocatorComponent` provides:
- Automatic geolocation
- Search functionality
- Multiple filtering options
- Detail view panel
- Direct call, map, and email actions
- Responsive UI

---

## 📈 Performance Notes

### Database
- Use index on (latitude, longitude) for spatial queries
- Consider GiST index for PostGIS if migrating to PostgreSQL

### Frontend
- Geolocation is requested asynchronously (non-blocking)
- Components use `ChangeDetectionStrategy.OnPush`
- RxJS `takeUntil` prevents memory leaks

### Optimization Tips
- Implement pagination for large pharmacies list
- Cache geolocation results
- Add request debouncing for realtime search
- Consider WebSocket for live updates

---

## 🔮 Future Enhancements

- [ ] Google Maps/Leaflet integration
- [ ] Rating and reviews system
- [ ] Favorites/bookmarks
- [ ] Waiting time estimates
- [ ] Prescription matching
- [ ] Insurance acceptance verification
- [ ] Integration with prescription service
- [ ] Push notifications for nearby pharmacies
- [ ] Photo gallery
- [ ] Payment integration for online orders

---

## 🆘 Troubleshooting

### Issue: Service not showing in Eureka
**Solution:** Ensure Eureka server is running on port 8761

### Issue: Database not created
**Solution:** Hibernate should auto-create. If not, manually create:
```sql
CREATE DATABASE IF NOT EXISTS pharmacydb;
```

### Issue: Geolocation permission denied
**Solution:** Browser permission required. On development, use localhost (HTTPS not required)

### Issue: CORS errors
**Solution:** Verify gateway CORS configuration includes your frontend URL

### Issue: Distance calculation incorrect
**Solution:** Verify latitude/longitude format (decimal degrees, not radians)

---

## 📚 References

- **Haversine Formula:** https://en.wikipedia.org/wiki/Haversine_formula
- **Spring Boot Docs:** https://spring.io/projects/spring-boot
- **Angular Docs:** https://angular.io/docs
- **MySQL Geolocation:** https://dev.mysql.com/doc/

---

## 📄 License

This component is part of the NeuroGuard project.

---

**Last Updated:** April 2026  
**Version:** 1.0.0  
**Maintainer:** NeuroGuard Development Team
