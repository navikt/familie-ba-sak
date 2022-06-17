package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseEndretAbonnent
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
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
            tilkjentYtelse.andelerTilkjentYtelse, utenlandskePeriodebeløp, valutakurser
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
            tilkjentYtelse.andelerTilkjentYtelse, utenlandskePeriodebeløp, valutakurser
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
            tilkjentYtelse.andelerTilkjentYtelse, utenlandskePeriodebeløp, valutakurser
        )

        tilkjentYtelseRepository.oppdaterTilkjentYtelse(tilkjentYtelse, oppdaterteAndeler)
    }
}

/**
 * En litt risikabel funksjon, som lener seg på at AndelTilkjentYtelse.equals() fortsetter å sjekke _funksjonell_ likhet,
 * dvs utelukker [id], [tilkjentYtelse] og [endretUtbetalingAndeler], blant annet
 * Merk også at det er en inkonsistens mellom hashCode() og equals(), som potensielt gjør likhetssjekk ustabil
 */
fun TilkjentYtelseRepository.oppdaterTilkjentYtelse(
    tilkjentYtelse: TilkjentYtelse,
    oppdaterteAndeler: List<AndelTilkjentYtelse>
) {
    val gamleAndeler = tilkjentYtelse.andelerTilkjentYtelse
    if (gamleAndeler.size == oppdaterteAndeler.size && gamleAndeler.containsAll(oppdaterteAndeler))
        return

    val skalFjernes = gamleAndeler - oppdaterteAndeler
    val skalLeggesTil = oppdaterteAndeler - gamleAndeler

    gamleAndeler.removeAll(skalFjernes)
    gamleAndeler.addAll(skalLeggesTil)

    this.saveAndFlush(tilkjentYtelse)
}
