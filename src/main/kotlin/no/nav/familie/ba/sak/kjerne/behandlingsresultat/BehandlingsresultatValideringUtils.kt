package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærTilOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
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
        val andelerIFortidenTidslinje = andelerDenneBehandlingen.tilTidslinjerPerAktørOgType().beskjærTilOgMed(forrigeMåned.tilTidspunkt())
        val andelerIFortidenForrigeBehanldingTidslinje = andelerForrigeBehandling.tilTidslinjerPerAktørOgType().beskjærTilOgMed(forrigeMåned.tilTidspunkt())

        val erEndringTilbakeITidTidslinjer =
            andelerIFortidenTidslinje.outerJoin(andelerIFortidenForrigeBehanldingTidslinje) { nyAndel, gammelAndel ->
                nyAndel?.kalkulertUtbetalingsbeløp != gammelAndel?.kalkulertUtbetalingsbeløp
            }

        erEndringTilbakeITidTidslinjer.forEach { _, erEndringTilbakeITidTidslinje ->
            erEndringTilbakeITidTidslinje.perioder().forEach {
                val erEndring = it.innhold == true
                if (erEndring) {
                    throw Feil("Det er endringer i andelene som går tilbake i tid. Gjelder andelene fra ${it.fraOgMed.tilYearMonth()} til ${it.tilOgMed.tilYearMonth()}.")
                }
            }
        }
    }

    fun validerSatsErUendret(
        andelerDenneBehandlingen: List<AndelTilkjentYtelse>,
        andelerForrigeBehandling: List<AndelTilkjentYtelse>,
    ) {
        val andelerIFortidenTidslinje = andelerDenneBehandlingen.tilTidslinjerPerAktørOgType()
        val andelerIFortidenForrigeBehanldingTidslinje = andelerForrigeBehandling.tilTidslinjerPerAktørOgType()

        val erEndringISatsTidslinjer =
            andelerIFortidenTidslinje.outerJoin(andelerIFortidenForrigeBehanldingTidslinje) { nyAndel, gammelAndel ->
                nyAndel?.sats != gammelAndel?.sats
            }

        erEndringISatsTidslinjer.forEach { _, erEndringISatsTidslinje ->
            erEndringISatsTidslinje.perioder().forEach {
                val erEndring = it.innhold == true
                if (erEndring) {
                    throw Feil("Det er endringer i andelene som går tilbake i tid. Gjelder andelene fra ${it.fraOgMed.tilYearMonth()} til ${it.tilOgMed.tilYearMonth()}.")
                }
            }
        }
    }
}
