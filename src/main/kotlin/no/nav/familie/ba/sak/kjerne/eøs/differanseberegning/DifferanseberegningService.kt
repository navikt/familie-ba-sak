package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.snittKombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.tidspunktKombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate

@Service
class DifferanseberegningService(
    private val valutakursService: ValutakursService,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val persongrunnlagService: PersongrunnlagService
) {
    @Transactional
    fun beregnDifferanseFraTilkjentYtelse(behandlingId: BehandlingId, tilkjentYtelse: TilkjentYtelse) {
        val valutakurser = valutakursService.hentValutakurser(behandlingId)
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId)

        val nyTilkjentYtelse = beregnDifferanse(tilkjentYtelse, utenlandskePeriodebeløp, valutakurser)
        tilkjentYtelseRepository.save(nyTilkjentYtelse)
    }

    @Transactional
    fun beregnDifferanseFraUtenlandskePeridebeløp(
        behandlingId: BehandlingId,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>
    ) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId.id)
        val valutakurser = valutakursService.hentValutakurser(behandlingId)

        val nyTilkjentYtelse = beregnDifferanse(tilkjentYtelse, utenlandskePeriodebeløp, valutakurser)
        tilkjentYtelseRepository.save(nyTilkjentYtelse)
    }

    @Transactional
    fun beregnDifferanseFraValutakurser(behandlingId: BehandlingId, valutakurser: Collection<Valutakurs>) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId.id)
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId)

        val nyTilkjentYtelse = beregnDifferanse(tilkjentYtelse, utenlandskePeriodebeløp, valutakurser)
        tilkjentYtelseRepository.save(nyTilkjentYtelse)
    }

    private fun beregnDifferanse(
        tilkjentYtelse: TilkjentYtelse,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
        valutakurser: Collection<Valutakurs>
    ): TilkjentYtelse {

        val nyTilkjentYtelse = TilkjentYtelse(
            behandling = tilkjentYtelse.behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now()
        )

        val utenlandskePeriodebeløpTidslinjer = utenlandskePeriodebeløp.tilSeparateTidslinjerForBarna()
        val valutakursTidslinjer = valutakurser.tilSeparateTidslinjerForBarna()
        val andelTilkjentYtelseTidslinjer = tilkjentYtelse.tilSeparateTidslinjerForBarna()

        val alleBarnAktører: Set<Aktør> =
            utenlandskePeriodebeløpTidslinjer.keys + valutakursTidslinjer.keys + andelTilkjentYtelseTidslinjer.keys

        val barnasAndelerTilkjentYtelse = alleBarnAktører.flatMap { aktør ->
            val utenlandskePeriodebeløpTidslinje = utenlandskePeriodebeløpTidslinjer.getOrDefault(aktør, TomTidslinje())
            val valutakursTidslinje = valutakursTidslinjer.getOrDefault(aktør, TomTidslinje())
            val andelTilkjentYtelseTidslinje = andelTilkjentYtelseTidslinjer.getOrDefault(aktør, TomTidslinje())

            val utenlandskePeriodebeløpINorskeKroner =
                utenlandskePeriodebeløpTidslinje.tidspunktKombinerMed(valutakursTidslinje) { tidspunkt, upb, vk ->
                    when {
                        upb == null || vk == null -> null
                        upb.valutakode != vk.valutakode -> null
                        upb.beløp == null || vk.kurs == null -> null
                        else -> upb.tilMånedligBeløp(tidspunkt)?.multiply(vk.kurs)
                    }
                }

            val differanseberegnetAndelTilkjentYtelseTidslinjer =
                andelTilkjentYtelseTidslinje.snittKombinerMed(utenlandskePeriodebeløpINorskeKroner) { aty, beløp ->
                    when {
                        aty == null || beløp == null -> null
                        else -> aty.medUtenlandskPeriodebeløp(beløp)
                    }
                }

            differanseberegnetAndelTilkjentYtelseTidslinjer
                .filtrerIkkeNull()
                .beskjærEtter(andelTilkjentYtelseTidslinje) // Skal ikke være lenger enn denne
                .filtrer { it!!.kalkulertUtbetalingsbeløp > 0 }
                .tilAndelTilkjentYtelse(nyTilkjentYtelse)
        }

        val søkersAndelerTilkjentYtelse = tilkjentYtelse.kopierSøkersAndelerTil(nyTilkjentYtelse)

        nyTilkjentYtelse.andelerTilkjentYtelse.addAll(barnasAndelerTilkjentYtelse + søkersAndelerTilkjentYtelse)
        return nyTilkjentYtelse
    }
}

