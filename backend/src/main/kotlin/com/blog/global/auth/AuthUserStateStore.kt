package com.blog.global.auth

interface AuthUserStateStore {
    fun getVersion(userId: Long): Long
    fun getStatus(userId: Long): AuthUserStatus
    fun bumpVersion(userId: Long): Long
    fun setStatus(userId: Long, status: AuthUserStatus)
    fun markDeletedAndBump(userId: Long)
}