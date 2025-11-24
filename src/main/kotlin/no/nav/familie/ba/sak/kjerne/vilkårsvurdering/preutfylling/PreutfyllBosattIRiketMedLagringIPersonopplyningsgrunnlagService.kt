package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.filtrereUgyldigeAdresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.lagErBostedsadresseIFinnmarkEllerNordTromsTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.lagErDeltBostedIFinnmarkEllerNordTromsTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.lagErOppholdsadresserPåSvalbardTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.lagTidslinjeForAdresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.iUkraina
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.lagErNordiskStatsborgerTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.tilPerson
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærFraOgMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_FRA_SØKNAD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
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
class PreutfyllBosattIRiketMedLagringIPersonopplyningsgrunnlagService(
    private val søknadService: SøknadService,
    private val persongrunnlagService: PersongrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {
    fun preutfyllBosattIRiket(
        vilkårsvurdering: Vilkårsvurdering,
        identerVilkårSkalPreutfyllesFor: List<String>? = null,
    ) {
        val behandling = vilkårsvurdering.behandling
        val identer =
            vilkårsvurdering
                .personResultater
                .map { it.aktør.aktivFødselsnummer() }
                .filter { identerVilkårSkalPreutfyllesFor?.contains(it) ?: true }

        vilkårsvurdering
            .personResultater
            .filter { it.aktør.aktivFødselsnummer() in identer }
            .forEach { personResultat ->
                val personOpplysningsgrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)
                val person = personOpplysningsgrunnlag.personer.find { it.aktør == personResultat.aktør } ?: throw Feil("Aktør ${personResultat.aktør.aktørId} har personresultat men ikke persongrunnlag")

                if (person.statsborgerskap.iUkraina()) {
                    return@forEach
                }

                val adresserForPerson = Adresser.opprettFra(person)

                val nyeBosattIRiketVilkårResultater =
                    genererBosattIRiketVilkårResultat(
                        personResultat = personResultat,
                        fødselsdatoForBeskjæring = if (personResultat.erSøkersResultater()) personOpplysningsgrunnlag.eldsteBarnSinFødselsdato else person.fødselsdato,
                        adresserForPerson = adresserForPerson,
                        behandling = behandling,
                    )

                if (nyeBosattIRiketVilkårResultater.isNotEmpty()) {
                    personResultat.vilkårResultater.removeIf { it.vilkårType == BOSATT_I_RIKET }
                    personResultat.vilkårResultater.addAll(nyeBosattIRiketVilkårResultater)
                }
            }
    }

    fun genererBosattIRiketVilkårResultat(
        personResultat: PersonResultat,
        fødselsdatoForBeskjæring: LocalDate = LocalDate.MIN,
        adresserForPerson: Adresser,
        behandling: Behandling,
    ): Set<VilkårResultat> {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)
        val person = personResultat.aktør.tilPerson(personopplysningGrunnlag)

        val erBosattINorgeTidslinje =
            adresserForPerson.bostedsadresser.filtrereUgyldigeAdresser().lagTidslinjeForAdresser(
                personResultat.aktør.aktørId,
                "Bostedadresse",
            ) { adresse -> adresse.erINorge() }

        val erNordiskStatsborgerTidslinje = lagErNordiskStatsborgerTidslinje(person.statsborgerskap)
        val erBostedsadresseIFinnmarkEllerNordTromsTidslinje = adresserForPerson.lagErBostedsadresseIFinnmarkEllerNordTromsTidslinje(personResultat)
        val erDeltBostedIFinnmarkEllerNordTromsTidslinje = adresserForPerson.lagErDeltBostedIFinnmarkEllerNordTromsTidslinje(personResultat.aktør.aktørId)
        val erOppholdsadressePåSvalbardTidslinje = adresserForPerson.lagErOppholdsadresserPåSvalbardTidslinje(personResultat)

        val erBosattINorgeEllerPåSvalbardTidslinje =
            erBosattINorgeTidslinje
                .kombinerMed(erOppholdsadressePåSvalbardTidslinje) { bosattINorge, bosattPåSvalbard ->
                    when {
                        bosattINorge == true || bosattPåSvalbard == true -> true
                        bosattINorge == false && bosattPåSvalbard == false -> false
                        else -> null
                    }
                }.tilPerioderIkkeNull()
                .tilTidslinje()

        val andelForAktør = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør)

        if (behandling.erFinnmarksEllerSvalbardtillegg()) {
            validerKombinasjonerAvAdresserForFinnmarksOgSvalbardtileggbehandlinger(
                behandling = behandling,
                erDeltBostedIFinnmarkEllerNordTromsTidslinje = erDeltBostedIFinnmarkEllerNordTromsTidslinje,
                erOppholdsadressePåSvalbardTidslinje = erOppholdsadressePåSvalbardTidslinje,
                andelForAktør = andelForAktør,
            )
        }

        val erNordiskStatsborgerOgBosattINorgeTidslinje =
            erNordiskStatsborgerTidslinje.kombinerMed(erBosattINorgeEllerPåSvalbardTidslinje) { erNordiskStatsborger, erBosattINorge ->
                if (erNordiskStatsborger == true && erBosattINorge == true) {
                    OppfyltDelvilkår(begrunnelse = "- Norsk/nordisk statsborgerskap")
                } else {
                    IkkeOppfyltDelvilkår
                }
            }

        val erØvrigeKravForBosattIRiketOppfyltTidslinje = lagErØvrigeKravForBosattIRiketOppfyltTidslinje(erBosattINorgeEllerPåSvalbardTidslinje, personResultat, person.fødselsdato)

        val erBosattIFinnmarkEllerNordTromsTidslinje =
            erBostedsadresseIFinnmarkEllerNordTromsTidslinje.kombinerMed(erDeltBostedIFinnmarkEllerNordTromsTidslinje) { erBostedsadresseIFinnmarkEllerNordTroms, erDeltBostedIFinnmarkEllerNordTroms ->
                erBostedsadresseIFinnmarkEllerNordTroms == true || erDeltBostedIFinnmarkEllerNordTroms == true
            }

        val førsteBosattINorgeDato = erBosattINorgeEllerPåSvalbardTidslinje.filtrer { it == true }.startsTidspunkt

        val erBosattIRiketTidslinje =
            erNordiskStatsborgerOgBosattINorgeTidslinje
                .kombinerMed(erØvrigeKravForBosattIRiketOppfyltTidslinje) { erNordiskOgBosatt, erØvrigeKravOppfylt ->
                    when {
                        erNordiskOgBosatt is OppfyltDelvilkår -> erNordiskOgBosatt
                        erØvrigeKravOppfylt is OppfyltDelvilkår -> erØvrigeKravOppfylt
                        else -> IkkeOppfyltDelvilkår
                    }
                }.kombinerMed(erBosattIFinnmarkEllerNordTromsTidslinje, erOppholdsadressePåSvalbardTidslinje) { erBosattIRiket, erBosattIFinnmarkEllerNordTroms, erOppholdsadressePåSvalbard ->
                    when (erBosattIRiket) {
                        is OppfyltDelvilkår -> {
                            val utdypendeVilkårsvurderinger =
                                when {
                                    erOppholdsadressePåSvalbard == true -> listOf(BOSATT_PÅ_SVALBARD)
                                    erBosattIFinnmarkEllerNordTroms == true -> listOf(BOSATT_I_FINNMARK_NORD_TROMS)
                                    else -> emptyList()
                                }
                            erBosattIRiket.copy(utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger)
                        }

                        else -> erBosattIRiket
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
                    erOpprinneligPreutfylt = true,
                )
            }.toSet()
    }

    private fun lagErØvrigeKravForBosattIRiketOppfyltTidslinje(
        erBosattINorgeEllerPåSvalbardTidslinje: Tidslinje<Boolean>,
        personResultat: PersonResultat,
        personFødselsdato: LocalDate,
    ): Tidslinje<Delvilkår> =
        erBosattINorgeEllerPåSvalbardTidslinje
            .tilPerioder()
            .map { erBosattINorgePeriode ->
                Periode(
                    verdi =
                        when (erBosattINorgePeriode.verdi) {
                            true -> sjekkØvrigeKravForPeriode(erBosattINorgePeriode, personResultat, personFødselsdato)
                            else -> IkkeOppfyltDelvilkår
                        },
                    fom = erBosattINorgePeriode.fom,
                    tom = erBosattINorgePeriode.tom,
                )
            }.tilTidslinje()

    private fun sjekkØvrigeKravForPeriode(
        erBosattINorgePeriode: Periode<Boolean?>,
        personResultat: PersonResultat,
        personFødselsdato: LocalDate,
    ): Delvilkår =
        when {
            erBosattINorgePeriode.erMinst12Måneder() ->
                OppfyltDelvilkår("- Norsk bostedsadresse i minst 12 måneder.")

            erBosattINorgePeriode.omfatter(personFødselsdato) ->
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
}
