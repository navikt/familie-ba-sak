package no.nav.familie.ba.sak.kjerne.beregning.domene

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.math.BigDecimal
import java.time.YearMonth

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "PatchetAndelTilkjentYtelse")
@Table(name = "PATCHET_ANDEL_TILKJENT_YTELSE")
data class PatchetAndelTilkjentYtelse(
    @Id
    @Column(name = "id")
    val id: Long,
    @Column(name = "behandling_id", nullable = false, updatable = false)
    val behandlingId: Long,
    @Column(name = "tilkjent_ytelse_id", nullable = false, updatable = false)
    var tilkjentYtelseId: Long,
    @Column(name = "aktoer_id", nullable = false, updatable = false)
    val aktørId: String,
    @Column(name = "kalkulert_utbetalingsbelop", nullable = false)
    val kalkulertUtbetalingsbeløp: Int,
    @Column(name = "stonad_fom", nullable = false, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val stønadFom: YearMonth,
    @Column(name = "stonad_tom", nullable = false, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val stønadTom: YearMonth,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: YtelseType,
    @Column(name = "sats", nullable = false)
    val sats: Int,
    @Column(name = "prosent", nullable = false)
    val prosent: BigDecimal,
    @Column(name = "kilde_behandling_id")
    var kildeBehandlingId: Long? = null,
    @Column(name = "periode_offset")
    var periodeOffset: Long? = null,
    @Column(name = "forrige_periode_offset")
    var forrigePeriodeOffset: Long? = null,
    @Column(name = "nasjonalt_periodebelop")
    val nasjonaltPeriodebeløp: Int?,
    @Column(name = "differanseberegnet_periodebelop")
    val differanseberegnetPeriodebeløp: Int? = null,
) : BaseEntitet()

fun AndelTilkjentYtelse.tilPatchetAndelTilkjentYtelse(): PatchetAndelTilkjentYtelse =
    PatchetAndelTilkjentYtelse(
        id = this.id,
        behandlingId = this.behandlingId,
        tilkjentYtelseId = this.tilkjentYtelse.id,
        aktørId = this.aktør.aktørId,
        kalkulertUtbetalingsbeløp = this.kalkulertUtbetalingsbeløp,
        stønadFom = this.stønadFom,
        stønadTom = this.stønadTom,
        type = this.type,
        sats = this.sats,
        prosent = this.prosent,
        kildeBehandlingId = this.kildeBehandlingId,
        periodeOffset = this.periodeOffset,
        forrigePeriodeOffset = this.forrigePeriodeOffset,
        nasjonaltPeriodebeløp = this.nasjonaltPeriodebeløp,
        differanseberegnetPeriodebeløp = this.differanseberegnetPeriodebeløp,
    )
