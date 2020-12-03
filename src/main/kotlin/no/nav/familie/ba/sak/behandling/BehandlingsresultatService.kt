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

/**
 * TODO finn et mer passende navn enn krav.
 * Krav i denne sammenheng er både krav fra søker, men også "krav" fra forrige behandling som kan ha endret seg.
 * På en måte er alt krav fra søker, men "kravene" fra forrige behandling kan stamme fra en annen søknad.
 */
data class Krav(
        val personIdent: String,
        val ytelseType: YtelseType,
        val erSøknadskrav: Boolean,
        val resultatTyper: List<BehandlingResultatType> = emptyList()
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

    /**
     * Metode for å utlede kravene for å utlede behandlingsresultat per krav.
     * Metoden finner kravene som ble stilt i søknaden,
     * samt ytelsestypene per person fra forrige behandling.
     */
    fun utledKrav(søknadDTO: SøknadDTO?,
                  forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<Krav> {
        val krav: MutableSet<Krav> =
                søknadDTO?.barnaMedOpplysninger?.filter { it.inkludertISøknaden }?.map {
                    Krav(personIdent = it.ident,
                         ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                         erSøknadskrav = true)
                }?.toMutableSet() ?: mutableSetOf()

        forrigeAndelerTilkjentYtelse.forEach {
            val nyttKrav = Krav(
                    personIdent = it.personIdent,
                    ytelseType = it.type,
                    erSøknadskrav = false
            )

            if (!krav.contains(nyttKrav)) {
                krav.add(nyttKrav)
            }
        }

        return krav.toList()
    }

    fun utledKravMedResultat(krav: List<Krav>,
                             forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
                             andelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<Krav> {
        return krav.map { enkeltKrav: Krav ->
            val andeler = andelerTilkjentYtelse.filter { andel -> andel.personIdent == enkeltKrav.personIdent }
            val forrigeAndeler =
                    forrigeAndelerTilkjentYtelse.filter { andel -> andel.personIdent == enkeltKrav.personIdent }

            val forrigeAndelerTidslinje = LocalDateTimeline(forrigeAndeler.map {
                LocalDateSegment(
                        it.stønadFom.førsteDagIInneværendeMåned(),
                        it.stønadTom.sisteDagIInneværendeMåned(),
                        it
                )
            })
            val andelerTidslinje = LocalDateTimeline(andeler.map {
                LocalDateSegment(
                        it.stønadFom.førsteDagIInneværendeMåned(),
                        it.stønadTom.sisteDagIInneværendeMåned(),
                        it
                )
            })

            val segmenterLagtTil = andelerTidslinje.disjoint(forrigeAndelerTidslinje)
            val segmenterFjernet = forrigeAndelerTidslinje.disjoint(andelerTidslinje)

            val resultatTyper = mutableListOf<BehandlingResultatType>()
            if (erAvslagPåSøknad(enkeltKrav = enkeltKrav, segmenterLagtTil = segmenterLagtTil)) {
                resultatTyper.add(BehandlingResultatType.AVSLÅTT)
            }

            if (erInnvilgetSøknad(enkeltKrav = enkeltKrav, segmenterLagtTil = segmenterLagtTil)) {
                resultatTyper.add(BehandlingResultatType.INNVILGET)
            }

            if (erYtelsenOpphørt(andeler = andeler)) {
                resultatTyper.add(BehandlingResultatType.OPPHØRT)
            }

            if (erYtelsenEndretTilbakeITid(enkeltKrav = enkeltKrav,
                                           segmenterLagtTil = segmenterLagtTil,
                                           segmenterFjernet = segmenterFjernet)) {
                resultatTyper.add(BehandlingResultatType.ENDRING)
            }

            if (erYtelsenFortsattInnvilget(forrigeAndeler = forrigeAndeler, andeler = andeler)) {
                resultatTyper.add(BehandlingResultatType.FORTSATT_INNVILGET)
            }

            enkeltKrav.copy(
                    resultatTyper = resultatTyper.toList()
            )
        }
    }

    private fun erAvslagPåSøknad(enkeltKrav: Krav,
                                 segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) = enkeltKrav.erSøknadskrav && segmenterLagtTil.isEmpty

    private fun erInnvilgetSøknad(enkeltKrav: Krav,
                                  segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>) = enkeltKrav.erSøknadskrav && !segmenterLagtTil.isEmpty

    private fun erYtelsenOpphørt(andeler: List<AndelTilkjentYtelse>) = andeler.isNotEmpty() && andeler.none { it.erLøpende() }

    private fun erYtelsenFortsattInnvilget(forrigeAndeler: List<AndelTilkjentYtelse>,
                                           andeler: List<AndelTilkjentYtelse>) = forrigeAndeler.isNotEmpty() && forrigeAndeler.any { it.erLøpende() } && andeler.any { it.erLøpende() }

    private fun erYtelsenEndretTilbakeITid(enkeltKrav: Krav,
                                           segmenterLagtTil: LocalDateTimeline<AndelTilkjentYtelse>,
                                           segmenterFjernet: LocalDateTimeline<AndelTilkjentYtelse>) = !enkeltKrav.erSøknadskrav && (erEndringerTilbakeITid(
            segmenterLagtTil) || erEndringerTilbakeITid(segmenterFjernet))

    private fun erEndringerTilbakeITid(andeler: LocalDateTimeline<AndelTilkjentYtelse>) = !andeler.isEmpty && andeler.any { !it.value.erLøpende() }
}
