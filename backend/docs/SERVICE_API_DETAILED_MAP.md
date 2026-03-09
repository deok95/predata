# 서비스별 상세 로직 + API 매핑

- 기준 문서: `backend/docs/openapi/*.json`
- 기준 코드: `backend/src/main/kotlin/com/predata/backend/{controller,service}`
- 필드 표기: `*` = required field, 응답 필드는 `ApiEnvelope.data` 기준으로 전개
- 권한(Auth) 컬럼은 문서/경로 규칙 기반 분류이며, 최종 권한은 SecurityConfig와 인터셉터가 우선

## auth

- 엔드포인트 수: **7**
- 서비스 로직 핵심 플로우:
  - 1) `send-code`에서 이메일 중복 여부를 확인하고 인증코드 발송/만료시간 기록.
  - 2) `verify-code`에서 시도횟수/만료 검증 후 verified 상태로 전환.
  - 3) `complete-signup`에서 verified 상태 재검증 후 member 생성 + JWT 발급.
  - 4) Google OAuth는 `googleId` 또는 `email` 기준으로 기존 계정과 링크 후 토큰 발급.

| Method | Path | Auth | OperationId | Request DTO | Request Fields | Resp | Response DTO | Response Data Fields |
|---|---|---|---|---|---|---|---|---|
| POST | `/api/auth/complete-signup` | Public | `completeSignup` | `CompleteSignupRequest` | email:string*, code:string*, password:string*, passwordConfirm:string*, countryCode:string*, gender:string, birthDate:string, jobCategory:string, ageGroup:integer | 200 | `ApiEnvelopeMapStringObject` | object |
| POST | `/api/auth/google` | Public | `googleLogin` | `GoogleAuthRequest` | googleToken:string*, countryCode:string, jobCategory:string, ageGroup:integer | 200 | `ApiEnvelopeGoogleAuthResponse` | success:boolean*, message:string*, token:string, memberId:integer, needsAdditionalInfo:boolean* |
| POST | `/api/auth/google/complete-registration` | Public | `completeGoogleRegistration` | `CompleteGoogleRegistrationRequest` | googleId:string*, email:string*, countryCode:string*, gender:string, birthDate:string, jobCategory:string, ageGroup:integer | 200 | `ApiEnvelopeGoogleAuthResponse` | success:boolean*, message:string*, token:string, memberId:integer, needsAdditionalInfo:boolean* |
| POST | `/api/auth/login` | Public | `login` | `LoginRequest` | email:string*, password:string* | 200 | `ApiEnvelopeMapStringObject` | object |
| POST | `/api/auth/send-code` | Public | `sendCode` | `SendCodeRequest` | email:string* | 200 | `ApiEnvelopeMapStringObject` | object |
| POST | `/api/auth/verify-code` | Public | `verifyCode` | `VerifyCodeRequest` | email:string*, code:string* | 200 | `ApiEnvelopeMapStringObject` | object |
| POST | `/api/auth/wallet` | Public | `walletLogin` | `WalletLoginRequest` | walletAddress:string* | 200 | `ApiEnvelopeMapStringObject` | object |

## member-social

- 엔드포인트 수: **26**
- 서비스 로직 핵심 플로우:
  - 1) 프로필 조회/수정, 팔로우 관계, 댓글 CRUD를 SocialService에서 통합 처리.
  - 2) `SocialPolicy`로 username 규칙, 자기팔로우 금지, 댓글 content 정규화 적용.
  - 3) 알림 읽음 처리/미읽음 카운트, 추천코드 생성·적용, 리더보드/티어/배지 조회 제공.
  - 4) 마이페이지 대시보드(`members/me/dashboard`)는 집계 응답 DTO를 고정 계약으로 제공.

| Method | Path | Auth | OperationId | Request DTO | Request Fields | Resp | Response DTO | Response Data Fields |
|---|---|---|---|---|---|---|---|---|
| GET | `/api/badges/definitions` | JWT | `getAllDefinitions` | `-` | - | 200 | `ApiEnvelopeListBadgeDefinition` | array<id:string*, name:string*, description:string*, category:string*, rarity:string*, iconUrl:string, sortOrder:integer*> |
| GET | `/api/badges/member/{memberId}` | JWT | `getMemberBadges` | `-` | - | 200 | `ApiEnvelopeListBadgeWithProgressResponse` | array<badge:BadgeDefinitionDto*, progress:integer*, target:integer*, earned:boolean*, awardedAt:string> |
| GET | `/api/badges/member/{memberId}/earned` | JWT | `getEarnedBadges` | `-` | - | 200 | `ApiEnvelopeListBadgeWithProgressResponse` | array<badge:BadgeDefinitionDto*, progress:integer*, target:integer*, earned:boolean*, awardedAt:string> |
| GET | `/api/leaderboard/member/{memberId}` | JWT | `getMemberRank` | `-` | - | 200 | `ApiEnvelopeMemberRankResponse` | entry:LeaderboardEntry, found:boolean* |
| GET | `/api/leaderboard/top` | Public | `getTopLeaderboard` | `-` | - | 200 | `ApiEnvelopeListLeaderboardEntry` | array<rank:integer*, memberId:integer*, email:string*, tier:string*, accuracyScore:integer*, accuracyPercentage:number*, totalPredictions:integer*, correctPredictions:integer*> |
| POST | `/api/members` | JWT | `createMember` | `CreateMemberRequest` | email:string*, walletAddress:string, countryCode:string*, jobCategory:string*, ageGroup:integer* | 200 | `ApiEnvelopeMemberResponse` | memberId:integer*, email:string*, username:string, displayName:string, bio:string, avatarUrl:string, walletAddress:string, countryCode:string*, jobCategory:string*, ageGroup:integer*, tier:string*, tierWeight:number* |
| GET | `/api/members/by-wallet` | JWT | `getMemberByWallet` | `-` | - | 200 | `ApiEnvelopePublicMemberResponse` | memberId:integer*, username:string, displayName:string, bio:string, avatarUrl:string, walletAddress:string, countryCode:string*, tier:string*, tierWeight:number*, hasVotingPass:boolean* |
| GET | `/api/members/me` | JWT | `getMyInfo` | `-` | - | 200 | `ApiEnvelopeMemberResponse` | memberId:integer*, email:string*, username:string, displayName:string, bio:string, avatarUrl:string, walletAddress:string, countryCode:string*, jobCategory:string*, ageGroup:integer*, tier:string*, tierWeight:number* |
| GET | `/api/members/me/dashboard` | JWT | `getMyDashboard` | `-` | - | 200 | `ApiEnvelopeMyDashboardResponse` | memberId:integer*, followers:integer*, following:integer*, questionsCreated:integer*, totalVotes:integer*, voteCredits:integer*, creatorEarnings:number*, memberSince:string* |
| PUT | `/api/members/wallet` | JWT | `updateWalletAddress` | `UpdateWalletRequest` | walletAddress:string | 200 | `ApiEnvelopeMemberResponse` | memberId:integer*, email:string*, username:string, displayName:string, bio:string, avatarUrl:string, walletAddress:string, countryCode:string*, jobCategory:string*, ageGroup:integer*, tier:string*, tierWeight:number* |
| GET | `/api/members/{id}` | Public | `getMember` | `-` | - | 200 | `ApiEnvelopePublicMemberResponse` | memberId:integer*, username:string, displayName:string, bio:string, avatarUrl:string, walletAddress:string, countryCode:string*, tier:string*, tierWeight:number*, hasVotingPass:boolean* |
| GET | `/api/notifications` | JWT | `getNotifications` | `-` | - | 200 | `ApiEnvelopeListNotificationResponse` | array<id:integer*, type:string*, title:string*, message:string*, relatedQuestionId:integer, read:boolean*, createdAt:string*> |
| POST | `/api/notifications/read-all` | JWT | `markAllAsRead` | `-` | - | 200 | `ApiEnvelopeMarkAllReadResponse` | success:boolean*, updated:integer* |
| GET | `/api/notifications/unread-count` | JWT | `getUnreadCount` | `-` | - | 200 | `ApiEnvelopeUnreadCountResponse` | count:integer* |
| POST | `/api/notifications/{id}/read` | JWT | `markAsRead` | `-` | - | 200 | `ApiEnvelopeMarkReadResponse` | success:boolean* |
| POST | `/api/referrals/apply` | JWT | `applyReferral` | `ApplyReferralRequest` | referralCode:string* | 200 | `ApiEnvelopeReferralResult` | success:boolean*, message:string*, referrerReward:integer*, refereeReward:integer* |
| GET | `/api/referrals/code` | JWT | `getMyCode` | `-` | - | 200 | `ApiEnvelopeReferralCodeResponse` | code:string* |
| GET | `/api/referrals/stats` | JWT | `getStats` | `-` | - | 200 | `ApiEnvelopeReferralStatsResponse` | referralCode:string*, totalReferrals:integer*, totalPointsEarned:integer*, referees:array<RefereeInfo>* |
| GET | `/api/tiers/progress/{memberId}` | JWT | `getTierProgress` | `-` | - | 200 | `ApiEnvelopeTierProgressResponse` | memberId:integer*, currentTier:string*, currentScore:integer*, nextTier:string*, nextTierThreshold:integer*, progressPercentage:number*, tierWeight:number*, totalPredictions:integer*, correctPredictions:integer*, accuracyPercentage:number* |
| GET | `/api/tiers/statistics` | JWT | `getTierStatistics` | `-` | - | 200 | `ApiEnvelopeTierStatistics` | totalMembers:integer*, bronzeCount:integer*, silverCount:integer*, goldCount:integer*, platinumCount:integer*, averageAccuracyScore:integer* |
| PUT | `/api/users/me/profile` | JWT | `updateMyProfile` | `UpdateSocialProfileRequest` | username:string, displayName:string, bio:string, avatarUrl:string | 200 | `ApiEnvelopeSocialProfileResponse` | memberId:integer*, username:string, displayName:string, bio:string, avatarUrl:string, followersCount:integer*, followingCount:integer*, isFollowing:boolean* |
| GET | `/api/users/{id}` | Public | `getProfile` | `-` | - | 200 | `ApiEnvelopeSocialProfileResponse` | memberId:integer*, username:string, displayName:string, bio:string, avatarUrl:string, followersCount:integer*, followingCount:integer*, isFollowing:boolean* |
| POST | `/api/users/{id}/follow` | JWT | `follow` | `-` | - | 200 | `ApiEnvelopeFollowActionResponse` | targetMemberId:integer*, following:boolean* |
| DELETE | `/api/users/{id}/follow` | JWT | `unfollow` | `-` | - | 200 | `ApiEnvelopeFollowActionResponse` | targetMemberId:integer*, following:boolean* |
| GET | `/api/users/{id}/followers` | Public | `getFollowers` | `-` | - | 200 | `ApiEnvelopeFollowListResponse` | items:array<FollowUserItem>*, totalElements:integer*, totalPages:integer*, page:integer*, size:integer* |
| GET | `/api/users/{id}/following` | Public | `getFollowing` | `-` | - | 200 | `ApiEnvelopeFollowListResponse` | items:array<FollowUserItem>*, totalElements:integer*, totalPages:integer*, page:integer*, size:integer* |

