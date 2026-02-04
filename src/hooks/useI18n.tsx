'use client';

import { useState, useCallback, useMemo } from 'react';
import type { ReactNode } from 'react';
import { I18nContext, getTranslation } from '@/lib/i18n';
import type { Locale } from '@/lib/i18n';
import { safeLocalStorage } from '@/lib/safeLocalStorage';

const STORAGE_KEY = 'predata-locale';

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(() => {
    const saved = safeLocalStorage.getItem(STORAGE_KEY);
    return (saved === 'en' || saved === 'ko') ? saved : 'ko';
  });

  const setLocale = useCallback((newLocale: Locale) => {
    setLocaleState(newLocale);
    safeLocalStorage.setItem(STORAGE_KEY, newLocale);
  }, []);

  const t = useCallback((key: string) => getTranslation(locale, key), [locale]);

  const value = useMemo(() => ({ locale, t, setLocale }), [locale, t, setLocale]);

  return (
    <I18nContext.Provider value={value}>
      {children}
    </I18nContext.Provider>
  );
}
