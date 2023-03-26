package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerIngenOverlappendeEndring
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerPeriodeInnenforTilkjentytelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerÅrsak
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.fraRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class EndretUtbetalingAndelService(
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val endretUtbetalingAndelOppdatertAbonnementer: List<EndretUtbetalingAndelerOppdatertAbonnent> = emptyList(),
    private val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService
) {
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
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.behandlingId.id)
                ?: throw Feil("Fant ikke personopplysninggrunnlag på behandling ${behandling.behandlingId}")

        val andelTilkjentYtelser =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.behandlingId.id)

        endretUtbetalingAndel.fraRestEndretUtbetalingAndel(restEndretUtbetalingAndel, person)

        val andreEndredeAndelerPåBehandling =
            endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandling.behandlingId)
                .filter { it.id != endretUtbetalingAndelId }

        val gyldigTomEtterDagensDato = beregnGyldigTomIFremtiden(
            andreEndredeAndelerPåBehandling = andreEndredeAndelerPåBehandling,
            endretUtbetalingAndel = endretUtbetalingAndel,
            andelTilkjentYtelser = andelTilkjentYtelser
        )

        validerTomDato(
            tomDato = endretUtbetalingAndel.tom,
            gyldigTomEtterDagensDato = gyldigTomEtterDagensDato,
            årsak = endretUtbetalingAndel.årsak
        )

        if (endretUtbetalingAndel.tom == null) {
            endretUtbetalingAndel.tom = gyldigTomEtterDagensDato
        }
        validerÅrsak(
            årsak = endretUtbetalingAndel.årsak,
            endretUtbetalingAndel = endretUtbetalingAndel,
            vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.behandlingId)
        )

        validerUtbetalingMotÅrsak(
            årsak = endretUtbetalingAndel.årsak,
            skalUtbetales = endretUtbetalingAndel.prosent != BigDecimal(0)
        )

        validerIngenOverlappendeEndring(
            endretUtbetalingAndel = endretUtbetalingAndel,
            eksisterendeEndringerPåBehandling = andreEndredeAndelerPåBehandling
        )

        validerPeriodeInnenforTilkjentytelse(endretUtbetalingAndel, andelTilkjentYtelser)

        endretUtbetalingAndelRepository.saveAndFlush(endretUtbetalingAndel)

        beregningService.oppdaterBehandlingMedBeregning(
            behandling,
            personopplysningGrunnlag,
            endretUtbetalingAndel
        )

        endretUtbetalingAndelOppdatertAbonnementer.forEach {
            it.endretUtbetalingAndelerOppdatert(
                behandlingId = behandling.behandlingId,
                endretUtbetalingAndeler = andreEndredeAndelerPåBehandling + endretUtbetalingAndel
            )
        }
    }

    @Transactional
    fun fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelId: Long
    ) {
        endretUtbetalingAndelRepository.deleteById(endretUtbetalingAndelId)

        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.behandlingId.id)
                ?: throw Feil("Fant ikke personopplysninggrunnlag på behandling ${behandling.behandlingId}")

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        endretUtbetalingAndelOppdatertAbonnementer.forEach { abonnent ->
            abonnent.endretUtbetalingAndelerOppdatert(
                behandlingId = behandling.behandlingId,
                endretUtbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(behandling.behandlingId.id)
            )
        }
    }

    @Transactional
    fun opprettTomEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling
    ) =
        endretUtbetalingAndelRepository.save(
            EndretUtbetalingAndel(
                behandlingId = behandling.behandlingId
            )
        )

    @Transactional
    fun kopierEndretUtbetalingAndelFraForrigeBehandling(behandling: Behandling, forrigeBehandling: Behandling) {
        endretUtbetalingAndelHentOgPersisterService.hentForBehandling(forrigeBehandling.behandlingId).forEach {
            endretUtbetalingAndelRepository.save(
                it.copy(
                    id = 0,
                    behandlingId = behandling.behandlingId
                )
            )
        }
    }
}

interface EndretUtbetalingAndelerOppdatertAbonnent {
    fun endretUtbetalingAndelerOppdatert(
        behandlingId: BehandlingId,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>
    )
}