## question

- 엔드포인트 수: **27**
- 서비스 로직 핵심 플로우:
  - 1) 질문 조회는 QuestionService가 상태/풀정보/가시성 규칙을 반영해 반환.
  - 2) Draft Open은 active draft UNIQUE 슬롯을 사용해 동시 작성 충돌 차단.
  - 3) Draft Submit은 idempotency 키 + draft/credit 락 + quota/중복/카테고리 검증을 순차 적용.
  - 4) 성공 시 크레딧 차감 원장 기록, 질문 생성, draft consumed 처리 후 캐시 후처리.

| Method | Path | Auth | OperationId | Request DTO | Request Fields | Resp | Response DTO | Response Data Fields |
|---|---|---|---|---|---|---|---|---|
| GET | `/api/admin/questions` | Admin | `getAllQuestions_1` | `-` | - | 200 | `ApiEnvelopePageQuestionAdminView` | totalElements:integer, totalPages:integer, pageable:PageableObject, numberOfElements:integer, first:boolean, last:boolean, size:integer, content:array<QuestionAdminView>, number:integer, sort:SortObject, empty:boolean |
| POST | `/api/admin/questions` | Admin | `createQuestion` | `AdminCreateQuestionRequest` | title:string*, type:string*, marketType:string*, resolutionRule:string*, resolutionSource:string, resolveAt:string, disputeUntil:string, voteResultSettlement:boolean, category:string, votingDuration:integer*, bettingDuration:integer*, executionModel:string* | 200 | `ApiEnvelopeQuestionCreationResponse` | success:boolean*, questionId:integer*, title:string*, category:string*, expiredAt:string*, message:string* |
| POST | `/api/admin/questions/generate` | Admin | `generateQuestion` | `-` | - | 200 | `QuestionGenerationResponse` | success:boolean*, questionId:integer, title:string, category:string, message:string*, isDemoMode:boolean* |
| POST | `/api/admin/questions/generate-batch` | Admin | `generateBatch` | `BatchGenerateQuestionsRequest` | subcategory:string, targetDate:string, dryRun:boolean* | 200 | `ApiEnvelopeBatchGenerateQuestionsResponse` | success:boolean*, batchId:string*, generatedAt:string*, subcategory:string*, requestedCount:integer*, acceptedCount:integer*, rejectedCount:integer*, opinionCount:integer*, verifiableCount:integer*, drafts:array<GeneratedQuestionDraftDto>*, message:string |
| GET | `/api/admin/questions/generation-batches/{batchId}` | Admin | `getBatch` | `-` | - | 200 | `ApiEnvelopeBatchGenerateQuestionsResponse` | success:boolean*, batchId:string*, generatedAt:string*, subcategory:string*, requestedCount:integer*, acceptedCount:integer*, rejectedCount:integer*, opinionCount:integer*, verifiableCount:integer*, drafts:array<GeneratedQuestionDraftDto>*, message:string |
| POST | `/api/admin/questions/generation-batches/{batchId}/publish` | Admin | `publishBatch` | `PublishGeneratedBatchRequest` | publishDraftIds:array<string>* | 200 | `ApiEnvelopePublishResultResponse` | success:boolean*, batchId:string*, publishedCount:integer*, failedCount:integer*, message:string |
| POST | `/api/admin/questions/generation-batches/{batchId}/retry` | Admin | `retryBatch` | `RetryFailedGenerationRequest` | subcategory:string, maxRetryCount:integer*, force:boolean* | 200 | `ApiEnvelopeBatchGenerateQuestionsResponse` | success:boolean*, batchId:string*, generatedAt:string*, subcategory:string*, requestedCount:integer*, acceptedCount:integer*, rejectedCount:integer*, opinionCount:integer*, verifiableCount:integer*, drafts:array<GeneratedQuestionDraftDto>*, message:string |
| POST | `/api/admin/questions/legacy` | Admin | `createQuestionLegacy` | `CreateQuestionRequest` | title:string*, category:string*, expiredAt:string*, categoryWeight:number | 200 | `ApiEnvelopeQuestionCreationResponse` | success:boolean*, questionId:integer*, title:string*, category:string*, expiredAt:string*, message:string* |
| DELETE | `/api/admin/questions/purge-all` | Admin | `purgeAllQuestions` | `-` | - | 200 | `ApiEnvelopePurgeQuestionsResponse` | success:boolean*, deletedQuestions:integer*, cleanedTables:object*, message:string* |
| PUT | `/api/admin/questions/{id}` | Admin | `updateQuestion` | `UpdateQuestionRequest` | title:string, category:string, expiredAt:string | 200 | `ApiEnvelopeQuestionCreationResponse` | success:boolean*, questionId:integer*, title:string*, category:string*, expiredAt:string*, message:string* |
| DELETE | `/api/admin/questions/{id}` | Admin | `deleteQuestion` | `-` | - | 200 | `ApiEnvelopeDeleteQuestionResponse` | success:boolean*, message:string* |
| GET | `/api/admin/settings/question-generator` | Admin | `getSettings` | `-` | - | 200 | `ApiEnvelopeQuestionGeneratorSettingsResponse` | enabled:boolean*, intervalSeconds:integer*, categories:array<string>*, region:string*, dailyCount:integer*, opinionCount:integer*, votingHours:integer*, bettingHours:integer*, breakMinutes:integer*, revealMinutes:integer*, lastGeneratedAt:string, isDemoMode:boolean* |
| PUT | `/api/admin/settings/question-generator` | Admin | `updateSettings` | `UpdateQuestionGeneratorSettingsRequest` | enabled:boolean, intervalSeconds:integer, categories:array<string>, region:string, dailyCount:integer, opinionCount:integer, votingHours:integer, bettingHours:integer, breakMinutes:integer, revealMinutes:integer | 200 | `ApiEnvelopeQuestionGeneratorSettingsResponse` | enabled:boolean*, intervalSeconds:integer*, categories:array<string>*, region:string*, dailyCount:integer*, opinionCount:integer*, votingHours:integer*, bettingHours:integer*, breakMinutes:integer*, revealMinutes:integer*, lastGeneratedAt:string, isDemoMode:boolean* |
| GET | `/api/questions` | Public | `getAllQuestions` | `-` | - | 200 | `ApiEnvelopeListQuestionResponse` | array<id:integer*, title:string*, category:string, status:string*, type:string*, executionModel:string*, finalResult:string, totalBetPool:integer*> |
| GET | `/api/questions/credits/status` | Public | `getCreditStatus` | `-` | - | 200 | `ApiEnvelopeCreditStatusResponse` | yearlyBudget:integer*, usedCredits:integer*, availableCredits:integer*, requiredCredits:integer*, requiredCreditsByVoteWindow:object*, resetAtUtc:string* |
| POST | `/api/questions/drafts/open` | JWT | `openDraft` | `-` | - | 201 | `ApiEnvelopeDraftOpenResponse` | draftId:string*, expiresAt:string*, submitIdempotencyKey:string* |
| POST | `/api/questions/drafts/{draftId}/cancel` | JWT | `cancelDraft` | `-` | - | 200 | `ApiEnvelopeUnit` | object |
| POST | `/api/questions/drafts/{draftId}/submit` | JWT | `submitDraft` | `SubmitQuestionDraftRequest` | title:string*, category:string*, description:string, voteWindowType:string*, settlementMode:string*, resolutionRule:string, resolutionSource:string, thumbnailUrl:string, tags:array<string>*, sourceLinks:array<string>*, boostEnabled:boolean*, creatorSplitInPool:integer* | 201 | `ApiEnvelopeDraftSubmitResponse` | questionId:integer*, remainingCredits:integer*, usedCredits:integer*, votingEndAt:string* |
| GET | `/api/questions/me/created` | Public | `getMyCreatedQuestions` | `-` | - | 200 | `ApiEnvelopeListCreatedQuestionItemResponse` | array<questionId:integer*, title:string*, category:string, status:string*, totalVotes:integer*, earnings:number*, createdAt:string*> |
| GET | `/api/questions/status/{status}` | Public | `getQuestionsByStatus` | `-` | - | 200 | `ApiEnvelopeListQuestionResponse` | array<id:integer*, title:string*, category:string, status:string*, type:string*, executionModel:string*, finalResult:string, totalBetPool:integer*> |
| GET | `/api/questions/top3` | Public | `getTop3Questions` | `-` | - | 200 | `ApiEnvelopeListTopQuestionResponse` | array<questionId:integer*, title:string*, category:string, yesVotes:integer*, noVotes:integer*, totalVotes:integer*, yesPrice:number*, lastVoteAt:string*> |
| GET | `/api/questions/{id}` | Public | `getQuestion` | `-` | - | 200 | `ApiEnvelopeQuestionResponse` | id:integer*, title:string*, category:string, status:string*, type:string*, executionModel:string*, finalResult:string, totalBetPool:integer*, yesBetPool:integer*, noBetPool:integer*, yesPercentage:number*, noPercentage:number* |
| GET | `/api/questions/{id}/comments` | Public | `getComments` | `-` | - | 200 | `ApiEnvelopeCommentListResponse` | items:array<QuestionCommentResponse>*, totalElements:integer*, totalPages:integer*, page:integer*, size:integer* |
| POST | `/api/questions/{id}/comments` | JWT | `createComment` | `CreateCommentRequest` | content:string*, parentCommentId:integer | 200 | `ApiEnvelopeQuestionCommentResponse` | commentId:integer*, questionId:integer*, memberId:integer*, username:string, displayName:string, avatarUrl:string, parentCommentId:integer, content:string*, likeCount:integer*, isMine:boolean*, createdAt:string*, updatedAt:string* |
| GET | `/api/questions/{id}/odds` | Public | `getOdds` | `-` | - | 200 | `ApiEnvelopeOddsResponse` | questionId:integer*, yesOdds:string*, noOdds:string*, yesPrice:string*, noPrice:string*, poolYes:integer*, poolNo:integer*, totalPool:integer* |
| POST | `/api/questions/{id}/view` | JWT | `incrementViewCount` | `-` | - | 200 | `ApiEnvelopeBoolean` | boolean |
| DELETE | `/api/questions/{questionId}/comments/{commentId}` | JWT | `deleteComment` | `-` | - | 200 | `ApiEnvelopeUnit` | object |

