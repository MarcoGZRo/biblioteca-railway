let books = [], readers = [], loans = [], fines = [], modalType = '';
let editingBookId = null;
const selectedLoan = { book: null, reader: null };
const $ = id => document.getElementById(id);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const fmt = d => d ? new Date(d + 'T12:00:00').toLocaleDateString('es-PE') : '—';
const localDate = date => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
function dueDateForNewLoan() {
  const due = new Date();
  due.setHours(12, 0, 0, 0);
  due.setDate(due.getDate() + 7);
  return fmt(localDate(due));
}

// El frontend y el backend ahora son proyectos independientes.
// Configuración pública (sin contraseñas): frontend/config.js
const API_BASE_URL = (window.APP_CONFIG?.API_BASE_URL ?? 'http://localhost:8080').replace(/\/+$/, '');

async function api(path, options = {}) {
  const headers = {...(options.body ? {'Content-Type': 'application/json'} : {}), ...options.headers};
  const res = await fetch(API_BASE_URL + '/api' + path, { ...options, headers });
  if (!res.ok) {
    const error = await res.json().catch(() => ({}));
    throw Error(error.message || `Error HTTP ${res.status}`);
  }
  return res.status === 204 ? null : res.json();
}

function notify(message) {
  $('toast').textContent = message;
  $('toast').classList.remove('hidden');
  setTimeout(() => $('toast').classList.add('hidden'), 3500);
}

async function refresh() {
  try {
    [books, readers, loans, fines] = await Promise.all([api('/books'), api('/readers'), api('/loans'), api('/fines')]);
    const stats = await api('/stats');
    for (const key of ['books', 'readers', 'active', 'overdue']) $('s' + key).textContent = stats[key];
    renderBooks();
    renderReaders();
    renderLoans();
    renderFines();
    renderRewards();
    $('recent').innerHTML = loanTable(loans.slice(0, 5));
  } catch (error) { notify('Error de conexión: ' + error.message); }
}

function go(page) {
  document.querySelectorAll('.page').forEach(element => element.classList.add('hidden'));
  $(page).classList.remove('hidden');
  document.querySelectorAll('button.nav[data-page]').forEach(element => element.classList.toggle('active', element.dataset.page === page));
  $('heading').textContent = ({dashboard:'Panel general', books:'Catálogo de libros', readers:'Gestión de lectores', loans:'Préstamos y devoluciones', fines:'Registro de multas', rewards:'Recompensas por lectura'})[page];
}

function table(head, rows) {
  return rows.length
    ? '<div class="table-wrap"><table><thead><tr>' + head.map(h => '<th>' + esc(h) + '</th>').join('') + '</tr></thead><tbody>' + rows.join('') + '</tbody></table></div>'
    : '<div class="empty">No hay registros para mostrar.</div>';
}
function pill(text, kind='') { return '<span class="pill ' + kind + '">' + esc(text) + '</span>'; }

function renderBooks() {
  const query = $('bookSearch').value.toLowerCase();
  $('bookTable').innerHTML = table(['Código','Título','Autor','Año','Disponibilidad','Acciones'], books
    .filter(b => [b.code,b.title,b.author].join(' ').toLowerCase().includes(query))
    .map(b => '<tr><td><b>' + esc(b.code) + '</b></td><td>' + esc(b.title) + '</td><td>' + esc(b.author) + '</td><td>' + b.publicationYear + '</td><td>' + pill(b.available?'Disponible':'Prestado', b.available?'':'warn') + '</td><td><div class="row-actions"><button type="button" class="linkbtn" onclick="editBook(' + b.id + ')">Editar</button><button type="button" class="linkbtn dangerbtn" onclick="deleteBook(' + b.id + ')">Eliminar</button></div></td></tr>'));
}

