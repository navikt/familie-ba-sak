package no.nav.familie.ba.sak.arbeidsfordeling

import no.nav.familie.ba.sak.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.ADRESSEBESKYTTELSEGRADERING
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ArbeidsfordelingService(private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
                              private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                              private val oppgaveService: OppgaveService,
                              private val loggService: LoggService,
                              private val integrasjonClient: IntegrasjonClient,
                              private val personopplysningerService: PersonopplysningerService) {

    fun manueltOppdaterBehandlendeEnhet(behandling: Behandling, arbeidsfordelingsenhet: Arbeidsfordelingsenhet) {
        val aktivArbeidsfordelingPåBehandling =
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id)
                ?: throw Feil("Finner ikke tilknyttet arbeidsfordelingsenhet på behandling ${behandling.id}")


        arbeidsfordelingPåBehandlingRepository.save(
                aktivArbeidsfordelingPåBehandling.copy(
                        behandlendeEnhetId = arbeidsfordelingsenhet.enhetId,
                        behandlendeEnhetNavn = arbeidsfordelingsenhet.enhetNavn,
                        manueltOverstyrt = true
                )
        )
    }

    fun settBehandlendeEnhet(behandling: Behandling, arbeidsfordelingsenhet: Arbeidsfordelingsenhet) {
        arbeidsfordelingPåBehandlingRepository.save(
                ArbeidsfordelingPåBehandling(behandlingId = behandling.id,
                                             behandlendeEnhetId = arbeidsfordelingsenhet.enhetId,
                                             behandlendeEnhetNavn = arbeidsfordelingsenhet.enhetNavn)
        )
    }

    fun fastsettBehandlendeEnhet(behandling: Behandling,
                                 manuellOppdatering: Boolean) {
        val arbeidsfordelingsenhet = hentArbeidsfordelingsenhet(behandling)

        val aktivArbeidsfordelingPåBehandling =
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id)

        val (lagretArbeidsfordelingPåBehandling, endret) = when (aktivArbeidsfordelingPåBehandling) {
            null -> {
                val arbeidsfordelingPåBehandling = ArbeidsfordelingPåBehandling(behandlingId = behandling.id,
                                                                                behandlendeEnhetId = arbeidsfordelingsenhet.enhetId,
                                                                                behandlendeEnhetNavn = arbeidsfordelingsenhet.enhetNavn)
                arbeidsfordelingPåBehandlingRepository.save(arbeidsfordelingPåBehandling)
                Pair(arbeidsfordelingPåBehandling, true)
            }
            else -> {
                if ((!aktivArbeidsfordelingPåBehandling.manueltOverstyrt || manuellOppdatering) &&
                    (aktivArbeidsfordelingPåBehandling.behandlendeEnhetId != arbeidsfordelingsenhet.enhetId)) {

                    loggService.opprettBehandlendeEnhetEndret(behandling,
                                                              aktivArbeidsfordelingPåBehandling.behandlendeEnhetNavn,
                                                              arbeidsfordelingsenhet.enhetNavn)
                    aktivArbeidsfordelingPåBehandling.also {
                        it.behandlendeEnhetId = arbeidsfordelingsenhet.enhetId
                        it.behandlendeEnhetNavn = arbeidsfordelingsenhet.enhetNavn
                    }
                    arbeidsfordelingPåBehandlingRepository.save(aktivArbeidsfordelingPåBehandling)
                    Pair(aktivArbeidsfordelingPåBehandling, true)
                } else {
                    Pair(aktivArbeidsfordelingPåBehandling, false)
                }
            }
        }

        if (endret) {
            logger.info("Fastsetter behandlende enhet på behandling ${behandling.id}: $lagretArbeidsfordelingPåBehandling")

            oppgaveService.hentOppgaverSomIkkeErFerdigstilt(behandling).forEach { dbOppgave ->
                val oppgave = oppgaveService.hentOppgave(dbOppgave.gsakId.toLong())

                if (oppgave.tildeltEnhetsnr != lagretArbeidsfordelingPåBehandling.behandlendeEnhetId) {
                    logger.info("Oppdaterer enhet fra ${oppgave.tildeltEnhetsnr} til ${lagretArbeidsfordelingPåBehandling.behandlendeEnhetId} på oppgave ${oppgave.id}")
                    oppgaveService.oppdaterOppgave(oppgave.copy(
                            tildeltEnhetsnr = lagretArbeidsfordelingPåBehandling.behandlendeEnhetId
                    ))
                }
            }
        }
    }

    fun hentAbeidsfordelingPåBehandling(behandlingId: Long): ArbeidsfordelingPåBehandling {
        return arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandlingId)
               ?: error("Finner ikke tilknyttet arbeidsfordeling på behandling med id $behandlingId")
    }

    fun hentArbeidsfordelingsenhet(behandling: Behandling): Arbeidsfordelingsenhet {
        val søker = identMedAdressebeskyttelse(behandling.fagsak.hentAktivIdent().ident)

        val personinfoliste = when (val personopplysningGrunnlag =
                personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)) {
            null -> listOf(søker)
            else -> personopplysningGrunnlag.barna.map { barn ->
                identMedAdressebeskyttelse(barn.personIdent.ident)
            }.plus(søker)
        }

        val identMedStrengeste = finnPersonMedStrengesteAdressebeskyttelse(personinfoliste)

        return integrasjonClient.hentBehandlendeEnhet(identMedStrengeste ?: søker.ident).firstOrNull()
               ?: throw Feil(message = "Fant flere eller ingen enheter på behandling.")
    }

    private fun identMedAdressebeskyttelse(ident: String) = IdentMedAdressebeskyttelse(
            ident = ident,
            adressebeskyttelsegradering = personopplysningerService.hentPersoninfoMedRelasjoner(ident).adressebeskyttelseGradering)

    data class IdentMedAdressebeskyttelse(
            val ident: String,
            val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING?
    )

    companion object {

        val logger = LoggerFactory.getLogger(this::class.java)
    }
}