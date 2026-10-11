(() => {
  const input = document.getElementById('eventSearch');
  const categorySelect = document.getElementById('categoryFilter');
  const priceSelect = document.getElementById('pricingFilter');
  const statusSelect = document.getElementById('eventStatusFilter');
  const count = document.getElementById('resultCount');
  const empty = document.getElementById('v1Empty');
  const cards = Array.from(document.querySelectorAll('.v1-event-card'));
  const chips = Array.from(document.querySelectorAll('.v1-category-button'));
  if (!input || !categorySelect || !priceSelect || !statusSelect) return;
  const params = new URLSearchParams(window.location.search);
  input.value = params.get('q') || '';
  const category = params.get('category');
  // The selected category is kept even when the event collection has zero entries of that type.
  if (category && !Array.from(categorySelect.options).some(option => option.value.toLowerCase() === category.toLowerCase())) {
    const option = document.createElement('option');
    option.value = category;
    option.textContent = category;
    categorySelect.add(option);
  }
  if (category) categorySelect.value = Array.from(categorySelect.options).find(option => option.value.toLowerCase() === category.toLowerCase())?.value || 'ALL';
  if (params.get('pricing') && ['ALL','FREE','PAID'].includes(params.get('pricing').toUpperCase())) priceSelect.value = params.get('pricing').toUpperCase();
  if (params.get('status') && ['ALL','UPCOMING','COMPLETED'].includes(params.get('status').toUpperCase())) statusSelect.value = params.get('status').toUpperCase();

  function apply() {
    const search = input.value.trim().toLocaleLowerCase();
    const cat = categorySelect.value.toLocaleLowerCase();
    const pricing = priceSelect.value;
    const status = statusSelect.value;
    let showing = 0;
    cards.forEach(card => {
      const match = (!search || (card.dataset.search || '').toLocaleLowerCase().includes(search)) &&
        (cat === 'all' || (card.dataset.category || '').toLocaleLowerCase() === cat) &&
        (pricing === 'ALL' || card.dataset.pricing === pricing) &&
        (status === 'ALL' || card.dataset.status === status);
      card.hidden = !match;
      if (match) showing++;
    });
    count.textContent = `${showing} event${showing === 1 ? '' : 's'}`;
    if (empty) empty.hidden = showing > 0 || cards.length === 0;
    chips.forEach(chip => {
      const selected = chip.dataset.category.toLocaleLowerCase() === cat;
      chip.classList.toggle('is-active', selected);
      chip.setAttribute('aria-pressed', String(selected));
    });
    const next = new URLSearchParams();
    if (input.value.trim()) next.set('q', input.value.trim());
    if (categorySelect.value !== 'ALL') next.set('category',categorySelect.value);
    if (pricing !== 'ALL') next.set('pricing',pricing);
    if (status !== 'UPCOMING') next.set('status',status);
    history.replaceState(null, '', `${location.pathname}${next.size ? '?' + next : ''}`);
  }
  input.addEventListener('input', apply);
  [categorySelect,priceSelect,statusSelect].forEach(el => el.addEventListener('change',apply));
  chips.forEach(chip => chip.addEventListener('click', () => { categorySelect.value = chip.dataset.category;apply(); }));
  document.getElementById('v1ApplyFilters')?.addEventListener('click', apply);
  document.getElementById('v1ClearFilters')?.addEventListener('click', () => {
    input.value = '';categorySelect.value='ALL';priceSelect.value='ALL';statusSelect.value='UPCOMING';apply();
  });
  input.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); apply(); } });
  apply();
})();
