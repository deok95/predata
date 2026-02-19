// USDC Contract Configuration for Polygon
// Change NEXT_PUBLIC_POLYGON_CHAIN_ID=137 when switching to mainnet

export const USDC_DECIMALS = 6;

// USDC contract addresses
export const USDC_ADDRESS: Record<number, `0x${string}`> = {
  137: '0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359',   // Polygon Mainnet (Native USDC)
  80002: (process.env.NEXT_PUBLIC_USDC_ADDRESS_AMOY ||
    '0x41E94Eb019C0762f9Bfcf9Fb1E58725BfB0e7582') as `0x${string}`, // Polygon Amoy Testnet
};

// USDC receiver wallet address
export const RECEIVER_WALLET = (process.env.NEXT_PUBLIC_RECEIVER_WALLET ||
  '0x0000000000000000000000000000000000000000') as `0x${string}`;

// Currently active chain ID
export const ACTIVE_CHAIN_ID = Number(
  process.env.NEXT_PUBLIC_POLYGON_CHAIN_ID || '80002'
);

// USDC address for current chain
export const ACTIVE_USDC_ADDRESS = USDC_ADDRESS[ACTIVE_CHAIN_ID] || USDC_ADDRESS[80002];

// ERC-20 transfer ABI (minimal ABI required for USDC transfer)
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

// Pricing policy
export const VOTING_PASS_PRICE = 10; // Voting pass $10
export const BET_MIN_USDC = 1;       // Minimum bet amount
export const BET_MAX_USDC = 100;     // Maximum bet amount

// Convert USDC amount to on-chain units (6 decimals)
export function toUSDCUnits(amount: number): bigint {
  return BigInt(Math.round(amount * 10 ** USDC_DECIMALS));
}

// Convert on-chain units to USDC amount
export function fromUSDCUnits(units: bigint): number {
  return Number(units) / 10 ** USDC_DECIMALS;
}
