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
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.springframework.stereotype.Service
import java.time.LocalDate
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
        val alleBostedsAdresserForPerson =
            pdlRestClient
                .hentBostedsadresserForPerson(fødselsnummer = personResultat.aktør.aktivFødselsnummer())
                .sortedBy { it.gyldigFraOgMed }

        val harBostedsadresseINorgeTidslinje =
            when (alleBostedsAdresserForPerson.size) {
                0 -> {
                    tomTidslinje()
                }

                1 -> {
                    val bostedsadresse = alleBostedsAdresserForPerson.single()
                    Periode(
                        verdi = harBostedsAdresseINorge(bostedsadresse),
                        fom = bostedsadresse.gyldigFraOgMed,
                        tom = bostedsadresse.gyldigTilOgMed,
                    ).tilTidslinje()
                }

                else -> {
                    val erBosattINorgePerioder =
                        alleBostedsAdresserForPerson
                            .zipWithNext { denne, neste ->
                                Periode(
                                    verdi = harBostedsAdresseINorge(denne),
                                    fom = denne.gyldigFraOgMed,
                                    tom = denne.gyldigTilOgMed ?: neste.gyldigFraOgMed,
                                )
                            }.toMutableList()

                    // zipWithNext tar ikke med den siste perioden så må legge den til her
                    erBosattINorgePerioder.add(
                        Periode(
                            verdi = harBostedsAdresseINorge(alleBostedsAdresserForPerson.last()),
                            fom = alleBostedsAdresserForPerson.last().gyldigFraOgMed,
                            tom = alleBostedsAdresserForPerson.last().gyldigTilOgMed,
                        ),
                    )

                    erBosattINorgePerioder
                        .tilTidslinje()
                }
            }

        return harBostedsadresseINorgeTidslinje
            .tilPerioder()
            .map { periode ->

                val oppfyllerVilkår = periode.verdi == true && ChronoUnit.MONTHS.between(periode.fom, periode.tom ?: LocalDate.MAX) >= 12

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
