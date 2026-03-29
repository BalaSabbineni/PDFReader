// ─── App State ────────────────────────────────────────────────────────────────
const state = {
    pdfs: [],
    currentPdf: null,
    loadingText: false,
    authMode: 'login'   // 'login' | 'register'
};

// ─── DOM refs ─────────────────────────────────────────────────────────────────
const bookshelf      = document.getElementById('bookshelf');
const playerBar      = document.getElementById('player-bar');
const playerTitle    = document.getElementById('player-title');
const playerPage     = document.getElementById('player-page');
const playPauseBtn   = document.getElementById('btn-play-pause');
const stopBtn        = document.getElementById('btn-stop');
const prevBtn        = document.getElementById('btn-prev');
const nextBtn        = document.getElementById('btn-next');
const speedSelect    = document.getElementById('speed-select');
const langSelect     = document.getElementById('lang-select');
const progressFill   = document.getElementById('progress-fill');
const uploadModal    = document.getElementById('upload-modal');
const uploadForm     = document.getElementById('upload-form');
const uploadBtn      = document.getElementById('btn-upload-open');
const uploadClose    = document.getElementById('upload-close');
const uploadStatus   = document.getElementById('upload-status');
const toast          = document.getElementById('toast');
const pageText       = document.getElementById('page-text');
const textPanel      = document.getElementById('text-panel');

// Auth elements
const authOverlay    = document.getElementById('auth-overlay');
const authForm       = document.getElementById('auth-form');
const authUsername   = document.getElementById('auth-username');
const authPassword   = document.getElementById('auth-password');
const authSubmit     = document.getElementById('auth-submit');
const authStatus     = document.getElementById('auth-status');
const tabLogin       = document.getElementById('tab-login');
const tabRegister    = document.getElementById('tab-register');
const userBadge      = document.getElementById('user-badge');
const logoutBtn      = document.getElementById('btn-logout');

// ─── Init ─────────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    if (!Player.isSupported()) {
        showToast('Your browser does not support Text-to-Speech', 'error');
    }

    if (Auth.isLoggedIn()) {
        showApp();
    } else {
        showAuthOverlay();
    }
});

// ─── Auth UI ──────────────────────────────────────────────────────────────────
function showAuthOverlay() {
    authOverlay.classList.add('visible');
}

function showApp() {
    authOverlay.classList.remove('visible');
    userBadge.textContent = Auth.getUsername() || '';
    loadBooks();
    bindEvents();
}

function showAuthTab(mode) {
    state.authMode = mode;
    authStatus.textContent = '';
    authStatus.className = 'upload-status';
    authPassword.value = '';

    if (mode === 'login') {
        tabLogin.classList.add('active');
        tabRegister.classList.remove('active');
        authSubmit.textContent = 'Sign In';
    } else {
        tabRegister.classList.add('active');
        tabLogin.classList.remove('active');
        authSubmit.textContent = 'Create Account';
    }
}

tabLogin.addEventListener('click',    () => showAuthTab('login'));
tabRegister.addEventListener('click', () => showAuthTab('register'));

authForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = authUsername.value.trim();
    const password = authPassword.value;

    authSubmit.disabled = true;
    authStatus.textContent = state.authMode === 'login' ? 'Signing in...' : 'Creating account...';
    authStatus.className = 'upload-status info';

    try {
        if (state.authMode === 'login') {
            await Auth.login(username, password);
        } else {
            await Auth.register(username, password);
        }
        showApp();
    } catch (err) {
        authStatus.textContent = err.message;
        authStatus.className = 'upload-status error';
    } finally {
        authSubmit.disabled = false;
    }
});

logoutBtn.addEventListener('click', () => Auth.logout());

// ─── Load books ───────────────────────────────────────────────────────────────
async function loadBooks() {
    bookshelf.innerHTML = '<div class="loading-spinner"></div>';
    try {
        state.pdfs = await API.listPdfs();
        renderBookshelf();
    } catch (e) {
        bookshelf.innerHTML = `<p class="empty-state">Could not connect to backend.<br><small>${e.message}</small></p>`;
    }
}

function renderBookshelf() {
    if (!state.pdfs.length) {
        bookshelf.innerHTML = '<p class="empty-state">No books yet. Upload a PDF to get started!</p>';
        return;
    }
    bookshelf.innerHTML = state.pdfs.map(pdf => `
        <div class="book-card" data-id="${pdf.id}">
            <div class="book-cover">
                <div class="book-icon">&#128366;</div>
                <button class="btn-delete" data-id="${pdf.id}" title="Delete">&#10005;</button>
            </div>
            <div class="book-info">
                <h3 class="book-title">${escHtml(pdf.title)}</h3>
                <span class="book-meta">${pdf.pageCount} pages &bull; ${formatSize(pdf.fileSize)}</span>
            </div>
        </div>
    `).join('');

    bookshelf.querySelectorAll('.book-card').forEach(card => {
        card.addEventListener('click', (e) => {
            if (e.target.closest('.btn-delete')) return;
            const pdf = state.pdfs.find(p => p.id === card.dataset.id);
            if (pdf) openBook(pdf);
        });
    });

    bookshelf.querySelectorAll('.btn-delete').forEach(btn => {
        btn.addEventListener('click', () => confirmDelete(btn.dataset.id));
    });
}

