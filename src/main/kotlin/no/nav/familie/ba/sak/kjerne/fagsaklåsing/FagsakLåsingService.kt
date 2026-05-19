package no.nav.familie.ba.sak.kjerne.fagsaklåsing

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.hentSaksbehandlerNavn
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.AvsluttSakRequest
import no.nav.familie.kontrakter.felles.dokarkiv.DokarkivBruker
import no.nav.familie.kontrakter.felles.dokarkiv.GjenåpneSakRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class FagsakLåsingService(
    private val fagsakRepository: FagsakRepository,
    private val `fagsakLåsingRepository`: FagsakLåsingRepository,
    private val integrasjonKlient: IntegrasjonKlient,
    private val persongrunnlagService: PersongrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val featureToggleService: FeatureToggleService,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun låsFagsak(fagsakId: Long) {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_LÅSE_FAGSAK)) {
            logger.info("Toggle for låsing av fagsak er av, hopper ut")
            return
        }

        val fagsak =
            fagsakRepository.finnFagsak(fagsakId)
                ?: throw Feil("Fant ikke fagsak $fagsakId")

        if (fagsak.status != FagsakStatus.AVSLUTTET) {
            logger.info("Status for fagsak $fagsakId er ${fagsak.status}, hopper ut")
            return
        }

        if (behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(fagsakId)) {
            logger.info("Fagsak $fagsakId har åpen behandling, hopper ut")
            return
        }

        val aktivLåsForFagsak = finnAktivLåsForFagsak(fagsak.id)
        if (aktivLåsForFagsak?.hendelse == FagsakLåsHendelse.LÅST) {
            throw Feil("Fagsak $fagsakId har allerede aktiv låsing")
        }

        val fagsakBleLåstOppForUnder30DagerSiden = aktivLåsForFagsak?.opprettetTidspunkt?.isAfter(LocalDateTime.now().minusDays(30)) == true
        if (fagsakBleLåstOppForUnder30DagerSiden) {
            logger.info("Fagsak $fagsakId ble låst opp for under 30 dager siden, hopper ut")
            return
        }

        val barnPåFagsak =
            persongrunnlagService.hentSøkerOgBarnPåFagsak(fagsakId)?.filter { it.type == PersonType.BARN }
                ?: throw Feil("Fant ingen barn på fagsak $fagsakId")

        val yngsteBarnsFødselsdato =
            barnPåFagsak.maxOfOrNull { it.fødselsdato }
                ?: throw Feil("Fant ikke yngste barns fødselsdato på fagsak $fagsakId")

        val låsedato = yngsteBarnsFødselsdato.plusYears(19).atStartOfDay()
        if (LocalDateTime.now() < låsedato) {
            logger.info("Fagsak skal ikke låses før $låsedato, hopper ut")
            return
        }

        lagreOgDeaktiverGammel(
            FagsakLåsing(
                fagsak = fagsak,
                tidspunkt = låsedato,
                hendelse = FagsakLåsHendelse.LÅST,
                begrunnelse = "Automatisk låst iht. arkivloven fordi yngste barn fylte 18 år ${yngsteBarnsFødselsdato.plusYears(18)}",
                aktiv = true,
            ),
        )

        oppdaterStatus(fagsak, FagsakStatus.LÅST)

        val arbeidsfordeling = arbeidsfordelingService.hentArbeidsfordelingsenhetPåIdenter(fagsak, barnPåFagsak.map { it.aktør.aktivFødselsnummer() }, null)

        integrasjonKlient.avsluttSak(
            AvsluttSakRequest(
                tema = Tema.BAR,
                fagsakId = fagsakId.toString(),
                fagsaksystem = Fagsystem.BA,
                bruker = DokarkivBruker(BrukerIdType.FNR, fagsak.aktør.aktivFødselsnummer()),
                opprettetDato = fagsak.opprettetTidspunkt,
                avsluttetDato = låsedato,
                administrativEnhet = arbeidsfordeling.enhetId,
            ),
        )
        logger.info("Fagsak $fagsakId er låst og meldt til Joark")
    }

    @Transactional
    fun låsOppFagsak(
        fagsakId: Long,
        begrunnelseForÅLåseOppFagsak: String,
    ): Fagsak {
        val fagsak = fagsakRepository.finnFagsak(fagsakId) ?: throw Feil("Finner ikke fagsak med id $fagsakId")

        if (fagsak.status != FagsakStatus.LÅST) {
            throw FunksjonellFeil("Fagsaken må ha status LÅST for å kunne låses opp. Nåværende status: ${fagsak.status}")
        }

        if (begrunnelseForÅLåseOppFagsak.isBlank()) {
            throw FunksjonellFeil("Begrunnelse kan ikke være tom")
        }

        lagreOgDeaktiverGammel(
            FagsakLåsing(
                fagsak = fagsak,
                tidspunkt = LocalDateTime.now(),
                hendelse = FagsakLåsHendelse.LÅST_OPP,
                begrunnelse = begrunnelseForÅLåseOppFagsak,
                aktiv = true,
            ),
        )

        oppdaterStatus(fagsak, FagsakStatus.AVSLUTTET)

        integrasjonKlient.gjenåpneSakIDokarkiv(
            GjenåpneSakRequest(
                tema = Tema.BAR,
                fagsakId = fagsakId.toString(),
                fagsaksystem = Fagsystem.BA,
                bruker =
                    DokarkivBruker(
                        idType = BrukerIdType.FNR,
                        id = fagsak.aktør.aktivFødselsnummer(),
                    ),
            ),
        )

        return fagsak
    }

    fun finnAktivLåsForFagsak(fagsakId: Long) = fagsakLåsingRepository.finnAktivLåsForFagsak(fagsakId = fagsakId)

    private fun lagreOgDeaktiverGammel(fagsakLåsing: FagsakLåsing): FagsakLåsing {
        val aktivFagsakLåsing = fagsakLåsingRepository.finnAktivLåsForFagsak(fagsakLåsing.fagsak.id)

        if (aktivFagsakLåsing != null && aktivFagsakLåsing.id != fagsakLåsing.id) {
            fagsakLåsingRepository.saveAndFlush(aktivFagsakLåsing.also { it.aktiv = false })
        }

        return fagsakLåsingRepository.save(fagsakLåsing)
    }

    private fun oppdaterStatus(
        fagsak: Fagsak,
        nyStatus: FagsakStatus,
    ): Fagsak {
        logger.info("${hentSaksbehandlerNavn()} endrer status på fagsak ${fagsak.id} fra ${fagsak.status} til $nyStatus")
        fagsak.status = nyStatus

        return lagre(fagsak)
    }

    private fun lagre(fagsak: Fagsak): Fagsak {
        logger.info("${hentSaksbehandlerNavn()} oppretter fagsak $fagsak")
        return fagsakRepository.save(fagsak).also { saksstatistikkEventPublisher.publiserSaksstatistikk(it.id) }
    }
}
