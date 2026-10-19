# Project Development Rules

## General
- Read README.md and docs/ before implementing features.
- Keep frontend and backend in one Git repository.
- Do not modify unrelated modules.
- Never invent API contracts; update docs/api-spec.md first.
- Report changed files and test results.

## Frontend
- React + TypeScript + Vite.
- Place API clients in frontend/src/api/.
- Keep shared types in frontend/src/types/.
- Handle loading, errors, and empty states.

## Backend
- Java 21 and Spring Boot.
- Use Controller -> Service -> Repository layers.
- Validate request DTOs.
- Implement role-based authorization.
- Use database transactions for business operations.

## Testing
- Run frontend build and tests.
- Run backend tests and Maven package.
- Never claim a test passed without executing it.