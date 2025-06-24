package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerIngenOverlappendeEndring
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerPeriodeInnenforTilkjentytelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerÅrsak
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.fraRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak.ETTERBETALING_3MND
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak.ETTERBETALING_3ÅR
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
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
    private val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,
) {
    @Transactional
    fun oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelId: Long,
        restEndretUtbetalingAndel: RestEndretUtbetalingAndel,
    ) {
        val endretUtbetalingAndel = endretUtbetalingAndelRepository.getReferenceById(endretUtbetalingAndelId)
        val personerPåEndretUtbetalingAndel =
            restEndretUtbetalingAndel.personIdenter
                ?: restEndretUtbetalingAndel.personIdent?.let { listOf(it) }
                ?: throw FunksjonellFeil("Endret utbetaling andel må ha minst én person ident")

        val personer =
            persongrunnlagService
                .hentPersonerPåBehandling(personerPåEndretUtbetalingAndel, behandling)

        val andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        endretUtbetalingAndel.fraRestEndretUtbetalingAndel(restEndretUtbetalingAndel, personer.toSet())

        val andreEndredeAndelerPåBehandling =
            endretUtbetalingAndelHentOgPersisterService
                .hentForBehandling(behandling.id)
                .filter { it.id != endretUtbetalingAndelId }
                .filterNot { it.manglerObligatoriskFelt() }

        val gyldigTomEtterDagensDato =
            beregnGyldigTomIFremtiden(
                andreEndredeAndelerPåBehandling = andreEndredeAndelerPåBehandling,
                endretUtbetalingAndel = endretUtbetalingAndel,
                andelTilkjentYtelser = andelTilkjentYtelser,
            )

        validerTomDato(
            tomDato = endretUtbetalingAndel.tom,
            gyldigTomEtterDagensDato = gyldigTomEtterDagensDato,
            årsak = endretUtbetalingAndel.årsak,
        )

        if (endretUtbetalingAndel.tom == null) {
            endretUtbetalingAndel.tom = gyldigTomEtterDagensDato
        }
        validerÅrsak(
            endretUtbetalingAndel = endretUtbetalingAndel,
            vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id),
        )

        validerUtbetalingMotÅrsak(
            årsak = endretUtbetalingAndel.årsak,
            skalUtbetales = endretUtbetalingAndel.prosent != BigDecimal(0),
        )

        validerIngenOverlappendeEndring(
            endretUtbetalingAndel = endretUtbetalingAndel,
            eksisterendeEndringerPåBehandling = andreEndredeAndelerPåBehandling,
        )

        validerPeriodeInnenforTilkjentytelse(endretUtbetalingAndel, andelTilkjentYtelser)

        endretUtbetalingAndelRepository.saveAndFlush(endretUtbetalingAndel)

        oppdaterBehandlingMedBeregningOgVarsleAbonnenter(behandling)
    }

    @Transactional
    fun fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelId: Long,
    ) = fjernEndretUtbetalingAndelerOgOppdaterTilkjentYtelse(behandling, listOf(endretUtbetalingAndelId))

    @Transactional
    fun fjernEndretUtbetalingAndelerOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelIder: List<Long>,
    ) {
        endretUtbetalingAndelRepository.deleteAllById(endretUtbetalingAndelIder)

        oppdaterBehandlingMedBeregningOgVarsleAbonnenter(behandling)
    }

    @Transactional
    fun fjernEndretUtbetalingAndelerMedÅrsak3MndEller3ÅrGenerertIDenneBehandlingen(behandling: Behandling) {
        val endretUtbetalingAndelerSomSkalSlettes =
            endretUtbetalingAndelRepository
                .findByBehandlingId(behandling.id)
                .filter { it.årsak in setOf(ETTERBETALING_3ÅR, ETTERBETALING_3MND) && it.behandlingId == behandling.id }
                .map { it.id }

        fjernEndretUtbetalingAndelerOgOppdaterTilkjentYtelse(behandling, endretUtbetalingAndelerSomSkalSlettes)
    }

    @Transactional
    fun opprettTomEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
    ) = endretUtbetalingAndelRepository.save(
        EndretUtbetalingAndel(
            behandlingId = behandling.id,
        ),
    )

    @Transactional
    fun kopierEndretUtbetalingAndelFraForrigeBehandling(
        behandling: Behandling,
        forrigeBehandling: Behandling,
    ) {
        endretUtbetalingAndelHentOgPersisterService.hentForBehandling(forrigeBehandling.id).forEach {
            endretUtbetalingAndelRepository.save(
                it.copy(
                    id = 0,
                    behandlingId = behandling.id,
                    personer = it.personer.toMutableSet(),
                ),
            )
        }
    }

    private fun oppdaterBehandlingMedBeregningOgVarsleAbonnenter(behandling: Behandling) {
        val personopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
                ?: throw Feil("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        endretUtbetalingAndelOppdatertAbonnementer.forEach { abonnent ->
            abonnent.endretUtbetalingAndelerOppdatert(
                behandlingId = BehandlingId(behandling.id),
                endretUtbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(behandling.id),
            )
        }
    }
}

interface EndretUtbetalingAndelerOppdatertAbonnent {
    fun endretUtbetalingAndelerOppdatert(
        behandlingId: BehandlingId,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    )
}
