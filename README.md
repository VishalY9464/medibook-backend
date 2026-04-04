# MediBook — UC3 Schedule Service

---

## What we built in UC3

A complete slot management system for doctors.
Think of it like a doctor's diary — but digital, real time, and smart.
Doctor adds time windows when they are free.
Patients see only the free slots and pick one to book.

---

## What problem UC3 solves

Without UC3:
- No way for doctor to say "I am free at 10:00 AM on April 10"
- No way for patient to see available time slots
- No calendar management for doctors
- Double booking possible — two patients same slot

With UC3:
- Doctor adds slots to their digital calendar
- Doctor can block slots for personal reasons
- Doctor can generate weekly/daily recurring slots automatically
- Patient sees only free unblocked slots
- Double booking prevented by @Version (optimistic locking)
- Expired slots auto cleaned by scheduler at midnight

---

## How UC3 connects to UC1 and UC2

UC1 gave us:
- User account + JWT token
- Doctor is a User with role=Provider

UC2 gave us:
- Doctor professional profile (providerId)
- Doctor is verified by admin and available

UC3 builds on top:
- Doctor uses their providerId from UC2
- Creates time slots linked to their providerId
- Three records now exist for every doctor:
  1. users table → basic account (UC1)
  2. providers table → professional profile (UC2)
  3. availability_slots → time calendar (UC3)

UC4 will use UC3:
- When patient books → bookSlot() called here
- When patient cancels → releaseSlot() called here
- UC3 is the bridge between doctor schedule and patient booking

---

## Files we created and why each exists

**SlotRequest.java (DTO)**
- What doctor sends when creating a slot
- providerId, date, startTime, endTime, durationMinutes
- recurrence (NONE/DAILY/WEEKLY) for recurring slots
- recurrenceEndDate for when to stop recurring
- We never expose entity directly to outside world

**AvailabilitySlot.java (entity)**
- Maps to availability_slots table in MySQL
- One row = one appointment window
- isBooked = false means patient can book it
- isBooked = true means patient already booked it
- isBlocked = false means slot is visible to patients
- isBlocked = true means doctor blocked it — invisible
- @Version field prevents double booking
- createdAt auto set by @PrePersist

**SlotRepository.java**
- All database queries for availability_slots table
- findByProviderId() — doctor views full schedule
- findByProviderIdAndDate() — slots on specific date
- findAvailableByProviderAndDate() — main patient query
- findExpiredSlots() — scheduler finds old slots to delete
- deleteBySlotId() — with @Modifying + @Transactional
- Spring writes SQL automatically

**ScheduleService.java (interface)**
- Defines the contract — what methods must exist
- addSlot(), blockSlot(), bookSlot(), releaseSlot()...
- No logic here — just declares what must be implemented
- PDF 5 layer pattern requires this strictly

**ScheduleServiceImpl.java (business logic)**
- addSlot: validates past date → builds slot → saves
- addBulkSlots: loops through list → calls addSlot each time
- generateRecurringSlots: loops from start to end date
  - DAILY → moves currentDate by 1 day each loop
  - WEEKLY → moves currentDate by 1 week each loop
- bookSlot: checks not booked → checks not blocked → sets isBooked=true → @Transactional ensures atomicity
- releaseSlot: sets isBooked=false → slot available again
- blockSlot: checks not booked → sets isBlocked=true
- unblockSlot: sets isBlocked=false → visible again
- deleteExpiredSlots: finds past unbooked → deletes all
- deleteSlot: @Transactional + @Modifying required

