package com.predata.backend.util

import com.predata.backend.config.JwtAuthInterceptor
import com.predata.backend.exception.UnauthorizedException
import jakarta.servlet.http.HttpServletRequest

fun HttpServletRequest.authenticatedMemberId(): Long =
    getAttribute(JwtAuthInterceptor.ATTR_MEMBER_ID) as? Long
        ?: throw UnauthorizedException("Authentication required.")

fun HttpServletRequest.authenticatedEmail(): String =
    getAttribute(JwtAuthInterceptor.ATTR_EMAIL) as? String
        ?: throw UnauthorizedException("Authentication required.")

fun HttpServletRequest.authenticatedRole(): String =
    getAttribute(JwtAuthInterceptor.ATTR_ROLE) as? String
        ?: throw UnauthorizedException("Authentication required.")
