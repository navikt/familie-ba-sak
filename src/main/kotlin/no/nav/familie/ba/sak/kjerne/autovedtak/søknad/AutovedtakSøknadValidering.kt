package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.vedtakSimuleringMottakereTilSimuleringDto
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.math.BigDecimal

object AutovedtakSøknadValidering {
    private const val MÅ_HÅNDTERES_MANUELT = "Behandling av søknad må håndteres manuelt."

    fun validerAtVilkårsvurderingErOppfylt(vilkårsvurdering: Vilkårsvurdering) {
        val erVilkårsvurderingOppfylt = vilkårsvurdering.personResultater.flatMap { it.vilkårResultater }.all { it.erOppfylt() }

        if (!erVilkårsvurderingOppfylt) {
            throw AutovedtakMåBehandlesManueltFeil("Vilkårsvurderingen er ikke oppfylt.\n$MÅ_HÅNDTERES_MANUELT")
        }
    }

    fun validerAtBehandlingsresultatErInnvilgetEllerDelvisInnvilget(behandlingsresultat: Behandlingsresultat) {
        val gyldigeBehandlingsresultater = setOf(INNVILGET, DELVIS_INNVILGET)
        if (behandlingsresultat !in gyldigeBehandlingsresultater) {
            throw AutovedtakMåBehandlesManueltFeil("Automatisk behandling av søknad gir behandlingsresultatet '${behandlingsresultat.displayName}'.\n$MÅ_HÅNDTERES_MANUELT")
        }
    }

    fun validerAtKunPersonerFremstiltKravForHarEndringIAndeler(
        behandlingId: Long,
        andelerDenneBehandlingen: Collection<AndelTilkjentYtelse>,
        andelerForrigeBehandling: Collection<AndelTilkjentYtelse>,
        personerFremstiltKravFor: Set<Aktør>,
    ) {
        val endringIAndelerTidslinjer =
            andelerDenneBehandlingen.tilTidslinjerPerAktørOgType().outerJoin(andelerForrigeBehandling.tilTidslinjerPerAktørOgType()) { nyAndel, gammelAndel ->
                when {
                    nyAndel == null && gammelAndel == null -> false
                    nyAndel == null || gammelAndel == null -> true
                    else -> nyAndel.felterHarEndretSegSidenForrigeBehandling(forrigeAndel = gammelAndel)
                }
            }

        val personerMedEndringIAndelerUtenKrav =
            endringIAndelerTidslinjer
                .filterValues { endringTidslinje -> endringTidslinje.tilPerioder().any { it.verdi == true } }
                .keys
                .map { (aktør, _) -> aktør }
                .toSet() - personerFremstiltKravFor

        if (personerMedEndringIAndelerUtenKrav.isNotEmpty()) {
            secureLogger.warn(
                "Automatisk behandling av søknad i behandling $behandlingId gir endring i andeler for personer det ikke er fremstilt krav for. " +
                    "AktørIder: ${personerMedEndringIAndelerUtenKrav.map { it.aktørId }}",
            )
            throw AutovedtakMåBehandlesManueltFeil(
                "Automatisk behandling av søknad gir endring i andeler for ${personerMedEndringIAndelerUtenKrav.size} person(er) det ikke er fremstilt krav for.\n$MÅ_HÅNDTERES_MANUELT",
            )
        }
    }

    fun validerAtSimuleringGirUtbetaling(simulering: List<ØkonomiSimuleringMottaker>) {
        val harIngenUtbetaling = simulering.flatMap { it.økonomiSimuleringPostering }.all { it.beløp.compareTo(BigDecimal.ZERO) == 0 }

        if (harIngenUtbetaling) {
            throw AutovedtakMåBehandlesManueltFeil("Automatisk behandling av søknad fører til ingen utbetaling.\n$MÅ_HÅNDTERES_MANUELT")
        }
    }

    fun validerAtDetIkkeErFeilutbetaling(simulering: List<ØkonomiSimuleringMottaker>) {
        val feilutbetaling = vedtakSimuleringMottakereTilSimuleringDto(simulering).feilutbetaling
        if (feilutbetaling > BigDecimal.ZERO) {
            throw AutovedtakMåBehandlesManueltFeil("Automatisk behandling av søknad fører til feilutbetaling.\n$MÅ_HÅNDTERES_MANUELT")
        }
    }
}
