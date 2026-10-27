/** Base URL of the Spring backend. On a phone or emulator, use your computer's LAN IP, not localhost. */
export const API_URL = (process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080').replace(/\/+$/, '');

/** Shape of the backend's ErrorResponse. */
type ApiErrorBody = {
  message?: string;
  fieldErrors?: Record<string, string>;
};

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

export async function postJson<T>(path: string, body: unknown, token?: string): Promise<T> {
  return requestJson<T>('POST', path, body, token);
}

export async function requestJson<T>(method: 'GET' | 'PATCH' | 'POST', path: string, body?: unknown, token?: string): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, `Cannot reach the server at ${API_URL}`);
  }

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const error = (data ?? {}) as ApiErrorBody;
    const fieldErrors = error.fieldErrors ?? {};
    // Validation failures carry the useful text in fieldErrors, not message
    const message = Object.values(fieldErrors)[0] ?? error.message ?? `Request failed (${response.status})`;
    throw new ApiError(response.status, message, fieldErrors);
  }
  return data as T;
}
