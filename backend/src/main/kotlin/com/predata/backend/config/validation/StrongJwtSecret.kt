package com.predata.backend.config.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * JWT 서명 키의 최소 강도를 검증한다 (UTF-8 기준 최소 32바이트 = 256비트).
 * 빈 값은 통과시키며, @NotBlank 와 함께 사용해야 한다.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [StrongJwtSecretValidator::class])
@MustBeDocumented
annotation class StrongJwtSecret(
    val message: String = "JWT secret must be at least 32 bytes (256 bits). Generate with: openssl rand -hex 64",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class StrongJwtSecretValidator : ConstraintValidator<StrongJwtSecret, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value.isNullOrBlank()) return true
        return value.toByteArray(Charsets.UTF_8).size >= 32
    }
}
