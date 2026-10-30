# Command and error logging

`CommandLoggingFilter` records API requests and `/health` in the backend terminal.
It runs before Spring Security so requests rejected with 401 or 403 are included.
`TokenAuthenticationFilter` supplies the verified account ID and role as internal
request attributes before the security context is cleared.

Example format:

```text
command request=<uuid> method=GET path=/api/operator/orders actor=8 role=OPERATOR status=200 durationMs=12 outcome=completed error=none
```

Each entry includes a generated request ID, HTTP method, path, account ID/role,
HTTP status, elapsed milliseconds, and outcome. Login/registration normally show
an anonymous actor because they begin without an authenticated bearer session.
Subsequent authenticated requests show the verified account ID and role.

- Successful requests and redirects use INFO.
- HTTP 4xx outcomes use WARN, including validation, missing records, and access denials.
- HTTP 5xx outcomes and unexpected exceptions use ERROR. Exceptions are rethrown;
  logging does not turn failures into successful responses. If response headers
  were already committed, the logged status remains the one sent to the client;
  the exception outcome and ERROR level still identify the failure.
- Streaming requests log when started, without waiting for the connection to close.
  Async errors, timeouts, and error statuses at completion are also recorded.
  A stream error may occur after HTTP 200 headers were already sent; its error
  outcome and severity distinguish it from a successful stream.
- Image assets and OPTIONS preflight requests are excluded from command logging.

The logger does not inspect request/response bodies, headers, query parameters,
passwords, or session tokens. Unexpected exception entries record the exception
class rather than its message or stack trace. Paths are bounded and control
characters/whitespace are replaced to keep one event per log line.

Existing domain logs remain: database health checks, mock payment approvals,
permission errors, and committed order state-transition events. A command request
ID is a correlation identifier for HTTP activity; it does not replace the order
transition event IDs or the persisted `order_events` records.

## Manual verification

Start the latest backend normally, without the demo profile, and use an existing
operator account in the frontend. Do not run the seed for these checks.

1. Log in and open each dashboard tab. Observe GET command entries with the
   operator identity and successful statuses.
2. Refresh or load another page. Observe another command entry with no credentials.
3. In a database safe for manual testing, pause and reopen a restaurant. Expect
   PUT command entries with status 204 and the dashboard refreshing after success.
4. Sign out or use a different role when checking protected operator endpoints.
   Expect 401/403 entries; no business operation should execute.
5. Check invalid inputs/missing IDs through an API client if desired. Expect the
   existing validation/not-found response and a corresponding command entry.

API/security tests use mock requests; backend integration tests use H2. Logging
implementation and automated verification do not require modifying Supabase or
executing the demo seed. These checks do not replace a live UI rehearsal.