function renderReaders() {
  const query = $('readerSearch').value.toLowerCase();
  $('readerTable').innerHTML = table(['Nombre','DNI','Dirección','Teléfono','Libros devueltos','Recompensa','Acciones'], readers
    .filter(r => [r.name,r.dni].join(' ').toLowerCase().includes(query))
    .map(r => '<tr><td><b>' + esc(r.name) + '</b></td><td>' + esc(r.dni) + '</td><td>' + esc(r.address) + '</td><td>' + esc(r.phone) + '</td><td><b>' + r.readCount + '</b> / 100</td><td>' + (r.rewardEligible ? pill('Caballero de la Lectura','gold') + '<div class="compact-action"><button type="button" class="reward-button" onclick="printTitlePlaceholder()">Imprimir título</button></div>' : pill('En progreso','neutral')) + '</td><td><button type="button" class="linkbtn dangerbtn" onclick="deleteReader(' + r.id + ')">Eliminar</button></td></tr>'));
}

function printTitlePlaceholder() {
  // Mensaje informativo: no genera diploma ni envía nada a una impresora.
  notify('Impresión realizada (simulación). No se imprimió ningún documento.');
}

function renderRewards() {
  const eligible = readers.filter(r => r.rewardEligible).length;
  $('rewardSummary').textContent = eligible === 1 ? '1 lector ha alcanzado la meta' : `${eligible} lectores han alcanzado la meta`;
  const ordered = [...readers].sort((a,b) => b.readCount - a.readCount);
  $('rewardsTable').innerHTML = ordered.length ? '<div class="rewards-grid">' + ordered.map(r => {
    const percent = Math.min(100, Math.max(0, r.readCount));
    return '<article class="reward-card ' + (r.rewardEligible ? 'completed' : '') + '">' +
      '<div class="reward-card-head"><div><strong>' + esc(r.name) + '</strong><p>DNI ' + esc(r.dni) + '</p></div>' + (r.rewardEligible ? pill('Meta alcanzada', 'gold') : pill('En progreso','neutral')) + '</div>' +
      '<div class="progress-label"><span>Lecturas completadas</span><b>' + r.readCount + ' / 100</b></div>' +
      '<div class="progress"><div class="progress-fill" style="width:' + percent + '%"></div></div>' +
      (r.rewardEligible ? '<p class="reward-title">★ Caballero de la Lectura</p><button type="button" class="reward-button" onclick="printTitlePlaceholder()">Imprimir título</button>' : '<p class="reward-remaining">Faltan ' + (100-r.readCount) + ' lecturas para el reconocimiento</p>') +
      '</article>';
  }).join('') + '</div>' : '<div class="empty">Aún no hay lectores registrados.</div>';
}

function loanTable(items) {
  return table(['Libro','Lector','Préstamo','Vencimiento','Estado','Multa','Acción'], items.map(l =>
    '<tr class="' + (l.status==='VENCIDO'?'overdue-row':'') + '"><td><b>' + esc(l.book) + '</b><br><small>' + esc(l.code) + '</small></td>' +
    '<td>' + esc(l.reader) + '</td><td>' + fmt(l.loanDate) + '</td><td>' + fmt(l.dueDate) + '</td>' +
    '<td>' + pill(l.status, l.status==='VENCIDO'?'warn':l.status==='DEVUELTO'?'neutral':'') + '</td>' +
    '<td>S/ ' + l.fine + '</td><td>' + (l.returnDate?fmt(l.returnDate):'<button class="linkbtn" onclick="giveBack(' + l.id + ')">Devolver</button>') + '</td></tr>'));
}

function renderLoans() {
  const query = $('loanSearch').value.trim().toLowerCase();
  const state = $('loanStatus').value;
  const result = loans.filter(l =>
    (!state || l.status === state) &&
    [l.book, l.reader, l.code, l.dni, l.status].join(' ').toLowerCase().includes(query)
  );
  $('loanFilterCount').textContent = `Mostrando ${result.length} de ${loans.length} préstamos`;
  $('loanTable').innerHTML = loanTable(result);
}

function renderFines() {
  const query = $('fineSearch').value.trim().toLowerCase();
  const items = fines.filter(f => [f.reader, f.dni, f.book, f.code].join(' ').toLowerCase().includes(query));
  $('fineCount').textContent = fines.length;
  $('fineTotal').textContent = 'S/ ' + fines.reduce((total, f) => total + Number(f.amountSoles || 0), 0);
  $('fineTable').innerHTML = table(['N.º','Lector','Libro','Fecha devolución','Días de atraso','Importe'], items.map(f =>
    '<tr><td><b>#' + f.id + '</b></td><td><b>' + esc(f.reader) + '</b><br><small>DNI ' + esc(f.dni) + '</small></td>' +
    '<td>' + esc(f.book) + '<br><small>' + esc(f.code) + '</small></td><td>' + fmt(f.returnDate) + '</td>' +
    '<td>' + f.daysLate + '</td><td><b>S/ ' + f.amountSoles + '</b></td></tr>'
  ));
}

