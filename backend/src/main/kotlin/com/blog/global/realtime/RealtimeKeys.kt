package com.blog.global.realtime

object RealtimeKeys {
    fun room(matchId:Long) = "room:$matchId"
    fun battle(matchId:Long) = "battle:$matchId"
    fun lobby() = "battle:lobby"
}