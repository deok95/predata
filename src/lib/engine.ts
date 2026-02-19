export const calculateOdds = (poolYes: number, poolNo: number, subsidy: number) => {
  // Brand subsidy provides liquidity to prevent sudden initial odds changes.
  const effectiveYes = Number(poolYes) + (Number(subsidy) / 2);
  const effectiveNo = Number(poolNo) + (Number(subsidy) / 2);
  const totalPool = effectiveYes + effectiveNo;

  if (totalPool === 0) return { yesOdds: "2.00", noOdds: "2.00" };

  // Apply 1% fee so that sum of prices equals 1.01 ($1.01).
  const yesPrice = (effectiveYes / totalPool) * 1.01;
  const noPrice = (effectiveNo / totalPool) * 1.01;

  return {
    yesPrice: yesPrice.toFixed(2),
    noPrice: noPrice.toFixed(2),
    yesOdds: (1 / yesPrice).toFixed(2), // Odds to display on screen
    noOdds: (1 / noPrice).toFixed(2)
  };
};