fun TilkjentYtelse.tilSeparateTidslinjerForBarna(): Map<Aktør, Tidslinje<AndelTilkjentYtelseForTidslinje, Måned>> {

    return this.andelerTilkjentYtelse
        .filter { !it.erSøkersAndel() }
        .groupBy { it.aktør }
        .mapValues {
            tidslinje {
                it.value.map {
                    Periode(
                        it.stønadFom.tilTidspunkt(),
                        it.stønadTom.tilTidspunkt(),
                        it.forTidslinje()
                    )
                }
            }
        }
}

fun TilkjentYtelse.kopierSøkersAndelerTil(tilkjentYtelse: TilkjentYtelse): List<AndelTilkjentYtelse> {
    return andelerTilkjentYtelse
        .filter { it.erSøkersAndel() }
        .map {
            it.copy(
                id = 0,
                tilkjentYtelse = tilkjentYtelse
            )
        }
}

fun TilkjentYtelse.andelerForSøker() = andelerTilkjentYtelse.filter { it.erSøkersAndel() }

fun AndelTilkjentYtelse.forTidslinje(): AndelTilkjentYtelseForTidslinje =
    AndelTilkjentYtelseForTidslinje(
        behandlingId = this.behandlingId,
        aktør = this.aktør,
        kalkulertUtbetalingsbeløp = this.kalkulertUtbetalingsbeløp,
        type = this.type,
        sats = this.sats,
        prosent = this.prosent,
        endretUtbetalingAndeler = this.endretUtbetalingAndeler
    ).also { it.id = this.id }

data class AndelTilkjentYtelseForTidslinje(
    val aktør: Aktør,
    val kalkulertUtbetalingsbeløp: Int,
    val type: YtelseType,
    val sats: Int,
    val prosent: BigDecimal,
    val endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    val behandlingId: Long,
    val utenlandskPeriodebeløp: Int? = null,
    val nasjonaltPeriodebeløp: Int? = null,
    val differanseberegnetBeløp: Int? = null
) {
    internal var id: Long? = null
}

fun AndelTilkjentYtelseForTidslinje.medUtenlandskPeriodebeløp(beløp: BigDecimal): AndelTilkjentYtelseForTidslinje {
    val nyttNasjonaltPeriodebeløp = when {
        differanseberegnetBeløp == null -> kalkulertUtbetalingsbeløp
        differanseberegnetBeløp != kalkulertUtbetalingsbeløp -> kalkulertUtbetalingsbeløp
        else -> this.nasjonaltPeriodebeløp!!
    }

    val avrundetUtenlandskPeriodebeløp = beløp.round(MathContext(0, RoundingMode.DOWN)).intValueExact()
    val nyttDifferanseberegnetBeløp = nyttNasjonaltPeriodebeløp - avrundetUtenlandskPeriodebeløp

    return this.copy(
        kalkulertUtbetalingsbeløp = nyttDifferanseberegnetBeløp,
        nasjonaltPeriodebeløp = nyttNasjonaltPeriodebeløp,
        utenlandskPeriodebeløp = avrundetUtenlandskPeriodebeløp,
        differanseberegnetBeløp = nyttDifferanseberegnetBeløp
    )
}

fun Tidslinje<AndelTilkjentYtelseForTidslinje, Måned>.tilAndelTilkjentYtelse(tilkjentYtelse: TilkjentYtelse): List<AndelTilkjentYtelse> {
    return this
        .perioder().map {
            val aty = it.innhold!! // Forutsetter at tidslinjen kun har innhold
            AndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                stønadFom = it.fraOgMed.tilYearMonth(), // Forutsetter at tidslinjen kun har endelige perioder
                stønadTom = it.tilOgMed.tilYearMonth(), // Forutsetter at tidslinjen kun har endelige perioderr
                behandlingId = aty.behandlingId,
                aktør = aty.aktør,
                kalkulertUtbetalingsbeløp = aty.kalkulertUtbetalingsbeløp,
                type = aty.type,
                sats = aty.sats,
                prosent = aty.prosent,
                endretUtbetalingAndeler = aty.endretUtbetalingAndeler.toMutableList(),
            )
        }
}
