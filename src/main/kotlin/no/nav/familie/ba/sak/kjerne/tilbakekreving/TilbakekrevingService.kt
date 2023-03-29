package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.Tilbakekreving
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Brevmottaker
import no.nav.familie.kontrakter.felles.tilbakekreving.FeilutbetaltePerioderDto
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.ManuellAdresseInfo
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType.FULLMEKTIG
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType.VERGE
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Verge
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakekrevingService(
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val vedtakRepository: VedtakRepository,
    private val totrinnskontrollRepository: TotrinnskontrollRepository,
    private val brevmottakerRepository: BrevmottakerRepository,
    private val simuleringService: SimuleringService,
    private val tilgangService: TilgangService,
    private val persongrunnlagService: PersongrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val tilbakekrevingKlient: TilbakekrevingKlient,
    private val personidentService: PersonidentService,
    private val personopplysningerService: PersonopplysningerService,
    private val featureToggleService: FeatureToggleService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService
) {

    fun validerRestTilbakekreving(restTilbakekreving: RestTilbakekreving?, behandlingId: BehandlingId) {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "opprette tilbakekreving"
        )

        val feilutbetaling = simuleringService.hentFeilutbetaling(behandlingId = behandlingId)
        validerVerdierPåRestTilbakekreving(restTilbakekreving, feilutbetaling)
    }

    @Transactional
    fun lagreTilbakekreving(restTilbakekreving: RestTilbakekreving, behandlingId: BehandlingId): Tilbakekreving? {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)

        val tilbakekreving = Tilbakekreving(
            begrunnelse = restTilbakekreving.begrunnelse,
            behandling = behandling,
            valg = restTilbakekreving.valg,
            varsel = restTilbakekreving.varsel,
            tilbakekrevingsbehandlingId = tilbakekrevingRepository
                .findByBehandlingId(behandling.behandlingId.id)?.tilbakekrevingsbehandlingId
        )

        tilbakekrevingRepository.deleteByBehandlingId(behandlingId.id)
        return tilbakekrevingRepository.save(tilbakekreving)
    }

    fun hentTilbakekrevingsvalg(behandlingId: BehandlingId): Tilbakekrevingsvalg? {
        return tilbakekrevingRepository.findByBehandlingId(behandlingId.id)?.valg
    }

    fun slettTilbakekrevingPåBehandling(behandlingId: BehandlingId) =
        tilbakekrevingRepository.findByBehandlingId(behandlingId.id)?.let { tilbakekrevingRepository.delete(it) }

    fun hentForhåndsvisningVarselbrev(
        behandlingId: BehandlingId,
        forhåndsvisTilbakekrevingsvarselbrevRequest: ForhåndsvisTilbakekrevingsvarselbrevRequest
    ): ByteArray {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hent forhåndsvisning av varselbrev for tilbakekreving"
        )

        val vedtak = vedtakRepository.findByBehandlingAndAktivOptional(behandlingId.id)
            ?: throw Feil(
                "Fant ikke vedtak for behandling $behandlingId ved forhåndsvisning av varselbrev" +
                    " for tilbakekreving."
            )

        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId)
        val arbeidsfordeling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)
        val institusjon = hentTilbakekrevingInstitusjon(vedtak.behandling.fagsak)
        val verge = hentVerge(vedtak.behandling.verge?.ident)

        return tilbakekrevingKlient.hentForhåndsvisningVarselbrev(
            forhåndsvisVarselbrevRequest = ForhåndsvisVarselbrevRequest(
                varseltekst = forhåndsvisTilbakekrevingsvarselbrevRequest.fritekst,
                ytelsestype = Ytelsestype.BARNETRYGD,
                behandlendeEnhetId = arbeidsfordeling.behandlendeEnhetId,
                behandlendeEnhetsNavn = arbeidsfordeling.behandlendeEnhetNavn,
                språkkode = persongrunnlag.søker.målform.tilSpråkkode(),
                feilutbetaltePerioderDto = FeilutbetaltePerioderDto(
                    sumFeilutbetaling = simuleringService.hentFeilutbetaling(behandlingId).toLong(),
                    perioder = hentTilbakekrevingsperioderISimulering(
                        simuleringService.hentSimuleringPåBehandling(behandlingId),
                        featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ)
                    )
                ),
                fagsystem = Fagsystem.BA,
                eksternFagsakId = vedtak.behandling.fagsak.id.toString(),
                ident = persongrunnlag.søker.aktør.aktivFødselsnummer(),
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerNavn(),
                verge = verge,
                institusjon = institusjon
            )
        )
    }

    fun søkerHarÅpenTilbakekreving(fagsakId: Long): Boolean =
        tilbakekrevingKlient.harÅpenTilbakekrevingsbehandling(fagsakId)

    fun opprettTilbakekreving(behandling: Behandling): TilbakekrevingId =
        tilbakekrevingKlient.opprettTilbakekrevingBehandling(lagOpprettTilbakekrevingRequest(behandling))

    fun lagOpprettTilbakekrevingRequest(behandling: Behandling): OpprettTilbakekrevingRequest {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandling.behandlingId)

        val enhet = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.behandlingId)

        val aktivtVedtak = vedtakRepository.findByBehandlingAndAktivOptional(behandling.behandlingId.id)
            ?: throw Feil("Fant ikke aktivt vedtak på behandling ${behandling.behandlingId}")

        val totrinnskontroll = totrinnskontrollRepository.findByBehandlingAndAktiv(behandling.behandlingId.id)

        val revurderingsvedtaksdato = aktivtVedtak.vedtaksdato?.toLocalDate() ?: throw Feil(
            message = "Finner ikke revurderingsvedtaksdato på vedtak ${aktivtVedtak.id} " +
                "ved iverksetting av tilbakekreving mot familie-tilbake"
        )

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.behandlingId.id)
            ?: throw Feil("Fant ikke tilbakekreving på behandling ${behandling.behandlingId}")

        val institusjon = hentTilbakekrevingInstitusjon(behandling.fagsak)
        val verge = hentVerge(behandling.verge?.ident)

        val manuelleBrevMottakere =
            brevmottakerRepository.finnBrevMottakereForBehandling(behandling.behandlingId.id).map { baSakBrevMottaker ->
                val mottakerType = MottakerType.valueOf(baSakBrevMottaker.type.name)
                val vergetype = when {
                    mottakerType == FULLMEKTIG -> Vergetype.ANNEN_FULLMEKTIG
                    mottakerType == VERGE && behandling.fagsak.type == FagsakType.NORMAL -> Vergetype.VERGE_FOR_VOKSEN
                    mottakerType == VERGE && behandling.fagsak.type != FagsakType.NORMAL -> Vergetype.VERGE_FOR_BARN
                    else -> null
                }

                Brevmottaker(
                    type = mottakerType,
                    vergetype = vergetype,
                    navn = baSakBrevMottaker.navn,
                    manuellAdresseInfo = ManuellAdresseInfo(
                        adresselinje1 = baSakBrevMottaker.adresselinje1,
                        adresselinje2 = baSakBrevMottaker.adresselinje2,
                        postnummer = baSakBrevMottaker.postnummer,
                        poststed = baSakBrevMottaker.poststed,
                        landkode = baSakBrevMottaker.landkode
                    )
                )
            }.toSet()

        return OpprettTilbakekrevingRequest(
            fagsystem = Fagsystem.BA,
            regelverk = behandling.kategori.tilRegelverk(),
            ytelsestype = Ytelsestype.BARNETRYGD,
            eksternFagsakId = behandling.fagsak.id.toString(),
            personIdent = personopplysningGrunnlag.søker.aktør.aktivFødselsnummer(),
            eksternId = behandling.behandlingId.id.toString(),
            behandlingstype = Behandlingstype.TILBAKEKREVING,
            // Manuelt opprettet er per nå ikke håndtert i familie-tilbake.
            manueltOpprettet = false,
            språkkode = personopplysningGrunnlag.søker.målform.tilSpråkkode(),
            enhetId = enhet.behandlendeEnhetId,
            enhetsnavn = enhet.behandlendeEnhetNavn,
            saksbehandlerIdent = totrinnskontroll?.saksbehandlerId ?: SikkerhetContext.hentSaksbehandler(),
            varsel = opprettVarsel(
                tilbakekreving,
                simuleringService.hentSimuleringPåBehandling(behandling.behandlingId),
                featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ)
            ),
            revurderingsvedtaksdato = revurderingsvedtaksdato,
            // Verge er per nå ikke støttet i familie-ba-sak.
            verge = verge,
            faktainfo = hentFaktainfoForTilbakekreving(behandling, tilbakekreving),
            institusjon = institusjon,
            manuelleBrevmottakere = manuelleBrevMottakere
        )
    }

    fun opprettTilbakekrevingsbehandlingManuelt(fagsakId: Long): Ressurs<String> {
        val kanOpprettesRespons = tilbakekrevingKlient.kanTilbakekrevingsbehandlingOpprettesManuelt(fagsakId)
        if (!kanOpprettesRespons.kanBehandlingOpprettes) {
            return Ressurs.funksjonellFeil(
                frontendFeilmelding = kanOpprettesRespons.melding,
                melding = "familie-tilbake svarte nei på om tilbakekreving kunne opprettes"
            )
        }

        val behandling = kanOpprettesRespons.kravgrunnlagsreferanse?.toLong()
            ?.let {
                behandlingHentOgPersisterService.finnAvsluttedeBehandlingerPåFagsak(fagsakId = fagsakId)
                    .find { beh -> beh.behandlingId.id == it }
            }
        return if (behandling != null) {
            tilbakekrevingKlient.opprettTilbakekrevingsbehandlingManuelt(
                OpprettManueltTilbakekrevingRequest(
                    eksternFagsakId = fagsakId.toString(),
                    ytelsestype = Ytelsestype.BARNETRYGD,
                    eksternId = kanOpprettesRespons.kravgrunnlagsreferanse!!
                )
            )

            Ressurs.success("Tilbakekreving opprettet")
        } else {
            logger.error("Kan ikke opprette tilbakekrevingsbehandling. Respons inneholder referanse til en ukjent behandling")
            Ressurs.funksjonellFeil(
                melding = "Kan ikke opprette tilbakekrevingsbehandling. Respons inneholder referanse til en ukjent behandling",
                frontendFeilmelding = "Av tekniske årsaker så kan ikke behandling opprettes. Kontakt brukerstøtte for å rapportere feilen."
            )
        }
    }

    private fun hentVerge(vergeIdent: String?): Verge? {
        val verge: Verge? = if (vergeIdent != null) {
            val aktør = personidentService.hentAktør(vergeIdent)
            personopplysningerService.hentPersoninfoNavnOgAdresse(aktør).let {
                Verge(
                    vergetype = Vergetype.VERGE_FOR_BARN,
                    navn = it.navn!!,
                    personIdent = aktør.aktivFødselsnummer()
                )
            }
        } else {
            null
        }
        return verge
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TilbakekrevingService::class.java)
    }
}
