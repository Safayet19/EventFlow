(() => {
  const loginTab = document.getElementById('loginTab');
  const registerTab = document.getElementById('registerTab');
  const loginView = document.getElementById('loginView');
  const registerView = document.getElementById('registerView');
  const switchTo = role => {
    if (!loginTab || !registerTab || !loginView || !registerView) return;
    const register = role === 'register';
    loginTab.classList.toggle('is-active', !register);
    registerTab.classList.toggle('is-active', register);
    loginTab.setAttribute('aria-selected', String(!register));
    registerTab.setAttribute('aria-selected', String(register));
    loginView.hidden = register; registerView.hidden = !register;
    document.body.classList.toggle('v1-register-open', register);
    const loggedOutNotice = document.getElementById('v1LogoutSuccess');
    if (loggedOutNotice) loggedOutNotice.hidden = register;
    document.title = register ? 'Register | EventFlow' : 'Login | EventFlow';
    const heroTitle = document.getElementById('v1AuthHeroTitle');
    const heroDescription = document.getElementById('v1AuthHeroDescription');
    if (heroTitle) heroTitle.innerHTML = register
      ? 'Create Your<br><em>Account</em>' : 'Welcome<br><em>Back</em>';
    if (heroDescription) heroDescription.textContent = register
      ? 'Join EventFlow to discover or organize events.'
      : 'Log in to your EventFlow account and continue your event journey.';
  };
  loginTab?.addEventListener('click', () => switchTo('login'));
  registerTab?.addEventListener('click', () => switchTo('register'));
  if (window.eventflowV1Tab === 'register' || window.location.pathname.endsWith('/register')) switchTo('register');
  document.querySelectorAll('.v1-password-toggle').forEach(button => {
    button.addEventListener('click', () => {
      const input = document.getElementById(button.dataset.target);
      if (!input) return;
      const showing = input.type === 'password';
      input.type = showing ? 'text' : 'password';
      button.classList.toggle('is-visible', showing);
      button.setAttribute('aria-pressed', String(showing));
      button.setAttribute('aria-label', showing ? 'Hide password' : 'Show password');
    });
  });

  // Restore original Quick Access UX: select a role -> fill the fields -> click Sign In.
  // The server only renders demo-role credentials when a loopback demo session is allowed.
  const demoNotice = document.getElementById('v1DemoSelection');
  document.querySelectorAll('.v1-demo-option').forEach(option => {
    option.addEventListener('click', () => {
      switchTo('login');
      const email = document.getElementById('loginEmail');
      const password = document.getElementById('loginPassword');
      if (!email || !password) return;
      email.value = option.dataset.demoEmail || '';
      password.value = option.dataset.demoPassword || '';
      email.dispatchEvent(new Event('input', { bubbles: true }));
      password.dispatchEvent(new Event('input', { bubbles: true }));
      document.querySelectorAll('.v1-demo-option').forEach(item => {
        const selected = item === option;
        item.classList.toggle('is-selected', selected);
        item.setAttribute('aria-pressed', String(selected));
      });
      if (demoNotice) {
        demoNotice.hidden = false;
        demoNotice.textContent = `${option.dataset.demoRole} selected. Click Sign In to continue.`;
      }
      document.querySelector('#loginForm button[type="submit"]')?.focus();
    });
  });
  const strong = value => value.length >= 8 && /[A-Z]/.test(value) && /[a-z]/.test(value) && /\d/.test(value) && /[^A-Za-z0-9]/.test(value);
  const registerPassword = document.getElementById('registerPassword');
  const registerConfirm = document.getElementById('confirmPassword');
  const passwordChecks = value => ({
    length: value.length >= 8,
    lower: /[a-z]/.test(value),
    upper: /[A-Z]/.test(value),
    number: /[0-9]/.test(value),
    symbol: /[^A-Za-z0-9]/.test(value)
  });
  const updateMatch = () => {
    const message = document.getElementById('v1PasswordMatch');
    if (!message || !registerConfirm) return;
    message.textContent = !registerConfirm.value ? ''
      : registerPassword.value === registerConfirm.value ? 'Passwords match' : 'Passwords do not match yet';
    message.dataset.valid = String(!!registerConfirm.value && registerPassword.value === registerConfirm.value);
  };
  const updateStrength = () => {
    if (!registerPassword) return;
    const checks = passwordChecks(registerPassword.value);
    const score = Object.values(checks).filter(Boolean).length;
    const label = document.getElementById('v1StrengthLabel');
    const bar = document.getElementById('v1StrengthBar');
    const meter = document.getElementById('v1StrengthMeter');
    if (bar) { bar.style.width = `${score * 20}%`; bar.dataset.level = score <= 2 ? 'weak' : score < 5 ? 'medium' : 'strong'; }
    if (label) label.textContent = !registerPassword.value ? 'Enter a password'
      : score <= 2 ? 'Weak' : score < 5 ? 'Medium' : 'Strong';
    meter?.setAttribute('aria-valuenow', String(score));
    Object.entries(checks).forEach(([key, passed]) => {
      const item = document.querySelector(`[data-strength-rule="${key}"]`);
      if (item) item.classList.toggle('is-met', passed);
    });
    updateMatch();
  };
  registerPassword?.addEventListener('input', updateStrength);
  registerConfirm?.addEventListener('input', updateMatch);
  updateStrength();

  document.getElementById('registerForm')?.addEventListener('submit', event => {
    const first = document.getElementById('registerPassword').value;
    const second = document.getElementById('confirmPassword').value;
    const error = document.getElementById('v1RegisterError');
    if (!strong(first) || first !== second) {
      event.preventDefault();
      error.textContent = !strong(first) ? 'Use uppercase, lowercase, a number and symbol (8+ characters).' : 'Passwords do not match.';
    }
  });

  // Restore the original two-step forgot-password dialog (no email service).
  const forgotModal = document.getElementById('v1ForgotModal');
  const forgotLink = document.getElementById('v1ForgotLink');
  const forgotEmailStep = document.getElementById('v1ForgotEmailStep');
  const forgotResetStep = document.getElementById('v1ForgotResetStep');
  let previouslyFocused = null;
  const setForgotStep = step => {
    const reset = step === 'reset';
    forgotEmailStep.hidden = reset;
    forgotResetStep.hidden = !reset;
    if (reset) {
      const email = document.getElementById('v1ForgotEmail').value.trim();
      if (email) document.getElementById('v1ForgotResetEmail').value = email;
    }
  };
  const openForgot = (step = 'email') => {
    previouslyFocused = document.activeElement;
    setForgotStep(step);
    forgotModal.hidden = false;
    document.body.classList.add('v1-forgot-active');
    const field = document.getElementById(step === 'reset' ? 'v1ForgotNewPassword' : 'v1ForgotEmail');
    field?.focus();
  };
  const closeForgot = () => {
    forgotModal.hidden = true;
    document.body.classList.remove('v1-forgot-active');
    previouslyFocused?.focus();
  };
  forgotLink?.addEventListener('click', event => {
    event.preventDefault();
    document.getElementById('v1ForgotError').hidden = true;
    openForgot('email');
  });
  document.getElementById('v1ForgotClose')?.addEventListener('click', closeForgot);
  document.getElementById('v1ForgotBack')?.addEventListener('click', () => setForgotStep('email'));
  forgotModal?.addEventListener('click', event => {
    if (event.target === forgotModal) closeForgot();
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !forgotModal.hidden) closeForgot();
  });
  document.getElementById('v1ForgotResetForm')?.addEventListener('submit', event => {
    const first = document.getElementById('v1ForgotNewPassword').value;
    const confirm = document.getElementById('v1ForgotConfirmPassword').value;
    const error = document.getElementById('v1ForgotValidation');
    if (!strong(first) || first !== confirm) {
      event.preventDefault();
      error.hidden = false;
      error.textContent = !strong(first)
        ? 'Use uppercase, lowercase, a number and symbol (8+ characters).'
        : 'Passwords do not match.';
    } else {
      error.hidden = true;
    }
  });
  if (document.body.dataset.forgotEmail) {
    document.getElementById('v1ForgotEmail').value = document.body.dataset.forgotEmail;
    document.getElementById('v1ForgotResetEmail').value = document.body.dataset.forgotEmail;
  }
  const forgotError = document.body.dataset.forgotError;
  if (forgotError) {
    const error = document.getElementById('v1ForgotError');
    error.textContent = forgotError;
    error.hidden = false;
  }
  if (document.body.dataset.forgotStep || document.body.dataset.forgotOpen === 'true') {
    openForgot(document.body.dataset.forgotStep || 'email');
  }
})();
