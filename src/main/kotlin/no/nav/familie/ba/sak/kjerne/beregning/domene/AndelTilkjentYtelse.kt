package no.nav.familie.ba.sak.kjerne.beregning.domene

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.YearMonthConverter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.YtelsetypeBA
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.tilAndelForVedtaksbegrunnelseTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.tilAndelForVedtaksperiodeTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.tidslinje.Tidslinje
import java.math.BigDecimal
import java.time.YearMonth
import java.util.Objects

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "AndelTilkjentYtelse")
@Table(name = "ANDEL_TILKJENT_YTELSE")
data class AndelTilkjentYtelse(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "andel_tilkjent_ytelse_seq_generator")
    @SequenceGenerator(
        name = "andel_tilkjent_ytelse_seq_generator",
        sequenceName = "andel_tilkjent_ytelse_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandlingId: Long,
    @ManyToOne(cascade = [CascadeType.MERGE])
    @JoinColumn(name = "tilkjent_ytelse_id", nullable = false, updatable = false)
    var tilkjentYtelse: TilkjentYtelse,
    @OneToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,
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
    /* kildeBehandlingId, periodeOffset og forrigePeriodeOffset trengs kun i forbindelse med
     iverksetting/konsistensavstemming, og settes først ved generering av selve oppdraget mot økonomi.
     Samme informasjon finnes i utbetalingsoppdraget på hver enkelt sak, men for å gjøre operasjonene mer forståelig
     og enklere å jobbe med har vi valgt å trekke det ut hit. */
    @Column(name = "kilde_behandling_id")
    var kildeBehandlingId: Long? = null,
    // Brukes for å koble seg på tidligere kjeder sendt til økonomi
    @Column(name = "periode_offset")
    var periodeOffset: Long? = null,
    @Column(name = "forrige_periode_offset")
    var forrigePeriodeOffset: Long? = null,
    @Column(name = "nasjonalt_periodebelop")
    val nasjonaltPeriodebeløp: Int?,
    @Column(name = "differanseberegnet_periodebelop")
    val differanseberegnetPeriodebeløp: Int? = null,
    @Column(name = "belop_uten_endret_utbetaling")
    val beløpUtenEndretUtbetaling: Int? = null,
) : BaseEntitet() {
    val periode
        get() = MånedPeriode(stønadFom, stønadTom)

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        } else if (this === other) {
            return true
        }

        val annen = other as AndelTilkjentYtelse
        return Objects.equals(behandlingId, annen.behandlingId) &&
            Objects.equals(type, annen.type) &&
            Objects.equals(kalkulertUtbetalingsbeløp, annen.kalkulertUtbetalingsbeløp) &&
            Objects.equals(stønadFom, annen.stønadFom) &&
            Objects.equals(stønadTom, annen.stønadTom) &&
            Objects.equals(aktør, annen.aktør) &&
            Objects.equals(nasjonaltPeriodebeløp, annen.nasjonaltPeriodebeløp) &&
            Objects.equals(differanseberegnetPeriodebeløp, annen.differanseberegnetPeriodebeløp) &&
            Objects.equals(beløpUtenEndretUtbetaling, annen.beløpUtenEndretUtbetaling)
    }

    override fun hashCode(): Int =
        Objects.hash(
            id,
            behandlingId,
            type,
            kalkulertUtbetalingsbeløp,
            stønadFom,
            stønadTom,
            aktør,
            nasjonaltPeriodebeløp,
            differanseberegnetPeriodebeløp,
        )

    override fun toString(): String =
        "AndelTilkjentYtelse(id = $id, behandling = $behandlingId, type = $type, sats = $sats, prosent = $prosent, " +
            "beløp = $kalkulertUtbetalingsbeløp, stønadFom = $stønadFom, stønadTom = $stønadTom, periodeOffset = $periodeOffset, " +
            "forrigePeriodeOffset = $forrigePeriodeOffset, kildeBehandlingId = $kildeBehandlingId, nasjonaltPeriodebeløp = $nasjonaltPeriodebeløp, " +
            "differanseberegnetBeløp = $differanseberegnetPeriodebeløp, beløpUtenEndretUtbetaling = $beløpUtenEndretUtbetaling)"

    fun stønadsPeriode() = MånedPeriode(this.stønadFom, this.stønadTom)

    fun erUtvidet() = this.type == YtelseType.UTVIDET_BARNETRYGD

    fun erSmåbarnstillegg() = this.type == YtelseType.SMÅBARNSTILLEGG

    fun erFinnmarkstillegg() = this.type == YtelseType.FINNMARKSTILLEGG

    fun erSvalbardtillegg() = this.type == YtelseType.SVALBARDTILLEGG

    fun erSøkersAndel() = erUtvidet() || erSmåbarnstillegg()

    fun erLøpende(): Boolean = this.stønadTom > YearMonth.now()

    fun erDeltBosted() = this.prosent == BigDecimal(50)

    fun vurdertEtter(personResultater: Set<PersonResultat>): Regelverk {
        val relevanteVilkårsResultater = finnRelevanteVilkårsresulaterForRegelverk(personResultater)

        return when {
            relevanteVilkårsResultater.isEmpty() -> Regelverk.NASJONALE_REGLER
            relevanteVilkårsResultater.all { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN } -> Regelverk.EØS_FORORDNINGEN
            relevanteVilkårsResultater.all { it.vurderesEtter == Regelverk.NASJONALE_REGLER } -> Regelverk.NASJONALE_REGLER
            else -> Regelverk.NASJONALE_REGLER
        }
    }

    fun erAndelSomSkalSendesTilOppdrag(): Boolean = this.kalkulertUtbetalingsbeløp != 0

    fun erAndelSomharNullutbetalingPgaDifferanseberegning() =
        this.kalkulertUtbetalingsbeløp == 0 &&
            this.differanseberegnetPeriodebeløp != null &&
            this.differanseberegnetPeriodebeløp <= 0

    private fun finnRelevanteVilkårsresulaterForRegelverk(
        personResultater: Set<PersonResultat>,
    ): List<VilkårResultat> =
        personResultater
            .filter { !it.erSøkersResultater() }
            .filter { this.aktør == it.aktør }
            .flatMap { it.vilkårResultater }
            .filter {
                this.stønadFom > (it.periodeFom ?: TIDENES_MORGEN).toYearMonth() &&
                    (it.periodeTom == null || this.stønadFom <= it.periodeTom?.toYearMonth())
            }.filter { vilkårResultat ->
                regelverkAvhengigeVilkår().any { it == vilkårResultat.vilkårType }
            }
}