// Selector con búsqueda en una lista visible, preparado para catálogos más extensos.
function loanPicker(kind, label) {
  return '<div class="picker-field"><div class="picker-label">' + label + '</div>' +
    '<input name="' + kind + 'Id" id="picked-' + kind + '" type="hidden">' +
    '<input type="search" id="find-' + kind + '" class="picker-search" placeholder="Buscar por ' +
    (kind === 'book' ? 'título, autor o código' : 'nombre o DNI') + '..." oninput="filterLoanChoices(&quot;' + kind + '&quot;)" autocomplete="off" aria-label="Buscar ' + label.toLowerCase() + '">' +
    '<div class="picker-list" id="list-' + kind + '" role="listbox" aria-label="' + label + '"></div>' +
    '<div class="picked-label" id="selected-' + kind + '">Sin seleccionar</div></div>';
}

function filterLoanChoices(kind) {
  const term = $('find-' + kind).value.trim().toLowerCase();
  const options = kind === 'book' ? books.filter(b => b.available) : readers;
  const filtered = options.filter(item => (kind === 'book'
    ? [item.title, item.author, item.code]
    : [item.name, item.dni]).join(' ').toLowerCase().includes(term));
  const visible = filtered.slice(0, 50);
  $('list-' + kind).innerHTML = visible.length ? visible.map(item => {
    const selected = selectedLoan[kind] === item.id;
    const main = kind === 'book' ? item.title : item.name;
    const sub = kind === 'book' ? item.code + ' · ' + item.author : 'DNI ' + item.dni;
    return '<button type="button" class="picker-option ' + (selected ? 'is-selected' : '') +
      '" role="option" aria-selected="' + selected + '" onclick="selectLoanItem(&quot;' + kind + '&quot;,' + item.id + ')">' +
      '<span><b>' + esc(main) + '</b><small>' + esc(sub) + '</small></span><span class="picker-check">' + (selected ? '✓' : '') + '</span></button>';
  }).join('') : '<div class="picker-empty">No se encontraron resultados.</div>';
  if (filtered.length > 50) {
    $('list-' + kind).innerHTML += '<div class="picker-info">Se muestran 50 de ' + filtered.length + '. Escribe para afinar la búsqueda.</div>';
  }
}

function selectLoanItem(kind, id) {
  const entry = kind === 'book' ? books.find(b => b.id === id && b.available) : readers.find(r => r.id === id);
  if (!entry) return;
  selectedLoan[kind] = id;
  $('picked-' + kind).value = String(id);
  $('selected-' + kind).textContent = '✓ Seleccionado: ' + (kind === 'book' ? entry.title + ' (' + entry.code + ')' : entry.name + ' (' + entry.dni + ')');
  $('selected-' + kind).classList.add('chosen');
  filterLoanChoices(kind);
}

function editBook(id) {
  const book = books.find(b => b.id === id);
  if (!book) { notify('No se encontró el libro'); return; }
  showModal('book');
  editingBookId = id;
  modalType = 'book-edit';
  $('modalTitle').textContent = 'Editar libro';
  const form = $('form');
  for (const field of ['title', 'author', 'code', 'publicationYear']) {
    form.elements.namedItem(field).value = book[field];
  }
}

async function deleteBook(id) {
  const book = books.find(b => b.id === id);
  if (!book || !confirm(`¿Eliminar el libro «${book.title}»? Solo se permite si no tiene préstamos registrados.`)) return;
  try {
    await api('/books/' + id, {method:'DELETE'});
    await refresh();
    notify('Libro eliminado correctamente');
  } catch (error) { notify(error.message); }
}

async function deleteReader(id) {
  const reader = readers.find(r => r.id === id);
  if (!reader || !confirm(`¿Eliminar al lector «${reader.name}»? Solo se permite si no tiene préstamos registrados.`)) return;
  try {
    await api('/readers/' + id, {method:'DELETE'});
    await refresh();
    notify('Lector eliminado correctamente');
  } catch (error) { notify(error.message); }
}

