package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.hentUtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerEndretUtbetalingAndelErGyldigForÅrsak
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerIngenOverlappendeEndring
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerPeriodeInnenforTilkjentytelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.fraRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.hentGyldigEndretBegrunnelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class EndretUtbetalingAndelService(
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val sanityService: SanityService,
) {
    fun hentEndredeUtbetalingAndeler(behandlingId: Long) =
        endretUtbetalingAndelRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelId: Long,
        restEndretUtbetalingAndel: RestEndretUtbetalingAndel
    ) {
        val endretUtbetalingAndel = endretUtbetalingAndelRepository.getById(endretUtbetalingAndelId)
        val person =
            persongrunnlagService.hentPersonerPåBehandling(listOf(restEndretUtbetalingAndel.personIdent!!), behandling)
                .first()

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
                ?: throw Feil("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        val andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        endretUtbetalingAndel.fraRestEndretUtbetalingAndel(restEndretUtbetalingAndel, person)

        validerEndretUtbetalingAndelErGyldigForÅrsak(endretUtbetalingAndel = endretUtbetalingAndel, andelTilkjentYtelser = andelTilkjentYtelser)

        val andreEndredeAndelerPåBehandling = endretUtbetalingAndelRepository.findByBehandlingId(behandling.id)
            .filter { it.id != endretUtbetalingAndelId }

        val gyldigTomEtterDagensDato = beregnTomHvisDenIkkeErSatt(
            andreEndredeAndelerPåBehandling = andreEndredeAndelerPåBehandling,
            endretUtbetalingAndel = endretUtbetalingAndel,
            andelTilkjentYtelser = andelTilkjentYtelser
        )
        if (endretUtbetalingAndel.tom?.isAfter(YearMonth.now()) == true && endretUtbetalingAndel.tom != gyldigTomEtterDagensDato) {
            throw FunksjonellFeil(
                frontendFeilmelding = "Du kan ikke legge inn til og med dato som er i neste måned eller senere. Om det gjelder en løpende periode vil systemet legge inn riktig dato for deg.",
                melding = "Du kan ikke legge inn til og med dato som er i neste måned eller senere. Om det gjelder en løpende periode vil systemet legge inn riktig dato for deg."
            )
        }

        if (endretUtbetalingAndel.tom == null) {
            endretUtbetalingAndel.tom = gyldigTomEtterDagensDato
        }

        validerIngenOverlappendeEndring(
            endretUtbetalingAndel = endretUtbetalingAndel,
            eksisterendeEndringerPåBehandling = andreEndredeAndelerPåBehandling
        )

        validerPeriodeInnenforTilkjentytelse(endretUtbetalingAndel, andelTilkjentYtelser)

        beregningService.oppdaterBehandlingMedBeregning(
            behandling, personopplysningGrunnlag, endretUtbetalingAndel
        )
    }

    @Transactional
    fun fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelId: Long,
    ) {
        val endretUtbetalingAndel = endretUtbetalingAndelRepository.getById(endretUtbetalingAndelId)
        endretUtbetalingAndel.andelTilkjentYtelser.forEach { it.endretUtbetalingAndeler.clear() }
        endretUtbetalingAndelRepository.delete(endretUtbetalingAndel)

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
                ?: throw Feil("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
    }

    @Transactional
    fun opprettTomEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling
    ) =
        endretUtbetalingAndelRepository.save(
            EndretUtbetalingAndel(
                behandlingId = behandling.id,
            )
        )

    @Transactional
    fun kopierEndretUtbetalingAndelFraForrigeBehandling(behandling: Behandling, forrigeBehandling: Behandling) {
        hentForBehandling(forrigeBehandling.id).forEach {
            endretUtbetalingAndelRepository.save(
                it.copy(
                    id = 0,
                    behandlingId = behandling.id,
                    andelTilkjentYtelser = mutableListOf()
                )
            )
        }
    }

    fun hentForBehandling(behandlingId: Long) = endretUtbetalingAndelRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun fjernKnytningTilAndelTilkjentYtelse(behandlingId: Long) {
        hentForBehandling(behandlingId).filter { it.andelTilkjentYtelser.isNotEmpty() }.forEach {
            it.andelTilkjentYtelser.clear()
        }
    }

    @Transactional
    fun oppdaterEndreteUtbetalingsandelerMedBegrunnelser(behandling: Behandling): MutableList<EndretUtbetalingAndel> {
        val endredeUtbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(behandling.id)
        val andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        endredeUtbetalingAndeler.forEach {
            val utvidetScenario =
                andelTilkjentYtelser.hentUtvidetScenarioForEndringsperiode(it.periode)

            it.vedtakBegrunnelseSpesifikasjoner =
                listOf(
                    it.hentGyldigEndretBegrunnelse(
                        sanityService.hentSanityBegrunnelser(),
                        utvidetScenario,
                    )
                )
        }

        return endretUtbetalingAndelRepository.saveAll(endredeUtbetalingAndeler)
    }
}
