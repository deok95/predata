// USDC Contract Configuration for Polygon
// 메인넷 전환 시 NEXT_PUBLIC_POLYGON_CHAIN_ID=137 로 변경

export const USDC_DECIMALS = 6;

// USDC 컨트랙트 주소
export const USDC_ADDRESS: Record<number, `0x${string}`> = {
  137: '0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359',   // Polygon 메인넷 (Native USDC)
  80002: (process.env.NEXT_PUBLIC_USDC_ADDRESS_AMOY ||
    '0x41E94Eb019C0762f9Bfcf9Fb1E58725BfB0e7582') as `0x${string}`, // Polygon Amoy 테스트넷
};

// USDC 수신 지갑 주소
export const RECEIVER_WALLET = (process.env.NEXT_PUBLIC_RECEIVER_WALLET ||
  '0x0000000000000000000000000000000000000000') as `0x${string}`;

// 현재 활성 체인 ID
export const ACTIVE_CHAIN_ID = Number(
  process.env.NEXT_PUBLIC_POLYGON_CHAIN_ID || '80002'
);

// 현재 체인의 USDC 주소
export const ACTIVE_USDC_ADDRESS = USDC_ADDRESS[ACTIVE_CHAIN_ID] || USDC_ADDRESS[80002];

// ERC-20 transfer ABI (USDC 전송에 필요한 최소 ABI)
export const ERC20_ABI = [
  {
    name: 'transfer',
    type: 'function',
    stateMutability: 'nonpayable',
    inputs: [
      { name: 'to', type: 'address' },
      { name: 'amount', type: 'uint256' },
    ],
    outputs: [{ name: '', type: 'bool' }],
  },
  {
    name: 'approve',
    type: 'function',
    stateMutability: 'nonpayable',
    inputs: [
      { name: 'spender', type: 'address' },
      { name: 'amount', type: 'uint256' },
    ],
    outputs: [{ name: '', type: 'bool' }],
  },
  {
    name: 'balanceOf',
    type: 'function',
    stateMutability: 'view',
    inputs: [{ name: 'account', type: 'address' }],
    outputs: [{ name: '', type: 'uint256' }],
  },
  {
    name: 'allowance',
    type: 'function',
    stateMutability: 'view',
    inputs: [
      { name: 'owner', type: 'address' },
      { name: 'spender', type: 'address' },
    ],
    outputs: [{ name: '', type: 'uint256' }],
  },
] as const;

// 가격 정책
export const VOTING_PASS_PRICE = 10; // 투표 패스 $10
export const BET_MIN_USDC = 1;       // 최소 베팅 금액
export const BET_MAX_USDC = 100;     // 최대 베팅 금액

// USDC 금액을 온체인 단위(6 decimals)로 변환
export function toUSDCUnits(amount: number): bigint {
  return BigInt(Math.round(amount * 10 ** USDC_DECIMALS));
}

// 온체인 단위를 USDC 금액으로 변환
export function fromUSDCUnits(units: bigint): number {
  return Number(units) / 10 ** USDC_DECIMALS;
}
