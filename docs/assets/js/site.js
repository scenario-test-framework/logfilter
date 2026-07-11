(() => {
  const root = document.documentElement;
  const languageButtons = document.querySelectorAll('[data-lang-button]');
  const translatableText = document.querySelectorAll('[data-ja][data-en]');
  const translatableLabels = document.querySelectorAll('[data-aria-ja][data-aria-en]');
  const translatableLinks = document.querySelectorAll('[data-href-ja][data-href-en]');
  const copyStatus = document.querySelector('.copy-status');

  const metadata = {
    ja: {
      title: 'logfilter — 必要なログだけ、瞬時に。',
      description: '大容量ログから、期間・レベル・文字列・正規表現で必要なイベントを抽出するオープンソースCLI。',
      copied: 'コマンドをコピーしました。',
      failed: 'コピーできませんでした。コマンドを選択してコピーしてください。',
    },
    en: {
      title: 'logfilter — Find the logs that matter. Instantly.',
      description: 'An open-source CLI for extracting events from large log files by time range, level, text, or regular expression.',
      copied: 'Command copied.',
      failed: 'Could not copy. Select the command and copy it manually.',
    },
  };

  const setLanguage = (language) => {
    const lang = language === 'en' ? 'en' : 'ja';
    root.lang = lang;
    document.title = metadata[lang].title;
    document.querySelector('meta[name="description"]')?.setAttribute('content', metadata[lang].description);

    translatableText.forEach((element) => {
      element.textContent = element.dataset[lang];
    });
    translatableLabels.forEach((element) => {
      element.setAttribute('aria-label', element.dataset[`aria${lang[0].toUpperCase()}${lang.slice(1)}`]);
    });
    translatableLinks.forEach((element) => {
      element.href = element.dataset[`href${lang[0].toUpperCase()}${lang.slice(1)}`];
    });
    languageButtons.forEach((button) => {
      button.setAttribute('aria-pressed', String(button.dataset.langButton === lang));
    });
    try { localStorage.setItem('logfilter-language', lang); } catch (_) { /* Preferences are optional. */ }
  };

  languageButtons.forEach((button) => {
    button.addEventListener('click', () => setLanguage(button.dataset.langButton));
  });

  document.querySelectorAll('[data-copy]').forEach((button) => {
    button.addEventListener('click', async () => {
      const lang = root.lang === 'en' ? 'en' : 'ja';
      try {
        await navigator.clipboard.writeText(button.dataset.copy);
        copyStatus.textContent = metadata[lang].copied;
      } catch (_) {
        copyStatus.textContent = metadata[lang].failed;
      }
    });
  });

  let preferredLanguage = 'ja';
  try { preferredLanguage = localStorage.getItem('logfilter-language') || 'ja'; } catch (_) { /* Use Japanese. */ }
  setLanguage(preferredLanguage);
})();
