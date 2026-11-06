(() => {
  'use strict';
  const table = document.getElementById('finance-payment-table');
  if (!table) return;
  const rows = Array.from(table.querySelectorAll('.finance-payment-row'));
  const search = document.getElementById('finance-search');
  const status = document.getElementById('finance-status-filter');
  const method = document.getElementById('finance-method-filter');
  const visibleCount = document.getElementById('finance-visible-count');
  const filterEmpty = document.getElementById('finance-filter-empty');
  const emptyRow = document.getElementById('finance-empty-row');
  const money = value => new Intl.NumberFormat('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
  const amountOf = row => Number.parseFloat(row.dataset.amount || '0') || 0;
  const rowsByStatus = key => rows.filter(row => row.dataset.status === key);
  const sum = list => list.reduce((total, row) => total + amountOf(row), 0);
  const setText = (id, value) => { const el = document.getElementById(id); if (el) el.textContent = value; };
  const refreshStats = () => {
    const success = rowsByStatus('success');
    const pending = rowsByStatus('pending');
    const refunded = rowsByStatus('refunded');
    const attention = rows.filter(row => ['failed', 'pending'].includes(row.dataset.status));
    setText('finance-paid-total', 'LKR ' + money(sum(success)));
    setText('finance-success-count', success.length);
    setText('finance-pending-total', 'LKR ' + money(sum(pending)));
    setText('finance-pending-count', pending.length);
    setText('finance-refunded-total', 'LKR ' + money(sum(refunded)));
    setText('finance-refunded-count', refunded.length);
    setText('finance-attention-count', attention.length);
    const track = document.getElementById('finance-paid-track');
    if (track) {
      const all = sum(rows);
      track.style.width = (all > 0 ? Math.min(100, sum(success) / all * 100) : 0) + '%';
    }
    setText('finance-footer-count', rows.length);
  };
  const applyFilters = () => {
    const query = (search?.value || '').trim().toLowerCase();
    let shown = 0;
    rows.forEach(row => {
      const match = (!query || (row.dataset.search || '').toLowerCase().includes(query) || (row.dataset.reference || '').toLowerCase().includes(query))
        && (status?.value === 'all' || row.dataset.status === status?.value)
        && (method?.value === 'all' || row.dataset.method === method?.value);
      row.hidden = !match;
      if (match) shown++;
    });
    if (visibleCount) visibleCount.textContent = shown;
    if (filterEmpty) filterEmpty.hidden = shown !== 0 || rows.length === 0;
    if (emptyRow) emptyRow.hidden = rows.length !== 0;
  };
  [search, status, method].forEach(control => {
    control?.addEventListener(control === search ? 'input' : 'change', applyFilters);
  });
  document.getElementById('finance-reset')?.addEventListener('click', () => {
    if (search) search.value = '';
    if (status) status.value = 'all';
    if (method) method.value = 'all';
    applyFilters();
  });
  document.getElementById('finance-export')?.addEventListener('click', () => {
    const visible = rows.filter(row => !row.hidden);
    const escape = value => '"' + String(value ?? '').replace(/"/g, '""') + '"';
    const data = [['Payment ID','Transaction reference','Reservation ID','Customer ID','Amount (LKR)','Method','Status','Processed at']];
    visible.forEach(row => data.push([row.dataset.paymentId, row.dataset.reference, row.dataset.reservationId, row.dataset.customerId, amountOf(row).toFixed(2), row.dataset.method, row.dataset.status, row.dataset.date]));
    const csv = data.map(line => line.map(escape).join(',')).join('\r\n');
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url; link.download = 'rentease-payment-ledger.csv'; link.click();
    URL.revokeObjectURL(url);
  });
  refreshStats();
  applyFilters();
})();
