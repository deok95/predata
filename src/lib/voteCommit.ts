/**
 * Commit-Reveal 투표 유틸리티
 *
 * 보안 정책: salt는 클라이언트에서만 생성하고 보관
 * - 서버는 commitHash만 저장
 * - reveal 시 클라이언트가 salt를 제공하여 검증
 */

/**
 * 랜덤 salt 생성 (32바이트 hex)
 */
export function generateSalt(): string {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

/**
 * SHA-256 해시 생성
 * 형식: SHA-256(questionId:memberId:choice:salt)
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
 * localStorage에 salt 저장
 */
export function saveSaltToStorage(questionId: number, salt: string): void {
  try {
    localStorage.setItem(`vote_salt_${questionId}`, salt);
  } catch (error) {
    console.error('Failed to save salt to localStorage:', error);
  }
}

/**
 * localStorage에서 salt 조회
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
 * localStorage에서 salt 삭제
 */
export function removeSaltFromStorage(questionId: number): void {
  try {
    localStorage.removeItem(`vote_salt_${questionId}`);
  } catch (error) {
    console.error('Failed to remove salt from localStorage:', error);
  }
}

/**
 * salt 유무 확인
 */
export function hasSaltInStorage(questionId: number): boolean {
  return getSaltFromStorage(questionId) !== null;
}
