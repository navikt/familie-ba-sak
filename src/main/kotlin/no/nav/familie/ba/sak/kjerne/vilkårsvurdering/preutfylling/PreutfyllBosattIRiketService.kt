package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.iNordiskLand
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.task.dto.AktørId
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.omfatter
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.springframework.stereotype.Service
import java.time.LocalDate
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
        val erBosattINorgeTidslinje = lagErBosattINorgeTidslinje(personResultat)

        val erNordiskStatsborgerTidslinje = lagErNordiskStatsborgerTidslinje(personResultat)

        val erBosattOgHarNordiskStatsborgerskapTidslinje =
            erNordiskStatsborgerTidslinje.kombinerMed(erBosattINorgeTidslinje) { erNordisk, erBosatt ->
                val nordiskOgBosatt = erNordisk == true && erBosatt == true
                if (nordiskOgBosatt) {
                    Delvilkår.OppfyltDelvilkår("- Norsk/nordisk statsborgerskap")
                } else {
                    Delvilkår.IkkeOppfyltDelvilkår
                }
            }

        val erØvrigeKravForBosattIRiketOppfyltTidslinje = lagErØvrigeKravForBosattIRiketOppfyltTidslinje(erBosattINorgeTidslinje, personResultat)

        val erBosattIRiketTidslinje =
            erØvrigeKravForBosattIRiketOppfyltTidslinje
                .kombinerMed(erBosattOgHarNordiskStatsborgerskapTidslinje) { erØvrigeKravOppfylt, erNordiskOgBosatt ->
                    val oppfylt = erØvrigeKravOppfylt?.erOppfylt == true || erNordiskOgBosatt?.erOppfylt == true

                    val kommentar = listOf(erNordiskOgBosatt, erØvrigeKravOppfylt).mapNotNull { it?.begrunnelse?.takeIf { kommentar -> kommentar.isNotBlank() } }.joinToString("\n")

                    if (oppfylt) {
                        Delvilkår.OppfyltDelvilkår(kommentar)
                    } else {
                        Delvilkår.IkkeOppfyltDelvilkår
                    }
                }.beskjærFraOgMed(eldsteBarnsFødselsdato)

        return erBosattIRiketTidslinje
            .tilPerioder()
            .map { erBosattINorgePeriode ->
                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat = if (erBosattINorgePeriode.verdi?.erOppfylt == true) Resultat.OPPFYLT else Resultat.IKKE_OPPFYLT,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = erBosattINorgePeriode.fom,
                    periodeTom = erBosattINorgePeriode.tom,
                    begrunnelse = "Fylt ut automatisk fra registerdata i PDL \n" + erBosattINorgePeriode.verdi?.begrunnelse,
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSet()
    }

    private fun lagErØvrigeKravForBosattIRiketOppfyltTidslinje(
        erBosattINorgeTidslinje: Tidslinje<Boolean>,
        personResultat: PersonResultat,
    ): Tidslinje<Delvilkår> =
        erBosattINorgeTidslinje
            .tilPerioder()
            .map { erBosattINorgePeriode ->
                Periode(
                    verdi =
                        if (erBosattINorgePeriode.verdi == true) {
                            sjekkØvrigeKravForPeriode(erBosattINorgePeriode, personResultat)
                        } else {
                            Delvilkår.IkkeOppfyltDelvilkår
                        },
                    fom = erBosattINorgePeriode.fom,
                    tom = erBosattINorgePeriode.tom,
                )
            }.tilTidslinje()

    private fun sjekkØvrigeKravForPeriode(
        erBosattINorgePeriode: Periode<Boolean?>,
        personResultat: PersonResultat,
    ): Delvilkår =
        when {
            erBosattINorgePeriode.erMinst12Måneder() ->
                Delvilkår.OppfyltDelvilkår("- Norsk bostedsadresse i minst 12 måneder.")

            erFødselsdatoIPeriode(personResultat.vilkårsvurdering.behandling.id, personResultat.aktør.aktørId, erBosattINorgePeriode) ->
                Delvilkår.OppfyltDelvilkår("- Bosatt i Norge siden fødsel.")

            erBosattINorgePeriode.omfatter(LocalDate.now()) && erOppgittAtPlanleggerÅBoINorge12Måneder(personResultat) ->
                Delvilkår.OppfyltDelvilkår("- Oppgitt i søknad at planlegger å bo i Norge i minst 12 måneder.")

            else -> Delvilkår.IkkeOppfyltDelvilkår
        }

    private fun Periode<*>.erMinst12Måneder(): Boolean = ChronoUnit.MONTHS.between(fom, tom ?: LocalDate.now()) >= 12

    private fun erOppgittAtPlanleggerÅBoINorge12Måneder(personResultat: PersonResultat): Boolean {
        val søknad =
            søknadService.finnSøknad(behandlingId = personResultat.vilkårsvurdering.behandling.id)
                ?: return false
        val planleggerÅBoNeste12Mnd =
            if (personResultat.erSøkersResultater()) {
                søknad.søker.planleggerÅBoINorge12Mnd
            } else {
                søknad.barn.find { it.fnr == personResultat.aktør.aktivFødselsnummer() }?.planleggerÅBoINorge12Mnd
            }
        return planleggerÅBoNeste12Mnd == true
    }

    private fun harBostedsAdresseINorge(bostedsadresse: Bostedsadresse): Boolean = bostedsadresse.vegadresse != null || bostedsadresse.matrikkeladresse != null || bostedsadresse.ukjentBosted != null

    private fun erFødselsdatoIPeriode(
        behandlingId: Long,
        aktørId: AktørId,
        erBosattINorgePeriode: Periode<Boolean?>,
    ): Boolean {
        val fødselsdato =
            persongrunnlagService
                .hentAktivThrows(behandlingId)
                .søkerOgBarn
                .find { it.aktør.aktørId == aktørId }
                ?.fødselsdato ?: throw Feil("Finner ikke barn med aktørId $aktørId i persongrunnlag for behandlingId $behandlingId")
        return erBosattINorgePeriode.omfatter(fødselsdato)
    }

    private fun lagErNordiskStatsborgerTidslinje(personResultat: PersonResultat): Tidslinje<Boolean> {
        val statsborgerskapGruppertPåLand =
            pdlRestClient
                .hentStatsborgerskap(personResultat.aktør, historikk = true)
                .groupBy { it.land }

        return statsborgerskapGruppertPåLand.values
            .map { statsborgerskapFraSammeLand ->
                statsborgerskapFraSammeLand
                    .map { Periode(it, it.gyldigFraOgMed, it.gyldigTilOgMed) }
                    .tilTidslinje()
            }.kombiner { iterable -> iterable.any { it.iNordiskLand() } }
    }

    private fun lagErBosattINorgeTidslinje(personResultat: PersonResultat): Tidslinje<Boolean> {
        val alleBostedsadresserForPerson =
            pdlRestClient
                .hentBostedsadresserForPerson(fødselsnummer = personResultat.aktør.aktivFødselsnummer())
                .sortedBy { it.gyldigFraOgMed }

        return alleBostedsadresserForPerson
            .windowed(size = 2, step = 1, partialWindows = true) {
                val denne = it.first()
                val neste = it.getOrNull(1)

                Periode(
                    verdi = harBostedsAdresseINorge(denne),
                    fom = denne.gyldigFraOgMed,
                    tom = denne.gyldigTilOgMed ?: neste?.gyldigFraOgMed?.minusDays(1),
                )
            }.tilTidslinje()
    }
}

private sealed class Delvilkår {
    abstract val erOppfylt: Boolean
    abstract val begrunnelse: String

    data class OppfyltDelvilkår(
        override val begrunnelse: String,
    ) : Delvilkår() {
        override val erOppfylt: Boolean = true
    }

    data object IkkeOppfyltDelvilkår : Delvilkår() {
        override val erOppfylt: Boolean = false
        override val begrunnelse: String = ""
    }
}
