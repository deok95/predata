package com.predata.backend.domain.policy

import com.predata.backend.domain.VotingPhase

/**
 * Domain policy for question voting-phase transitions.
 * Keeps FSM rules outside application services.
 */
object QuestionPhaseTransitionPolicy {
    fun isAllowed(current: VotingPhase, next: VotingPhase): Boolean {
        return when (current) {
            VotingPhase.VOTING_COMMIT_OPEN -> next == VotingPhase.VOTING_REVEAL_OPEN
            VotingPhase.VOTING_REVEAL_OPEN -> next == VotingPhase.VOTING_REVEAL_CLOSED || next == VotingPhase.BETTING_OPEN
            VotingPhase.VOTING_REVEAL_CLOSED -> next == VotingPhase.BETTING_OPEN
            VotingPhase.BETTING_OPEN -> next == VotingPhase.SETTLEMENT_PENDING
            VotingPhase.SETTLEMENT_PENDING -> next == VotingPhase.SETTLED
            VotingPhase.SETTLED -> next == VotingPhase.REWARD_DISTRIBUTED
            VotingPhase.REWARD_DISTRIBUTED -> false
        }
    }
}
