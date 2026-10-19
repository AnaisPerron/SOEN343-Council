import React, { createContext, useContext, useEffect, useState } from 'react';
import * as SecureStore from 'expo-secure-store';
 

export const API_URL = 'http://192.168.0.10:8080';
const USE_MOCK = true;
 
const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);
 
//test users
const MOCK_USERS = {
  'user@tender.com':    { password: 'user',    role: 'user',    name: 'Test User' },
  'courier@tender.com': { password: 'courier', role: 'courier', name: 'Test Courier' },
  'admin@tender.com':   { password: 'admin',   role: 'admin',   name: 'Test Admin' },
};
 
async function loginRequest(email, password, role) {
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 600));
    const acct = MOCK_USERS[email.toLowerCase()];
    if (!acct || acct.password !== password) throw new Error('Invalid email or password.');
    if (acct.role !== role) throw new Error(`This account is not a ${role} account.`);
    return { token: 'mock-token-' + role, user: { email, role: acct.role, name: acct.name } };
  }
  const res = await fetch(`${API_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, role }),
  });
  if (!res.ok) throw new Error(res.status === 401 ? 'Invalid email or password.' : 'Server error.');
  return res.json(); 
}
 
export function AuthProvider({ children }) {
  const [session, setSession] = useState(null); 
  const [loading, setLoading] = useState(true);
 
  useEffect(() => {
    (async () => {
      try {
        const saved = await SecureStore.getItemAsync('session');
        if (saved) setSession(JSON.parse(saved));
      } catch {}
      setLoading(false);
    })();
  }, []);
 
  const login = async (email, password, role) => {
    const data = await loginRequest(email, password, role);
    setSession(data);
    await SecureStore.setItemAsync('session', JSON.stringify(data));
  };
 
  const logout = async () => {
    setSession(null);
    await SecureStore.deleteItemAsync('session');
  };
 
  return (
    <AuthContext.Provider value={{ session, user: session?.user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}