**ScheduleResource.java (REST controller)**
- Entry point for all /slots/** API calls
- Receives request → calls service → returns JSON
- No business logic here — thin controller

---

## Most important concept — Slot State Machine

```
CREATED
→ isBooked = false, isBlocked = false
→ Patient CAN see it and book it
         ↓
Doctor blocks it
→ isBlocked = true
→ Patient CANNOT see it — completely invisible
         ↓
Doctor unblocks it
→ isBlocked = false
→ Patient CAN see it again
         ↓
Patient books it (UC4 calls bookSlot())
→ isBooked = true
→ Disappears from available list
         ↓
Patient cancels (UC4 calls releaseSlot())
→ isBooked = false
→ Slot available again for others
         ↓
Date passes, nobody booked
→ SlotExpiryScheduler runs at midnight
→ deleteExpiredSlots() called automatically
→ Slot deleted — keeps database clean
```

---

## Double Booking Prevention — How @Version works

This is a strict PDF requirement under Data Integrity.

```java
@Version
private int version;
```

**Scenario WITHOUT @Version:**
- Patient A and Patient B both click Book on same slot at same millisecond
- Both read isBooked = false
- Both set isBooked = true
- Both save
- Result → DOUBLE BOOKING — two patients, same slot ❌

**Scenario WITH @Version:**
- Patient A reads slot → version = 0
- Patient B reads slot → version = 0
- Patient A saves → version becomes 1
- Patient B tries to save → sees version is now 1, not 0
- CONFLICT detected → throws OptimisticLockException
- Patient B gets error "slot already booked"
- Only Patient A gets the slot ✅

This is called Optimistic Locking.
No database locks needed. No performance impact.
Just a version number that increments on every save.

---

## Recurring Slots — How the logic works

Doctor wants every Monday 10:00-10:30 in April:

```
Input:
date = 2026-04-07 (first Monday)
recurrence = WEEKLY
recurrenceEndDate = 2026-04-30

Logic in generateRecurringSlots():
currentDate = April 7
while currentDate is not after April 30:
  → create slot for currentDate at 10:00-10:30
  → save to database
  → currentDate = currentDate + 1 week

Iteration 1 → April 7  → slot created ✅
Iteration 2 → April 14 → slot created ✅
Iteration 3 → April 21 → slot created ✅
Iteration 4 → April 28 → slot created ✅
Iteration 5 → May 5    → after April 30 → loop stops

Result: 4 slots created automatically
Doctor saved from adding each one manually
```

---

## How all files connect

**Doctor adds a slot:**
```
POST /slots/add
         ↓
ScheduleResource.addSlot()
         ↓
ScheduleServiceImpl.addSlot()
→ validates date is not in past
→ builds AvailabilitySlot object
→ calls SlotRepository.save()
         ↓
MySQL availability_slots table → new row inserted
```

**Patient views available slots:**
```
GET /slots/available/2?date=2026-04-10
         ↓
ScheduleResource.getAvailable()
         ↓
ScheduleServiceImpl.getAvailableSlots()
         ↓
SlotRepository.findAvailableByProviderAndDate()
→ WHERE isBooked=false AND isBlocked=false
         ↓
Returns only free slots to patient
```

**Patient books a slot (called from UC4):**
```
AppointmentService calls bookSlot(slotId)
         ↓
ScheduleServiceImpl.bookSlot()
→ @Transactional ensures atomic operation
→ checks isBooked=false
→ checks isBlocked=false
→ sets isBooked=true
→ saves with @Version increment
→ if two patients tried same slot →
  second one gets OptimisticLockException
         ↓
Slot disappears from patient search immediately
```

---

## API Endpoints

| Method | URL | Who calls | When in UI |
|--------|-----|-----------|------------|
| POST | /slots/add | Doctor | Schedule page → Add Slot button |
| POST | /slots/bulk | Doctor | Schedule page → Save All button |
| POST | /slots/recurring | Doctor | Schedule page → Set Recurring |
| GET | /slots/provider/{providerId} | Doctor / Admin | My Schedule calendar loads |
| GET | /slots/available/{providerId}?date | Patient | Book Appointment → picks date |
| GET | /slots/{slotId} | System (UC4) | Internally before booking |
| PUT | /slots/{slotId} | Doctor | Edit slot form → Save |
| PUT | /slots/{slotId}/block | Doctor | Right click slot → Block |
| PUT | /slots/{slotId}/unblock | Doctor | Click blocked slot → Unblock |
| DELETE | /slots/{slotId} | Doctor / System | Delete slot / Midnight scheduler |

---

## Important fixes applied during UC3

**Fix 1 — deleteBySlotId() needs @Transactional + @Modifying**
```
Problem: No EntityManager with actual transaction available
Reason:  Custom delete by non-primary-key field needs
         explicit transaction annotation
Fix:     Added @Modifying + @Transactional on deleteBySlotId()
         Added @Transactional on deleteSlot() in ServiceImpl
```

**Fix 2 — Wrong providerId in URL**
```
Problem: GET /slots/available/1 returned empty list
Reason:  Slot had providerId=2 but URL used providerId=1
Fix:     Always use correct providerId from providers table
         Run GET /providers/all to check your actual providerId
```

---

## Test Results

| API | Status |
|-----|--------|
| POST /slots/add | ✅ 201 Created |
| POST /slots/bulk | ✅ 201 Created, 2 slots |
| POST /slots/recurring | ✅ 201 Created, 4 weekly slots |
| GET /slots/provider/2 | ✅ 200 OK, all slots |
| GET /slots/available/2?date | ✅ 200 OK, free slots only |
| GET /slots/1 | ✅ 200 OK, single slot |
| PUT /slots/1 | ✅ 200 OK, duration updated |
| PUT /slots/1/block | ✅ 200 OK, slot invisible to patients |
| PUT /slots/1/unblock | ✅ 200 OK, slot visible again |
| DELETE /slots/1 | ✅ 200 OK, slot deleted |

---

## What we achieved so far

**UC1 ✅ — Auth Service**
- Secure login system
- JWT token controls who accesses what
- BCrypt keeps passwords safe
- Foundation for all other UCs

**UC2 ✅ — Provider Service**
- Doctor professional profiles
- Admin verification before going live
- Patient can search and filter doctors
- avgRating auto updated from reviews

**UC3 ✅ — Schedule Service**
- Doctor calendar management
- Slots available for patient booking
- Double booking prevented by @Version
- Block/unblock for personal time
- Recurring slots save doctor time
- Foundation ready for appointment booking

---

## What comes next — UC4

```
UC4 → Appointment Service

Patient picks a slot from UC3
→ Creates appointment record
→ Calls bookSlot() from UC3
→ Appointment goes through lifecycle:

PENDING → CONFIRMED → COMPLETED
                   → CANCELLED → releaseSlot() called
                   → NO_SHOW  → detected by scheduler

This is where everything connects:
UC1 (who is the patient)
+ UC2 (which doctor)
+ UC3 (which slot)
= UC4 (the actual appointment)
```

---

## How to run

1. MySQL running with medibook_db created
2. application.properties password updated
3. Run MedibookApplication.java
4. App starts on http://localhost:8080
5. Swagger UI at http://localhost:8080/swagger-ui.html

---

## Current status

| UC | Status |
|----|--------|
| UC1 — Auth Service | ✅ COMPLETE |
| UC2 — Provider Service | ✅ COMPLETE |
| UC3 — Schedule Service | ✅ COMPLETE |
| UC4 — Appointment Service | ⏳ IN PROGRESS |
| UC5 — Payment Service | ⏳ PENDING |
| UC6 — Review Service | ⏳ PENDING |
| UC7 — Notification Service | ⏳ PENDING |
| UC8 — Medical Records | ⏳ PENDING |
| Web Layer (Thymeleaf) | ⏳ PENDING |
| Exception Handling | ⏳ PENDING |
| Scheduler Jobs | ⏳ PENDING |
| Docker Setup | ⏳ PENDING |

---

*MediBook | Capgemini Training Project*
