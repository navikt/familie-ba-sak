package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.iNordiskLand
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.springframework.stereotype.Service

@Service
class PreutfyllLovligOppholdService(
    private val pdlRestClient: PdlRestClient,
) {
    fun preutfyllLovligOpphold(vilkårsvurdering: Vilkårsvurdering) {
        if (vilkårsvurdering.behandling.kategori == BehandlingKategori.EØS) return

        vilkårsvurdering.personResultater.forEach { personResultat ->

            val lovligOppholdVilkårResultat = genererLovligOppholdVilkårResultat(personResultat)

            if (lovligOppholdVilkårResultat.isNotEmpty()) {
                personResultat.vilkårResultater.removeIf { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
                personResultat.vilkårResultater.addAll(lovligOppholdVilkårResultat)
            }
        }
    }

    fun genererLovligOppholdVilkårResultat(personResultat: PersonResultat): Set<VilkårResultat> {
        val erNorskEllerNordiskStatsborgerTidslinje = lagErNorskNordiskStatsborgerTidslinje(personResultat)

        val delvilkårPerioder =
            erNorskEllerNordiskStatsborgerTidslinje
                .tilPerioder()
                .map { erNorskEllerNordiskStataborger ->
                    Periode(
                        verdi =
                            if (erNorskEllerNordiskStataborger.verdi == true) {
                                OppfyltDelvilkår("- Norsk/nordisk statsborgerskap")
                            } else {
                                IkkeOppfyltDelvilkår
                            },
                        fom = erNorskEllerNordiskStataborger.fom,
                        tom = erNorskEllerNordiskStataborger.tom,
                    )
                }

        return delvilkårPerioder
            .map { periode ->
                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat =
                        if (periode.verdi is OppfyltDelvilkår) {
                            Resultat.OPPFYLT
                        } else {
                            Resultat.IKKE_OPPFYLT
                        },
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    periodeFom = periode.fom,
                    periodeTom = periode.tom,
                    begrunnelse = "Fylt ut automatisk fra registerdata i PDL \n" + (periode.verdi.begrunnelse),
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSet()
    }

    fun lagErNorskNordiskStatsborgerTidslinje(personResultat: PersonResultat): Tidslinje<Boolean> {
        val statsborgerskapGruppertPåNavn =
            pdlRestClient
                .hentStatsborgerskap(personResultat.aktør, historikk = true)
                .groupBy { it.land }

        return statsborgerskapGruppertPåNavn.values
            .map { statsborgerskapSammeLand ->
                statsborgerskapSammeLand
                    .map { Periode(it, it.gyldigFraOgMed, it.gyldigTilOgMed) }
                    .tilTidslinje()
            }.kombiner { iterable -> iterable.any { it.iNordiskLand() } }
    }
}
