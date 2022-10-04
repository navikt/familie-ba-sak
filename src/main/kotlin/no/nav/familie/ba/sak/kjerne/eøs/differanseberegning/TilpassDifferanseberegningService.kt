package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseEndretAbonnent
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.AndelTilkjentYtelsePraktiskLikhet.erIPraksisLik
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.AndelTilkjentYtelsePraktiskLikhet.inneholderIPraksis
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilpassDifferanseberegningEtterTilkjentYtelseService(
    private val valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs>,
    private val utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) : TilkjentYtelseEndretAbonnent {

    @Transactional
    override fun endretTilkjentYtelse(tilkjentYtelse: TilkjentYtelse) {
        val behandlingId = BehandlingId(tilkjentYtelse.behandling.id)
        val valutakurser = valutakursRepository.finnFraBehandlingId(behandlingId.id)
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id)

        val oppdaterteAndeler = beregnDifferanse(
            tilkjentYtelse.andelerTilkjentYtelse,
            utenlandskePeriodebeløp,
            valutakurser
        )

        tilkjentYtelseRepository.oppdaterTilkjentYtelse(tilkjentYtelse, oppdaterteAndeler)
    }
}

@Service
class TilpassDifferanseberegningEtterUtenlandskPeriodebeløpService(
    private val valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs>,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) : PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp> {
    @Transactional
    override fun skjemaerEndret(
        behandlingId: BehandlingId,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>
    ) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingOptional(behandlingId.id) ?: return
        val valutakurser = valutakursRepository.finnFraBehandlingId(behandlingId.id)

        val oppdaterteAndeler = beregnDifferanse(
            tilkjentYtelse.andelerTilkjentYtelse,
            utenlandskePeriodebeløp,
            valutakurser
        )

        tilkjentYtelseRepository.oppdaterTilkjentYtelse(tilkjentYtelse, oppdaterteAndeler)
    }
}

@Service
class TilpassDifferanseberegningEtterValutakursService(
    private val utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) : PeriodeOgBarnSkjemaEndringAbonnent<Valutakurs> {

    @Transactional
    override fun skjemaerEndret(behandlingId: BehandlingId, valutakurser: Collection<Valutakurs>) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingOptional(behandlingId.id) ?: return
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id)

        val oppdaterteAndeler = beregnDifferanse(
            tilkjentYtelse.andelerTilkjentYtelse,
            utenlandskePeriodebeløp,
            valutakurser
        )

        tilkjentYtelseRepository.oppdaterTilkjentYtelse(tilkjentYtelse, oppdaterteAndeler)
    }
}

/**
 * En litt risikabel funksjon, som benytter "funksjonell likhet" for å sjekke etter endringer
 * på andel tilkjent ytelse
 */
fun TilkjentYtelseRepository.oppdaterTilkjentYtelse(
    tilkjentYtelse: TilkjentYtelse,
    oppdaterteAndeler: Collection<AndelTilkjentYtelse>
) {
    if (tilkjentYtelse.andelerTilkjentYtelse.erIPraksisLik(oppdaterteAndeler)) {
        return
    }

    // Her er det viktig å beholde de originale andelene, som styres av JPA og har alt av innhold
    val skalBeholdes = tilkjentYtelse.andelerTilkjentYtelse
        .filter { oppdaterteAndeler.inneholderIPraksis(it) }

    val skalLeggesTil = oppdaterteAndeler
        .filter { !tilkjentYtelse.andelerTilkjentYtelse.inneholderIPraksis(it) }

    // Forsikring: Sjekk at det ikke oppstår eller forsvinner andeler når de sjekkes for likhet
    if (oppdaterteAndeler.size != (skalBeholdes.size + skalLeggesTil.size)) {
        throw IllegalStateException("Avvik mellom antall innsendte andeler og kalkulerte endringer")
    }

    tilkjentYtelse.andelerTilkjentYtelse.clear()
    tilkjentYtelse.andelerTilkjentYtelse.addAll(skalBeholdes + skalLeggesTil)

    // Ekstra forsikring: Bygger tidslinjene på nytt for å sjekke at det ikke er introdusert duplikater
    // Krasjer med Exception hvis det forekommer perioder per aktør og ytelsetype som overlapper
    // Bør fjernes hvis det ikke forekommer feil
    tilkjentYtelse.andelerTilkjentYtelse.sjekkForDuplikater()

    this.saveAndFlush(tilkjentYtelse)
}

@Deprecated("Brukes som sikkerhetsnett for å sjekke at det ikke oppstår duplikater. Burde være unødvendig")
internal fun Iterable<AndelTilkjentYtelse>.sjekkForDuplikater() {
    try {
        // Det skal ikke være overlapp i andeler for en gitt ytelsestype og aktør
        this.groupBy { it.aktør.aktørId + it.type }
            .mapValues { (_, andeler) -> tidslinje { andeler.map { it.tilPeriode() } } }
            .values.forEach { it.perioder() }
    } catch (throwable: Throwable) {
        throw IllegalStateException(
            "Endring av andeler tilkjent ytelse i differanseberegning holder på å introdusere duplikater",
            throwable
        )
    }
}
