# MediBook — UC6 Review / Rating Service

---

## Overview

The Review/Rating Service allows patients to rate and review healthcare providers
after a completed appointment. One review is permitted per appointment enforced
by a unique constraint on appointmentId. The service automatically updates the
doctor average rating after every review submission, update, or deletion.
Admin moderation can remove inappropriate content.

---

## Folder Structure

```
src/main/java/com/medibook/review/
├── dto/
│   └── ReviewRequest.java
├── entity/
│   └── Review.java
├── repository/
│   └── ReviewRepository.java
├── service/
│   ├── ReviewService.java
│   └── impl/
│       └── ReviewServiceImpl.java
└── resource/
    └── ReviewResource.java
```

---

## Entity — Review.java

| Field | Type | Description |
|-------|------|-------------|
| reviewId | int | Primary key auto generated |
| appointmentId | int | Unique — one review per appointment |
| patientId | int | Who wrote the review |
| providerId | int | Which doctor was reviewed |
| rating | int | 1 to 5 stars only |
| comment | String | Written feedback optional |
| isAnonymous | boolean | Hide patient name if true |
| createdAt | LocalDateTime | Auto set on save |
| updatedAt | LocalDateTime | Auto updated on every change |

---

## Business Rules (strictly from PDF)

- Only COMPLETED appointments can be reviewed
- One review per appointment enforced by unique constraint on appointmentId
- Rating must be between 1 and 5 stars validated in both DTO and ServiceImpl
- Doctor avgRating updates automatically after every review submit, update, or delete
- Admin can delete inappropriate reviews
- Anonymous reviews supported — patient can hide their name
- Patient can update their own review rating and comment

---

## PDF Requirement Check

| PDF Requirement | Implemented |
|----------------|-------------|
| One review per appointment | ✅ Unique constraint + DuplicateResourceException |
| Rating 1-5 only | ✅ @Min @Max on DTO + BadRequestException in service |
| avgRating auto update | ✅ updateDoctorRating() called after every change |
| Anonymous reviews | ✅ isAnonymous field on entity |
| Admin moderation | ✅ DELETE /reviews/{reviewId} |
| Patient can update review | ✅ PUT /reviews/{reviewId} |

---

## API Endpoints

| Method | URL | Who Calls | When |
|--------|-----|-----------|------|
| POST | /reviews/submit | Patient | After appointment COMPLETED |
| GET | /reviews/provider/{providerId} | Patient / Guest | View doctor profile |
| GET | /reviews/patient/{patientId} | Patient | View own review history |
| GET | /reviews/{reviewId} | Admin / System | View specific review |
| PUT | /reviews/{reviewId} | Patient | Edit their own review |
| DELETE | /reviews/{reviewId} | Admin | Remove inappropriate review |
| GET | /reviews/provider/{providerId}/average | Guest / Patient | Doctor profile page |
| GET | /reviews/provider/{providerId}/count | Guest / Patient | Doctor profile page |

---

## Service Connections

```
ReviewServiceImpl
    → AppointmentService (UC4)
      → verify appointment exists
      → check appointment status = COMPLETED
      → throws 404 if appointment not found
      → throws 400 if appointment not completed

    → ProviderService (UC2)
      → updateRating(providerId, newAvg)
      → called after every submit, update, delete
      → doctor profile shows new avgRating instantly
```

---

## How avgRating Updates Internally

```
Patient submits review (rating = 5)
↓
ReviewServiceImpl.submitReview()
→ saves review to database
→ calls updateDoctorRating(providerId)
  → SELECT AVG(rating) FROM reviews WHERE provider_id = ?
  → gets new average e.g. 4.3
  → providerService.updateRating(providerId, 4.3)
  → UPDATE providers SET avg_rating = 4.3 WHERE provider_id = ?
Doctor profile now shows avgRating = 4.3 ✅
```

---

## Exception Handling

| Exception | HTTP Code | When Thrown |
|-----------|-----------|-------------|
| ResourceNotFoundException | 404 Not Found | Review not found by ID |
| DuplicateResourceException | 409 Conflict | Review already exists for appointment |
| BadRequestException | 400 Bad Request | Appointment not COMPLETED |
| BadRequestException | 400 Bad Request | Rating not between 1 and 5 |

---

## Test Flow (Postman)

```
Step 1 — Complete appointment
PUT http://localhost:8080/appointments/{id}/status?status=COMPLETED
Authorization: Bearer <token>

Step 2 — Submit review
POST http://localhost:8080/reviews/submit
Authorization: Bearer <token>
Body:
{
  "appointmentId": 1,
  "patientId": 3,
  "providerId": 2,
  "rating": 5,
  "comment": "Dr. Sharma was very professional and helpful.",
  "anonymous": false
}

Step 3 — Verify doctor avgRating updated
GET http://localhost:8080/providers/2

Step 4 — Get all reviews for doctor
GET http://localhost:8080/reviews/provider/2

Step 5 — Get average rating
GET http://localhost:8080/reviews/provider/2/average

Step 6 — Update review
PUT http://localhost:8080/reviews/1
Body: { "rating": 4, "comment": "Updated review." }

Step 7 — Delete review (admin)
DELETE http://localhost:8080/reviews/1
Authorization: Bearer <admin_token>
```

---

## Test Results

| API | Expected | Status |
|-----|----------|--------|
| POST /reviews/submit | 201 Created | ✅ |
| GET /reviews/provider/2 | 200 OK | ✅ |
| GET /reviews/patient/3 | 200 OK | ✅ |
| GET /reviews/provider/2/average | 200 OK avgRating shown | ✅ |
| GET /reviews/provider/2/count | 200 OK count shown | ✅ |
| PUT /reviews/1 | 200 OK updated | ✅ |
| DELETE /reviews/1 | 200 OK deleted | ✅ |

---

*MediBook | Capgemini Training Project | Java Spring Boot*
