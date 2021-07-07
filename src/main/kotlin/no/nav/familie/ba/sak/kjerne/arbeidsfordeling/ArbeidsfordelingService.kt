package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.norg2.Norg2RestClient
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ArbeidsfordelingService(private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
                              private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                              private val oppgaveService: OppgaveService,
                              private val loggService: LoggService,
                              private val norg2RestClient: Norg2RestClient,
                              private val integrasjonClient: IntegrasjonClient,
                              private val personopplysningerService: PersonopplysningerService,
private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher) {

    fun manueltOppdaterBehandlendeEnhet(behandling: Behandling, endreBehandlendeEnhet: RestEndreBehandlendeEnhet) {
        val aktivArbeidsfordelingPåBehandling =
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id)
                ?: throw Feil("Finner ikke tilknyttet arbeidsfordelingsenhet på behandling ${behandling.id}")

        val forrigeArbeidsfordelingsenhet = Arbeidsfordelingsenhet(enhetId = aktivArbeidsfordelingPåBehandling.behandlendeEnhetId,
                                                                   enhetNavn = aktivArbeidsfordelingPåBehandling.behandlendeEnhetNavn)

        val oppdatertArbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingRepository.save(
                aktivArbeidsfordelingPåBehandling.copy(
                        behandlendeEnhetId = endreBehandlendeEnhet.enhetId,
                        behandlendeEnhetNavn = norg2RestClient.hentEnhet(endreBehandlendeEnhet.enhetId).navn,
                        manueltOverstyrt = true
                )
        )

        postFastsattBehandlendeEnhet(
                behandling = behandling,
                forrigeArbeidsfordelingsenhet = forrigeArbeidsfordelingsenhet,
                oppdatertArbeidsfordelingPåBehandling = oppdatertArbeidsfordelingPåBehandling,
                manuellOppdatering = true,
                begrunnelse = endreBehandlendeEnhet.begrunnelse
        )
        saksstatistikkEventPublisher.publiserBehandlingsstatistikk(behandling.id)
    }

    fun fastsettBehandlendeEnhet(behandling: Behandling) {
        val arbeidsfordelingsenhet = hentArbeidsfordelingsenhet(behandling)

        val aktivArbeidsfordelingPåBehandling =
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id)

        val forrigeArbeidsfordelingsenhet =
                if (aktivArbeidsfordelingPåBehandling != null) Arbeidsfordelingsenhet(enhetId = aktivArbeidsfordelingPåBehandling.behandlendeEnhetId,
                                                                                      enhetNavn = aktivArbeidsfordelingPåBehandling.behandlendeEnhetNavn) else null

        val oppdatertArbeidsfordelingPåBehandling = when (aktivArbeidsfordelingPåBehandling) {
            null -> {
                arbeidsfordelingPåBehandlingRepository.save(ArbeidsfordelingPåBehandling(behandlingId = behandling.id,
                                                                                         behandlendeEnhetId = arbeidsfordelingsenhet.enhetId,
                                                                                         behandlendeEnhetNavn = arbeidsfordelingsenhet.enhetNavn))
            }
            else -> {
                if (!aktivArbeidsfordelingPåBehandling.manueltOverstyrt &&
                    (aktivArbeidsfordelingPåBehandling.behandlendeEnhetId != arbeidsfordelingsenhet.enhetId)) {

                    aktivArbeidsfordelingPåBehandling.also {
                        it.behandlendeEnhetId = arbeidsfordelingsenhet.enhetId
                        it.behandlendeEnhetNavn = arbeidsfordelingsenhet.enhetNavn
                    }
                    arbeidsfordelingPåBehandlingRepository.save(aktivArbeidsfordelingPåBehandling)
                }
                aktivArbeidsfordelingPåBehandling
            }
        }

        postFastsattBehandlendeEnhet(
                behandling = behandling,
                forrigeArbeidsfordelingsenhet = forrigeArbeidsfordelingsenhet,
                oppdatertArbeidsfordelingPåBehandling = oppdatertArbeidsfordelingPåBehandling,
                manuellOppdatering = false,
        )
    }

    private fun postFastsattBehandlendeEnhet(behandling: Behandling,
                                             forrigeArbeidsfordelingsenhet: Arbeidsfordelingsenhet?,
                                             oppdatertArbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling,
                                             manuellOppdatering: Boolean,
                                             begrunnelse: String = "") {
        logger.info("Fastsatt behandlende enhet ${if (manuellOppdatering) "manuelt" else "automatisk"} på behandling ${behandling.id}: $oppdatertArbeidsfordelingPåBehandling")

        if (forrigeArbeidsfordelingsenhet != null && forrigeArbeidsfordelingsenhet.enhetId != oppdatertArbeidsfordelingPåBehandling.behandlendeEnhetId) {
            loggService.opprettBehandlendeEnhetEndret(behandling = behandling,
                                                      fraEnhet = forrigeArbeidsfordelingsenhet,
                                                      tilEnhet = oppdatertArbeidsfordelingPåBehandling,
                                                      manuellOppdatering = manuellOppdatering,
                                                      begrunnelse = begrunnelse)

            oppgaveService.hentOppgaverSomIkkeErFerdigstilt(behandling).forEach { dbOppgave ->
                val oppgave = oppgaveService.hentOppgave(dbOppgave.gsakId.toLong())

                if (oppgave.tildeltEnhetsnr != oppdatertArbeidsfordelingPåBehandling.behandlendeEnhetId) {
                    logger.info("Oppdaterer enhet fra ${oppgave.tildeltEnhetsnr} til ${oppdatertArbeidsfordelingPåBehandling.behandlendeEnhetId} på oppgave ${oppgave.id}")
                    oppgaveService.patchOppgave(oppgave.copy(
                            tildeltEnhetsnr = oppdatertArbeidsfordelingPåBehandling.behandlendeEnhetId
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

        return integrasjonClient.hentBehandlendeEnhet(identMedStrengeste ?: søker.ident).singleOrNull()
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

        private val logger = LoggerFactory.getLogger(ArbeidsfordelingService::class.java)
    }
}