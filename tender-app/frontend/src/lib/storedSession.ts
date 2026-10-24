import type { AuthSession } from '@/types/auth';

const STORAGE_KEY = 'tender.session';

// localStorage only exists on web; on native these are no-ops, so the session lasts until the app closes
function storage(): Storage | null {
  try {
    return typeof localStorage === 'undefined' ? null : localStorage;
  } catch {
    return null;
  }
}

export function loadSession(): AuthSession | null {
  try {
    const raw = storage()?.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthSession) : null;
  } catch {
    return null;
  }
}

export function saveSession(session: AuthSession | null): void {
  try {
    if (session) storage()?.setItem(STORAGE_KEY, JSON.stringify(session));
    else storage()?.removeItem(STORAGE_KEY);
  } catch {
    // Storage can be blocked (e.g. private browsing); the session still works for this page load
  }
}
