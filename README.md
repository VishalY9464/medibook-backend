# MediBook — UC2 Provider Service

---

## What we built in UC2

A complete provider profile management system.
Think of it like the doctor registration desk at a hospital.
Doctor comes in, fills their professional details, waits for admin approval,
then appears on the platform for patients to find and book.

---

## What problem UC2 solves
```
Without UC2:
→ No doctor profiles on platform
→ Patients cannot search for doctors
→ No verification system — fake doctors could join
→ No way to filter by specialization or location

With UC2:
→ Doctors create professional profiles after registering as user (UC1)
→ Admin verifies credentials before doctor goes live
→ Patients can search, filter, and browse verified doctors
→ Doctor can manage their own availability status
```

---

## How UC2 connects to UC1
```
UC1 gave us:
→ User account (userId, email, role, JWT token)
→ Every doctor is FIRST a User with role=Provider

UC2 builds on top:
→ Doctor uses their userId from UC1
→ Creates a Provider profile linked by userId
→ Two records now exist for every doctor:
  1. users table    → basic account (UC1)
  2. providers table → professional profile (UC2)
→ They are linked by userId field
```

---

## Files we created and why each exists
```
ProviderRequest.java (DTO)
→ what doctor sends when creating their profile
→ userId, specialization, qualification,
  experienceYears, bio, clinicName, clinicAddress
→ userId links profile to their user account from UC1
→ we never expose Provider entity directly to outside
  DTO carries only what client needs to send

Provider.java (entity)
→ maps to providers table in MySQL
→ stores professional details of every doctor
→ isVerified = false by default
  admin must approve before doctor appears in search
→ isAvailable = true by default
  doctor can turn off when on leave
→ avgRating = 0.0 by default
  updated automatically when patient submits review
→ createdAt auto set by @PrePersist

ProviderRepository.java
→ all database queries for providers table
→ findByUserId() — doctor views their own profile
→ findBySpecialization() — patient filters by type
→ findByIsVerified() — admin sees pending approvals
→ findByIsVerifiedAndIsAvailable() — main patient search
→ searchByNameOrSpecialization() — search bar query
→ Spring writes SQL automatically from method names

ProviderService.java (interface)
→ defines the contract — what methods must exist
→ registerProvider(), getProviderById(), searchProviders()
→ verifyProvider(), setAvailability(), updateRating()
→ no logic here — just declares what must be implemented
→ evaluators check this 5 layer pattern strictly

ProviderServiceImpl.java (business logic)
→ registerProvider: checks duplicate → builds Provider → saves
→ getBySpecialization: gets doctors → filters verified only
→ searchProviders: keyword search → filters verified only
→ verifyProvider: sets isVerified=true → doctor goes live
→ setAvailability: doctor on leave → sets isAvailable=false
→ updateRating: called by ReviewService when review submitted
→ getAllProviders: admin sees all including unverified
→ getVerifiedAndAvailableProviders: main patient search list

ProviderResource.java (REST controller)
→ entry point for all /providers/** API calls
→ receives request → calls service → returns JSON
→ no business logic here — thin controller
```

---

## Important business rules from PDF
```
Rule 1 — Admin verification required
→ isVerified = false when doctor first registers
→ Doctor CANNOT appear in patient search until admin approves
→ Admin checks qualification documents then calls verify API
→ Only after isVerified=true → doctor visible to patients

Rule 2 — One provider profile per user
→ One user account can only have one doctor profile
→ If doctor tries to create second profile → error thrown
→ Checked in registerProvider() before saving

Rule 3 — Both conditions needed for patient search
→ isVerified = true  (admin approved)
→ isAvailable = true (doctor accepting appointments)
→ BOTH must be true for doctor to appear in search
→ Doctor on leave → sets isAvailable=false → disappears from search

Rule 4 — avgRating auto updated
→ Doctor starts with avgRating = 0.0
→ Every time patient submits review in UC6
→ ReviewService calculates new average
→ Calls ProviderService.updateRating() to update here
```