// ─── Open book ────────────────────────────────────────────────────────────────
async function openBook(pdf) {
    if (state.loadingText) return;
    state.loadingText = true;
    state.currentPdf = pdf;

    Player.stop();
    playerBar.classList.add('visible');
    playerTitle.textContent = pdf.title;
    playerPage.textContent = 'Loading...';
    playPauseBtn.disabled = true;
    pageText.textContent = 'Extracting text from PDF...';
    textPanel.classList.add('visible');

    document.querySelectorAll('.book-card').forEach(c => c.classList.remove('active'));
    document.querySelector(`.book-card[data-id="${pdf.id}"]`)?.classList.add('active');

    try {
        const textData = await API.getPdfText(pdf.id);
        Player.load(textData, {
            onPageChange:  (page, total) => updatePageDisplay(page, total),
            onFinish:      () => { playPauseBtn.textContent = '▶'; showToast('Finished!'); },
            onStateChange: (playing) => { playPauseBtn.textContent = playing ? '⏸' : '▶'; },
            onTranslating: (active) => {
                if (active) pageText.textContent = 'Translating to Telugu...';
                else updatePageDisplay(Player.getState().currentPage, Player.getState().totalPages);
            }
        });
        updatePageDisplay(0, textData.pages.length);
        playPauseBtn.disabled = false;
        showToast(`"${pdf.title}" loaded — press play!`);
    } catch (e) {
        showToast('Failed to load PDF text: ' + e.message, 'error');
        pageText.textContent = 'Failed to load text.';
    } finally {
        state.loadingText = false;
    }
}

function updatePageDisplay(page, total) {
    playerPage.textContent = `Page ${page + 1} / ${total}`;
    const pct = total > 0 ? ((page + 1) / total) * 100 : 0;
    progressFill.style.width = pct + '%';
    pageText.textContent = Player.getState().currentText || '';
}

// ─── Player controls ──────────────────────────────────────────────────────────
function bindEvents() {
    playPauseBtn.addEventListener('click', () => {
        const s = Player.getState();
        if (s.isPlaying) Player.pause();
        else if (s.currentPage > 0 || pageText.textContent) Player.resume();
        else Player.play();

        // Edge case: if resume doesn't work (browser quirk), fallback to play
        setTimeout(() => {
            if (!window.speechSynthesis.speaking && !window.speechSynthesis.pending) {
                Player.play();
            }
        }, 100);
    });

    stopBtn.addEventListener('click', () => {
        Player.stop();
        updatePageDisplay(0, Player.getState().totalPages);
    });

    prevBtn.addEventListener('click', () => Player.prevPage());
    nextBtn.addEventListener('click', () => Player.nextPage());

    speedSelect.addEventListener('change', () => {
        Player.setSpeed(parseFloat(speedSelect.value));
    });

    langSelect.addEventListener('change', () => {
        Player.setLanguage(langSelect.value);
        showToast(langSelect.value === 'te' ? 'Telugu mode — text will be translated' : 'English mode');
    });

    // Upload modal
    uploadBtn.addEventListener('click', () => uploadModal.classList.add('visible'));
    uploadClose.addEventListener('click', () => uploadModal.classList.remove('visible'));
    uploadModal.addEventListener('click', (e) => {
        if (e.target === uploadModal) uploadModal.classList.remove('visible');
    });

    uploadForm.addEventListener('submit', handleUpload);
}

// ─── Upload ───────────────────────────────────────────────────────────────────
async function handleUpload(e) {
    e.preventDefault();
    const file  = document.getElementById('file-input').files[0];
    const title = document.getElementById('title-input').value.trim();

    if (!file) return;

    uploadStatus.textContent = 'Uploading...';
    uploadStatus.className = 'upload-status info';

    try {
        const result = await API.uploadPdf(file, title || null);
        uploadStatus.textContent = `"${result.title}" uploaded successfully!`;
        uploadStatus.className = 'upload-status success';
        uploadForm.reset();
        await loadBooks();
        setTimeout(() => {
            uploadModal.classList.remove('visible');
            uploadStatus.textContent = '';
        }, 1500);
    } catch (e) {
        uploadStatus.textContent = 'Upload failed: ' + e.message;
        uploadStatus.className = 'upload-status error';
    }
}

// ─── Delete ───────────────────────────────────────────────────────────────────
async function confirmDelete(id) {
    const pdf = state.pdfs.find(p => p.id === id);
    if (!pdf) return;
    if (!confirm(`Delete "${pdf.title}"?`)) return;

    try {
        await API.deletePdf(id);
        if (state.currentPdf?.id === id) {
            Player.stop();
            playerBar.classList.remove('visible');
            textPanel.classList.remove('visible');
            state.currentPdf = null;
        }
        await loadBooks();
        showToast(`"${pdf.title}" deleted`);
    } catch (e) {
        showToast('Delete failed: ' + e.message, 'error');
    }
}

// ─── Utilities ────────────────────────────────────────────────────────────────
function showToast(msg, type = 'info') {
    toast.textContent = msg;
    toast.className = `toast visible ${type}`;
    setTimeout(() => toast.classList.remove('visible'), 3000);
}

function formatSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function escHtml(str) {
    const d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
}
