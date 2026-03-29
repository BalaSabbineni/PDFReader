// ─── Audio Player (Web Speech API) ───────────────────────────────────────────

const Player = (() => {
    let utterance    = null;
    let pages        = [];
    let currentPage  = 0;
    let isPlaying    = false;
    let playbackRate = 1.0;
    let language     = 'en';            // 'en' | 'te'
    let transCache   = {};              // pageIndex → translated text

    let onPageChange  = null;
    let onFinish      = null;
    let onStateChange = null;
    let onTranslating = null;           // (bool) → called while fetching translation

    // ── Translation (Google Translate unofficial endpoint) ──────────────────
    async function translateToTelugu(text) {
        try {
            const url = 'https://translate.googleapis.com/translate_a/single?' +
                new URLSearchParams({ client: 'gtx', sl: 'en', tl: 'te', dt: 't', q: text });
            const res  = await fetch(url);
            const data = await res.json();
            // data[0] is an array of [translatedSegment, original, ...] pairs
            return data[0].map(seg => seg[0]).filter(Boolean).join('');
        } catch (e) {
            console.warn('Translation failed, using original text:', e);
            return text;
        }
    }

    // ── Core speak ──────────────────────────────────────────────────────────
    function speak(text, lang) {
        window.speechSynthesis.cancel();
        utterance      = new SpeechSynthesisUtterance(text);
        utterance.rate  = playbackRate;
        utterance.pitch = 1;
        utterance.lang  = lang || 'en-US';

        utterance.onend = () => {
            if (currentPage < pages.length - 1) {
                currentPage++;
                if (onPageChange) onPageChange(currentPage, pages.length);
                speakCurrentPage();
            } else {
                isPlaying = false;
                if (onFinish) onFinish();
                if (onStateChange) onStateChange(false);
            }
        };

        utterance.onerror = (e) => {
            if (e.error !== 'interrupted') {
                console.error('Speech error:', e.error);
                isPlaying = false;
                if (onStateChange) onStateChange(false);
            }
        };

        window.speechSynthesis.speak(utterance);
    }

    // ── Speak current page (with optional translation) ──────────────────────
    async function speakCurrentPage() {
        if (!pages.length) return;

        let text = pages[currentPage].text;

        if (language === 'te') {
            if (transCache[currentPage] !== undefined) {
                text = transCache[currentPage];
            } else {
                if (onTranslating) onTranslating(true);
                text = await translateToTelugu(text);
                transCache[currentPage] = text;
                if (onTranslating) onTranslating(false);
            }
        }

        speak(text, language === 'te' ? 'te-IN' : 'en-US');
    }

    return {
        load(pdfTextData, callbacks = {}) {
            window.speechSynthesis.cancel();
            pages        = pdfTextData.pages || [];
            currentPage  = 0;
            isPlaying    = false;
            transCache   = {};
            onPageChange  = callbacks.onPageChange  || null;
            onFinish      = callbacks.onFinish      || null;
            onStateChange = callbacks.onStateChange || null;
            onTranslating = callbacks.onTranslating || null;
        },

        play() {
            if (!pages.length) return;
            isPlaying = true;
            if (onStateChange) onStateChange(true);
            speakCurrentPage();
        },

        pause() {
            isPlaying = false;
            window.speechSynthesis.pause();
            if (onStateChange) onStateChange(false);
        },

        resume() {
            isPlaying = true;
            window.speechSynthesis.resume();
            if (onStateChange) onStateChange(true);
        },

        stop() {
            isPlaying = false;
            window.speechSynthesis.cancel();
            currentPage = 0;
            if (onPageChange) onPageChange(0, pages.length);
            if (onStateChange) onStateChange(false);
        },

        nextPage() {
            if (currentPage < pages.length - 1) {
                window.speechSynthesis.cancel();
                currentPage++;
                if (onPageChange) onPageChange(currentPage, pages.length);
                if (isPlaying) speakCurrentPage();
            }
        },

        prevPage() {
            if (currentPage > 0) {
                window.speechSynthesis.cancel();
                currentPage--;
                if (onPageChange) onPageChange(currentPage, pages.length);
                if (isPlaying) speakCurrentPage();
            }
        },

        goToPage(index) {
            if (index >= 0 && index < pages.length) {
                window.speechSynthesis.cancel();
                currentPage = index;
                if (onPageChange) onPageChange(currentPage, pages.length);
                if (isPlaying) speakCurrentPage();
            }
        },

        setSpeed(rate) {
            playbackRate = rate;
            if (isPlaying) {
                window.speechSynthesis.cancel();
                speakCurrentPage();
            }
        },

        setLanguage(lang) {
            language   = lang;
            transCache = {};            // clear cache on language change
            if (isPlaying) {
                window.speechSynthesis.cancel();
                speakCurrentPage();
            }
        },

        getState() {
            // Return translated text if cached, original otherwise
            const text = language === 'te'
                ? (transCache[currentPage] ?? pages[currentPage]?.text ?? '')
                : (pages[currentPage]?.text ?? '');
            return {
                isPlaying,
                currentPage,
                totalPages: pages.length,
                playbackRate,
                language,
                currentText: text
            };
        },

        isSupported() {
            return 'speechSynthesis' in window;
        }
    };
})();
