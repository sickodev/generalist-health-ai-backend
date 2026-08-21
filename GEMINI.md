# Backend Project Rules & Workflow Guidelines

Refer to [AGENTS.md](./AGENTS.md) for full project rules, branching strategies, commit conventions, and development protocols.

## Key Directives

- **Rules Reference**: Strictly adhere to all guidelines and workflow requirements defined in [AGENTS.md](./AGENTS.md).
- **Branch Naming**: `feature/task-F<number>` *(e.g., `feature/task-F1`, `feature/task-F8`)*
- **Commit Message Format**: `F<number>:<message changes>` *(e.g., `F1:install dependencies`)*
- **Execution Protocol**: Only write code or modify files when specifically instructed by the user.
- **Verification**: Run `mvn clean test` or `mvn test-compile` to verify compilation and test integrity before committing.
