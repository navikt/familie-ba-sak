package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseEndretAbonnent
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.oppdaterTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.AutomatiskOppdaterValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface BarnasDifferanseberegningEndretAbonnent {
    fun barnasDifferanseberegningEndret(tilkjentYtelse: TilkjentYtelse)
}

@Service
class TilpassDifferanseberegningEtterTilkjentYtelseService(
    private val valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs>,
    private val utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val barnasDifferanseberegningEndretAbonnenter: List<BarnasDifferanseberegningEndretAbonnent>,
    private val unleashService: UnleashNextMedContextService,
) : TilkjentYtelseEndretAbonnent {
    @Transactional
    override fun endretTilkjentYtelse(tilkjentYtelse: TilkjentYtelse) {
        val behandlingId = BehandlingId(tilkjentYtelse.behandling.id)
        val valutakurser = valutakursRepository.finnFraBehandlingId(behandlingId.id)
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id)

        val skalBrukeNyDifferanseberegning = unleashService.isEnabled(FeatureToggle.SKAL_BRUKE_NY_DIFFERANSEBEREGNING)

        val oppdaterteAndeler =
            beregnDifferanse(
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                skalBrukeNyDifferanseberegning = skalBrukeNyDifferanseberegning,
            )

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.oppdaterTilkjentYtelse(tilkjentYtelse, oppdaterteAndeler)
        barnasDifferanseberegningEndretAbonnenter.forEach { it.barnasDifferanseberegningEndret(oppdatertTilkjentYtelse) }
    }
}

@Service
class TilpassDifferanseberegningEtterUtenlandskPeriodebeløpService(
    private val valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs>,
    private val automatiskOppdaterValutakursService: AutomatiskOppdaterValutakursService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val barnasDifferanseberegningEndretAbonnenter: List<BarnasDifferanseberegningEndretAbonnent>,
    private val unleashService: UnleashNextMedContextService,
) : PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp> {
    @Transactional
    override fun skjemaerEndret(
        behandlingId: BehandlingId,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    ) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingOptional(behandlingId.id) ?: return

        automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(behandlingId, utenlandskePeriodebeløp)

        val valutakurser = valutakursRepository.finnFraBehandlingId(behandlingId.id)

        val skalBrukeNyDifferanseberegning = unleashService.isEnabled(FeatureToggle.SKAL_BRUKE_NY_DIFFERANSEBEREGNING)

        val oppdaterteAndeler =
            beregnDifferanse(
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                skalBrukeNyDifferanseberegning = skalBrukeNyDifferanseberegning,
            )

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.oppdaterTilkjentYtelse(tilkjentYtelse, oppdaterteAndeler)
        barnasDifferanseberegningEndretAbonnenter.forEach { it.barnasDifferanseberegningEndret(oppdatertTilkjentYtelse) }
    }
}

@Service
class TilpassDifferanseberegningEtterValutakursService(
    private val utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val barnasDifferanseberegningEndretAbonnenter: List<BarnasDifferanseberegningEndretAbonnent>,
    private val unleashService: UnleashNextMedContextService,
) : PeriodeOgBarnSkjemaEndringAbonnent<Valutakurs> {
    @Transactional
    override fun skjemaerEndret(
        behandlingId: BehandlingId,
        valutakurser: Collection<Valutakurs>,
    ) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingOptional(behandlingId.id) ?: return
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id)

        val skalBrukeNyDifferanseberegning = unleashService.isEnabled(FeatureToggle.SKAL_BRUKE_NY_DIFFERANSEBEREGNING)

        val oppdaterteAndeler =
            beregnDifferanse(
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                skalBrukeNyDifferanseberegning = skalBrukeNyDifferanseberegning,
            )

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.oppdaterTilkjentYtelse(tilkjentYtelse, oppdaterteAndeler)
        barnasDifferanseberegningEndretAbonnenter.forEach { it.barnasDifferanseberegningEndret(oppdatertTilkjentYtelse) }
    }
}

@Service
class TilpassDifferanseberegningSøkersYtelserService(
    private val persongrunnlagService: PersongrunnlagService,
    private val kompetanseRepository: KompetanseRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val unleashService: UnleashNextMedContextService,
) : BarnasDifferanseberegningEndretAbonnent {
    override fun barnasDifferanseberegningEndret(tilkjentYtelse: TilkjentYtelse) {
        val skalBrukeNyDifferanseberegning = unleashService.isEnabled(FeatureToggle.SKAL_BRUKE_NY_DIFFERANSEBEREGNING)

        val oppdaterteAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(
                barna = persongrunnlagService.hentBarna(tilkjentYtelse.behandling.id),
                kompetanser = kompetanseRepository.finnFraBehandlingId(tilkjentYtelse.behandling.id),
                personResultater = vilkårsvurderingRepository.findByBehandlingAndAktiv(tilkjentYtelse.behandling.id)!!.personResultater,
                skalBrukeNyDifferanseberegning = skalBrukeNyDifferanseberegning,
            )
        tilkjentYtelseRepository.oppdaterTilkjentYtelse(tilkjentYtelse, oppdaterteAndeler)
    }
}
