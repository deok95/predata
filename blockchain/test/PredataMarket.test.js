import {
  time,
  loadFixture,
} from "@nomicfoundation/hardhat-toolbox/network-helpers.js";
import { expect } from "chai";
import hre from "hardhat";

describe("PredataMarket", function () {
  async function deployPredataFixture() {
    const [admin, user1, user2, user3] = await hre.ethers.getSigners();

    const PredataMarket = await hre.ethers.getContractFactory("PredataMarket");
    const predata = await PredataMarket.deploy();

    return { predata, admin, user1, user2, user3 };
  }

  describe("질문 생성", function () {
    it("관리자가 질문을 생성할 수 있어야 함", async function () {
      const { predata, admin } = await loadFixture(deployPredataFixture);
      
      const expiredAt = (await time.latest()) + 3600n;
      
      await expect(
        predata.createQuestion(1, "EPL: 맨시티 vs 리버풀", "SPORTS", expiredAt)
      )
        .to.emit(predata, "QuestionCreated");
      
      const question = await predata.getQuestion(1);
      expect(question.questionId).to.equal(1);
      expect(question.title).to.equal("EPL: 맨시티 vs 리버풀");
    });

    it("일반 사용자는 질문을 생성할 수 없어야 함", async function () {
      const { predata, user1 } = await loadFixture(deployPredataFixture);
      
      const expiredAt = (await time.latest()) + 3600n;
      
      await expect(
        predata.connect(user1).createQuestion(1, "Test", "SPORTS", expiredAt)
      ).to.be.revertedWith("Only admin can call this");
    });
  });

  describe("베팅", function () {
    it("배치로 베팅을 처리할 수 있어야 함", async function () {
      const { predata, admin, user1, user2 } = await loadFixture(deployPredataFixture);
      
      const expiredAt = (await time.latest()) + 3600n;
      await predata.createQuestion(1, "Test Question", "SPORTS", expiredAt);
      
      await expect(
        predata.batchPlaceBets(
          [1, 1],
          [user1.address, user2.address],
          [true, false],
          [1000, 2000]
        )
      )
        .to.emit(predata, "BetPlaced");
      
      const bet1 = await predata.getUserBet(1, user1.address);
      expect(bet1.amount).to.equal(1000);
      expect(bet1.choice).to.equal(true);
      
      const question = await predata.getQuestion(1);
      expect(question.yesBetPool).to.equal(1000);
      expect(question.noBetPool).to.equal(2000);
      expect(question.totalBetPool).to.equal(3000);
    });

    it("만료된 질문에는 베팅할 수 없어야 함", async function () {
      const { predata, admin, user1 } = await loadFixture(deployPredataFixture);
      
      const expiredAt = (await time.latest()) + 3600n;
      await predata.createQuestion(1, "Test", "SPORTS", expiredAt);
      
      await time.increase(3601); // 만료 시간 지남
      
      await expect(
        predata.batchPlaceBets([1], [user1.address], [true], [1000])
      ).to.be.revertedWith("Question expired");
    });
  });

  describe("정산", function () {
    it("질문을 정산할 수 있어야 함", async function () {
      const { predata, admin, user1, user2 } = await loadFixture(deployPredataFixture);
      
      const expiredAt = (await time.latest()) + 3600n;
      await predata.createQuestion(1, "Test", "SPORTS", expiredAt);
      
      await predata.batchPlaceBets(
        [1, 1],
        [user1.address, user2.address],
        [true, false],
        [1000, 2000]
      );
      
      await expect(predata.settleQuestion(1, 1))
        .to.emit(predata, "QuestionSettled");
      
      const question = await predata.getQuestion(1);
      expect(question.settled).to.equal(true);
      expect(question.finalResult).to.equal(1); // YES
    });

    it("승자가 당첨금을 청구할 수 있어야 함", async function () {
      const { predata, admin, user1, user2 } = await loadFixture(deployPredataFixture);
      
      const expiredAt = (await time.latest()) + 3600n;
      await predata.createQuestion(1, "Test", "SPORTS", expiredAt);
      
      await predata.batchPlaceBets(
        [1, 1],
        [user1.address, user2.address],
        [true, false],
        [1000, 2000]
      );
      
      await predata.settleQuestion(1, 1); // YES 승리
      
      await expect(predata.connect(user1).claimWinnings(1))
        .to.emit(predata, "WinningsClaimed")
        .withArgs(1, user1.address, 3000);
      
      await expect(
        predata.connect(user2).claimWinnings(1)
      ).to.be.revertedWith("Lost bet");
    });
  });

  describe("배당률 계산", function () {
    it("배당률을 올바르게 계산해야 함", async function () {
      const { predata, admin, user1, user2 } = await loadFixture(deployPredataFixture);
      
      const expiredAt = (await time.latest()) + 3600n;
      await predata.createQuestion(1, "Test", "SPORTS", expiredAt);
      
      await predata.batchPlaceBets(
        [1, 1],
        [user1.address, user2.address],
        [true, false],
        [1000, 2000]
      );
      
      const [yesOdds, noOdds] = await predata.calculateOdds(1);
      
      expect(yesOdds).to.equal(30000);
      expect(noOdds).to.equal(15000);
    });
  });
});
