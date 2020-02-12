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
            val behandling = opprettNyBehandlingPåFagsak(fagsak,
                                                         nyBehandling.journalpostID,
                                                         nyBehandling.behandlingType,
                                                         randomSaksnummer())
            lagreSøkerOgBarnIPersonopplysningsgrunnlaget(nyBehandling, behandling)
        } else {
            throw Exception("Kan ikke lagre ny behandling. Fagsaken har en aktiv behandling som ikke er iverksatt.")
        }

        return fagsak
    }

    @Transactional
    fun opprettEllerOppdaterBehandlingFraHendelse(nyBehandling: NyBehandling): Fagsak {
        val fagsak = hentEllerOpprettFagsakForPersonIdent(nyBehandling.fødselsnummer)

        val aktivBehandling = hentBehandlingHvisEksisterer(fagsak.id)

        if (aktivBehandling == null || aktivBehandling.status == BehandlingStatus.IVERKSATT) {
            val behandling = opprettNyBehandlingPåFagsak(fagsak,
                                                         nyBehandling.journalpostID,
                                                         nyBehandling.behandlingType,
                                                         randomSaksnummer())
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

    fun hentEllerOpprettFagsakForPersonIdent(fødselsnummer: String): Fagsak {
        val personIdent = PersonIdent(fødselsnummer)

        val fagsak = fagsakService.hentFagsakForPersonident(personIdent) ?: run {
            val nyFagsak = Fagsak(null, AktørId("1"), personIdent)
            fagsakService.lagreFagsak(nyFagsak)
            nyFagsak
        }

        return fagsak
    }

    fun opprettNyBehandlingPåFagsak(fagsak: Fagsak,
                                    journalpostID: String?,
                                    behandlingType: BehandlingType,
                                    saksnummer: String): Behandling {
        val behandling =
                Behandling(fagsak = fagsak, journalpostID = journalpostID, type = behandlingType, saksnummer = saksnummer)
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

    private fun lagreBarnPåEksisterendePersonopplysningsgrunnlag(barnasFødselsnummer: Array<String>,
                                                                 personopplysningGrunnlag: PersonopplysningGrunnlag) {
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

        if (aktivVedtak != null && aktivVedtak.id != vedtak.id) {
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

        val vedtak = Vedtak(
                behandling = behandling,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                vedtaksdato = LocalDate.now(),
                resultat = nyttVedtak.resultat
        )

        if (nyttVedtak.resultat == VedtakResultat.AVSLÅTT) {
            vedtak.stønadBrevMarkdown = Result.runCatching { dokGenService.hentStønadBrevMarkdown(vedtak) }
                    .fold(
                            onSuccess = { it },
                            onFailure = { e ->
                                return Ressurs.failure("Klart ikke å opprette vedtak på grunn av feil fra dokumentgenerering.",
                                                       e)
                            }
                    )

        }

        lagreVedtak(vedtak)

        return fagsakService.hentRestFagsak(fagsakId)
    }


    @Transactional
    fun oppdaterAktivVedtakMedBeregning(fagsakId: Long, nyBeregning: NyBeregning)
            : Ressurs<RestFagsak> {
        if (nyBeregning.barnasBeregning == null || nyBeregning.barnasBeregning.isEmpty()) {
            return Ressurs.failure("Barnas beregning er null eller tømt")
        }

        val behandling = hentBehandlingHvisEksisterer(fagsakId)
        if (behandling == null) {
            return Ressurs.failure("Fant ikke behandling på fagsak $fagsakId")
        }

        val vedtak = hentAktivVedtakForBehandling(behandling.id)
        if (vedtak == null) {
            return Ressurs.failure("Fant ikke aktiv vedtak på fagsak $fagsakId, behandling ${behandling.id}")
        }

        if (vedtak.resultat == VedtakResultat.AVSLÅTT) {
            return Ressurs.failure("Kan ikke legge beregning til avslag vedtak")
        }

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(vedtak.behandling.id)

        nyBeregning.barnasBeregning.map {
            val barn =
                    personRepository.findByPersonIdentAndPersonopplysningGrunnlag(PersonIdent(it.fødselsnummer),
                                                                                  personopplysningGrunnlag?.id)
            if (barn == null) {
                throw RuntimeException("Barnet du prøver å registrere på vedtaket er ikke tilknyttet behandlingen.")
            }

            if (it.stønadFom.isBefore(barn.fødselsdato)) {
                throw RuntimeException("Ugyldig fra og med dato for ${barn.fødselsdato}")
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

        vedtak.stønadBrevMarkdown = Result.runCatching { dokGenService.hentStønadBrevMarkdown(vedtak) }
                .fold(
                        onSuccess = { it },
                        onFailure = { e ->
                            return Ressurs.failure("Klart ikke å opprette vedtak på grunn av feil fra dokumentgenerering.",
                                                   e)
                        }
                )

        lagreVedtak(vedtak)

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
