package no.nav.familie.ba.sak.kjerne.behandling

import com.fasterxml.jackson.annotation.JsonAutoDetect
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrerInstitusjonDto
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrerSøknadDto
import no.nav.familie.ba.sak.ekstern.restDomene.TilbakekrevingDto
import no.nav.familie.ba.sak.ekstern.restDomene.UtvidetBehandlingDto
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils.validerBehandlingIkkeSendtTilEksterneTjenester
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils.validerhenleggelsestype
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.BeslutningPåVedtakDto
import no.nav.familie.ba.sak.kjerne.steg.BehandlingsresultatSteg
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * tilgangService.validerTilgangTilBehandling gjøres inne i stegService for hvert endepunkt
 */
@RestController
@RequestMapping("/api/behandlinger/{behandlingId}/steg")
@ProtectedWithClaims(issuer = "azuread")
class BehandlingStegController(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val stegService: StegService,
    private val tilgangService: TilgangService,
    private val featureToggleService: FeatureToggleService,
) {
    @PostMapping(
        path = ["registrer-søknad"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun registrereSøknadOgHentPersongrunnlag(
        @PathVariable behandlingId: Long,
        @RequestBody registrerSøknadDto: RegistrerSøknadDto,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "registrere søknad",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)

        stegService.håndterSøknad(behandling = behandling, registrerSøknadDto = registrerSøknadDto)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["vilkårsvurdering"])
    fun validerVilkårsvurdering(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "vurdere vilkårsvurdering",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        stegService.håndterVilkårsvurdering(behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandling.id)))
    }

    @GetMapping(path = ["behandlingsresultat/valider"])
    fun validerBehandlingsresultat(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<Boolean>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "validere behandlingsresultat",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val behandlingSteg: BehandlingsresultatSteg =
            stegService.hentBehandlingSteg(StegType.BEHANDLINGSRESULTAT) as BehandlingsresultatSteg

        behandlingSteg.preValiderSteg(
            behandling = behandling,
            stegService = stegService,
        )

        return ResponseEntity.ok(
            Ressurs.success(
                true,
            ),
        )
    }

    @PostMapping(path = ["behandlingsresultat"])
    fun utledBehandlingsresultat(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "vurdere behandlingsresultat",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        stegService.håndterBehandlingsresultat(behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["tilbakekreving"])
    fun lagreTilbakekrevingOgGåVidereTilNesteSteg(
        @PathVariable behandlingId: Long,
        @RequestBody tilbakekrevingDto: TilbakekrevingDto?,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "vurdere tilbakekreving",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        stegService.håndterVurderTilbakekreving(behandling, tilbakekrevingDto)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["send-til-beslutter"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendTilBeslutter(
        @PathVariable behandlingId: Long,
        @RequestParam behandlendeEnhet: String,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "foreslå vedtak",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        stegService.håndterSendTilBeslutter(behandling, behandlendeEnhet)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandling.id)))
    }

    @PostMapping(path = ["iverksett-vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun iverksettVedtak(
        @PathVariable behandlingId: Long,
        @RequestBody beslutningPåVedtakDto: BeslutningPåVedtakDto,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.BESLUTTER,
            handling = "iverksette vedtak",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        stegService.håndterBeslutningForVedtak(behandling, beslutningPåVedtakDto)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandling.id)))
    }

    @PutMapping(path = ["henlegg"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun henleggBehandlingOgSendBrev(
        @PathVariable behandlingId: Long,
        @RequestBody henleggInfo: HenleggBehandlingInfoDto,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "henlegge behandling",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        validerhenleggelsestype(
            henleggÅrsak = henleggInfo.årsak,
            tekniskVedlikeholdToggel = featureToggleService.isEnabled(FeatureToggle.TEKNISK_VEDLIKEHOLD_HENLEGGELSE, behandling.id),
            behandlingId = behandling.id,
        )

        validerTilgangTilHenleggelseAvBehandling(
            behandling = behandling,
            tekniskEndringToggle = featureToggleService.isEnabled(FeatureToggle.TEKNISK_ENDRING, behandling.id),
        )

        validerBehandlingIkkeSendtTilEksterneTjenester(behandling = behandling)

        stegService.håndterHenleggBehandling(behandling, henleggInfo)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandling.id)))
    }

    private fun validerTilgangTilHenleggelseAvBehandling(
        behandling: Behandling,
        tekniskEndringToggle: Boolean,
    ) {
        if (behandling.erTekniskEndring() && !tekniskEndringToggle) {
            throw FunksjonellFeil("Du har ikke tilgang til å henlegge en behandling som er opprettet med årsak=${behandling.opprettetÅrsak.visningsnavn}. Ta kontakt med teamet dersom dette ikke stemmer.")
        }
    }

    @PostMapping(path = ["registrer-institusjon"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun registerInstitusjon(
        @PathVariable behandlingId: Long,
        @RequestBody restInstitusjon: RegistrerInstitusjonDto,
    ): ResponseEntity<Ressurs<UtvidetBehandlingDto>> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val institusjon = restInstitusjon.tilInstitusjon()

        stegService.håndterRegistrerInstitusjon(behandling, institusjon)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagUtvidetBehandlingDto(behandlingId = behandling.id)))
    }
}

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class HenleggBehandlingInfoDto(
    val årsak: HenleggÅrsak,
    val begrunnelse: String,
)

enum class HenleggÅrsak(
    val beskrivelse: String,
) {
    SØKNAD_TRUKKET("Søknad trukket"),
    FEILAKTIG_OPPRETTET("Behandling feilaktig opprettet"),
    AUTOMATISK_HENLAGT("Behandlingen er automatisk henlagt"),
    TEKNISK_VEDLIKEHOLD("Teknisk vedlikehold"),
    ;

    fun tilBehandlingsresultat(opprettetÅrsak: BehandlingÅrsak) =
        when (this) {
            FEILAKTIG_OPPRETTET -> {
                Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET
            }

            SØKNAD_TRUKKET -> {
                Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET
            }

            AUTOMATISK_HENLAGT -> {
                when (opprettetÅrsak) {
                    BehandlingÅrsak.SMÅBARNSTILLEGG, BehandlingÅrsak.SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID -> Behandlingsresultat.HENLAGT_AUTOMATISK_SMÅBARNSTILLEGG
                    else -> Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE
                }
            }

            TEKNISK_VEDLIKEHOLD -> {
                Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD
            }
        }
}
