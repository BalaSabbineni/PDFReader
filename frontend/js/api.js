// ─── API Configuration ────────────────────────────────────────────────────────
// Priority: ?backend= query param → hostname check → ngrok → localhost
function getBaseUrl() {
    const params = new URLSearchParams(window.location.search);
    if (params.get('backend')) return params.get('backend');

    const host = window.location.hostname;
    if (host === 'localhost' || host === '127.0.0.1' || host === '') return 'http://localhost:50000';

    // GitHub Pages or ngrok-hosted frontend → route to backend via ngrok
    return 'https://futile-presumingly-hyacinth.ngrok-free.dev';
}

const BASE_URL = getBaseUrl();
console.log('API base:', BASE_URL);

// Base headers (ngrok bypass + auth token)
function getHeaders() {
    const h = { 'ngrok-skip-browser-warning': 'true' };
    const token = typeof Auth !== 'undefined' ? Auth.getToken() : null;
    if (token) h['Authorization'] = `Bearer ${token}`;
    return h;
}

const API = {
    async listPdfs() {
        const res = await fetch(`${BASE_URL}/api/pdfs`, { headers: getHeaders() });
        if (!res.ok) throw new Error('Failed to fetch PDFs');
        return res.json();
    },

    async uploadPdf(file, title) {
        const formData = new FormData();
        formData.append('file', file);
        if (title) formData.append('title', title);

        const res = await fetch(`${BASE_URL}/api/pdfs/upload`, {
            method: 'POST',
            headers: getHeaders(),
            body: formData
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({ error: 'Upload failed' }));
            throw new Error(err.error || 'Upload failed');
        }
        return res.json();
    },

    async getPdfText(id) {
        const res = await fetch(`${BASE_URL}/api/pdfs/${id}/text`, { headers: getHeaders() });
        if (!res.ok) throw new Error('Failed to extract text');
        return res.json();
    },

    async deletePdf(id) {
        const res = await fetch(`${BASE_URL}/api/pdfs/${id}`, {
            method: 'DELETE',
            headers: getHeaders()
        });
        if (!res.ok) throw new Error('Delete failed');
        return res.json();
    }
};
