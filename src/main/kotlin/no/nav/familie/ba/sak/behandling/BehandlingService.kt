package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.*
import no.nav.familie.ba.sak.behandling.domene.vedtak.*
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.mottak.NyBehandling
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence

@Service
class BehandlingService(private val behandlingRepository: BehandlingRepository,
                        private val vedtakRepository: VedtakRepository,
                        private val vedtakBarnRepository: VedtakBarnRepository,
                        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                        private val personRepository: PersonRepository,
                        private val dokGenService: DokGenService,
                        private val fagsakService: FagsakService,
                        private val integrasjonTjeneste: IntegrasjonTjeneste) {

    @Transactional
    fun opprettBehandling(nyBehandling: NyBehandling): Fagsak {
        val fagsak = hentEllerOpprettFagsakForPersonIdent(nyBehandling.fødselsnummer)

        val aktivBehandling = hentBehandlingHvisEksisterer(fagsak.id)

        if (aktivBehandling == null || aktivBehandling.status == BehandlingStatus.IVERKSATT) {
            val behandling = opprettNyBehandlingPåFagsak(fagsak, nyBehandling.journalpostID, nyBehandling.behandlingType, randomSaksnummer())
            lagreSøkerOgBarnIPersonopplysningsgrunnlaget(nyBehandling, behandling)
        } else {
            throw Exception("Kan ikke lagre ny behandling. Fagsaken har en aktiv behandling som ikke er iverksatt.")
        }

        return fagsak
    }

    @Transactional
    fun opprettBehandling(
            personIdent: PersonIdent,
            behandlingFraFagsak: (Fagsak)->Behandling,
            personopplysningsGrunnlagFraBehandlingId : (Long?)->PersonopplysningGrunnlag ): Fagsak {

        val fagsak = hentEllerOpprettFagsak(personIdent);
        val behandling = behandlingFraFagsak(fagsak)

        lagreBehandling(behandling)

        personopplysningGrunnlagRepository.save(personopplysningsGrunnlagFraBehandlingId(behandling.id))

        return fagsak
    }

    @Transactional
    fun opprettEllerOppdaterBehandlingFraHendelse(nyBehandling: NyBehandling): Fagsak {
        val fagsak = hentEllerOpprettFagsakForPersonIdent(nyBehandling.fødselsnummer)

        val aktivBehandling = hentBehandlingHvisEksisterer(fagsak.id)

        if (aktivBehandling == null || aktivBehandling.status == BehandlingStatus.IVERKSATT) {
            val behandling = opprettNyBehandlingPåFagsak(fagsak, nyBehandling.journalpostID, nyBehandling.behandlingType, randomSaksnummer())
            lagreSøkerOgBarnIPersonopplysningsgrunnlaget(nyBehandling, behandling)
        } else if (aktivBehandling.status == BehandlingStatus.OPPRETTET || aktivBehandling.status == BehandlingStatus.UNDER_BEHANDLING) {
            val grunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(aktivBehandling.id)
            lagreBarnPåEksisterendePersonopplysningsgrunnlag(nyBehandling.barnasFødselsnummer, grunnlag!!)
            aktivBehandling.status = BehandlingStatus.OPPRETTET
            behandlingRepository.save(aktivBehandling)
        } else {
            throw Exception("Kan ikke lagre ny behandling. Fagsaken er ferdig behandlet og sendt til iverksetting.")
        }

        return fagsak
    }

    fun hentEllerOpprettFagsakForPersonIdent(fødselsnummer: String): Fagsak =
        hentEllerOpprettFagsak(PersonIdent(fødselsnummer))

    private fun hentEllerOpprettFagsak(personIdent: PersonIdent): Fagsak =
        fagsakService.hentFagsakForPersonident(personIdent) ?: opprettFagsak(personIdent)

    private fun opprettFagsak(personIdent: PersonIdent): Fagsak {
        val nyFagsak = Fagsak(null, AktørId("1"), personIdent)
        fagsakService.lagreFagsak(nyFagsak)
        return nyFagsak
    }


    fun opprettNyBehandlingPåFagsak(fagsak: Fagsak, journalpostID: String?, behandlingType: BehandlingType, saksnummer: String): Behandling {
        val behandling = Behandling(fagsak = fagsak, journalpostID = journalpostID, type = behandlingType, saksnummer = saksnummer)
        lagreBehandling(behandling)
        return behandling
    }

    private fun lagreSøkerOgBarnIPersonopplysningsgrunnlaget(nyBehandling: NyBehandling, behandling: Behandling) {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandling.id)

        personopplysningGrunnlag.leggTilPerson(Person(
                personIdent = behandling.fagsak.personIdent,
                type = PersonType.SØKER,
                personopplysningGrunnlag = personopplysningGrunnlag,
                fødselsdato = integrasjonTjeneste.hentPersoninfoFor(nyBehandling.fødselsnummer)?.fødselsdato
        ))

        lagreBarnPåEksisterendePersonopplysningsgrunnlag(nyBehandling.barnasFødselsnummer, personopplysningGrunnlag)

        personopplysningGrunnlag.aktiv = true
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
    }

    private fun lagreBarnPåEksisterendePersonopplysningsgrunnlag(barnasFødselsnummer: Array<String>, personopplysningGrunnlag: PersonopplysningGrunnlag) {
        barnasFødselsnummer.map { nyttBarn ->
            if (personopplysningGrunnlag.barna.none { eksisterendeBarn -> eksisterendeBarn.personIdent.ident == nyttBarn }) {
                personopplysningGrunnlag.leggTilPerson(Person(
                        personIdent = PersonIdent(nyttBarn),
                        type = PersonType.BARN,
                        personopplysningGrunnlag = personopplysningGrunnlag,
                        fødselsdato = integrasjonTjeneste.hentPersoninfoFor(nyttBarn)?.fødselsdato
                ))
            }
        }

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
    }

    fun hentBehandlingHvisEksisterer(fagsakId: Long?): Behandling? {
        return behandlingRepository.findByFagsakAndAktiv(fagsakId)
    }

    fun hentVedtakHvisEksisterer(behandlingId: Long?): Vedtak? {
        return vedtakRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun hentBehandling(behandlingId: Long?): Behandling? {
        return behandlingRepository.finnBehandling(behandlingId)
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

    fun hentAktivVedtakForBehandling(behandlingId: Long?): Vedtak? {
        return vedtakRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun hentVedtak(vedtakId: Long): Vedtak? {
        return vedtakRepository.getOne(vedtakId)
    }

    fun hentBarnForVedtak(vedtakId: Long?): List<VedtakBarn> {
        return vedtakBarnRepository.finnBarnBeregningForVedtak(vedtakId)
    }

    fun oppdaterStatusPåBehandling(behandlingId: Long?, status: BehandlingStatus) {
        when (val behandling = hentBehandling(behandlingId)) {
            null -> throw Exception("Feilet ved oppdatering av status på behandling. Fant ikke behandling med id $behandlingId")
            else -> {
                if (status == BehandlingStatus.IVERKSATT) {
                    val fagsak = behandling.fagsak
                    fagsak.status = FagsakStatus.LØPENDE
                    fagsakService.lagreFagsak(fagsak)
                }

                behandling.status = status
                behandlingRepository.save(behandling)
            }
        }
    }

    fun lagreVedtak(vedtak: Vedtak) {
        val aktivVedtak = hentVedtakHvisEksisterer(vedtak.behandling.id)

        if (aktivVedtak != null) {
            aktivVedtak.aktiv = false
            vedtakRepository.save(aktivVedtak)
        }

        vedtakRepository.save(vedtak)
    }

    fun nyttVedtakForAktivBehandling(fagsakId: Long,
                                     nyttVedtak: NyttVedtak,
                                     ansvarligSaksbehandler: String): Ressurs<RestFagsak> {
        val behandling = hentBehandlingHvisEksisterer(fagsakId)
                         ?: throw Error("Fant ikke behandling på fagsak $fagsakId")

        if (nyttVedtak.barnasBeregning.isEmpty()) {
            throw Error("Fant ingen barn på behandlingen og kan derfor ikke opprette nytt vedtak")
        }

        val vedtak = Vedtak(
                behandling = behandling,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                vedtaksdato = LocalDate.now(),
                resultat = nyttVedtak.resultat
        )

        vedtak.stønadBrevMarkdown = Result.runCatching { dokGenService.hentStønadBrevMarkdown(vedtak) }
                .fold(
                        onSuccess = { it },
                        onFailure = { e ->
                            return Ressurs.failure("Klart ikke å opprette vedtak på grunn av feil fra dokumentgenerering.",
                                                   e)
                        }
                )

        lagreVedtak(vedtak)

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
        nyttVedtak.barnasBeregning.map {
            val barn =
                    personRepository.findByPersonIdentAndPersonopplysningGrunnlag(PersonIdent(it.fødselsnummer),
                                                                                  personopplysningGrunnlag?.id)
                    ?: return Ressurs.failure("Barnet du prøver å registrere på vedtaket er ikke tilknyttet behandlingen.")

            if (it.stønadFom.isBefore(barn.fødselsdato)) {
                return Ressurs.failure("Ugyldig fra og med dato", Exception("Ugyldig fra og med dato for ${barn.fødselsdato}"))
            }

            vedtakBarnRepository.save(
                    VedtakBarn(
                            barn = barn,
                            vedtak = vedtak,
                            beløp = it.beløp,
                            stønadFom = it.stønadFom,
                            stønadTom = barn.fødselsdato?.plusYears(18)!!
                    )
            )
        }

        return fagsakService.hentRestFagsak(fagsakId)
    }

    fun hentHtmlVedtakForBehandling(behandlingId: Long): Ressurs<String> {
        val vedtak = hentAktivVedtakForBehandling(behandlingId)
                     ?: return Ressurs.failure("Behandling ikke funnet")
        val html = Result.runCatching { dokGenService.lagHtmlFraMarkdown(vedtak.stønadBrevMarkdown) }
                .fold(
                        onSuccess = { it },
                        onFailure = { e ->
                            return Ressurs.failure("Klarte ikke å hent vedtaksbrev", e)
                        }
                )

        return Ressurs.success(html)
    }

    internal fun hentPdfForVedtak(vedtak: Vedtak): ByteArray {
        return Result.runCatching {
            LOG.debug("henter stønadsbrevMarkdown fra behandlingsVedtak")
            val markdown = vedtak.stønadBrevMarkdown
            LOG.debug("kaller lagPdfFraMarkdown med stønadsbrevMarkdown")
            dokGenService.lagPdfFraMarkdown(markdown)
        }
                .fold(
                        onSuccess = { it },
                        onFailure = {
                            throw Exception("Klarte ikke å hente PDF for vedtak med id ${vedtak.id}", it)
                        }
                )
    }

    @Transactional
    fun opphørBehandlingOgVedtak(saksbehandler: String,
                                 saksnummer : String,
                                 gjeldendeBehandlingsId: Long,
                                 nyBehandlingType: BehandlingType,
                                 postProsessor: (Vedtak) -> Unit): Ressurs<Vedtak> {

        val gjeldendeVedtak = vedtakRepository.findByBehandlingAndAktiv(gjeldendeBehandlingsId)
                              ?: return Ressurs.failure("Fant ikke aktivt vedtak tilknyttet behandling ${gjeldendeBehandlingsId}")

        val gjeldendeVedtakPerson = vedtakBarnRepository.finnBarnBeregningForVedtak(gjeldendeVedtak.id)
        if (gjeldendeVedtakPerson.isEmpty()) {
            return Ressurs.failure("Fant ikke vedtak barn tilknyttet behandling ${gjeldendeBehandlingsId} og vedtak ${gjeldendeVedtak.id}")
        }

        val gjeldendeBehandling = gjeldendeVedtak.behandling;
        if(!gjeldendeBehandling.aktiv) {
            return Ressurs.failure("Aktivt vedtak er tilknyttet behandling ${gjeldendeBehandlingsId} som IKKE er aktivt")
        }

        /// TODO Her følger det med samme journalpost_id som forrige behandling. Er det riktig?
        val nyBehandling = Behandling(fagsak = gjeldendeBehandling.fagsak,
                                      journalpostID = gjeldendeBehandling.journalpostID,
                                      saksnummer = saksnummer,
                                      type = nyBehandlingType)

        // Må flushe denne til databasen for å sørge å opprettholde unikhet på (fagsakid,aktiv)
        behandlingRepository.saveAndFlush(gjeldendeBehandling.also { it.aktiv=false })
        behandlingRepository.save(nyBehandling)

        val nyttVedtak = Vedtak(
                ansvarligSaksbehandler = saksbehandler,
                behandling = nyBehandling,
                resultat = VedtakResultat.OPPHØRT,
                vedtaksdato = LocalDate.now())

        // Trenger ikke flush her fordi det kreves unikhet på (behandlingid,aktiv) og det er ny behandlingsid
        vedtakRepository.save(gjeldendeVedtak.also { it.aktiv=false })
        vedtakRepository.save(nyttVedtak)

        /// TODO For opphør er beløpet det samme, men perioden fra nå til gammel til-og-med-dato. Er det riktig?
        val nyeVedtakPerson = gjeldendeVedtakPerson
                .map { p ->
                    VedtakBarn(vedtak = nyttVedtak,
                               barn = p.barn,
                               beløp = p.beløp,
                               stønadFom = LocalDate.now(),
                               stønadTom = p.stønadTom)
                }


        vedtakBarnRepository.saveAll(nyeVedtakPerson)

        postProsessor(nyttVedtak)

        return Ressurs.success(nyttVedtak)

    }

    private fun randomSaksnummer(): String {
        return ThreadLocalRandom.current()
                .ints(STRING_LENGTH.toLong(), 0, charPool.size)
                .asSequence()
                .map(charPool::get)
                .joinToString("")
    }

    companion object {
        val charPool: List<Char> = ('A'..'Z') + ('0'..'9')
        const val STRING_LENGTH = 10
        val LOG: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
