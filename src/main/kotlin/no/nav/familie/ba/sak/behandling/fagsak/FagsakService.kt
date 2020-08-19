package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.*
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.TilkjentYtelseUtils
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.FAMILIERELASJONSROLLE
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.Ident
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
        private val fagsakPersonRepository: FagsakPersonRepository,
        private val persongrunnlagService: PersongrunnlagService,
        private val personRepository: PersonRepository,
        private val behandlingRepository: BehandlingRepository,
        private val behandlingResultatService: BehandlingResultatService,
        private val vedtakRepository: VedtakRepository,
        private val totrinnskontrollRepository: TotrinnskontrollRepository,
        private val tilkjentYtelseRepository: TilkjentYtelseRepository,
        private val personopplysningerService: PersonopplysningerService) {

    @Transactional
    fun hentEllerOpprettFagsak(fagsakRequest: FagsakRequest): Ressurs<RestFagsak> {
        val personIdent = when {
            fagsakRequest.personIdent !== null -> PersonIdent(fagsakRequest.personIdent)
            fagsakRequest.aktørId !== null -> personopplysningerService.hentAktivPersonIdent(Ident(fagsakRequest.aktørId))
            else -> throw Feil(
                    "Hverken aktørid eller personident er satt på fagsak-requesten. Klarer ikke opprette eller hente fagsak.",
                    "Fagsak er forsøkt opprettet uten ident. Dette er en systemfeil, vennligst ta kontakt med systemansvarlig.",
                    HttpStatus.BAD_REQUEST
            )
        }

        val fagsak = hentEllerOpprettFagsak(personIdent)

        return hentRestFagsak(fagsakId = fagsak.id)
    }

    @Transactional
    fun hentEllerOpprettFagsak(personIdent: PersonIdent): Fagsak {
        val identer = personopplysningerService.hentIdenter(Ident(personIdent.ident)).map { PersonIdent(it.ident) }.toSet()
        var fagsak = fagsakPersonRepository.finnFagsak(personIdenter = identer)
        if (fagsak == null) {
            fagsak = Fagsak().also {
                it.søkerIdenter = setOf(FagsakPerson(personIdent = personIdent, fagsak = it))
                lagre(it)
            }
        } else if (fagsak.søkerIdenter.none { fagsakPerson -> fagsakPerson.personIdent == personIdent }) {
            fagsak.also {
                it.søkerIdenter += FagsakPerson(personIdent = personIdent, fagsak = it)
                lagre(it)
            }
        }
        return fagsak
    }

    @Transactional
    fun lagre(fagsak: Fagsak): Fagsak {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter fagsak $fagsak")
        return fagsakRepository.save(fagsak)
    }

    fun hentRestFagsak(fagsakId: Long): Ressurs<RestFagsak> {
        val fagsak = fagsakRepository.finnFagsak(fagsakId)
        val restBehandlinger: List<RestBehandling> = lagRestBehandlinger(fagsak)
        return Ressurs.success(data = fagsak.toRestFagsak(restBehandlinger))
    }

    fun hentRestFagsakForPerson(personIdent: PersonIdent): Ressurs<RestFagsak?> {
        val fagsak = fagsakRepository.finnFagsakForPersonIdent(personIdent)
        if (fagsak != null) {
            val restBehandlinger: List<RestBehandling> = lagRestBehandlinger(fagsak)
            return Ressurs.success(data = fagsak.toRestFagsak(restBehandlinger))
        }
        return Ressurs.success(data = null)
    }

    private fun lagRestBehandlinger(fagsak: Fagsak): List<RestBehandling> {
        val behandlinger = behandlingRepository.finnBehandlinger(fagsak.id)

        return behandlinger.map { behandling ->
            val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)

            val restVedtakForBehandling = vedtakRepository.finnVedtakForBehandling(behandling.id).map { vedtak ->
                val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandling.id))
                val restVedtakPerson = lagRestVedtakPerson(andelerTilkjentYtelse, personopplysningGrunnlag)
                vedtak.toRestVedtak(restVedtakPerson)
            }

            val totrinnskontroll = totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)

            val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingOptional(behandlingId = behandling.id)

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
                    underkategori = behandling.underkategori,
                    endretAv = behandling.endretAv,
                    totrinnskontroll = totrinnskontroll?.toRestTotrinnskontroll(),
                    beregningOversikt = if (tilkjentYtelse == null || personopplysningGrunnlag == null) emptyList() else
                        TilkjentYtelseUtils.hentBeregningOversikt(
                                tilkjentYtelseForBehandling = tilkjentYtelse,
                                personopplysningGrunnlag = personopplysningGrunnlag)
            )
        }
    }

    fun oppdaterStatus(fagsak: Fagsak, nyStatus: FagsakStatus) {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer status på fagsak ${fagsak.id} fra ${fagsak.status} til $nyStatus")
        fagsak.status = nyStatus

        lagre(fagsak)
    }


    fun hentEllerOpprettFagsakForPersonIdent(fødselsnummer: String): Fagsak {
        val personIdent = PersonIdent(fødselsnummer)
        return hentEllerOpprettFagsak(personIdent)
    }

    fun hent(personIdent: PersonIdent): Fagsak? {
        val identer = personopplysningerService.hentIdenter(Ident(personIdent.ident)).map { PersonIdent(it.ident) }.toSet()
        return fagsakPersonRepository.finnFagsak(identer)
    }

    fun hentLøpendeFagsaker(): List<Fagsak> {
        return fagsakRepository.finnLøpendeFagsaker()
    }

    fun hentFagsakDeltager(personIdent: String): List<RestFagsakDeltager> {
        val personer = personRepository.findByPersonIdent(PersonIdent(personIdent))
        val personInfo = runCatching {
            personopplysningerService.hentPersoninfoFor(personIdent)
        }.fold(
                onSuccess = { it },
                onFailure = {
                    val clientError = it as? HttpClientErrorException?
                    if (clientError != null && clientError.statusCode == HttpStatus.NOT_FOUND) {
                        throw clientError
                    } else {
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
                    val søkerInfo = if (behandling.fagsak.hentAktivIdent().ident == personIdent) personInfo else
                        runCatching {
                            personopplysningerService.hentPersoninfoFor(behandling.fagsak.hentAktivIdent().ident)
                        }.fold(
                                onSuccess = { it },
                                onFailure = {
                                    throw IllegalStateException("Feil ved henting av person fra TPS/PDL", it)
                                }

                        )

                    assosierteFagsakDeltagerMap[behandling.fagsak.id] = RestFagsakDeltager(
                            navn = søkerInfo.navn,
                            ident = behandling.fagsak.hentAktivIdent().ident,
                            rolle = FagsakDeltagerRolle.FORELDER,
                            kjønn = søkerInfo.kjønn,
                            fagsakId = behandling.fagsak.id
                    )
                }
            }
        }

        //The given person and its parents may be included in the result, no matter whether they have a case.
        val assosierteFagsakDeltager = assosierteFagsakDeltagerMap.values.toMutableList()
        val erBarn = Period.between(personInfo.fødselsdato, LocalDate.now()).years < 18

        if (assosierteFagsakDeltager.find { it.ident == personIdent } == null) {
            assosierteFagsakDeltager.add(RestFagsakDeltager(
                    navn = personInfo.navn,
                    ident = personIdent,
                    //we set the role to unknown when the person is not a child because the person may not have a child
                    rolle = if (erBarn) FagsakDeltagerRolle.BARN else FagsakDeltagerRolle.UKJENT,
                    kjønn = personInfo.kjønn
            ))
        }

        if (erBarn) {
            personInfo.familierelasjoner.filter {
                it.relasjonsrolle == FAMILIERELASJONSROLLE.FAR || it.relasjonsrolle == FAMILIERELASJONSROLLE.MOR
                || it.relasjonsrolle == FAMILIERELASJONSROLLE.MEDMOR
            }.forEach {
                if (assosierteFagsakDeltager.find { d -> d.ident == it.personIdent.id } == null) {
                    val forelderInfo = runCatching {
                        personopplysningerService.hentPersoninfoFor(it.personIdent.id)
                    }.fold(
                            onSuccess = { it },
                            onFailure = {
                                throw IllegalStateException("Feil ved henting av person fra TPS/PDL", it)
                            }

                    )

                    val fagsak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent(it.personIdent.id))

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
