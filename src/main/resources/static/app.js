const API = '/api';
let currentTournamentId = null;
let currentTournamentTotalRounds = null;
let rosterCache = []; // master player list, fetched once and re-used for search filtering

async function api(path, method = 'GET', body) {
  const res = await fetch(API + path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined
  });
  if (!res.ok) {
    const msg = await res.text();
    setStatus('❌ Error: ' + msg, true);
    throw new Error(msg);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

function setStatus(msg, isError) {
  const el = document.getElementById('status');
  el.textContent = msg;
  el.style.color = isError ? '#c0392b' : '#27ae60';
}

async function loadTournamentList() {
  const list = await api('/tournaments');
  const select = document.getElementById('t-select');
  select.innerHTML = '<option value="">-- select --</option>';
  list.forEach(t => {
    const opt = document.createElement('option');
    opt.value = t.id;
    opt.textContent = `${t.name} (round ${t.currentRound}/${t.totalRounds}, ${t.status})`;
    select.appendChild(opt);
  });
}

async function createTournament() {
  const name = document.getElementById('t-name').value.trim();
  let rounds = document.getElementById('t-rounds').value;
  if (!name) { setStatus('Enter a tournament name first.', true); return; }
  if (!rounds) rounds = 5; // will still be editable later; simple default
  const t = await api('/tournaments', 'POST', { name, rounds: Number(rounds) });
  setStatus('✅ Created tournament: ' + t.name);
  await loadTournamentList();
  document.getElementById('t-select').value = t.id;
  selectTournament();
}

async function deleteTournament() {
  if (!currentTournamentId) { setStatus('Select a tournament first.', true); return; }
  const ok = confirm('Delete this whole tournament? This removes all its players and match history. This cannot be undone.');
  if (!ok) return;
  await api(`/tournaments/${currentTournamentId}`, 'DELETE');
  setStatus('✅ Tournament deleted.');
  currentTournamentId = null;
  document.getElementById('players-section').classList.add('hidden');
  document.getElementById('pairings-section').classList.add('hidden');
  document.getElementById('standings-section').classList.add('hidden');
  document.getElementById('awards-section').classList.add('hidden');
  await loadTournamentList();
  document.getElementById('t-select').value = '';
}

async function selectTournament() {
  const id = document.getElementById('t-select').value;
  currentTournamentId = id || null;
  document.getElementById('players-section').classList.toggle('hidden', !currentTournamentId);
  document.getElementById('pairings-section').classList.toggle('hidden', !currentTournamentId);
  document.getElementById('standings-section').classList.toggle('hidden', !currentTournamentId);
  document.getElementById('awards-section').classList.toggle('hidden', !currentTournamentId);
  if (currentTournamentId) {
    const t = await api(`/tournaments/${currentTournamentId}`);
    currentTournamentTotalRounds = t.totalRounds;
    refreshStandings();
    refreshCurrentRound();
    refreshAwards();
    loadRoster();
  }
}

async function addPlayer() {
  const name = document.getElementById('p-name').value.trim();
  const rating = Number(document.getElementById('p-rating').value) || 1200;
  const ageVal = document.getElementById('p-age').value;
  const age = ageVal ? Number(ageVal) : null;
  const gender = document.getElementById('p-gender').value || null;
  if (!currentTournamentId) { setStatus('Select or create a tournament first.', true); return; }
  if (!name) { setStatus('Enter a player name.', true); return; }
  await api(`/tournaments/${currentTournamentId}/players`, 'POST', { name, rating, age, gender });
  document.getElementById('p-name').value = '';
  document.getElementById('p-age').value = '';
  document.getElementById('p-gender').value = '';
  setStatus('✅ Added player: ' + name);
  await refreshStandings();
  await loadRoster(); // this player is now saved/updated in the roster too
}

// ── Master roster: reuse players from past tournaments with their
//    up-to-date ratings instead of retyping everything every time. ──
async function loadRoster() {
  rosterCache = await api('/players');
  renderRoster();
}

function renderRoster() {
  const query = (document.getElementById('roster-search').value || '').toLowerCase();
  const box = document.getElementById('roster-box');
  const filtered = rosterCache.filter(p => p.name.toLowerCase().includes(query));

  if (filtered.length === 0) {
    box.innerHTML = '<p class="roster-empty">No saved players yet — add one below and it will show up here next time.</p>';
    return;
  }

  box.innerHTML = '';
  filtered.forEach(p => {
    const meta = [p.age ? `${p.age}y` : null, p.gender || null].filter(Boolean).join(', ');
    const row = document.createElement('div');
    row.className = 'roster-row';
    row.innerHTML = `
      <input type="checkbox" id="roster-${p.id}" value="${p.id}">
      <label for="roster-${p.id}">${p.name}</label>
      <span class="roster-meta">${meta}</span>
      <span class="roster-rating">${p.rating}</span>
      <button class="small-btn danger-btn" onclick="deleteRosterProfile(${p.id}, '${p.name.replace(/'/g, "\\'")}')">🗑</button>`;
    box.appendChild(row);
  });
}

async function deleteRosterProfile(profileId, name) {
  const ok = confirm(`Remove "${name}" from the saved roster? (Does not affect tournaments they've already played in.)`);
  if (!ok) return;
  await api(`/players/roster/${profileId}`, 'DELETE');
  setStatus(`✅ Removed ${name} from the roster.`);
  await loadRoster();
}

async function clearRoster() {
  const ok = confirm('Clear the ENTIRE saved player roster? Everyone will need to be re-added manually next time. This does not touch any existing tournament.');
  if (!ok) return;
  await api('/players/roster', 'DELETE');
  setStatus('✅ Roster cleared.');
  await loadRoster();
}

async function addSelectedFromRoster() {
  if (!currentTournamentId) { setStatus('Select or create a tournament first.', true); return; }
  const checked = Array.from(document.querySelectorAll('#roster-box input[type="checkbox"]:checked'));
  const profileIds = checked.map(c => Number(c.value));
  if (profileIds.length === 0) { setStatus('Check at least one saved player to add.', true); return; }

  const added = await api(`/tournaments/${currentTournamentId}/players/from-roster`, 'POST', { profileIds });
  setStatus(`✅ Added ${added.length} player(s) from the roster with their latest ratings.`);
  checked.forEach(c => c.checked = false);
  await refreshStandings();
}

async function generatePairings() {
  if (!currentTournamentId) return;
  const matches = await api(`/tournaments/${currentTournamentId}/pairings`, 'POST');
  setStatus(`✅ Generated ${matches.length} board(s) for the next round.`);
  await refreshCurrentRound();
}

async function refreshCurrentRound() {
  const matches = await api(`/tournaments/${currentTournamentId}/matches/current`);
  const label = document.getElementById('round-label');
  const genBtn = document.getElementById('generate-btn');
  let roundNum = matches.length > 0 ? matches[0].round : 0;

  if (roundNum > 0) {
    label.textContent = currentTournamentTotalRounds
      ? `— Round ${roundNum} of ${currentTournamentTotalRounds}`
      : `— Round ${roundNum}`;
  } else {
    label.textContent = '';
  }

  if (currentTournamentTotalRounds && roundNum >= currentTournamentTotalRounds) {
    genBtn.disabled = true;
    genBtn.textContent = `🏁 Final round reached (${currentTournamentTotalRounds}/${currentTournamentTotalRounds})`;
  } else {
    genBtn.disabled = false;
    genBtn.textContent = 'Generate Next Round Pairings';
  }

  const container = document.getElementById('boards');
  container.innerHTML = '';
  matches.forEach(m => {
    const div = document.createElement('div');
    const played = m.resultWhite !== null && m.resultWhite >= 0;
    div.className = 'board' + (played || m.bye ? ' done' : '');

    if (m.bye) {
      div.innerHTML = `<span>🎁 BYE — <strong>${m.white.name}</strong></span>`;
    } else {
      div.innerHTML = `
        <span>Board ${m.boardNumber}: ⬜ <strong>${m.white.name}</strong> (${m.white.rating})
        vs ⬛ <strong>${m.black.name}</strong> (${m.black.rating})</span>
        <span>
          <button onclick="submitResult(${m.id}, 1)">White wins</button>
          <button onclick="submitResult(${m.id}, 0.5)">Draw</button>
          <button onclick="submitResult(${m.id}, 0)">Black wins</button>
        </span>`;
      if (played) {
        const label = m.resultWhite === 1 ? 'White won' : m.resultWhite === 0.5 ? 'Draw' : 'Black won';
        div.querySelector('span:last-child').innerHTML = `<em>${label} ✔</em>`;
      }
    }
    container.appendChild(div);
  });
}

async function submitResult(matchId, result) {
  await api(`/matches/${matchId}/result`, 'POST', { result });
  setStatus('✅ Result recorded.');
  await refreshCurrentRound();
  await refreshStandings();
  await refreshAwards();
}

async function refreshStandings() {
  const players = await api(`/tournaments/${currentTournamentId}/standings`);
  document.getElementById('player-count').textContent = `${players.length} player(s) registered`;
  const body = document.getElementById('standings-body');
  body.innerHTML = '';
  players.forEach((p, i) => {
    const tr = document.createElement('tr');
    tr.id = `player-row-${p.id}`;
    tr.innerHTML = `<td>${i + 1}</td>
      <td class="name-cell">${p.name}</td>
      <td>${p.score}</td>
      <td class="rating-cell">${p.rating}</td>
      <td class="age-cell">${p.age ?? ''}</td>
      <td class="gender-cell">${p.gender ?? ''}</td>
      <td>${p.wins}</td><td>${p.losses}</td><td>${p.draws}</td><td>${p.byeCount}</td>
      <td>
        <button class="small-btn" onclick="startEditPlayer(${p.id}, '${p.name.replace(/'/g, "\\'")}', ${p.rating}, ${p.age ?? 'null'}, ${p.gender ? `'${p.gender}'` : 'null'})">✏️ Edit</button>
        <button class="small-btn danger-btn" onclick="deletePlayerFromStandings(${p.id}, '${p.name.replace(/'/g, "\\'")}')">🗑</button>
      </td>`;
    body.appendChild(tr);
  });
}

async function deletePlayerFromStandings(playerId, name) {
  const ok = confirm(`Remove "${name}" from this tournament? Any matches they've played in this tournament will also be removed.`);
  if (!ok) return;
  await api(`/players/${playerId}`, 'DELETE');
  setStatus(`✅ Removed ${name} from this tournament.`);
  await refreshStandings();
  await refreshCurrentRound();
  await refreshAwards();
}

// ── Inline fix for a typo'd name, wrong rating, age, or gender ──
function startEditPlayer(playerId, currentName, currentRating, currentAge, currentGender) {
  const tr = document.getElementById(`player-row-${playerId}`);
  const nameCell = tr.querySelector('.name-cell');
  const ratingCell = tr.querySelector('.rating-cell');
  const ageCell = tr.querySelector('.age-cell');
  const genderCell = tr.querySelector('.gender-cell');
  const actionsCell = tr.lastElementChild;

  nameCell.innerHTML = `<input type="text" class="edit-input name-input" id="edit-name-${playerId}" value="${currentName.replace(/"/g, '&quot;')}">`;
  ratingCell.innerHTML = `<input type="number" class="edit-input" id="edit-rating-${playerId}" value="${currentRating}">`;
  ageCell.innerHTML = `<input type="number" class="edit-input" id="edit-age-${playerId}" value="${currentAge ?? ''}" style="width:55px">`;
  genderCell.innerHTML = `
    <select class="edit-input select-input" id="edit-gender-${playerId}">
      <option value="" ${!currentGender ? 'selected' : ''}>--</option>
      <option value="Male" ${currentGender === 'Male' ? 'selected' : ''}>Male</option>
      <option value="Female" ${currentGender === 'Female' ? 'selected' : ''}>Female</option>
    </select>`;
  actionsCell.innerHTML = `
    <button class="small-btn" onclick="saveEditPlayer(${playerId})">💾 Save</button>
    <button class="small-btn" onclick="refreshStandings()">✖ Cancel</button>`;
}

async function saveEditPlayer(playerId) {
  const name = document.getElementById(`edit-name-${playerId}`).value.trim();
  const rating = Number(document.getElementById(`edit-rating-${playerId}`).value);
  const ageVal = document.getElementById(`edit-age-${playerId}`).value;
  const age = ageVal ? Number(ageVal) : null;
  const gender = document.getElementById(`edit-gender-${playerId}`).value || null;
  if (!name) { setStatus('Name cannot be blank.', true); return; }
  await api(`/players/${playerId}`, 'PUT', { name, rating, age, gender });
  setStatus('✅ Player updated.');
  await refreshStandings();
  await refreshCurrentRound(); // boards show names/ratings too, keep them in sync
  await refreshAwards();
  await loadRoster();
}

// ── Awards: overall top 3 + Kiddie / Junior / Lady categories ──
async function refreshAwards() {
  if (!currentTournamentId) return;
  const awards = await api(`/tournaments/${currentTournamentId}/awards`);

  const champList = document.getElementById('award-champions');
  const medals = ['🥇', '🥈', '🥉'];
  champList.innerHTML = awards.overallTop3.length
    ? awards.overallTop3.map((p, i) => `<li>${medals[i] || ''} <span class="award-name">${p.name}</span> — ${p.score} pts</li>`).join('')
    : '<li class="award-empty">No players yet</li>';

  setAwardCard('award-kiddie', awards.topKiddie);
  setAwardCard('award-junior', awards.topJunior);
  setAwardCard('award-lady', awards.topLady);
}

function setAwardCard(elementId, player) {
  const el = document.getElementById(elementId);
  if (player) {
    el.classList.remove('award-empty');
    el.innerHTML = `<span class="award-name">${player.name}</span><br>${player.score} pts · rating ${player.rating}`;
  } else {
    el.classList.add('award-empty');
    el.textContent = 'No eligible player yet';
  }
}

loadTournamentList();
loadRoster();
