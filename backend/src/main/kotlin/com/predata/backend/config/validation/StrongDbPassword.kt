package com.predata.backend.config.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * DB 패스워드의 최소 길이를 검증한다 (최소 12자).
 * 빈 값은 통과시키며, @NotBlank 와 함께 사용해야 한다.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [StrongDbPasswordValidator::class])
@MustBeDocumented
annotation class StrongDbPassword(
    val message: String = "DB password must be at least 12 characters",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class StrongDbPasswordValidator : ConstraintValidator<StrongDbPassword, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value.isNullOrBlank()) return true
        return value.length >= 12
    }
}
