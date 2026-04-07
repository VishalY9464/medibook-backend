# MediBook — UC8 Medical Record Service

---

## Overview

The Medical Record Service stores electronic health records created by providers
after each completed appointment. Each MedicalRecord is linked to an appointment
and contains the diagnosis, prescription, clinical notes, an optional document
attachment URL, and a follow-up date. Patients can read their own records.
Providers access records they created. Records with a follow-up date trigger an
automated reminder notification via the Notification Service.

---

## Folder Structure

```
src/main/java/com/medibook/record/
├── dto/
│   └── RecordRequest.java
├── entity/
│   └── MedicalRecord.java
├── repository/
│   └── RecordRepository.java
├── service/
│   ├── RecordService.java
│   └── impl/
│       └── RecordServiceImpl.java
└── resource/
    └── RecordResource.java
```

---

## Entity — MedicalRecord.java

| Field | Type | Description |
|-------|------|-------------|
| recordId | int | Primary key auto generated |
| appointmentId | int | Unique — one record per appointment |
| patientId | int | Which patient this record belongs to |
| providerId | int | Which doctor created this record |
| diagnosis | String | Doctor diagnosis — required |
| prescription | String | Medications and dosage |
| notes | String | Clinical notes |
| attachmentUrl | String | S3 URL for lab reports or X-rays |
| followUpDate | LocalDate | Triggers reminder notification on this date |
| createdAt | LocalDateTime | Auto set on save |
| updatedAt | LocalDateTime | Auto updated on every change |

---

## Database Table — medical_records

```sql
CREATE TABLE medical_records (
    record_id INT AUTO_INCREMENT PRIMARY KEY,
    appointment_id INT NOT NULL UNIQUE,
    patient_id INT NOT NULL,
    provider_id INT NOT NULL,
    diagnosis VARCHAR(2000) NOT NULL,
    prescription VARCHAR(2000),
    notes VARCHAR(2000),
    attachment_url VARCHAR(500),
    follow_up_date DATE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);
```

---

## Business Rules (strictly from PDF)

- Records are created by the provider AFTER marking appointment COMPLETE
- One record per appointment enforced by unique constraint on appointmentId
- Diagnosis field is mandatory
- Patient can view only their own records
- Doctor can view only records they created
- Admin has read-only audit access to all records
- Records with follow-up date trigger automated reminder notification
- Doctor can edit record within allowed time window after appointment
- Only admin can delete records — protects medical record integrity

---

## PDF Requirement Check

| PDF Requirement | Implemented |
|----------------|-------------|
| Created after COMPLETED appointment | ✅ BadRequestException if not COMPLETED |
| One record per appointment | ✅ Unique constraint + DuplicateResourceException |
| Patient sees own records only | ✅ getRecordsByPatient() queries by patientId |
| Doctor sees own records only | ✅ getRecordsByProvider() queries by providerId |
| Admin read-only audit access | ✅ GET /records/all endpoint |
| Follow-up date reminder | ✅ getFollowUpRecords() called by scheduler |
| Document attachment URL | ✅ attachDocument() method |
| HIPAA audit trail | ✅ createdAt updatedAt tracked automatically |

---

## HIPAA Compliance

```
Every record tracks:
→ createdAt — when record was first created
→ updatedAt — when record was last modified
→ providerId — who created it
→ patientId — who it belongs to

@PrePersist → sets createdAt and updatedAt automatically
@PreUpdate  → updates updatedAt on every save

This creates complete audit trail for every record change.
PDF non-functional requirement explicitly requires this.
```

---

## API Endpoints

| Method | URL | Who Calls | When |
|--------|-----|-----------|------|
| POST | /records/create | Doctor | After completing appointment |
| GET | /records/appointment/{appointmentId} | Patient / Doctor | View record for appointment |
| GET | /records/patient/{patientId} | Patient | View medical history |
| GET | /records/provider/{providerId} | Doctor | View records they created |
| GET | /records/{recordId} | Admin | Audit access |
| PUT | /records/{recordId} | Doctor | Edit within time window |
| PUT | /records/{recordId}/attach | Doctor | Attach lab report URL |
| DELETE | /records/{recordId} | Admin | Compliance management |
| GET | /records/followups?date= | Scheduler | Auto follow-up reminders |
| GET | /records/patient/{patientId}/upcoming | Patient | Upcoming follow-ups |

---

## Service Connections

```
RecordServiceImpl
    → AppointmentRepository (UC4)
      → verify appointment exists
      → check appointment status = COMPLETED
      → throws 404 if appointment not found
      → throws 400 if not COMPLETED

    → NotificationService (UC7) via Scheduler
      → FollowUpReminderScheduler runs every night
      → calls getFollowUpRecords(today)
      → sends FOLLOWUP notification to patient
      → patient receives reminder to come for follow-up
```

