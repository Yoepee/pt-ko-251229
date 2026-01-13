package com.blog.domain.battle.entity

import com.blog.global.jpa.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "battle_match_results")
class BattleMatchResult(

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    var match: BattleMatch,

    @Enumerated(EnumType.STRING)
    @Column(name = "winner_team", nullable = false, length = 4)
    var winnerTeam: BattleWinnerTeam,

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", nullable = false, length = 10)
    var endReason: BattleEndReason,

    @Column(name = "lane0_final", nullable = false)
    var lane0Final: Int = 0,

    @Column(name = "lane1_final", nullable = false)
    var lane1Final: Int = 0,

    @Column(name = "lane2_final", nullable = false)
    var lane2Final: Int = 0,

    @Column(name = "inputs_team_a", nullable = false)
    var inputsTeamA: Int = 0,

    @Column(name = "inputs_team_b", nullable = false)
    var inputsTeamB: Int = 0,

    @Column(name = "extra_stats", nullable = false, columnDefinition = "jsonb")
    var extraStats: String = "{}",
) : BaseTimeEntity() {

    @Id
    @Column(name = "match_id")
    var matchId: Long? = null
        protected set
}