---

## How all files connect
```
Patient searches for doctor
         ↓
GET /providers/search?keyword=cardio
         ↓
ProviderResource.search()
         ↓
ProviderServiceImpl.searchProviders("cardio")
→ calls ProviderRepository.searchByNameOrSpecialization()
→ filters to verified doctors only
         ↓
ProviderRepository queries MySQL providers table
         ↓
Returns list of matching verified doctors

Admin verifies a doctor
         ↓
PUT /providers/1/verify
         ↓
ProviderResource.verifyProvider()
         ↓
ProviderServiceImpl.verifyProvider()
→ finds Provider by id
→ sets isVerified = true
→ saves to database
         ↓
Doctor now appears in patient search
```

---

## API Endpoints
```
POST   /providers/register
→ doctor creates professional profile
→ body: userId, specialization, qualification, clinic details
→ returns 201 Created with providerId and isVerified=false

GET    /providers/{providerId}
→ get full doctor profile by id
→ used when patient clicks on a doctor card

GET    /providers/user/{userId}
→ doctor views their own profile
→ userId comes from JWT token

GET    /providers/specialization/{specialization}
→ patient filters doctors by type
→ example: /providers/specialization/Cardiologist
→ returns verified doctors only

GET    /providers/search?keyword=xxx
→ patient uses search bar
→ searches specialization and clinic address
→ example: ?keyword=cardio finds Cardiologist
→ returns verified doctors only

GET    /providers/available
→ all verified AND available doctors
→ main list shown to patients on search page

GET    /providers/all
→ admin sees all doctors including unverified
→ admin uses this to find pending verifications

PUT    /providers/{providerId}
→ doctor updates their own profile
→ can change bio, clinic details, specialization

PUT    /providers/{providerId}/verify
→ admin approves a doctor
→ sets isVerified = true
→ doctor now visible to patients

PUT    /providers/{providerId}/availability?isAvailable=false
→ doctor going on leave sets to false
→ doctor coming back sets to true

DELETE /providers/{providerId}
→ admin removes doctor from platform
```

---

## Test results
```
✅ POST /providers/register         → 201 Created, isVerified=false
✅ GET  /providers/1                → 200 OK, full profile
✅ GET  /providers/user/1           → 200 OK, profile by userId
✅ GET  /providers/specialization/Cardiologist → list after verify
✅ GET  /providers/search?keyword=cardio       → matching doctors
✅ GET  /providers/available        → verified + available doctors
✅ GET  /providers/all              → all doctors (admin view)
✅ PUT  /providers/1                → 200 OK, profile updated
✅ PUT  /providers/1/verify         → 200 OK, now visible to patients
✅ PUT  /providers/1/availability   → 200 OK, availability updated
✅ DELETE /providers/1              → 200 OK, doctor removed
```

---

## What we achieved so far
```
UC1 ✅ → Any person can register and login securely
          JWT token controls who accesses what
          BCrypt keeps passwords safe

UC2 ✅ → Doctors have professional profiles
          Admin verification prevents fake doctors
          Patients can search and filter doctors
          Foundation ready for slot booking (UC3)
```

---

## What comes next
```
UC3 → Schedule Service
→ Doctor adds available time slots
→ Slots have date, start time, end time, duration
→ Doctor can block slots for leave
→ Recurring slots (daily/weekly patterns)
→ Patients see real time available slots
→ This is what connects to appointment booking in UC4
```

---

## How to run
```
1. MySQL running with medibook_db created
2. application.properties password updated
3. Run MedibookApplication.java
4. App starts on http://localhost:8080
5. Swagger UI at http://localhost:8080/swagger-ui.html
```

---

## Current status
```
✅ UC1 — Auth Service      COMPLETE
✅ UC2 — Provider Service  COMPLETE
⏳ UC3 — Schedule Service  IN PROGRESS
⏳ UC4 onwards             PENDING
```

---

*MediBook | Capgemini Training Project*