## voting

- 엔드포인트 수: **21**
- 서비스 로직 핵심 플로우:
  - 1) VoteCommandService가 질문 상태·중복·일일한도를 검증 후 투표를 원자적으로 저장.
  - 2) 투표 저장 후 vote_summary upsert + WebSocket(`/topic/votes`) 브로드캐스트.
  - 3) VoteRecord와 OnChainVoteRelay 큐를 생성해 온체인 릴레이 파이프라인으로 전달.
  - 4) VoteQueryService는 canVote/remainingDailyVotes를 읽기 전용으로 계산.

| Method | Path | Auth | OperationId | Request DTO | Request Fields | Resp | Response DTO | Response Data Fields |
|---|---|---|---|---|---|---|---|---|
| GET | `/api/activities/me` | JWT | `getMyActivities` | `-` | - | 200 | `ApiEnvelopeListMemberActivityView` | array<id:integer, questionId:integer*, activityType:string*, choice:string*, amount:integer*, createdAt:string*> |
| GET | `/api/activities/me/question/{questionId}` | JWT | `getMyActivitiesByQuestion` | `-` | - | 200 | `ApiEnvelopeListMemberQuestionActivityView` | array<id:integer, questionId:integer*, activityType:string*, choice:string*, amount:integer*, latencyMs:integer, parentBetId:integer, createdAt:string*> |
| GET | `/api/activities/question/{questionId}` | JWT | `getActivitiesByQuestion` | `-` | - | 200 | `ApiEnvelopeListQuestionActivityView` | array<id:integer, memberId:integer*, questionId:integer*, activityType:string*, choice:string*, amount:integer*, latencyMs:integer, parentBetId:integer> |
| GET | `/api/admin/vote-ops/relay` | Admin | `getRelayList` | `-` | - | 200 | `ApiEnvelopeRelayListResponse` | total:integer*, page:integer*, size:integer*, totalPages:integer*, items:array<RelayItemSummary>* |
| POST | `/api/admin/vote-ops/relay/{id}/retry` | Admin | `forceRetry` | `-` | - | 200 | `ApiEnvelopeRelayItemSummary` | id:integer*, voteId:integer*, memberId:integer*, questionId:integer*, choice:string*, status:string*, retryCount:integer*, txHash:string, errorMessage:string, nextRetryAt:string, createdAt:string*, updatedAt:string* |
| GET | `/api/admin/vote-ops/summary/{questionId}` | Admin | `getSummary` | `-` | - | 200 | `ApiEnvelopeVoteSummaryResponse` | questionId:integer*, yesCount:integer*, noCount:integer*, totalCount:integer* |
| GET | `/api/admin/vote-ops/usage` | Admin | `getUsage` | `-` | - | 200 | `ApiEnvelopeUsageSummaryResponse` | date:string*, totalMembers:integer*, totalVotes:integer*, page:integer*, size:integer*, totalPages:integer*, entries:array<UsageEntry>* |
| POST | `/api/admin/voting/circuit-breaker/reset` | Admin | `resetCircuitBreaker` | `-` | - | 200 | `ApiEnvelopeVotingPauseResponse` | message:string*, questionId:integer |
| POST | `/api/admin/voting/pause-all` | Admin | `pauseAll` | `-` | - | 200 | `ApiEnvelopeVotingPauseResponse` | message:string*, questionId:integer |
| POST | `/api/admin/voting/pause/{questionId}` | Admin | `pauseVoting` | `-` | - | 200 | `ApiEnvelopeVotingPauseResponse` | message:string*, questionId:integer |
| POST | `/api/admin/voting/resume-all` | Admin | `resumeAll` | `-` | - | 200 | `ApiEnvelopeVotingPauseResponse` | message:string*, questionId:integer |
| POST | `/api/admin/voting/resume/{questionId}` | Admin | `resumeVoting` | `-` | - | 200 | `ApiEnvelopeVotingPauseResponse` | message:string*, questionId:integer |
| GET | `/api/admin/voting/status` | Admin | `getStatus_2` | `-` | - | 200 | `ApiEnvelopeVotingSystemStatusResponse` | pauseStatus:object*, circuitBreaker:object* |
| GET | `/api/tickets/status` | JWT | `getStatus_1` | `-` | - | 200 | `ApiEnvelopeTicketStatusResponse` | remainingTickets:integer*, maxTickets:integer*, resetAt:string* |
| POST | `/api/votes` | JWT | `vote` | `VoteRequest` | questionId:integer*, choice:string*, latencyMs:integer | 200 | `ApiEnvelopeVoteResponse` | voteId:integer*, questionId:integer*, choice:string*, remainingDailyVotes:integer*, onChainStatus:string* |
| POST | `/api/votes/commit` | JWT | `commit` | `VoteCommitRequest` | questionId:integer*, commitHash:string* | 200 | `VoteCommitResponse` | success:boolean*, message:string*, voteCommitId:integer, remainingTickets:integer |
| GET | `/api/votes/feed` | Public (Optional JWT) | `getFeed` | `-` | - | 200 | `ApiEnvelopeListVoteFeedItemResponse` | array<questionId:integer*, title:string*, category:string, yesVotes:integer*, noVotes:integer*, totalVotes:integer*, yesPrice:number*, submitterId:integer> |
| GET | `/api/votes/results/{questionId}` | JWT | `getResults` | `-` | - | 200 | `ApiEnvelopeMapStringObject` | object |
| POST | `/api/votes/reveal` | JWT | `reveal` | `VoteRevealRequest` | questionId:integer*, choice:string*, salt:string* | 200 | `VoteRevealResponse` | success:boolean*, message:string* |
| GET | `/api/votes/status/{questionId}` | Public (Optional JWT) | `getStatus` | `-` | - | 200 | `ApiEnvelopeVoteStatusResponse` | canVote:boolean*, alreadyVoted:boolean*, remainingVotes:integer*, reason:string |
| POST | `/api/voting-pass/purchase` | JWT | `purchaseVotingPass` | `VotingPassPurchaseRequest` | memberId:integer* | 200 | `ApiEnvelopeUnit` | object |

