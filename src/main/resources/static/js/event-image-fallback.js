/** If an uploaded image fails, restore the original EventFlow event placeholder. */
(() => {
  if (window.eventflowImageFallbackInstalled) return;
  window.eventflowImageFallbackInstalled = true;

  const selector = 'img[data-event-image], .ef-event-photo img';
  const originalDefault = '/images/event-visual.png';

  function showFallback(img) {
    if (!(img instanceof HTMLImageElement) || !img.matches(selector)) return;
    if (img.dataset.eventFallbackApplied === 'true') return;
    img.dataset.eventFallbackApplied = 'true';
    if (new URL(img.currentSrc || img.src, document.baseURI).pathname !== originalDefault) {
      img.src = originalDefault;
    }
  }

  document.addEventListener('error', event => showFallback(event.target), true);
  document.querySelectorAll(selector).forEach(img => {
    if (img.complete && img.naturalWidth === 0) showFallback(img);
  });
})();
