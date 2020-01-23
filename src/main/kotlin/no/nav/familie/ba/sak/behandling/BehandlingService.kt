package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.*
import no.nav.familie.ba.sak.behandling.domene.vedtak.*
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.mottak.NyBehandling
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence

@Service
class BehandlingService(
        private val behandlingRepository: BehandlingRepository,
        private val behandlingVedtakRepository: BehandlingVedtakRepository,
        private val behandlingVedtakBarnRepository: BehandlingVedtakBarnRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val personRepository: PersonRepository,
        private val dokGenService: DokGenService,
        private val fagsakService: FagsakService,
        private val integrasjonTjeneste: IntegrasjonTjeneste
) {
    fun nyBehandling(fødselsnummer: String,
                     behandlingType: BehandlingType,
                     journalpostID: String?,
                     saksnummer: String): Behandling {
        //final var søkerAktørId = oppslagTjeneste.hentAktørId(fødselsnummer);

        val personIdent = PersonIdent(fødselsnummer)
        val fagsak = when (val it = fagsakService.hentFagsakForPersonident(personIdent)) {
            null -> Fagsak(null, AktørId("1"), personIdent)
            else -> it
        }
        fagsakService.lagreFagsak(fagsak)

        val behandling = Behandling(fagsak = fagsak, journalpostID = journalpostID, type = behandlingType, saksnummer = saksnummer)
        lagreBehandling(behandling)

        return behandling
    }

    val STRING_LENGTH = 10
    private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')

    @Transactional
    fun opprettBehandling(nyBehandling: NyBehandling): Fagsak {
        // val søkerAktørId = integrasjonTjeneste.hentAktørId(nyBehandling.fødselsnummer);

        val behandling = nyBehandling(
                fødselsnummer = nyBehandling.fødselsnummer,
                behandlingType = nyBehandling.behandlingType,
                journalpostID = nyBehandling.journalpostID,
                // Saksnummer byttes ut med gsaksnummer senere
                saksnummer = ThreadLocalRandom.current()
                        .ints(STRING_LENGTH.toLong(), 0, charPool.size)
                        .asSequence()
                        .map(charPool::get)
                        .joinToString(""))

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandling.id)

        val søker = Person(
                personIdent = behandling.fagsak.personIdent,
                type = PersonType.SØKER,
                personopplysningGrunnlag = personopplysningGrunnlag,
                fødselsdato = integrasjonTjeneste.hentPersoninfoFor(nyBehandling.fødselsnummer)?.fødselsdato
        )
        personopplysningGrunnlag.leggTilPerson(søker)

        if (nyBehandling.barnasFødselsnummer.isEmpty()) {
            throw Exception("Kan ikke lage en behandling uten barn")
        }
        nyBehandling.barnasFødselsnummer.map {
            personopplysningGrunnlag.leggTilPerson(Person(
                    personIdent = PersonIdent(it),
                    type = PersonType.BARN,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    fødselsdato = integrasjonTjeneste.hentPersoninfoFor(it)?.fødselsdato
            ))
        }
        personopplysningGrunnlag.setAktiv(true)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        return behandling.fagsak
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
            val aktivBehandlingVedtak = hentBehandlingVedtakHvisEksisterer(aktivBehandling.id)
            if (aktivBehandlingVedtak?.status != BehandlingVedtakStatus.IVERKSATT) {
                throw IllegalStateException("Den aktive behandlingen er ikke iverksatt. Kan ikke opprette ny behandling.")
            }

            aktivBehandling.aktiv = false
            behandlingRepository.save(aktivBehandling)
        }

        behandlingRepository.save(behandling)
    }

    fun hentAktivVedtakForBehandling(behandlingId: Long?): BehandlingVedtak? {
        return behandlingVedtakRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun hentBehandlingVedtak(behandlingId: Long): BehandlingVedtak? {
        return behandlingVedtakRepository.getOne(behandlingId)
    }

    fun hentBarnBeregningForVedtak(behandlingVedtakId: Long?): List<BehandlingVedtakBarn> {
        return behandlingVedtakBarnRepository.finnBarnBeregningForVedtak(behandlingVedtakId)
    }

    fun oppdatertStatusPåBehandlingVedtak(behandlingVedtak: BehandlingVedtak, status: BehandlingVedtakStatus) {
        behandlingVedtak.status = status
        behandlingVedtakRepository.save(behandlingVedtak)
    }

    fun oppdatertStatusPåBehandlingVedtak(behandlingVedtakId: Long, status: BehandlingVedtakStatus) {
        when (val behandlingVedtak = hentBehandlingVedtak(behandlingVedtakId)) {
            null -> throw Exception("Feilet ved oppdatering av status på vedtak. Fant ikke vedtak med id $behandlingVedtakId")
            else -> {
                behandlingVedtak.status = status
                behandlingVedtakRepository.save(behandlingVedtak)
            }
        }
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

    internal fun hentPdfForBehandlingVedtak(behandlingVedtakId: Long): ByteArray {
        val behandlingVedtak = behandlingVedtakRepository.findByIdOrNull(behandlingVedtakId)
        return Result.runCatching { dokGenService.lagPdfFraMarkdown(behandlingVedtak?.stønadBrevMarkdown!!) }
            .fold(
                onSuccess = { it },
                onFailure = { e ->
                    throw Exception("Klarte ikke å hente PDF for vedtak med id $behandlingVedtakId")
                }
            )
    }
}