## market-amm

- 엔드포인트 수: **14**
- 서비스 로직 핵심 플로우:
  - 1) SwapService가 FPMM 수식엔진(FpmmMathEngine)으로 BUY/SELL 결과를 계산.
  - 2) 슬리피지(minOut), 보유수량, 잔고, pool 상태를 정책(AmmTradePolicy)으로 검증.
  - 3) 체결 시 pool/userShares/walletLedger/feePool/swapHistory를 트랜잭션 내 일괄 반영.
  - 4) 체결 직후 `/topic/pool/{id}` 및 `/topic/markets` 실시간 이벤트 발행.

| Method | Path | Auth | OperationId | Request DTO | Request Fields | Resp | Response DTO | Response Data Fields |
|---|---|---|---|---|---|---|---|---|
| GET | `/api/admin/markets/batches` | Admin | `listBatches` | `-` | - | 200 | `ApiEnvelopeListMarketBatchSummaryResponse` | array<id:integer*, cutoffSlotUtc:string*, status:string*, startedAt:string*, finishedAt:string, totalCandidates:integer*, selectedCount:integer*, openedCount:integer*> |
| GET | `/api/admin/markets/batches/{batchId}/candidates` | Admin | `getCandidates` | `-` | - | 200 | `ApiEnvelopeListMarketCandidateResponse` | array<id:integer*, batchId:integer*, questionId:integer*, category:string*, voteCount:integer*, rankInCategory:integer*, selectionStatus:string*, selectionReason:string> |
| POST | `/api/admin/markets/batches/{batchId}/retry-open` | Admin | `retryOpen` | `-` | - | 200 | `ApiEnvelopeRetryOpenResponse` | batchId:integer*, status:string*, openedCount:integer*, failedCount:integer* |
| GET | `/api/admin/markets/batches/{batchId}/summary` | Admin | `getSummary` | `-` | - | 200 | `ApiEnvelopeMarketBatchSummaryDetailResponse` | batchId:integer*, status:string*, totalCandidates:integer*, selectedCount:integer*, openedCount:integer*, failedCount:integer*, successRate:number* |
| POST | `/api/admin/markets/batches/{cutoffSlot}/run` | Admin | `runBatch` | `-` | - | 200 | `ApiEnvelopeMarketBatchSummaryResponse` | id:integer*, cutoffSlotUtc:string*, status:string*, startedAt:string*, finishedAt:string, totalCandidates:integer*, selectedCount:integer*, openedCount:integer*, failedCount:integer*, errorSummary:string |
| POST | `/api/pool/seed` | JWT | `seedPool` | `SeedPoolRequest` | questionId:integer*, seedUsdc:number*, feeRate:number* | 200 | `ApiEnvelopeSeedPoolResponse` | questionId:integer*, yesShares:number*, noShares:number*, collateralLocked:number*, feeRate:number*, k:number*, currentPrice:PriceSnapshot* |
| GET | `/api/pool/{questionId}` | Public | `getPoolState` | `-` | - | 200 | `ApiEnvelopePoolStateResponse` | questionId:integer*, status:string*, yesShares:number*, noShares:number*, k:number*, feeRate:number*, collateralLocked:number*, totalVolumeUsdc:number*, totalFeesUsdc:number*, currentPrice:PriceSnapshot*, version:integer* |
| GET | `/api/questions/top3` | Public | `getTop3Questions` | `-` | - | 200 | `ApiEnvelopeListTopQuestionResponse` | array<questionId:integer*, title:string*, category:string, yesVotes:integer*, noVotes:integer*, totalVotes:integer*, yesPrice:number*, lastVoteAt:string*> |
| POST | `/api/swap` | JWT | `executeSwap` | `SwapRequest` | questionId:integer*, action:string*, outcome:string*, usdcIn:number, sharesIn:number, minSharesOut:number, minUsdcOut:number | 200 | `ApiEnvelopeSwapResponse` | sharesAmount:number*, usdcAmount:number*, effectivePrice:number*, fee:number*, priceBefore:PriceSnapshot*, priceAfter:PriceSnapshot*, poolState:PoolSnapshot*, myShares:MySharesSnapshot* |
| GET | `/api/swap/history/{questionId}` | Public | `getSwapHistory` | `-` | - | 200 | `ApiEnvelopeListSwapHistoryResponse` | array<swapId:integer*, memberId:integer*, memberEmail:string, action:string*, outcome:string*, usdcAmount:number*, sharesAmount:number*, effectivePrice:number*> |
| GET | `/api/swap/my-history/{questionId}` | JWT | `getMySwapHistory` | `-` | - | 200 | `ApiEnvelopeListSwapHistoryResponse` | array<swapId:integer*, memberId:integer*, memberEmail:string, action:string*, outcome:string*, usdcAmount:number*, sharesAmount:number*, effectivePrice:number*> |
| GET | `/api/swap/my-shares/{questionId}` | JWT | `getMyShares` | `-` | - | 200 | `ApiEnvelopeMySharesSnapshot` | yesShares:number*, noShares:number*, yesCostBasis:number*, noCostBasis:number* |
| GET | `/api/swap/price-history/{questionId}` | Public | `getPriceHistory` | `-` | - | 200 | `ApiEnvelopeListPricePointResponse` | array<timestamp:string*, yesPrice:number*, noPrice:number*> |
| GET | `/api/swap/simulate` | Public | `simulateSwap` | `-` | - | 200 | `ApiEnvelopeSwapSimulationResponse` | sharesOut:number, usdcOut:number, effectivePrice:number*, slippage:number*, fee:number*, minReceived:number*, priceBefore:PriceSnapshot*, priceAfter:PriceSnapshot* |

