package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.finnAktørIderMedUgyldigEtterbetalingsperiode
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.barn
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.strengtfortrolig.StrengtFortroligService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TilkjentYtelseValideringService(
    private val totrinnskontrollService: TotrinnskontrollService,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
    private val strengtFortroligService: StrengtFortroligService,
) {
    fun validerAtIngenUtbetalingerOverstiger100Prosent(behandling: Behandling) {
        if (behandling.erMigrering() || behandling.erTekniskEndring() || behandling.erSatsendringNasjonal() || behandling.erMånedligValutajustering() || behandling.erSatsendringEøs()) return
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)

        if (totrinnskontroll?.godkjent == true) {
            validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(behandling)
            validerAtSøkerIkkeFårFlereUtvidetAndelerSammePeriode(behandling)
        }
    }

    fun validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(behandling: Behandling) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)

        val søkerOgBarn = persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandlingId = behandling.id)

        val barnMedAndreRelevanteTilkjentYtelser =
            søkerOgBarn.barn().map {
                Pair(
                    it,
                    beregningService.hentRelevanteTilkjentYtelserForPerson(it.aktør, behandling.fagsak.id),
                )
            }

        secureLogger.info("Andeler tilkjent ytelse i inneværende behandling=${behandling.id}: " + tilkjentYtelse.andelerTilkjentYtelse)
        secureLogger.info(
            "Barn og deres andeler tilkjent ytelse fra andre fagsaker: " +
                barnMedAndreRelevanteTilkjentYtelser.map {
                    "${it.first} -> ${it.second}"
                },
        )

        TilkjentYtelseValidering.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
            behandlendeBehandlingTilkjentYtelse = tilkjentYtelse,
            barnMedAndreRelevanteTilkjentYtelser = barnMedAndreRelevanteTilkjentYtelser,
            søkerOgBarn = søkerOgBarn,
        )
    }

    private fun validerAtSøkerIkkeFårFlereUtvidetAndelerSammePeriode(behandling: Behandling) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)
        val fagsak = behandling.fagsak

        val søker = fagsak.skjermetBarnSøker?.aktør ?: fagsak.aktør
        val søkersTilkjentYtelserIAndreFagsak = beregningService.hentRelevanteTilkjentYtelserForPerson(søker, behandling.fagsak.id)

        secureLogger.info("Andeler tilkjent ytelse i inneværende behandling for søker=${behandling.id}: " + tilkjentYtelse.andelerTilkjentYtelse.filter { it.aktør == søker })
        secureLogger.info("Andeler tilkjent ytelse i andre behandlinger for søker=${behandling.id}: " + søkersTilkjentYtelserIAndreFagsak.flatMap { it.andelerTilkjentYtelse }.filter { it.aktør == søker })

        TilkjentYtelseValidering.validerAtSøkerIkkeFårFlereUtvidetUtbetalingerSammePeriode(
            behandlendeBehandlingTilkjentYtelse = tilkjentYtelse,
            andreRelevanteTilkjentYtelser = søkersTilkjentYtelserIAndreFagsak,
        )
    }

    fun validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(behandlingId: Long) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingId)

        if (tilkjentYtelse.harAndelerTilkjentYtelseMedSammeOffset()) {
            secureLogger.info("Behandling har flere andeler med likt offset: ${tilkjentYtelse.andelerTilkjentYtelse}")
            throw Feil("Behandling $behandlingId har andel tilkjent ytelse med offset lik en annen andel i behandlingen.")
        }
    }

    private fun TilkjentYtelse.harAndelerTilkjentYtelseMedSammeOffset(): Boolean {
        val periodeOffsetForAndeler = this.andelerTilkjentYtelse.mapNotNull { it.periodeOffset }

        return periodeOffsetForAndeler.size != periodeOffsetForAndeler.distinct().size
    }

    fun barnetrygdLøperForAnnenForelder(
        behandling: Behandling,
        barna: List<Person>,
    ): Boolean =
        barna.any {
            beregningService
                .hentRelevanteTilkjentYtelserForPerson(aktør = it.aktør, fagsakId = behandling.fagsak.id)
                .isNotEmpty()
        }

    fun finnAktørerMedUgyldigEtterbetalingsperiode(
        behandlingId: Long,
    ): List<Aktør> {
        val tilkjentYtelse = beregningService.hentOptionalTilkjentYtelseForBehandling(behandlingId = behandlingId)

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(
                behandling = behandling,
            )
        val forrigeAndelerTilkjentYtelse =
            forrigeBehandling
                ?.let { beregningService.hentOptionalTilkjentYtelseForBehandling(behandlingId = it.id) }
                ?.andelerTilkjentYtelse

        val søknadsdato = behandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id) ?: behandling.opprettetTidspunkt
        val gyldigEtterbetalingFom = hentGyldigEtterbetaling3MndFom(søknadsdato.toLocalDate())

        val skjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil = strengtFortroligService.hentSkjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil(behandling.fagsak)

        val aktørIderMedUgyldigEtterbetalingsperiode =
            finnAktørIderMedUgyldigEtterbetalingsperiode(
                forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse ?: emptyList(),
                andelerTilkjentYtelse = tilkjentYtelse?.andelerTilkjentYtelse?.toList() ?: emptyList(),
                gyldigEtterbetalingFom = gyldigEtterbetalingFom,
            )

        return aktørIderMedUgyldigEtterbetalingsperiode.filterNot {
            it.aktivFødselsnummer() in skjermedeBarnUtenLøpendeAndelerSaksbehandlerIkkeHarTilgangTil
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(TilkjentYtelseValideringService::class.java)
    }
}
