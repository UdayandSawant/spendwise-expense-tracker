'use strict';

// ---------- settings ----------
const CATEGORIES = {
  Food:          { icon: '🍔', color: '#f97316' },
  Travel:        { icon: '✈️', color: '#3b82f6' },
  Shopping:      { icon: '🛍️', color: '#ec4899' },
  Bills:         { icon: '💡', color: '#eab308' },
  Entertainment: { icon: '🎬', color: '#8b5cf6' },
  Health:        { icon: '💊', color: '#10b981' },
  Other:         { icon: '📦', color: '#64748b' }
};

const money = new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: 'INR', maximumFractionDigits: 2
});

const $ = (id) => document.getElementById(id);
const pad = (n) => String(n).padStart(2, '0');

const state = {
  month: currentMonth(),
  category: 'Food',
  editingId: null,
  expenses: []
};

function currentMonth() {
  const d = new Date();
  return d.getFullYear() + '-' + pad(d.getMonth() + 1);
}

function todayDate() {
  const d = new Date();
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function categoryInfo(name) {
  return CATEGORIES[name] || CATEGORIES.Other;
}

function formatDate(iso) {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric'
  });
}

// ---------- API ----------
async function api(path, options) {
  const res = await fetch(path, Object.assign({ headers: { 'Content-Type': 'application/json' } }, options));
  if (res.status === 204) return null;
  let data = null;
  try { data = await res.json(); } catch (e) { /* no body */ }
  if (!res.ok) {
    let message = (data && data.message) || 'Something went wrong';
    if (data && data.details) message = Object.values(data.details).join('. ');
    throw new Error(message);
  }
  return data;
}

async function loadAll() {
  try {
    const q = encodeURIComponent(state.month);
    const [expenses, summary] = await Promise.all([
      api('/api/expenses?month=' + q),
      api('/api/summary?month=' + q)
    ]);
    state.expenses = expenses;
    renderSummary(summary);
    renderList(expenses);
  } catch (err) {
    toast('Could not load data: ' + err.message, true);
  }
}

// ---------- rendering ----------
function renderSummary(s) {
  const total = Number(s.totalSpent);
  const previous = Number(s.previousMonthTotal);

  $('totalSpent').textContent = money.format(total);
  $('txCount').textContent = s.transactionCount;
  $('lastMonth').textContent = money.format(previous);

  const note = $('changeNote');
  note.className = 'stat-note';
  if (previous > 0) {
    const diff = ((total - previous) / previous) * 100;
    note.textContent = (diff >= 0 ? '▲ ' : '▼ ') + Math.abs(diff).toFixed(0) + '% vs last month';
    note.classList.add(diff > 0 ? 'note-up' : 'note-down');
  } else {
    note.textContent = 'No data for last month';
  }

  if (s.topCategory) {
    const info = categoryInfo(s.topCategory);
    $('topCategory').textContent = info.icon + ' ' + s.topCategory;
    const share = total > 0 ? Math.round((Number(s.byCategory[0].total) / total) * 100) : 0;
    $('topCategoryNote').textContent = share + '% of your spending';
  } else {
    $('topCategory').textContent = '-';
    $('topCategoryNote').textContent = '\u00a0';
  }

  renderBreakdown(s.byCategory, total);
}

function renderBreakdown(byCategory, total) {
  const legend = $('legend');
  legend.textContent = '';
  $('donutValue').textContent = money.format(total);

  if (!byCategory.length || total <= 0) {
    $('donut').style.background = 'conic-gradient(var(--border) 0 100%)';
    legend.appendChild(el('li', 'legend-empty', 'Add an expense to see your breakdown.'));
    return;
  }

  let start = 0;
  const stops = [];
  byCategory.forEach((row) => {
    const info = categoryInfo(row.category);
    const percent = (Number(row.total) / total) * 100;
    stops.push(info.color + ' ' + start.toFixed(2) + '% ' + (start + percent).toFixed(2) + '%');
    start += percent;

    const li = el('li');
    const top = el('div', 'legend-top');
    top.appendChild(el('span', null, info.icon + ' ' + row.category));
    top.appendChild(el('span', null, money.format(Number(row.total)) + ' · ' + percent.toFixed(0) + '%'));
    const bar = el('div', 'bar');
    const fill = el('i');
    fill.style.width = percent.toFixed(1) + '%';
    fill.style.background = info.color;
    bar.appendChild(fill);
    li.appendChild(top);
    li.appendChild(bar);
    legend.appendChild(li);
  });
  $('donut').style.background = 'conic-gradient(' + stops.join(', ') + ')';
}

