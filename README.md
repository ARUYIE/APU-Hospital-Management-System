# APU Hospital Management System (APU_HMS)

A desktop Hospital Management System built with **Java Swing**, developed as a NetBeans/Maven project. It supports multiple user roles (Admin Staff, Doctor, Medical Manager, Patient) and manages appointments, billing, hospital assets, records, and more — all backed by flat-file (text) data storage.

## Features

- **Role-based access** — separate flows for Admin Staff, Doctors, Medical Managers, and Patients
- **Appointments** — booking, time slot management, and scheduling
- **Patient records** — consultations, assessments, lab requests, prescriptions, vital signs
- **Billing & insurance** — bill generation, insurance network tracking
- **Hospital asset management** — track rooms, wards, labs, and imaging equipment; allocate/book assets with conflict detection; request/approval workflow (see [Asset Management](#hospital-asset-management) below)
- **Staff management** — departments, shift rosters, doctor–manager assignments
- **Reports & charts** — visual reporting on hospital data
- **Reviews** — comments and ratings

## Project Structure

```
APU_HMS/
├── src/hms/
│   ├── Main.java              # Application entry point
│   ├── gui/                   # Swing UI (LoginFrame, DashboardFrame, panels/)
│   ├── role/                  # User & role classes (Doctor, Patient, AdminStaff, MedicalManager, etc.)
│   └── util/                  # Business logic & managers (Billing, Assets, Departments, Records, Reports, etc.)
├── data/                      # Flat-file data storage (.txt) — users, bills, appointments, assets, etc.
├── pom.xml                    # Maven build configuration
└── nbproject/                 # NetBeans project files
```

## Requirements

- **Java 23** (as configured in `pom.xml`) — adjust `maven.compiler.source`/`target` if you need to target an older JDK
- **Maven** for building, or **NetBeans** (project files are included)
- Dependency: [`jdatepicker`](https://github.com/JDatePicker/JDatePicker) `1.3.4`

## Getting Started
### Run from NetBeans
#### Git Clone

```bash
git clone https://github.com/ARUYIE/APU-Hospital-Management-System.git
```

#### Build with Maven
```bash
cd APU_HMS
mvn clean compile
```

#### Package into a runnable JAR

```bash
mvn clean package
java -jar target/APU_HMS-1.0.0.jar
```

### Run from NetBeans
#### Download Zip
Open the `APU_HMS` folder as an existing project in NetBeans and run it directly.

On launch, the app opens a login screen (`LoginFrame`) and routes users into the dashboard based on their role.
## Git Clone
Open Netbeans > Team > Git > Clone > Put the Repository Url > Finish

#### Data Storage

The system persists data as pipe-delimited (`|`) text files in the `data/` folder rather than a database, for example:

| File | Purpose |
|------|---------|
| `users.txt` | User accounts |
| `bookings.txt` / `-appointments.txt` | Appointments |
| `bills.txt` | Billing |
| `insurance_networks.txt` | Insurance networks |
| `prescriptions.txt` | Prescriptions |
| `lab_requests.txt` | Lab requests |
| `vital_signs.txt` | Patient vital signs |
| `department.txt` | Departments |
| `roster.txt` / `shift_rosters.txt` / `shift_time.txt` | Staff scheduling |
| `doctor_manager_assignments.txt` | Doctor–manager assignments |
| `reviews.txt` | Comments/ratings |
| `-audit_logs.txt` | Audit trail |
| `hospital_assets.txt` | Asset inventory |
| `asset_allocations.txt` | Asset bookings/usage |
| `asset_requests.txt` | Formal asset requests |

These files are created and managed automatically by the relevant manager classes in `src/hms/util/` (e.g. `UserRepository`, `BillingManager`, `AssetManager`).

## Development Notes

- `Tables.java` and `UserRepository.java` contain shared/reusable helper functions.
- When adding role-specific functionality, work within the corresponding class under `src/hms/role/`.

---

## Hospital Asset Management

The asset management module lets the hospital track and allocate physical assets — consultation rooms, inpatient wards, labs, imaging rooms, operation theatres, pharmacies, and equipment.

### Core Components

| Class | Location | Purpose |
|---|---|---|
| `AssetType` (enum) | `src/hms/util/` | `CONSULTATION_ROOM`, `INPATIENT_WARD`, `LAB`, `IMAGING_ROOM`, `OPERATION_THEATRE`, `PHARMACY`, `EQUIPMENT` |
| `Asset` (model) | `src/hms/util/` | A physical asset: id, type, name, location, capacity, status (`AVAILABLE`/`OCCUPIED`/`MAINTENANCE`/`OUT_OF_SERVICE`), department, description, created date |
| `AssetAllocation` (model) | `src/hms/util/` | A booking of an asset: id, asset id, user id, department, purpose, start/end time, status (`ALLOCATED`/`COMPLETED`/`CANCELLED`), notes |
| `AssetRequest` (model) | `src/hms/util/` | A formal request for an asset: id, asset type needed, user, department, purpose, priority (`LOW`/`MEDIUM`/`HIGH`), status (`PENDING`/`APPROVED`/`REJECTED`/`FULFILLED`), notes |
| `AssetManager` | `src/hms/util/` | CRUD + queries for assets |
| `AssetAllocationManager` | `src/hms/util/` | Bookings, conflict detection, availability queries |
| `AssetRequestManager` | `src/hms/util/` | Requests and approval workflow |
| `ManageAssetsPanel` | `src/hms/gui/panels/` | Admin UI for managing asset inventory |
| `RequestAssetPanel` | `src/hms/gui/panels/` | User UI for requesting/allocating assets |


