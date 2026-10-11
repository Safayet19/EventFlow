(() => {
  'use strict';
  const coupon = document.getElementById('efCouponCode');
  const apply = document.getElementById('efCouponApply');
  const wrapper = document.getElementById('efCheckoutCoupon');
  const result = document.getElementById('efCouponResult');
  const note = document.getElementById('efMethodNote');
  if (!coupon || !apply || !wrapper || !result) return;
  const methodLabels = {
    BKASH: 'bKash selected.',
    NAGAD: 'Nagad selected.',
    VISA: 'Visa selected.'
  };
  document.querySelectorAll('input[name="paymentMethod"]').forEach(input => {
    input.addEventListener('change', () => { if (note) note.textContent = methodLabels[input.value]; });
  });
  const originalPrice = document.getElementById('efOriginalPrice').textContent;
  let appliedCode = coupon.value.trim().toUpperCase();
  document.getElementById('efPaymentForm')?.addEventListener('submit', event => {
    if (coupon.value.trim() && coupon.value.trim() !== appliedCode) {
      event.preventDefault();
      result.textContent = 'Apply the promo code first.';
      result.className = 'ef-promo-message error';
      coupon.focus();
    }
  }, true);
  coupon.addEventListener('input', () => {
    coupon.value = coupon.value.toUpperCase().replace(/[^A-Z0-9_-]/g, '');
    if (coupon.value.trim() !== appliedCode) {
      appliedCode = '';
      document.getElementById('efDiscount').textContent = '− ৳0';
      document.getElementById('efPayable').textContent = originalPrice;
    }
    result.textContent = coupon.value && coupon.value !== appliedCode ? 'Click Apply to use this code.' : '';
    result.className = 'ef-promo-message';
  });
  apply.addEventListener('click', async () => {
    const code = coupon.value.trim();
    if (!code) { result.textContent = 'Enter a coupon code first.'; result.className = 'ef-promo-message error'; coupon.focus(); return; }
    apply.disabled = true;
    apply.textContent = 'Checking…';
    const q = new URLSearchParams({couponCode:code});
    if (wrapper.dataset.ticketId) q.set('ticketTypeId', wrapper.dataset.ticketId);
    try {
      const response = await fetch(`/participant/events/${encodeURIComponent(wrapper.dataset.eventId)}/coupon-preview?${q}`, {headers:{Accept:'application/json'}});
      const payload = await response.json();
      if (!response.ok || !payload.valid) throw new Error(payload.message || 'Invalid coupon.');
      document.getElementById('efOriginalPrice').textContent = payload.originalPrice;
      document.getElementById('efDiscount').textContent = '− ' + payload.discount;
      document.getElementById('efPayable').textContent = payload.finalPrice;
      appliedCode = payload.couponCode;
      result.textContent = `${payload.couponCode} applied.`;
      result.className = 'ef-promo-message success';
    } catch(error) {
      result.textContent = error.message || 'Unable to check the code.';
      result.className = 'ef-promo-message error';
    } finally {apply.disabled = false; apply.textContent = 'Apply';}
  });
})();
