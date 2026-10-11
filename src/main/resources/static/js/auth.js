(() => {
    const $ = (selector, scope = document) => scope.querySelector(selector);
    const $$ = (selector, scope = document) => [...scope.querySelectorAll(selector)];

    const pageShell = $('.page-shell');
    const tabs = $('.tabs');
    const loginTab = $('#loginTab');
    const registerTab = $('#registerTab');
    const loginView = $('#loginView');
    const registerView = $('#registerView');
    const forgotModal = $('#forgotModal');
    const toast = $('#toast');
    let toastTimer;

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
    const namePattern = /^[\p{L}' .-]{3,50}$/u;

    function setAuthLoading(form, label) {
        const button = form?.querySelector('button[type="submit"]');
        if (!button || button.disabled) return;
        button.disabled = true;
        button.innerHTML = `<span class="auth-spinner" aria-hidden="true"></span><span>${label}</span>`;
    }

    function switchView(view) {
        const isRegister = view === 'register';
        tabs.classList.toggle('register-active', isRegister);
        loginTab.classList.toggle('active', !isRegister);
        registerTab.classList.toggle('active', isRegister);
        loginTab.setAttribute('aria-selected', String(!isRegister));
        registerTab.setAttribute('aria-selected', String(isRegister));

        loginView.hidden = isRegister;
        registerView.hidden = !isRegister;
        loginView.classList.toggle('active', !isRegister);
        registerView.classList.toggle('active', isRegister);
    }

    loginTab.addEventListener('click', () => switchView('login'));
    registerTab.addEventListener('click', () => switchView('register'));
    $$('[data-switch]').forEach(button => {
        button.addEventListener('click', () => switchView(button.dataset.switch));
    });

    function replayAnimation(element, className) {
        if (!element) return;
        element.classList.remove(className);
        void element.offsetWidth;
        element.classList.add(className);
    }

    function setFieldState(input, message = '') {
        const field = input.closest('.field');
        const error = $(`#${input.id}Error`);
        const invalid = Boolean(message);
        const hasValue = input.value.trim() !== '';
        const wasInvalid = field.classList.contains('invalid');
        const wasValid = field.classList.contains('valid');

        field.classList.toggle('invalid', invalid);
        field.classList.toggle('valid', !invalid && hasValue);
        input.setAttribute('aria-invalid', String(invalid));

        if (error) {
            error.textContent = message;
            error.classList.toggle('show', invalid);
            error.setAttribute('aria-live', 'polite');
        }

        if (invalid && !wasInvalid) {
            replayAnimation(field, 'validation-shake');
        } else if (!invalid && hasValue && !wasValid) {
            replayAnimation(field, 'validation-pop');
        }

        return !invalid;
    }

    function validateEmail(input) {
        const value = input.value.trim();
        if (!value) return setFieldState(input, 'Email address is required.');
        if (!emailPattern.test(value)) return setFieldState(input, 'Enter a valid email address.');
        return setFieldState(input);
    }

    function passwordChecks(value) {
        return {
            length: value.length >= 8,
            upper: /[A-Z]/.test(value),
            lower: /[a-z]/.test(value),
            number: /\d/.test(value),
            special: /[^A-Za-z0-9]/.test(value)
        };
    }

    function validateStrongPassword(input, showRules = true) {
        const value = input.value;
        const checks = passwordChecks(value);

        if (showRules) {
            Object.entries(checks).forEach(([rule, passed]) => {
                const chip = $(`[data-rule="${rule}"]`, $('#passwordRules'));
                if (chip) chip.classList.toggle('passed', passed);
            });

            const passedCount = Object.values(checks).filter(Boolean).length;
            const strengthBar = $('#passwordStrengthBar');
            const strengthLabel = $('#passwordStrengthLabel');
            if (strengthBar) strengthBar.style.width = `${passedCount * 20}%`;
            if (strengthLabel) {
                strengthLabel.textContent = passedCount <= 2 ? 'Weak' : passedCount <= 4 ? 'Getting stronger' : 'Strong password';
                strengthLabel.dataset.level = passedCount <= 2 ? 'weak' : passedCount <= 4 ? 'medium' : 'strong';
            }
        }

        if (!value) return setFieldState(input, 'Password is required.');
        if (!Object.values(checks).every(Boolean)) {
            return setFieldState(input, 'Use all five password requirements shown above.');
        }
        return setFieldState(input);
    }

    function validateLoginPassword(input) {
        if (!input.value) return setFieldState(input, 'Password is required.');
        if (input.value.length < 8) return setFieldState(input, 'Password must be at least 8 characters.');
        return setFieldState(input);
    }

    function validateName(input) {
        const value = input.value.trim().replace(/\s+/g, ' ');
        input.value = value;
        if (!value) return setFieldState(input, 'Full name is required.');
        if (!namePattern.test(value)) return setFieldState(input, 'Use 3–50 letters and normal name characters only.');
        return setFieldState(input);
    }

    function validateRole(input) {
        if (!input.value) return setFieldState(input, 'Choose Participant or Organizer.');
        return setFieldState(input);
    }

    function validateConfirmPassword() {
        const password = $('#registerPassword');
        const confirm = $('#confirmPassword');
        if (!confirm.value) return setFieldState(confirm, 'Please confirm your password.');
        if (confirm.value !== password.value) return setFieldState(confirm, 'Passwords do not match.');
        return setFieldState(confirm);
    }

    $('#loginEmail').addEventListener('blur', e => validateEmail(e.target));
    $('#loginPassword').addEventListener('blur', e => validateLoginPassword(e.target));
    $('#registerEmail').addEventListener('blur', e => validateEmail(e.target));
    $('#fullName').addEventListener('blur', e => validateName(e.target));
    $('#role').addEventListener('change', e => validateRole(e.target));
    $('#confirmPassword').addEventListener('input', validateConfirmPassword);
    $('#registerPassword').addEventListener('input', e => {
        validateStrongPassword(e.target);
        if ($('#confirmPassword').value) validateConfirmPassword();
    });

    $$('.demo-account').forEach(button => {
        button.addEventListener('click', () => {
            switchView('login');

            const emailInput = $('#loginEmail');
            const passwordInput = $('#loginPassword');
            emailInput.value = button.dataset.demoEmail || '';
            passwordInput.value = button.dataset.demoPassword || '';

            validateEmail(emailInput);
            validateLoginPassword(passwordInput);

            $$('.demo-account').forEach(item => item.classList.remove('selected'));
            button.classList.add('selected');
            replayAnimation(button, 'demo-selected-pop');
            showToast(`${button.dataset.demoRole} selected`, 'Login details filled. Click Sign In to continue.');
            $('#loginForm .primary-button').focus();
        });
    });

    $('#loginForm').addEventListener('submit', event => {
        const validEmail = validateEmail($('#loginEmail'));
        const validPassword = validateLoginPassword($('#loginPassword'));

        if (!validEmail || !validPassword) {
            event.preventDefault();
            return;
        }
        setAuthLoading(event.currentTarget, 'Signing In...');
    });

    $('#registerForm').addEventListener('submit', event => {
        const validations = [
            validateName($('#fullName')),
            validateRole($('#role')),
            validateEmail($('#registerEmail')),
            validateStrongPassword($('#registerPassword')),
            validateConfirmPassword()
        ];

        if (!validations.every(Boolean)) {
            event.preventDefault();
            return;
        }
        setAuthLoading(event.currentTarget, 'Creating Account...');
    });

    $$('.password-toggle').forEach(button => {
        button.addEventListener('click', () => {
            const input = document.getElementById(button.dataset.target);
            const showing = input.type === 'text';
            input.type = showing ? 'password' : 'text';
            button.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
            button.classList.toggle('active', !showing);
        });
    });

    function setForgotStep(step) {
        const resetStep = step === 'reset';
        $('#forgotEmailStep').hidden = resetStep;
        $('#forgotResetStep').hidden = !resetStep;
    }

    function updateForgotPasswordStrength(value) {
        const checks = passwordChecks(value);
        Object.entries(checks).forEach(([rule, passed]) => {
            const chip = $(`[data-forgot-rule="${rule}"]`, $('#forgotPasswordRules'));
            if (chip) chip.classList.toggle('passed', passed);
        });

        const passedCount = Object.values(checks).filter(Boolean).length;
        const strengthBar = $('#forgotPasswordStrengthBar');
        const strengthLabel = $('#forgotPasswordStrengthLabel');
        if (strengthBar) strengthBar.style.width = `${passedCount * 20}%`;
        if (strengthLabel) {
            strengthLabel.textContent = passedCount <= 2 ? 'Weak' : passedCount <= 4 ? 'Getting stronger' : 'Strong password';
            strengthLabel.dataset.level = passedCount <= 2 ? 'weak' : passedCount <= 4 ? 'medium' : 'strong';
        }
        return checks;
    }

    function validateForgotNewPassword() {
        const input = $('#forgotNewPassword');
        const checks = updateForgotPasswordStrength(input.value);
        if (!input.value) return setFieldState(input, 'New password is required.');
        if (!Object.values(checks).every(Boolean)) {
            return setFieldState(input, 'Use all five password requirements shown above.');
        }
        return setFieldState(input);
    }

    function validateForgotConfirmPassword() {
        const password = $('#forgotNewPassword');
        const confirm = $('#forgotConfirmPassword');
        if (!confirm.value) return setFieldState(confirm, 'Please confirm your new password.');
        if (confirm.value !== password.value) return setFieldState(confirm, 'New passwords do not match.');
        return setFieldState(confirm);
    }

    function openForgot(step = 'email') {
        setForgotStep(step);
        forgotModal.classList.add('open');
        forgotModal.setAttribute('aria-hidden', 'false');

        if (step === 'email') {
            const currentEmail = $('#loginEmail').value.trim();
            if (currentEmail && !$('#forgotEmail').value.trim()) $('#forgotEmail').value = currentEmail;
            setTimeout(() => $('#forgotEmail').focus(), 80);
        } else {
            setTimeout(() => $('#forgotNewPassword').focus(), 80);
        }
    }

    function closeForgot() {
        forgotModal.classList.remove('open');
        forgotModal.setAttribute('aria-hidden', 'true');
    }

    $('#forgotButton').addEventListener('click', () => openForgot('email'));
    $('#closeForgot').addEventListener('click', closeForgot);
    $('#forgotBack').addEventListener('click', () => {
        const resetEmail = $('#forgotResetEmail').value.trim();
        if (resetEmail) $('#forgotEmail').value = resetEmail;
        setForgotStep('email');
        setTimeout(() => $('#forgotEmail').focus(), 50);
    });

    forgotModal.addEventListener('click', event => {
        if (event.target === forgotModal) closeForgot();
    });
    document.addEventListener('keydown', event => {
        if (event.key === 'Escape' && forgotModal.classList.contains('open')) closeForgot();
    });

    $('#forgotEmail').addEventListener('blur', e => validateEmail(e.target));
    $('#forgotForm').addEventListener('submit', event => {
        if (!validateEmail($('#forgotEmail'))) {
            event.preventDefault();
            return;
        }
        setAuthLoading(event.currentTarget, 'Checking...');
    });

    $('#forgotNewPassword').addEventListener('input', () => {
        validateForgotNewPassword();
        if ($('#forgotConfirmPassword').value) validateForgotConfirmPassword();
    });
    $('#forgotConfirmPassword').addEventListener('input', validateForgotConfirmPassword);
    $('#forgotResetForm').addEventListener('submit', event => {
        const validPassword = validateForgotNewPassword();
        const validConfirm = validateForgotConfirmPassword();
        if (!validPassword || !validConfirm) {
            event.preventDefault();
            return;
        }
        setAuthLoading(event.currentTarget, 'Updating...');
    });

    function showToast(title, message) {
        $('#toastTitle').textContent = title;
        $('#toastMessage').textContent = message;
        toast.classList.add('show');
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => toast.classList.remove('show'), 3000);
    }

    if (pageShell.dataset.activeTab === 'register') {
        switchView('register');
    }

    if (pageShell.dataset.registerError) {
        switchView('register');
        const message = pageShell.dataset.registerError;
        showToast('Registration failed', message);
        const lower = message.toLowerCase();
        if (lower.includes('email')) setFieldState($('#registerEmail'), message);
        else if (lower.includes('name')) setFieldState($('#fullName'), message);
        else if (lower.includes('participant') || lower.includes('organizer') || lower.includes('role')) setFieldState($('#role'), message);
        else if (lower.includes('do not match')) setFieldState($('#confirmPassword'), message);
        else if (lower.includes('password')) setFieldState($('#registerPassword'), message);
    }

    if (pageShell.dataset.registerSuccess) {
        switchView('login');
        showToast('Account created', pageShell.dataset.registerSuccess);
    }

    if (pageShell.dataset.loginError) {
        switchView('login');
        showToast('Sign in failed', pageShell.dataset.loginError);
        [$('#loginEmail'), $('#loginPassword')].forEach(input => {
            const field = input?.closest('.field');
            field?.classList.add('invalid');
            replayAnimation(field, 'validation-shake');
        });
    }

    if (pageShell.dataset.logoutSuccess) {
        switchView('login');
        showToast('Signed out', pageShell.dataset.logoutSuccess);
    }

    if (pageShell.dataset.passwordResetSuccess) {
        switchView('login');
        showToast('Password updated', pageShell.dataset.passwordResetSuccess);
    }

    if (pageShell.dataset.forgotStep) {
        switchView('login');
        openForgot(pageShell.dataset.forgotStep);
    }

    if (pageShell.dataset.forgotError) {
        const message = pageShell.dataset.forgotError;
        const resetStep = pageShell.dataset.forgotStep === 'reset';
        if (resetStep) {
            const lower = message.toLowerCase();
            if (lower.includes('match')) setFieldState($('#forgotConfirmPassword'), message);
            else setFieldState($('#forgotNewPassword'), message);
        } else {
            setFieldState($('#forgotEmail'), message);
        }
    }

})();
