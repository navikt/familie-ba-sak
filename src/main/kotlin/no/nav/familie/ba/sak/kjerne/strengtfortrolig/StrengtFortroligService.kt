package no.nav.familie.ba.sak.kjerne.strengtfortrolig

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.PersonDto
import no.nav.familie.ba.sak.ekstern.restDomene.UtvidetBehandlingDto
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.logg.Logg
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Identkonverterer
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelserDto
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class StrengtFortroligService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
    private val featureToggleService: FeatureToggleService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {
    /**
     * Anonymiserer strengt fortrolige barn i UtvidetBehandlingDto dersom saksbehandler
     * mangler tilgang til kode6/7 men barn ikke lenger har løpende andeler.
     */
    fun anonymiserStrengtFortroligBarn(
        utvidetBehandlingDto: UtvidetBehandlingDto,
        behandlingId: Long,
    ): UtvidetBehandlingDto {
        val barnMedStrengtFortroligKodeSomSkalAnonymiseres = hentBarnMedStrengtFortroligKodeSomSkalAnonymiseres(behandlingId).takeIf { it.isNotEmpty() } ?: return utvidetBehandlingDto

        val anonymiserteNavnForIdent =
            barnMedStrengtFortroligKodeSomSkalAnonymiseres
                .sorted()
                .mapIndexed { index, ident ->
                    ident to "$SKJERMET_BARN ${index + 1}"
                }.toMap()

        return utvidetBehandlingDto.copy(
            personer =
                utvidetBehandlingDto.personer.map { person ->
                    person.personIdent
                        .anonymiserHvisSkjermet(anonymiserteNavnForIdent)
                        ?.let { person.anonymiser(it) }
                        ?: person
                },
            personResultater = utvidetBehandlingDto.personResultater.filter { it.personIdent !in barnMedStrengtFortroligKodeSomSkalAnonymiseres },
            søknadsgrunnlag =
                utvidetBehandlingDto.søknadsgrunnlag?.let { søknad ->
                    søknad.copy(
                        barnaMedOpplysninger =
                            søknad.barnaMedOpplysninger.map { barn ->
                                barn.ident.anonymiserHvisSkjermet(anonymiserteNavnForIdent)?.let {
                                    barn.copy(ident = it, navn = it, fødselsdato = SKJERMET_BARN_FØDSELSDATO, skjermesForBruker = true)
                                } ?: barn
                            },
                    )
                },
            utbetalingsperioder =
                utvidetBehandlingDto.utbetalingsperioder.map { periode ->
                    periode.copy(
                        utbetalingsperiodeDetaljer =
                            periode.utbetalingsperiodeDetaljer.map { detalj ->
                                detalj.person.personIdent.anonymiserHvisSkjermet(anonymiserteNavnForIdent)?.let {
                                    detalj.copy(person = detalj.person.anonymiser(it))
                                } ?: detalj
                            },
                    )
                },
            personerMedAndelerTilkjentYtelse =
                utvidetBehandlingDto.personerMedAndelerTilkjentYtelse.map { personMedAndeler ->
                    personMedAndeler.personIdent?.anonymiserHvisSkjermet(anonymiserteNavnForIdent)?.let {
                        personMedAndeler.copy(personIdent = it, skjermesForBruker = true)
                    } ?: personMedAndeler
                },
            endretUtbetalingAndeler =
                utvidetBehandlingDto.endretUtbetalingAndeler.map { endretAndel ->
                    endretAndel.copy(
                        personIdenter = endretAndel.personIdenter?.map { it.anonymiserHvisSkjermet(anonymiserteNavnForIdent) ?: it },
                        inneholderBarnSomSkalSkjermes = endretAndel.personIdenter?.any { it in barnMedStrengtFortroligKodeSomSkalAnonymiseres } == true,
                    )
                },
            kompetanser =
                utvidetBehandlingDto.kompetanser.map { kompetanse ->
                    kompetanse.copy(
                        barnIdenter = kompetanse.barnIdenter.map { it.anonymiserHvisSkjermet(anonymiserteNavnForIdent) ?: it },
                        inneholderBarnSomSkalSkjermes = kompetanse.barnIdenter.any { it in barnMedStrengtFortroligKodeSomSkalAnonymiseres },
                    )
                },
            valutakurser =
                utvidetBehandlingDto.valutakurser.map { valutakurs ->
                    valutakurs.copy(
                        barnIdenter = valutakurs.barnIdenter.map { it.anonymiserHvisSkjermet(anonymiserteNavnForIdent) ?: it },
                        inneholderBarnSomSkalSkjermes = valutakurs.barnIdenter.any { it in barnMedStrengtFortroligKodeSomSkalAnonymiseres },
                    )
                },
            utenlandskePeriodebeløp =
                utvidetBehandlingDto.utenlandskePeriodebeløp.map { utenlandsk ->
                    utenlandsk.copy(
                        barnIdenter = utenlandsk.barnIdenter.map { it.anonymiserHvisSkjermet(anonymiserteNavnForIdent) ?: it },
                        inneholderBarnSomSkalSkjermes = utenlandsk.barnIdenter.any { it in barnMedStrengtFortroligKodeSomSkalAnonymiseres },
                    )
                },
            manglendeSvalbardmerking =
                utvidetBehandlingDto.manglendeSvalbardmerking.map { merking ->
                    merking.ident.anonymiserHvisSkjermet(anonymiserteNavnForIdent)?.let {
                        merking.copy(ident = it, skjermesForBruker = true)
                    } ?: merking
                },
            manglendeFinnmarkmerking =
                utvidetBehandlingDto.manglendeFinnmarkmerking?.let {
                    it.ident.anonymiserHvisSkjermet(anonymiserteNavnForIdent)?.let { anonymisertIdent ->
                        it.copy(ident = anonymisertIdent, skjermesForBruker = true)
                    } ?: it
                },
        )
    }

    /**
     * Filtrerer bort logginnslag som inneholder personident eller navn til strengt fortrolige barn
     * som saksbehandler ikke har tilgang til.
     */
    fun filtrerLoggForStrengtFortroligeBarn(
        logger: List<Logg>,
        behandlingId: Long,
    ): List<Logg> {
        val barnMedStrengtFortroligKodeSomSkalAnonymiseres = hentBarnMedStrengtFortroligKodeSomSkalAnonymiseres(behandlingId)
        if (barnMedStrengtFortroligKodeSomSkalAnonymiseres.isEmpty()) return logger

        val forbudteLogg =
            buildList {
                barnMedStrengtFortroligKodeSomSkalAnonymiseres.forEach { ident ->
                    add(ident)
                    add(Identkonverterer.formaterIdent(ident))
                }
            }

        return logger.map { logg ->
            if (forbudteLogg.any { logg.tekst.contains(it) }) {
                logg.copy(tekst = "Informasjon om strengt fortrolig barn er filtrert ut.")
            } else {
                logg
            }
        }
    }

    /**
     * Filtrerer vekk vedtaksperioder som inneholder utbetalingsdetaljer for skjermede barn
     * som saksbehandler ikke har tilgang til.
     */
    fun filtrerVekkVedtaksperioderMedSkjermetBarn(
        vedtaksperioder: List<UtvidetVedtaksperiodeMedBegrunnelserDto>,
        behandlingId: Long,
    ): List<UtvidetVedtaksperiodeMedBegrunnelserDto> {
        val skjermedeIdenter = hentBarnMedStrengtFortroligKodeSomSkalAnonymiseres(behandlingId)
        if (skjermedeIdenter.isEmpty()) return vedtaksperioder

        return vedtaksperioder.filterNot { periode ->
            periode.utbetalingsperiodeDetaljer.any { it.person.personIdent in skjermedeIdenter }
        }
    }

    /**
     * Sjekker om saksbehandler har tilgang til alle personer, eller om de eneste personene
     * saksbehandler mangler tilgang til er skjermede barn uten løpende andeler.
     */
    fun harTilgangTilAllePersonerEllerKunManglendeTilgangTilSkjermedeBarnUtenLøpendeAndeler(
        fagsakId: Long,
        personIdenter: List<String>,
        søker: Aktør,
    ): Boolean {
        val tilgangerTilPersoner = sjekkTilgangTilPersoner(personIdenter)
        if (tilgangerTilPersoner.all { it.harTilgang }) return true

        val personerSaksbehandlerIkkeHarTilgangTil = tilgangerTilPersoner.filterNot { it.harTilgang }.map { it.personIdent }.toSet()
        val skjermedeBarnUtenAndeler = hentSkjermedeBarnUtenLøpendeAndeler(fagsakId, tilgangerTilPersoner, søker)

        return personerSaksbehandlerIkkeHarTilgangTil == skjermedeBarnUtenAndeler
    }

    fun finnSkjermedeBarnSaksbehandlerManglerTilgangTilUtenLøpendeAndelerPåFagsak(fagsak: Fagsak): Set<Aktør> {
        if (SikkerhetContext.erSystemKontekst()) return emptySet()

        val søkerOgBarn = personopplysningGrunnlagRepository.finnSøkerOgBarnAktørerTilFagsak(fagsak.id).takeIf { it.isNotEmpty() } ?: return emptySet()
        val søker =
            søkerOgBarn.firstOrNull { it.type == PersonType.SØKER }?.aktør
                ?: fagsak.skjermetBarnSøker?.aktør
                ?: fagsak.aktør

        val tilgangerTilPersoner = sjekkTilgangTilPersoner(søkerOgBarn.map { it.aktør.aktivFødselsnummer() })
        val skjermedeIdenter = hentSkjermedeBarnUtenLøpendeAndeler(fagsak.id, tilgangerTilPersoner, søker)

        return søkerOgBarn
            .filter { it.aktør.aktivFødselsnummer() in skjermedeIdenter }
            .map { it.aktør }
            .toSet()
    }

    fun harFagsakPersonMedStrengtFortroligAdressebeskyttelse(fagsak: Fagsak): Boolean {
        val identer =
            personopplysningGrunnlagRepository
                .finnSøkerOgBarnAktørerTilFagsak(fagsak.id)
                .takeIf { it.isNotEmpty() }
                ?.map { it.aktør.aktivFødselsnummer() }
                ?: listOf(fagsak.skjermetBarnSøker?.aktør?.aktivFødselsnummer() ?: fagsak.aktør.aktivFødselsnummer())

        return familieIntegrasjonerTilgangskontrollService
            .hentIdenterMedStrengtFortroligAdressebeskyttelse(identer)
            .isNotEmpty()
    }

    private fun hentBarnMedStrengtFortroligKodeSomSkalAnonymiseres(behandlingId: Long): Set<String> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val personerPåBehandling = personopplysningGrunnlagRepository.finnSøkerOgBarnAktørerTilAktiv(behandlingId).takeIf { it.isNotEmpty() }
        val søker = behandling.fagsak.skjermetBarnSøker?.aktør ?: behandling.fagsak.aktør

        val personIdenter =
            personerPåBehandling
                ?.map { it.aktør.aktivFødselsnummer() }
                ?: listOf(behandling.fagsak.aktør.aktivFødselsnummer())

        val tilgangerTilPersoner = sjekkTilgangTilPersoner(personIdenter)

        return hentSkjermedeBarnUtenLøpendeAndeler(behandling.fagsak.id, tilgangerTilPersoner, søker)
    }

    private fun sjekkTilgangTilPersoner(personIdenter: List<String>): List<Tilgang> =
        familieIntegrasjonerTilgangskontrollService
            .sjekkTilgangTilPersoner(personIdenter)
            .map { it.value }

    private fun hentSkjermedeBarnUtenLøpendeAndeler(
        fagsakId: Long,
        tilgangerTilPersoner: List<Tilgang>,
        søker: Aktør,
    ): Set<String> {
        if (!featureToggleService.isEnabled(FeatureToggle.TILLAT_TILGANG_SKJERMET_BARN_UTEN_LØPENDE_ANDELER)) {
            return emptySet()
        }

        if (tilgangerTilPersoner.none { it.harTilgang && it.personIdent == søker.aktivFødselsnummer() }) {
            return emptySet()
        }

        val barnUtenStrengtFortroligTilgang =
            tilgangerTilPersoner
                .filterNot { it.harTilgang }
                .takeIf { it.isNotEmpty() && it.all { p -> p.begrunnelse?.contains(BEGRUNNELSE_STRENGT_FORTROLIG) == true } }
                ?.map { it.personIdent }
                ?: return emptySet()

        val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId) ?: return emptySet()
        val andelerPåBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id)

        val alleSkjermedeBarnHarAndelerMenIngenLøpende =
            barnUtenStrengtFortroligTilgang.all { ident ->
                val barnetsAndeler = andelerPåBehandling.filter { it.aktør.aktivFødselsnummer() == ident }
                barnetsAndeler.none { it.erLøpende() }
            }

        if (alleSkjermedeBarnHarAndelerMenIngenLøpende) {
            secureLogger.info(
                "Tillater tilgang til fagsak=$fagsakId for saksbehandler=${SikkerhetContext.hentSaksbehandler()}. " +
                    "Saksbehandler har ikke tilgang til personer med strengt fortrolig adresse, men barn som er skjermet har ikke lenger løpende andeler",
            )
            return barnUtenStrengtFortroligTilgang.toSet()
        }

        return emptySet()
    }

    private fun PersonDto.anonymiser(anonymisertNavn: String) =
        copy(
            personIdent = anonymisertNavn,
            navn = anonymisertNavn,
            fødselsdato = SKJERMET_BARN_FØDSELSDATO,
            registerhistorikk = null,
            dødsfallDato = null,
            harFalskIdentitet = false,
            skjermesForBruker = true,
        )

    private fun String.anonymiserHvisSkjermet(identerSomSkalAnonymiseres: Map<String, String>) = identerSomSkalAnonymiseres[this]

    companion object {
        const val SKJERMET_BARN = "SKJERMET BARN"
        const val BEGRUNNELSE_STRENGT_FORTROLIG = "Bruker mangler rollen '0000-GA-Strengt_Fortrolig_Adresse'"
        val SKJERMET_BARN_FØDSELSDATO = LocalDate.of(9999, 1, 1)
    }
}
