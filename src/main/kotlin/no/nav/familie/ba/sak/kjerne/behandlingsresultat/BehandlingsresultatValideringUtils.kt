package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_OG_ENDRET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_UTBETALING
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_UTEN_UTBETALING
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.IKKE_VURDERT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.OPPHØRT
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMed
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
        // Valider kombinasjon av behandlingstype og behandlingsresultat
        when {
            behandling.erFørstegangsbehandling() -> {
                val ugyldigeBehandlingsresultaterForTypeFørstegangsbehandling =
                    setOf(AVSLÅTT_OG_OPPHØRT, ENDRET_UTBETALING, ENDRET_UTEN_UTBETALING, ENDRET_OG_OPPHØRT, OPPHØRT, FORTSATT_INNVILGET, IKKE_VURDERT)
                if (resultat in ugyldigeBehandlingsresultaterForTypeFørstegangsbehandling) {
                    throw FunksjonellFeil("Behandlingsresultatet '${resultat.displayName}' er ugyldig i kombinasjon med behandlingstype '${behandling.type.visningsnavn}'.")
                }
            }

            behandling.erRevurdering() -> {
                if (resultat == IKKE_VURDERT) {
                    throw FunksjonellFeil("Behandlingsresultatet '${resultat.displayName}' er ugyldig i kombinasjon med behandlingstype '${behandling.type.visningsnavn}'.")
                }
            }
        }

        // Valider kombinasjon av behandlingsårsak og behandlingsresultat
        when {
            behandling.erKlage() -> {
                val ugyldigeBehandlingsresultaterForÅrsakKlage =
                    setOf(AVSLÅTT_OG_OPPHØRT, AVSLÅTT_ENDRET_OG_OPPHØRT, AVSLÅTT_OG_ENDRET, AVSLÅTT)
                if (resultat in ugyldigeBehandlingsresultaterForÅrsakKlage) {
                    throw FunksjonellFeil("Behandlingsårsak '${behandling.opprettetÅrsak.visningsnavn}' er ugyldig i kombinasjon med resultat '${resultat.displayName}'.")
                }
            }

            behandling.erManuellMigrering() -> {
                if (resultat.erAvslått() || resultat == DELVIS_INNVILGET) {
                    throw FunksjonellFeil(
                        "Du har fått behandlingsresultatet ${resultat.displayName}. " +
                            "Dette er ikke støttet på migreringsbehandlinger. Meld sak i Porten om du er uenig i resultatet.",
                    )
                }
            }

            behandling.erOmregning() -> {
                if (resultat !in setOf(FORTSATT_INNVILGET, FORTSATT_OPPHØRT)) {
                    throw Feil("Behandling $behandling er omregningssak, men er ikke uendret behandlingsresultat")
                }
            }
        }
    }

    fun validerIngenEndringTilbakeITid(
        andelerDenneBehandlingen: Collection<AndelTilkjentYtelse>,
        andelerForrigeBehandling: Collection<AndelTilkjentYtelse>,
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
        andelerDenneBehandlingen: Collection<AndelTilkjentYtelse>,
        andelerForrigeBehandling: Collection<AndelTilkjentYtelse>,
    ) {
        val andelerDenneBehandlingTidslinje = andelerDenneBehandlingen.tilTidslinjerPerAktørOgType()
        val andelerForrigeBehanldingTidslinje = andelerForrigeBehandling.tilTidslinjerPerAktørOgType()

        val endringISatsTidslinjer =
            andelerDenneBehandlingTidslinje.outerJoin(andelerForrigeBehanldingTidslinje) { nyAndel, gammelAndel ->
                if (nyAndel?.sats != gammelAndel?.sats && nyAndel?.kalkulertUtbetalingsbeløp != 0 && gammelAndel?.kalkulertUtbetalingsbeløp != 0) {
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
