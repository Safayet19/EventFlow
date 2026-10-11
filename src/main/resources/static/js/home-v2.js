/* Home-only interactions. Event search is submitted to /events?q=... where site.js
   initializes the Explore page's live filter from that query parameter. */
(() => {
    const focusButton = document.getElementById('efNavSearch');
    const searchInput = document.getElementById('efEventSearch');
    focusButton?.addEventListener('click', () => {
        if (!searchInput) return;
        searchInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
        searchInput.focus({ preventScroll: true });
    });
})();
