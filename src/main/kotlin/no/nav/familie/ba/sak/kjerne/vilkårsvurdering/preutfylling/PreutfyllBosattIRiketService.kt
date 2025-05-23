package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.springframework.stereotype.Service
import java.time.LocalDate.MAX
import java.time.temporal.ChronoUnit

@Service
class PreutfyllBosattIRiketService(
    private val pdlRestClient: PdlRestClient,
) {
    fun prefutfyllBosattIRiket(vilkårsvurdering: Vilkårsvurdering) {
        vilkårsvurdering.personResultater.forEach { personResultat ->

            val bosattIRiketVilkårResultat = genererBosattIRiketVilkårResultat(personResultat)

            if (bosattIRiketVilkårResultat.isNotEmpty()) {
                personResultat.vilkårResultater.removeIf { it.vilkårType == Vilkår.BOSATT_I_RIKET }
                personResultat.vilkårResultater.addAll(bosattIRiketVilkårResultat)
            }
        }
    }

    fun genererBosattIRiketVilkårResultat(personResultat: PersonResultat): Set<VilkårResultat> {
        val alleBostedsadresserForPerson =
            pdlRestClient
                .hentBostedsadresserForPerson(fødselsnummer = personResultat.aktør.aktivFødselsnummer())
                .sortedBy { it.gyldigFraOgMed }

        val harBostedsadresseINorgeTidslinje =
            alleBostedsadresserForPerson
                .windowed(size = 2, step = 1, partialWindows = true) {
                    val denne = it.first()
                    val neste = it.getOrNull(1)
                    Periode(
                        verdi = harBostedsAdresseINorge(denne),
                        fom = denne.gyldigFraOgMed,
                        tom = denne.gyldigTilOgMed ?: neste?.gyldigFraOgMed?.minusDays(1),
                    )
                }.tilTidslinje()

        return harBostedsadresseINorgeTidslinje
            .tilPerioder()
            .map { periode ->

                val oppfyllerVilkår =
                    periode.verdi == true && ChronoUnit.MONTHS.between(periode.fom, periode.tom ?: MAX) >= 12

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat = if (oppfyllerVilkår) Resultat.OPPFYLT else Resultat.IKKE_OPPFYLT,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = periode.fom,
                    periodeTom = periode.tom,
                    begrunnelse = "Fylt inn automatisk fra registerdata i PDL",
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSet()
    }

    private fun harBostedsAdresseINorge(bostedsadresse: Bostedsadresse): Boolean = bostedsadresse.vegadresse != null || bostedsadresse.matrikkeladresse != null || bostedsadresse.ukjentBosted != null
}
