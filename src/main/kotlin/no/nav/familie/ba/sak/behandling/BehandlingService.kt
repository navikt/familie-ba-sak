package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.*
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.økonomi.ØkonomiKlient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class BehandlingService(
        private val fagsakRepository: FagsakRepository,
        private val behandlingRepository: BehandlingRepository,
        private val behandlingVedtakRepository: BehandlingVedtakRepository,
        private val behandlingVedtakBarnRepository: BehandlingVedtakBarnRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val personRepository: PersonRepository,
        private val dokGenService: DokGenService,
        private val fagsakService: FagsakService
) {
    fun nyBehandling(fødselsnummer: String,
                     barnasFødselsnummer: Array<String>,
                     behandlingType: BehandlingType,
                     journalpostID: String,
                     saksnummer: String): Behandling {
        //final var søkerAktørId = oppslagTjeneste.hentAktørId(fødselsnummer);

        val fagsak = Fagsak(null, AktørId("1"), PersonIdent(fødselsnummer))
        fagsakRepository.save(fagsak)
        val behandling = Behandling(id = null, fagsak = fagsak, journalpostID = journalpostID, type = behandlingType, saksnummer = saksnummer)
        lagreBehandling(behandling)

        return behandling
    }

    fun hentBehandlingHvisEksisterer(fagsakId: Long?): Behandling? {
        return behandlingRepository.findByFagsakAndAktiv(fagsakId)
    }

    fun hentBehandlingVedtakHvisEksisterer(behandlingId: Long?): BehandlingVedtak? {
        return behandlingVedtakRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun hentBehandlinger(fagsakId: Long?): List<Behandling?> {
        return behandlingRepository.finnBehandlinger(fagsakId)
    }

    fun lagreBehandling(behandling: Behandling) {
        val aktivBehandling = hentBehandlingHvisEksisterer(behandling.fagsak.id)

        if (aktivBehandling != null) {
            aktivBehandling.aktiv = false
            behandlingRepository.save(aktivBehandling)
        }

        behandlingRepository.save(behandling)
    }

    fun hentAktivVedtakForBehandling(behandlingId: Long?): BehandlingVedtak? {
        return behandlingVedtakRepository.findByBehandlingAndAktiv(behandlingId);
    }

    fun hentVedtakForBehandling(behandlingId: Long?): List<BehandlingVedtak?> {
        return behandlingVedtakRepository.finnVedtakForBehandling(behandlingId)
    }

    fun hentBarnBeregningForVedtak(behandlingVedtakId: Long?): List<BehandlingVedtakBarn> {
        return behandlingVedtakBarnRepository.finnBarnBeregningForVedtak(behandlingVedtakId)
    }

    fun oppdatertStatusPåBehandlingVedtak(behandlingVedtak: BehandlingVedtak, status: BehandlingVedtakStatus) {
        behandlingVedtak.status = status
        behandlingVedtakRepository.save(behandlingVedtak)
    }

    fun lagreBehandlingVedtak(behandlingVedtak: BehandlingVedtak) {
        val aktivBehandlingVedtak = hentBehandlingVedtakHvisEksisterer(behandlingVedtak.behandling.id)

        if (aktivBehandlingVedtak != null) {
            aktivBehandlingVedtak.aktiv = false
            behandlingVedtakRepository.save(aktivBehandlingVedtak)
        }

        behandlingVedtakRepository.save(behandlingVedtak)
    }

    fun nyttVedtakForAktivBehandling(fagsakId: Long, nyttVedtak: NyttVedtak, ansvarligSaksbehandler: String): Ressurs<RestFagsak> {
        val behandling = hentBehandlingHvisEksisterer(fagsakId)
                ?: throw Error("Fant ikke behandling på fagsak $fagsakId")

        if (nyttVedtak.barnasBeregning.isEmpty()) {
            throw Error("Fant ingen barn på behandlingen og kan derfor ikke opprette nytt vedtak")
        }

        val behandlingVedtak = BehandlingVedtak(
                behandling = behandling,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                vedtaksdato = LocalDate.now()
        )

        behandlingVedtak.stønadBrevMarkdown = Result.runCatching { dokGenService.hentStønadBrevMarkdown(behandlingVedtak) }
                .fold(
                        onSuccess = { it },
                        onFailure = { e -> return Ressurs.failure("Klart ikke å opprette vedtak på grunn av feil fra dokumentgenerering.", e) }
                )


        lagreBehandlingVedtak(behandlingVedtak)

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
        nyttVedtak.barnasBeregning.map {
            val barn = personRepository.findByPersonIdentAndPersonopplysningGrunnlag(PersonIdent(it.fødselsnummer), personopplysningGrunnlagId = personopplysningGrunnlag?.id)
                    ?: return Ressurs.failure("Barnet du prøver å registrere på vedtaket er ikke tilknyttet behandlingen.")

            behandlingVedtakBarnRepository.save(
                    BehandlingVedtakBarn(
                            barn = barn,
                            behandlingVedtak = behandlingVedtak,
                            beløp = it.beløp,
                            stønadFom = it.stønadFom,
                            stønadTom = barn.fødselsdato?.plusYears(18)!!
                    )
            )
        }

        return fagsakService.hentRestFagsak(fagsakId)
    }

    fun hentHtmlVedtakForBehandling(behandlingId: Long): Ressurs<String> {
        val behandlingVedtak = hentAktivVedtakForBehandling(behandlingId)
                ?: return Ressurs.failure("Behandling ikke funnet")
        val html = Result.runCatching { dokGenService.lagHtmlFraMarkdown(behandlingVedtak.stønadBrevMarkdown) }
                .fold(
                        onSuccess = { it },
                        onFailure = { e ->
                            return Ressurs.failure("Klarte ikke å hent vedtaksbrev", e)
                        }
                )

        return Ressurs.success(html)
    }
}