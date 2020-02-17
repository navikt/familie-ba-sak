package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.*
import no.nav.familie.ba.sak.behandling.domene.vedtak.*
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.mottak.NyBehandling
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.task.OpprettBehandleSakOppgaveForNyBehandlingTask
import no.nav.familie.ba.sak.økonomi.OppdragId
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
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
                        private val vilkårService: VilkårService,
                        private val integrasjonTjeneste: IntegrasjonTjeneste,
                        private val featureToggleService: FeatureToggleService) {

    @Transactional
    fun opprettBehandling(nyBehandling: NyBehandling): Fagsak {
        val fagsak = hentEllerOpprettFagsakForPersonIdent(nyBehandling.fødselsnummer)

        val aktivBehandling = hentBehandlingHvisEksisterer(fagsak.id)

        if (aktivBehandling == null || aktivBehandling.status == BehandlingStatus.IVERKSATT) {
            val behandling = opprettNyBehandlingPåFagsak(fagsak,
                                                         nyBehandling.journalpostID,
                                                         nyBehandling.behandlingType,
                                                         randomSaksnummer(),
                                                         nyBehandling.kategori,
                                                         nyBehandling.underkategori)
            lagreSøkerOgBarnIPersonopplysningsgrunnlaget(nyBehandling, behandling)
            if (featureToggleService.isEnabled("familie-ba-sak.lag-oppgave")) {
                Task.nyTask(OpprettBehandleSakOppgaveForNyBehandlingTask.TASK_STEP_TYPE, behandling.id.toString())
            } else {
                LOG.info("Lag opprettOppgaveTask er skrudd av i miljø")
            }
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
                                                         randomSaksnummer(),
                                                         nyBehandling.kategori,
                                                         nyBehandling.underkategori)

            lagreSøkerOgBarnIPersonopplysningsgrunnlaget(nyBehandling, behandling)
            if (featureToggleService.isEnabled("familie-ba-sak.lag-oppgave")) {
                Task.nyTask(OpprettBehandleSakOppgaveForNyBehandlingTask.TASK_STEP_TYPE, behandling.id.toString())
            } else {
                LOG.info("Lag opprettOppgaveTask er skrudd av i miljø")
            }
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

    @Transactional
    fun opphørVedtak(saksbehandler: String,
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
        if (!gjeldendeBehandling.aktiv) {
            return Ressurs.failure("Aktivt vedtak er tilknyttet behandling ${gjeldendeBehandlingsId} som IKKE er aktivt")
        }

        /// TODO Her følger det med samme journalpost_id som forrige behandling. Er det riktig?
        val nyBehandling = Behandling(fagsak = gjeldendeBehandling.fagsak,
                                      journalpostID = gjeldendeBehandling.journalpostID,
                                      saksnummer = randomSaksnummer(),
                                      type = nyBehandlingType,
                                      kategori = gjeldendeBehandling.kategori,
                                      underkategori = gjeldendeBehandling.underkategori)

        // Må flushe denne til databasen for å sørge å opprettholde unikhet på (fagsakid,aktiv)
        behandlingRepository.saveAndFlush(gjeldendeBehandling.also { it.aktiv = false })
        behandlingRepository.save(nyBehandling)

        val nyttVedtak = Vedtak(
                ansvarligSaksbehandler = saksbehandler,
                behandling = nyBehandling,
                resultat = VedtakResultat.OPPHØRT,
                vedtaksdato = LocalDate.now())

        // Trenger ikke flush her fordi det kreves unikhet på (behandlingid,aktiv) og det er ny behandlingsid
        vedtakRepository.save(gjeldendeVedtak.also { it.aktiv = false })
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


    fun hentEllerOpprettFagsakForPersonIdent(fødselsnummer: String): Fagsak =
            hentEllerOpprettFagsak(PersonIdent(fødselsnummer))

    private fun hentEllerOpprettFagsak(personIdent: PersonIdent): Fagsak =
            fagsakService.hentFagsakForPersonident(personIdent) ?: opprettFagsak(personIdent)

    private fun opprettFagsak(personIdent: PersonIdent): Fagsak {
        val nyFagsak = Fagsak(null, AktørId("1"), personIdent)
        fagsakService.lagreFagsak(nyFagsak)
        return nyFagsak
    }

    fun opprettNyBehandlingPåFagsak(fagsak: Fagsak,
                                    journalpostID: String?,
                                    behandlingType: BehandlingType,
                                    saksnummer: String,
                                    kategori: BehandlingKategori,
                                    underkategori: BehandlingUnderkategori): Behandling {
        val behandling =
                Behandling(fagsak = fagsak,
                           journalpostID = journalpostID,
                           type = behandlingType,
                           saksnummer = saksnummer,
                           kategori = kategori,
                           underkategori = underkategori)
        lagreNyOgDeaktiverGammelBehandling(behandling)
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

    fun hentAktiveBehandlingerForLøpendeFagsaker(): List<OppdragId> {
        return fagsakService.hentLøpendeFagsaker()
                .mapNotNull { fagsak -> hentBehandlingHvisEksisterer(fagsak.id) }
                .map { behandling ->
                    OppdragId(
                            hentSøker(behandling)!!.personIdent.ident,
                            behandling.id!!)
                }
    }

    fun hentBehandlinger(fagsakId: Long?): List<Behandling?> {
        return behandlingRepository.finnBehandlinger(fagsakId)
    }

    fun lagreBehandling(behandling: Behandling) {
        behandlingRepository.save(behandling)
    }

    fun lagreNyOgDeaktiverGammelBehandling(behandling: Behandling) {
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
                LOG.info("Endrer status på behandling $behandlingId fra ${behandling.status} til $status")
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

    @Transactional
    fun nyttVedtakForAktivBehandling(behandling: Behandling,
                                     personopplysningGrunnlag: PersonopplysningGrunnlag,
                                     nyttVedtak: NyttVedtak,
                                     ansvarligSaksbehandler: String): Ressurs<RestFagsak> {
        vilkårService.vurderVilkårOgLagResultat(personopplysningGrunnlag, nyttVedtak.samletVilkårResultat, behandling.id!!)

        val vedtak = Vedtak(
                behandling = behandling,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                vedtaksdato = LocalDate.now(),
                resultat = nyttVedtak.resultat,
                begrunnelse = nyttVedtak.begrunnelse
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

        return fagsakService.hentRestFagsak(behandling.fagsak.id)
    }


    @Transactional
    fun oppdaterAktivVedtakMedBeregning(vedtak: Vedtak,
                                        personopplysningGrunnlag: PersonopplysningGrunnlag,
                                        nyBeregning: NyBeregning)
            : Ressurs<RestFagsak> {
        nyBeregning.barnasBeregning.map {
            val barn =
                    personRepository.findByPersonIdentAndPersonopplysningGrunnlag(PersonIdent(it.fødselsnummer),
                                                                                  personopplysningGrunnlag.id)
                    ?: throw RuntimeException("Barnet du prøver å registrere på vedtaket er ikke tilknyttet behandlingen.")

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

        return fagsakService.hentRestFagsak(vedtak.behandling.fagsak.id)
    }

    fun hentHtmlVedtakForBehandling(behandlingId: Long): Ressurs<String> {
        val vedtak = hentAktivVedtakForBehandling(behandlingId)
                     ?: return Ressurs.failure("Behandling ikke funnet")
        val html = Result.runCatching { dokGenService.lagHtmlFraMarkdown(vedtak.resultat.toDokGenTemplate(), vedtak.stønadBrevMarkdown) }
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
            dokGenService.lagPdfFraMarkdown(vedtak.resultat.toDokGenTemplate(), markdown)
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

    private fun hentSøker(behandling: Behandling): Person? {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)!!.personer.find { person -> person.type == PersonType.SØKER }
    }

    companion object {
        val charPool: List<Char> = ('A'..'Z') + ('0'..'9')
        const val STRING_LENGTH = 10
        val LOG: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
