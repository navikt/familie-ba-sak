package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PreutfyllLovligOppholdService(
    private val pdlRestClient: PdlRestClient,
) {
    fun preutfyllLovligOpphold(vilkårsvurdering: Vilkårsvurdering) {
        vilkårsvurdering.personResultater.forEach { personResultat ->

            val lovligOppholdVilkårResultat = genererLovligOppholdVilkårResultat(personResultat)

            if (lovligOppholdVilkårResultat.isNotEmpty()) {
                personResultat.vilkårResultater.removeIf { it.vilkårType == LOVLIG_OPPHOLD }
                personResultat.vilkårResultater.addAll(lovligOppholdVilkårResultat)
            }
        }
    }

    fun genererLovligOppholdVilkårResultat(personResultat: PersonResultat): Set<VilkårResultat> {
        val erNordiskStatsborgerTidslinje = pdlRestClient.lagErNordiskStatsborgerTidslinje(personResultat)

        val delvilkårPerioder =
            erNordiskStatsborgerTidslinje
                .tilPerioder()
                .map { erNordiskStatsborger ->
                    Periode(
                        verdi =
                            when (erNordiskStatsborger.verdi) {
                                true -> OppfyltDelvilkår("- Norsk/nordisk statsborgerskap")
                                else -> IkkeOppfyltDelvilkår
                            },
                        fom = erNordiskStatsborger.fom ?: finnDatoFørsteBostedsadresseINorge(personResultat),
                        tom = erNordiskStatsborger.tom,
                    )
                }

        return delvilkårPerioder
            .map { periode ->
                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat = periode.verdi.tilResultat(),
                    vilkårType = LOVLIG_OPPHOLD,
                    periodeFom = periode.fom,
                    periodeTom = periode.tom,
                    begrunnelse = "Fylt ut automatisk fra registerdata i PDL\n" + periode.verdi.begrunnelse,
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                    begrunnelseForManuellKontroll = periode.verdi.begrunnelseForManuellKontroll,
                )
            }.toSet()
    }

    fun finnDatoFørsteBostedsadresseINorge(personResultat: PersonResultat): LocalDate? =
        pdlRestClient
            .hentBostedsadresserForPerson(fødselsnummer = personResultat.aktør.aktivFødselsnummer())
            .filter { it.vegadresse != null || it.matrikkeladresse != null || it.ukjentBosted != null }
            .mapNotNull { it.gyldigFraOgMed }
            .minByOrNull { it }
}
