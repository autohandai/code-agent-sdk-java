# Security Policy

Please do not report security vulnerabilities through public GitHub issues.

Email security-sensitive reports to [security@autohand.ai](mailto:security@autohand.ai).
If that address is unavailable, contact [hey@autohand.ai](mailto:hey@autohand.ai)
and include `Security` in the subject.

## Scope

Security reports may include:

- Unsafe handling of API keys, environment variables, or credentials.
- CLI subprocess escaping or argument handling issues.
- Permission-response behavior that could approve tools unexpectedly.
- Vulnerabilities in examples that could mislead users into unsafe defaults.
- Dependency vulnerabilities with a practical exploit path for SDK users.

## Safe Reporting

Include:

- A clear description of the issue.
- Reproduction steps or proof of concept when safe to share.
- Affected versions or commit SHAs.
- Any known mitigation.

Avoid including real secrets, customer data, or exploit details that are not
needed to understand the issue.
