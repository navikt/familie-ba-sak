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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class Autobrev6og18ÅrService(
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val behandlingService: BehandlingService,
) {

    fun opprettOmregningsoppgaveForBarnIBrytingsAlder(fagsakId: Long, alder: Int) {

        //TODO: Skal det feile eller skal den bare avslutte om det ikke er en aktiv behandling?
        val behandling = behandlingService.hentAktivForFagsak(fagsakId) ?: error("Fant ikke aktiv behandling")

        // Finne ut om fagsak er løpende -> hvis nei, avslutt uten feil
        if (behandling.fagsak.status != FagsakStatus.LØPENDE) {
            LOG.info("Fagsak ${behandling.fagsak.id} har ikke status løpende, og derfor prosesseres den ikke videre.")
            return
        }

        // Finne ut om fagsak har behandling som ikke er fullført -> hvis ja, feile task og logge feil og øke metrikk
        //  hvis tasken forsøker for siste gang -> opprett oppgave for å håndtere videre manuelt
        // TODO: trenger vi denne sjekken når den allerede gjøres i behnadlingsservice l.75
        if (behandling.status != BehandlingStatus.AVSLUTTET) {
            error("Kan ikke opprette ny behandling for fagsak ${behandling.fagsak.id} ettersom den allerede har en åpen behanding.")
        }

        when (alder) {
            Alder.seks.år -> opprettOmregningsoppgaveForBarnSeksÅr(behandling)
            Alder.atten.år -> opprettOmregningsoppgaveForBarnAttenÅr(behandling)
            else -> error("Alder ikke støttet $alder.")
        }

        SendAutobrev6og18ÅrTask.LOG.info("SendAutobrev6og18ÅrTask for fagsak ${behandling.fagsak.id}")
    }

    private fun opprettOmregningsoppgaveForBarnAttenÅr(behandling: Behandling) {

        val barnMedOppgittAlder = hentAlleBarnMedOppgittAlder(behandling, Alder.atten.år)
        if (barnMedOppgittAlder.isEmpty()) return

        opprettNyOmregningsBehandling(behandling)
    }

    private fun opprettOmregningsoppgaveForBarnSeksÅr(behandling: Behandling) {
        val barnMedOppgittAlder = hentAlleBarnMedOppgittAlder(behandling, Alder.seks.år)
        if (barnMedOppgittAlder.isEmpty()) error("Fant ingen barn som fyller ${Alder.seks.år} inneværende måned for behandling ${behandling.id}")

        opprettNyOmregningsBehandling(behandling)
    }

    private fun hentAlleBarnMedOppgittAlder(behandling: Behandling, alder: Int): List<Person> =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)?.personer
                    ?.filter { it.type == PersonType.BARN }
                    ?.filter { it.fyllerAntallÅrInneværendeMåned(alder) }
            ?: error("Fant ingen personer på behandling ${behandling.id}")

    private fun opprettNyOmregningsBehandling(behandling: Behandling) {
        val nybehandling = NyBehandling(søkersIdent = behandling.fagsak.hentAktivIdent().ident,
                                        behandlingType = BehandlingType.REVURDERING,
                                        kategori = behandling.kategori,
                                        underkategori = behandling.underkategori,
                                        behandlingÅrsak = BehandlingÅrsak.OMREGNING,
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

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

enum class Alder(val år: Int) {
    seks(år = 6),
    atten(år = 18)
}