## settlement-reward

- 엔드포인트 수: **23**
- 서비스 로직 핵심 플로우:
  - 1) initiateSettlement: 결과/근거를 기록하고 dispute deadline을 설정.
  - 2) finalizeSettlement: 승리 side 계산 후 payout/풀 정리/질문 상태 확정.
  - 3) 자동정산은 adapter 결과의 confidence/소스상태를 검증한 뒤 수행.
  - 4) 정산 후 creator share, voter reward 분배 및 후속 상태전환 처리.

| Method | Path | Auth | OperationId | Request DTO | Request Fields | Resp | Response DTO | Response Data Fields |
|---|---|---|---|---|---|---|---|---|
| POST | `/api/admin/rewards/distribute/{questionId}` | Admin | `distributeRewards` | `-` | - | 200 | `ApiEnvelopeMapStringObject` | object |
| POST | `/api/admin/rewards/retry/{questionId}` | Admin | `retryFailedDistributions` | `-` | - | 200 | `ApiEnvelopeMapStringObject` | object |
| POST | `/api/admin/settlements/questions/{id}/cancel` | Admin | `cancelSettlement` | `-` | - | 200 | `object` | object |
| POST | `/api/admin/settlements/questions/{id}/finalize` | Admin | `finalizeSettlement` | `FinalizeSettlementRequest` | force:boolean* | 200 | `object` | object |
| POST | `/api/admin/settlements/questions/{id}/settle` | Admin | `settleQuestion` | `SettleQuestionRequest` | finalResult:string*, sourceUrl:string | 200 | `object` | object |
| POST | `/api/admin/settlements/questions/{id}/settle-auto` | Admin | `settleQuestionAuto` | `-` | - | 200 | `object` | object |
| GET | `/api/analysis/questions/{id}/abusing-report` | JWT | `getAbusingReport` | `-` | - | 200 | `ApiEnvelopeAbusingReport` | questionId:integer*, suspiciousGroups:array<SuspiciousGroup>*, overallGap:number*, totalMembers:integer*, suspiciousMembers:integer*, recommendation:string* |
| GET | `/api/analysis/questions/{id}/by-country` | JWT | `getVotesByCountry` | `-` | - | 200 | `ApiEnvelopeObject` | object |
| GET | `/api/analysis/questions/{id}/fast-clickers` | JWT | `getFastClickers` | `-` | - | 200 | `ApiEnvelopeFastClickersResponse` | questionId:integer*, thresholdMs:integer*, suspiciousCount:integer*, percentage:number* |
| GET | `/api/analysis/questions/{id}/premium-data` | JWT | `getPremiumData` | `-` | - | 200 | `ApiEnvelopePremiumDataResponse` | questionId:integer*, questionTitle:string*, filters:PremiumDataRequest*, totalCount:integer*, yesCount:integer*, noCount:integer*, yesPercentage:number*, noPercentage:number*, data:array<PremiumDataPoint>* |
| GET | `/api/analysis/questions/{id}/quality-score` | JWT | `getQualityScore` | `-` | - | 200 | `ApiEnvelopeQualityScoreResponse` | questionId:integer*, qualityScore:number*, grade:string* |
| POST | `/api/analysis/questions/{id}/simulate-filter` | JWT | `simulateFilter` | `FilterOptions` | minLatencyMs:integer*, onlyBettors:boolean*, minTierWeight:number*, excludeCountries:array<string>* | 200 | `ApiEnvelopeFilterSimulationResponse` | questionId:integer*, filterOptions:FilterOptions*, originalVoteCount:integer*, filteredVoteCount:integer*, removedCount:integer*, originalYesPercentage:number*, filteredYesPercentage:number*, percentageChange:number* |
| GET | `/api/analysis/questions/{id}/weighted-votes` | JWT | `getWeightedVotes` | `-` | - | 200 | `ApiEnvelopeWeightedVoteResult` | rawYesPercentage:number*, weightedYesPercentage:number*, totalVotes:integer*, effectiveVotes:number* |
| GET | `/api/analytics/dashboard/{questionId}` | JWT | `getQualityDashboard` | `-` | - | 200 | `ApiEnvelopeQualityDashboard` | questionId:integer*, demographics:VoteDemographicsReport*, gapAnalysis:VoteBetGapReport*, filteringEffect:FilteringEffectReport*, overallQualityScore:number* |
| GET | `/api/analytics/demographics/{questionId}` | JWT | `getVoteDemographics` | `-` | - | 200 | `ApiEnvelopeVoteDemographicsReport` | questionId:integer*, totalVotes:integer*, byCountry:array<CountryVoteData>*, byJob:array<JobVoteData>*, byAge:array<AgeVoteData>* |
| GET | `/api/analytics/filtering-effect/{questionId}` | JWT | `getFilteringEffect` | `-` | - | 200 | `ApiEnvelopeFilteringEffectReport` | questionId:integer*, beforeFiltering:FilteringData*, afterFiltering:FilteringData*, filteredCount:integer*, filteredPercentage:number* |
| GET | `/api/analytics/gap-analysis/{questionId}` | JWT | `getVoteBetGapAnalysis` | `-` | - | 200 | `ApiEnvelopeVoteBetGapReport` | questionId:integer*, voteDistribution:DistributionData*, betDistribution:DistributionData*, gapPercentage:number*, qualityScore:number* |
| GET | `/api/analytics/global/stats` | JWT | `getGlobalStats` | `-` | - | 200 | `ApiEnvelopeMapStringObject` | object |
| POST | `/api/premium-data/export` | JWT | `exportPremiumData` | `PremiumDataRequest` | questionId:integer*, countryCode:string, jobCategory:string, ageGroup:integer, minTier:string, minLatencyMs:integer | 200 | `ApiEnvelopePremiumDataResponse` | questionId:integer*, questionTitle:string*, filters:PremiumDataRequest*, totalCount:integer*, yesCount:integer*, noCount:integer*, yesPercentage:number*, noPercentage:number*, data:array<PremiumDataPoint>* |
| POST | `/api/premium-data/preview` | JWT | `previewPremiumData` | `PremiumDataRequest` | questionId:integer*, countryCode:string, jobCategory:string, ageGroup:integer, minTier:string, minLatencyMs:integer | 200 | `ApiEnvelopePremiumDataResponse` | questionId:integer*, questionTitle:string*, filters:PremiumDataRequest*, totalCount:integer*, yesCount:integer*, noCount:integer*, yesPercentage:number*, noPercentage:number*, data:array<PremiumDataPoint>* |
| GET | `/api/premium-data/quality-summary/{questionId}` | JWT | `getQualitySummary` | `-` | - | 200 | `ApiEnvelopeDataQualitySummary` | questionId:integer*, totalVotes:integer*, byLatency:LatencyBreakdown*, byTier:object* |
| GET | `/api/rewards/{memberId}` | JWT | `getTotalRewards` | `-` | - | 200 | `ApiEnvelopeTotalRewardResponse` | memberId:integer*, currentBalance:integer*, totalVotes:integer*, tier:string*, tierWeight:number*, estimatedRewardPerVote:integer* |
| GET | `/api/settlements/history/me` | JWT | `getMySettlementHistory` | `-` | - | 200 | `ApiEnvelopeListSettlementHistoryItem` | array<questionId:integer*, questionTitle:string*, myChoice:string*, finalResult:string*, betAmount:integer*, payout:integer*, profit:integer*, isWinner:boolean*> |

