package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseDomene
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.tidspunktKombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class DifferanseberegningService(
    private val valutakursService: ValutakursService,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
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

        val utenlandskePeriodebeløpTidslinjer = utenlandskePeriodebeløp.tilSeparateTidslinjerForBarna()
        val valutakursTidslinjer = valutakurser.tilSeparateTidslinjerForBarna()
        val andelTilkjentYtelseTidslinjer = tilkjentYtelse.tilSeparateTidslinjerForBarna()

        val barnMedAlleTidslinjer: Set<Aktør> =
            utenlandskePeriodebeløpTidslinjer.keys
                .intersect(valutakursTidslinjer.keys)
                .intersect(andelTilkjentYtelseTidslinjer.keys)

        val barnasAndelerTilkjentYtelse = barnMedAlleTidslinjer.flatMap { aktør ->
            val utenlandskePeriodebeløpTidslinje = utenlandskePeriodebeløpTidslinjer.getValue(aktør)
            val valutakursTidslinje = valutakursTidslinjer.getValue(aktør)
            val andelTilkjentYtelseTidslinje = andelTilkjentYtelseTidslinjer.getValue(aktør)

            val utenlandskePeriodebeløpINorskeKroner =
                utenlandskePeriodebeløpTidslinje.tidspunktKombinerMed(valutakursTidslinje) { tidspunkt, upb, vk ->
                    upb.multipliserMed(vk, tidspunkt)
                }

            val differanseberegnetAndelTilkjentYtelseTidslinjer =
                andelTilkjentYtelseTidslinje.kombinerUtenNullMed(utenlandskePeriodebeløpINorskeKroner) { aty, beløp ->
                    aty.medUtenlandskPeriodebeløp(beløp)
                }

            differanseberegnetAndelTilkjentYtelseTidslinjer
                .filtrerIkkeNull()
                .beskjærEtter(andelTilkjentYtelseTidslinje) // Skal ikke være lenger enn denne
                .filtrer { it!!.kalkulertUtbetalingsbeløp > 0 } // Vi sender bare positive beløp til oppdragssystemet
                .tilAndelTilkjentYtelse()
        }

        return tilkjentYtelse.kopierOgErstattBarnasAndeler(barnasAndelerTilkjentYtelse)
    }
}

fun TilkjentYtelse.tilSeparateTidslinjerForBarna(): Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>> {

    return this.andelerTilkjentYtelse
        .filter { !it.erSøkersAndel() }
        .groupBy { it.aktør }
        .mapValues {
            tidslinje {
                it.value.map {
                    Periode(
                        it.stønadFom.tilTidspunkt(),
                        it.stønadTom.tilTidspunkt(),
                        // Ta bort periode, slik at det ikke blir med på innholdet som vurderes for likhet
                        it.utenPeriode()
                    )
                }
            }
        }
}

fun <T : AndelTilkjentYtelseDomene<T>> Tidslinje<T, Måned>.tilAndelTilkjentYtelse(): List<T> {
    return this
        .perioder().map {
            it.innhold!!.medPeriode(it.fraOgMed.tilYearMonth(), it.tilOgMed.tilYearMonth())
        }
}

fun TilkjentYtelse.kopierOgErstattBarnasAndeler(barnasAndeler: Iterable<AndelTilkjentYtelse>): TilkjentYtelse {
    val nyTilkjentYtelse = TilkjentYtelse(
        behandling = this.behandling,
        opprettetDato = LocalDate.now(),
        endretDato = LocalDate.now()
    )

    // Fjern koblingen fra endret utebetalingsandel til andel tilkjente ytelser fordi de kan være gamle
    barnasAndeler
        .flatMap { andelerTilkjentYtelse -> andelerTilkjentYtelse.endretUtbetalingAndeler }
        .map { endretUtbetalingAndel -> endretUtbetalingAndel.andelTilkjentYtelser.clear() }

    // Lag tilbakekobling fra endret utebetalingsandel til (ny) andel tilkjent ytelse
    barnasAndeler.map { andelTilkjentYtelse ->
        andelTilkjentYtelse.endretUtbetalingAndeler.forEach { it.andelTilkjentYtelser.add(andelTilkjentYtelse) }
    }

    // Søkers andeler skal være uendret
    val søkersAndeler = this.andelerTilkjentYtelse.filter { it.erSøkersAndel() }
    val alleAndeler: Iterable<AndelTilkjentYtelse> = søkersAndeler + barnasAndeler

    nyTilkjentYtelse.andelerTilkjentYtelse.addAll(
        alleAndeler.map { it.copy(tilkjentYtelse = nyTilkjentYtelse) }
    )

    return nyTilkjentYtelse
}
