# JavaFX Vehicle Service System

This project is a **desktop Vehicle Service Management System** developed using **JavaFX**.  
It is designed to manage vehicles, customers, drivers, maintenance records, and assignments in a structured and modular way.

## ✨ Features
- Vehicle, customer, driver, and maintenance management
- Assignment tracking
- Authentication and session handling
- JavaFX UI with FXML
- Dark / light theme support
- MySQL database integration

## 🧱 Architecture
The project follows a layered and modular architecture:
- `model` – Domain/entity classes
- `dao` – Data Access Object (DAO) layer
- `db` / `config` – Database and configuration handling
- `ui` – JavaFX UI, controllers, navigation, and themes

## 🔧 Technologies Used
- Java
- JavaFX
- FXML
- MySQL
- JDBC
- Maven

## 🗄 Database
The application uses a **MySQL** database.  
The SQL script required for database setup is provided in the repository.

To run the project:
1. Create a MySQL database
2. Execute the provided SQL script
3. Update database credentials in the configuration files

## 📌 Notes
This project was developed as a **personal and educational project** to practice:
- Object-Oriented Programming (OOP)
- JavaFX application development
- Database-driven software design
- Clean and maintainable architecture
