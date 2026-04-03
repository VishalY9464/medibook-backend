# MediBook — Online Appointment Booking System

> Book Smarter. Heal Faster. Care Better.

---

## What is MediBook?

MediBook is a full-stack Online Appointment Booking System built with Java Spring Boot.
It works like Practo — patients find doctors, book appointments, make payments, and
view medical records. Doctors manage their schedule and patient records. Admin controls
the entire platform.

---

## UC1 — Auth Service (COMPLETE)

### What it does
Handles all authentication and user management for the entire platform.
Every protected API on the platform is secured through this service.
No other service works without this — it is the foundation.

---

### Why each file exists
```
User.java
→ defines the users table in MySQL
→ stores everyone — patients, doctors, admins in one table
→ role field separates them (Patient / Provider / Admin)

UserRepository.java
→ all database queries for users table
→ findByEmail() → used during login
→ existsByEmail() → check duplicate on register
→ findAllByRole() → admin gets all patients or providers
→ deleteByUserId() → admin deletes account

AuthService.java (interface)
→ defines the CONTRACT — what methods must exist
→ register(), login(), logout(), validateToken()...
→ AuthServiceImpl must implement all of these
→ keeps code loosely coupled (easy to change later)

AuthServiceImpl.java
→ actual business logic lives here
→ bcrypt hashes password on register
→ validates password on login using bcrypt.matches()
→ generates JWT token after successful login
→ refreshes token, updates profile, changes password

JwtUtil.java
→ generates JWT token with email + role + userId inside
→ token expires in 24 hours (from application.properties)
→ validates token on every request
→ extracts email, role, userId from token

JwtFilter.java
→ intercepts EVERY request before it reaches controller
→ reads Authorization: Bearer <token> header
→ validates token using JwtUtil
→ sets authentication in SecurityContext
→ Spring Security then knows who this user is

SecurityConfig.java
→ defines which URLs are public (no token needed)
→ defines which role can access which URL
→ adds JwtFilter to filter chain
→ configures BCrypt password encoder
→ disables CSRF (not needed for JWT REST APIs)

AuthResource.java
→ REST controller — entry point for all auth APIs
→ receives HTTP requests from Postman or browser
→ calls AuthService methods
→ returns JSON responses

RegisterRequest.java
→ what client sends when registering
→ fullName, email, password, phone, role
→ has validation annotations (@NotBlank, @Email, @Size)

LoginRequest.java
→ what client sends when logging in
→ email and password only

AuthResponse.java
→ what server sends back after login
→ token, role, userId, fullName, message
```

---

### How all classes connect
```
Client sends request (Postman / Browser)
          ↓
JwtFilter.java
→ checks Authorization header
→ if token exists → validates via JwtUtil
→ sets role in SecurityContext
          ↓
SecurityConfig.java
→ /auth/register and /auth/login → public (no token needed)
→ /appointments/** → Patient only
→ /slots/** → Provider only
→ /admin/** → Admin only
          ↓
AuthResource.java (@RestController)
→ receives the request
→ calls authService.register() or authService.login()
          ↓
AuthService.java (interface)
→ defines what methods exist
          ↓
AuthServiceImpl.java (business logic)
→ calls UserRepository → talks to MySQL
→ calls JwtUtil → generates token
→ calls PasswordEncoder → bcrypt hash
          ↓
UserRepository.java
→ findByEmail(), save(), existsByEmail()...
          ↓
User.java (entity)
→ maps to users table in MySQL
          ↓
MySQL medibook_db → users table
```

---

### How JWT works
```
Step 1 → User logs in with email + password
Step 2 → AuthServiceImpl validates credentials
Step 3 → JwtUtil.generateToken() creates token with:
         → email (subject)
         → role  (Patient / Provider / Admin)
         → userId
         → expiry (24 hours)
Step 4 → Token returned to client in AuthResponse
Step 5 → Client stores token
Step 6 → Client sends token in every future request:
         Authorization: Bearer eyJhbGci...
Step 7 → JwtFilter intercepts request
Step 8 → JwtUtil.validateToken() → true or false
Step 9 → JwtUtil.extractRole() → ROLE_Patient etc
Step 10→ SecurityConfig checks role → allows or denies
```

---

### How BCrypt works
```
Register:
password "rahul123" → BCrypt.encode() → "$2a$10$xyz..."
stored in database as hash (never plain text)

Login:
BCrypt.matches("rahul123", "$2a$10$xyz...") → true
even if database is hacked passwords are safe
```

---

### API Endpoints
```
POST   /auth/register            → register new Patient or Provider
POST   /auth/login               → login, returns JWT token
POST   /auth/logout              → logout (client discards token)
POST   /auth/refresh             → get new token from old token
GET    /auth/profile/{userId}    → get user profile
PUT    /auth/profile/{userId}    → update name, phone, profilePic
PUT    /auth/password/{userId}   → change password
PUT    /auth/deactivate/{userId} → deactivate account (soft delete)
```

---

### Test Results
```
✅ POST /auth/register          → 201 Created
✅ POST /auth/login             → 200 OK + JWT token
✅ GET  /auth/profile/1         → 200 OK + user data
✅ PUT  /auth/profile/1         → 200 OK + updated data
✅ PUT  /auth/password/1        → 200 OK
✅ POST /auth/logout            → 200 OK
✅ POST /auth/refresh           → 200 OK + new token
✅ PUT  /auth/deactivate/1      → 200 OK
```

---

### How to Run
```
1. Make sure MySQL is running
2. Create database → CREATE DATABASE medibook_db;
3. Update application.properties → spring.datasource.password=yourpassword
4. Run MedibookApplication.java
5. App starts on → http://localhost:8080
6. Swagger UI  → http://localhost:8080/swagger-ui.html
```

---

## Status
```
✅ UC1 — Auth Service        COMPLETE
⏳ UC2 — Provider Service    IN PROGRESS
⏳ UC3 onwards               PENDING
```

---

*MediBook *
