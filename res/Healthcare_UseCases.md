# Healthcare Patient Engagement – Care Coordination Platform
## Fully Dressed Use Cases

---

## UC-01: Patient Registration & Profile Management

### Primary Actor
- Patient

### Supporting Actors
- Identity Provider (Azure AD)
- Notification Service

### Description
Allows a patient to register, create, and manage a secure profile in the system.

### Preconditions
- Patient is not already registered
- System is available

### Postconditions
- Patient profile is created
- Confirmation notification is sent

### Main Flow
1. Patient accesses registration page
2. Patient enters personal details (name, contact, demographics)
3. System validates mandatory fields
4. System invokes Identity Service for authentication setup
5. System creates patient profile in database
6. System generates confirmation response
7. Notification Service sends confirmation (SMS/Email)

### Alternate Flows
- A1: Invalid data → System returns validation error
- A2: Duplicate registration → System rejects request

### Business Rules
- Mandatory fields must be validated
- Data encryption must be applied for PHI

---

## UC-02: Consent Management

### Primary Actor
- Patient

### Supporting Actors
- Consent Service

### Description
Captures and manages patient consent with versioning.

### Preconditions
- Patient is registered

### Postconditions
- Consent record stored with version history

### Main Flow
1. Patient accesses consent form
2. Patient reviews terms
3. Patient provides consent
4. System stores consent with version number
5. System logs consent event

### Alternate Flows
- A1: Consent denied → System restricts service access

### Business Rules
- Consent must be version-controlled
- Consent required before accessing services

---

## UC-03: Appointment Scheduling

### Primary Actor
- Patient

### Supporting Actors
- Appointment Service
- Notification Service

### Description
Allows patients to schedule appointments with healthcare providers.

### Preconditions
- Patient authenticated
- Doctor availability exists

### Postconditions
- Appointment created
- Confirmation sent

### Main Flow
1. Patient views available slots
2. Patient selects slot
3. System validates availability
4. Appointment is booked
5. Event published: AppointmentBookedEvent
6. Notification sent to patient

### Alternate Flows
- A1: Slot already booked → System prompts re-selection

### Business Rules
- No double booking allowed
- Appointment must trigger notification

---

## UC-04: Care Plan Management

### Primary Actor
- Care Coordinator

### Supporting Actors
- Care Plan Service
- Notification Service

### Description
Manages lifecycle of patient care plans.

### Preconditions
- Patient exists
- Care coordinator authenticated

### Postconditions
- Care plan created/updated
- Tasks assigned

### Main Flow
1. Care coordinator creates care plan
2. System associates plan with patient
3. Tasks and activities defined
4. System stores plan
5. Event published: CarePlanCreatedEvent
6. Notifications triggered

### Alternate Flows
- A1: Invalid plan data → Validation error

### Business Rules
- Care plans must follow defined protocol
- Versioning must be maintained

---

## UC-05: Teleconsultation

### Primary Actor
- Doctor

### Supporting Actors
- Telemedicine Service
- Medical Record Service

### Description
Enables virtual consultation between doctor and patient.

### Preconditions
- Appointment scheduled

### Postconditions
- Consultation completed
- Notes recorded

### Main Flow
1. Doctor initiates teleconsult session
2. Patient joins session
3. Communication established via ACS
4. Doctor records consultation notes
5. System updates EHR

### Alternate Flows
- A1: Network failure → Retry connection

### Business Rules
- Sessions must be secure (encrypted)
- All interactions logged

---

## UC-06: Medical Record Management (FHIR)

### Primary Actor
- Doctor

### Supporting Actors
- FHIR Service

### Description
Manages patient clinical records using FHIR standard.

### Preconditions
- Patient exists

### Postconditions
- Medical records updated

### Main Flow
1. Doctor retrieves patient record
2. Updates clinical information
3. System transforms data to FHIR format
4. Stores in FHIR repository

### Business Rules
- FHIR compliance mandatory
- Record versioning required

---

## UC-07: Notification & Communication

### Primary Actor
- System

### Supporting Actors
- Notification Service (ACS)

### Description
Sends alerts and updates via multiple channels.

### Preconditions
- Event triggered

### Postconditions
- Notification delivered

### Main Flow
1. Event generated (e.g., appointment booked)
2. Notification Service receives event
3. Message formatted
4. Sent via SMS/Email/Push

### Alternate Flows
- A1: Delivery failure → Retry

### Business Rules
- Multi-channel support required
- Failure handling mandatory

---

## UC-08: IoT Device Data Ingestion

### Primary Actor
- IoT Device

### Supporting Actors
- IoT Hub
- Telemetry Service

### Description
Captures patient vitals via connected devices.

### Preconditions
- Device registered

### Postconditions
- Telemetry stored and processed

### Main Flow
1. Device sends telemetry (BP, HR, glucose)
2. IoT Hub receives data
3. Data forwarded to ingestion service
4. Telemetry processed
5. Event generated

### Alternate Flows
- A1: Invalid data → Discard

### Business Rules
- Data must be validated
- Secure communication required

---

## UC-09: Alert Management

### Primary Actor
- System

### Supporting Actors
- Alert Service
- Notification Service

### Description
Generates alerts based on abnormal readings.

### Preconditions
- Telemetry processed

### Postconditions
- Alert generated
- Notification sent

### Main Flow
1. Telemetry event evaluated
2. Rule engine checks thresholds
3. Alert generated if abnormal
4. Notification sent

### Alternate Flows
- A1: Threshold not breached → No alert

### Business Rules
- Rules must be configurable
- Alerts require escalation handling

---

## UC-10: Audit & Monitoring

### Primary Actor
- System

### Supporting Actors
- Monitoring Service
- Logging Service

### Description
Tracks system and user activities.

### Preconditions
- User/system activity occurs

### Postconditions
- Logs stored
- Metrics available

### Main Flow
1. Action performed
2. System logs event with metadata
3. Logs sent to monitoring platform
4. Alerts triggered on anomalies

### Business Rules
- Audit logs immutable
- Must track all critical operations
