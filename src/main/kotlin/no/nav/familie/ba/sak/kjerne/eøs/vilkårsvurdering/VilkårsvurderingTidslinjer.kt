package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.common.erUnder18ÅrVilkårTidslinje
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.barn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.søker
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.tidslinje.beskjærEtter
import no.nav.familie.tidslinje.inneholder
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.erUnder18ÅrVilkårTidslinje as familieFellesErUnder18ÅrVilkårTidslinje
import no.nav.familie.tidslinje.Tidslinje as FamilieFellesTidslinje

class VilkårsvurderingTidslinjer(
    vilkårsvurdering: Vilkårsvurdering,
    søkerOgBarn: List<PersonEnkel>,
) {
    private val barna: List<PersonEnkel> = søkerOgBarn.barn()
    private val søker: Aktør = søkerOgBarn.søker().aktør

    internal val barnOgFødselsdatoer = barna.associate { it.aktør to it.fødselsdato }

    private val aktørTilPersonResultater =
        vilkårsvurdering.personResultater.associateBy { it.aktør }

    private val vilkårsresultaterTidslinjeMap =
        aktørTilPersonResultater
            .entries
            .associate { (aktør, personResultat) ->
                aktør to
                    personResultat.vilkårResultater
                        .groupBy { it.vilkårType }
                        .map { it.value.tilVilkårRegelverkResultatTidslinje() }
            }

    private val søkersTidslinje: SøkersTidslinjer =
        SøkersTidslinjer(
            tidslinjer = this,
            aktør = søker,
            fagsakType = vilkårsvurdering.behandling.fagsak.type,
            behandlingUnderkategori = vilkårsvurdering.behandling.underkategori,
        )

    fun søkersTidslinjer(): SøkersTidslinjer = søkersTidslinje

    private val barnasTidslinjer: Map<Aktør, BarnetsTidslinjer> =
        barna
            .map {
                it.aktør to
                    BarnetsTidslinjer(
                        tidslinjer = this,
                        aktør = it.aktør,
                        fagsakType = vilkårsvurdering.behandling.fagsak.type,
                        behandlingUnderkategori = vilkårsvurdering.behandling.underkategori,
                    )
            }.toMap()

    fun forBarn(barn: Person) = barnasTidslinjer[barn.aktør]!!

    fun barnasTidslinjer(): Map<Aktør, BarnetsTidslinjer> = barnasTidslinjer

    class SøkersTidslinjer(
        tidslinjer: VilkårsvurderingTidslinjer,
        aktør: Aktør,
        fagsakType: FagsakType,
        behandlingUnderkategori: BehandlingUnderkategori,
    ) {
        val vilkårsresultatTidslinjer = tidslinjer.vilkårsresultaterTidslinjeMap[aktør] ?: listOf(TomTidslinje())

        private val vilkårsresultatMånedTidslinjer =
            vilkårsresultatTidslinjer.map { it.tilMånedsbasertTidslinjeForVilkårRegelverkResultat() }

        val regelverkResultatTidslinje =
            vilkårsresultatMånedTidslinjer
                .kombinerUtenNull {
                    kombinerVilkårResultaterTilRegelverkResultat(
                        personType = PersonType.SØKER,
                        alleVilkårResultater = it,
                        fagsakType = fagsakType,
                        behandlingUnderkategori = behandlingUnderkategori,
                    )
                }
    }

    class BarnetsTidslinjer(
        tidslinjer: VilkårsvurderingTidslinjer,
        aktør: Aktør,
        fagsakType: FagsakType,
        behandlingUnderkategori: BehandlingUnderkategori,
    ) {
        private val søkersTidslinje = tidslinjer.søkersTidslinje

        val vilkårsresultatTidslinjer: List<Tidslinje<VilkårRegelverkResultat, Dag>> =
            tidslinjer.vilkårsresultaterTidslinjeMap[aktør] ?: listOf(TomTidslinje())

        private val vilkårsresultatMånedTidslinjer: List<Tidslinje<VilkårRegelverkResultat, Måned>> =
            vilkårsresultatTidslinjer.map { it.tilMånedsbasertTidslinjeForVilkårRegelverkResultat() }

        val erUnder18ÅrVilkårTidslinje = erUnder18ÅrVilkårTidslinje(tidslinjer.barnOgFødselsdatoer.getValue(aktør))

        val egetRegelverkResultatTidslinje: Tidslinje<RegelverkResultat, Måned> =
            vilkårsresultatMånedTidslinjer
                .kombinerUtenNull {
                    kombinerVilkårResultaterTilRegelverkResultat(
                        personType = PersonType.BARN,
                        alleVilkårResultater = it,
                        fagsakType = fagsakType,
                        behandlingUnderkategori = behandlingUnderkategori,
                    )
                }.beskjærEtter(erUnder18ÅrVilkårTidslinje)

        val regelverkResultatTidslinje =
            egetRegelverkResultatTidslinje
                .kombinerMed(søkersTidslinje.regelverkResultatTidslinje) { barnetsResultat, søkersResultat ->
                    barnetsResultat.kombinerMed(søkersResultat)
                }
                // Barnets egne tidslinjer kan på dette tidspunktet strekke seg 18 år frem i tid,
                // og mye lenger enn søkers regelverk-tidslinje, som skal være begrensningen. Derfor besjærer vi mot den
                .beskjærEtter(søkersTidslinje.regelverkResultatTidslinje)
    }
}

