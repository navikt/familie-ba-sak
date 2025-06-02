package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDate.MAX
import java.time.temporal.ChronoUnit

@Service
class PreutfyllBosattIRiketService(
    private val pdlRestClient: PdlRestClient,
    private val søknadService: SøknadService,
    private val persongrunnlagService: PersongrunnlagService,
) {
    fun prefutfyllBosattIRiket(vilkårsvurdering: Vilkårsvurdering) {
        if (vilkårsvurdering.behandling.kategori == BehandlingKategori.EØS) return

        val eldsteBarnsFødselsdato =
            persongrunnlagService
                .hentAktivThrows(vilkårsvurdering.behandling.id)
                .barna
                .minOfOrNull { it.fødselsdato } ?: LocalDate.MIN

        vilkårsvurdering.personResultater.forEach { personResultat ->

            val bosattIRiketVilkårResultat = genererBosattIRiketVilkårResultat(personResultat, eldsteBarnsFødselsdato)

            if (bosattIRiketVilkårResultat.isNotEmpty()) {
                personResultat.vilkårResultater.removeIf { it.vilkårType == Vilkår.BOSATT_I_RIKET }
                personResultat.vilkårResultater.addAll(bosattIRiketVilkårResultat)
            }
        }
    }

    fun genererBosattIRiketVilkårResultat(
        personResultat: PersonResultat,
        eldsteBarnsFødselsdato: LocalDate = LocalDate.MIN,
    ): Set<VilkårResultat> {
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
                .beskjærFraOgMed(eldsteBarnsFødselsdato)

        return harBostedsadresseINorgeTidslinje
            .tilPerioder()
            .map { erBosattINorgePeriode ->

                val oppfyllerVilkår =
                    erBosattINorgePeriode.verdi == true && (erPeriodeMinst12Måneder(erBosattINorgePeriode) || planleggerÅBoINorgeNeste12Mnd(personResultat))

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat = if (oppfyllerVilkår) Resultat.OPPFYLT else Resultat.IKKE_OPPFYLT,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = erBosattINorgePeriode.fom,
                    periodeTom = erBosattINorgePeriode.tom,
                    begrunnelse = "Fylt inn automatisk fra registerdata i PDL",
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSet()
    }

    private fun erPeriodeMinst12Måneder(periode: Periode<Boolean?>): Boolean = ChronoUnit.MONTHS.between(periode.fom, periode.tom ?: MAX) >= 12

    private fun planleggerÅBoINorgeNeste12Mnd(personResultat: PersonResultat): Boolean {
        val søknad = søknadService.hentSøknad(behandlingId = personResultat.vilkårsvurdering.behandling.id)
        val planleggerÅBoNeste12Mnd =
            if (personResultat.erSøkersResultater()) {
                søknad?.søker?.planleggerÅBoINorge12Mnd
            } else {
                søknad?.barn?.find { it.fnr == personResultat.aktør.aktivFødselsnummer() }?.planleggerÅBoINorge12Mnd
            }
        return planleggerÅBoNeste12Mnd == true
    }

    private fun harBostedsAdresseINorge(bostedsadresse: Bostedsadresse): Boolean = bostedsadresse.vegadresse != null || bostedsadresse.matrikkeladresse != null || bostedsadresse.ukjentBosted != null
}
