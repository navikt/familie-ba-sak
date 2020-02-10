package no.nav.familie.ba.sak.mottak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class MottakController(private val oidcUtil: OIDCUtil,
                       private val behandlingService: BehandlingService,
                       private val fagsakService: FagsakService,
                       private val integrasjonTjeneste: IntegrasjonTjeneste,
                       private val featureToggleService: FeatureToggleService) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping(path = ["/behandling/opprett"])
    fun opprettBehandling(@RequestBody nyBehandling: NyBehandling): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = try {
            oidcUtil.getClaim("preferred_username") ?: "VL"
        } catch (e: JwtTokenValidatorException) {
            "VL"
        }

        if (featureToggleService.isEnabled("familie-ba-sak.lag-oppgave")){
            logger.info("FeatureToggle for lag-oppgave er skrudd på")
        } else {
            logger.info("FeatureToggle for lag-oppgave er skrudd av")
        }
        if (featureToggleService.isEnabled("familie-ba-sak.distribuer-vedtaksbrev")){
            logger.info("FeatureToggle for distribuer-vedtaksbrev er skrudd på")
        } else {
            logger.info("FeatureToggle for distribuer-vedtaksbrev er skrudd av")
        }

        logger.info("{} oppretter ny behandling", saksbehandlerId)

        logger.info("{} oppretter ny behandling", saksbehandlerId)

        return Result.runCatching { behandlingService.opprettBehandling(
                PersonIdent(nyBehandling.fødselsnummer),
                lagBehandlingFraFagsakFunc(nyBehandling),
                lagPersonopplysningGrunnlagFraBehandlingsIdFunc(nyBehandling)) }
                .fold(
                        onFailure = {
                            logger.info("Opprettelse av behandling feilet", it)
                            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Ressurs.failure(it.message, it))
                        },
                        onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = it.id)) }
                )
    }

    @PostMapping(path = ["/behandling/opprettfrahendelse"])
    fun opprettEllerOppdaterBehandlingFraHendelse(@RequestBody nyBehandling: NyBehandling): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = "VL"

        if (featureToggleService.isEnabled("familie-ba-sak.lag-oppgave")){
            logger.info("FeatureToggle for lag-oppgave er skrudd på")
        } else {
            logger.info("FeatureToggle for lag-oppgave er skrudd av")
        }

        logger.info("{} oppretter ny behandling fra hendelse", saksbehandlerId)

        return Result.runCatching { behandlingService.opprettEllerOppdaterBehandlingFraHendelse(nyBehandling) }
                .fold(
                        onFailure = {
                            logger.info("Opprettelse av behandling fra hendelse feilet", it)
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(Ressurs.failure(it.message, it))
                        },
                        onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = it.id)) }
                )
    }

    private fun lagPersonopplysningGrunnlagFraBehandlingsIdFunc(nyBehandling: NyBehandling): (Long?)-> PersonopplysningGrunnlag {

        return fun(behandlingId:Long?) : PersonopplysningGrunnlag {
            val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId)

            val søker = Person(
                    personIdent = PersonIdent(nyBehandling.fødselsnummer),
                    type = PersonType.SØKER,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    fødselsdato = integrasjonTjeneste.hentPersoninfoFor(nyBehandling.fødselsnummer)?.fødselsdato
            )
            personopplysningGrunnlag.leggTilPerson(søker)

            nyBehandling.barnasFødselsnummer.map {
                personopplysningGrunnlag.leggTilPerson(Person(
                        personIdent = PersonIdent(it),
                        type = PersonType.BARN,
                        personopplysningGrunnlag = personopplysningGrunnlag,
                        fødselsdato = integrasjonTjeneste.hentPersoninfoFor(it)?.fødselsdato
                ))
            }
            personopplysningGrunnlag.aktiv = true
            return personopplysningGrunnlag
        }
    }

    private fun lagBehandlingFraFagsakFunc(nyBehandling: NyBehandling) : (Fagsak)->Behandling {
        return fun(fagsak:Fagsak) : Behandling {
            return Behandling(
                    fagsak = fagsak,
                    type = nyBehandling.behandlingType,
                    journalpostID = nyBehandling.journalpostID,
                    // Saksnummer byttes ut med gsaksnummer senere
                    saksnummer = ThreadLocalRandom.current()
                            .ints(BehandlingService.STRING_LENGTH.toLong(), 0, charPool.size)
                            .asSequence()
                            .map(charPool::get)
                            .joinToString("")
            )
        }
    }

    private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')


}

class NyBehandling(val fødselsnummer: String,
                   val barnasFødselsnummer: Array<String>,
                   val behandlingType: BehandlingType,
                   val journalpostID: String?) {
}
