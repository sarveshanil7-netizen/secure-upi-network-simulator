# Secure UPI Network Simulator

A Java-based simulation of a UPI-style digital payment system that demonstrates client-server communication, simulated transactions, account management, and QR-code scanning through a web interface.

> **Disclaimer — Academic Demo Only:** This project is intended for educational and demonstration purposes. It does not connect to the real Unified Payments Interface (UPI), banks, NPCI, or a payment gateway. No real money is transferred.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation and Setup](#installation-and-setup)
- [Running the Application](#running-the-application)
- [QR Scanner](#qr-scanner)
- [Troubleshooting](#troubleshooting)
- [Security and Limitations](#security-and-limitations)
- [Future Enhancements](#future-enhancements)
- [Contributors](#contributors)

## Overview

Secure UPI Network Simulator is an academic software project designed to demonstrate how a digital payment application can be structured using a web frontend, a Java backend, network communication, and a relational database.

The application provides a browser-based interface for interacting with simulated payment functionality. It combines frontend technologies with backend networking components and database operations to illustrate the flow of information between users and a server.

The project also includes a QR-scanning interface intended to demonstrate scanning and decoding QR codes in a simulated payment environment.

## Features

- **User Interface:** Web pages for interacting with the application.
- **User Authentication:** Login and registration interfaces, subject to the implemented backend functionality.
- **Account-Based Payments:** Simulated payment functionality using demo account information.
- **Payment History:** A dedicated interface for viewing transaction history, subject to the available backend implementation.
- **Phone-Based Payment Interface:** A page for entering phone-related payment information.
- **QR Code Scanning:** Scan QR codes using a camera or upload a QR-code image.
- **TCP Communication:** Java components for TCP client-server communication.
- **UDP Communication:** Java components for UDP client-server communication.
- **HTTP Backend:** A Java HTTP server for handling supported web requests.
- **Database Integration:** MySQL for storing application data, according to the configured schema.

## Technology Stack

| Component | Technology |
|---|---|
| Frontend | HTML5, CSS3, JavaScript |
| Backend | Java |
| Network communication | TCP, UDP, HTTP |
| Database | MySQL |
| QR scanning | HTML5 QR Code JavaScript library |
| Development tools | Visual Studio Code, Git, GitHub |
| Browser | Chrome, Edge, or another modern browser |

## Architecture

The application is organized into three main layers.

1. **Frontend layer:** Displays the web pages and collects user input.
2. **Backend layer:** Runs Java server components, processes supported requests, and handles network communication.
3. **Database layer:** Stores application data in MySQL, based on the tables and queries implemented in the project.

### High-level architecture

```text
              User / Browser
                    |
                    v
          HTML + CSS + JavaScript
                    |
                    v
             Java HTTP Server
               Port 8080
                    |
          +---------+---------+
          |                   |
          v                   v
     TCP Components      UDP Components
       Port 5000           Port 5002
          |
          v
     MySQL Database
```

The diagram represents the main components and intended communication flow. Actual request routing and database interactions depend on the corresponding Java implementation.

## Project Structure

```text
secure-upi-network-simulator/
|
+-- Backend/
|   +-- UPIApplication.java
|   +-- UPIHttpServer.java
|   +-- TCPClient.java
|   +-- TCPServer.java
|   +-- UDPClient.java
|   +-- UDPServer.java
|   +-- Other Java source files
|   +-- Required libraries, if provided
|
+-- Frontend/
|   +-- homepage.html
|   +-- homepage.js
|   +-- login.html
|   +-- login.js
|   +-- register.html
|   +-- payment.js
|   +-- history.html
|   +-- global-history.html
|   +-- phone-payment.html
|   +-- scan-payment.html
|   +-- styles.css
|   +-- Images and other frontend assets
|
+-- README.md
```

## Prerequisites

Install the following before running the project:

- **Java Development Kit (JDK):** A version compatible with the Java source files.
- **MySQL Server:** For the configured database.
- **Visual Studio Code:** Recommended for editing and running the project.
- **Modern web browser:** Required to use the frontend.
- **Git:** Required to clone the repository.
- **Java dependencies:** Any required JAR files referenced by the backend.

Verify the Java installation:

```powershell
java -version
javac -version
```

Verify that Git is installed:

```powershell
git --version
```

## Installation and Setup

### 1. Clone the repository

Open a terminal and run:

```powershell
git clone https://github.com/sarveshanil7-netizen/secure-upi-network-simulator.git
```

Navigate to the project directory:

```powershell
cd secure-upi-network-simulator
```

### 2. Configure MySQL

Start the MySQL server and open your preferred MySQL client.

Create the application database if it does not already exist:

```sql
CREATE DATABASE IF NOT EXISTS secure_upi_db;
```

Select the database:

```sql
USE secure_upi_db;
```

Next, configure the database connection settings used by the Java backend.

Check the source code for the configured:

- MySQL hostname and port
- Database name
- Username
- Password
- JDBC connection URL
- Required database tables

Create or initialize the tables using the SQL scripts or schema definitions supplied by the project. If the repository does not contain a schema script, the required tables must be created according to the SQL queries in the Java source code.

**Important:** Creating the database alone does not create the tables. The backend will only work correctly when its connection settings and database schema match the implementation.

Do not commit real database passwords or other secrets to GitHub.

### 3. Check backend dependencies

Navigate to the backend directory:

```powershell
cd Backend
```

Check whether the required JAR files are available. If the Java code uses MySQL Connector/J, ensure that the compatible connector JAR is available at the classpath location expected by the project.

If your backend uses a different library path, update the classpath commands below accordingly.

## Running the Application

Run the backend and frontend in separate terminals.

### 1. Start the Java backend

Open a terminal in the `Backend` directory.

If the source files use the default Java package and the required dependencies are in a `lib` folder, compile the Java files using:

```powershell
javac -cp ".;lib/*" *.java
```

Then start the application:

```powershell
java -cp ".;lib/*" UPIApplication
```

If the project uses a different package structure or dependency location, adjust these commands to match the source files.

The backend is expected to start its configured server components. In the current development setup, the expected ports are:

| Component | Port |
|---|---:|
| HTTP server | 8080 |
| TCP server | 5000 |
| UDP server | 5002 |

If startup fails, check the terminal output for compilation errors, missing libraries, database connection errors, or ports that are already in use.

### 2. Start the frontend

Open a **second terminal** and navigate to the `Frontend` directory.

One option is to use Python's local HTTP server:

```powershell
cd Frontend
python -m http.server 5500
```

If the Python command is not available, use the Live Server extension in Visual Studio Code instead.

Open the homepage in your browser:

http://localhost:5500/homepage.html

If the application uses another frontend entry page, use the correct filename from the repository.

### 3. Test the application

After starting the required services:

1. Open the homepage.
2. Navigate through the available login and registration pages.
3. Test the features implemented in the current version.
4. Check the browser console and backend terminal if an operation fails.
5. Test QR scanning separately using a compatible demo QR code.

Use only test data and demo accounts intended for the project.

## QR Scanner

The project includes `Frontend/scan-payment.html`, which provides a QR-scanning interface.

The page supports:

- **Camera scanning:** Scan a compatible QR code using the device camera.
- **Image upload:** Select an image containing a QR code.
- **QR decoding:** Read the encoded text from a supported QR code.
- **Demo account recognition:** Recognize supported demo account identifiers.

### Current status

**Work in progress.** The QR scanner is a demonstration feature within a simulated payment environment. Scanning a QR code does not itself initiate a payment, and the scanner does not connect to a real payment gateway or transfer actual money.

Camera access may require permission from the browser. Use `localhost` or another secure context, grant camera permission, and ensure the QR-scanning JavaScript library can load.

## Troubleshooting

| Problem | What to check |
|---|---|
| `java` or `javac` is not recognized | Install a compatible JDK and configure the system PATH. |
| Compilation fails | Verify the source filenames, package declarations, Java version, and required JAR files. |
| MySQL connection fails | Check that MySQL is running and that the connection URL and credentials match the local configuration. |
| Database or table not found | Confirm the database name and initialize the required tables. |
| Port 8080, 5000, or 5002 is occupied | Identify the process using the port or configure an available port in the relevant server. |
| Homepage does not load | Verify the frontend directory, filename, and local server URL. |
| API requests fail | Check the backend status, endpoint URL, browser console, and network tab. |
| Camera does not open | Check browser permissions, HTTPS/localhost requirements, and camera availability. |
| QR image cannot be decoded | Use a clear image containing a supported QR code. |
| Changes do not appear | Save the file, refresh the browser, and check that you are editing the correct project copy. |

## Security and Limitations

This project is a learning and demonstration application, not a production financial system.

- It does not connect to a real UPI network or payment gateway.
- It does not perform actual bank transfers.
- Demo account details and credentials must not be treated as real financial credentials.
- The security of the application depends on the authentication, validation, and database-handling mechanisms actually implemented.
- QR scanning only demonstrates decoding and handling supported QR content; it does not establish that a payment is genuine.
- A production payment application would require appropriate security testing, secure credential handling, authorization, transaction validation, monitoring, and integration with authorized payment infrastructure.

Do not use this application to store real banking credentials or process real payments.

## Future Enhancements

Potential improvements include:

- Automated database schema initialization.
- Stronger input validation and error handling.
- More comprehensive automated tests.
- Improved transaction validation and audit logging.
- Enhanced QR scanning and demo account verification.
- Better documentation of the backend API.
- Additional UI feedback and accessibility improvements.
- Secure configuration through environment variables.
- Screenshots and a recorded demonstration of the application.

These are proposed enhancements and should not be interpreted as features that are already implemented.

## Contributors

This is a collaborative academic project. Contributors should be credited here with their names and relevant responsibilities.

| Contributor | Responsibility |
|---|---|
| Project team | Development, testing, integration, and documentation |

Update this section with the actual team members and their contributions.

## License

No license is specified in this README. Unless a license file is added to the repository, do not assume that the project is licensed for unrestricted reuse or redistribution.

---

**Secure UPI Network Simulator — An educational project demonstrating simulated digital payments, web development, networking, and database integration.**
