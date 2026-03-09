package com.predata.backend.service

import com.predata.backend.domain.policy.QuestionContentPolicy
import com.predata.backend.exception.BadRequestException
import org.springframework.stereotype.Service

@Service
class QuestionContentValidationService {

    fun validateTitle(title: String) {
        QuestionContentPolicy.validateTitle(title)?.let { throw BadRequestException(it) }
    }

    fun validateResolutionRule(rule: String?) {
        QuestionContentPolicy.validateResolutionRule(rule)?.let { throw BadRequestException(it) }
    }
}
