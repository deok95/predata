import hre from "hardhat";

async function main() {
  console.log("🚀 Predata Market 배포 시작...");
  
  const [deployer] = await hre.ethers.getSigners();
  console.log("배포 계정:", deployer.address);
  
  const balance = await hre.ethers.provider.getBalance(deployer.address);
  console.log("계정 잔액:", hre.ethers.formatEther(balance), "ETH");
  
  // 컨트랙트 배포
  const PredataMarket = await hre.ethers.getContractFactory("PredataMarket");
  console.log("컨트랙트 배포 중...");
  
  const predata = await PredataMarket.deploy();
  await predata.waitForDeployment();
  
  const contractAddress = await predata.getAddress();
  console.log("✅ PredataMarket 배포 완료!");
  console.log("📝 컨트랙트 주소:", contractAddress);
  console.log("👤 Admin 주소:", deployer.address);
  
  // 배포 정보 저장
  const deploymentInfo = {
    network: hre.network.name,
    contractAddress: contractAddress,
    adminAddress: deployer.address,
    deployedAt: new Date().toISOString(),
    chainId: (await hre.ethers.provider.getNetwork()).chainId
  };
  
  console.log("\n=== 배포 정보 ===");
  console.log(JSON.stringify(deploymentInfo, null, 2));
  
  // Basescan 검증 정보
  if (hre.network.name !== "hardhat") {
    console.log("\n=== Basescan 검증 명령어 ===");
    console.log(`npx hardhat verify --network ${hre.network.name} ${contractAddress}`);
  }
  
  return predata;
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
