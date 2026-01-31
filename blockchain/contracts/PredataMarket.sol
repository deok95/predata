// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title PredataMarket
 * @notice 예측 시장 베팅 시스템 - Base L2에 배포
 * @dev 베팅은 온체인에 기록되지만, 페르소나 데이터는 오프체인에 저장
 */
contract PredataMarket {
    // ========== 상태 변수 ==========
    
    address public admin;
    uint256 public questionCount;
    
    struct Question {
        uint256 questionId;
        string title;
        string category;
        uint256 totalBetPool;
        uint256 yesBetPool;
        uint256 noBetPool;
        FinalResult finalResult;
        bool settled;
        uint256 settledAt;
        uint256 createdAt;
        uint256 expiredAt;
    }
    
    struct Bet {
        address user;
        bool choice; // true = YES, false = NO
        uint256 amount;
        uint256 timestamp;
        bool claimed;
    }
    
    enum FinalResult { PENDING, YES, NO }
    
    // 질문 ID => Question
    mapping(uint256 => Question) public questions;
    
    // 질문 ID => 사용자 주소 => Bet
    mapping(uint256 => mapping(address => Bet)) public userBets;
    
    // 사용자가 참여한 질문 목록 추적
    mapping(address => uint256[]) public userQuestions;
    
    // ========== 이벤트 ==========
    
    event QuestionCreated(
        uint256 indexed questionId,
        string title,
        string category,
        uint256 expiredAt,
        uint256 timestamp
    );
    
    event BetPlaced(
        uint256 indexed questionId,
        address indexed user,
        bool choice,
        uint256 amount,
        uint256 timestamp
    );
    
    event PoolsUpdated(
        uint256 indexed questionId,
        uint256 yesBetPool,
        uint256 noBetPool,
        uint256 totalBetPool
    );
    
    event QuestionSettled(
        uint256 indexed questionId,
        FinalResult result,
        uint256 timestamp
    );
    
    event WinningsClaimed(
        uint256 indexed questionId,
        address indexed user,
        uint256 payout
    );
    
    // ========== 수정자 ==========
    
    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin can call this");
        _;
    }
    
    // ========== 생성자 ==========
    
    constructor() {
        admin = msg.sender;
        questionCount = 0;
    }
    
    // ========== 관리자 함수 ==========
    
    /**
     * @notice 새로운 질문 생성
     * @param questionId 백엔드 DB의 질문 ID
     * @param title 질문 제목
     * @param category 카테고리
     * @param expiredAt 만료 시간 (timestamp)
     */
    function createQuestion(
        uint256 questionId,
        string memory title,
        string memory category,
        uint256 expiredAt
    ) external onlyAdmin {
        require(questions[questionId].questionId == 0, "Question already exists");
        require(expiredAt > block.timestamp, "Invalid expiration time");
        
        questions[questionId] = Question({
            questionId: questionId,
            title: title,
            category: category,
            totalBetPool: 0,
            yesBetPool: 0,
            noBetPool: 0,
            finalResult: FinalResult.PENDING,
            settled: false,
            settledAt: 0,
            createdAt: block.timestamp,
            expiredAt: expiredAt
        });
        
        questionCount++;
        
        emit QuestionCreated(questionId, title, category, expiredAt, block.timestamp);
    }
    
    /**
     * @notice 배치로 베팅 처리 (가스비 절감)
     * @param questionIds 질문 ID 배열
     * @param users 사용자 주소 배열
     * @param choices 선택 배열
     * @param amounts 금액 배열
     */
    function batchPlaceBets(
        uint256[] memory questionIds,
        address[] memory users,
        bool[] memory choices,
        uint256[] memory amounts
    ) external onlyAdmin {
        require(
            questionIds.length == users.length &&
            users.length == choices.length &&
            choices.length == amounts.length,
            "Array length mismatch"
        );
        
        for (uint256 i = 0; i < questionIds.length; i++) {
            _placeBet(questionIds[i], users[i], choices[i], amounts[i]);
        }
    }
    
    /**
     * @notice 질문 정산
     * @param questionId 질문 ID
     * @param result 최종 결과
     */
    function settleQuestion(uint256 questionId, FinalResult result) external onlyAdmin {
        Question storage q = questions[questionId];
        require(q.questionId != 0, "Question does not exist");
        require(!q.settled, "Already settled");
        require(result != FinalResult.PENDING, "Invalid result");
        
        q.finalResult = result;
        q.settled = true;
        q.settledAt = block.timestamp;
        
        emit QuestionSettled(questionId, result, block.timestamp);
    }
    
    // ========== 내부 함수 ==========
    
    function _placeBet(
        uint256 questionId,
        address user,
        bool choice,
        uint256 amount
    ) internal {
        Question storage q = questions[questionId];
        require(q.questionId != 0, "Question does not exist");
        require(!q.settled, "Question already settled");
        require(block.timestamp < q.expiredAt, "Question expired");
        require(amount > 0, "Amount must be greater than 0");
        
        Bet storage existingBet = userBets[questionId][user];
        
        if (existingBet.amount == 0) {
            // 첫 번째 베팅
            userBets[questionId][user] = Bet({
                user: user,
                choice: choice,
                amount: amount,
                timestamp: block.timestamp,
                claimed: false
            });
            
            userQuestions[user].push(questionId);
        } else {
            // 기존 베팅 업데이트
            require(existingBet.choice == choice, "Cannot change bet choice");
            existingBet.amount += amount;
        }
        
        // 풀 업데이트
        if (choice) {
            q.yesBetPool += amount;
        } else {
            q.noBetPool += amount;
        }
        q.totalBetPool += amount;
        
        emit BetPlaced(questionId, user, choice, amount, block.timestamp);
        emit PoolsUpdated(questionId, q.yesBetPool, q.noBetPool, q.totalBetPool);
    }
    
    // ========== 사용자 함수 ==========
    
    /**
     * @notice 당첨금 청구
     * @param questionId 질문 ID
     */
    function claimWinnings(uint256 questionId) external {
        Question storage q = questions[questionId];
        require(q.settled, "Question not settled yet");
        
        Bet storage bet = userBets[questionId][msg.sender];
        require(bet.amount > 0, "No bet found");
        require(!bet.claimed, "Already claimed");
        require(
            (q.finalResult == FinalResult.YES && bet.choice) ||
            (q.finalResult == FinalResult.NO && !bet.choice),
            "Lost bet"
        );
        
        // 당첨금 계산
        uint256 winningPool = bet.choice ? q.yesBetPool : q.noBetPool;
        uint256 payout = (bet.amount * q.totalBetPool) / winningPool;
        
        bet.claimed = true;
        
        // 실제로는 백엔드에서 포인트로 지급하므로 이벤트만 발생
        emit WinningsClaimed(questionId, msg.sender, payout);
    }
    
    // ========== 조회 함수 ==========
    
    /**
     * @notice 사용자의 베팅 정보 조회
     */
    function getUserBet(uint256 questionId, address user) 
        external 
        view 
        returns (Bet memory) 
    {
        return userBets[questionId][user];
    }
    
    /**
     * @notice 사용자가 참여한 모든 질문 ID 조회
     */
    function getUserQuestions(address user) 
        external 
        view 
        returns (uint256[] memory) 
    {
        return userQuestions[user];
    }
    
    /**
     * @notice 질문 상세 정보 조회
     */
    function getQuestion(uint256 questionId) 
        external 
        view 
        returns (Question memory) 
    {
        return questions[questionId];
    }
    
    /**
     * @notice 현재 배당률 계산
     * @dev 프론트엔드에서 호출
     */
    function calculateOdds(uint256 questionId) 
        external 
        view 
        returns (uint256 yesOdds, uint256 noOdds) 
    {
        Question storage q = questions[questionId];
        
        if (q.totalBetPool == 0) {
            return (10000, 10000); // 1.00 배당 (100% = 10000)
        }
        
        if (q.yesBetPool == 0) {
            yesOdds = 99999; // 최대 배당
            noOdds = 10100;  // 최소 배당
        } else if (q.noBetPool == 0) {
            yesOdds = 10100;
            noOdds = 99999;
        } else {
            yesOdds = (q.totalBetPool * 10000) / q.yesBetPool;
            noOdds = (q.totalBetPool * 10000) / q.noBetPool;
        }
        
        return (yesOdds, noOdds);
    }
    
    /**
     * @notice 관리자 변경
     */
    function transferAdmin(address newAdmin) external onlyAdmin {
        require(newAdmin != address(0), "Invalid address");
        admin = newAdmin;
    }
}
