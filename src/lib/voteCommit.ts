/**
 * Commit-Reveal voting utility
 *
 * Security policy: salt is generated and stored only on client
 * - Server stores only commitHash
 * - Client provides salt during reveal for verification
 */

/**
 * Generate random salt (32-byte hex)
 */
export function generateSalt(): string {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

/**
 * Generate SHA-256 hash
 * Format: SHA-256(questionId:memberId:choice:salt)
 */
export async function generateCommitHash(
  questionId: number,
  memberId: number,
  choice: 'YES' | 'NO',
  salt: string
): Promise<string> {
  const data = `${questionId}:${memberId}:${choice}:${salt}`;
  const encoder = new TextEncoder();
  const dataBuffer = encoder.encode(data);
  const hashBuffer = await crypto.subtle.digest('SHA-256', dataBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
}

/**
 * Save salt to localStorage
 */
export function saveSaltToStorage(questionId: number, salt: string): void {
  try {
    localStorage.setItem(`vote_salt_${questionId}`, salt);
  } catch (error) {
    console.error('Failed to save salt to localStorage:', error);
  }
}

/**
 * Retrieve salt from localStorage
 */
export function getSaltFromStorage(questionId: number): string | null {
  try {
    return localStorage.getItem(`vote_salt_${questionId}`);
  } catch (error) {
    console.error('Failed to get salt from localStorage:', error);
    return null;
  }
}

/**
 * Delete salt from localStorage
 */
export function removeSaltFromStorage(questionId: number): void {
  try {
    localStorage.removeItem(`vote_salt_${questionId}`);
  } catch (error) {
    console.error('Failed to remove salt from localStorage:', error);
  }
}

/**
 * Check if salt exists
 */
export function hasSaltInStorage(questionId: number): boolean {
  return getSaltFromStorage(questionId) !== null;
}
