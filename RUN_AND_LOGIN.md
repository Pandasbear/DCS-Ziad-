# Run Guide and Login Details

## 1. Compile

```bash
mkdir -p build/classes
javac -cp "lib/*" -d build/classes $(find src -name "*.java")
```

## 2. Start Derby DB server

```bash
java -cp "lib/*" org.apache.derby.drda.NetworkServerControl start -h 127.0.0.1 -p 1527
```

## 3. Start RMI server

```bash
java -cp "build/classes:lib/*" Server.ServerSocket
```

## 4. Start GUI client

```bash
java -cp "build/classes:lib/*" Client.ClientMain
```

## 5. Validate connectivity manually

```bash
java -cp "build/classes:lib/*" Client.ClientMain
```

## Login details

- **HR (default):**
  - User ID: `H-000001`
  - Password: `hr123`
- **Staff users:**
  - Created by HR in the GUI.
  - User ID is auto-generated (UUID-based token).
  - Password is the initial password set during registration.
  - Password rule: 8-64 chars with uppercase, lowercase, number, and symbol.