## finance-wallet

- 엔드포인트 수: **12**
- 서비스 로직 핵심 플로우:
  - 1) 입금 검증은 온체인 영수증+Transfer 이벤트를 파싱해 sender/receiver/amount 일치 확인.
  - 2) 검증 성공 시 payment_tx + wallet ledger + treasury ledger + transaction history 반영.
  - 3) 출금은 lock->전송->receipt 확인->confirm/fail(잠금해제) 2단계로 안정성 확보.
  - 4) 포트폴리오는 활동기록과 질문상태를 결합해 요약/오픈포지션/카테고리/정확도 추세 계산.

| Method | Path | Auth | OperationId | Request DTO | Request Fields | Resp | Response DTO | Response Data Fields |
|---|---|---|---|---|---|---|---|---|
| GET | `/api/admin/finance/treasury-ledgers` | Admin | `getTreasuryLedgers` | `-` | - | 200 | `ApiEnvelopePagedResponseTreasuryLedgerRow` | page:integer*, size:integer*, totalElements:integer*, totalPages:integer*, items:array<TreasuryLedgerRow>* |
| GET | `/api/admin/finance/wallet-ledgers` | Admin | `getWalletLedgers` | `-` | - | 200 | `ApiEnvelopePagedResponseWalletLedgerRow` | page:integer*, size:integer*, totalElements:integer*, totalPages:integer*, items:array<WalletLedgerRow>* |
| GET | `/api/admin/finance/wallets/{memberId}` | Admin | `getWallet` | `-` | - | 200 | `ApiEnvelopeMemberWalletResponse` | memberId:integer*, availableBalance:number*, lockedBalance:number*, updatedAt:string* |
| GET | `/api/blockchain/question/{questionId}` | JWT | `getQuestionFromChain` | `-` | - | 200 | `ApiEnvelopeQuestionOnChain` | questionId:integer*, totalBetPool:integer*, yesBetPool:integer*, noBetPool:integer*, settled:boolean* |
| GET | `/api/blockchain/status` | JWT | `getBlockchainStatus` | `-` | - | 200 | `ApiEnvelopeBlockchainStatusResponse` | enabled:boolean*, network:string*, totalQuestions:integer*, totalTransactions:integer* |
| POST | `/api/payments/verify-deposit` | JWT | `verifyDeposit` | `VerifyDepositRequest` | txHash:string*, amount:number*, fromAddress:string | 200 | `object` | object |
| POST | `/api/payments/withdraw` | JWT | `withdraw` | `WithdrawRequest` | amount:number*, walletAddress:string* | 200 | `ApiEnvelopeWithdrawResponse` | success:boolean*, txHash:string, amount:number, fee:number, totalDebited:number, newBalance:number, message:string* |
| GET | `/api/portfolio/accuracy-trend` | JWT | `getAccuracyTrend` | `-` | - | 200 | `ApiEnvelopeObject` | object |
| GET | `/api/portfolio/category-breakdown` | JWT | `getCategoryBreakdown` | `-` | - | 200 | `ApiEnvelopeObject` | object |
| GET | `/api/portfolio/positions` | JWT | `getOpenPositions` | `-` | - | 200 | `ApiEnvelopeObject` | object |
| GET | `/api/portfolio/summary` | JWT | `getPortfolioSummary` | `-` | - | 200 | `ApiEnvelopeObject` | object |
| GET | `/api/transactions/my` | JWT | `getMyTransactions` | `-` | - | 200 | `ApiEnvelopeTransactionHistoryResponse` | content:array<TransactionItemDto>*, totalElements:integer*, totalPages:integer*, page:integer*, size:integer* |

## ops-admin

- 엔드포인트 수: **60**
- 서비스 로직 핵심 플로우:
  - 1) 마켓 배치는 cutoff 기준으로 Top3 선별 후 seedPool + BREAK→BETTING 전환.
  - 2) 라이프사이클 스케줄러가 VOTING/BREAK/BETTING/REVEAL 전환 및 자동정산 트리거를 관리.
  - 3) 스포츠 스케줄러가 football-data.org 동기화, 골/종료 이벤트 발행, 질문 생성 연계.
  - 4) 릴레이 스케줄러가 onchain relay 상태(PENDING/FAILED/CONFIRMED)와 백오프 재시도를 관리.

