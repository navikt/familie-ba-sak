package no.nav.familie.ba.sak.behandling.autobrev

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.task.SendAutobrev6og18ÅrTask
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class Autobrev6og18ÅrService(
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val behandlingService: BehandlingService,
) {

    fun opprettOmregningsoppgaveForBarnIBrytingsAlder(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO) {

        //TODO: Skal det feile eller skal den bare avslutte om det ikke er en aktiv behandling?
        val behandling = behandlingService.hentAktivForFagsak(autobrev6og18ÅrDTO.fagsakId) ?: error("Fant ikke aktiv behandling")

        // Finne ut om fagsak er løpende -> hvis nei, avslutt uten feil
        if (behandling.fagsak.status != FagsakStatus.LØPENDE) {
            LOG.info("Fagsak ${behandling.fagsak.id} har ikke status løpende, og derfor prosesseres den ikke videre.")
            return
        }

        // TODO: Finn ut om brev for denne omregning allerede blitt sendt. (idempotent)
        // behandling av type OMREGNING_6ÅR|18ÅR og Vedtaks dato inneværende måned -> allerede sendt.
        if(brevAlleredeSendt(autobrev6og18ÅrDTO)) {
            return
        }

        // Finne ut om fagsak har behandling som ikke er fullført -> hvis ja, feile task og logge feil og øke metrikk
        //  hvis tasken forsøker for siste gang -> opprett oppgave for å håndtere videre manuelt
        // TODO: trenger vi denne sjekken når den allerede gjøres i behnadlingsservice l.75
        if (behandling.status != BehandlingStatus.AVSLUTTET) {
            error("Kan ikke opprette ny behandling for fagsak ${behandling.fagsak.id} ettersom den allerede har en åpen behanding.")
        }

        if(barnMedAngittAlderInneværendeMånedEksisterer(behandlingId = behandling.id, alder = autobrev6og18ÅrDTO.alder)) {
            LOG.warn("Fagsak ${behandling.fagsak.id} har ikke noe barn med ålder ${autobrev6og18ÅrDTO.alder} ")
        }

        if(autobrev6og18ÅrDTO.alder == Alder.atten.år &&
           barnUnder18årInneværendeMånedEksisterer(behandlingId = behandling.id)) {
        }

        val behandlingÅrsak = if(autobrev6og18ÅrDTO.alder == 6) {
            BehandlingÅrsak.OMREGNING_6ÅR
        } else {
            BehandlingÅrsak.OMREGNING_18ÅR
        }

        opprettNyOmregningsBehandling(behandling = behandling,
                                      behandlingÅrsak = behandlingÅrsak)

        SendAutobrev6og18ÅrTask.LOG.info("SendAutobrev6og18ÅrTask for fagsak ${behandling.fagsak.id}")
    }

    private fun brevAlleredeSendt(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO): Boolean {
        // TODO: Det trenges en modellavklaring, hvordan persisterer vi informasjon om at denne omregningen gjelder inneværende måned:
        // På vedtaket, beregning.opprettetDato eller skal modellen utvides?
        return false
        //behandlingService.hentBehandlinger(autobrev6og18ÅrDTO.fagsakId)
                //.filter { it.opprettetÅrsak == BehandlingÅrsak.OMREGNING_6ÅR }
                //.any {innværendemåned}
    }

    private fun barnMedAngittAlderInneværendeMånedEksisterer(behandlingId: Long, alder: Int): Boolean =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.personer
                    ?.any { it.type == PersonType.BARN && it.fyllerAntallÅrInneværendeMåned(alder) } ?: false

    private fun barnUnder18årInneværendeMånedEksisterer(behandlingId: Long): Boolean =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.personer
                    ?.any { it.type == PersonType.BARN && it.erYngreEnnInneværendeMåned(Alder.atten.år) } ?: false

    private fun opprettNyOmregningsBehandling(behandling: Behandling, behandlingÅrsak: BehandlingÅrsak) {
        val nybehandling = NyBehandling(søkersIdent = behandling.fagsak.hentAktivIdent().ident,
                                        behandlingType = BehandlingType.REVURDERING,
                                        kategori = behandling.kategori,
                                        underkategori = behandling.underkategori,
                                        behandlingÅrsak = behandlingÅrsak,
                                        skalBehandlesAutomatisk = true
        )

        // Oppretter behandling, men her gjenstår å få kopiert persongrunnlag og vilkårsvurdering og
        // fullført behandling. Bør kanskje legges til stegservice?
        // TODO: Automatiskbehandling må troligen bli utvudet for å kunne håndtere omregnings-behandlinger automatisk.
        val revurdering = behandlingService.opprettBehandling(nybehandling)
    }

    fun Person.fyllerAntallÅrInneværendeMåned(år: Int): Boolean {
        return this.fødselsdato.isAfter(LocalDate.now().minusYears(år.toLong()).førsteDagIInneværendeMåned()) &&
               this.fødselsdato.isBefore(LocalDate.now().minusYears(år.toLong()).sisteDagIMåned())
    }

    fun Person.erYngreEnnInneværendeMåned(år: Int): Boolean {
        return this.fødselsdato.isAfter(LocalDate.now().minusYears(år.toLong()).sisteDagIMåned())
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

enum class Alder(val år: Int) {
    seks(år = 6),
    atten(år = 18)
}