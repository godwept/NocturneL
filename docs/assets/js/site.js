(() => {
  document.documentElement.classList.add('js');

  const toggle = document.querySelector('.nav-toggle');
  const navigation = document.querySelector('.site-nav');

  const closeNavigation = () => {
    if (!toggle || !navigation) return;
    toggle.setAttribute('aria-expanded', 'false');
    navigation.classList.remove('is-open');
  };

  if (toggle && navigation) {
    toggle.addEventListener('click', () => {
      const expanded = toggle.getAttribute('aria-expanded') === 'true';
      toggle.setAttribute('aria-expanded', String(!expanded));
      navigation.classList.toggle('is-open', !expanded);
    });

    navigation.querySelectorAll('a').forEach((link) => link.addEventListener('click', closeNavigation));
    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape') {
        closeNavigation();
        toggle.focus();
      }
    });
  }

  const manual = document.querySelector('.manual-content');
  const headingSelector = 'h2[id], h3[id]';
  manual?.querySelectorAll(headingSelector).forEach((heading) => {
    const anchor = document.createElement('a');
    anchor.className = 'heading-anchor';
    anchor.href = `#${heading.id}`;
    anchor.setAttribute('aria-label', `Link to ${heading.textContent.trim()}`);
    anchor.textContent = '#';
    heading.append(anchor);
  });
})();
