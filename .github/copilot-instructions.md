# Copilot instructions for this repository

## Build, run, and test (current project setup)

- **Compile app**
  - `mkdir -p build/classes`
  - `javac -cp "lib/*" -d build/classes $(find src -name "*.java")`
- **Run Derby DB server**
  - `java -cp "lib/*" org.apache.derby.drda.NetworkServerControl start -h 127.0.0.1 -p 1527`
- **Run RMI server**
  - `java -cp "build/classes:lib/*" Server.ServerSocket`
- **Run GUI client**
  - `java -cp "build/classes:lib/*" Client.ClientMain`
- **Validate app flow**
  - Start DB + server + GUI and validate HR/staff actions directly from the UI.

## Windows setup (Derby download + configuration)

1. Install **JDK 17+** and set `JAVA_HOME`.
2. Download Apache Derby binary distribution from the official Apache Derby site.
3. Copy these jars into the project `lib/` folder:
   - `derby.jar`
   - `derbyclient.jar`
   - `derbyshared.jar`
   - `derbynet.jar`
   - `derbytools.jar`
4. Open terminal (PowerShell/CMD) in project root and run:
   - `java -cp "lib/*" org.apache.derby.drda.NetworkServerControl start -h 127.0.0.1 -p 1527`
5. Start server and GUI with the run commands above.

## Multi-computer run instructions

### Host machine (server + DB)

1. Start Derby and RMI server on the host.
2. Use host IP in server startup:
   - `java -Dcrest.server.host=0.0.0.0 -cp "build/classes:lib/*" Server.ServerSocket`
3. Open firewall ports:
   - Derby: `1527`
   - RMI registry/service: `1099`
4. For SSL across machines, server certificate must include host DNS/IP in SAN.

### Client machine

1. Place trusted server cert/keystore locally.
2. Start client with host + truststore properties:
   - `java -Dcrest.server.host=<SERVER_IP> -Dcrest.truststore.path=<PATH_TO_TRUSTSTORE> -Dcrest.truststore.password=<PASSWORD> -cp "build/classes:lib/*" Client.ClientMain`

## Default login credentials

- **HR**
  - User ID: `H-000001`
  - Password: `hr123`
- **Staff (demo account)**
  - User ID: `E-000001`
  - Password: `staff123`
- **New staff**
  - Created by HR from GUI.
  - New IDs are UUID-based tokens.
  - Initial password must contain uppercase, lowercase, number, symbol (8-64 chars).

## Architecture and conventions

- `Common`: shared contracts/models (`Authorization`, `Employee`, `UserSession`, `UserRole`).
- `Server`: RMI bootstrap, service logic, repositories, DB init, audit.
- `Client`: Swing GUI app (`CrestGuiApp`) and client entry (`ClientMain`).
- Service-layer role gates are authoritative:
  - HR: register/list/read/update/delete employees, pending leave decisions, reports.
  - STAFF: profile updates, leave apply/view/history.
- Keep validation centralized in `ServiceImpl` `require*` helpers.
- Repository classes stay table-focused (`UsersRepository`, `EmployeesRepository`, `EmployeeDetailsRepository`, `LeaveBalanceRepository`, `LeaveApplicationsRepository`).
