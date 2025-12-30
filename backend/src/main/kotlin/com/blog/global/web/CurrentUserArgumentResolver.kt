package com.blog.global.web

import com.blog.global.security.JwtPrincipal
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val isAnnotated = parameter.hasParameterAnnotation(CurrentUser::class.java)
        val isLong = parameter.parameterType == Long::class.java || parameter.parameterType == Long::class.javaObjectType
        return isAnnotated && isLong
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        val principal = auth.principal as? JwtPrincipal ?: return null
        return principal.userId
    }
}