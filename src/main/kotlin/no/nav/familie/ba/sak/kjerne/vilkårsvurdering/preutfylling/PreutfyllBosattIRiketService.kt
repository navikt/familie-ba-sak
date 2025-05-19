package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlBostedsadresse
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
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

            personResultat.vilkårResultater.removeIf { it.vilkårType == Vilkår.BOSATT_I_RIKET }
            personResultat.vilkårResultater.addAll(bosattIRiketVilkårResultat)
        }
    }

    fun genererBosattIRiketVilkårResultat(personResultat: PersonResultat): Set<VilkårResultat> {
        val alleBostedsAdresserForPerson =
            pdlRestClient
                .hentBostedsadresserForPerson(aktør = personResultat.aktør)
                .bostedsadresse
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
                        fom = bostedsadresse.gyldigFraOgMed.toLocalDate(),
                        tom = bostedsadresse.gyldigTilOgMed?.toLocalDate(),
                    ).tilTidslinje()
                }

                else -> {
                    val erBosattINorgePerioder =
                        alleBostedsAdresserForPerson
                            .zipWithNext { denne, neste ->
                                Periode(
                                    verdi = harBostedsAdresseINorge(denne),
                                    fom = denne.gyldigFraOgMed.toLocalDate(),
                                    tom = denne.gyldigTilOgMed?.toLocalDate() ?: neste.gyldigFraOgMed.toLocalDate(),
                                )
                            }.toMutableList()

                    // zipWithNext tar ikke med den siste perioden så må legge den til her
                    erBosattINorgePerioder.add(
                        Periode(
                            verdi = harBostedsAdresseINorge(alleBostedsAdresserForPerson.last()),
                            fom = alleBostedsAdresserForPerson.last().gyldigFraOgMed.toLocalDate(),
                            tom = alleBostedsAdresserForPerson.last().gyldigTilOgMed?.toLocalDate(),
                        ),
                    )

                    erBosattINorgePerioder
                        .tilTidslinje()
                }
            }

        return harBostedsadresseINorgeTidslinje
            .tilPerioder()
            .filter { it.verdi == true && ChronoUnit.MONTHS.between(it.fom, it.tom ?: LocalDate.MAX) >= 12 }
            .map { periode ->
                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = periode.fom,
                    periodeTom = periode.tom,
                    begrunnelse = "Fylt inn automatisk fra registerdata i PDL",
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSet()
    }

    private fun harBostedsAdresseINorge(pdlBostedsAdresse: PdlBostedsadresse): Boolean = pdlBostedsAdresse.vegadresse != null || pdlBostedsAdresse.matrikkeladresse != null || pdlBostedsAdresse.ukjentBosted != null
}
