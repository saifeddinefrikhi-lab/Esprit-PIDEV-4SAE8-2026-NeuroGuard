NeuroGuard – Intelligent Alzheimer Patient Monitoring System
https://img.shields.io/badge/Spring%2520Boot-3.4.3-brightgreen
https://img.shields.io/badge/Angular-17-red
https://img.shields.io/badge/MySQL-8.0-blue
https://img.shields.io/badge/Eureka-Discovery-blueviolet
https://img.shields.io/badge/License-MIT-yellow

📋 Overview
NeuroGuard is a comprehensive microservices‑based platform designed to improve the safety and quality of life of Alzheimer's patients. The system enables continuous monitoring, risk detection through business rules and machine learning, real‑time alert generation, and structured collaboration among patients, caregivers, and healthcare providers.

By leveraging a modern cloud‑native architecture, NeuroGuard provides a scalable, secure, and intelligent solution for Alzheimer's care management.

✨ Features
User Management – Role‑based access (Admin, Patient, Caregiver, Provider) with JWT authentication.

Medical History Tracking – Store and manage detailed medical records including diagnoses, allergies, surgeries, and assigned caregivers/providers.

Rule‑Based Alerts – Automatic alert generation based on configurable medical rules (e.g., severe progression, allergy detection, fall risk).

ML‑Powered Risk Prediction – Integration with a Python ML service to predict hospitalisation risk using patient features.

Real‑Time Notifications – In‑app notifications for patients and caregivers when alerts are created.

API Gateway – Centralized entry point with dynamic routing, CORS configuration, and load balancing.

Service Discovery – Eureka server for seamless microservice registration and discovery.

File Upload/Download – Secure storage of medical documents (scans, reports) associated with patient histories.

🛠️ Tech Stack
Frontend
Framework: Angular 17

Styling: SCSS, Tabler Icons

HTTP Client: Angular HttpClient with interceptors for JWT

Real‑time Updates: (Optional) WebSocket / STOMP

Backend
Language: Java 17

Framework: Spring Boot 3.4.3

Security: Spring Security, JWT (Auth0)

Persistence: Spring Data JPA, Hibernate, MySQL

Service Discovery: Netflix Eureka

API Gateway: Spring Cloud Gateway

Feign Clients: Declarative REST clients with interceptor for token propagation

ML Integration: Python Flask microservice (scikit‑learn)

Build Tool: Maven

🏗️ Architecture
The system follows a microservices architecture with the following components:

text
┌─────────────────────────────────────────────────────────────────┐
│                         Angular Frontend                         │
│                         (http://localhost:4200)                   │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Gateway Service (8083)                       │
│              (Routing, CORS, Load Balancing)                      │
└───────┬──────────────────┬──────────────────┬──────────────────┬─┘
        │                  │                  │                  │
        ▼                  ▼                  ▼                  ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│  User Service │  │Medical History│  │Risk Alert     │  │   ML Predictor│
│    (8081)     │  │  Service (8082)│  │Service (8084) │  │   (5000)      │
│ - Auth        │  │ - CRUD history│  │ - Rule alerts │  │ - /predict    │
│ - User CRUD   │  │ - File upload │  │ - ML alerts   │  │ - /health     │
│ - JWT tokens  │  │ - Caregiver   │  │ - Notifications│  │               │
└───────────────┘  └───────────────┘  └───────────────┘  └───────────────┘
        │                  │                  │                  │
        └──────────────────┴──────────────────┴──────────────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │   Eureka Server (8761) │
                    │   Service Registry     │
                    └───────────────────────┘
Eureka Server – All microservices register themselves, enabling dynamic discovery.

Gateway – Routes requests to appropriate services, adds CORS headers, and handles cross‑cutting concerns.

User Service – Manages users, roles, authentication, and JWT issuance.

Medical History Service – Core domain for patient medical data and file storage.

Risk Alert Service – Evaluates patient data against rules and ML predictions, generates alerts and notifications.

ML Predictor Service – Python‑based service that returns hospitalisation risk probabilities.

👥 Contributors
Saif Eddine Ben Hadj Youssef – Lead Developer

Your team members can be added here

🎓 Academic Context
This project is developed as part of the PI (Projet Intégrateur) at ESPERT (École Supérieure Privée d'Ingénierie et de Technologies).

Contexte
La prise en charge des patients atteints de la maladie d’Alzheimer nécessite une surveillance continue afin de prévenir les situations à risque telles que les chutes, les fugues ou les comportements anormaux. Cependant, les solutions existantes sont souvent peu automatisées et insuffisamment personnalisées. Une application intelligente permettrait d’analyser en continu le comportement des patients et d’améliorer leur sécurité.

Objectif(s)
Développement d’une application web intelligente pour la gestion des patients Alzheimer, la détection automatique des situations à risque par règles métier, la génération d’alertes et le suivi des patients, avec des rôles clairs pour l’administrateur, l’aidant, le soignant et le patient.

🚀 Getting Started
Prerequisites
Java 17+

Node.js 18+ & npm

MySQL 8.0

Python 3.10+ (for ML service)

Maven

Backend Setup
Clone the repository

bash
git clone https://github.com/your-org/neuroguard-backend.git
cd neuroguard-backend
Create MySQL databases

sql
CREATE DATABASE userdb;
CREATE DATABASE medical_history_db;
CREATE DATABASE risk_alert_db;
Update configuration
Edit application.yaml in each microservice to match your MySQL credentials and JWT secret.

Build and run Eureka Server

bash
cd eureka-server
mvn clean package
java -jar target/eureka-server-0.0.1-SNAPSHOT.jar
Run the other microservices in separate terminals (order: user-service, medical-history-service, risk-alert-service, gateway-service)

bash
cd user-service && mvn spring-boot:run
cd medical-history-service && mvn spring-boot:run
cd risk-alert-service && mvn spring-boot:run
cd gateway-service && mvn spring-boot:run
Start the ML predictor

bash
cd ml-predictor-service
pip install -r requirements.txt
python app.py
Frontend Setup
bash
cd FrontEnd
npm install
ng serve
The application will be available at http://localhost:4200.

📚 Additional Resources
Spring Cloud Gateway Documentation

Eureka Service Discovery

Angular Documentation

JWT with Spring Security

🙏 Acknowledgments
Thanks to our academic supervisors at ESPERT for their guidance.

Special thanks to the open‑source community for the amazing tools that made this project possible.

📄 License
This project is licensed under the MIT License – see the LICENSE file for details.