function renderList(expenses) {
  const list = $('txList');
  list.textContent = '';
  $('emptyState').hidden = expenses.length > 0;

  expenses.forEach((e) => {
    const info = categoryInfo(e.category);
    const li = el('li', 'tx');

    const icon = el('div', 'tx-icon', info.icon);
    icon.style.background = info.color + '26';

    const main = el('div', 'tx-main');
    main.appendChild(el('div', 'tx-title', e.title));
    const meta = el('div', 'tx-meta');
    const badge = el('span', 'badge', e.category);
    badge.style.background = info.color + '26';
    badge.style.color = info.color;
    meta.appendChild(badge);
    meta.appendChild(el('span', null, formatDate(e.expenseDate)));
    if (e.note) meta.appendChild(el('span', null, '· ' + e.note));
    main.appendChild(meta);

    const amount = el('div', 'tx-amount', money.format(Number(e.amount)));

    const actions = el('div', 'tx-actions');
    const editBtn = el('button', null, '✏️');
    editBtn.title = 'Edit';
    editBtn.addEventListener('click', () => startEdit(e));
    const delBtn = el('button', null, '🗑️');
    delBtn.title = 'Delete';
    delBtn.addEventListener('click', () => removeExpense(e));
    actions.appendChild(editBtn);
    actions.appendChild(delBtn);

    li.appendChild(icon);
    li.appendChild(main);
    li.appendChild(amount);
    li.appendChild(actions);
    list.appendChild(li);
  });
}

function renderChips() {
  const box = $('categoryChips');
  box.textContent = '';
  Object.keys(CATEGORIES).forEach((name) => {
    const info = CATEGORIES[name];
    const chip = el('button', 'chip' + (state.category === name ? ' selected' : ''), info.icon + ' ' + name);
    chip.type = 'button';
    chip.style.setProperty('--chip-color', info.color);
    chip.style.setProperty('--chip-bg', info.color + '22');
    chip.addEventListener('click', () => {
      state.category = name;
      renderChips();
    });
    box.appendChild(chip);
  });
}

// ---------- form actions ----------
function resetForm() {
  $('expenseForm').reset();
  $('expenseDate').value = todayDate();
  $('formError').textContent = '';
  $('formTitle').textContent = 'Add expense';
  $('submitBtn').textContent = 'Add expense';
  $('cancelEdit').hidden = true;
  state.editingId = null;
  state.category = 'Food';
  renderChips();
}

function startEdit(e) {
  state.editingId = e.id;
  state.category = CATEGORIES[e.category] ? e.category : 'Other';
  $('title').value = e.title;
  $('amount').value = e.amount;
  $('expenseDate').value = e.expenseDate;
  $('note').value = e.note || '';
  $('formTitle').textContent = 'Edit expense';
  $('submitBtn').textContent = 'Save changes';
  $('cancelEdit').hidden = false;
  $('formError').textContent = '';
  renderChips();
  $('expenseForm').scrollIntoView({ behavior: 'smooth', block: 'center' });
  $('title').focus();
}

async function removeExpense(e) {
  if (!window.confirm('Delete "' + e.title + '"?')) return;
  try {
    await api('/api/expenses/' + e.id, { method: 'DELETE' });
    toast('Expense deleted');
    if (state.editingId === e.id) resetForm();
    await loadAll();
  } catch (err) {
    toast(err.message, true);
  }
}

async function onSubmit(event) {
  event.preventDefault();
  const error = $('formError');
  error.textContent = '';

  const title = $('title').value.trim();
  const amount = Number($('amount').value);
  const expenseDate = $('expenseDate').value;

  if (!title) { error.textContent = 'Please enter what you spent on.'; return; }
  if (!(amount > 0)) { error.textContent = 'Please enter an amount greater than 0.'; return; }
  if (!expenseDate) { error.textContent = 'Please choose a date.'; return; }

  const payload = {
    title: title,
    amount: amount,
    category: state.category,
    expenseDate: expenseDate,
    note: $('note').value.trim()
  };

  const editing = state.editingId !== null;
  try {
    await api(editing ? '/api/expenses/' + state.editingId : '/api/expenses', {
      method: editing ? 'PUT' : 'POST',
      body: JSON.stringify(payload)
    });
    toast(editing ? 'Expense updated ✅' : 'Expense added ✅');

    // show the month the expense belongs to
    state.month = expenseDate.slice(0, 7);
    $('monthPicker').value = state.month;

    resetForm();
    await loadAll();
  } catch (err) {
    error.textContent = err.message;
  }
}

// ---------- small helpers ----------
let toastTimer = null;
function toast(message, isError) {
  const box = $('toast');
  box.textContent = message;
  box.className = 'toast show' + (isError ? ' error' : '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { box.className = 'toast'; }, 2600);
}

function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  $('themeToggle').textContent = theme === 'dark' ? '☀️' : '🌙';
}

function initTheme() {
  let theme = 'light';
  try { theme = localStorage.getItem('spendwise-theme') || 'light'; } catch (e) { /* storage blocked */ }
  applyTheme(theme);
  $('themeToggle').addEventListener('click', () => {
    const next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    applyTheme(next);
    try { localStorage.setItem('spendwise-theme', next); } catch (e) { /* storage blocked */ }
  });
}

// ---------- start ----------
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  $('monthPicker').value = state.month;
  $('monthPicker').addEventListener('change', (ev) => {
    state.month = ev.target.value || currentMonth();
    loadAll();
  });
  $('expenseForm').addEventListener('submit', onSubmit);
  $('cancelEdit').addEventListener('click', resetForm);
  resetForm();
  loadAll();
});