// TODO: Endre navn til VilkårsvurderingTidslinjer når den andre klassen er slettet
class VilkårsvurderingFamilieFellesTidslinjer(
    vilkårsvurdering: Vilkårsvurdering,
    søkerOgBarn: List<PersonEnkel>,
) {
    private val barna: List<PersonEnkel> = søkerOgBarn.barn()
    private val søker: Aktør = søkerOgBarn.søker().aktør

    private val søkersTidslinje: SøkersTidslinjer =
        SøkersTidslinjer(
            vilkårResultater = vilkårsvurdering.hentPersonResultaterTil(søker.aktørId),
            fagsakType = vilkårsvurdering.behandling.fagsak.type,
            behandlingUnderkategori = vilkårsvurdering.behandling.underkategori,
        )

    private val barnasTidslinjer: Map<Aktør, BarnetsTidslinjer> =
        barna.associate {
            it.aktør to
                BarnetsTidslinjer(
                    barn = it,
                    vilkårResultater = vilkårsvurdering.hentPersonResultaterTil(it.aktør.aktørId),
                    søkersRegelverkResultatTidslinje = søkersTidslinje.regelverkResultatTidslinje,
                    fagsakType = vilkårsvurdering.behandling.fagsak.type,
                    behandlingUnderkategori = vilkårsvurdering.behandling.underkategori,
                )
        }

    fun søkersTidslinje(): SøkersTidslinjer = søkersTidslinje

    fun barnasTidslinjer(): Map<Aktør, BarnetsTidslinjer> = barnasTidslinjer

    class SøkersTidslinjer(
        vilkårResultater: List<VilkårResultat>,
        fagsakType: FagsakType,
        behandlingUnderkategori: BehandlingUnderkategori,
    ) {
        val vilkårsresultatTidslinjer =
            vilkårResultater
                .groupBy { it.vilkårType }
                .map { it.value.tilVilkårRegelverkResultatTidslinjeFamilieFelles() }

        val regelverkResultatTidslinje =
            vilkårsresultatTidslinjer
                .map { it.tilMånedsbasertTidslinjeForVilkårRegelverkResultat() }
                .kombinerUtenNull {
                    kombinerVilkårResultaterTilRegelverkResultat(
                        personType = PersonType.SØKER,
                        alleVilkårResultater = it,
                        fagsakType = fagsakType,
                        behandlingUnderkategori = behandlingUnderkategori,
                    )
                }
    }

    class BarnetsTidslinjer(
        barn: PersonEnkel,
        vilkårResultater: List<VilkårResultat>,
        søkersRegelverkResultatTidslinje: FamilieFellesTidslinje<RegelverkResultat>,
        fagsakType: FagsakType,
        behandlingUnderkategori: BehandlingUnderkategori,
    ) {
        val erUnder18ÅrVilkårTidslinje = familieFellesErUnder18ÅrVilkårTidslinje(barn.fødselsdato)

        val vilkårsresultatTidslinjer =
            vilkårResultater
                .groupBy { it.vilkårType }
                .map { it.value.tilVilkårRegelverkResultatTidslinjeFamilieFelles() }

        val egetRegelverkResultatTidslinje: FamilieFellesTidslinje<RegelverkResultat> =
            vilkårsresultatTidslinjer
                .map { it.tilMånedsbasertTidslinjeForVilkårRegelverkResultat() }
                .kombinerUtenNull {
                    kombinerVilkårResultaterTilRegelverkResultat(
                        personType = PersonType.BARN,
                        alleVilkårResultater = it,
                        fagsakType = fagsakType,
                        behandlingUnderkategori = behandlingUnderkategori,
                    )
                }.beskjærEtter(erUnder18ÅrVilkårTidslinje)

        val regelverkResultatTidslinje =
            egetRegelverkResultatTidslinje
                .kombinerMed(søkersRegelverkResultatTidslinje) { barnetsResultat, søkersResultat ->
                    barnetsResultat.kombinerMed(søkersResultat)
                }
                // Barnets egne tidslinjer kan på dette tidspunktet strekke seg 18 år frem i tid,
                // og mye lenger enn søkers regelverk-tidslinje, som skal være begrensningen. Derfor beskjærer vi mot den
                .beskjærEtter(søkersRegelverkResultatTidslinje)
    }

    fun harBlandetRegelverk(): Boolean =
        søkerHarNasjonalOgFinnesBarnMedEøs() ||
            søkersTidslinje().regelverkResultatTidslinje.inneholder(RegelverkResultat.OPPFYLT_BLANDET_REGELVERK) ||
            barnasTidslinjer().values.any { it.egetRegelverkResultatTidslinje.inneholder(RegelverkResultat.OPPFYLT_BLANDET_REGELVERK) }

    private fun søkerHarNasjonalOgFinnesBarnMedEøs(): Boolean =
        barnasTidslinjer().values.any {
            it.egetRegelverkResultatTidslinje
                .kombinerMed(søkersTidslinje().regelverkResultatTidslinje) { barnRegelverk, søkerRegelverk ->
                    barnRegelverk == RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN && søkerRegelverk == RegelverkResultat.OPPFYLT_NASJONALE_REGLER
                }.inneholder(true)
        }
}
