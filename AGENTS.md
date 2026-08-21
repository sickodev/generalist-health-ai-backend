# Backend Agent Guidelines & Workflow Rules

## Overview
This repository houses the Backend RCM (Revenue Cycle Management) Service for Generalist Health AI, built with Spring Boot, Java 17, PostgreSQL, Redis, and Maven.

---

## Git Branching & Commit Conventions

For every task executed on this repository:

1. **Branch Naming**:
   - Before coding or making changes for any task, switch to or create a dedicated feature branch named:
     `feature/task-F<number>`
     *(e.g., `feature/task-F1`, `feature/task-F8`, etc.)*

2. **Commit Message Format**:
   - Every commit must follow the strict format:
     `F<number>:<message changes>`
     *(e.g., `F1:configure postgresql and security dependencies`, `F2:implement claims rest endpoints`)*

---

## Execution & Safety Protocol

1. **Explicit Instruction**:
   - Do not write code or modify files without explicit user instruction.
2. **Build & Test Verification**:
   - Always verify build and test integrity (e.g., `mvn clean test` or `mvn test-compile`) before finalizing any commit.
3. **Clean Architecture**:
   - Maintain separation of concerns across controller, service, repository, and DTO layers.
