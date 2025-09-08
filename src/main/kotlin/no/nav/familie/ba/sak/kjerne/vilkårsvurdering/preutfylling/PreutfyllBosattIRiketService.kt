package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_FRA_SØKNAD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.ba.sak.task.dto.AktørId
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.omfatter
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrer
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class PreutfyllBosattIRiketService(
    private val pdlRestClient: SystemOnlyPdlRestClient,
    private val søknadService: SøknadService,
    private val persongrunnlagService: PersongrunnlagService,
) {
    fun preutfyllBosattIRiket(vilkårsvurdering: Vilkårsvurdering) {
        val identer = vilkårsvurdering.personResultater.map { it.aktør.aktivFødselsnummer() }
        val bostedsadresser = pdlRestClient.hentBostedsadresseOgDeltBostedForPersoner(identer)

        vilkårsvurdering.personResultater.forEach { personResultat ->
            val fødselsdatoForBeskjæring = finnFødselsdatoForBeskjæring(personResultat)
            val bostedsadresserForPerson = Adresser.opprettFra(bostedsadresser[personResultat.aktør.aktivFødselsnummer()])

            val bosattIRiketVilkårResultat =
                genererBosattIRiketVilkårResultat(
                    personResultat = personResultat,
                    fødselsdatoForBeskjæring = fødselsdatoForBeskjæring,
                    adresserForPerson = bostedsadresserForPerson,
                )

            if (bosattIRiketVilkårResultat.isNotEmpty()) {
                personResultat.vilkårResultater.removeIf { it.vilkårType == BOSATT_I_RIKET }
                personResultat.vilkårResultater.addAll(bosattIRiketVilkårResultat)
            }
        }
    }

    fun genererBosattIRiketVilkårResultat(
        personResultat: PersonResultat,
        fødselsdatoForBeskjæring: LocalDate = LocalDate.MIN,
        adresserForPerson: Adresser,
    ): Set<VilkårResultat> {
        val erBosattINorgeTidslinje = lagErBosattINorgeTidslinje(adresserForPerson)
        val erBosattIFinnmarkEllerNordTromsTidslinje = lagErBosattIFinnmarkEllerNordTromsTidslinje(adresserForPerson)
        val erNordiskStatsborgerTidslinje = pdlRestClient.lagErNordiskStatsborgerTidslinje(personResultat)

        val erBosattOgHarNordiskStatsborgerskapTidslinje =
            erNordiskStatsborgerTidslinje.kombinerMed(erBosattINorgeTidslinje, erBosattIFinnmarkEllerNordTromsTidslinje) { erNordisk, erBosattINorge, erBosattIFinnmarkEllerNordTroms ->
                val nordiskOgBosatt = erNordisk == true && erBosattINorge == true
                val utdypendeVilkårsvurderinger = if (erBosattIFinnmarkEllerNordTroms == true) listOf(BOSATT_I_FINNMARK_NORD_TROMS) else emptyList()
                if (nordiskOgBosatt) {
                    OppfyltDelvilkår(begrunnelse = "- Norsk/nordisk statsborgerskap", utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger)
                } else {
                    IkkeOppfyltDelvilkår
                }
            }

        val erØvrigeKravForBosattIRiketOppfyltTidslinje = lagErØvrigeKravForBosattIRiketOppfyltTidslinje(erBosattINorgeTidslinje, personResultat)

        val førsteBosattINorgeDato = erBosattINorgeTidslinje.filtrer { it == true }.startsTidspunkt

        val erBosattIRiketTidslinje =
            erBosattOgHarNordiskStatsborgerskapTidslinje
                .kombinerMed(erØvrigeKravForBosattIRiketOppfyltTidslinje) { erNordiskOgBosatt, erØvrigeKravOppfylt ->
                    when {
                        erNordiskOgBosatt is OppfyltDelvilkår -> erNordiskOgBosatt
                        erØvrigeKravOppfylt is OppfyltDelvilkår -> erØvrigeKravOppfylt
                        else -> IkkeOppfyltDelvilkår
                    }
                }.beskjærFraOgMed(maxOf(fødselsdatoForBeskjæring, førsteBosattINorgeDato))

        return erBosattIRiketTidslinje
            .tilPerioderIkkeNull()
            .map { erBosattINorgePeriode ->
                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat = erBosattINorgePeriode.verdi.tilResultat(),
                    vilkårType = BOSATT_I_RIKET,
                    periodeFom = erBosattINorgePeriode.fom,
                    periodeTom = erBosattINorgePeriode.tom,
                    begrunnelse = PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + erBosattINorgePeriode.verdi.begrunnelse,
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                    begrunnelseForManuellKontroll = erBosattINorgePeriode.verdi.begrunnelseForManuellKontroll,
                    utdypendeVilkårsvurderinger = erBosattINorgePeriode.verdi.utdypendeVilkårsvurderinger,
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
                        when (erBosattINorgePeriode.verdi) {
                            true -> sjekkØvrigeKravForPeriode(erBosattINorgePeriode, personResultat)
                            else -> IkkeOppfyltDelvilkår
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
                OppfyltDelvilkår("- Norsk bostedsadresse i minst 12 måneder.")

            erFødselsdatoIPeriode(personResultat.vilkårsvurdering.behandling.id, personResultat.aktør.aktørId, erBosattINorgePeriode) ->
                OppfyltDelvilkår("- Bosatt i Norge siden fødsel.")

            erBosattINorgePeriode.omfatter(LocalDate.now()) && erOppgittAtPlanleggerÅBoINorge12Måneder(personResultat) ->
                OppfyltDelvilkår("- Oppgitt i søknad at planlegger å bo i Norge i minst 12 måneder.", INFORMASJON_FRA_SØKNAD)

            else -> IkkeOppfyltDelvilkår
        }

    private fun Periode<*>.erMinst12Måneder(): Boolean = ChronoUnit.MONTHS.between(fom, tom ?: LocalDate.now()) >= 12

    private fun erOppgittAtPlanleggerÅBoINorge12Måneder(personResultat: PersonResultat): Boolean {
        val søknad = søknadService.finnSøknad(behandlingId = personResultat.vilkårsvurdering.behandling.id) ?: return false
        return if (personResultat.erSøkersResultater()) {
            søknad.søker.planleggerÅBoINorge12Mnd
        } else {
            søknad.barn.find { it.fnr == personResultat.aktør.aktivFødselsnummer() }?.planleggerÅBoINorge12Mnd ?: false
        }
    }

    fun finnFødselsdatoForBeskjæring(
        personResultat: PersonResultat,
    ): LocalDate {
        val barna = persongrunnlagService.hentAktivThrows(personResultat.vilkårsvurdering.behandling.id).barna
        val fødselsdatoForBeskjæring =
            if (personResultat.erSøkersResultater()) {
                barna.minOfOrNull { it.fødselsdato }
            } else {
                barna.find { it.aktør.aktørId == personResultat.aktør.aktørId }?.fødselsdato
            }
        return fødselsdatoForBeskjæring ?: LocalDate.MIN
    }

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

    private fun lagErBosattINorgeTidslinje(adresser: Adresser): Tidslinje<Boolean> = lagTidslinjeForAdresser(adresser.bostedsadresser) { it.erINorge() }

    private fun lagErBosattIFinnmarkEllerNordTromsTidslinje(adresser: Adresser): Tidslinje<Boolean> {
        val bostedsadresserIFinnmarkEllerNordTromsTidslinje = lagTidslinjeForAdresser(adresser.bostedsadresser) { it.erIFinnmarkEllerNordTroms() }
        val delteBostederIFinnmarkEllerNordTromsTidslinje = lagTidslinjeForAdresser(adresser.delteBosteder) { it.erIFinnmarkEllerNordTroms() }

        return bostedsadresserIFinnmarkEllerNordTromsTidslinje
            .kombinerMed(delteBostederIFinnmarkEllerNordTromsTidslinje) { bostedsadresseIFinnmarkEllerNordTroms, deltBostedIFinnmarkEllerNordTroms ->
                bostedsadresseIFinnmarkEllerNordTroms == true || deltBostedIFinnmarkEllerNordTroms == true
            }
    }

    private fun lagTidslinjeForAdresser(
        adresser: List<Adresse>,
        operator: (Adresse) -> Boolean,
    ): Tidslinje<Boolean> =
        adresser
            .sortedBy { it.gyldigFraOgMed }
            .windowed(size = 2, step = 1, partialWindows = true) {
                val denne = it.first()
                val neste = it.getOrNull(1)

                Periode(
                    verdi = operator(denne),
                    fom = denne.gyldigFraOgMed,
                    tom = denne.gyldigTilOgMed ?: neste?.gyldigFraOgMed?.minusDays(1),
                )
            }.tilTidslinje()
}
