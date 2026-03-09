package com.predata.backend.config.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * 이더리움/Polygon 프라이빗 키 형식을 검증한다 (0x + 64 hex chars = 256비트).
 * 빈 값은 통과시키며, @NotBlank 와 함께 사용해야 한다.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [HexPrivateKeyValidator::class])
@MustBeDocumented
annotation class HexPrivateKey(
    val message: String = "Private key must be in hex format: 0x followed by exactly 64 hex characters",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class HexPrivateKeyValidator : ConstraintValidator<HexPrivateKey, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value.isNullOrBlank()) return true
        return HEX_PRIVATE_KEY_PATTERN.matches(value)
    }

    companion object {
        private val HEX_PRIVATE_KEY_PATTERN = Regex("^0x[0-9a-fA-F]{64}$")
    }
}