| Method | Path | Auth | OperationId | Request DTO | Request Fields | Resp | Response DTO | Response Data Fields |
|---|---|---|---|---|---|---|---|---|
| POST | `/api/admin/abuse/ban` | Admin | `banMember` | `BanRequest` | memberId:integer*, reason:string* | 200 | `object` | object |
| GET | `/api/admin/abuse/banned` | Admin | `getBannedMembers` | `-` | - | 200 | `object` | object |
| GET | `/api/admin/abuse/ip-lookup` | Admin | `lookupIp` | `-` | - | 200 | `object` | object |
| GET | `/api/admin/abuse/member-ips/{memberId}` | Admin | `getMemberIps` | `-` | - | 200 | `object` | object |
| GET | `/api/admin/abuse/multi-account-report` | Admin | `getMultiAccountReport` | `-` | - | 200 | `object` | object |
| POST | `/api/admin/abuse/unban` | Admin | `unbanMember` | `UnbanRequest` | memberId:integer* | 200 | `object` | object |
| GET | `/api/admin/audit-logs` | Admin | `getAuditLogs` | `-` | - | 200 | `ApiEnvelopePageAuditLogResponse` | totalElements:integer, totalPages:integer, pageable:PageableObject, numberOfElements:integer, first:boolean, last:boolean, size:integer, content:array<AuditLogResponse>, number:integer, sort:SortObject, empty:boolean |
| POST | `/api/admin/bot/init` | Admin | `initBots` | `-` | - | 200 | `ApiEnvelopeMapStringObject` | object |
| POST | `/api/admin/bot/trade` | Admin | `triggerTrading` | `-` | - | 200 | `ApiEnvelopeMapStringString` | object |
| POST | `/api/admin/bot/vote` | Admin | `triggerVoting` | `-` | - | 200 | `ApiEnvelopeMapStringString` | object |
| GET | `/api/admin/dashboard/voting` | Admin | `getVotingDashboard` | `-` | - | 200 | `ApiEnvelopeMapStringObject` | object |
| GET | `/api/admin/dashboard/voting/{questionId}` | Admin | `getQuestionHealth` | `-` | - | 200 | `ApiEnvelopeMapStringObject` | object |
| GET | `/api/admin/finance/treasury-ledgers` | Admin | `getTreasuryLedgers` | `-` | - | 200 | `ApiEnvelopePagedResponseTreasuryLedgerRow` | page:integer*, size:integer*, totalElements:integer*, totalPages:integer*, items:array<TreasuryLedgerRow>* |
| GET | `/api/admin/finance/wallet-ledgers` | Admin | `getWalletLedgers` | `-` | - | 200 | `ApiEnvelopePagedResponseWalletLedgerRow` | page:integer*, size:integer*, totalElements:integer*, totalPages:integer*, items:array<WalletLedgerRow>* |
| GET | `/api/admin/finance/wallets/{memberId}` | Admin | `getWallet` | `-` | - | 200 | `ApiEnvelopeMemberWalletResponse` | memberId:integer*, availableBalance:number*, lockedBalance:number*, updatedAt:string* |
| GET | `/api/admin/markets/batches` | Admin | `listBatches` | `-` | - | 200 | `ApiEnvelopeListMarketBatchSummaryResponse` | array<id:integer*, cutoffSlotUtc:string*, status:string*, startedAt:string*, finishedAt:string, totalCandidates:integer*, selectedCount:integer*, openedCount:integer*> |
| GET | `/api/admin/markets/batches/{batchId}/candidates` | Admin | `getCandidates` | `-` | - | 200 | `ApiEnvelopeListMarketCandidateResponse` | array<id:integer*, batchId:integer*, questionId:integer*, category:string*, voteCount:integer*, rankInCategory:integer*, selectionStatus:string*, selectionReason:string> |
| POST | `/api/admin/markets/batches/{batchId}/retry-open` | Admin | `retryOpen` | `-` | - | 200 | `ApiEnvelopeRetryOpenResponse` | batchId:integer*, status:string*, openedCount:integer*, failedCount:integer* |
| GET | `/api/admin/markets/batches/{batchId}/summary` | Admin | `getSummary_1` | `-` | - | 200 | `ApiEnvelopeMarketBatchSummaryDetailResponse` | batchId:integer*, status:string*, totalCandidates:integer*, selectedCount:integer*, openedCount:integer*, failedCount:integer*, successRate:number* |
| POST | `/api/admin/markets/batches/{cutoffSlot}/run` | Admin | `runBatch` | `-` | - | 200 | `ApiEnvelopeMarketBatchSummaryResponse` | id:integer*, cutoffSlotUtc:string*, status:string*, startedAt:string*, finishedAt:string, totalCandidates:integer*, selectedCount:integer*, openedCount:integer*, failedCount:integer*, errorSummary:string |
| GET | `/api/admin/questions` | Admin | `getAllQuestions` | `-` | - | 200 | `ApiEnvelopePageQuestionAdminView` | totalElements:integer, totalPages:integer, pageable:PageableObject, numberOfElements:integer, first:boolean, last:boolean, size:integer, content:array<QuestionAdminView>, number:integer, sort:SortObject, empty:boolean |
| POST | `/api/admin/questions` | Admin | `createQuestion` | `AdminCreateQuestionRequest` | title:string*, type:string*, marketType:string*, resolutionRule:string*, resolutionSource:string, resolveAt:string, disputeUntil:string, voteResultSettlement:boolean, category:string, votingDuration:integer*, bettingDuration:integer*, executionModel:string* | 200 | `ApiEnvelopeQuestionCreationResponse` | success:boolean*, questionId:integer*, title:string*, category:string*, expiredAt:string*, message:string* |
| POST | `/api/admin/questions/generate` | Admin | `generateQuestion` | `-` | - | 200 | `QuestionGenerationResponse` | success:boolean*, questionId:integer, title:string, category:string, message:string*, isDemoMode:boolean* |
| POST | `/api/admin/questions/generate-batch` | Admin | `generateBatch` | `BatchGenerateQuestionsRequest` | subcategory:string, targetDate:string, dryRun:boolean* | 200 | `ApiEnvelopeBatchGenerateQuestionsResponse` | success:boolean*, batchId:string*, generatedAt:string*, subcategory:string*, requestedCount:integer*, acceptedCount:integer*, rejectedCount:integer*, opinionCount:integer*, verifiableCount:integer*, drafts:array<GeneratedQuestionDraftDto>*, message:string |
| GET | `/api/admin/questions/generation-batches/{batchId}` | Admin | `getBatch` | `-` | - | 200 | `ApiEnvelopeBatchGenerateQuestionsResponse` | success:boolean*, batchId:string*, generatedAt:string*, subcategory:string*, requestedCount:integer*, acceptedCount:integer*, rejectedCount:integer*, opinionCount:integer*, verifiableCount:integer*, drafts:array<GeneratedQuestionDraftDto>*, message:string |
| POST | `/api/admin/questions/generation-batches/{batchId}/publish` | Admin | `publishBatch` | `PublishGeneratedBatchRequest` | publishDraftIds:array<string>* | 200 | `ApiEnvelopePublishResultResponse` | success:boolean*, batchId:string*, publishedCount:integer*, failedCount:integer*, message:string |
| POST | `/api/admin/questions/generation-batches/{batchId}/retry` | Admin | `retryBatch` | `RetryFailedGenerationRequest` | subcategory:string, maxRetryCount:integer*, force:boolean* | 200 | `ApiEnvelopeBatchGenerateQuestionsResponse` | success:boolean*, batchId:string*, generatedAt:string*, subcategory:string*, requestedCount:integer*, acceptedCount:integer*, rejectedCount:integer*, opinionCount:integer*, verifiableCount:integer*, drafts:array<GeneratedQuestionDraftDto>*, message:string |
| POST | `/api/admin/questions/legacy` | Admin | `createQuestionLegacy` | `CreateQuestionRequest` | title:string*, category:string*, expiredAt:string*, categoryWeight:number | 200 | `ApiEnvelopeQuestionCreationResponse` | success:boolean*, questionId:integer*, title:string*, category:string*, expiredAt:string*, message:string* |
| DELETE | `/api/admin/questions/purge-all` | Admin | `purgeAllQuestions` | `-` | - | 200 | `ApiEnvelopePurgeQuestionsResponse` | success:boolean*, deletedQuestions:integer*, cleanedTables:object*, message:string* |
| PUT | `/api/admin/questions/{id}` | Admin | `updateQuestion` | `UpdateQuestionRequest` | title:string, category:string, expiredAt:string | 200 | `ApiEnvelopeQuestionCreationResponse` | success:boolean*, questionId:integer*, title:string*, category:string*, expiredAt:string*, message:string* |
| DELETE | `/api/admin/questions/{id}` | Admin | `deleteQuestion` | `-` | - | 200 | `ApiEnvelopeDeleteQuestionResponse` | success:boolean*, message:string* |
| POST | `/api/admin/rewards/distribute/{questionId}` | Admin | `distributeRewards` | `-` | - | 200 | `ApiEnvelopeMapStringObject` | object |
| POST | `/api/admin/rewards/retry/{questionId}` | Admin | `retryFailedDistributions` | `-` | - | 200 | `ApiEnvelopeMapStringObject` | object |
| GET | `/api/admin/settings/question-generator` | Admin | `getSettings` | `-` | - | 200 | `ApiEnvelopeQuestionGeneratorSettingsResponse` | enabled:boolean*, intervalSeconds:integer*, categories:array<string>*, region:string*, dailyCount:integer*, opinionCount:integer*, votingHours:integer*, bettingHours:integer*, breakMinutes:integer*, revealMinutes:integer*, lastGeneratedAt:string, isDemoMode:boolean* |
| PUT | `/api/admin/settings/question-generator` | Admin | `updateSettings` | `UpdateQuestionGeneratorSettingsRequest` | enabled:boolean, intervalSeconds:integer, categories:array<string>, region:string, dailyCount:integer, opinionCount:integer, votingHours:integer, bettingHours:integer, breakMinutes:integer, revealMinutes:integer | 200 | `ApiEnvelopeQuestionGeneratorSettingsResponse` | enabled:boolean*, intervalSeconds:integer*, categories:array<string>*, region:string*, dailyCount:integer*, opinionCount:integer*, votingHours:integer*, bettingHours:integer*, breakMinutes:integer*, revealMinutes:integer*, lastGeneratedAt:string, isDemoMode:boolean* |
| POST | `/api/admin/settlements/questions/{id}/cancel` | Admin | `cancelSettlement` | `-` | - | 200 | `object` | object |
| POST | `/api/admin/settlements/questions/{id}/finalize` | Admin | `finalizeSettlement` | `FinalizeSettlementRequest` | force:boolean* | 200 | `object` | object |
| POST | `/api/admin/settlements/questions/{id}/settle` | Admin | `settleQuestion` | `SettleQuestionRequest` | finalResult:string*, sourceUrl:string | 200 | `object` | object |
| POST | `/api/admin/settlements/questions/{id}/settle-auto` | Admin | `settleQuestionAuto` | `-` | - | 200 | `object` | object |
| POST | `/api/admin/sports/generate` | Admin | `manualGenerate` | `-` | - | 200 | `ApiEnvelopeMatchSyncResult` | fetched:integer*, inserted:integer*, updated:integer*, questionCreated:integer*, questionSkipped:integer* |
| POST | `/api/admin/sports/generate-match-questions` | Admin | `generateMatchQuestions` | `-` | - | 200 | `ApiEnvelopeMatchQuestionGenerateResult` | created:integer*, skipped:integer* |
| GET | `/api/admin/sports/live` | Admin | `getLiveMatches` | `-` | - | 200 | `ApiEnvelopeListLiveMatchInfo` | array<matchId:integer*, questionId:integer, leagueName:string*, homeTeam:string*, awayTeam:string*, homeScore:integer*, awayScore:integer*, matchDate:string*> |
| GET | `/api/admin/sports/questions` | Admin | `getMatchQuestions` | `-` | - | 200 | `ApiEnvelopeListMatchQuestionView` | array<questionId:integer*, title:string*, category:string, status:string*, phase:string, matchId:integer, matchTime:string, createdAt:string*> |
| GET | `/api/admin/sports/upcoming` | Admin | `getUpcomingMatches` | `-` | - | 200 | `ApiEnvelopeListUpcomingMatchInfo` | array<matchId:integer*, questionId:integer, leagueName:string*, homeTeam:string*, awayTeam:string*, matchTime:string*, status:string*> |
| POST | `/api/admin/sports/update-results` | Admin | `manualUpdateResults` | `-` | - | 200 | `ApiEnvelopeLivePollResult` | polled:integer*, updated:integer*, goalEvents:integer*, finishedEvents:integer*, cancelledEvents:integer* |
| GET | `/api/admin/sybil/report/{questionId}` | Admin | `getSybilReport` | `-` | - | 200 | `ApiEnvelopeSybilReportResponse` | questionId:integer*, suspiciousAccountCount:integer*, suspiciousAccounts:array<SybilSuspiciousAccount>* |
| GET | `/api/admin/vote-ops/relay` | Admin | `getRelayList` | `-` | - | 200 | `ApiEnvelopeRelayListResponse` | total:integer*, page:integer*, size:integer*, totalPages:integer*, items:array<RelayItemSummary>* |
| POST | `/api/admin/vote-ops/relay/{id}/retry` | Admin | `forceRetry` | `-` | - | 200 | `ApiEnvelopeRelayItemSummary` | id:integer*, voteId:integer*, memberId:integer*, questionId:integer*, choice:string*, status:string*, retryCount:integer*, txHash:string, errorMessage:string, nextRetryAt:string, createdAt:string*, updatedAt:string* |
| GET | `/api/admin/vote-ops/summary/{questionId}` | Admin | `getSummary` | `-` | - | 200 | `ApiEnvelopeVoteSummaryResponse` | questionId:integer*, yesCount:integer*, noCount:integer*, totalCount:integer* |
| GET | `/api/admin/vote-ops/usage` | Admin | `getUsage` | `-` | - | 200 | `ApiEnvelopeUsageSummaryResponse` | date:string*, totalMembers:integer*, totalVotes:integer*, page:integer*, size:integer*, totalPages:integer*, entries:array<UsageEntry>* |
| POST | `/api/admin/voting/circuit-breaker/reset` | Admin | `resetCircuitBreaker` | `-` | - | 200 | `ApiEnvelopeVotingPauseResponse` | message:string*, questionId:integer |
| POST | `/api/admin/voting/pause-all` | Admin | `pauseAll` | `-` | - | 200 | `ApiEnvelopeVotingPauseResponse` | message:string*, questionId:integer |
| POST | `/api/admin/voting/pause/{questionId}` | Admin | `pauseVoting` | `-` | - | 200 | `ApiEnvelopeVotingPauseResponse` | message:string*, questionId:integer |
| POST | `/api/admin/voting/resume-all` | Admin | `resumeAll` | `-` | - | 200 | `ApiEnvelopeVotingPauseResponse` | message:string*, questionId:integer |
| POST | `/api/admin/voting/resume/{questionId}` | Admin | `resumeVoting` | `-` | - | 200 | `ApiEnvelopeVotingPauseResponse` | message:string*, questionId:integer |
| GET | `/api/admin/voting/status` | Admin | `getStatus` | `-` | - | 200 | `ApiEnvelopeVotingSystemStatusResponse` | pauseStatus:object*, circuitBreaker:object* |
| GET | `/api/betting/suspension/match/{matchId}` | JWT | `checkSuspensionByMatch` | `-` | - | 200 | `ApiEnvelopeBettingSuspensionMatchResponse` | suspended:boolean*, remainingSeconds:integer* |
| GET | `/api/betting/suspension/question/{questionId}` | JWT | `checkSuspensionByQuestion` | `-` | - | 200 | `ApiEnvelopeBettingSuspensionStatus` | suspended:boolean*, resumeAt:string, remainingSeconds:integer* |
| GET | `/api/health` | Public | `health` | `-` | - | 200 | `ApiEnvelopeHealthStatusResponse` | status:string*, service:string*, version:string* |
| GET | `/api/sports/matches` | Public | `getMatches` | `-` | - | 200 | `array` | array<id:integer, leagueCode:string, homeTeam:string*, awayTeam:string*, homeScore:integer, awayScore:integer, matchStatus:string*, matchTime:string*> |

