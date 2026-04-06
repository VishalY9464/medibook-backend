# MediBook — UC4 Appointment Service

## What we built in UC4

The complete appointment booking lifecycle system.
Think of it like the actual booking desk at a hospital.
Patient picks doctor, picks slot, confirms booking.
System creates appointment record and marks slot as taken.
Doctor conducts appointment and marks it complete.
Patient can cancel or reschedule if needed.

## What problem UC4 solves

Without UC4 there was no way to actually book an appointment,
no lifecycle management from scheduled to completed,
no cancellation or rescheduling, and no connection
between patient doctor and slot.

With UC4 patient books appointment in one API call,
slot automatically marked as booked connecting UC3,
doctor marks appointment complete, patient cancels
and slot released automatically, no show detection
ready for scheduler, everything connected UC1 UC2 UC3 UC4.

## How UC4 connects to UC1 UC2 UC3

UC1 gave us User account with patientId.
UC2 gave us Doctor profile with providerId.
UC3 gave us Time slot with slotId.

UC4 combines all three into one Appointment record:
patientId + providerId + slotId + serviceType
+ date + time + modeOfConsultation + status

This is the central record of the entire system.
Everything else like payment review and medical record
links back to appointmentId.

## Files we created and why each exists

AppointmentRequest.java is the DTO for what patient sends
when booking. Contains patientId, providerId, slotId,
serviceType, appointmentDate, startTime, endTime,
modeOfConsultation which is IN_PERSON or TELECONSULTATION,
and optional notes from patient.

Appointment.java is the entity that maps to appointments
table in MySQL. It is the central record linking patient
plus doctor plus slot. The status field holds
SCHEDULED or COMPLETED or CANCELLED or NO_SHOW.
PrePersist sets createdAt automatically.
PreUpdate sets updatedAt on every status change.
updatedAt is part of PDF audit trail requirement.

AppointmentRepository.java has all database queries.
findByPatientId lets patient view history.
findByProviderId lets doctor view schedule.
findByProviderIdAndAppointmentDate gives todays schedule.
findUpcomingByPatientId returns upcoming only.
findNoShowAppointments helps scheduler detect no shows.
countByProviderId helps doctor earnings analytics.

AppointmentService.java is the interface that defines
the contract of what methods must exist.
bookAppointment, cancelAppointment, rescheduleAppointment,
completeAppointment, updateStatus, getAppointmentCount.

AppointmentServiceImpl.java has actual business logic.
bookAppointment verifies slot then creates record then
calls scheduleService.bookSlot from UC3.
cancelAppointment checks SCHEDULED then sets CANCELLED
then calls scheduleService.releaseSlot from UC3.
rescheduleAppointment releases old slot and books new
while verifying same doctor as PDF requires.
completeAppointment sets COMPLETED which unlocks
review in UC6 and medical record in UC8.
updateStatus is used by scheduler for NO_SHOW.

AppointmentResource.java is the REST controller
and entry point for all /appointments calls.
Thin controller with no business logic here.

## Appointment Status Lifecycle from PDF

Patient books appointment and status becomes SCHEDULED.

From SCHEDULED there are three paths.
Doctor marks complete and status becomes COMPLETED.
After COMPLETED patient can submit review in UC6
and doctor can create medical record in UC8.

Patient cancels and status becomes CANCELLED.
After CANCELLED slot released back to available in UC3
and refund triggered in UC5.

Nobody shows up and scheduler auto sets NO_SHOW.

## Key business rules from PDF

Rule 1 is slot verification before booking.
Slot must not be booked meaning isBooked is false.
Slot must not be blocked meaning isBlocked is false.
Slot must belong to requested doctor.
All checked before appointment created.

Rule 2 is same doctor for rescheduling.
Patient cannot switch doctor by rescheduling.
Must book new appointment for different doctor.

Rule 3 is status based operations.
Can only cancel SCHEDULED appointments.
Can only complete SCHEDULED appointments.
Can only reschedule SCHEDULED appointments.

Rule 4 is atomic operations with Transactional annotation.
Booking means appointment save plus slot booking together.
Cancellation means status update plus slot release together.
If one fails both roll back and no inconsistent state.

Rule 5 is audit trail requirement from PDF.
PreUpdate auto updates updatedAt on every change.
PDF requires all status changes logged with timestamp.

## How bookAppointment works step by step

Patient sends POST to /appointments/book.
AppointmentResource receives request.
AppointmentServiceImpl.bookAppointment runs.
Gets slot from UC3 via scheduleService.getSlotById.
Checks slot isBooked equals false.
Checks slot isBlocked equals false.
Checks slot providerId matches request providerId.
Builds Appointment object with status SCHEDULED.
Saves appointment to database.
Calls scheduleService.bookSlot and UC3 marks slot booked.
Transactional annotation ensures both happen together.
Returns appointmentId plus status plus date plus time.

## How cancelAppointment works step by step

Patient sends PUT to /appointments/id/cancel.
AppointmentServiceImpl.cancelAppointment runs.
Finds appointment by id.
Checks status equals SCHEDULED.
If COMPLETED or NO_SHOW throws error.
Sets status to CANCELLED.
Saves appointment and PreUpdate updates updatedAt.
Calls scheduleService.releaseSlot.
Slot immediately available for other patients.
Transactional ensures both happen together.
Returns success message.

## API Endpoints

POST /appointments/book is for patient booking.
GET /appointments/appointmentId gets details.
GET /appointments/patient/patientId gets patient history.
GET /appointments/patient/patientId/upcoming gets upcoming only.
GET /appointments/provider/providerId gets doctor schedule.
GET /appointments/provider/id/date with date param gets daily schedule.
PUT /appointments/id/cancel cancels booking.
PUT /appointments/id/reschedule moves to new slot.
PUT /appointments/id/complete is for doctor to mark done.
PUT /appointments/id/status with status param is for admin or scheduler.
GET /appointments/provider/id/count gets total count.

## Test Results

POST /appointments/book returned 201 Created.
GET /appointments/1 returned 200 OK.
GET /appointments/patient/3 returned 200 OK with list.
GET /appointments/patient/3/upcoming returned 200 OK upcoming.
GET /appointments/provider/2 returned 200 OK with list.
GET /appointments/provider/2/date returned 200 OK daily.
PUT /appointments/1/complete returned 200 OK.
PUT /appointments/1/status returned 200 OK.
GET /appointments/provider/2/count returned 200 OK count.
PUT /appointments/2/cancel returned 200 OK cancelled.
PUT /appointments/3/reschedule returned 200 OK rescheduled.

## What we achieved so far

UC1 is complete with secure login JWT BCrypt foundation.
UC2 is complete with doctor profiles admin verification search.
UC3 is complete with slot calendar block recurring double booking prevention.
UC4 is complete with booking lifecycle connected to all above.

## What comes next UC5

UC5 is Payment Service.
Patient pays for appointment online.
Mock payment with no real gateway yet.
Payment status goes PENDING then SUCCESS then REFUNDED.
Refund triggered when appointment cancelled.
Provider earnings dashboard available.
Admin revenue analytics available.

## Current status

UC1 Auth Service is COMPLETE.
UC2 Provider Service is COMPLETE.
UC3 Schedule Service is COMPLETE.
UC4 Appointment Service is COMPLETE.
UC5 Payment Service is IN PROGRESS.
UC6 onwards is PENDING.

MediBook Capgemini Training Project
