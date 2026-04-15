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
- **Seeded staff users (UUID-derived 6-char IDs):**
  - `5ffc65` / `Seed#123A` (Alice Wong)
  - `81ba63` / `Seed#123B` (Brian Tan)
  - `0df6ff` / `Seed#123C` (Chloe Lim)
- **For new staff created by HR:**
  - User ID is auto-generated from UUID (6-char compact token).
  - Password rule: 8-64 chars with uppercase, lowercase, number, and symbol.