## 프론트 연동 API (src/services/api.ts 추출)

- 총 경로 수: **41**
- 호출 래퍼: `src/services/api.ts`
- 훅 계층: `src/hooks/useApi.ts`
- WebSocket: `src/services/websocket.ts` (`/ws`, `/topic/markets`, `/topic/votes`, `/topic/pool/{questionId}`)

- `/api/activities/me/question/{var}`
- `/api/activities/question/{var}`
- `/api/auth/complete-signup`
- `/api/auth/google`
- `/api/auth/google/complete-registration`
- `/api/auth/login`
- `/api/auth/send-code`
- `/api/auth/verify-code`
- `/api/leaderboard/member/{var}`
- `/api/leaderboard/top?limit={var}`
- `/api/members/me`
- `/api/members/me/dashboard`
- `/api/payments/verify-deposit`
- `/api/payments/withdraw`
- `/api/pool/{var}`
- `/api/portfolio/accuracy-trend`
- `/api/portfolio/category-breakdown`
- `/api/portfolio/positions`
- `/api/portfolio/summary`
- `/api/questions/credits/status`
- `/api/questions/drafts/open`
- `/api/questions/drafts/{var}/cancel`
- `/api/questions/drafts/{var}/submit`
- `/api/questions/me/created`
- `/api/questions/status/{var}`
- `/api/questions/{var}`
- `/api/questions/{var}/comments`
- `/api/questions/{var}/comments/{var}`
- `/api/settlements/history/me`
- `/api/swap`
- `/api/swap/history/{var}`
- `/api/swap/my-history/{var}`
- `/api/swap/my-shares/{var}`
- `/api/swap/simulate?{var}`
- `/api/users/me/profile`
- `/api/users/{var}`
- `/api/users/{var}/follow`
- `/api/users/{var}/followers`
- `/api/users/{var}/following`
- `/api/votes`
- `/api/votes/status/{var}`

## 외부 API/인프라 연동

- OpenAI Chat Completions (`openai.api.base-url`)
- Anthropic Messages API
- Google Gemini API
- football-data.org
- Polygon RPC + ERC20 USDC (web3j)
- Google OAuth2 / Gmail SMTP
