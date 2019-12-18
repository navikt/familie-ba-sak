package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.*
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BehandlingslagerService(
        private val fagsakRepository: FagsakRepository,
        private val behandlingRepository: BehandlingRepository,
        private val behandlingVedtakRepository: BehandlingVedtakRepository,
        private val behandlingVedtakBarnRepository: BehandlingVedtakBarnRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val personRepository: PersonRepository,
        private val dokGenService: DokGenService
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

    fun hentVedtakForBehandling(behandlingId: Long?): List<BehandlingVedtak?> {
        return behandlingVedtakRepository.finnVedtakForBehandling(behandlingId)
    }

    fun hentBarnBeregningForVedtak(behandlingVedtakId: Long?): List<BehandlingVedtakBarn?> {
        return behandlingVedtakBarnRepository.finnBarnBeregningForVedtak(behandlingVedtakId)
    }

    fun lagreBehandlingVedtak(behandlingVedtak: BehandlingVedtak) {
        val aktivBehandlingVedtak = hentBehandlingVedtakHvisEksisterer(behandlingVedtak.behandling.id)

        if (aktivBehandlingVedtak != null) {
            aktivBehandlingVedtak.aktiv = false
            behandlingVedtakRepository.save(aktivBehandlingVedtak)
        }

        behandlingVedtakRepository.save(behandlingVedtak)
    }

    fun nyttVedtakForAktivBehandling(fagsakId: Long, nyttVedtak: NyttVedtak, ansvarligSaksbehandler: String): Ressurs<BehandlingVedtak> {
        val behandling = hentBehandlingHvisEksisterer(fagsakId)
                ?: throw Error("Fant ikke behandling på fagsak $fagsakId")

        val tidligsteStønadFom: LocalDate? = nyttVedtak.barnasBeregning.map { barnBeregning -> barnBeregning.stønadFom }.min()
        val yngsteBarn: LocalDate? = LocalDate.now() // Her må vi ha fødselsdato for barn

        if (tidligsteStønadFom == null || yngsteBarn == null) {
            throw Error("Fant ikke barn i listen over beregninger")
        } else {
            val behandlingVedtak = BehandlingVedtak(
                    behandling = behandling,
                    ansvarligSaksbehandler = ansvarligSaksbehandler,
                    vedtaksdato = LocalDate.now(),
                    stønadFom = tidligsteStønadFom,
                    stønadTom = yngsteBarn.plusYears(18),
                    stønadBrevMarkdown = "" // TODO hent markdown fra dokgen
            )

            behandlingVedtak.stønadBrevMarkdown = Result.runCatching { dokGenService.hentStønadBrevMarkdown(behandlingVedtak) }
                    .fold(
                            onSuccess = { it },
                            onFailure = { e -> return Ressurs.failure("Klart ikke å generer markdown", e) }
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
                                stønadTom = LocalDate.now() // TODO plus 18 år fra fødselsdato
                        )
                )
            }

            return Ressurs.success(data = behandlingVedtak)
        }
    }

    fun hentHtmlVedtakForBehandling(behandlingId: Long): Ressurs<String> {
        val behandlingVedtak = behandlingVedtakRepository.finnBehandlingVedtak(behandlingId)
                ?: return Ressurs.failure("Vedtak ikke funnet")
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