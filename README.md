# MediBook — UC7 Notification Service

---

## Overview

The Notification Service dispatches in-app, email, and SMS alerts for all
booking lifecycle events. Notifications carry a relatedId and relatedType for
deep-linking from the notification centre. The service tracks read/unread state
and exposes an unread badge count. Admin can broadcast platform-wide messages
via the bulk dispatch endpoint.

---

## Folder Structure

```
src/main/java/com/medibook/notification/
├── dto/
│   └── NotificationRequest.java
├── entity/
│   └── Notification.java
├── repository/
│   └── NotificationRepository.java
├── service/
│   ├── NotificationService.java
│   └── impl/
│       └── NotificationServiceImpl.java
└── resource/
    └── NotificationResource.java
```

---

## Entity — Notification.java

| Field | Type | Description |
|-------|------|-------------|
| notificationId | int | Primary key auto generated |
| recipientId | int | Who receives this notification |
| type | String | BOOKING / REMINDER / CANCELLATION / PAYMENT / FOLLOWUP |
| title | String | Short heading shown in bell icon |
| message | String | Full notification message |
| channel | String | APP / EMAIL / SMS |
| relatedId | int | Links to appointment/payment/record ID |
| relatedType | String | APPOINTMENT / PAYMENT / RECORD |
| isRead | boolean | false = unread shown in badge |
| sentAt | LocalDateTime | Auto set when notification created |

---

## Three Channels Explained

| Channel | How It Works | Status |
|---------|-------------|--------|
| APP | Saved to database shown in UI bell icon | REAL works now |
| EMAIL | Sent via Gmail SMTP JavaMailSender | REAL works now |
| SMS | Mock logs to console | MOCK Twilio swap ready |

Every notification is always saved to database regardless of channel.

---

## Notification Types (PDF)

| Type | When Sent | Who Receives |
|------|-----------|--------------|
| BOOKING | Appointment booked | Patient and Provider |
| REMINDER | 24hr and 1hr before appointment | Patient |
| CANCELLATION | Appointment cancelled | Patient and Provider |
| PAYMENT | Payment processed | Patient |
| FOLLOWUP | Follow up date from medical record | Patient |
| ANNOUNCEMENT | Admin platform broadcast | All users |

---

## Email Configuration

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=yadavvishal9464@gmail.com
spring.mail.password=kifsugzxkqgrjmne
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

## SMS Twilio Swap Design

```java
// CURRENT mock mode
private void sendSms(String phone, String message) {
    System.out.println("SMS MOCK: " + message);
}

// WHEN TWILIO READY uncomment one line in sendSms():
// Twilio.init(accountSid, authToken);
// Message.creator(new PhoneNumber(phone),
//     new PhoneNumber(twilioPhone), message).create();
```

One method change. Nothing else in the entire codebase changes.

---

## API Endpoints

| Method | URL | Who Calls | When |
|--------|-----|-----------|------|
| POST | /notifications/send | Any service / Admin | After key events |
| POST | /notifications/bulk | Admin | Platform announcement |
| GET | /notifications/recipient/{id} | Patient / Provider | Bell icon clicked |
| GET | /notifications/unread/count/{id} | UI on every page | Badge number |
| PUT | /notifications/{id}/read | Patient | Clicks notification |
| PUT | /notifications/read/all/{id} | Patient | Mark all read |
| DELETE | /notifications/{id} | Patient | Delete notification |
| GET | /notifications/all | Admin | Platform log |

---

## Service Connections

```
NotificationServiceImpl
    → JavaMailSender sends real emails via Gmail SMTP
    → UserRepository looks up patient email by recipientId

Called BY other services:
    → AppointmentService (UC4) after booking and cancellation
    → PaymentService (UC5) after payment processed
    → RecordService (UC8) FollowUpReminderScheduler
```

---

## Test Flow (Postman)

```
Step 1 — Send APP notification
POST http://localhost:8080/notifications/send
Authorization: Bearer <token>
Body:
{
  "recipientId": 1,
  "type": "BOOKING",
  "title": "Appointment Confirmed",
  "message": "Your appointment with Dr. Sharma is confirmed for April 10 at 10:00 AM",
  "channel": "APP",
  "relatedId": 1,
  "relatedType": "APPOINTMENT"
}

Step 2 — Send EMAIL notification
Same body but "channel": "EMAIL"
Real email arrives in patient Gmail inbox

Step 3 — Get all notifications
GET http://localhost:8080/notifications/recipient/1

Step 4 — Get unread count
GET http://localhost:8080/notifications/unread/count/1

Step 5 — Mark as read
PUT http://localhost:8080/notifications/1/read

Step 6 — Mark all read
PUT http://localhost:8080/notifications/read/all/1

Step 7 — Bulk send (admin)
POST http://localhost:8080/notifications/bulk
Body:
{
  "recipientIds": [1, 2, 3],
  "title": "Welcome to MediBook",
  "message": "Thank you for joining MediBook platform."
}
```

---

## Test Results

| API | Expected | Status |
|-----|----------|--------|
| POST /notifications/send APP | 201 Created | PASSED |
| POST /notifications/send EMAIL | 201 and email received | PASSED |
| GET /notifications/recipient/1 | 200 OK list | PASSED |
| GET /notifications/unread/count/1 | 200 OK count | PASSED |
| PUT /notifications/1/read | 200 OK | PASSED |
| PUT /notifications/read/all/1 | 200 OK | PASSED |
| DELETE /notifications/1 | 200 OK | PASSED |
| POST /notifications/bulk | 200 OK | PASSED |

---

*MediBook | Capgemini Training Project | Java Spring Boot*
