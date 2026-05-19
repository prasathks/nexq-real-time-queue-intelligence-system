/**
 * NexQ — Shared JavaScript Utilities
 * API helpers, auth guards, formatting utilities
 */

const API_BASE = '';

// ── API Helpers ──────────────────────────────────────────────────────────────

function getAuthHeaders() {
    const token = localStorage.getItem('nexq_token');
    return {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': 'Bearer ' + token } : {})
    };
}

async function handleResponse(res) {
    const text = await res.text();
    let data;
    try { data = JSON.parse(text); } catch { data = { message: text }; }
    if (!res.ok) {
        const msg = data.message || data.error || `Error ${res.status}`;
        throw new Error(msg);
    }
    return data;
}

async function apiGet(url) {
    const res = await fetch(API_BASE + url, { headers: getAuthHeaders() });
    return handleResponse(res);
}

async function apiPost(url, body) {
    const res = await fetch(API_BASE + url, {
        method: 'POST', headers: getAuthHeaders(), body: JSON.stringify(body)
    });
    return handleResponse(res);
}

async function apiPut(url, body) {
    const res = await fetch(API_BASE + url, {
        method: 'PUT', headers: getAuthHeaders(), body: JSON.stringify(body)
    });
    return handleResponse(res);
}

async function apiDelete(url) {
    const res = await fetch(API_BASE + url, { method: 'DELETE', headers: getAuthHeaders() });
    if (res.status === 204) return null;
    return handleResponse(res);
}

// ── Auth Guards ──────────────────────────────────────────────────────────────

function requireAuth(requiredRole) {
    const token = localStorage.getItem('nexq_token');
    const role = localStorage.getItem('nexq_role');
    if (!token) { window.location.href = '/index.html'; return; }

    const roleHierarchy = { USER: 1, STAFF: 2, ADMIN: 3 };
    const userLevel = roleHierarchy[role] || 0;
    const requiredLevel = roleHierarchy[requiredRole] || 0;

    if (userLevel < requiredLevel) {
        alert('Access denied: insufficient permissions');
        window.location.href = '/index.html';
    }
}

function logout() {
    localStorage.removeItem('nexq_token');
    localStorage.removeItem('nexq_role');
    localStorage.removeItem('nexq_user');
    window.location.href = '/index.html';
}

// ── UI Utilities ─────────────────────────────────────────────────────────────

function showAlert(message, type = 'info') {
    const el = document.getElementById('authAlert');
    if (!el) return;
    el.className = `alert alert-${type}`;
    el.textContent = message;
    el.classList.remove('d-none');
}

function hideAlert() {
    const el = document.getElementById('authAlert');
    if (el) el.classList.add('d-none');
}

function showAlertIn(elementId, message, type = 'info') {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.className = `alert alert-${type}`;
    el.textContent = message;
    el.classList.remove('d-none');
    setTimeout(() => el.classList.add('d-none'), 5000);
}

function setLoading(btnId, spinnerId, loading) {
    const btn = document.getElementById(btnId);
    const spinner = document.getElementById(spinnerId);
    if (!btn || !spinner) return;
    btn.disabled = loading;
    spinner.classList.toggle('d-none', !loading);
    const text = btn.querySelector('.btn-text');
    if (text) text.style.opacity = loading ? '0.5' : '1';
}

// ── Formatting ────────────────────────────────────────────────────────────────

function formatDate(dateStr) {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleString('en-IN', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit', hour12: true
    });
}

function escHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function timeAgo(dateStr) {
    if (!dateStr) return '';
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'Just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    return `${Math.floor(hrs / 24)}d ago`;
}
