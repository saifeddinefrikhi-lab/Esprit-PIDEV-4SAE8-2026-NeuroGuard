# NeuroGuard Assurance Service - Comprehensive Analysis

**Analysis Date:** April 15, 2026  
**Service:** `neuroguard-backend/assurance-service`  
**Port:** 8086

---

## Table of Contents
1. [Complete File Structure](#complete-file-structure)
2. [Main Java Classes & Purposes](#main-java-classes--purposes)
3. [Issues, Bugs & Code Quality Problems](#issues-bugs--code-quality-problems)
4. [The "Optimizer" Functionality](#the-optimizer-functionality)
5. [Error Messages & Incomplete Implementations](#error-messages--incomplete-implementations)
6. [Configuration Files](#configuration-files)
7. [Dependencies from pom.xml](#dependencies-from-pomxml)
8. [Service Endpoints](#service-endpoints)

---

## Complete File Structure

```
assurance-service/
├── pom.xml                          # Maven configuration
├── src/main/
│   ├── java/com/neuroguard/assuranceservice/
│   │   ├── AssuranceServiceApplication.java    # Main Spring Boot app
│   │   ├── controller/
│   │   │   ├── AssuranceController.java              # Core assurance operations
│   │   │   ├── AssuranceReportController.java        # PDF reporting
│   │   │   └── SimulationController.java             # Insurance simulation
│   │   ├── service/
│   │   │   ├── AssuranceService.java                 # Interface
│   │   │   ├── AssuranceServiceImpl.java              # CRUD implementation
│   │   │   ├── CoverageRiskAssessmentService.java    # Risk calculation (90% of logic)
│   │   │   ├── StatisticsService.java                # Analytics & statistics
│   │   │   ├── SimulationService.java                # Procedure simulation
│   │   │   ├── PDFGenerationService.java             # PDF report creation
│   │   │   ├── NotificationService.java              # Interface
│   │   │   ├── NotificationServiceImpl.java           # Notification dispatcher
│   │   │   ├── EmailNotificationService.java         # Email sending
│   │   │   ├── SmsNotificationService.java           # SMS sending (Twilio)
│   │   │   └── UserServiceClient.java                # Feign client
│   │   ├── client/
│   │   │   ├── MLPredictorClient.java                # Python ML service
│   │   │   ├── MedicalHistoryClient.java             # Medical records
│   │   │   ├── ConsultationClient.java               # Consultation history
│   │   │   └── RiskAlertClient.java                  # Active alerts
│   │   ├── entity/
│   │   │   ├── Assurance.java                        # Insurance policy record
│   │   │   ├── CoverageRiskAssessment.java           # Risk assessment result
│   │   │   ├── Notification.java                     # Notification history
│   │   │   └── AssuranceStatus.java                  # Status enum (PENDING/APPROVED/REJECTED)
│   │   ├── repository/
│   │   │   ├── AssuranceRepository.java              # JPA repository
│   │   │   ├── CoverageRiskAssessmentRepository.java # JPA repository
│   │   │   └── NotificationRepository.java           # JPA repository
│   │   ├── dto/
│   │   │   ├── AssuranceRequestDto.java              # POST request
│   │   │   ├── AssuranceResponseDto.java             # API response
│   │   │   ├── CoverageRiskAssessmentDto.java        # Risk assessment DTO
│   │   │   ├── StatisticsDto.java                    # Statistics response
│   │   │   ├── ProcedureSimulationDto.java           # Procedure cost simulation
│   │   │   ├── SimulationResponseDto.java            # Rentability analysis response
│   │   │   ├── NotificationRequest.java              # Notification request
│   │   │   ├── AlertDto.java                         # Alert object
│   │   │   ├── MLPredictionDto.java                  # ML service response
│   │   │   └── UserDto.java                          # User object
│   │   ├── config/
│   │   │   ├── FeignClientConfig.java                # Feign/OpenFeign setup
│   │   │   ├── SecurityConfig.java                   # JWT security
│   │   │   ├── JwtAuthenticationFilter.java          # JWT token validation
│   │   │   ├── JwtUtils.java                         # JWT utilities
│   │   │   ├── GlobalExceptionHandler.java           # Exception handling
│   │   │   ├── MailConfig.java                       # Email configuration
│   │   │   ├── SmsConfig.java                        # SMS configuration
│   │   │   ├── EnvConfig.java                        # Environment variables
│   │   │   └── ErrorResponse.java                    # Error response wrapper
│   │   ├── scheduler/
│   │   │   └── RiskAssessmentScheduler.java          # Scheduled tasks
│   │   └── util/
│   │       └── CoverageRiskAssessmentMapper.java     # Entity to DTO mapping
│   └── resources/
│       ├── application.yml                           # Main configuration
│       └── templates/
│           └── (email templates for Thymeleaf)
└── target/                                            # Compiled classes
```

---

## Main Java Classes & Purposes

### Core Application
- **AssuranceServiceApplication.java** - Spring Boot entry point with Eureka discovery, Feign clients, async processing, and scheduling enabled

### Controllers (3 REST Controllers)
1. **AssuranceController.java** - Main REST endpoints for insurance management
   - CRUD operations on assurance records
   - Risk assessment generation and retrieval
   - Advanced statistics endpoints
   - **⚠️ CONTAINS TEST ENDPOINT** (marked for removal)

2. **AssuranceReportController.java** - PDF report generation
   - Single assurance PDF download
   - Bulk export multiple assurances as PDF

3. **SimulationController.java** - Insurance analysis
   - Procedure cost simulation with coverage calculation
   - Rentability analysis ("optimizer")

### Services (11 Service Classes)
1. **AssuranceServiceImpl** - CRUD operations for assurance records
   - Async notification sending after key events
   - Patient verification via UserServiceClient
   - Graceful fallback when user-service is unavailable

2. **CoverageRiskAssessmentService** - **CORE LOGIC** (90% of service complexity)
   - Multi-step risk calculation (11 steps)
   - Medical complexity scoring (0-100) with 6 weighted components
   - Generates test medical data instead of using real records (⚠️ **CRITICAL ISSUE**)
   - Determines coverage level (BASIC/ENHANCED/COMPREHENSIVE/INTENSIVE)
   - Estimates annual claim cost with seed-based variation (+50% variation)
   - Schedules reassessment dates based on risk levels
   - Integrates with ML predictor, medical history, and alert services

3. **StatisticsService** - Advanced analytics
   - Patient-level statistics (7 dimensions of data)
   - Assurance-level statistics (8 dimensions)
   - Comprehensive risk and cost metrics
   - Procedure frequency analysis
   - Care team composition analysis

4. **SimulationService** - Insurance plan analysis
   - Procedure reimbursement simulation with coverage percentages
   - Rentability analysis (benefit-to-cost ratio)
   - Plan optimization recommendations
   - Potential annual savings calculation

5. **NotificationServiceImpl** - Notification dispatcher
   - Routes notifications to EMAIL and/or SMS channels
   - Builds customized SMS messages from templates
   - Fallback handling when services unavailable

6. **EmailNotificationService** - Email sending
   - Uses JavaMailSender with Thymeleaf template rendering
   - Saves notification history to database
   - Error logging and failure tracking

7. **SmsNotificationService** - SMS sending
   - Twilio SDK integration
   - Phone number validation
   - SMS history tracking

8. **PDFGenerationService** - PDF report creation
   - Uses iTextPDF library
   - Simple HTML-to-PDF conversion
   - Professional report formatting
   - Bulk export support

9. **UserServiceClient** - Feign client
   - Calls user-service to fetch patient/user details
   - Used for patient name, email, phone validation

10. **CoverageRiskAssessmentMapper** - Entity mapping utility

11. **RiskAssessmentScheduler** - Two scheduled tasks
    - Daily recalculation of due assessments (cron: 0 0 2 * * *)
    - 6-hourly monitoring of high-risk cases (cron: 0 0 */6 * * *)

### Repositories (3 JPA Repositories)
- **AssuranceRepository** - CRUD + `findByPatientId()`
- **CoverageRiskAssessmentRepository** - CRUD + `findByAssuranceId()` + `findByPatientId()` + `deleteByAssuranceId()`
- **NotificationRepository** - CRUD + `findByPatientIdOrderByCreatedAtDesc()` + `findByType()` + `findByStatus()`

---

## Issues, Bugs & Code Quality Problems

### 🔴 CRITICAL ISSUES

#### 1. **Test Data Override - Medical History Completely Ignored**
- **Location:** `CoverageRiskAssessmentService.java` lines 46, 560
- **Severity:** CRITICAL - Makes all risk assessments unreliable
- **Problem:** 
  ```java
  MedicalHistoryDto medicalHistory = fetchMedicalHistory(patientId);
  // OVERRIDE with generated test data based on assurance ID
  medicalHistory = generateTestMedicalData(assuranceSeed);
  ```
- **Impact:** Real patient medical data is discarded; synthetic profiles replace actual clinical information
- **Root Cause:** Uses assurance ID modulo 100 as seed to generate 5 different profiles (0=excellent health, 1=mild cognitive impairment, 2=moderate dementia, 3=severe dementia, 4=end-stage)
- **Fix Required:** Remove the override line OR make it conditional (e.g., only if medical history is null)

#### 2. **Temporary Test Endpoint Not Removed**
- **Location:** `AssuranceController.java` lines 29-44
- **Severity:** HIGH - Security & API cleanliness issue
- **Problem:** Endpoint `/api/assurances/test/{assuranceId}` left in code with "TEMPORARY TEST ENDPOINT - Remove after testing" comment
- **Fix:** Delete the entire `testRiskAssessmentData()` method

#### 3. **System.out.println Debug Statements Everywhere**
- **Locations:** `AssuranceController.java` (6 instances), `PDFGenerationService.java`, other classes
- **Severity:** MEDIUM - Logging best practices, production code cleanliness
- **Problem:**
  ```java
  System.out.println("DEBUG: generateCoverageAssessment called - assuranceId: ..." + assuranceId);
  ```
- **Should be:**
  ```java
  log.debug("generateCoverageAssessment called - assuranceId: {}", assuranceId);
  ```
- **Instances:** At least 8 System.out.println calls mixed with proper log statements

#### 4. **Empty Configuration Values**
- **Location:** `application.yml` lines 10, 51
- **Severity:** HIGH - Runtime failure
- **Issues:**
  ```yaml
  datasource:
    password:           # ❌ EMPTY - will fail to connect
  
  spring.mail:
    password: ${MAIL_PASSWORD:}  # ❌ EMPTY - email won't send
  
  datasource:
    username: root      # ⚠️ Hardcoded weak credentials
  ```
- **Impact:** 
  - Database connections will fail
  - Email notifications won't work
  - No fallback values provided

#### 5. **Null Pointer Risk in FeignClientConfig**
- **Location:** `FeignClientConfig.java` line 18-20
- **Severity:** MEDIUM
- **Problem:**
  ```java
  ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
  if (attributes != null && attributes.getRequest() != null) {  // ✓ Has check
  ```
- **Issue:** Code is actually safe here, but could fail if called outside request context

### 🟡 HIGH PRIORITY ISSUES

#### 6. **Test Data Seed Predictability**
- **Location:** `CoverageRiskAssessmentService.java` line 37-38
- **Problem:** Using `assuranceId % 100` means only 100 unique "seeds"
- **Impact:** Same ID modulo 100 → same fake data
- **Example:** Assurance ID 1001 and 101 both get seed 1 → identical fake profiles

#### 7. **Hardcoded Cost Multiplier Variation (50%)**
- **Location:** `CoverageRiskAssessmentService.java` line 308
- **Problem:**
  ```java
  double seedMultiplier = 0.7 + (assuranceSeed % 50) * 0.01;  // 0.7 to 1.2 (50% variation!)
  ```
- **Impact:** Same patient with different assurance IDs gets 50% different cost estimates
- **Business Logic Issue:** Cost varies dramatically based on assurance ID, not patient risk

#### 8. **No Null Checks on DTO Fields**
- **Location:** Multiple places in services
- **Example:** `AssuranceServiceImpl.java` line 45
  ```java
  UserDto patient = userServiceClient.getUserById(request.getPatientId());
  if (patient == null) {
      throw new RuntimeException("Patient not found with ID: " + request.getPatientId());
  }
  ```
- **Better approach:** Custom exception, proper error response

#### 9. **Graceful Degradation Issues**
- **Location:** `AssuranceServiceImpl.java` getAllAssurances() and similar methods
- **Problem:** When user-service is down, returns partial data without clear indication
- **Better approach:** Add status flag or error to response

#### 10. **Exception Handling Returns Empty DTOs**
- **Location:** `AssuranceController.java` line 101
- **Problem:**
  ```java
  catch (Exception e) {
      System.out.println("DEBUG: Exception in generateCoverageAssessment: " + e.getMessage());
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(new CoverageRiskAssessmentDto());  // ❌ Empty DTO
  }
  ```
- **Issue:** Client gets empty object instead of error details
- **Fix:** Use GlobalExceptionHandler or return ErrorResponse

#### 11. **Missing Validation on Risk Assessment Recalculation**
- **Location:** `RiskAssessmentScheduler.java` line 47
- **Problem:** Recalculates even for REJECTED assurances
- **Should:** Skip rejected/inactive assurances

#### 12. **Email Template Names Don't Match**
- **Location:** `EmailNotificationService.java` line 78
- **Problem:**
  ```java
  String templateName = "email-" + type.toLowerCase().replace("_", "-");
  // Results in: "email-assurance-created", "email-assurance-approved", etc.
  ```
- **Issue:** No template files found in `resources/templates/`
- **Fallback:** Uses plaintext instead, but logs warning

### 🟢 MEDIUM PRIORITY ISSUES

#### 13. **Missing CoverageRiskAssessment in Assurance Entity**
- **Location:** `Assurance.java` lines 40-45
- **Issue:** Relationship defined but rarely populated
- **Should:** Ensure OneToOne is properly cascaded

#### 14. **Statistics Service Missing Null Checks**
- **Location:** `StatisticsService.java` lines 40-45
- **Problem:** Streams filter but don't handle null streams
- **Fix:** Safe filtering approach

#### 15. **PDF Generation Fallback to HTML Text**
- **Location:** `PDFGenerationService.java` line 76
- **Issue:** When HTML conversion fails, just adds HTML as text
- **Better:** Use ByteArrayOutputStream with iText properly

#### 16. **Hardcoded Email Address**
- **Location:** `EmailNotificationService.java` line 51
- **Problem:** `helper.setFrom("noreply@neuroguard.com")`
- **Should:** Read from configuration

#### 17. **SMS Message Truncation Not Handled**
- **Location:** `SmsNotificationService.java` (implied)
- **Issue:** Thymeleaf template exceeds 160 chars (SMS limit)
- **Should:** Implement message splitting/truncation

#### 18. **No Request Validation on DTOs**
- **Location:** Controller methods
- **Issue:** `@Valid` decorator used but validation messages vague
- **Should:** Implement custom validators

---

## The "Optimizer" Functionality

### What It's Actually For
The "optimizer" is **NOT a true optimization algorithm**. It's a **rentability analysis tool** that:
1. Calculates historical insurance cost-benefit ratio
2. Recommends plan changes if usage patterns change
3. Estimates potential annual savings

### Location & Implementation
- **File:** `SimulationService.java` method `getRentabilityAnalysis()`
- **Endpoint:** `GET /api/simulations/rentability/{patientId}`

### Algorithm Steps
```
1. Get patient's current insurance plan (assurance record)
2. Fetch consultation history from consultation-service
3. Assume 12 months active + $120/month premium
4. Calculate total benefits: history.size() * $60 * 0.7 = ~$42 per consult saved
5. Calculate ratio: totalBenefits / totalPremiums
6. Ratio interpretation:
   - < 0.4: "Usage is LOW" → recommend BASIC plan ($60/month)
   - 0.4-1.2: "Usage is NORMAL" → keep CURRENT plan
   - > 1.2: "Usage is HIGH" → recommend INTENSIVE plan
7. Calculate potential savings: (current_premium - recommended_premium) * 12
```

### Issues with This "Optimizer"
1. **Hardcoded Numbers:** $60/month basic, $120/month current, $42 per consultation saved
2. **Oversimplified:** Doesn't account for actual claim costs, only consultation count
3. **No Rate Adjustments:** Ignores risk profile changes that affect premiums
4. **No Persistence:** Results not saved; just calculated on each request
5. **Manual Recommendations Only:** No automatic plan switching

### Procedure Simulation Feature
- **Method:** `simulateProcedure()` in `SimulationService.java`
- **Purpose:** Show patient how much insurance will cover for specific procedures
- **Hardcoded costs:**
  ```
  CONSULTATION_GENERAL=60
  CONSULTATION_SPECIALIST=120
  MRI_SCAN=450
  BLOOD_TEST=85
  COGNITIVE_THERAPY=150
  HOME_CARE_SESSION=75
  ```
- **Coverage levels:** BASIC (70%), ENHANCED (75%), COMPREHENSIVE (85%), INTENSIVE (100%)
- **Issue:** Doesn't use actual plan from assurance record; assumes percentages

---

## Error Messages & Incomplete Implementations

### Incomplete Features

#### 1. **Email Templates Missing**
- **Issue:** `resources/templates/` folder exists but is empty
- **Code attempts to use:**
  - `email-assurance-created`
  - `email-assurance-approved`
  - `email-assurance-rejected`
- **Fallback:** Uses plaintext that's hardcoded in service
- **Status:** ⚠️ INCOMPLETE - Never throws error, just silently falls back

#### 2. **SMS Configuration Missing Validation**
- **Location:** `SmsNotificationService.java`
- **Issue:** Relies on Twilio SDK but no actual implementation shown
- **Status:** ⚠️ INCOMPLETE - Not fully visible in provided files

#### 3. **PDF Template Engine**
- **Location:** `PDFGenerationService.java` line 28
- **Code:**
  ```java
  @Autowired(required = false)
  private TemplateEngine templateEngine;
  ```
- **Issue:** Optional Thymeleaf dependency; manual HTML generation used as fallback
- **Status:** ⚠️ PARTIALLY IMPLEMENTED - Works but not using template engine

### Error Handling Patterns

#### Missing Custom Exceptions
```java
// ❌ Current: Uses RuntimeException everywhere
throw new RuntimeException("Patient not found with ID: " + request.getPatientId());

// ✓ Should be:
throw new PatientNotFoundException("Patient not found with ID: " + request.getPatientId());
```

#### GlobalExceptionHandler (Only 3 Handlers)
```java
1. MethodArgumentNotValidException → 400 BAD_REQUEST
2. RuntimeException → 500 INTERNAL_SERVER_ERROR
3. Exception → 500 INTERNAL_SERVER_ERROR
```
Missing:
- `FeignException` (when downstream services fail)
- `DataIntegrityViolationException` (duplicate/constraint violations)
- Custom exceptions

### Known Incomplete Implementations

#### 1. **Geriatric Assessment Need**
- **Code in CoverageRiskAssessmentService.java:**
  ```java
  private Boolean determineGeriatricAssessmentNeed(MedicalHistoryDto medicalHistory, Integer complexityScore) {
      // This would need age data from user service in real implementation
      return complexityScore > 40;
  }
  ```
- **Issue:** No age validation; just uses complexity score
- **Status:** ⚠️ INCOMPLETE

#### 2. **Family History Analysis**
- **Code:**
  ```java
  if (medicalHistory.getFamilyHistory() != null && 
      medicalHistory.getFamilyHistory().toLowerCase().contains("dementia")) {
      procedures.add("Genetic counseling");
  }
  ```
- **Issue:** String matching on free text field; fragile
- **Status:** ⚠️ FRAGILE

#### 3. **Medication Allergies**
- **Code:** Checks for empty list but never populated from source
- **Status:** ⚠️ INCOMPLETE - Field exists but no data source

---

## Configuration Files

### application.yml (Application Configuration)
**Path:** `src/main/resources/application.yml`

```yaml
server:
  port: 8086

spring:
  application:
    name: assurance-service
  datasource:
    url: jdbc:mysql://localhost:3306/neuroguard_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
    username: root
    password:              # ❌ EMPTY - Default
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

jwt:
  secret: XDkzF2YNPA/7vXmPYJmaACjY6VBhwHJbr4pzPF5jguE=
  expiration: 86400000

# ML Predictor Service Configuration
ml-predictor:
  service:
    url: http://localhost:5000

logging:
  level:
    org.springframework.web: INFO
    org.springframework.security: DEBUG
    org.springframework.security.web.FilterChainProxy: DEBUG
    com.neuroguard.assuranceservice.config.JwtAuthenticationFilter: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

# Email Configuration
spring.mail:
  host: ${MAIL_HOST:smtp.gmail.com}
  port: ${MAIL_PORT:587}
  username: ${MAIL_USERNAME:}            # ❌ EMPTY
  password: ${MAIL_PASSWORD:}            # ❌ EMPTY
  properties.mail.smtp.auth: true
  properties.mail.smtp.starttls.enable: true
```

**Issues:**
- Database password empty (relies on Docker/env vars)
- Mail credentials empty (email notification will fail)
- Hardcoded localhost:3306 (not production-ready)
- Show-sql: true (performance impact)
- Security: DEBUG logging configured for multiple Spring Security classes

### JwtUtils Configuration
- **JWT Secret:** `XDkzF2YNPA/7vXmPYJmaACjY6VBhwHJbr4pzPF5jguE=` (should be in env vars)
- **Expiration:** 86400000ms = 24 hours

### Database
- **Type:** MySQL 8.0+
- **DDL Mode:** `update` (auto-creates/updates schema)
- **Dialect:** MySQLDialect
- **URL:** `jdbc:mysql://localhost:3306/neuroguard_db`

### Eureka Discovery
- **Registration:** Enabled
- **Service URL:** `http://localhost:8761/eureka/`
- **Prefer IP:** true

---

## Dependencies from pom.xml

### Spring Boot Framework (Parent)
- **Version:** 3.2.4
- **Java Version:** 17
- **Spring Cloud Version:** 2023.0.1

### Core Dependencies

| Artifact | Version | Purpose |
|----------|---------|---------|
| spring-boot-starter-actuator | 3.2.4 | Health checks, metrics |
| spring-boot-starter-data-jpa | 3.2.4 | Database ORM |
| spring-boot-starter-security | 3.2.4 | Authentication/authorization |
| spring-boot-starter-web | 3.2.4 | REST API support |
| spring-boot-starter-validation | 3.2.4 | Bean validation |

### Cloud & Microservices
| Artifact | Version | Purpose |
|----------|---------|---------|
| spring-cloud-starter-netflix-eureka-client | 2023.0.1 | Service discovery |
| spring-cloud-starter-openfeign | 2023.0.1 | HTTP client for services |

### Authentication
| Artifact | Version | Purpose |
|----------|---------|---------|
| java-jwt | 4.4.0 | JWT token creation/validation |
| spring-security-test | - | Testing security |

### Database
| Artifact | Version | Purpose |
|----------|---------|---------|
| mysql-connector-j | - | MySQL driver |

### Utilities
| Artifact | Version | Purpose |
|----------|---------|---------|
| lombok | 1.18.36 | Code generation (@Data, @Slf4j) |
| spring-boot-devtools | - | Live reload |

### Communication
| Artifact | Version | Purpose |
|----------|---------|---------|
| spring-boot-starter-mail | 3.2.4 | Send emails via JavaMailSender |
| spring-boot-starter-thymeleaf | 3.2.4 | Email template rendering |
| twilio | 9.2.0 | SMS sending |

### Document Generation
| Artifact | Version | Purpose |
|----------|---------|---------|
| itextpdf | 5.5.13.3 | PDF generation |

### Configuration
| Artifact | Version | Purpose |
|----------|---------|---------|
| dotenv-java | 3.0.0 | Load .env files |

### Testing
| Artifact | Version | Purpose |
|----------|---------|---------|
| spring-boot-starter-test | - | JUnit, Mockito, etc. |

---

## Service Endpoints

### Assurance Management (`/api/assurances/*`)

#### Create Insurance Policy
```
POST /api/assurances
Content-Type: application/json

{
  "patientId": 1,
  "providerName": "BlueCross",
  "policyNumber": "BC123456",
  "coverageDetails": "ENHANCED|75%",
  "illness": "Type 2 Diabetes",
  "postalCode": "75001",
  "mobilePhone": "+1234567890"
}

Response: 201 Created
{
  "id": 1,
  "patientId": 1,
  "patientDetails": { /* UserDto */ },
  "providerName": "BlueCross",
  "policyNumber": "BC123456",
  "coverageDetails": "ENHANCED|75%",
  "illness": "Type 2 Diabetes",
  "postalCode": "75001",
  "mobilePhone": "+1234567890",
  "status": "PENDING",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

#### Get Assurance by ID
```
GET /api/assurances/{id}

Response: 200 OK
{
  "id": 1,
  "patientId": 1,
  /* ... full assurance details ... */
}
```

#### Get All Assurances for Patient
```
GET /api/assurances/patient/{patientId}

Response: 200 OK
[
  { /* Assurance 1 */ },
  { /* Assurance 2 */ }
]
```

#### Get All Assurances (System-Wide)
```
GET /api/assurances

Response: 200 OK
[
  { /* Assurance 1 */ },
  { /* Assurance 2 */ },
  ...
]
```

#### Update Assurance
```
PUT /api/assurances/{id}
Content-Type: application/json

{
  "patientId": 1,
  "providerName": "BlueCross Updated",
  ...
}

Response: 200 OK
{ /* updated assurance */ }
```

#### Update Assurance Status
```
PUT /api/assurances/{id}/status?status=APPROVED
(or PENDING, REJECTED)

Response: 200 OK
{ /* assurance with new status */ }
```

#### Delete Assurance
```
DELETE /api/assurances/{id}

Response: 204 No Content
```

#### ⚠️ TEST ENDPOINT (FOR REMOVAL)
```
GET /api/assurances/test/{assuranceId}

Response: 200 OK
"Assurance 1 -> Medical Complexity: 42, Alzheimer's Risk: 65.00%, Est. Cost: $18500"
```

---

### Risk Assessment (`/api/assurances/{assuranceId}/risk-assessment`)

#### Generate Coverage Risk Assessment
```
POST /api/assurances/{assuranceId}/risk-assessment?patientId={patientId}

Response: 200 OK
{
  "id": 1,
  "assuranceId": 1,
  "patientId": 1,
  "alzheimersPredictionScore": 0.65,
  "alzheimersPredictionLevel": "HIGH",
  "activeAlertCount": 3,
  "highestAlertSeverity": "WARNING",
  "alertSeverityRatio": 0.67,
  "medicalComplexityScore": 72,
  "recommendedCoverageLevel": "COMPREHENSIVE",
  "estimatedAnnualClaimCost": 18500.0,
  "recommendedProcedures": [
    "Neuro-psychological assessment",
    "Advanced neuroimaging (MRI/PET)",
    "Occupational therapy assessment"
  ],
  "recommendedProviderCount": 3,
  "neurologyReferralNeeded": true,
  "geriatricAssessmentNeeded": true,
  "lastAssessmentDate": "2024-01-15T10:30:00",
  "nextRecommendedAssessmentDate": "2024-04-15T10:30:00",
  "riskStratum": "HIGH",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

#### Get Existing Risk Assessment
```
GET /api/assurances/{assuranceId}/risk-assessment

Response: 200 OK
{ /* CoverageRiskAssessmentDto */ }

Response: 404 Not Found (if no assessment exists)
```

#### Refresh/Recalculate Risk Assessment
```
PUT /api/assurances/{assuranceId}/risk-assessment/refresh?patientId={patientId}

Response: 200 OK
{ /* Updated CoverageRiskAssessmentDto */ }
```

---

### Reports (`/api/assurances/reports/*`)

#### Download PDF Report for Single Assurance
```
GET /api/assurances/{id}/report/pdf

Response: 200 OK (application/pdf)
[Binary PDF content]

Headers:
Content-Disposition: attachment; filename="assurance_1.pdf"
```

#### Bulk Export Multiple Assurances as PDF
```
POST /api/assurances/reports/bulk-export
Content-Type: application/json

[1, 2, 3, 4, 5]  // Array of assurance IDs

Response: 200 OK (application/pdf)
[Binary PDF content - combined document]

Headers:
Content-Disposition: attachment; filename="assurances_export_2024-01-15_103000.pdf"
```

---

### Statistics (`/api/assurances/stats/*`)

#### Get Patient-Level Statistics
```
GET /api/assurances/stats/patient/{patientId}

Response: 200 OK
{
  "patientId": 1,
  "totalAssurances": 2,
  "averageAlzheimersRisk": 0.58,
  "highestAlzheimersRisk": 0.72,
  "lowestAlzheimersRisk": 0.45,
  "standardDeviationRisk": 0.135,
  "totalEstimatedCost": 42000.0,
  "averageAnnualCost": 21000.0,
  "medianAnnualCost": 21000.0,
  "totalActiveAlerts": 5,
  "averageAlertsPerAssurance": 2.5,
  "highestSeverityAlerts": ["CRITICAL", "WARNING"],
  "averageComplexityScore": 65,
  "maxComplexityScore": 78,
  "recommendedProceduresFrequency": {
    "Neuro-psychological assessment": 2,
    "Advanced neuroimaging (MRI/PET)": 1,
    "Occupational therapy assessment": 1
  },
  "careTeamAverageSize": 3,
  "patientsNeedingNeurology": 1,
  "patientsNeedingGeriatrics": 2,
  "overallRiskLevel": "MODERATE"
}
```

#### Get Assurance-Level Statistics
```
GET /api/assurances/stats/assurance/{assuranceId}

Response: 200 OK
{
  "assuranceId": 1,
  "averageRiskScore": 0.65,
  "patientsHighRisk": 0,
  "patientsMediumRisk": 1,
  "patientsLowRisk": 0,
  "riskDistribution": 0.0,
  "totalProjectedCost": 18500.0,
  "averageClaimCost": 18500.0,
  "costVariance": 0.0,
  "costStandardDeviation": 0.0,
  "patientsWithHighAlzRisk": 0,
  "averageAlzheimersPrevalence": 0.0,
  "topProcedures": [
    {
      "procedureName": "Neuro-psychological assessment",
      "frequency": 1,
      "percentage": 33.33
    }
  ],
  "refferalNeeds": {
    "neurology": 1,
    "geriatrics": 1,
    "cardiology": 0
  }
}
```

---

### Simulations (`/api/simulations/*`)

#### Simulate Procedure Reimbursement
```
GET /api/simulations/procedure?patientId={patientId}&procedureName={procedureName}

Example:
GET /api/simulations/procedure?patientId=1&procedureName=MRI_SCAN

Response: 200 OK
{
  "procedureName": "MRI_SCAN",
  "totalBaseCost": 450.0,
  "insuranceCoverage": 0.75,
  "insuranceReimbursement": 337.5,
  "patientRemainder": 112.5,
  "coverageLevel": "ENHANCED|75%"
}
```

**Available Procedures:**
- CONSULTATION_GENERAL ($60)
- CONSULTATION_SPECIALIST ($120)
- MRI_SCAN ($450)
- BLOOD_TEST ($85)
- COGNITIVE_THERAPY ($150)
- HOME_CARE_SESSION ($75)

#### Get Rentability Analysis ("Optimizer")
```
GET /api/simulations/rentability/{patientId}

Response: 200 OK
{
  "patientId": 1,
  "currentPlan": "ENHANCED|75%",
  "monthlyPremium": 120.0,
  "totalPremiumsPaid": 1440.0,
  "totalBenefitsReceived": 840.0,
  "benefitCostRatio": 0.583,
  "rentabilityStatus": "NEUTRAL",
  "optimizationAdvice": [
    "Your healthcare consumption is balanced for your current plan."
  ],
  "recommendedPlan": "CURRENT",
  "potentialAnnualSavings": 0.0,
  "consultationCount": 14,
  "estimatedTotalCost": 840.0
}
```

**Status Interpretations:**
- `PROFITABLE` (ratio > 0.8): Patient is getting good value from insurance
- `NEUTRAL` (0.4 < ratio < 0.8): Balanced usage
- `LOSS` (ratio < 0.4): Patient not using insurance enough

---

## Summary Table

| Category | Count | Details |
|----------|-------|---------|
| **Classes** | 35+ | Controllers (3), Services (11), Repositories (3), DTOs (12), Entities (4), Configs (9), Clients (4) |
| **Endpoints** | 18 | Assurance CRUD (7), Risk Assessment (3), Reports (2), Statistics (2), Simulations (2), Test (1) |
| **Scheduled Tasks** | 2 | Risk assessment recalc (daily 2 AM), High-risk monitoring (every 6 hours) |
| **Dependencies** | 22+ | Spring Boot 3.2.4, Spring Cloud 2023.0.1, MySQL, Feign, JWT, iTextPDF, Twilio, Thymeleaf |
| **Critical Issues** | 5 | Test data override, test endpoint, debug statements, empty config, null checks |
| **High Priority Issues** | 7 | Predictable seeds, hardcoded costs, weak error handling, missing templates |
| **Medium Priority Issues** | 8 | Missing validation, fragile string matching, hardcoded values |

---

## Recommendations

### Immediate Actions (This Week)
1. ✅ Remove test endpoint `/api/assurances/test/{assuranceId}`
2. ✅ Replace all System.out.println with log.debug/log.info/log.error
3. ✅ REMOVE or CONDITION the test data override in CoverageRiskAssessmentService
4. ✅ Set environment variables for database and mail passwords
5. ✅ Create email templates in resources/templates/

### High Priority (Next Sprint)
1. Refactor cost calculation to not use assurance seed
2. Implement proper custom exceptions (PatientNotFoundException, etc.)
3. Add email template files for Thymeleaf
4. Fix exception handling to return ErrorResponse DTOs
5. Validate procedure simulation costs against real data

### Medium Priority (Future)
1. Implement true optimization algorithm if needed
2. Add age to risk calculation for geriatric assessment
3. Implement message truncation for SMS
4. Add database migration scripts (Flyway/Liquibase)
5. Implement caching for frequently accessed data

---

**End of Analysis Report**