enum class YtelseType(
    val klassifisering: String,
) {
    ORDINÆR_BARNETRYGD("BATR"),
    UTVIDET_BARNETRYGD("BAUTV-OP"),
    SMÅBARNSTILLEGG("BATRSMA"),
    FINNMARKSTILLEGG("BATRFIN"),
    SVALBARDTILLEGG("BATRSVAL"),
    ;

    fun tilYtelseType(): YtelsetypeBA =
        when (this) {
            ORDINÆR_BARNETRYGD -> YtelsetypeBA.ORDINÆR_BARNETRYGD
            UTVIDET_BARNETRYGD -> YtelsetypeBA.UTVIDET_BARNETRYGD
            SMÅBARNSTILLEGG -> YtelsetypeBA.SMÅBARNSTILLEGG
            FINNMARKSTILLEGG -> YtelsetypeBA.FINNMARKSTILLEGG
            SVALBARDTILLEGG -> YtelsetypeBA.SVALBARDTILLEGG
        }

    fun tilSatsType(
        person: Person,
        fom: YearMonth,
        tom: YearMonth,
    ): Set<SatsType> =
        when (this) {
            ORDINÆR_BARNETRYGD -> {
                val sisteMÅnedForTilleggsOrba = minOf(SatsService.finnSisteSatsFor(SatsType.TILLEGG_ORBA).gyldigTom, person.hentSeksårsdag()).toYearMonth()
                when {
                    tom <= sisteMÅnedForTilleggsOrba -> setOf(SatsType.TILLEGG_ORBA)
                    fom > sisteMÅnedForTilleggsOrba -> setOf(SatsType.ORBA)
                    else -> setOf(SatsType.TILLEGG_ORBA, SatsType.ORBA)
                }
            }

            UTVIDET_BARNETRYGD -> {
                setOf(SatsType.UTVIDET_BARNETRYGD)
            }

            SMÅBARNSTILLEGG -> {
                setOf(SatsType.SMA)
            }

            FINNMARKSTILLEGG -> {
                setOf(SatsType.FINNMARKSTILLEGG)
            }

            SVALBARDTILLEGG -> {
                setOf(SatsType.SVALBARDTILLEGG)
            }
        }
}

private fun regelverkAvhengigeVilkår() =
    listOf(
        Vilkår.BOR_MED_SØKER,
        Vilkår.BOSATT_I_RIKET,
        Vilkår.LOVLIG_OPPHOLD,
    )

fun Collection<AndelTilkjentYtelse>.tilTidslinjerPerAktørOgType(): Map<Pair<Aktør, YtelseType>, Tidslinje<AndelTilkjentYtelse>> =
    groupBy { Pair(it.aktør, it.type) }.mapValues { (_, andelerTilkjentYtelsePåPerson) ->
        andelerTilkjentYtelsePåPerson.tilTidslinje()
    }

fun Collection<AndelTilkjentYtelse>.tilAndelForVedtaksperiodeTidslinjerPerAktørOgType(): Map<Pair<Aktør, YtelseType>, Tidslinje<AndelForVedtaksperiode>> =
    groupBy { Pair(it.aktør, it.type) }.mapValues { (_, andelerTilkjentYtelsePåPerson) ->
        andelerTilkjentYtelsePåPerson.tilAndelForVedtaksperiodeTidslinje()
    }

fun Collection<AndelTilkjentYtelse>.tilAndelForVedtaksbegrunnelseTidslinjerPerAktørOgType(): Map<Pair<Aktør, YtelseType>, Tidslinje<AndelForVedtaksbegrunnelse>> =
    groupBy { Pair(it.aktør, it.type) }.mapValues { (_, andelerTilkjentYtelsePåPerson) ->
        andelerTilkjentYtelsePåPerson.tilAndelForVedtaksbegrunnelseTidslinje()
    }
