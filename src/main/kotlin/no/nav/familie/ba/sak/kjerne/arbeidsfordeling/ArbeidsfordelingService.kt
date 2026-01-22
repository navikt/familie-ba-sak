package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle.HENT_ARBEIDSFORDELING_FOR_AUTOMATISK_BEHANDLING_ETTER_PORTEFØLJEJUSTERING
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlPersonInfo
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.MIDLERTIDIG_ENHET
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.STEINKJER
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.hentArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.tilArbeidsfordelingsenhet
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.barn
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
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
    private val integrasjonKlient: IntegrasjonKlient,
    private val personopplysningerService: PersonopplysningerService,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val tilpassArbeidsfordelingService: TilpassArbeidsfordelingService,
    private val featureToggleService: FeatureToggleService,
) {
    @Transactional
    fun manueltOppdaterBehandlendeEnhet(
        behandling: Behandling,
        endreBehandlendeEnhet: EndreBehandlendeEnhetDto,
    ) {
        validerEndringAvBehandlendeEnhet(endreBehandlendeEnhet)

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
                    behandlendeEnhetNavn = integrasjonKlient.hentEnhet(endreBehandlendeEnhet.enhetId).navn,
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

    private fun validerEndringAvBehandlendeEnhet(endreBehandlendeEnhet: EndreBehandlendeEnhetDto) {
        if (endreBehandlendeEnhet.enhetId == STEINKJER.enhetsnummer) {
            throw FunksjonellFeil(
                melding = "Fra og med 5. januar 2026 er det ikke lenger å mulig å endre behandlende enhet til Steinkjer.",
            )
        }
    }

    @Transactional
    fun oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(
        behandling: Behandling,
        nyEnhetId: String,
    ) {
        val aktivArbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(behandling.id)

        if (nyEnhetId == aktivArbeidsfordelingPåBehandling.behandlendeEnhetId) return

        val forrigeArbeidsfordelingsenhet =
            Arbeidsfordelingsenhet(
                enhetId = aktivArbeidsfordelingPåBehandling.behandlendeEnhetId,
                enhetNavn = aktivArbeidsfordelingPåBehandling.behandlendeEnhetNavn,
            )

        val oppdatertArbeidsfordelingPåBehandling =
            arbeidsfordelingPåBehandlingRepository.save(
                aktivArbeidsfordelingPåBehandling.copy(
                    behandlendeEnhetId = nyEnhetId,
                    behandlendeEnhetNavn = BarnetrygdEnhet.fraEnhetsnummer(nyEnhetId).enhetsnavn,
                ),
            )

        loggService.opprettBehandlendeEnhetEndret(
            behandling = behandling,
            fraEnhet = forrigeArbeidsfordelingsenhet,
            tilEnhet = oppdatertArbeidsfordelingPåBehandling,
            manuellOppdatering = false,
            begrunnelse = "Porteføljejustering",
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
            when {
                behandling.erAutomatiskOgSkalHaTidligereBehandling() && aktivArbeidsfordelingPåBehandling != null -> {
                    aktivArbeidsfordelingPåBehandling
                }

                behandling.erAutomatiskOgSkalHaTidligereBehandling() && sisteBehandlingSomErIverksatt == null -> {
                    throw Feil("Kan ikke fastsette arbeidsfordelingsenhet. Finner ikke tidligere behandling.")
                }

                behandling.erAutomatiskOgSkalHaTidligereBehandling() -> {
                    fastsettArbeidsfordelingFraTidligereBehandlinger(behandling)
                }

                else -> {
                    fastsettArbeidsfordelingPåBehandling(behandling, aktivArbeidsfordelingPåBehandling)
                }
            }

        postFastsattBehandlendeEnhet(
            behandling = behandling,
            forrigeArbeidsfordelingsenhet = forrigeArbeidsfordelingsenhet,
            oppdatertArbeidsfordelingPåBehandling = oppdatertArbeidsfordelingPåBehandling,
            manuellOppdatering = false,
        )
    }

    private fun fastsettArbeidsfordelingPåBehandling(
        behandling: Behandling,
        aktivArbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling?,
    ): ArbeidsfordelingPåBehandling {
        val arbeidsfordelingsenhet = hentArbeidsfordelingsenhet(behandling)
        val tilpassetArbeidsfordelingsenhet =
            tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                navIdent = NavIdent(SikkerhetContext.hentSaksbehandler()),
            )

        return when {
            aktivArbeidsfordelingPåBehandling == null -> {
                arbeidsfordelingPåBehandlingRepository.save(
                    ArbeidsfordelingPåBehandling(
                        behandlingId = behandling.id,
                        behandlendeEnhetId = tilpassetArbeidsfordelingsenhet.enhetId,
                        behandlendeEnhetNavn = tilpassetArbeidsfordelingsenhet.enhetNavn,
                    ),
                )
            }

            aktivArbeidsfordelingPåBehandling.manueltOverstyrt -> {
                aktivArbeidsfordelingPåBehandling
            }

            aktivArbeidsfordelingPåBehandling.behandlendeEnhetId != tilpassetArbeidsfordelingsenhet.enhetId -> {
                arbeidsfordelingPåBehandlingRepository.save(
                    aktivArbeidsfordelingPåBehandling.copy(
                        behandlendeEnhetId = tilpassetArbeidsfordelingsenhet.enhetId,
                        behandlendeEnhetNavn = tilpassetArbeidsfordelingsenhet.enhetNavn,
                    ),
                )
            }

            else -> {
                aktivArbeidsfordelingPåBehandling
            }
        }
    }

    private fun fastsettArbeidsfordelingFraTidligereBehandlinger(
        behandling: Behandling,
    ): ArbeidsfordelingPåBehandling {
        val sisteGyldigeArbeidsfordeling = arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(behandling.fagsak.id)

        val skalHenteArbeidsfordelingsenhet =
            featureToggleService.isEnabled(HENT_ARBEIDSFORDELING_FOR_AUTOMATISK_BEHANDLING_ETTER_PORTEFØLJEJUSTERING) && (
                sisteGyldigeArbeidsfordeling == null ||
                    sisteGyldigeArbeidsfordeling.behandlendeEnhetId == MIDLERTIDIG_ENHET.enhetsnummer ||
                    sisteGyldigeArbeidsfordeling.behandlendeEnhetId == STEINKJER.enhetsnummer
            )

        val arbeidsfordelingPåBehandling =
            if (skalHenteArbeidsfordelingsenhet) {
                val arbeidsfordelingsenhet = hentArbeidsfordelingsenhet(behandling)
                if (arbeidsfordelingsenhet.enhetId == STEINKJER.enhetsnummer) {
                    throw Feil("Kan ikke sette behandlende enhet til '${STEINKJER.enhetsnavn}' etter porteføljejustering.")
                }

                ArbeidsfordelingPåBehandling(
                    behandlingId = behandling.id,
                    behandlendeEnhetId = arbeidsfordelingsenhet.enhetId,
                    behandlendeEnhetNavn = arbeidsfordelingsenhet.enhetNavn,
                )
            } else {
                sisteGyldigeArbeidsfordeling?.copy(
                    id = 0,
                    behandlingId = behandling.id,
                )
                    // Dette kan slettes når toggle fjernes
                    ?: ArbeidsfordelingPåBehandling(
                        behandlingId = behandling.id,
                        behandlendeEnhetId = MIDLERTIDIG_ENHET.enhetsnummer,
                        behandlendeEnhetNavn = MIDLERTIDIG_ENHET.enhetsnavn,
                    )
            }

        return arbeidsfordelingPåBehandlingRepository.save(arbeidsfordelingPåBehandling)
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
            ?: throw Feil("Finner ikke tilknyttet arbeidsfordeling på behandling med id $behandlingId")

    fun hentArbeidsfordelingsenhet(behandling: Behandling): Arbeidsfordelingsenhet {
        val søkerIdent = behandling.fagsak.aktør.aktivFødselsnummer()

        val arbeidsfordelingPersoner: Map<String, ArbeidsfordelingPerson> =
            personopplysningGrunnlagRepository
                .finnSøkerOgBarnAktørerTilAktiv(behandling.id)
                .barn()
                .associate {
                    it.aktør.aktivFødselsnummer() to arbeidsfordelingPerson(it.aktør, PersonType.BARN)
                }.plus(
                    søkerIdent to arbeidsfordelingPerson(behandling.fagsak.aktør, PersonType.SØKER),
                )

        return hentArbeidsfordelingForPersoner(
            fagsak = behandling.fagsak,
            arbeidsfordelingPersoner = arbeidsfordelingPersoner,
            behandlingType = behandling.kategori.tilOppgavebehandlingType(),
        )
    }

    private fun arbeidsfordelingPerson(
        aktør: Aktør,
        personType: PersonType,
    ): ArbeidsfordelingPerson {
        val pdlPersonInfo = personopplysningerService.hentPdlPersonInfoEnkel(aktør)
        return ArbeidsfordelingPerson(
            pdlPersonInfo,
            IdentMedAdressebeskyttelse(
                ident = aktør.aktivFødselsnummer(),
                adressebeskyttelsegradering =
                    pdlPersonInfo
                        .personInfoBase()
                        .adressebeskyttelseGradering,
                personType = personType,
            ),
        )
    }

    fun hentArbeidsfordelingsenhetPåIdenter(
        fagsak: Fagsak,
        barnIdenter: List<String>,
        behandlingstype: Behandlingstype?,
    ): Arbeidsfordelingsenhet {
        val søkerIdent = fagsak.aktør.aktivFødselsnummer()

        val arbeidsfordelingPersoner: Map<String, ArbeidsfordelingPerson> =
            mapOf(
                søkerIdent to
                    arbeidsfordelingPerson(
                        aktør = personidentService.hentAktør(søkerIdent),
                        personType = PersonType.SØKER,
                    ),
            ).plus(
                barnIdenter.associateWith {
                    arbeidsfordelingPerson(
                        aktør = personidentService.hentAktør(it),
                        personType = PersonType.BARN,
                    )
                },
            )

        return hentArbeidsfordelingForPersoner(
            fagsak = fagsak,
            arbeidsfordelingPersoner = arbeidsfordelingPersoner,
            behandlingType = behandlingstype,
        )
    }

    fun hentArbeidsfordelingForPersoner(
        fagsak: Fagsak,
        arbeidsfordelingPersoner: Map<String, ArbeidsfordelingPerson>,
        behandlingType: Behandlingstype?,
    ): Arbeidsfordelingsenhet {
        val søkerIdent = fagsak.aktør.aktivFødselsnummer()

        val identMedStrengesteAdressebeskyttelse =
            finnPersonMedStrengesteAdressebeskyttelse(
                arbeidsfordelingPersoner.values.map { it.identMedAdressebeskyttelse },
            ) ?: søkerIdent

        val (pdlPersonInfoMedStrengesteAdressebeskyttelse, _) = arbeidsfordelingPersoner[identMedStrengesteAdressebeskyttelse] ?: throw Feil("Ident med strengeste adressebeskyttelse matcher ingen av personene i behandlingen")

        return when (pdlPersonInfoMedStrengesteAdressebeskyttelse) {
            is PdlPersonInfo.Person -> {
                integrasjonKlient.hentBehandlendeEnhet(identMedStrengesteAdressebeskyttelse, behandlingType).singleOrNull()
                    ?: throw Feil(message = "Fant flere eller ingen enheter på behandling.")
            }

            is PdlPersonInfo.FalskPerson -> {
                // Dersom ident med strengeste adressebeskyttelse er falsk, henter vi siste gyldige arbeidsfordeling for fagsak og bruker den
                val sisteGyldigeArbeidsfordelingForFagsak = arbeidsfordelingPåBehandlingRepository.finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(fagsak.id) ?: throw Feil(message = "Kan ikke arbeidsfordele behandling for falsk identitet i fagsak uten tidligere behandling")
                Arbeidsfordelingsenhet(enhetId = sisteGyldigeArbeidsfordelingForFagsak.behandlendeEnhetId, enhetNavn = sisteGyldigeArbeidsfordelingForFagsak.behandlendeEnhetNavn)
            }
        }
    }

    data class IdentMedAdressebeskyttelse(
        val ident: String,
        val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING?,
        val personType: PersonType,
    )

    data class ArbeidsfordelingPerson(
        val pdlPersonInfo: PdlPersonInfo,
        val identMedAdressebeskyttelse: IdentMedAdressebeskyttelse,
    )

    companion object {
        private val logger = LoggerFactory.getLogger(ArbeidsfordelingService::class.java)
    }
}
