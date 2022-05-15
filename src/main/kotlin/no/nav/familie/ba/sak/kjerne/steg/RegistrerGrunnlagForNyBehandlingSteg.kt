package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RegistrerGrunnlagForNyBehandlingSteg(
    private val behandlingService: BehandlingService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val personidentService: PersonidentService,
    private val vilkårService: VilkårService,
    private val kompetanseService: KompetanseService,
    private val featureToggleService: FeatureToggleService,
    private val valutakursService: ValutakursService,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService
) : BehandlingSteg<RegistrerGrunnlagForNyBehandlingDTO> {

    @Transactional
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: RegistrerGrunnlagForNyBehandlingDTO
    ): StegType {
        val forrigeBehandlingSomErVedtatt = behandlingHentOgPersisterService
            .hentForrigeBehandlingSomErVedtatt(behandling)

        opprettPersonopplysningGrunnlag(
            behandling,
            forrigeBehandlingSomErVedtatt,
            data.ident,
            data.barnasIdenter
        )

        opprettVilkårsvurdering(
            behandling,
            forrigeBehandlingSomErVedtatt,
            data.nyMigreringsdato!!
        )

        if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
            kopierKompetanse(
                behandling.id,
                forrigeBehandlingSomErVedtatt?.id
            )

            kopierValutakurs(
                behandling.id,
                forrigeBehandlingSomErVedtatt?.id
            )

            kopierUtenlandskPeriodebeløp(
                behandling.id,
                forrigeBehandlingSomErVedtatt?.id
            )
        }

        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun opprettPersonopplysningGrunnlag(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling?,
        søkerIdent: String,
        barnasIdenter: List<String>
    ) {
        val søker = personidentService.hentOgLagreAktør(søkerIdent, true)
        val barna = personidentService.hentOgLagreAktørIder(barnasIdenter, true)

        if (behandling.type == BehandlingType.REVURDERING && forrigeBehandlingSomErVedtatt != null) {
            val forrigePersongrunnlagBarna =
                beregningService.finnBarnFraBehandlingMedTilkjentYtsele(behandlingId = forrigeBehandlingSomErVedtatt.id)
            val forrigeMålform =
                persongrunnlagService.hentSøkersMålform(behandlingId = forrigeBehandlingSomErVedtatt.id)

            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = søker,
                barnFraInneværendeBehandling = barna,
                barnFraForrigeBehandling = forrigePersongrunnlagBarna,
                behandling = behandling,
                målform = forrigeMålform
            )
        } else {
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = søker,
                barnFraInneværendeBehandling = barna,
                behandling = behandling,
                målform = Målform.NB
            )
        }
    }

    private fun opprettVilkårsvurdering(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling?,
        nyMigreringsdato: LocalDate
    ) {
        when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.ENDRE_MIGRERINGSDATO -> {
                vilkårService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                    behandling = behandling,
                    forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt,
                    nyMigreringsdato = nyMigreringsdato
                )
                // Lagre ned migreringsdato
                behandlingService.lagreNedMigreringsdato(nyMigreringsdato, behandling)
            }
            BehandlingÅrsak.HELMANUELL_MIGRERING -> {
                vilkårService.genererVilkårsvurderingForHelmanuellMigrering(
                    behandling = behandling,
                    nyMigreringsdato = nyMigreringsdato
                )
                // Lagre ned migreringsdato
                behandlingService.lagreNedMigreringsdato(nyMigreringsdato, behandling)
            }
            !in listOf(BehandlingÅrsak.SØKNAD, BehandlingÅrsak.FØDSELSHENDELSE) -> {
                vilkårService.initierVilkårsvurderingForBehandling(
                    behandling = behandling,
                    bekreftEndringerViaFrontend = true,
                    forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt
                )
            }
            else -> logger.info(
                "Perioder i vilkårsvurdering generer ikke automatisk for " +
                    behandling.opprettetÅrsak.visningsnavn
            )
        }
    }

    fun kopierKompetanse(
        behandlingId: Long,
        forrigeBehandlingId: Long?
    ) {
        if (forrigeBehandlingId != null)
            kompetanseService.kopierKompetanse(
                fraBehandlingId = forrigeBehandlingId,
                tilBehandlingId = behandlingId
            )
    }

    fun kopierValutakurs(
        behandlingId: Long,
        forrigeBehandlingId: Long?
    ) {
        if (forrigeBehandlingId != null)
            valutakursService.kopierValutakurser(
                fraBehandlingId = forrigeBehandlingId,
                tilBehandlingId = behandlingId
            )
    }

    fun kopierUtenlandskPeriodebeløp(
        behandlingId: Long,
        forrigeBehandlingId: Long?
    ) {
        if (forrigeBehandlingId != null)
            utenlandskPeriodebeløpService.kopierUtenlandskePeriodebeløp(
                fraBehandlingId = forrigeBehandlingId,
                tilBehandlingId = behandlingId
            )
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_GRUNNLAG_FOR_NY_BEHANDLING
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}

data class RegistrerGrunnlagForNyBehandlingDTO(
    val ident: String,
    val barnasIdenter: List<String>,
    val nyMigreringsdato: LocalDate? = null
)
