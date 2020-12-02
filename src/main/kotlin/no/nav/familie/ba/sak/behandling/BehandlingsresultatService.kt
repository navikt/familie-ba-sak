package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import org.springframework.stereotype.Service
import java.util.*

@Service
class BehandlingsresultatService(
        private val behandlingService: BehandlingService,
        private val søknadGrunnlagService: SøknadGrunnlagService,
        private val beregningService: BeregningService
) {

    fun utledBehandlingsresultat(behandlingId: Long): List<Krav> {
        val behandling = behandlingService.hent(behandlingId = behandlingId)
        val forrigeBehandling = behandlingService.hentForrigeBehandlingSomErIverksatt(behandling)

        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingId)
        val forrigeTilkjentYtelse: TilkjentYtelse? =
                if (forrigeBehandling != null) beregningService.hentOptionalTilkjentYtelseForBehandling(behandlingId = forrigeBehandling.id)
                else null


        val krav: List<Krav> = BehandlingsresultatUtil.utledKrav(
                søknadDTO = søknadGrunnlagService.hentAktiv(behandlingId = behandlingId)?.hentSøknadDto(),
                forrigeAndelerTilkjentYtelse = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.toList() ?: emptyList()
        )

        return BehandlingsresultatUtil.utledKravMedResultat(
                krav = krav.toList(),
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.toList(),
                forrigeAndelerTilkjentYtelse = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.toList() ?: emptyList()
        )
    }
}

data class Krav(
        val personIdent: String,
        val ytelseType: YtelseType,
        val søknadskrav: Boolean,
        val resultatType: BehandlingResultatType = BehandlingResultatType.IKKE_VURDERT,
) {

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val entitet: Krav = other as Krav
        return Objects.equals(hashCode(), entitet.hashCode())
    }

    /**
     * Vi sjekker likhet på person og ytelsetype.
     * Søknadskrav trumfer, men håndteres ikke av equals/hashcode.
     */
    override fun hashCode(): Int {
        return Objects.hash(personIdent, ytelseType)
    }
}

object BehandlingsresultatUtil {

    // Bygg listen med "krav". TODO = finn et mer passende navn for krav ettersom det er en blanding av krav og eksisterende ytelse
    fun utledKrav(søknadDTO: SøknadDTO?,
                  forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<Krav> {
        val krav: MutableSet<Krav> =
                søknadDTO?.barnaMedOpplysninger?.filter { it.inkludertISøknaden }?.map {
                    Krav(personIdent = it.ident,
                         ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                         søknadskrav = true)
                }?.toMutableSet() ?: mutableSetOf()

        forrigeAndelerTilkjentYtelse.forEach {
            val nyttKrav = Krav(
                    personIdent = it.personIdent,
                    ytelseType = it.type,
                    søknadskrav = false
            )

            if (!krav.contains(krav)) {
                krav.add(nyttKrav)
            }
        }

        return krav.toList()
    }

    fun utledKravMedResultat(krav: List<Krav>,
                             forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                             andelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<Krav> {
        return krav.map { enkeltKrav: Krav ->
            val andelerForBarn = andelerTilkjentYtelse.filter { andel -> andel.personIdent == enkeltKrav.personIdent }
            val forrigeAndelerForBarn =
                    forrigeAndelerTilkjentYtelse.filter { andel -> andel.personIdent == enkeltKrav.personIdent }

            val andelerTidslinje = LocalDateTimeline(andelerForBarn.map {
                LocalDateSegment(
                        it.stønadFom.førsteDagIInneværendeMåned(),
                        it.stønadTom.sisteDagIInneværendeMåned(),
                        it
                )
            })
            val forrigeAndelerTidslinje = LocalDateTimeline(forrigeAndelerForBarn.map {
                LocalDateSegment(
                        it.stønadFom.førsteDagIInneværendeMåned(),
                        it.stønadTom.sisteDagIInneværendeMåned(),
                        it
                )
            })

            // Økning eller innvilgelse
            val nyeAndeler = andelerTidslinje.disjoint(forrigeAndelerTidslinje)

            // Reduksjon
            val fjernedeAndeler = forrigeAndelerTidslinje.disjoint(andelerTidslinje)

            if (nyeAndeler.isEmpty) {
                enkeltKrav.copy(
                        resultatType = if (enkeltKrav.søknadskrav) BehandlingResultatType.AVSLÅTT else BehandlingResultatType.INGEN_ENDRING
                )
            } else {
                enkeltKrav.copy(
                        resultatType = if (enkeltKrav.søknadskrav) BehandlingResultatType.INNVILGET else BehandlingResultatType.ENDRING,
                )

            }
        }
    }
}
