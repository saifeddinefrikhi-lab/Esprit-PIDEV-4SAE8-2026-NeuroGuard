# Guide de Dépannage - Erreur 500 POST /api/consultations

## Causes Communes de l'Erreur 500

### 1. **Erreur de Connexion à la Base de Données**
Si vous voyez une erreur liée à MySQL:
```
java.sql.SQLException: Access denied for user
```

**Solution:**
- Vérifiez le fichier `application.yaml`
- Assurez-vous que les identifiants MySQL sont corrects:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/consultation_db?createDatabaseIfNotExist=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root          # ✅ À vérifier
    password: root          # ✅ À vérifier
```
- Vérifiez que MySQL est en cours d'exécution: `mysql -u root -p root`
- La base de données `consultation_db` sera créée automatiquement

### 2. **Erreur de Connexion au Service Utilisateur**
Si vous voyez une erreur FeignException:
```
com.netflix.client.ClientException: No instances available
```

**Solution:**
- Vérifiez que le service utilisateur est en cours d'exécution sur le port 8081
- Assurez-vous que Eureka est accessible ou configurez une URL directe
- Vérifiez les logs pour plus de détails

### 3. **Erreur JWT (Token Invalide)**
Si vous voyez:
```
org.springframework.security.authentication.BadCredentialsException
```

**Solution:**
- Vérifiez que le token JWT est envoyé dans l'en-tête `Authorization: Bearer <token>`
- Assurez-vous que la clé secrète JWT dans `application.yaml` correspond à celle du service utilisateur
- Vérifiez que le token n'a pas expiré

### 4. **Erreur de Validation de Requête**
Si vous voyez une erreur de validation:
```
MethodArgumentNotValidException
```

**Solution:**
- Vérifiez que le corps de la requête est valide:
```json
{
  "title": "Consultation Title",
  "description": "Description",
  "startTime": "2024-02-25T10:00:00",
  "endTime": "2024-02-25T11:00:00",
  "type": "ONLINE",
  "patientId": 1,
  "caregiverId": null
}
```
- Les champs obligatoires sont: `title`, `startTime`, `type`, `patientId`

### 5. **Erreur de Création de Réunion Zoom**
Si vous voyez:
```
IllegalStateException: Impossible de créer la réunion Zoom
```

**Solution:**
- Le service ZoomService est actuellement simulé, ne pas modifier
- En production, configurer les identifiants Zoom OAuth2

### 6. **Patient ou Soignant Invalide**
Si vous voyez:
```
L'utilisateur spécifié n'est pas un patient valide
L'utilisateur spécifié n'est pas un soignant valide
```

**Solution:**
- Vérifiez que l'ID du patient existe dans le service utilisateur
- Vérifiez que l'ID du soignant existe dans le service utilisateur et a le rôle CAREGIVER
- Assurez-vous que le service utilisateur est accessible

## Logs de Débogage

### Activer les Logs Debug
Modifiez `application.yaml`:
```yaml
logging:
  level:
    com.neuroguard.consultationservice: DEBUG
    org.springframework.security: DEBUG
    org.springframework.data.jpa: DEBUG
```

### Afficher les Logs Hibernates
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

## Requête de Test Curl

```bash
# 1. Obtenir un token (depuis le service utilisateur)
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"provider_user","password":"password"}'

# 2. Créer une consultation avec le token
curl -X POST http://localhost:8084/api/consultations \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Consultation Test",
    "description": "Test consultation",
    "startTime": "2024-02-25T10:00:00",
    "endTime": "2024-02-25T11:00:00",
    "type": "ONLINE",
    "patientId": 1,
    "caregiverId": null
  }'
```

## Vérifications Préalables

1. **Base de Données:**
   ```bash
   mysql -u root -p root -e "SHOW DATABASES;" | grep consultation_db
   ```

2. **Services en Cours d'Exécution:**
   - Eureka: http://localhost:8761
   - User Service: http://localhost:8081
   - Consultation Service: http://localhost:8084

3. **Connectivité Réseau:**
   ```bash
   curl -I http://localhost:8081/actuator/health
   curl -I http://localhost:8084/actuator/health
   ```

## Ressources

- Configuration: `src/main/resources/application.yaml`
- Exception Handler: `src/main/java/com/neuroguard/consultationservice/exception/GlobalExceptionHandler.java`
- Service: `src/main/java/com/neuroguard/consultationservice/service/ConsultationService.java`

