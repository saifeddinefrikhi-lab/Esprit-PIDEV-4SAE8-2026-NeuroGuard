# 🧠 NeuroGuard - Intelligent Alzheimer's Risk Detection System

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Angular](https://img.shields.io/badge/Angular-18-DD0031?logo=angular)](https://angular.io/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178C6?logo=typescript)](https://www.typescriptlang.org/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap-5.x-7952B3?logo=bootstrap)](https://getbootstrap.com/)

> *An intelligent web application for comprehensive Alzheimer's disease patient management, featuring automated risk detection, real-time alerts, and multi-role patient monitoring.*

---

## 📋 Overview

**NeuroGuard** is a cutting-edge healthcare platform designed to revolutionize the care of Alzheimer's disease patients. Continuous monitoring is essential for preventing high-risk situations such as falls, wandering, and abnormal behaviors. However, existing solutions often lack automation and personalization. NeuroGuard addresses these challenges by providing an intelligent, automated system that continuously analyzes patient behavior and enhances their safety through predictive analytics and business rule-based detection.

### 🎯 Project Context

The care of Alzheimer's patients requires continuous surveillance to prevent dangerous situations such as:
- 🚨 Falls and physical injuries
- 🚶 Wandering and getting lost
- ⚠️ Abnormal or dangerous behaviors
- 📉 Health deterioration patterns

Current solutions are often:
- ❌ Poorly automated
- ❌ Insufficiently personalized
- ❌ Lack real-time monitoring capabilities
- ❌ Limited coordination between caregivers

### 🎯 Objectives

NeuroGuard aims to develop an intelligent web application with the following capabilities:
- 🔍 **Automated Risk Detection** - Business rule-based algorithms for identifying dangerous situations
- 🔔 **Smart Alert Generation** - Real-time notifications to relevant stakeholders
- 📊 **Comprehensive Patient Monitoring** - Continuous tracking of patient health and behavior
- 👥 **Multi-Role Management** - Clear role definitions for administrators, caregivers, healthcare providers, and patients
- 📈 **Medical History Tracking** - Detailed progression and diagnostic records
- 💬 **Community Forum** - Knowledge sharing and support network

---

## ✨ Features

### 🔐 Authentication & Authorization
- Secure user registration and login
- Role-based access control (RBAC)
- JWT token-based authentication
- Password recovery and reset

### 👤 User Role Management

#### 🔧 Administrator
- User management (create, update, delete, assign roles)
- System configuration and monitoring
- Overall platform oversight

#### 🏥 Healthcare Provider
- Patient medical history management
- Disease progression tracking
- Diagnosis and treatment planning
- Alert monitoring and response

#### 🤝 Caregiver
- Assigned patient monitoring
- Daily care task management
- Alert notifications and handling
- Patient activity tracking

#### 🙋 Patient
- Personal profile management
- Medical history viewing
- Alert history access
- Forum participation

### 🚨 Alert System
- Real-time risk detection
- Automated severity classification
- Multi-channel notifications
- Alert history and analytics
- Customizable alert rules

### 📋 Medical History Management
- Comprehensive patient records
- Disease progression tracking
- Diagnosis documentation
- Treatment history
- Stage-based categorization (Mild, Moderate, Severe)

### 💬 Community Forum
- Post creation and management
- Comment system with threading
- Topic categorization
- User engagement and support
- Knowledge sharing platform

### 🔍 Advanced Search & Filtering
- Real-time search functionality
- Multi-criteria filtering
- Sorting capabilities
- Pagination for large datasets

---

## 🛠️ Tech Stack

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| ![Angular](https://img.shields.io/badge/-Angular-DD0031?logo=angular&logoColor=white) | 18.x | Core framework |
| ![TypeScript](https://img.shields.io/badge/-TypeScript-3178C6?logo=typescript&logoColor=white) | 5.x | Primary language |
| ![Bootstrap](https://img.shields.io/badge/-Bootstrap-7952B3?logo=bootstrap&logoColor=white) | 5.x | UI framework |
| ![SCSS](https://img.shields.io/badge/-SCSS-CC6699?logo=sass&logoColor=white) | - | Styling |
| ![RxJS](https://img.shields.io/badge/-RxJS-B7178C?logo=reactivex&logoColor=white) | - | Reactive programming |
| **Tabler Icons** | - | Icon library |
| **FormsModule & ReactiveFormsModule** | - | Form handling |

**Key Frontend Features:**
- 🎨 Modern, gradient-based UI design
- 📱 Fully responsive layout
- ♿ Accessibility features
- 🔄 OnPush change detection strategy
- 🎭 Standalone component architecture
- 🚀 Lazy loading modules
- 🔌 HTTP interceptors for authentication

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| ![Spring Boot](https://img.shields.io/badge/-Spring%20Boot-6DB33F?logo=springboot&logoColor=white) | 3.x | Backend framework |
| ![Java](https://img.shields.io/badge/-Java-007396?logo=java&logoColor=white) | 17+ | Programming language |
| ![MySQL](https://img.shields.io/badge/-MySQL-4479A1?logo=mysql&logoColor=white) | 8.x | Database |
| **Spring Security** | - | Authentication & authorization |
| **JWT** | - | Token-based auth |
| **Spring Data JPA** | - | Data persistence |
| **Hibernate** | - | ORM framework |
| **Lombok** | - | Code generation |

**Key Backend Features:**
- 🔐 JWT-based authentication
- 🛡️ Role-based authorization
- 📊 RESTful API architecture
- 🗄️ JPA/Hibernate ORM
- 🔄 Business rule engine for risk detection
- 📧 Email notification system
- 🧪 Unit and integration testing

---

## 🏗️ Architecture

### Frontend Architecture
```
src/
├── app/
│   ├── core/                    # Core services & guards
│   │   ├── guards/              # Route guards
│   │   ├── interceptors/        # HTTP interceptors
│   │   ├── models/              # TypeScript interfaces/models
│   │   └── services/            # Business logic services
│   ├── Front-office/            # User-facing modules
│   │   ├── patient/             # Patient features
│   │   ├── caregiver/           # Caregiver features
│   │   └── healthcare-provider/ # Provider features
│   ├── Back-office/             # Admin modules
│   ├── pages/                   # Shared pages
│   │   ├── authentication/      # Login/Register
│   │   ├── post-list/           # Forum posts
│   │   └── post-detail/         # Post details
│   └── theme/                   # Layouts & shared components
├── assets/                      # Static resources
├── environments/                # Environment configs
└── scss/                        # Global styles
```

### Backend Architecture
```
src/main/java/
├── controllers/          # REST API endpoints
├── services/            # Business logic
├── repositories/        # Data access layer
├── models/              # Entity classes
├── dto/                 # Data transfer objects
├── security/            # Security configuration
├── config/              # Application config
└── utils/               # Utility classes
```

### Design Patterns
- ✅ **MVC Pattern** - Separation of concerns
- ✅ **Dependency Injection** - Loose coupling
- ✅ **Repository Pattern** - Data abstraction
- ✅ **DTO Pattern** - Data transfer
- ✅ **Guard Pattern** - Route protection
- ✅ **Interceptor Pattern** - HTTP request/response handling
- ✅ **Observer Pattern** - RxJS reactive programming

---

## 👥 Contributors

This project was developed by a dedicated team of students and supervised by experienced faculty members.

### Development Team
- 👨‍💻 **[Team Member 1]** - Full Stack Developer
- 👨‍💻 **[Team Member 2]** - Frontend Developer
- 👨‍💻 **[Team Member 3]** - Backend Developer
- 👨‍💻 **[Team Member 4]** - UI/UX Designer

### Academic Supervision
- 👨‍🏫 **[Supervisor Name]** - Project Supervisor
- 🏛️ **[Institution Name]** - Academic Institution

---

## 🎓 Academic Context

This project is developed as part of an **Integrated Project (Projet Intégré)** in the academic curriculum.

**Project Details:**
- 📚 **Course:** Integrated Project - Software Engineering
- 🏫 **Institution:** [Your Institution Name]
- 📅 **Academic Year:** 2025-2026
- 🎯 **Subject:** Application intelligente pour la détection des risques de la maladie d'Alzheimer
- ⏱️ **Duration:** [Project Duration]
- 🎓 **Level:** [Degree Level - e.g., License, Master]

**Learning Objectives:**
- Full-stack web application development
- Agile project management
- Team collaboration and version control
- Healthcare domain problem-solving
- Implementation of intelligent algorithms
- Real-world software deployment

---

## 🚀 Getting Started

### Prerequisites
- Node.js (v18 or higher)
- Angular CLI (v18)
- Java JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Git

### Installation

#### 1️⃣ Clone the Repository
```bash
git clone https://github.com/your-username/neuroguard.git
cd neuroguard
```

#### 2️⃣ Frontend Setup
```bash
# Navigate to frontend directory
cd FrontEnd

# Install dependencies
npm install

# Start development server
ng serve

# Application will run on http://localhost:4200
```

#### 3️⃣ Backend Setup
```bash
# Navigate to backend directory
cd BackEnd

# Configure database in application.properties
# Update the following properties:
# spring.datasource.url=jdbc:mysql://localhost:3306/neuroguard
# spring.datasource.username=your_username
# spring.datasource.password=your_password

# Build the application
mvn clean install

# Run the application
mvn spring-boot:run

# API will run on http://localhost:8080
```

#### 4️⃣ Database Setup
```sql
-- Create database
CREATE DATABASE neuroguard;

-- The application will auto-create tables on first run
-- Or import the provided SQL schema if available
```

### Environment Configuration

**Frontend** (`src/environments/environment.ts`):
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

**Backend** (`application.properties`):
```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/neuroguard
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update
jwt.secret=your-secret-key
```

### Default Credentials
```
Administrator:
Email: admin@neuroguard.com
Password: admin123

Healthcare Provider:
Email: provider@neuroguard.com
Password: provider123

Caregiver:
Email: caregiver@neuroguard.com
Password: caregiver123

Patient:
Email: patient@neuroguard.com
Password: patient123
```

---

## 🧪 Testing

### Run Frontend Tests
```bash
# Unit tests
ng test

# E2E tests
ng e2e

# Code coverage
ng test --code-coverage
```

### Run Backend Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AlertServiceTest
```

---

## 📦 Build & Deployment

### Frontend Production Build
```bash
ng build --configuration production

# Output will be in dist/ directory
```

### Backend Production Build
```bash
mvn clean package

# JAR file will be in target/ directory
java -jar target/neuroguard-backend.jar
```

---

## 📱 Application Features Demo

### Dashboard Views
- 🏠 **Home Dashboard** - Overview of patient status and recent alerts
- 📊 **Analytics** - Visual representations of patient data
- 🔔 **Alert Management** - Real-time alert monitoring and response
- 📋 **Patient Records** - Comprehensive medical history
- 💬 **Forum** - Community engagement platform

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- 🎨 **Mantis Template** - Base admin dashboard template
- 📚 **Angular Team** - Excellent framework and documentation
- 🍃 **Spring Team** - Robust backend framework
- 🎓 **Academic Supervisors** - Guidance and support
- 💡 **Healthcare Professionals** - Domain expertise and requirements
- 🌟 **Open Source Community** - Libraries and tools

### Special Thanks
- All healthcare professionals who provided insights into Alzheimer's care challenges
- Beta testers who provided valuable feedback
- The open-source community for amazing tools and libraries

---

## 📞 Contact & Support

For questions, issues, or suggestions:

- 📧 Email: support@neuroguard.com
- 🐛 Issues: [GitHub Issues](https://github.com/your-username/neuroguard/issues)
- 💬 Discussions: [GitHub Discussions](https://github.com/your-username/neuroguard/discussions)

---

## 🗺️ Roadmap

### Current Version (v1.0)
- ✅ User authentication and authorization
- ✅ Multi-role management
- ✅ Alert system
- ✅ Medical history tracking
- ✅ Community forum

### Future Enhancements (v2.0)
- 🔮 AI/ML-based behavior prediction
- 📱 Mobile application (iOS & Android)
- 🌐 Multi-language support
- 📊 Advanced analytics dashboard
- 🔗 IoT device integration
- 📞 Video consultation feature
- 🗣️ Voice assistant integration

---

<div align="center">

**Made with ❤️ for better Alzheimer's patient care**

⭐ Star this repository if you find it helpful!

</div>



