package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.*
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.FAMILIERELASJONSROLLE
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate
import java.time.Period

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
        val fagsak: Fagsak
        if (fagsakRequest.personIdent !== null) {
            fagsak = fagsakRepository.finnFagsakForPersonIdent(personIdent = PersonIdent(fagsakRequest.personIdent))
                    ?: Fagsak(aktørId = integrasjonClient.hentAktørId(fagsakRequest.personIdent),
                            personIdent = PersonIdent(fagsakRequest.personIdent)).also { lagre(it) }
        } else if (fagsakRequest.aktørId !== null) {
            var muligFagsak = fagsakRepository.finnFagsakForAktørId(aktørId = AktørId(fagsakRequest.aktørId))

            if (muligFagsak == null) {
                val personIdent = integrasjonClient.hentPersonIdent(fagsakRequest.aktørId)
                        ?: error("Kunne ikke hente fagsak. Finner ikke personident for gitt aktørid")
                muligFagsak = fagsakRepository.finnFagsakForPersonIdent(personIdent = personIdent)
                        ?: Fagsak(aktørId = AktørId(fagsakRequest.aktørId),
                                personIdent = personIdent
                        ).also { lagre(it) }
            }
            fagsak = muligFagsak
        } else {
            error("Hverken aktørid eller personident er satt på fagsak-requesten. Klarer ikke opprette eller hente fagsak")
        }
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
                    behandling.id.let { id -> personopplysningGrunnlagRepository.findByBehandlingAndAktiv(id) }

            val restVedtakForBehandling = vedtakRepository.finnVedtakForBehandling(behandling.id).map { vedtak ->
                val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)
                val restVedtakPerson = lagRestVedtakPerson(andelerTilkjentYtelse, personopplysningGrunnlag)
                vedtak.toRestVedtak(restVedtakPerson)
            }

            RestBehandling(
                    aktiv = behandling.aktiv,
                    behandlingId = behandling.id,
                    vedtakForBehandling = restVedtakForBehandling,
                    personer = personopplysningGrunnlag?.personer?.map { it.toRestPerson() } ?: emptyList(),
                    type = behandling.type,
                    status = behandling.status,
                    steg = behandling.steg,
                    personResultater = behandlingResultatService.hentAktivForBehandling(behandling.id)
                                                ?.personResultater?.map { it.tilRestPersonResultat() } ?: emptyList(),
                    samletResultat = behandlingResultatService.hentAktivForBehandling(behandling.id)?.hentSamletResultat()
                            ?: BehandlingResultatType.IKKE_VURDERT,
                    opprettetTidspunkt = behandling.opprettetTidspunkt,
                    kategori = behandling.kategori,
                    underkategori = behandling.underkategori
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

    fun hentFagsakDeltager(personIdent: String): List<RestFagsakDeltager> {
        val personer = personRepository.findByPersonIdent(PersonIdent(personIdent))
        val personInfo = runCatching {
            integrasjonClient.hentPersoninfoFor(personIdent)
        }.fold(
                onSuccess = { it },
                onFailure = {
                    val clientError= it as? HttpClientErrorException?
                    if(clientError!= null && clientError.statusCode == HttpStatus.NOT_FOUND){
                        throw clientError
                    }else{
                        throw IllegalStateException("Feil ved henting av person fra TPS/PDL", it)
                    }
                }

        )

        //We find all cases that either have the given person as applicant, or have it as a child
        val assosierteFagsakDeltagerMap = mutableMapOf<Long, RestFagsakDeltager>()

        personer.forEach {
            if (it.personopplysningGrunnlag.aktiv) {
                val behandling = behandlingRepository.finnBehandling(it.personopplysningGrunnlag.behandlingId)
                if (behandling.aktiv && !assosierteFagsakDeltagerMap.containsKey(behandling.fagsak.id)) {
                    //get applicant info from PDL. we assume that the applicant is always a person whose info is stored in PDL.
                    val søkerInfo = if (behandling.fagsak.personIdent.ident == personIdent) personInfo else
                        runCatching {
                            integrasjonClient.hentPersoninfoFor(behandling.fagsak.personIdent.ident)
                        }.fold(
                                onSuccess = { it },
                                onFailure = {
                                    throw IllegalStateException("Feil ved henting av person fra TPS/PDL", it)
                                }

                        )

                    assosierteFagsakDeltagerMap[behandling.fagsak.id] = RestFagsakDeltager(
                            navn = søkerInfo.navn,
                            ident = behandling.fagsak.personIdent.ident,
                            rolle = FagsakDeltagerRolle.FORELDER,
                            kjønn = søkerInfo.kjønn,
                            fagsakId = behandling.fagsak.id
                    )
                }
            }
        }

        //The given person and its parents may be included in the result, no matter whether they have a case.
        val assosierteFagsakDeltager= assosierteFagsakDeltagerMap.values.toMutableList()
        val erBarn= Period.between(personInfo.fødselsdato, LocalDate.now()).getYears()<18

        if(assosierteFagsakDeltager.find{it.ident== personIdent} == null){
            assosierteFagsakDeltager.add(RestFagsakDeltager(
                    navn = personInfo.navn,
                    ident = personIdent,
                    //we set the role to unknown when the person is not a child because the person may not have a child
                    rolle = if(erBarn) FagsakDeltagerRolle.BARN else FagsakDeltagerRolle.UKJENT,
                    kjønn = personInfo.kjønn
            ))
        }

        if(erBarn){
            personInfo.familierelasjoner.filter { it.relasjonsrolle== FAMILIERELASJONSROLLE.FAR || it.relasjonsrolle== FAMILIERELASJONSROLLE.MOR
                    || it.relasjonsrolle== FAMILIERELASJONSROLLE.MEDMOR }.forEach{
                if(assosierteFagsakDeltager.find({d-> d.ident == it.personIdent.id})== null){
                    val forelderInfo = runCatching {
                            integrasjonClient.hentPersoninfoFor(it.personIdent.id)
                        }.fold(
                                onSuccess = { it },
                                onFailure = {
                                    throw IllegalStateException("Feil ved henting av person fra TPS/PDL", it)
                                }

                        )

                    val fagsak= fagsakRepository.finnFagsakForPersonIdent(PersonIdent(it.personIdent.id))

                    assosierteFagsakDeltager.add(RestFagsakDeltager(
                            navn = forelderInfo.navn,
                            ident = it.personIdent.id,
                            rolle = FagsakDeltagerRolle.FORELDER,
                            kjønn = forelderInfo.kjønn,
                            fagsakId = fagsak?.id
                    ))
                }
            }
        }

        return assosierteFagsakDeltager
    }

    companion object {
        val LOG = LoggerFactory.getLogger(FagsakService::class.java)
    }
}
