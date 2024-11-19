package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.hentArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.tilArbeidsfordelingsenhet
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.barn
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.unleash.UnleashService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArbeidsfordelingService(
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val personidentService: PersonidentService,
    private val oppgaveService: OppgaveService,
    private val loggService: LoggService,
    private val integrasjonClient: IntegrasjonClient,
    private val personopplysningerService: PersonopplysningerService,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val tilpassArbeidsfordelingService: TilpassArbeidsfordelingService,
    private val unleashService: UnleashService,
) {
    @Transactional
    fun manueltOppdaterBehandlendeEnhet(
        behandling: Behandling,
        endreBehandlendeEnhet: RestEndreBehandlendeEnhet,
    ) {
        val aktivArbeidsfordelingPåBehandling =
            arbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(behandling.id)

        val forrigeArbeidsfordelingsenhet =
            Arbeidsfordelingsenhet(
                enhetId = aktivArbeidsfordelingPåBehandling.behandlendeEnhetId,
                enhetNavn = aktivArbeidsfordelingPåBehandling.behandlendeEnhetNavn,
            )

        val oppdatertArbeidsfordelingPåBehandling =
            arbeidsfordelingPåBehandlingRepository.save(
                aktivArbeidsfordelingPåBehandling.copy(
                    behandlendeEnhetId = endreBehandlendeEnhet.enhetId,
                    behandlendeEnhetNavn = integrasjonClient.hentEnhet(endreBehandlendeEnhet.enhetId).navn,
                    manueltOverstyrt = true,
                ),
            )

        postFastsattBehandlendeEnhet(
            behandling = behandling,
            forrigeArbeidsfordelingsenhet = forrigeArbeidsfordelingsenhet,
            oppdatertArbeidsfordelingPåBehandling = oppdatertArbeidsfordelingPåBehandling,
            manuellOppdatering = true,
            begrunnelse = endreBehandlendeEnhet.begrunnelse,
        )
        saksstatistikkEventPublisher.publiserBehandlingsstatistikk(behandling.id)
    }

    fun fastsettBehandlendeEnhet(
        behandling: Behandling,
        sisteBehandlingSomErIverksatt: Behandling? = null,
    ) {
        val aktivArbeidsfordelingPåBehandling =
            arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id)

        val forrigeArbeidsfordelingsenhet = aktivArbeidsfordelingPåBehandling?.tilArbeidsfordelingsenhet()

        val oppdatertArbeidsfordelingPåBehandling =
            if (behandling.erAutomatiskOgHarTidligereBehandling()) {
                fastsettArbeidsfordelingsenhetUtIfraForrigeBehandling(
                    behandling,
                    sisteBehandlingSomErIverksatt,
                    aktivArbeidsfordelingPåBehandling,
                )
            } else {
                val arbeidsfordelingsenhet = hentArbeidsfordelingsenhet(behandling)
                val tilpassetArbeidsfordelingsenhet =
                    tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                        arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                        navIdent = NavIdent(SikkerhetContext.hentSaksbehandler()),
                    )
                when (aktivArbeidsfordelingPåBehandling) {
                    null -> {
                        arbeidsfordelingPåBehandlingRepository.save(
                            ArbeidsfordelingPåBehandling(
                                behandlingId = behandling.id,
                                behandlendeEnhetId = tilpassetArbeidsfordelingsenhet.enhetId,
                                behandlendeEnhetNavn = tilpassetArbeidsfordelingsenhet.enhetNavn,
                            ),
                        )
                    }

                    else -> {
                        if (!aktivArbeidsfordelingPåBehandling.manueltOverstyrt &&
                            (aktivArbeidsfordelingPåBehandling.behandlendeEnhetId != tilpassetArbeidsfordelingsenhet.enhetId)
                        ) {
                            aktivArbeidsfordelingPåBehandling.also {
                                it.behandlendeEnhetId = tilpassetArbeidsfordelingsenhet.enhetId
                                it.behandlendeEnhetNavn = tilpassetArbeidsfordelingsenhet.enhetNavn
                            }
                            arbeidsfordelingPåBehandlingRepository.save(aktivArbeidsfordelingPåBehandling)
                        }
                        aktivArbeidsfordelingPåBehandling
                    }
                }
            }

        postFastsattBehandlendeEnhet(
            behandling = behandling,
            forrigeArbeidsfordelingsenhet = forrigeArbeidsfordelingsenhet,
            oppdatertArbeidsfordelingPåBehandling = oppdatertArbeidsfordelingPåBehandling,
            manuellOppdatering = false,
        )
    }

    private fun fastsettArbeidsfordelingsenhetUtIfraForrigeBehandling(
        behandling: Behandling,
        sisteBehandlingSomErIverksatt: Behandling?,
        aktivArbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling?,
    ): ArbeidsfordelingPåBehandling {
        if (aktivArbeidsfordelingPåBehandling != null) return aktivArbeidsfordelingPåBehandling

        sisteBehandlingSomErIverksatt ?: throw Feil("Kan ikke fastsette arbeidsfordelingsenhet. Finner ikke tidligere behandling.")

        val forrigeIverksattesBehandlingArbeidsfordelingsenhet =
            arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(
                sisteBehandlingSomErIverksatt.id,
            ) ?: throw Feil("Kan ikke fastsette arbeidsfordelingsenhet. Finner ikke arbeidsfordelingsenhet på forrige iverksatte behandling.")

        if (forrigeIverksattesBehandlingArbeidsfordelingsenhet.behandlendeEnhetId == BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer) {
            throw Feil("Kan ikke fastsette arbeidsfordelingsenhet. Forrige behandlende enhet er MIDLERTIDIG_ENHET")
        }

        return arbeidsfordelingPåBehandlingRepository.save(
            forrigeIverksattesBehandlingArbeidsfordelingsenhet.copy(
                id = 0,
                behandlingId = behandling.id,
            ),
        )
    }

    private fun postFastsattBehandlendeEnhet(
        behandling: Behandling,
        forrigeArbeidsfordelingsenhet: Arbeidsfordelingsenhet?,
        oppdatertArbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling,
        manuellOppdatering: Boolean,
        begrunnelse: String = "",
    ) {
        logger.info("Fastsatt behandlende enhet ${if (manuellOppdatering) "manuelt" else "automatisk"} på behandling ${behandling.id}: $oppdatertArbeidsfordelingPåBehandling")
        secureLogger.info("Fastsatt behandlende enhet ${if (manuellOppdatering) "manuelt" else "automatisk"} på behandling ${behandling.id}: ${oppdatertArbeidsfordelingPåBehandling.toSecureString()}")

        if (forrigeArbeidsfordelingsenhet != null && forrigeArbeidsfordelingsenhet.enhetId != oppdatertArbeidsfordelingPåBehandling.behandlendeEnhetId) {
            loggService.opprettBehandlendeEnhetEndret(
                behandling = behandling,
                fraEnhet = forrigeArbeidsfordelingsenhet,
                tilEnhet = oppdatertArbeidsfordelingPåBehandling,
                manuellOppdatering = manuellOppdatering,
                begrunnelse = begrunnelse,
            )

            oppgaveService.endreTilordnetEnhetPåOppgaverForBehandling(
                behandling,
                oppdatertArbeidsfordelingPåBehandling.behandlendeEnhetId,
            )
        }
    }

    fun hentArbeidsfordelingPåBehandling(behandlingId: Long): ArbeidsfordelingPåBehandling =
        arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandlingId)
            ?: error("Finner ikke tilknyttet arbeidsfordeling på behandling med id $behandlingId")

    fun hentArbeidsfordelingsenhet(behandling: Behandling): Arbeidsfordelingsenhet {
        val søker: IdentMedAdressebeskyttelse = identMedAdressebeskyttelse(behandling.fagsak.aktør)

        val personinfoliste: List<IdentMedAdressebeskyttelse> =
            personopplysningGrunnlagRepository
                .finnSøkerOgBarnAktørerTilAktiv(behandling.id)
                .barn()
                .mapNotNull {
                    try {
                        identMedAdressebeskyttelse(it.aktør)
                    } catch (e: PdlPersonKanIkkeBehandlesIFagsystem) {
                        logger.warn("Ignorerer barn fra hentArbeidsfordelingsenhet for behandling ${behandling.id} : ${e.årsak}")
                        secureLogger.warn("Ignorerer barn ${it.aktør.aktivFødselsnummer()} hentArbeidsfordelingsenhet for behandling ${behandling.id}: ${e.årsak}")
                        null
                    }
                }.plus(søker)

        val identMedStrengeste = finnPersonMedStrengesteAdressebeskyttelse(personinfoliste)

        return integrasjonClient.hentBehandlendeEnhet(identMedStrengeste ?: søker.ident).singleOrNull()
            ?: throw Feil(message = "Fant flere eller ingen enheter på behandling.")
    }

    fun hentArbeidsfordelingsenhetPåIdenter(
        søkerIdent: String,
        barnIdenter: List<String>,
    ): Arbeidsfordelingsenhet {
        val identMedStrengeste =
            finnPersonMedStrengesteAdressebeskyttelse((barnIdenter + søkerIdent).map { identMedAdressebeskyttelse(it) })

        return integrasjonClient.hentBehandlendeEnhet(identMedStrengeste ?: søkerIdent).singleOrNull()
            ?: throw Feil(message = "Fant flere eller ingen enheter på behandling.")
    }

    private fun identMedAdressebeskyttelse(ident: String) =
        IdentMedAdressebeskyttelse(
            ident = ident,
            adressebeskyttelsegradering =
                personopplysningerService
                    .hentPersoninfoEnkel(
                        personidentService.hentAktør(ident),
                    ).adressebeskyttelseGradering,
        )

    private fun identMedAdressebeskyttelse(aktør: Aktør) =
        IdentMedAdressebeskyttelse(
            ident = aktør.aktivFødselsnummer(),
            adressebeskyttelsegradering = personopplysningerService.hentPersoninfoEnkel(aktør).adressebeskyttelseGradering,
        )

    data class IdentMedAdressebeskyttelse(
        val ident: String,
        val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING?,
    )

    companion object {
        private val logger = LoggerFactory.getLogger(ArbeidsfordelingService::class.java)
    }
}
