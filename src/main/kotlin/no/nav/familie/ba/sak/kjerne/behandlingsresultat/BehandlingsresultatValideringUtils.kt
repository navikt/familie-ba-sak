package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.beskjærTilOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.YearMonth

object BehandlingsresultatValideringUtils {
    internal fun validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
        personerFremstiltKravFor: List<Aktør>,
        personResultater: Set<PersonResultat>,
    ) {
        val personerSomHarEksplisittAvslag = personResultater.filter { it.harEksplisittAvslag() }

        if (personerSomHarEksplisittAvslag.any { !personerFremstiltKravFor.contains(it.aktør) && !it.erSøkersResultater() }) {
            throw FunksjonellFeil(
                frontendFeilmelding = "Det eksisterer personer som har fått eksplisitt avslag, men som det ikke er blitt fremstilt krav for.",
                melding = "Det eksisterer personer som har fått eksplisitt avslag, men som det ikke har blitt fremstilt krav for.",
            )
        }
    }

    internal fun validerBehandlingsresultat(
        behandling: Behandling,
        resultat: Behandlingsresultat,
    ) {
        if ((
                behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING &&
                    setOf(
                        Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
                        Behandlingsresultat.ENDRET_UTBETALING,
                        Behandlingsresultat.ENDRET_UTEN_UTBETALING,
                        Behandlingsresultat.ENDRET_OG_OPPHØRT,
                        Behandlingsresultat.OPPHØRT,
                        Behandlingsresultat.FORTSATT_INNVILGET,
                        Behandlingsresultat.IKKE_VURDERT,
                    ).contains(resultat)
            ) ||
            (behandling.type == BehandlingType.REVURDERING && resultat == Behandlingsresultat.IKKE_VURDERT)
        ) {
            val feilmelding =
                "Behandlingsresultatet ${resultat.displayName.lowercase()} " +
                    "er ugyldig i kombinasjon med behandlingstype '${behandling.type.visningsnavn}'."
            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }
        if (behandling.opprettetÅrsak == BehandlingÅrsak.KLAGE &&
            setOf(
                Behandlingsresultat.AVSLÅTT_OG_OPPHØRT,
                Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT,
                Behandlingsresultat.AVSLÅTT_OG_ENDRET,
                Behandlingsresultat.AVSLÅTT,
            ).contains(resultat)
        ) {
            val feilmelding =
                "Behandlingsårsak ${behandling.opprettetÅrsak.visningsnavn.lowercase()} " +
                    "er ugyldig i kombinasjon med resultat '${resultat.displayName.lowercase()}'."
            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }
    }

    fun validerIngenEndringTilbakeITid(
        andelerDenneBehandlingen: List<AndelTilkjentYtelse>,
        andelerForrigeBehandling: List<AndelTilkjentYtelse>,
        nåMåned: YearMonth,
    ) {
        val forrigeMåned = nåMåned.minusMonths(1)
        val andelerIFortidenTidslinje = andelerDenneBehandlingen.tilTidslinjerPerAktørOgType().beskjærTilOgMed(forrigeMåned.sisteDagIInneværendeMåned())
        val andelerIFortidenForrigeBehanldingTidslinje = andelerForrigeBehandling.tilTidslinjerPerAktørOgType().beskjærTilOgMed(forrigeMåned.sisteDagIInneværendeMåned())

        val endringerIAndelerTilbakeITidTidslinjer =
            andelerIFortidenTidslinje.outerJoin(andelerIFortidenForrigeBehanldingTidslinje) { nyAndel, gammelAndel ->
                if (nyAndel?.kalkulertUtbetalingsbeløp != gammelAndel?.kalkulertUtbetalingsbeløp) {
                    ErEndringIAndel(andelForrigeBehandling = gammelAndel, andelDenneBehandlingen = nyAndel)
                } else {
                    IngenEndringIAndel
                }
            }

        endringerIAndelerTilbakeITidTidslinjer.kastFeilOgLoggVedEndringerIAndeler()
    }

    fun validerSatsErUendret(
        andelerDenneBehandlingen: List<AndelTilkjentYtelse>,
        andelerForrigeBehandling: List<AndelTilkjentYtelse>,
    ) {
        val andelerDenneBehandlingTidslinje = andelerDenneBehandlingen.tilTidslinjerPerAktørOgType()
        val andelerForrigeBehanldingTidslinje = andelerForrigeBehandling.tilTidslinjerPerAktørOgType()

        val endringISatsTidslinjer =
            andelerDenneBehandlingTidslinje.outerJoin(andelerForrigeBehanldingTidslinje) { nyAndel, gammelAndel ->
                if (nyAndel?.sats != gammelAndel?.sats) {
                    ErEndringIAndel(andelForrigeBehandling = gammelAndel, andelDenneBehandlingen = nyAndel)
                } else {
                    IngenEndringIAndel
                }
            }

        endringISatsTidslinjer.kastFeilOgLoggVedEndringerIAndeler()
    }

    private fun Map<Pair<Aktør, YtelseType>, Tidslinje<EndringIAndel>>.kastFeilOgLoggVedEndringerIAndeler() {
        this.forEach { (aktør, ytelsetype), endringIAndelTidslinje ->
            endringIAndelTidslinje.tilPerioderIkkeNull().forEach {
                if (it.verdi is ErEndringIAndel) {
                    val erEndringIAndel = it.verdi as ErEndringIAndel
                    val fom = it.fom?.toYearMonth() ?: MIN_MÅNED
                    val tom = it.tom?.toYearMonth() ?: MAX_MÅNED
                    secureLogger.info(
                        "Det er en uforventet endring i $ytelsetype-andel for $aktør i perioden $fom til $tom.\n" +
                            "Andel denne behandlingen: ${erEndringIAndel.andelDenneBehandlingen}\n" +
                            "Andel forrige behandling: ${erEndringIAndel.andelForrigeBehandling}",
                    )
                    throw Feil("Det er en uforventet endring i andel. Gjelder andel i perioden $fom til $tom. Se secure log for mer detaljer.")
                }
            }
        }
    }
}

private sealed interface EndringIAndel

private object IngenEndringIAndel : EndringIAndel

private data class ErEndringIAndel(
    val andelForrigeBehandling: AndelTilkjentYtelse?,
    val andelDenneBehandlingen: AndelTilkjentYtelse?,
) : EndringIAndel
