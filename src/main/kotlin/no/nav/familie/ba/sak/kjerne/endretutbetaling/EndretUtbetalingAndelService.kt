package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerOgSettTomDatoHvisNull
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.fraRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak.ETTERBETALING_3MND
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak.ETTERBETALING_3ÅR
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
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
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val endretUtbetalingAndelOppdatertAbonnementer: List<EndretUtbetalingAndelerOppdatertAbonnent> = emptyList(),
    private val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
    private val unleashService: UnleashNextMedContextService,
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
                .filter { personerPåEndretUtbetalingAndel.contains(it.aktør.aktivFødselsnummer()) }

        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandling.id)
                ?: throw Feil("Fant ikke vilkårsvurdering på behandling ${behandling.id}")

        val andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        endretUtbetalingAndel.fraRestEndretUtbetalingAndel(restEndretUtbetalingAndel, personer.toSet())

        val andreEndredeAndelerPåBehandling =
            endretUtbetalingAndelHentOgPersisterService
                .hentForBehandling(behandling.id)
                .filter { it.id != endretUtbetalingAndelId }
                .filterNot { it.manglerObligatoriskFelt() }

        val gyldigTomDatoPerAktør =
            beregnGyldigTomPerAktør(
                andreEndredeAndelerPåBehandling = andreEndredeAndelerPåBehandling,
                endretUtbetalingAndel = endretUtbetalingAndel,
                andelTilkjentYtelser = andelTilkjentYtelser,
            )

        val toggleErPå = unleashService.isEnabled(FeatureToggle.SKAL_SPLITTE_ENDRET_UTBETALING_ANDELER)
        if (skalSplitteEndretUtbetalingAndel(toggleErPå, endretUtbetalingAndel, gyldigTomDatoPerAktør)) {
            splittValiderOgLagreEndretUtbetalingAndeler(
                endretUtbetalingAndel = endretUtbetalingAndel,
                gyldigTomDatoPerAktør = gyldigTomDatoPerAktør,
                andreEndredeAndelerPåBehandling = andreEndredeAndelerPåBehandling,
                andelerTilkjentYtelse = andelTilkjentYtelser,
                vilkårsvurdering = vilkårsvurdering,
            )
        } else {
            validerOgLagreEndretUtbetalingAndel(
                endretUtbetalingAndel = endretUtbetalingAndel,
                andreEndredeAndelerPåBehandling = andreEndredeAndelerPåBehandling,
                andelerTilkjentYtelse = andelTilkjentYtelser,
                vilkårsvurdering = vilkårsvurdering,
            )
        }

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
    fun opprettTomEndretUtbetalingAndel(behandling: Behandling) =
        endretUtbetalingAndelRepository.save(
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

    @Transactional
    fun genererEndretUtbetalingAndelerMedÅrsakEtterbetaling3ÅrEller3Mnd(behandling: Behandling) {
        val søknadMottattDato = behandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id)?.toLocalDate() ?: return

        fjernEndretUtbetalingAndelerMedÅrsak3MndEller3ÅrGenerertIDenneBehandlingen(behandling)

        val nåværendeAndeler = beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id)
        val forrigeAndeler = beregningService.hentAndelerFraForrigeIverksattebehandling(behandling)
        val personIdenter = (nåværendeAndeler + forrigeAndeler).map { it.aktør.aktivFødselsnummer() }.distinct()
        val personerPåBehandling = persongrunnlagService.hentPersonerPåBehandling(personIdenter, behandling)

        val endretUtbetalingAndeler =
            genererEndretUtbetalingAndelerMedÅrsakEtterbetaling3ÅrEller3Mnd(
                behandling = behandling,
                søknadMottattDato = søknadMottattDato,
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
                personerPåBehandling = personerPåBehandling,
            )

        endretUtbetalingAndelRepository.saveAllAndFlush(endretUtbetalingAndeler)

        oppdaterBehandlingMedBeregningOgVarsleAbonnenter(behandling)
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

    private fun validerOgLagreEndretUtbetalingAndel(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        andreEndredeAndelerPåBehandling: List<EndretUtbetalingAndel>,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        vilkårsvurdering: Vilkårsvurdering,
    ) {
        validerOgSettTomDatoHvisNull(
            endretUtbetalingAndel = endretUtbetalingAndel,
            andreEndredeAndelerPåBehandling = andreEndredeAndelerPåBehandling,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            vilkårsvurdering = vilkårsvurdering,
        )
        endretUtbetalingAndelRepository.saveAndFlush(endretUtbetalingAndel)
    }

    private fun splittValiderOgLagreEndretUtbetalingAndeler(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        gyldigTomDatoPerAktør: Map<Aktør, YearMonth?>,
        andreEndredeAndelerPåBehandling: List<EndretUtbetalingAndel>,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        vilkårsvurdering: Vilkårsvurdering,
    ) {
        val splittedeAndeler =
            splittEndretUbetalingAndel(
                endretUtbetalingAndel = endretUtbetalingAndel,
                gyldigTomEtterDagensDatoPerAktør = gyldigTomDatoPerAktør,
            ).onEach { endretUtbetalingAndel ->
                validerOgSettTomDatoHvisNull(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    andreEndredeAndelerPåBehandling = andreEndredeAndelerPåBehandling,
                    andelerTilkjentYtelse = andelerTilkjentYtelse,
                    vilkårsvurdering = vilkårsvurdering,
                )
            }

        endretUtbetalingAndelRepository.delete(endretUtbetalingAndel) // Slett andelen som ble splittet
        endretUtbetalingAndelRepository.saveAll(splittedeAndeler)
    }
}

interface EndretUtbetalingAndelerOppdatertAbonnent {
    fun endretUtbetalingAndelerOppdatert(
        behandlingId: BehandlingId,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    )
}
