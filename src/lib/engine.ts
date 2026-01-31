export const calculateOdds = (poolYes: number, poolNo: number, subsidy: number) => {
  // 브랜드 지원금은 유동성을 공급하여 초기 배당 급변을 방지합니다.
  const effectiveYes = Number(poolYes) + (Number(subsidy) / 2);
  const effectiveNo = Number(poolNo) + (Number(subsidy) / 2);
  const totalPool = effectiveYes + effectiveNo;

  if (totalPool === 0) return { yesOdds: "2.00", noOdds: "2.00" };

  // 가격의 합이 1.01($1.01)이 되도록 수수료 1%를 녹여냅니다.
  const yesPrice = (effectiveYes / totalPool) * 1.01;
  const noPrice = (effectiveNo / totalPool) * 1.01;

  return {
    yesPrice: yesPrice.toFixed(2),
    noPrice: noPrice.toFixed(2),
    yesOdds: (1 / yesPrice).toFixed(2), // 화면에 표시될 배당률
    noOdds: (1 / noPrice).toFixed(2)
  };
};