function field(label,name,type='text',extra='') {
  return '<label class="field">' + label + '<input name="' + name + '" type="' + type + '" required ' + extra + '></label>';
}
function showModal(type) {
  modalType = type;
  editingBookId = null;
  let html = '';
  if (type === 'book') {
    html = field('Título','title') + field('Autor','author') + field('Código único','code') +
      '<label class="field">Año de impresión<div class="year-row"><input name="publicationYear" id="publicationYear" type="text" inputmode="numeric" maxlength="4" pattern="[0-9]{4}" placeholder="Ej. 1605" required aria-label="Año de impresión"><button class="secondary year-now" type="button" onclick="setCurrentYear()">Año actual</button></div><small class="field-hint">Introduce 4 dígitos (entre 1400 y 2100).</small></label>';
    $('modalTitle').textContent = 'Registrar nuevo libro';
  }
  if (type === 'reader') {
    html = field('Nombre completo','name') + field('DNI (8 dígitos)','dni','text','pattern="[0-9]{8}" maxlength="8"') + field('Dirección','address') + field('Teléfono','phone');
    $('modalTitle').textContent = 'Registrar lector';
  }
  if (type === 'loan') {
    const available = books.filter(b => b.available);
    if (!available.length || !readers.length) { notify('Registra un lector y un libro disponible primero'); return; }
    selectedLoan.book = null;
    selectedLoan.reader = null;
    html = loanPicker('book', 'Libro disponible') + loanPicker('reader', 'Lector') +
      '<div class="loan-due-date"><span>Fecha prevista de devolución</span><strong>' + dueDateForNewLoan() + '</strong><small>Se calcula automáticamente: 7 días después del préstamo.</small></div>' +
      '<p class="help-text">Selecciona un resultado de cada lista. No se prestan libros los martes.</p>';
    $('modalTitle').textContent = 'Registrar préstamo';
  }
  $('fields').innerHTML = html;
  $('overlay').classList.remove('hidden');
  if (type === 'loan') { filterLoanChoices('book'); filterLoanChoices('reader'); }
  const first = $('fields').querySelector('input:not([type=hidden])');
  if (first) first.focus();
}

function setCurrentYear() { $('publicationYear').value = String(new Date().getFullYear()); }
function closeModal() { $('overlay').classList.add('hidden'); $('form').reset(); editingBookId = null; }

$('form').onsubmit = async event => {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(event.target));
  if (modalType === 'book' || modalType === 'book-edit') {
    data.publicationYear = Number(data.publicationYear);
    if (!Number.isInteger(data.publicationYear) || data.publicationYear < 1400 || data.publicationYear > 2100) { notify('Escribe un año válido entre 1400 y 2100'); return; }
  }
  if (modalType === 'loan') {
    if (!selectedLoan.book || !selectedLoan.reader) { notify('Selecciona un libro y un lector de las listas'); return; }
    data.bookId = selectedLoan.book;
    data.readerId = selectedLoan.reader;
  }
  try {
    const isEdit = modalType === 'book-edit';
    await api(isEdit ? '/books/' + editingBookId : '/' + (modalType === 'book' ? 'books' : modalType === 'reader' ? 'readers' : 'loans'), {method:isEdit ? 'PUT' : 'POST', body:JSON.stringify(data)});
    closeModal();
    await refresh();
    notify(isEdit ? 'Libro actualizado correctamente' : 'Registro guardado correctamente');
  } catch (error) { notify(error.message); }
};

async function giveBack(id) {
  if (!confirm('¿Confirmas la devolución de este libro?')) return;
  try {
    const result = await api('/loans/' + id + '/return',{method:'POST'});
    await refresh();
    notify('Devolución registrada. Multa: S/ ' + result.fine);
  } catch (error) { notify(error.message); }
}

document.querySelectorAll('button.nav[data-page]').forEach(button => {
  button.addEventListener('click', () => go(button.dataset.page));
});
$('today').textContent = new Date().toLocaleDateString('es-PE',{weekday:'long',day:'numeric',month:'long',year:'numeric'});
refresh();
