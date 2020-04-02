package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.*
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FagsakService(
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
        private val fagsakRepository: FagsakRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val personRepository: PersonRepository,
        private val behandlingRepository: BehandlingRepository,
        private val behandlingResultatService: BehandlingResultatService,
        private val vedtakRepository: VedtakRepository,
        private val integrasjonClient: IntegrasjonClient) {

    @Transactional
    fun hentEllerOpprettFagsak(fagsakRequest: FagsakRequest): Ressurs<RestFagsak> {
        val fagsak = fagsakRepository.finnFagsakForPersonIdent(personIdent = PersonIdent(fagsakRequest.personIdent))
                     ?: Fagsak(aktørId = integrasjonClient.hentAktørId(fagsakRequest.personIdent),
                               personIdent = PersonIdent(fagsakRequest.personIdent)).also { lagre(it) }

        return hentRestFagsak(fagsakId = fagsak.id)
    }

    @Transactional
    fun lagre(fagsak: Fagsak): Fagsak {
        LOG.info("${SikkerhetContext.hentSaksbehandler()} oppretter fagsak $fagsak")
        return fagsakRepository.save(fagsak)
    }

    fun hentRestFagsak(fagsakId: Long): Ressurs<RestFagsak> {
        val fagsak = fagsakRepository.finnFagsak(fagsakId)

        val behandlinger = behandlingRepository.finnBehandlinger(fagsak.id)

        val restBehandlinger: List<RestBehandling> = behandlinger.map { behandling ->
            val personopplysningGrunnlag =
                    behandling.id.let { it1 -> personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it1) }

            val restVedtakForBehandling = vedtakRepository.finnVedtakForBehandling(behandling.id).map { vedtak ->
                val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)
                val restVedtakBarn = lagRestVedtakBarn(andelerTilkjentYtelse, personopplysningGrunnlag)
                vedtak.toRestVedtak(restVedtakBarn)
            }

            val begrunnelse =
                    behandlingResultatService.hentAktivForBehandling(behandlingId = behandling.id)?.periodeResultater?.first()?.vilkårResultater?.first()?.begrunnelse
                    ?: error("Kunne ikke finne begrunnelse på behandling med ID: ${behandling.id}")

            RestBehandling(
                    aktiv = behandling.aktiv,
                    behandlingId = behandling.id,
                    vedtakForBehandling = restVedtakForBehandling,
                    personer = personopplysningGrunnlag?.personer?.map { it.toRestPerson() } ?: emptyList(),
                    type = behandling.type,
                    status = behandling.status,
                    steg = behandling.steg,
                    periodeResultater = behandlingResultatService.hentAktivForBehandling(behandling.id)
                                                ?.periodeResultater?.map { it.tilRestPeriodeResultat() } ?: emptyList(),
                    opprettetTidspunkt = behandling.opprettetTidspunkt,
                    kategori = behandling.kategori,
                    underkategori = behandling.underkategori,
                    begrunnelse = begrunnelse
            )
        }

        return Ressurs.success(data = fagsak.toRestFagsak(restBehandlinger))
    }

    fun oppdaterStatus(fagsak: Fagsak, nyStatus: FagsakStatus) {
        LOG.info("${SikkerhetContext.hentSaksbehandler()} endrer status på fagsak ${fagsak.id} fra ${fagsak.status} til $nyStatus")
        fagsak.status = nyStatus

        lagre(fagsak)
    }

    private fun opprettFagsak(personIdent: PersonIdent): Fagsak {
        val aktørId = integrasjonClient.hentAktørId(personIdent.ident)
        val nyFagsak =
                Fagsak(aktørId = aktørId, personIdent = personIdent)
        return lagre(nyFagsak)
    }

    fun hentEllerOpprettFagsakForPersonIdent(fødselsnummer: String): Fagsak =
            hentEllerOpprettFagsak(PersonIdent(fødselsnummer))

    private fun hentEllerOpprettFagsak(personIdent: PersonIdent): Fagsak =
            hent(personIdent) ?: opprettFagsak(personIdent)

    fun hent(personIdent: PersonIdent): Fagsak? {
        return fagsakRepository.finnFagsakForPersonIdent(personIdent)
    }

    fun hentLøpendeFagsaker(): List<Fagsak> {
        return fagsakRepository.finnLøpendeFagsaker()
    }

    fun hentFagsaker(personIdent: String): RestFagsakSøk {
        val personer = personRepository.findByPersonIdent(PersonIdent(personIdent))
        val personInfo = runCatching {
            integrasjonClient.hentPersoninfoFor(personIdent)
        }.fold(
                onSuccess = { it },
                onFailure = {
                    throw IllegalStateException("Feil ved henting av person fra TPS/PDL", it)
                }

        )

        val assosierteFagsaker = mutableMapOf<Long, RestFunnetFagsak>()

        personer.map {
            val behandling = behandlingRepository.finnBehandling(it.personopplysningGrunnlag.behandlingId)
            assosierteFagsaker[behandling.fagsak.id] = RestFunnetFagsak(
                    behandling.fagsak.id,
                    it.type
            )
        }

        return RestFagsakSøk(personIdent,
                             personInfo.navn ?: "",
                             personInfo.kjønn ?: Kjønn.UKJENT,
                             assosierteFagsaker.values.toList())
    }

    companion object {
        val LOG = LoggerFactory.getLogger(FagsakService::class.java)
    }
}
