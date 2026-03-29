// ─── Auth Module ──────────────────────────────────────────────────────────────
// Depends on BASE_URL from api.js (loaded before this file)

const TOKEN_KEY    = 'pdf_token';
const USERNAME_KEY = 'pdf_username';

const Auth = {
    getToken()    { return localStorage.getItem(TOKEN_KEY); },
    getUsername() { return localStorage.getItem(USERNAME_KEY); },
    isLoggedIn()  { return !!this.getToken(); },

    _setSession(token, username) {
        localStorage.setItem(TOKEN_KEY, token);
        localStorage.setItem(USERNAME_KEY, username);
    },

    clearSession() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USERNAME_KEY);
    },

    async login(username, password) {
        const res = await fetch(`${BASE_URL}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'ngrok-skip-browser-warning': 'true' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Login failed');
        this._setSession(data.token, data.username);
        return data;
    },

    async register(username, password) {
        const res = await fetch(`${BASE_URL}/api/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'ngrok-skip-browser-warning': 'true' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Registration failed');
        this._setSession(data.token, data.username);
        return data;
    },

    logout() {
        this.clearSession();
        location.reload();
    }
};
