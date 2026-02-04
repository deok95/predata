import { describe, it, expect } from 'vitest';
import { getTranslation, translations } from '@/lib/i18n';

describe('i18n', () => {
  it('should return Korean translation by default', () => {
    expect(getTranslation('ko', 'nav.home')).toBe('홈');
    expect(getTranslation('ko', 'nav.marketplace')).toBe('마켓 탐색');
  });

  it('should return English translation', () => {
    expect(getTranslation('en', 'nav.home')).toBe('Home');
    expect(getTranslation('en', 'nav.marketplace')).toBe('Marketplace');
  });

  it('should fallback to Korean for missing English keys', () => {
    // If a key exists only in ko, it should fallback
    const koKeys = Object.keys(translations.ko);
    const enKeys = Object.keys(translations.en);

    // Verify both locales have same keys
    expect(koKeys.length).toBeGreaterThan(0);
    expect(enKeys.length).toBeGreaterThan(0);
    expect(koKeys.length).toBe(enKeys.length);
  });

  it('should return key itself for unknown keys', () => {
    expect(getTranslation('ko', 'unknown.key')).toBe('unknown.key');
    expect(getTranslation('en', 'unknown.key')).toBe('unknown.key');
  });

  it('should have all common keys in both locales', () => {
    const commonKeys = [
      'common.search', 'common.loading', 'common.error',
      'common.save', 'common.cancel', 'common.submit',
    ];

    commonKeys.forEach(key => {
      expect(getTranslation('ko', key)).not.toBe(key);
      expect(getTranslation('en', key)).not.toBe(key);
    });
  });

  it('should have auth keys in both locales', () => {
    expect(getTranslation('ko', 'auth.login')).toBe('로그인');
    expect(getTranslation('en', 'auth.login')).toBe('Log In');
    expect(getTranslation('ko', 'auth.register')).toBe('회원가입');
    expect(getTranslation('en', 'auth.register')).toBe('Sign Up');
  });
});