---

## Follow-Up Reminder Flow

```
Doctor creates record with followUpDate = April 20
↓
Record saved to database with followUpDate = 2026-04-20

FollowUpReminderScheduler runs at midnight every night:
@Scheduled(cron = "0 0 0 * * *")
→ calls recordService.getFollowUpRecords(LocalDate.now())
→ finds all records where followUpDate = today
→ for each record:
   → calls notificationService.send()
   → recipientId = record.patientId
   → type = FOLLOWUP
   → title = "Follow-up Reminder"
   → message = "You have a follow-up appointment today"
   → patient receives email and in-app notification ✅
```

---

## Document Attachment Flow

```
Doctor uploads lab report:
Step 1 → Upload file to AWS S3 (web layer UC)
Step 2 → S3 returns URL: https://s3.amazonaws.com/medibook/lab-report-1.pdf
Step 3 → Doctor calls:
PUT /records/1/attach
Body: { "attachmentUrl": "https://s3.amazonaws.com/medibook/lab-report-1.pdf" }
Step 4 → attachmentUrl saved to medical record
Step 5 → Patient can download document from their record ✅

In development: use any mock URL for testing
```

---

## Exception Handling

| Exception | HTTP Code | When Thrown |
|-----------|-----------|-------------|
| ResourceNotFoundException | 404 Not Found | Record not found by ID |
| ResourceNotFoundException | 404 Not Found | Appointment not found |
| DuplicateResourceException | 409 Conflict | Record already exists for appointment |
| BadRequestException | 400 Bad Request | Appointment not COMPLETED |
| BadRequestException | 400 Bad Request | Diagnosis is empty |
| BadRequestException | 400 Bad Request | Follow up date in the past |
| BadRequestException | 400 Bad Request | Attachment URL is empty |

---

## Test Flow (Postman)

```
Step 1 — Complete an appointment first
PUT http://localhost:8080/appointments/{id}/status?status=COMPLETED
Authorization: Bearer <provider_token>

Step 2 — Create medical record
POST http://localhost:8080/records/create
Authorization: Bearer <provider_token>
Body:
{
  "appointmentId": 1,
  "patientId": 3,
  "providerId": 2,
  "diagnosis": "Viral fever with mild throat infection",
  "prescription": "Paracetamol 500mg twice daily for 5 days. Cetirizine 10mg at night.",
  "notes": "Patient advised rest and hydration. Avoid cold foods.",
  "followUpDate": "2026-04-20"
}
Expected: 201 Created

Step 3 — Get record for appointment
GET http://localhost:8080/records/appointment/1

Step 4 — Get all records for patient
GET http://localhost:8080/records/patient/3

Step 5 — Get all records created by doctor
GET http://localhost:8080/records/provider/2

Step 6 — Attach document
PUT http://localhost:8080/records/1/attach
Body: { "attachmentUrl": "https://mock-s3.com/lab-report-1.pdf" }

Step 7 — Update record
PUT http://localhost:8080/records/1
Body: same as create with updated diagnosis

Step 8 — Get upcoming follow-ups for patient
GET http://localhost:8080/records/patient/3/upcoming

Step 9 — Delete record (admin only)
DELETE http://localhost:8080/records/1
Authorization: Bearer <admin_token>
```

---

## Test Results

| API | Expected | Status |
|-----|----------|--------|
| POST /records/create | 201 Created | PASSED |
| GET /records/appointment/1 | 200 OK | PASSED |
| GET /records/patient/3 | 200 OK list | PASSED |
| GET /records/provider/2 | 200 OK list | PASSED |
| PUT /records/1/attach | 200 OK | PASSED |
| PUT /records/1 | 200 OK updated | PASSED |
| GET /records/patient/3/upcoming | 200 OK list | PASSED |
| DELETE /records/1 | 200 OK | PASSED |

---

## Current Progress

| UC | Service | Status |
|----|---------|--------|
| UC1 | Auth Service | COMPLETE |
| UC2 | Provider Service | COMPLETE |
| UC3 | Schedule Service | COMPLETE |
| UC4 | Appointment Service | COMPLETE |
| UC5 | Payment Service | COMPLETE |
| UC6 | Review Service | COMPLETE |
| UC7 | Notification Service | COMPLETE |
| UC8 | Medical Records | COMPLETE |
| UC9 | Exception Handling | COMPLETE |
| Web Layer | Thymeleaf MVC | PENDING |
| Schedulers | Quartz Jobs | PENDING |
| Docker | Containerization | PENDING |

---

*MediBook | Capgemini Training Project | Java Spring Boot*
