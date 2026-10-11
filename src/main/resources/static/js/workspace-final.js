/* EventFlow final workspace behavior. Actual backend requests remain handled by site.js. */
(() => {
  'use strict';
  const map = {
    'grid-1x2':'grid',grid:'grid',search:'search',bell:'bell','bell-slash':'bell',
    'calendar-event':'calendar','calendar3':'calendar','calendar2-event':'calendar','calendar2-week':'calendar','calendar-check':'calendar','calendar2-check':'calendar','calendar2-plus':'calendar','calendar2-x':'calendar','calendar-plus':'calendar',
    'clock':'clock','clock-history':'clock','hourglass-split':'clock','geo-alt':'map-pin','geo-alt-fill':'map-pin',
    'people':'users','person-plus':'users','person-badge':'user','person-mic':'user','person-video3':'user','person':'user','person-circle':'user','person-slash':'user','person-check':'user',
    'ticket-perforated':'ticket','ticket-detailed':'ticket',headset:'headphones',
    'box-arrow-right':'log-out','box-arrow-in-right':'log-out','box-arrow-up-right':'arrow-right',
    'chevron-right':'chevron-right','chevron-down':'chevron-down','chevron-left':'arrow-left','arrow-left':'arrow-left','arrow-up-right':'arrow-right',
    'plus-lg':'plus','plus-square':'plus',check2:'check','check2-all':'check','check2-circle':'check-circle','check-circle':'check-circle','check-circle-fill':'check-circle',
    'x-lg':'x','x-circle':'x','eye-slash':'eye-off',list:'menu','list-ol':'menu',
    trash3:'trash',pencil:'edit','pencil-square':'edit',eye:'eye',
    'lock-fill':'lock','shield-lock-fill':'shield','shield-lock':'shield','shield-check':'shield',
    'envelope-fill':'mail',envelope:'mail',send:'arrow-right',
    'star-fill':'star',star:'star',stars:'star',award:'badge',trophy:'trophy',
    'cash-stack':'tag','tag-fill':'tag',tag:'tag',percent:'tag',
    'chat-left-text':'message','chat-square-text':'message','chat-square-heart':'message','chat-square':'message',whatsapp:'message',
    'cloud-arrow-up':'upload',download:'ticket',receipt:'clipboard',camera:'image','camera-fill':'image',images:'image',image:'image',
    'graph-up-arrow':'chart','pie-chart':'chart',building:'briefcase',
    'lightning-charge':'star','sliders':'settings',compass:'search','sort-down-alt':'filter',funnel:'filter',
    inbox:'mail',telephone:'headphones','circle-fill':'check-circle','info-circle':'alert-circle',
    'exclamation-circle-fill':'alert-circle','exclamation-triangle':'alert-circle',
    'arrows-fullscreen':'grid','moon-stars':'moon'
  };
  // Preserve original <i> elements for existing JS and rating interactions.
  // Render bundled vector icons *inside* the elements, without depending on an icon font.
  const patchIcon = icon => {
    const bi = [...icon.classList].find(n => n.startsWith('bi-'));
    if (!bi) return;
    const name = bi.slice(3);
    const id = map[name] || (name.includes('eye-slash') ? 'eye-off' : 'grid');
    let svg = icon.querySelector('svg.ef-icon');
    if (!svg) {
      svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      svg.classList.add('ef-icon');
      svg.setAttribute('aria-hidden','true');
      svg.setAttribute('focusable','false');
      const use=document.createElementNS('http://www.w3.org/2000/svg','use');
      svg.appendChild(use);
      icon.appendChild(svg);
    }
    const href=`/images/ef-icons.svg#i-${id==='moon'?'star':id}`;
    svg.querySelector('use').setAttribute('href',href);
  };
  const observedIcons = new WeakSet();
  const classObserver = new MutationObserver(records => {
    for (const record of records) if (record.target.matches?.('i.bi')) patchIcon(record.target);
  });
  const registerIcon = icon => {
    patchIcon(icon);
    if (!observedIcons.has(icon)) {
      observedIcons.add(icon);
      classObserver.observe(icon, { attributes: true, attributeFilter: ['class'] });
    }
  };
  const substituteIcons = (scope = document) => {
    if (scope.matches?.('i.bi')) registerIcon(scope);
    scope.querySelectorAll?.('i.bi').forEach(registerIcon);
  };
  substituteIcons();
  // Watch new nodes only; watch icon classes on the icon itself rather than
  // observing every class mutation across the entire UI.
  new MutationObserver(records => {
    for (const record of records) {
      for (const node of record.addedNodes) {
        if (node.nodeType !== 1 || node.matches?.('svg.ef-icon') || node.closest?.('svg.ef-icon')) continue;
        substituteIcons(node);
      }
    }
  }).observe(document.body, { childList: true, subtree: true });

  const menu = document.getElementById('sidebarToggle');
  const sidebar = document.getElementById('appSidebar');
  if (menu && sidebar) {
    const overlay = document.createElement('div');
    overlay.className='ef-sidebar-backdrop';
    overlay.setAttribute('aria-hidden','true');
    document.body.appendChild(overlay);
    const sync = () => {
      const open = sidebar.classList.contains('open');
      overlay.classList.toggle('open',open);
      menu.setAttribute('aria-expanded',String(open));
      menu.setAttribute('aria-label',open?'Close menu':'Open menu');
    };
    const mobileObserver = new MutationObserver(sync);
    mobileObserver.observe(sidebar,{attributes:true,attributeFilter:['class']});
    overlay.addEventListener('click', () => { sidebar.classList.remove('open');sync();menu.focus(); });
    document.querySelectorAll('.sidebar-nav a,.sidebar-logout button').forEach(a => a.addEventListener('click',()=>{sidebar.classList.remove('open');sync();}));
    document.addEventListener('keydown', e => {
      if (e.key==='Escape' && sidebar.classList.contains('open')) {sidebar.classList.remove('open');sync();menu.focus();}
    });
    window.addEventListener('resize',() => {if(innerWidth>760){sidebar.classList.remove('open');sync();}});
    sync();
  }

  // Existing registration cancellation and feedback forms are not replaced.
  // Stage filter is handled together with the existing price filter in site.js.
  const notificationButton=document.getElementById('notificationToggle');
  const notificationPanel=document.getElementById('notificationPanel');
  if (notificationButton && notificationPanel) {
    notificationButton.setAttribute('aria-haspopup','true');
    notificationButton.setAttribute('aria-controls','notificationPanel');
    notificationButton.setAttribute('aria-expanded','false');
    const syncPanel=()=>notificationButton.setAttribute('aria-expanded',String(notificationPanel.classList.contains('open')));
    new MutationObserver(syncPanel).observe(notificationPanel,{attributes:true,attributeFilter:['class']});
    document.addEventListener('keydown',e=>{if(e.key==='Escape'&&notificationPanel.classList.contains('open')) {notificationPanel.classList.remove('open');notificationButton.focus();}});
    syncPanel();
  }
})();
