(() => {
  const translations = {
    ja: {
      title: 'logfilter — 必要なログだけ、瞬時に。',
      description: '大容量ログから、期間・レベル・文字列・正規表現で必要なイベントを抽出するオープンソースCLI。',
      ogDescription: '大容量ログから必要なイベントを抽出する、Go製のオープンソースCLI。',
      ogLocale: 'ja_JP',
      docsUrl: 'https://github.com/scenario-test-framework/logfilter/blob/master/README.ja.md',
      skipLink: '本文へ移動',
      primaryNav: 'メインナビゲーション',
      navFeatures: '機能',
      navUsage: '使い方',
      navInstall: '導入',
      languageGroup: '表示言語',
      switchJapanese: '日本語を選択',
      switchEnglish: '英語を選択',
      heroTitleLine1: '必要なログだけ',
      heroTitleLine2: '瞬時に',
      heroLead: '大容量ログから、期間・レベル・文字列・正規表現で必要なイベントを抽出。スタックトレースもひとつのイベントとして保持します。',
      getStarted: '今すぐ使う',
      viewGithub: 'GitHubを見る',
      distributionLabel: '配布形式',
      distributionValue: '単一バイナリ',
      eventsLabel: 'イベント',
      eventsValue: 'マルチライン対応',
      encodingsLabel: '文字コード',
      demoCaption: '大量のログから条件に合うイベントを抽出する logfilter の実行例',
      demoFilter: 'ログレベル: WARN, ERROR',
      demoOutput: '出力: filtered.log',
      introTitle: 'ログ調査のノイズを、判断できる情報に。',
      introLead: '巨大なファイルをエディタで開き、検索を繰り返し、スタックトレースを手作業で追う必要はありません。logfilter はイベントのまとまりを保ったまま、調査に必要な部分だけを小さなファイルへ切り出します。',
      featuresTitle: '調査に必要な機能を、ひとつのCLIに。',
      featuresLead: '複数の条件は AND で組み合わせられます。広いログから、調べたい事象へ段階的に絞り込めます。',
      timeTitle: '期間で切り出す',
      timeBody: '開始・終了時刻を指定し、障害発生前後のログだけを抽出。From は含み、To は含まない明確な境界です。',
      levelTitle: 'レベルで絞る',
      levelBody: 'WARN、ERROR、FATAL など、必要なログレベルだけをまとめて指定できます。',
      contentTitle: '内容を探す',
      contentBody: '固定文字列の部分一致で、例外名やユーザーIDなどをイベント全体から検索します。',
      regexTitle: 'パターンで抽出する',
      regexBody: 'RE2 正規表現で複数の文言やパターンをまとめて指定。安全で予測可能な検索を実行できます。',
      workflowTitle: '準備して、絞って、確認する',
      workflowLead: 'いつものログ調査を3ステップに。入力ファイルはそのまま、結果だけを別ファイルで受け取れます。',
      prepareTitle: 'ログを用意',
      prepareBody: 'Apache access log、またはタイムスタンプで始まるアプリケーションログを指定します。',
      chooseTitle: '条件を指定',
      chooseBody: '期間、レベル、文字列、正規表現を、調査したい事象に合わせて組み合わせます。',
      inspectTitle: '結果を確認',
      inspectBody: 'マッチしたイベントと、そのスタックトレースを含む継続行が出力されます。',
      formatsTitle: '現場のログを、そのまま扱う。',
      formatsLead: '一般的なタイムスタンプ形式と、日本語環境で使われる文字コードに対応。変換作業を挟まず調査を始められます。',
      formatsLink: '対応形式を詳しく見る →',
      formatsLabel: 'ログ形式',
      platformsLabel: '実行環境',
      installTitle: 'すぐに始める。',
      installLead: 'Go、GitHub Releases、Docker。環境に合う方法を選べます。',
      copyCommand: 'コマンドをコピー',
      copied: 'コマンドをコピーしました。',
      copyFailed: 'コピーできませんでした。コマンドを選択してコピーしてください。',
      downloadLatest: '最新版をダウンロード',
      readDocs: 'ドキュメントを読む',
      footerTagline: '必要なログだけ、瞬時に。',
    },
    en: {
      title: 'logfilter — Find the logs that matter. Instantly.',
      description: 'An open-source CLI for extracting events from large log files by time range, level, text, or regular expression.',
      ogDescription: 'An open-source Go CLI for extracting the events you need from large log files.',
      ogLocale: 'en_US',
      docsUrl: 'https://github.com/scenario-test-framework/logfilter/blob/master/README.md',
      skipLink: 'Skip to content',
      primaryNav: 'Primary navigation',
      navFeatures: 'Features',
      navUsage: 'Usage',
      navInstall: 'Install',
      languageGroup: 'Display language',
      switchJapanese: 'Select Japanese',
      switchEnglish: 'Select English',
      heroTitleLine1: 'Find the logs',
      heroTitleLine2: 'that matter. Instantly.',
      heroLead: 'Extract the events you need from large log files by time range, level, text, or regular expression. Multiline stack traces stay together.',
      getStarted: 'Get started',
      viewGithub: 'View on GitHub',
      distributionLabel: 'Distribution',
      distributionValue: 'Single binary',
      eventsLabel: 'Events',
      eventsValue: 'Multiline aware',
      encodingsLabel: 'Encodings',
      demoCaption: 'Example of logfilter extracting matching events from a large log stream',
      demoFilter: 'Log levels: WARN, ERROR',
      demoOutput: 'Output: filtered.log',
      introTitle: 'Turn log noise into actionable evidence.',
      introLead: 'Stop opening huge files, repeating searches, and reconstructing stack traces by hand. logfilter keeps each event intact and writes only the evidence you need to a smaller file.',
      featuresTitle: 'Everything you need in one focused CLI.',
      featuresLead: 'Combine filters with AND logic to move from a large log file to the exact events you need.',
      timeTitle: 'Filter by time range',
      timeBody: 'Extract only the events around an incident. The start time is inclusive and the end time is exclusive.',
      levelTitle: 'Select log levels',
      levelBody: 'Include only the levels you need, such as WARN, ERROR, or FATAL.',
      contentTitle: 'Search event content',
      contentBody: 'Find exception names, user IDs, and other text anywhere in an event.',
      regexTitle: 'Match patterns',
      regexBody: 'Use RE2 regular expressions to match several messages or patterns with predictable performance.',
      workflowTitle: 'Prepare. Filter. Inspect.',
      workflowLead: 'A three-step workflow for everyday investigations. Keep the source untouched and inspect a focused output file.',
      prepareTitle: 'Prepare a log',
      prepareBody: 'Choose an Apache access log or an application log whose events start with timestamps.',
      chooseTitle: 'Choose filters',
      chooseBody: 'Combine time, level, text, and regex filters for the incident you are investigating.',
      inspectTitle: 'Inspect the result',
      inspectBody: 'Review matching events with all continuation lines, including stack traces, intact.',
      formatsTitle: 'Works with the logs you already have.',
      formatsLead: 'Support for common timestamp formats and Japanese encodings lets you investigate without a conversion step.',
      formatsLink: 'See all supported formats →',
      formatsLabel: 'Log formats',
      platformsLabel: 'Platforms',
      installTitle: 'Get started quickly.',
      installLead: 'Choose Go, GitHub Releases, or Docker—whichever fits your environment.',
      copyCommand: 'Copy command',
      copied: 'Command copied.',
      copyFailed: 'Could not copy. Select the command and copy it manually.',
      downloadLatest: 'Download the latest release',
      readDocs: 'Read the documentation',
      footerTagline: 'Find the logs that matter. Instantly.',
    },
  };

  const root = document.documentElement;
  const copyStatus = document.querySelector('.copy-status');
  const languageButtons = document.querySelectorAll('[data-lang-button]');

  const applyLanguage = (language, persist = false) => {
    const lang = language === 'ja' ? 'ja' : 'en';
    const copy = translations[lang];

    root.lang = lang;
    document.title = copy.title;
    document.querySelector('meta[name="description"]')?.setAttribute('content', copy.description);
    document.querySelector('meta[property="og:title"]')?.setAttribute('content', copy.title);
    document.querySelector('meta[property="og:description"]')?.setAttribute('content', copy.ogDescription);
    document.querySelector('meta[property="og:locale"]')?.setAttribute('content', copy.ogLocale);

    document.querySelectorAll('[data-i18n]').forEach((element) => {
      element.textContent = copy[element.dataset.i18n];
    });
    document.querySelectorAll('[data-i18n-aria]').forEach((element) => {
      element.setAttribute('aria-label', copy[element.dataset.i18nAria]);
    });
    document.querySelectorAll('[data-i18n-href]').forEach((element) => {
      element.href = copy[element.dataset.i18nHref];
    });
    languageButtons.forEach((button) => {
      button.setAttribute('aria-pressed', String(button.dataset.langButton === lang));
    });
    if (copyStatus) copyStatus.textContent = '';

    if (persist) {
      try { localStorage.setItem('logfilter-language', lang); } catch (_) { /* Preferences are optional. */ }
    }
  };

  languageButtons.forEach((button) => {
    button.addEventListener('click', () => applyLanguage(button.dataset.langButton, true));
  });

  const copyText = async (text) => {
    if (navigator.clipboard?.writeText) {
      try {
        await Promise.race([
          navigator.clipboard.writeText(text),
          new Promise((_, reject) => setTimeout(() => reject(new Error('Clipboard timeout')), 800)),
        ]);
        return true;
      } catch (_) { /* Try the compatibility fallback. */ }
    }

    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.setAttribute('readonly', '');
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    const copied = document.execCommand('copy');
    textarea.remove();
    return copied;
  };

  document.querySelectorAll('[data-copy]').forEach((button) => {
    button.addEventListener('click', async () => {
      const copy = translations[root.lang === 'ja' ? 'ja' : 'en'];
      try {
        const copied = await copyText(button.dataset.copy);
        if (copyStatus) copyStatus.textContent = copied ? copy.copied : copy.copyFailed;
      } catch (_) {
        if (copyStatus) copyStatus.textContent = copy.copyFailed;
      }
    });
  });

  const validLanguage = (value) => (value === 'ja' || value === 'en' ? value : null);
  const queryLanguage = validLanguage(new URLSearchParams(window.location.search).get('lang'));
  let storedLanguage = null;
  try { storedLanguage = validLanguage(localStorage.getItem('logfilter-language')); } catch (_) { /* Use browser language. */ }
  const browserLanguage = validLanguage((navigator.languages?.[0] || navigator.language || '').split('-')[0]);

  applyLanguage(queryLanguage || storedLanguage || browserLanguage || 'en');
})();
