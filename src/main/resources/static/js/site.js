(() => {
    const $ = (selector, root = document) => root.querySelector(selector);
    const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

    function replayValidationAnimation(element, className) {
        if (!element) return;
        element.classList.remove(className);
        void element.offsetWidth;
        element.classList.add(className);
    }

    function setFormLoading(form, label = 'Working...') {
        if (!form || form.dataset.submitting === 'true') return;
        const button = form.querySelector('button[type="submit"]');
        if (!button) return;
        form.dataset.submitting = 'true';
        button.disabled = true;
        button.dataset.originalHtml = button.innerHTML;
        button.innerHTML = `<span class="button-spinner" aria-hidden="true"></span><span>${label}</span>`;
    }

    // Mobile sidebar
    const sidebarToggle = $('#sidebarToggle');
    const sidebar = $('#appSidebar');
    sidebarToggle?.addEventListener('click', () => sidebar?.classList.toggle('open'));
    document.addEventListener('click', event => {
        if (!sidebar || !sidebar.classList.contains('open')) return;
        if (sidebar.contains(event.target) || sidebarToggle?.contains(event.target)) return;
        sidebar.classList.remove('open');
    });

    // Notification dropdown
    const notificationToggle = $('#notificationToggle');
    const notificationPanel = $('#notificationPanel');
    const notificationList = $('#notificationPreviewList');
    async function loadNotificationPreview() {
        if (!notificationList) return;
        notificationList.replaceChildren();
        const placeholder = document.createElement('div');
        placeholder.className = 'notification-empty';
        placeholder.textContent = 'Loading notifications...';
        notificationList.appendChild(placeholder);
        try {
            const url = notificationList.dataset.previewUrl;
            const response = await fetch(url, { credentials: 'same-origin', cache: 'no-store' });
            if (!response.ok) throw new Error('Unable to load notifications');
            const messages = await response.json();
            notificationList.replaceChildren();
            if (!messages.length) {
                placeholder.textContent = 'No notifications yet.';
                notificationList.appendChild(placeholder);
                return;
            }
            for (const item of messages) {
                const link = document.createElement('a');
                link.className = 'notification-preview' + (item.read ? '' : ' unread');
                link.href = '/notifications/' + encodeURIComponent(item.id) + '/open';
                const dot = document.createElement('span');
                dot.className = 'notice-dot';
                const copy = document.createElement('div');
                const message = document.createElement('p');
                message.textContent = item.message || '';
                const date = document.createElement('small');
                date.textContent = item.createdAt || '';
                copy.append(message, date);
                link.append(dot, copy);
                notificationList.appendChild(link);
            }
        } catch (error) {
            placeholder.textContent = 'Unable to load here. Use View all.';
            notificationList.replaceChildren(placeholder);
        }
    }
    notificationToggle?.addEventListener('click', event => {
        event.stopPropagation();
        notificationPanel?.classList.toggle('open');
        if (notificationPanel?.classList.contains('open')) loadNotificationPreview();
    });
    notificationPanel?.addEventListener('click', event => event.stopPropagation());
    document.addEventListener('click', () => notificationPanel?.classList.remove('open'));

    // Toasts
    $$('.app-toast').forEach(toast => {
        const close = () => {
            toast.classList.add('hide');
            setTimeout(() => toast.remove(), 240);
        };
        $('button', toast)?.addEventListener('click', close);
        setTimeout(close, 4500);
    });

    // Confirmation modal for logout, delete, disable, cancellation and other sensitive actions.
    // Create it automatically on pages that do not already include the fragment.
    let confirmModal = $('#confirmModal');
    if (!confirmModal) {
        document.body.insertAdjacentHTML('beforeend', `
            <div class="confirm-modal" id="confirmModal" aria-hidden="true">
                <div class="confirm-card" role="dialog" aria-modal="true" aria-labelledby="confirmTitle" aria-describedby="confirmMessage">
                    <div class="confirm-icon" aria-hidden="true"><i class="bi bi-exclamation-triangle"></i></div>
                    <h3 id="confirmTitle">Confirm action</h3>
                    <p id="confirmMessage">Are you sure you want to continue?</p>
                    <div class="confirm-actions">
                        <button class="secondary-button" id="confirmCancel" type="button">Cancel</button>
                        <button class="danger-button" id="confirmProceed" type="button">Confirm</button>
                    </div>
                </div>
            </div>`);
        confirmModal = $('#confirmModal');
    }

    const confirmTitle = $('#confirmTitle');
    const confirmMessage = $('#confirmMessage');
    const confirmCancel = $('#confirmCancel');
    const confirmProceed = $('#confirmProceed');
    let pendingForm = null;
    let previousFocus = null;
    const confirmIcon = $('#confirmModal .confirm-icon');
    const originalConfirmIcon = confirmIcon?.innerHTML || '';
    const logoutIcon = '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false" width="29" height="29" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M13 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h7"/><path d="M14 8l4 4-4 4"/><path d="M18 12H9"/></svg>';

    const closeConfirm = () => {
        pendingForm = null;
        confirmModal?.classList.remove('open');
        confirmModal?.setAttribute('aria-hidden', 'true');
        document.body.style.overflow = '';
        previousFocus?.focus();
        previousFocus = null;
    };

    $$('.confirm-form').forEach(form => {
        form.addEventListener('submit', event => {
            event.preventDefault();
            if (form.dataset.submitting === 'true') return;
            pendingForm = form;
            previousFocus = event.submitter || form.querySelector('button[type="submit"]');
            const isLogout = (form.dataset.confirmAction || '').toLowerCase() === 'log out';
            const isDanger = !isLogout && /delete|disable|remove|cancel/i.test(form.dataset.confirmAction || '');
            confirmModal?.classList.toggle('is-logout', isLogout);
            confirmModal?.classList.toggle('is-danger', isDanger);
            if (confirmIcon) confirmIcon.innerHTML = isLogout ? logoutIcon : originalConfirmIcon;
            if (confirmTitle) confirmTitle.textContent = form.dataset.confirmTitle || 'Confirm action';
            if (confirmMessage) confirmMessage.textContent = form.dataset.confirm || 'Are you sure you want to continue?';
            if (confirmProceed) confirmProceed.textContent = form.dataset.confirmAction || 'Confirm';
            confirmModal?.classList.add('open');
            confirmModal?.setAttribute('aria-hidden', 'false');
            document.body.style.overflow = 'hidden';
            setTimeout(() => confirmCancel?.focus(), 20);
        });
    });
    confirmCancel?.addEventListener('click', closeConfirm);
    confirmProceed?.addEventListener('click', () => {
        const form = pendingForm;
        pendingForm = null;
        confirmModal?.classList.remove('open');
        confirmModal?.setAttribute('aria-hidden', 'true');
        document.body.style.overflow = '';
        if (form) {
            setFormLoading(form, form.dataset.loadingLabel || 'Working...');
            form.submit();
        }
    });
    confirmModal?.addEventListener('click', event => {
        if (event.target === confirmModal) closeConfirm();
    });
    document.addEventListener('keydown', event => {
        if (event.key === 'Escape' && confirmModal?.classList.contains('open')) {
            event.preventDefault(); closeConfirm();
        }
    });

    // Public light/dark theme toggle. Regular mode is the default.
    const publicThemeToggle = $('#publicThemeToggle');
    const publicThemeRoot = document.documentElement;
    // V1 public pages deliberately share Home's permanent light theme.
    let savedPublicTheme = 'light';
    if (!document.body.classList.contains('v1-page')) {
        try { savedPublicTheme = localStorage.getItem('eventflowPublicTheme'); }
        catch (ignored) { savedPublicTheme = 'light'; }
    }
    const initialPublicTheme = savedPublicTheme === 'dark' ? 'dark' : 'light';
    publicThemeRoot.dataset.publicTheme = initialPublicTheme;

    function syncPublicThemeButton() {
        if (!publicThemeToggle) return;
        const dark = publicThemeRoot.dataset.publicTheme === 'dark';
        const icon = publicThemeToggle.querySelector('i');
        if (icon) icon.className = dark ? 'bi bi-sun' : 'bi bi-moon-stars';
        publicThemeToggle.setAttribute('aria-label', dark ? 'Switch to regular mode' : 'Switch to dark mode');
        publicThemeToggle.title = dark ? 'Regular mode' : 'Dark mode';
    }
    syncPublicThemeButton();
    publicThemeToggle?.addEventListener('click', () => {
        const next = publicThemeRoot.dataset.publicTheme === 'dark' ? 'light' : 'dark';
        publicThemeRoot.dataset.publicTheme = next;
        localStorage.setItem('eventflowPublicTheme', next);
        syncPublicThemeButton();
    });

    // Event search/filter
    const eventCards = $$('.event-card');
    const eventSearch = $('#eventSearch');
    const categoryFilter = $('#categoryFilter');
    const pricingFilter = $('#pricingFilter');
    const eventStatusFilter = $('#eventStatusFilter');
    const resultCount = $('#resultCount');
    const emptyState = $('#emptyState');

    const initialFilters = new URLSearchParams(window.location.search);
    const requestedSearch = initialFilters.get('q');
    if (requestedSearch && eventSearch) eventSearch.value = requestedSearch;
    const requestedCategory = initialFilters.get('category');
    if (requestedCategory && categoryFilter) {
        const matchingOption = Array.from(categoryFilter.options).find(option => option.value.toLowerCase() === requestedCategory.toLowerCase());
        if (matchingOption) categoryFilter.value = matchingOption.value;
    }

    function filterEvents() {
        if (!eventCards.length) return;
        const query = (eventSearch?.value || '').trim().toLowerCase();
        const category = categoryFilter?.value || 'ALL';
        const pricing = pricingFilter?.value || 'ALL';
        const status = eventStatusFilter?.value || 'ALL';
        let visible = 0;

        eventCards.forEach(card => {
            const text = `${card.dataset.title || ''} ${card.dataset.location || ''} ${card.dataset.category || ''}`.toLowerCase();
            const show = (!query || text.includes(query))
                && (category === 'ALL' || card.dataset.category === category)
                && (pricing === 'ALL' || card.dataset.pricing === pricing)
                && (status === 'ALL' || card.dataset.status === status);
            card.style.display = show ? '' : 'none';
            if (show) visible++;
        });
        if (resultCount) resultCount.textContent = `${visible} event${visible === 1 ? '' : 's'}`;
        if (emptyState) emptyState.style.display = visible === 0 ? 'block' : 'none';
    }
    eventSearch?.addEventListener('input', filterEvents);
    categoryFilter?.addEventListener('change', filterEvents);
    pricingFilter?.addEventListener('change', filterEvents);
    eventStatusFilter?.addEventListener('change', filterEvents);
    filterEvents();

    // Event details modal
    const eventModal = $('#eventModal');
    const closeEventModal = $('#closeEventModal');
    function setText(id, value) {
        const element = document.getElementById(id);
        if (element) element.textContent = value || '—';
    }
    function openEvent(card) {
        if (!eventModal || !card) return;
        setText('modalCategory', card.dataset.category);
        setText('modalTitle', card.dataset.title);
        setText('modalDescription', card.dataset.description);
        setText('modalDate', card.dataset.date);
        setText('modalTime', card.dataset.time);
        setText('modalLocation', card.dataset.location);
        const registered = card.dataset.registered;
        const capacity = card.dataset.capacity;
        setText('modalCapacity', registered ? `${registered} / ${capacity} registered` : `${capacity || '—'} seats`);
        setText('modalOrganizer', card.dataset.organizer);
        const fullDetails = $('#modalFullDetails');
        if (fullDetails) fullDetails.href = card.dataset.eventUrl || '/events';
        eventModal.classList.add('open');
        eventModal.setAttribute('aria-hidden', 'false');
        document.body.style.overflow = 'hidden';
    }
    function hideEventModal() {
        if (!eventModal) return;
        eventModal.classList.remove('open');
        eventModal.setAttribute('aria-hidden', 'true');
        document.body.style.overflow = '';
    }
    $$('.details-button').forEach(button => button.addEventListener('click', () => openEvent(button.closest('.event-card'))));
    closeEventModal?.addEventListener('click', hideEventModal);
    eventModal?.addEventListener('click', event => { if (event.target === eventModal) hideEventModal(); });

    // Event form live validation, modern date/time pickers and image preview
    const eventForm = $('#eventForm');
    if (eventForm) {
        const fields = {
            title: $('#title'),
            category: $('#category'),
            capacity: $('#capacity'),
            date: $('#date'),
            time: $('#time'),
            location: $('#location'),
            description: $('#description'),
            image: $('#image'),
            basePrice: $('#basePrice')
        };
        const counter = $('#descriptionCounter');
        const preview = $('#imagePreview');
        const datePickerButton = $('#openDatePicker');
        const timePickerButton = $('#openTimePicker');
        const pricingRadios = $$('input[name="pricingType"]', eventForm);
        const paidPricePanel = $('#paidPricePanel');
        const selectedPricingType = () => pricingRadios.find(radio => radio.checked)?.value || 'FREE';
        const today = new Date();
        const localToday = new Date(today.getTime() - today.getTimezoneOffset() * 60000).toISOString().split('T')[0];

        let datePicker = null;
        let timePicker = null;

        const currentLocalTime = () => {
            const now = new Date();
            return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
        };

        const visualFieldFor = name => {
            if (name === 'date' && datePicker?.altInput) return datePicker.altInput;
            if (name === 'time' && timePicker?.altInput) return timePicker.altInput;
            return fields[name];
        };

        function errorFor(name, message = '') {
            const field = fields[name];
            const visualField = visualFieldFor(name);
            const error = document.querySelector(`[data-error-for="${name}"]`);
            const invalid = Boolean(message);
            const hasValue = name === 'image' ? Boolean(field?.files?.length) : Boolean(field?.value?.trim());
            const wasInvalid = visualField?.classList.contains('invalid');
            const wasValid = visualField?.classList.contains('valid');

            if (error) {
                error.textContent = message;
                error.classList.toggle('show', invalid);
                error.setAttribute('aria-live', 'polite');
            }

            visualField?.classList.toggle('invalid', invalid);
            visualField?.classList.toggle('valid', !invalid && hasValue);

            if (visualField !== field) {
                field?.classList.remove('invalid', 'valid');
            }

            if (invalid && !wasInvalid) {
                replayValidationAnimation(visualField, 'validation-shake');
            } else if (!invalid && hasValue && !wasValid) {
                replayValidationAnimation(visualField, 'validation-pop');
            }
            return !invalid;
        }

        function validateField(name) {
            const value = fields[name]?.value?.trim() || '';
            if (name === 'title') return errorFor(name, value.length < 3 || value.length > 80 ? 'Use 3 to 80 characters.' : '');
            if (name === 'category') return errorFor(name, !value ? 'Choose a category.' : '');
            if (name === 'capacity') {
                const n = Number(value);
                const minimum = Number(fields.capacity?.min || 1);
                return errorFor(name, !Number.isInteger(n) || n < minimum || n > 100000 ? `Capacity must be between ${minimum} and 100000.` : '');
            }
            if (name === 'date') {
                if (!value) return errorFor(name, 'Choose an event date.');
                if (value < localToday) return errorFor(name, 'Event date cannot be in the past.');
                if (value === localToday && fields.time?.value) validateField('time');
                return errorFor(name, '');
            }
            if (name === 'time') {
                if (!value) return errorFor(name, 'Choose a start time.');
                if (fields.date?.value === localToday && value < currentLocalTime()) {
                    return errorFor(name, 'Start time cannot be in the past.');
                }
                return errorFor(name, '');
            }
            if (name === 'location') return errorFor(name, value.length < 3 || value.length > 100 ? 'Use 3 to 100 characters.' : '');
            if (name === 'description') return errorFor(name, value.length < 20 || value.length > 1000 ? 'Description must be 20 to 1000 characters.' : '');
            if (name === 'basePrice') {
                if (selectedPricingType() !== 'PAID') return errorFor(name, '');
                const amount = Number(value);
                return errorFor(name, !Number.isFinite(amount) || amount <= 0 || amount > 10000000 ? 'Enter a valid price greater than 0.' : '');
            }
            if (name === 'image') {
                const file = fields.image?.files?.[0];
                if (!file) return errorFor(name, '');
                if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) return errorFor(name, 'Use JPG, PNG or WEBP only.');
                if (file.size > 2 * 1024 * 1024) return errorFor(name, 'Image must be 2 MB or smaller.');
                return errorFor(name, '');
            }
            return true;
        }

        const updateTimeMinimum = () => {
            if (!fields.time) return;
            const minimum = fields.date?.value === localToday ? currentLocalTime() : '00:00';

            if (timePicker) {
                timePicker.set('minTime', minimum);
            } else {
                if (fields.date?.value === localToday) fields.time.min = minimum;
                else fields.time.removeAttribute('min');
            }

            if (fields.date?.value === localToday && fields.time.value && fields.time.value < minimum) {
                if (timePicker) timePicker.clear();
                else fields.time.value = '';
            }
        };

        if (fields.date && fields.time && window.flatpickr) {
            datePicker = flatpickr(fields.date, {
                minDate: 'today',
                dateFormat: 'Y-m-d',
                altInput: true,
                altFormat: 'd M Y',
                allowInput: false,
                disableMobile: true,
                monthSelectorType: 'static',
                prevArrow: '<i class="bi bi-chevron-left"></i>',
                nextArrow: '<i class="bi bi-chevron-right"></i>',
                onReady: (_, __, instance) => {
                    if (instance.altInput) instance.altInput.placeholder = 'Select event date';
                },
                onChange: () => {
                    updateTimeMinimum();
                    validateField('date');
                }
            });

            timePicker = flatpickr(fields.time, {
                enableTime: true,
                noCalendar: true,
                dateFormat: 'H:i',
                altInput: true,
                altFormat: 'h:i K',
                time_24hr: false,
                minuteIncrement: 1,
                allowInput: false,
                disableMobile: true,
                onReady: (_, __, instance) => {
                    if (instance.altInput) instance.altInput.placeholder = 'Select start time';
                },
                onOpen: updateTimeMinimum,
                onChange: () => validateField('time')
            });

            datePickerButton?.addEventListener('click', () => datePicker.open());
            timePickerButton?.addEventListener('click', () => {
                updateTimeMinimum();
                timePicker.open();
            });
        } else {
            // Safe fallback if the picker library cannot load.
            if (fields.date) {
                fields.date.readOnly = false;
                fields.date.type = 'date';
                fields.date.min = localToday;
            }
            if (fields.time) {
                fields.time.readOnly = false;
                fields.time.type = 'time';
            }
            datePickerButton?.addEventListener('click', () => fields.date?.showPicker?.());
            timePickerButton?.addEventListener('click', () => fields.time?.showPicker?.());
        }

        updateTimeMinimum();

        const updatePricingMode = () => {
            const paid = selectedPricingType() === 'PAID';
            paidPricePanel?.classList.toggle('hidden', !paid);
            if (fields.basePrice) {
                fields.basePrice.required = paid;
                if (!paid) errorFor('basePrice', '');
            }
        };
        pricingRadios.forEach(radio => radio.addEventListener('change', () => {
            updatePricingMode();
            if (selectedPricingType() === 'PAID') validateField('basePrice');
        }));
        updatePricingMode();

        Object.entries(fields).forEach(([name, field]) => {
            if (!field) return;
            if ((name === 'date' && datePicker) || (name === 'time' && timePicker)) return;
            const eventName = name === 'image' || field.tagName === 'SELECT' || name === 'date' || name === 'time' ? 'change' : 'input';
            field.addEventListener(eventName, () => {
                if (name === 'date') updateTimeMinimum();
                validateField(name);
            });
        });

        const updateCounter = () => {
            if (counter && fields.description) counter.textContent = `${fields.description.value.length} / 1000`;
        };
        fields.description?.addEventListener('input', updateCounter);
        updateCounter();

        fields.image?.addEventListener('change', () => {
            const file = fields.image.files?.[0];
            if (!file || !validateField('image') || !preview) return;
            const reader = new FileReader();
            reader.onload = event => preview.src = event.target.result;
            reader.readAsDataURL(file);
        });

        eventForm.addEventListener('submit', event => {
            let valid = true;
            Object.keys(fields).forEach(name => { if (!validateField(name)) valid = false; });
            if (!valid) {
                event.preventDefault();
                const firstError = $('.invalid', eventForm);
                firstError?.scrollIntoView({ behavior: 'smooth', block: 'center' });
                firstError?.focus();
            }
        });
    }

    // Generic table filtering
    function setupTableSearch(inputSelector, tableSelector, emptySelector, countSelector) {
        const input = $(inputSelector);
        const table = $(tableSelector);
        const empty = $(emptySelector);
        const count = $(countSelector);
        if (!input || !table) return;
        const rows = $$('tbody tr', table);
        const update = () => {
            const q = input.value.trim().toLowerCase();
            let visible = 0;
            rows.forEach(row => {
                const text = (row.dataset.search || row.dataset.name + ' ' + row.dataset.email || row.textContent).toLowerCase();
                const show = !q || text.includes(q);
                row.style.display = show ? '' : 'none';
                if (show) visible++;
            });
            if (empty) empty.style.display = visible === 0 ? 'block' : 'none';
            if (count) count.textContent = `${visible} ${count.id === 'registrationCount' ? 'registration' : count.id === 'adminEventCount' ? 'event' : 'user'}${visible === 1 ? '' : 's'}`;
        };
        input.addEventListener('input', update);
        update();
    }
    const adminEventTable = $('#adminEventTable');
    if (adminEventTable) {
        const adminEventSearch = $('#adminEventSearch');
        const adminEventCategoryFilter = $('#adminEventCategoryFilter');
        const adminEventPricingFilter = $('#adminEventPricingFilter');
        const adminEventStatusFilter = $('#adminEventStatusFilter');
        const adminEventEmpty = $('#adminEventEmpty');
        const adminEventCount = $('#adminEventCount');
        const rows = $$('tbody tr', adminEventTable);

        const filterAdminEvents = () => {
            const query = (adminEventSearch?.value || '').trim().toLowerCase();
            const category = adminEventCategoryFilter?.value || 'ALL';
            const pricing = adminEventPricingFilter?.value || 'ALL';
            const status = adminEventStatusFilter?.value || 'ALL';
            let visible = 0;

            rows.forEach(row => {
                const text = (row.dataset.search || row.textContent || '').toLowerCase();
                const show = (!query || text.includes(query))
                    && (category === 'ALL' || row.dataset.category === category)
                    && (pricing === 'ALL' || row.dataset.pricing === pricing)
                    && (status === 'ALL' || row.dataset.status === status);
                row.style.display = show ? '' : 'none';
                if (show) visible++;
            });

            if (adminEventEmpty) adminEventEmpty.style.display = visible === 0 ? 'block' : 'none';
            if (adminEventCount) adminEventCount.textContent = `${visible} event${visible === 1 ? '' : 's'}`;
        };

        adminEventSearch?.addEventListener('input', filterAdminEvents);
        adminEventCategoryFilter?.addEventListener('change', filterAdminEvents);
        adminEventPricingFilter?.addEventListener('change', filterAdminEvents);
        adminEventStatusFilter?.addEventListener('change', filterAdminEvents);
        filterAdminEvents();
    }
    // Organizer My Events: same filters and behavior as Admin Manage Events.
    const organizerEventGrid = $('#organizerEventGrid');
    if (organizerEventGrid) {
        const controls = {
            search: $('#organizerEventSearch'),
            category: $('#organizerEventCategoryFilter'),
            pricing: $('#organizerEventPricingFilter'),
            status: $('#organizerEventStatusFilter')
        };
        const cards = $$('.management-event-card', organizerEventGrid);
        const count = $('#organizerEventCount');
        const empty = $('#organizerEventEmpty');
        const updateOrganizerFilters = () => {
            const query = (controls.search?.value || '').trim().toLowerCase();
            const category = controls.category?.value || 'ALL';
            const pricing = controls.pricing?.value || 'ALL';
            const status = controls.status?.value || 'ALL';
            let visible = 0;
            cards.forEach(card => {
                const show = (!query || (card.dataset.search || '').toLowerCase().includes(query))
                    && (category === 'ALL' || card.dataset.category === category)
                    && (pricing === 'ALL' || card.dataset.pricing === pricing)
                    && (status === 'ALL' || card.dataset.status === status);
                card.style.display = show ? '' : 'none';
                if (show) visible++;
            });
            if (count) count.textContent = `${visible} event${visible === 1 ? '' : 's'}`;
            if (empty) empty.style.display = visible ? 'none' : 'block';
        };
        controls.search?.addEventListener('input', updateOrganizerFilters);
        [controls.category, controls.pricing, controls.status].forEach(select =>
            select?.addEventListener('change', updateOrganizerFilters));
        updateOrganizerFilters();
    }
    setupTableSearch('#registrationSearch', '#registrationTable', '#registrationEmpty', '#registrationCount');

    // User search + role filter
    const userSearch = $('#userSearch');
    const roleFilter = $('#roleFilter');
    const statusFilter = $('#statusFilter');
    const userTable = $('#userTable');
    const userEmpty = $('#userEmpty');
    const userCount = $('#userCount');
    if (userTable) {
        const rows = $$('tbody tr', userTable);
        const range = $('#userRange');
        const pager = $('#userPagination');
        const pageSize = 8;
        let currentPage = 1;
        const filterUsers = () => {
            const query = (userSearch?.value || '').trim().toLowerCase();
            const role = roleFilter?.value || 'ALL';
            const status = statusFilter?.value || 'ALL';
            const matches = rows.filter(row => {
                const text = `${row.dataset.name || ''} ${row.dataset.email || ''}`.toLowerCase();
                return (!query || text.includes(query))
                    && (role === 'ALL' || row.dataset.role === role)
                    && (status === 'ALL' || row.dataset.status === status);
            });
            const totalPages = Math.max(1, Math.ceil(matches.length / pageSize));
            currentPage = Math.min(currentPage, totalPages);
            const start = (currentPage - 1) * pageSize;
            const current = new Set(matches.slice(start, start + pageSize));
            rows.forEach(row => { row.style.display = current.has(row) ? '' : 'none'; });
            if (userEmpty) userEmpty.style.display = matches.length === 0 ? 'block' : 'none';
            if (userCount) userCount.textContent = `${matches.length} user${matches.length === 1 ? '' : 's'}`;
            if (range) range.textContent = matches.length ? `Showing ${start + 1}-${Math.min(start + pageSize, matches.length)} of ${matches.length} users` : 'No matching users';
            if (pager) {
                pager.replaceChildren();
                if (totalPages > 1) {
                    const addButton = (text, value, disabled, selected = false) => {
                        const button = document.createElement('button');
                        button.type = 'button';
                        if (text === '<' || text === '>') {
                            const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                            svg.setAttribute('viewBox', '0 0 24 24');
                            svg.setAttribute('width', '17');
                            svg.setAttribute('height', '17');
                            svg.setAttribute('fill', 'none');
                            svg.setAttribute('stroke', 'currentColor');
                            svg.setAttribute('stroke-width', '2');
                            svg.setAttribute('stroke-linecap', 'round');
                            svg.setAttribute('stroke-linejoin', 'round');
                            svg.setAttribute('aria-hidden', 'true');
                            const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                            path.setAttribute('d', text === '<' ? 'm15 18-6-6 6-6' : 'm9 18 6-6-6-6');
                            svg.appendChild(path);
                            button.appendChild(svg);
                        } else {
                            button.textContent = text;
                        }
                        button.className = 'ef-user-page-btn' + (selected ? ' active' : '');
                        button.disabled = disabled;
                        button.setAttribute('aria-label', text === '<' ? 'Previous page' : text === '>' ? 'Next page' : `Page ${value}`);
                        button.addEventListener('click', () => { currentPage = value; filterUsers(); });
                        pager.appendChild(button);
                    };
                    addButton('<', Math.max(1, currentPage - 1), currentPage === 1);
                    for (let p = 1; p <= totalPages; p++) {
                        if (totalPages > 7 && Math.abs(p - currentPage) > 2 && p !== 1 && p !== totalPages) continue;
                        addButton(String(p), p, false, p === currentPage);
                    }
                    addButton('>', Math.min(totalPages, currentPage + 1), currentPage === totalPages);
                }
            }
        };
        const resetFilter = () => { currentPage = 1; filterUsers(); };
        userSearch?.addEventListener('input', resetFilter);
        roleFilter?.addEventListener('change', resetFilter);
        statusFilter?.addEventListener('change', resetFilter);
        filterUsers();
    }

    // Profile validation
    const profileForm = $('#profileForm');
    if (profileForm) {
        const nameInput = $('#profileFullName');
        const error = $('[data-profile-error="name"]');
        const validateName = () => {
            const value = (nameInput?.value || '').trim().replace(/\s+/g, ' ');
            if (nameInput) nameInput.value = value;
            const valid = value.length >= 3 && value.length <= 50 && /^[\p{L}' .-]+$/u.test(value);
            if (error) {
                error.textContent = valid ? '' : 'Use 3 to 50 letters and normal name characters.';
                error.classList.toggle('show', !valid);
            }
            const wasInvalid = nameInput?.classList.contains('invalid');
            const wasValid = nameInput?.classList.contains('valid');
            nameInput?.classList.toggle('invalid', !valid);
            nameInput?.classList.toggle('valid', valid && Boolean(value));
            if (!valid && !wasInvalid) replayValidationAnimation(nameInput, 'validation-shake');
            if (valid && value && !wasValid) replayValidationAnimation(nameInput, 'validation-pop');
            return valid;
        };
        nameInput?.addEventListener('input', validateName);
        profileForm.addEventListener('submit', event => {
            if (!validateName()) event.preventDefault();
        });
    }

    const profilePhotoForm = $('#profilePhotoForm');
    if (profilePhotoForm) {
        const photoInput = $('#profilePhotoInput');
        const photoPreview = $('#profilePhotoPreview');
        const photoActions = $('#profilePhotoActions');
        const photoCancel = $('#profilePhotoCancel');
        const photoError = $('[data-profile-error="photo"]');
        const photoPicker = $('.profile-photo-picker', profilePhotoForm);
        const originalSrc = photoPreview?.dataset.originalSrc || photoPreview?.getAttribute('src') || '/images/default-avatar.png';

        const clearPhotoError = () => {
            if (photoError) {
                photoError.textContent = '';
                photoError.classList.remove('show');
            }
            photoInput?.classList.remove('invalid', 'valid');
        };

        const resetPhotoSelection = () => {
            if (photoInput) photoInput.value = '';
            if (photoPreview) photoPreview.src = originalSrc;
            if (photoActions) photoActions.hidden = true;
            photoPicker?.classList.remove('photo-pending');
            clearPhotoError();
        };

        const validatePhoto = () => {
            const file = photoInput?.files?.[0];
            let message = '';
            if (!file) message = 'Choose a profile picture first.';
            else if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) message = 'Use JPG, PNG or WEBP only.';
            else if (file.size > 2 * 1024 * 1024) message = 'Profile picture must be 2 MB or smaller.';
            if (photoError) {
                photoError.textContent = message;
                photoError.classList.toggle('show', Boolean(message));
            }
            photoInput?.classList.toggle('invalid', Boolean(message));
            photoInput?.classList.toggle('valid', !message && Boolean(file));
            return !message;
        };

        photoInput?.addEventListener('change', () => {
            const file = photoInput.files?.[0];
            if (!file) {
                resetPhotoSelection();
                return;
            }
            if (!validatePhoto()) {
                if (photoPreview) photoPreview.src = originalSrc;
                if (photoActions) photoActions.hidden = true;
                return;
            }
            if (photoPreview) {
                const reader = new FileReader();
                reader.onload = event => {
                    photoPreview.src = event.target.result;
                    photoPicker?.classList.add('photo-pending');
                    if (photoActions) photoActions.hidden = false;
                };
                reader.readAsDataURL(file);
            }
        });

        photoCancel?.addEventListener('click', resetPhotoSelection);

        profilePhotoForm.addEventListener('submit', event => {
            if (!validatePhoto()) event.preventDefault();
        });
    }

    const passwordForm = $('#passwordForm');
    if (passwordForm) {
        const current = $('#currentPassword');
        const next = $('#newPassword');
        const confirm = $('#confirmNewPassword');
        const checks = value => ({
            length: value.length >= 8,
            upper: /[A-Z]/.test(value),
            lower: /[a-z]/.test(value),
            number: /\d/.test(value),
            special: /[^A-Za-z0-9]/.test(value)
        });
        const showError = (key, message) => {
            const el = $(`[data-profile-error="${key}"]`);
            if (el) {
                el.textContent = message;
                el.classList.toggle('show', Boolean(message));
                el.setAttribute('aria-live', 'polite');
            }
        };
        const setPasswordFieldState = (input, valid) => {
            if (!input) return;
            const wasInvalid = input.classList.contains('invalid');
            const wasValid = input.classList.contains('valid');
            input.classList.toggle('invalid', !valid);
            input.classList.toggle('valid', valid && Boolean(input.value));
            if (!valid && !wasInvalid) replayValidationAnimation(input, 'validation-shake');
            if (valid && input.value && !wasValid) replayValidationAnimation(input, 'validation-pop');
        };
        const validatePassword = () => {
            const result = checks(next?.value || '');
            Object.entries(result).forEach(([key, passed]) => {
                $(`[data-profile-rule="${key}"]`)?.classList.toggle('passed', passed);
            });
            const valid = Object.values(result).every(Boolean);
            showError('new', valid ? '' : 'Use all five password requirements.');
            setPasswordFieldState(next, valid);
            return valid;
        };
        const validateConfirm = () => {
            const valid = Boolean(confirm?.value) && confirm.value === next?.value;
            showError('confirm', valid ? '' : 'Passwords do not match.');
            setPasswordFieldState(confirm, valid);
            return valid;
        };
        const validateCurrent = () => {
            const valid = Boolean(current?.value);
            showError('current', valid ? '' : 'Current password is required.');
            setPasswordFieldState(current, valid);
            return valid;
        };
        current?.addEventListener('input', validateCurrent);
        next?.addEventListener('input', () => { validatePassword(); if (confirm?.value) validateConfirm(); });
        confirm?.addEventListener('input', validateConfirm);
        passwordForm.addEventListener('submit', event => {
            if (![validateCurrent(), validatePassword(), validateConfirm()].every(Boolean)) event.preventDefault();
        });
    }

    $$('.site-password-toggle').forEach(button => {
        button.addEventListener('click', () => {
            const input = document.getElementById(button.dataset.passwordTarget);
            if (!input) return;
            const show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            const icon = button.querySelector('i');
            if (icon) icon.className = show ? 'bi bi-eye-slash' : 'bi bi-eye';
            button.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
        });
    });

    // Participant registration Free/Paid filter.
    const registrationList = $('#registrationList');
    const registrationPricingFilter = $('#registrationPricingFilter');
    if (registrationList && registrationPricingFilter) {
        const registrationCards = $$('.registration-card', registrationList);
        const registrationFilterCount = $('#registrationFilterCount');
        const registrationFilterEmpty = $('#registrationFilterEmpty');

        const filterRegistrations = () => {
            const pricing = registrationPricingFilter.value || 'ALL';
            const stage = $('#registrationStageFilter')?.value || 'ALL';
            let visible = 0;
            registrationCards.forEach(card => {
                const show = (pricing === 'ALL' || card.dataset.pricing === pricing)
                    && (stage === 'ALL' || card.dataset.stage === stage);
                card.style.display = show ? '' : 'none';
                if (show) visible++;
            });
            if (registrationFilterCount) {
                registrationFilterCount.textContent = `${visible} registration${visible === 1 ? '' : 's'}`;
            }
            if (registrationFilterEmpty) {
                registrationFilterEmpty.style.display = visible === 0 ? 'block' : 'none';
            }
        };

        registrationPricingFilter.addEventListener('change', filterRegistrations);
        $('#registrationStageFilter')?.addEventListener('change', filterRegistrations);
        filterRegistrations();
    }

    // Participant feedback modal
    const feedbackModal = $('#feedbackModal');
    const feedbackForm = $('#feedbackForm');
    const feedbackModalTitle = $('#feedbackModalTitle');
    const feedbackEventTitle = $('#feedbackEventTitle');
    const feedbackRating = $('#feedbackRating');
    const feedbackRatingLabel = $('#feedbackRatingLabel');
    const feedbackRatingError = $('#feedbackRatingError');
    const feedbackComment = $('#feedbackComment');
    const feedbackCommentCount = $('#feedbackCommentCount');
    const feedbackSubmitText = $('#feedbackSubmitText');
    const feedbackStarButtons = $$('#feedbackStars button');

    const feedbackRatingLabels = { 1: 'Poor', 2: 'Fair', 3: 'Good', 4: 'Very good', 5: 'Excellent' };

    const paintFeedbackStars = rating => {
        feedbackStarButtons.forEach(button => {
            button.classList.toggle('active', Number(button.dataset.rating || 0) <= rating);
        });
    };

    const updateFeedbackCounter = () => {
        if (feedbackCommentCount && feedbackComment) feedbackCommentCount.textContent = `${feedbackComment.value.length} / 500`;
    };

    const closeFeedbackModal = () => {
        feedbackModal?.classList.remove('open');
        feedbackModal?.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('modal-open');
        if (feedbackRatingError) feedbackRatingError.textContent = '';
    };

    $$('.feedback-open-button').forEach(button => {
        button.addEventListener('click', () => {
            if (!feedbackModal || !feedbackForm || !feedbackRating || !feedbackComment) return;
            const eventId = button.dataset.eventId || '';
            const savedRating = Number(button.dataset.rating || 0);
            const editing = savedRating > 0;
            feedbackForm.action = `/participant/events/${encodeURIComponent(eventId)}/feedback`;
            feedbackRating.value = String(savedRating);
            feedbackComment.value = button.dataset.comment || '';
            if (feedbackEventTitle) feedbackEventTitle.textContent = button.dataset.eventTitle || 'Event';
            if (feedbackModalTitle) feedbackModalTitle.textContent = editing ? 'Edit your feedback' : 'Rate your experience';
            if (feedbackSubmitText) feedbackSubmitText.textContent = editing ? 'Save Changes' : 'Submit Feedback';
            if (feedbackRatingLabel) feedbackRatingLabel.textContent = savedRating > 0 ? `${savedRating}/5 · ${feedbackRatingLabels[savedRating]}` : 'Select 1 to 5 stars';
            if (feedbackRatingError) feedbackRatingError.textContent = '';
            paintFeedbackStars(savedRating);
            updateFeedbackCounter();
            feedbackModal.classList.add('open');
            feedbackModal.setAttribute('aria-hidden', 'false');
            document.body.classList.add('modal-open');
        });
    });

    feedbackStarButtons.forEach(button => {
        const value = Number(button.dataset.rating || 0);
        button.addEventListener('mouseenter', () => paintFeedbackStars(value));
        button.addEventListener('mouseleave', () => paintFeedbackStars(Number(feedbackRating?.value || 0)));
        button.addEventListener('click', () => {
            if (!feedbackRating) return;
            feedbackRating.value = String(value);
            paintFeedbackStars(value);
            if (feedbackRatingLabel) feedbackRatingLabel.textContent = `${value}/5 · ${feedbackRatingLabels[value]}`;
            if (feedbackRatingError) feedbackRatingError.textContent = '';
        });
    });

    feedbackComment?.addEventListener('input', updateFeedbackCounter);
    $('#closeFeedbackModal')?.addEventListener('click', closeFeedbackModal);
    $('#feedbackCancel')?.addEventListener('click', closeFeedbackModal);
    feedbackModal?.addEventListener('click', event => { if (event.target === feedbackModal) closeFeedbackModal(); });

    feedbackForm?.addEventListener('submit', event => {
        const rating = Number(feedbackRating?.value || 0);
        if (rating < 1 || rating > 5) {
            event.preventDefault();
            if (feedbackRatingError) feedbackRatingError.textContent = 'Choose a rating from 1 to 5 stars.';
            const stars = $('#feedbackStars');
            if (stars) replayValidationAnimation(stars, 'validation-shake');
        }
    });

    // Feedback rating sort - highest/lowest without hiding event groups.
    const sortByRating = (container, items, direction) => {
        const sorted = [...items].sort((a, b) => {
            const ratingA = Number(a.dataset.rating ?? 0);
            const ratingB = Number(b.dataset.rating ?? 0);
            return direction === 'LOW' ? ratingA - ratingB : ratingB - ratingA;
        });
        sorted.forEach(item => container.appendChild(item));
        return sorted;
    };

    const feedbackAdminSort = $('#feedbackAdminSort');
    const feedbackAdminList = $('#feedbackAdminList');
    const feedbackAdminCount = $('#feedbackAdminCount');
    if (feedbackAdminSort && feedbackAdminList) {
        const groups = $$('.admin-feedback-event-group', feedbackAdminList);
        const sortAdminFeedback = () => {
            const sorted = sortByRating(feedbackAdminList, groups, feedbackAdminSort.value);
            sorted.forEach((group, index) => { group.open = index === 0; });
            if (feedbackAdminCount) {
                const reviewCount = groups.reduce((total, group) => total + Number(group.dataset.reviewCount || 0), 0);
                const eventLabel = groups.length === 1 ? 'rated event' : 'rated events';
                const reviewLabel = reviewCount === 1 ? 'review' : 'reviews';
                feedbackAdminCount.textContent = `${groups.length} ${eventLabel} · ${reviewCount} ${reviewLabel}`;
            }
        };
        feedbackAdminSort.addEventListener('change', sortAdminFeedback);
        sortAdminFeedback();
    }

    // Organizer feedback: completed-event selector and exact review navigation.
    const organizerFeedbackEventFilter = $('#organizerFeedbackEventFilter');
    const organizerPerformanceItems = $$('.organizer-feedback-performance-item');
    const organizerReviewGroups = $$('.organizer-feedback-review-group');
    const organizerFeedbackFilterEmpty = $('#organizerFeedbackFilterEmpty');

    const filterOrganizerFeedback = () => {
        if (!organizerFeedbackEventFilter) return;
        const selected = organizerFeedbackEventFilter.value || 'ALL';
        organizerPerformanceItems.forEach(item => {
            item.hidden = selected !== 'ALL' && item.dataset.feedbackEventId !== selected;
        });
        let visibleReviewGroups = 0;
        organizerReviewGroups.forEach(group => {
            const visible = selected === 'ALL' || group.dataset.feedbackEventId === selected;
            group.hidden = !visible;
            if (visible) visibleReviewGroups++;
            if (visible && selected !== 'ALL') group.open = true;
        });
        if (organizerFeedbackFilterEmpty) {
            organizerFeedbackFilterEmpty.hidden = selected === 'ALL' || visibleReviewGroups > 0;
        }
    };

    organizerFeedbackEventFilter?.addEventListener('change', filterOrganizerFeedback);
    filterOrganizerFeedback();

    $$('.feedback-event-review-link').forEach(link => {
        link.addEventListener('click', event => {
            const href = link.getAttribute('href') || '';
            if (!href.startsWith('#')) return;
            const target = document.querySelector(href);
            if (!target?.matches('.feedback-event-review-group')) return;
            event.preventDefault();
            const eventId = target.dataset.feedbackEventId || link.dataset.feedbackEventId || '';
            if (organizerFeedbackEventFilter && eventId) {
                organizerFeedbackEventFilter.value = eventId;
                filterOrganizerFeedback();
            }
            target.hidden = false;
            target.open = true;
            history.replaceState(null, '', `${window.location.pathname}${window.location.search}${href}`);
            target.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    });

    // Contact request review panel close behavior.
    const contactReviewDetails = $$('.contact-review-details');
    const closeContactReview = details => {
        if (details) details.open = false;
    };

    contactReviewDetails.forEach(details => {
        const closeButton = $('.contact-review-close', details);
        closeButton?.addEventListener('click', event => {
            event.preventDefault();
            event.stopPropagation();
            closeContactReview(details);
        });

        details.addEventListener('toggle', () => {
            if (!details.open) return;
            contactReviewDetails.forEach(other => {
                if (other !== details) other.open = false;
            });
        });
    });

    document.addEventListener('click', event => {
        contactReviewDetails.forEach(details => {
            if (details.open && !details.contains(event.target)) closeContactReview(details);
        });
    });

    // Open/position the exact item when a notification links to a specific target.
    if (window.location.hash) {
        const targetedItem = document.querySelector(window.location.hash);
        if (targetedItem?.matches('.feedback-event-review-group')) {
            const eventId = targetedItem.dataset.feedbackEventId || '';
            if (organizerFeedbackEventFilter && eventId) {
                organizerFeedbackEventFilter.value = eventId;
                filterOrganizerFeedback();
            }
            targetedItem.hidden = false;
            targetedItem.open = true;
        }
        if (targetedItem?.matches('.feedback-review-item')) {
            const group = targetedItem.closest('.feedback-event-review-group');
            const eventId = group?.dataset.feedbackEventId || '';
            if (organizerFeedbackEventFilter && eventId) {
                organizerFeedbackEventFilter.value = eventId;
                filterOrganizerFeedback();
            }
            if (group) {
                group.hidden = false;
                group.open = true;
            }
            targetedItem.classList.add('targeted-feedback');
            window.setTimeout(() => targetedItem.scrollIntoView({ behavior: 'smooth', block: 'center' }), 80);
        }
        if (targetedItem?.matches('tr[id^="request-"]')) {
            const review = targetedItem.querySelector('.contact-review-details');
            if (review) review.open = true;
        }
    }

    // Professional searchable/filterable/paginated operation tables (bookings and past events)
    $$('[data-smart-table]').forEach(root => {
        const body = $('[data-table-body]', root);
        if (!body) return;

        const rows = $$('tr', body);
        const search = $('[data-table-search]', root);
        const filters = $$('[data-table-filter]', root);
        const pageSizeSelect = $('[data-table-page-size]', root);
        const count = $('[data-table-count]', root);
        const range = $('[data-table-range]', root);
        const pagination = $('[data-table-pagination]', root);
        const empty = $('[data-table-empty]', root);
        const singular = root.dataset.rowLabel || 'item';
        const plural = root.dataset.rowLabelPlural || `${singular}s`;
        let page = 1;

        const filterRows = () => {
            const query = (search?.value || '').trim().toLowerCase();
            return rows.filter(row => {
                const searchable = (row.dataset.search || row.textContent || '').toLowerCase();
                if (query && !searchable.includes(query)) return false;

                return filters.every(filter => {
                    const value = filter.value || 'ALL';
                    if (value === 'ALL') return true;
                    const key = filter.dataset.tableFilter;
                    return (row.dataset[key] || '') === value;
                });
            });
        };

        const makePageButton = (label, targetPage, active = false, disabled = false, ariaLabel = '') => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = `table-page-button${active ? ' active' : ''}`;
            button.textContent = label;
            button.disabled = disabled;
            if (ariaLabel) button.setAttribute('aria-label', ariaLabel);
            button.addEventListener('click', () => {
                page = targetPage;
                render();
                root.scrollIntoView({ behavior: 'smooth', block: 'start' });
            });
            return button;
        };

        const renderPagination = (pageCount) => {
            if (!pagination) return;
            pagination.innerHTML = '';
            if (pageCount <= 1) return;

            pagination.appendChild(makePageButton('‹', Math.max(1, page - 1), false, page === 1, 'Previous page'));

            const pageNumbers = [];
            if (pageCount <= 7) {
                for (let n = 1; n <= pageCount; n++) pageNumbers.push(n);
            } else {
                pageNumbers.push(1);
                if (page > 4) pageNumbers.push('…');
                const start = Math.max(2, page - 1);
                const end = Math.min(pageCount - 1, page + 1);
                for (let n = start; n <= end; n++) pageNumbers.push(n);
                if (page < pageCount - 3) pageNumbers.push('…');
                pageNumbers.push(pageCount);
            }

            pageNumbers.forEach(value => {
                if (value === '…') {
                    const span = document.createElement('span');
                    span.className = 'table-page-ellipsis';
                    span.textContent = '…';
                    pagination.appendChild(span);
                } else {
                    pagination.appendChild(makePageButton(String(value), value, value === page));
                }
            });

            pagination.appendChild(makePageButton('›', Math.min(pageCount, page + 1), false, page === pageCount, 'Next page'));
        };

        const render = () => {
            const filtered = filterRows();
            const selectedSize = pageSizeSelect?.value || '10';
            const pageSize = selectedSize === 'ALL' ? Math.max(filtered.length, 1) : Math.max(Number(selectedSize) || 10, 1);
            const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize));
            page = Math.min(Math.max(page, 1), pageCount);
            const start = (page - 1) * pageSize;
            const end = Math.min(start + pageSize, filtered.length);
            const visibleRows = new Set(filtered.slice(start, end));

            rows.forEach(row => {
                row.style.display = visibleRows.has(row) ? '' : 'none';
            });

            if (count) count.textContent = `${filtered.length} ${filtered.length === 1 ? singular : plural}`;
            if (range) {
                range.textContent = filtered.length === 0
                    ? `Showing 0 of 0 ${plural}`
                    : `Showing ${start + 1}–${end} of ${filtered.length} ${filtered.length === 1 ? singular : plural}`;
            }
            if (empty) empty.style.display = filtered.length === 0 ? 'block' : 'none';
            renderPagination(pageCount);
        };

        search?.addEventListener('input', () => { page = 1; render(); });
        filters.forEach(filter => filter.addEventListener('change', () => { page = 1; render(); }));
        pageSizeSelect?.addEventListener('change', () => { page = 1; render(); });
        render();
    });

    $$('.program-time-picker').forEach(input => {
        if (!window.flatpickr) return;
        flatpickr(input, {
            enableTime: true,
            noCalendar: true,
            dateFormat: 'H:i',
            altInput: true,
            altFormat: 'h:i K',
            minuteIncrement: 5,
            disableMobile: true,
            allowInput: false
        });
    });

    // Event program editors and speaker photo previews
    $$('.program-edit-toggle').forEach(button => {
        button.addEventListener('click', () => {
            const container = button.closest('.program-session-content, .program-speaker-copy, .ticket-type-card-copy, .sponsor-card-copy, .gallery-card-copy');
            const editor = container?.querySelector('.program-inline-editor');
            if (!editor) return;
            const willOpen = editor.hasAttribute('hidden');
            $$('.program-inline-editor').forEach(other => {
                if (other !== editor) other.setAttribute('hidden', '');
            });
            if (willOpen) {
                editor.removeAttribute('hidden');
                editor.querySelector('input, select, textarea')?.focus();
            } else {
                editor.setAttribute('hidden', '');
            }
        });
    });

    $$('.program-edit-cancel').forEach(button => {
        button.addEventListener('click', () => {
            button.closest('.program-inline-editor')?.setAttribute('hidden', '');
        });
    });

    $$('.speaker-photo-input').forEach(input => {
        input.addEventListener('change', () => {
            const file = input.files?.[0];
            const preview = input.closest('.speaker-photo-upload')?.querySelector('.speaker-photo-preview');
            if (!file || !preview) return;
            if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 2 * 1024 * 1024) {
                input.value = '';
                return;
            }
            const reader = new FileReader();
            reader.onload = event => { preview.src = event.target?.result || preview.src; };
            reader.readAsDataURL(file);
        });
    });

    const bindProgramImagePreview = (selector, uploadSelector, previewSelector, placeholderSelector) => {
        $$(selector).forEach(input => {
            const upload = input.closest(uploadSelector);
            const preview = upload?.querySelector(previewSelector);
            const placeholder = upload?.querySelector(placeholderSelector);
            if (!upload || !preview || !placeholder) return;

            const reset = () => {
                upload.classList.remove('has-preview');
                preview.hidden = true;
                preview.removeAttribute('src');
                placeholder.hidden = false;
            };
            reset();

            input.addEventListener('change', () => {
                const file = input.files?.[0];
                reset();
                if (!file) return;
                if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 2 * 1024 * 1024) {
                    input.value = '';
                    return;
                }
                const reader = new FileReader();
                reader.onload = () => {
                    const data = reader.result;
                    if (input.files?.[0] !== file || typeof data !== 'string' || !data.startsWith('data:image/')) return;
                    const image = new Image();
                    image.onload = () => {
                        if (input.files?.[0] !== file) return;
                        preview.src = data;
                        preview.hidden = false;
                        placeholder.hidden = true;
                        upload.classList.add('has-preview');
                    };
                    image.src = data;
                };
                reader.readAsDataURL(file);
            });
        });
    };

    bindProgramImagePreview('.sponsor-logo-input', '.sponsor-logo-upload', '.sponsor-logo-preview', '.sponsor-logo-placeholder');

    // Coupon management date pickers and inline editors
    $$('.coupon-date-picker').forEach(input => {
        if (!window.flatpickr) return;
        flatpickr(input, {
            minDate: 'today',
            dateFormat: 'Y-m-d',
            altInput: true,
            altFormat: 'd M Y',
            allowInput: false,
            disableMobile: true,
            monthSelectorType: 'static',
            prevArrow: '<i class="bi bi-chevron-left"></i>',
            nextArrow: '<i class="bi bi-chevron-right"></i>'
        });
    });

    $$('input[name="code"], .coupon-code-input').forEach(input => {
        input.addEventListener('input', () => {
            const start = input.selectionStart;
            const end = input.selectionEnd;
            input.value = input.value.toUpperCase().replace(/[^A-Z0-9_-]/g, '');
            try { input.setSelectionRange(start, end); } catch (_) { }
        });
    });

    $$('.coupon-edit-toggle').forEach(button => {
        button.addEventListener('click', () => {
            const editor = document.getElementById(button.dataset.target || '');
            if (!editor) return;
            const willOpen = editor.hasAttribute('hidden');
            $$('.coupon-inline-editor').forEach(other => {
                if (other !== editor) other.setAttribute('hidden', '');
            });
            if (willOpen) {
                editor.removeAttribute('hidden');
                editor.querySelector('input, select')?.focus();
            } else {
                editor.setAttribute('hidden', '');
            }
        });
    });

    $$('.coupon-edit-cancel').forEach(button => {
        button.addEventListener('click', () => button.closest('.coupon-inline-editor')?.setAttribute('hidden', ''));
    });

    // Participant coupon validation and live price preview
    $$('[data-coupon-form]').forEach(form => {
        const input = $('.coupon-code-input', form);
        const applyButton = $('.coupon-apply-button', form);
        const result = $('.coupon-preview-result', form);
        if (!input || !applyButton || !result) return;

        const resetPreview = () => {
            result.className = 'coupon-preview-result';
            result.textContent = '';
        };

        input.addEventListener('input', resetPreview);

        applyButton.addEventListener('click', async () => {
            const code = input.value.trim().toUpperCase();
            if (!code) {
                result.className = 'coupon-preview-result error';
                result.innerHTML = '<i class="bi bi-exclamation-circle"></i><span>Enter a coupon code first.</span>';
                input.focus();
                return;
            }

            const eventId = form.dataset.eventId || '';
            const ticketId = form.dataset.ticketId || '';
            const params = new URLSearchParams({ couponCode: code });
            if (ticketId) params.set('ticketTypeId', ticketId);

            applyButton.disabled = true;
            applyButton.innerHTML = '<span class="button-spinner small"></span><span>Checking</span>';

            try {
                const response = await fetch(`/participant/events/${encodeURIComponent(eventId)}/coupon-preview?${params.toString()}`, {
                    headers: { 'Accept': 'application/json' }
                });
                const data = await response.json();
                if (!response.ok || !data.valid) throw new Error(data.message || 'Coupon could not be applied.');

                result.className = 'coupon-preview-result success';
                const title = document.createElement('strong');
                title.textContent = `${data.couponCode} applied`;
                const breakdown = document.createElement('div');
                breakdown.className = 'coupon-price-breakdown';
                for (const [tag, value] of [['span', data.originalPrice], ['em', `- ${data.discount}`], ['strong', data.finalPrice]]) {
                    const item = document.createElement(tag);
                    item.textContent = value;
                    breakdown.appendChild(item);
                }
                result.replaceChildren(title, breakdown);
            } catch (error) {
                result.className = 'coupon-preview-result error';
                result.textContent = error.message || 'Coupon could not be applied.';
            } finally {
                applyButton.disabled = false;
                applyButton.innerHTML = '<span>Apply</span>';
            }
        });
    });

    // Contact form message counter
    const contactMessage = $('#contactMessage');
    const contactMessageCount = $('#contactMessageCount');
    const updateContactMessageCount = () => {
        if (contactMessage && contactMessageCount) contactMessageCount.textContent = `${contactMessage.value.length} / 1500`;
    };
    contactMessage?.addEventListener('input', updateContactMessageCount);
    updateContactMessageCount();


    // Keep the original gallery placeholder visible until a selected image loads.
    bindProgramImagePreview('.gallery-image-input', '.gallery-image-upload', '.gallery-upload-preview', '.gallery-upload-placeholder');

    // Public event gallery lightbox
    const galleryItems = $$('.public-gallery-item');
    const galleryLightbox = $('#galleryLightbox');
    const galleryLightboxImage = $('#galleryLightboxImage');
    const galleryLightboxCaption = $('#galleryLightboxCaption');
    const galleryLightboxClose = $('#galleryLightboxClose');
    const galleryLightboxPrev = $('#galleryLightboxPrev');
    const galleryLightboxNext = $('#galleryLightboxNext');
    let galleryIndex = 0;

    const renderGalleryLightbox = () => {
        const item = galleryItems[galleryIndex];
        if (!item || !galleryLightboxImage) return;
        galleryLightboxImage.src = item.dataset.gallerySrc || '';
        galleryLightboxImage.alt = item.dataset.galleryCaption || 'Event gallery image';
        if (galleryLightboxCaption) {
            galleryLightboxCaption.textContent = item.dataset.galleryCaption || '';
            galleryLightboxCaption.hidden = !item.dataset.galleryCaption;
        }
        const multiple = galleryItems.length > 1;
        if (galleryLightboxPrev) galleryLightboxPrev.hidden = !multiple;
        if (galleryLightboxNext) galleryLightboxNext.hidden = !multiple;
    };

    const openGalleryLightbox = index => {
        if (!galleryLightbox || !galleryItems.length) return;
        galleryIndex = index;
        renderGalleryLightbox();
        galleryLightbox.classList.add('open');
        galleryLightbox.setAttribute('aria-hidden', 'false');
        document.body.style.overflow = 'hidden';
    };

    const closeGalleryLightbox = () => {
        if (!galleryLightbox) return;
        galleryLightbox.classList.remove('open');
        galleryLightbox.setAttribute('aria-hidden', 'true');
        document.body.style.overflow = '';
    };

    galleryItems.forEach((item, index) => item.addEventListener('click', () => openGalleryLightbox(index)));
    galleryLightboxClose?.addEventListener('click', closeGalleryLightbox);
    galleryLightboxPrev?.addEventListener('click', () => {
        galleryIndex = (galleryIndex - 1 + galleryItems.length) % galleryItems.length;
        renderGalleryLightbox();
    });
    galleryLightboxNext?.addEventListener('click', () => {
        galleryIndex = (galleryIndex + 1) % galleryItems.length;
        renderGalleryLightbox();
    });
    galleryLightbox?.addEventListener('click', event => {
        if (event.target === galleryLightbox) closeGalleryLightbox();
    });

    // Admin / Organizer analytics charts
    const initAnalyticsCharts = () => {
        const analytics = window.eventflowAnalytics;
        if (!analytics || !window.Chart) return;

        const text = '#9c9c93';
        const grid = 'rgba(255,255,255,.055)';
        const gold = '#ffd817';
        const goldSoft = 'rgba(255,216,23,.18)';
        const tooltip = { backgroundColor: '#141410', titleColor: '#f2f2e8', bodyColor: '#b8b8ae', borderColor: 'rgba(255,216,23,.18)', borderWidth: 1 };

        Chart.defaults.color = text;
        Chart.defaults.font.family = 'Inter, system-ui, sans-serif';

        const trend = $('#registrationTrendChart');
        if (trend) new Chart(trend, {
            type: 'line',
            data: { labels: analytics.trendLabels, datasets: [{ data: analytics.trendValues, borderColor: gold, backgroundColor: goldSoft, fill: true, tension: .38, borderWidth: 2.2, pointRadius: 3, pointHoverRadius: 5 }] },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false }, tooltip }, scales: { x: { grid: { display: false } }, y: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: grid } } } }
        });

        const category = $('#categoryChart');
        if (category) new Chart(category, {
            type: 'doughnut',
            data: { labels: analytics.categoryLabels, datasets: [{ data: analytics.categoryValues, backgroundColor: ['#ffd817','#c7a900','#8e7a18','#675b23','#444333','#2f2f29','#77745e','#b6a95a'], borderColor: '#11110d', borderWidth: 3 }] },
            options: { responsive: true, maintainAspectRatio: false, cutout: '68%', plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, usePointStyle: true, padding: 14 } }, tooltip } }
        });

        const topEvents = $('#topEventsChart');
        if (topEvents) new Chart(topEvents, {
            type: 'bar',
            data: { labels: analytics.topEventLabels, datasets: [{ data: analytics.topEventValues, backgroundColor: goldSoft, borderColor: gold, borderWidth: 1.3, borderRadius: 8 }] },
            options: { indexAxis: 'y', responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false }, tooltip }, scales: { x: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: grid } }, y: { grid: { display: false }, ticks: { callback: function(value) { const label = this.getLabelForValue(value); return label.length > 22 ? label.slice(0, 22) + '…' : label; } } } } }
        });
    };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initAnalyticsCharts, { once: true });
    else setTimeout(initAnalyticsCharts, 0);

    // Loading state for normal forms after their validation passes
    $$('form[data-loading]').forEach(form => {
        form.addEventListener('submit', event => {
            if (!event.defaultPrevented) setFormLoading(form);
        });
    });

    document.addEventListener('keydown', event => {
        if (event.key !== 'Escape') return;
        hideEventModal();
        closeFeedbackModal();
        closeGalleryLightbox();
        contactReviewDetails.forEach(closeContactReview);
        if (confirmModal?.classList.contains('open')) closeConfirm();
        notificationPanel?.classList.remove('open');
        sidebar?.classList.remove('open');
    });
})();
