package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.kanErstatte
import no.nav.familie.ba.sak.common.kanFlytteFom
import no.nav.familie.ba.sak.common.kanFlytteTom
import no.nav.familie.ba.sak.common.kanSplitte
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.toPeriode
import no.nav.familie.ba.sak.ekstern.restDomene.RestVedtakBegrunnelseTilknyttetVilkår
import no.nav.familie.ba.sak.ekstern.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand.Companion.sisteSivilstand
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

object VilkårsvurderingUtils {

    /**
     * Funksjon som forsøker å slette en periode på et vilkår.
     * Dersom det kun finnes en periode eller perioden som skal slettes
     * lager en glippe. Isåfall nullstiller vi bare perioden.
     */
    fun muterPersonResultatDelete(personResultat: PersonResultat, vilkårResultatId: Long) {
        personResultat.slettEllerNullstill(vilkårResultatId = vilkårResultatId)
    }

    /**
     * Funksjon som forsøker å legge til en periode på et vilkår.
     * Dersom det allerede finnes en uvurdet periode med samme vilkårstype
     * skal det kastes en feil.
     */
    fun muterPersonResultatPost(personResultat: PersonResultat, vilkårType: Vilkår) {
        val nyttVilkårResultat = VilkårResultat(
            personResultat = personResultat,
            vilkårType = vilkårType,
            resultat = Resultat.IKKE_VURDERT,
            begrunnelse = "",
            behandlingId = personResultat.vilkårsvurdering.behandling.id
        )
        if (harUvurdertePerioder(personResultat, vilkårType)) {
            throw FunksjonellFeil(
                melding = "Det finnes allerede uvurderte vilkår av samme vilkårType",
                frontendFeilmelding = "Du må ferdigstille vilkårsvurderingen på en periode som allerede er påbegynt, før du kan legge til en ny periode"
            )
        }
        personResultat.addVilkårResultat(vilkårResultat = nyttVilkårResultat)
    }

    /**
     * Funksjon som tar inn endret vilkår og muterer personens vilkårresultater til å få plass til den endrede perioden.
     * @param[personResultat] Person med vilkår som eventuelt justeres
     * @param[restVilkårResultat] Det endrede vilkårresultatet
     * @return VilkårResultater før og etter mutering
     */
    fun muterPersonVilkårResultaterPut(
        personResultat: PersonResultat,
        restVilkårResultat: RestVilkårResultat
    ): Pair<List<VilkårResultat>, List<VilkårResultat>> {
        validerAvslagUtenPeriodeMedLøpende(
            personSomEndres = personResultat,
            vilkårSomEndres = restVilkårResultat
        )
        val kopiAvVilkårResultater = personResultat.vilkårResultater.toList()

        kopiAvVilkårResultater
            .filter { !(it.erAvslagUtenPeriode() && it.id != restVilkårResultat.id) }
            .forEach {
                tilpassVilkårForEndretVilkår(
                    personResultat = personResultat,
                    vilkårResultat = it,
                    restVilkårResultat = restVilkårResultat
                )
            }

        return Pair(kopiAvVilkårResultater, personResultat.vilkårResultater.toList())
    }

    fun validerAvslagUtenPeriodeMedLøpende(personSomEndres: PersonResultat, vilkårSomEndres: RestVilkårResultat) {
        val resultaterPåVilkår =
            personSomEndres.vilkårResultater.filter { it.vilkårType == vilkårSomEndres.vilkårType && it.id != vilkårSomEndres.id }
        when {
            // For bor med søker-vilkåret kan avslag og innvilgelse være overlappende, da man kan f.eks. avslå full barnetrygd, men innvilge delt
            vilkårSomEndres.vilkårType == Vilkår.BOR_MED_SØKER -> return
            vilkårSomEndres.erAvslagUtenPeriode() && resultaterPåVilkår.any { it.resultat == Resultat.OPPFYLT && it.harFremtidigTom() } ->
                throw FunksjonellFeil(
                    "Finnes løpende oppfylt ved forsøk på å legge til avslag uten periode ",
                    "Du kan ikke legge til avslag uten datoer fordi det finnes oppfylt løpende periode på vilkåret."
                )

            vilkårSomEndres.harFremtidigTom() && resultaterPåVilkår.any { it.erAvslagUtenPeriode() } ->
                throw FunksjonellFeil(
                    "Finnes avslag uten periode ved forsøk på å legge til løpende oppfylt",
                    "Du kan ikke legge til løpende periode fordi det er vurdert avslag uten datoer på vilkåret."
                )
        }
    }

    private fun harUvurdertePerioder(personResultat: PersonResultat, vilkårType: Vilkår): Boolean {
        val uvurdetePerioderMedSammeVilkårType = personResultat.vilkårResultater
            .filter { it.vilkårType == vilkårType }
            .find { it.resultat == Resultat.IKKE_VURDERT }
        return uvurdetePerioderMedSammeVilkårType != null
    }

    /**
     * @param [personResultat] person vilkårresultatet tilhører
     * @param [vilkårResultat] vilkårresultat som skal oppdaters på person
     * @param [restVilkårResultat] oppdatert resultat fra frontend
     */
    fun tilpassVilkårForEndretVilkår(
        personResultat: PersonResultat,
        vilkårResultat: VilkårResultat,
        restVilkårResultat: RestVilkårResultat
    ) {
        val periodePåNyttVilkår: Periode = restVilkårResultat.toPeriode()

        if (vilkårResultat.id == restVilkårResultat.id) {
            vilkårResultat.oppdater(restVilkårResultat)
        } else if (vilkårResultat.vilkårType == restVilkårResultat.vilkårType && !restVilkårResultat.erAvslagUtenPeriode()) {
            val periode: Periode = vilkårResultat.toPeriode()

            var nyFom = periodePåNyttVilkår.tom
            if (periodePåNyttVilkår.tom != TIDENES_ENDE) {
                nyFom = periodePåNyttVilkår.tom.plusDays(1)
            }

            val nyTom = periodePåNyttVilkår.fom.minusDays(1)

            when {
                periodePåNyttVilkår.kanErstatte(periode) -> {
                    personResultat.removeVilkårResultat(vilkårResultatId = vilkårResultat.id)
                }

                periodePåNyttVilkår.kanSplitte(periode) -> {
                    personResultat.removeVilkårResultat(vilkårResultatId = vilkårResultat.id)
                    personResultat.addVilkårResultat(
                        vilkårResultat.kopierMedNyPeriode(
                            fom = periode.fom,
                            tom = nyTom,
                            behandlingId = personResultat.vilkårsvurdering.behandling.id
                        )
                    )
                    personResultat.addVilkårResultat(
                        vilkårResultat.kopierMedNyPeriode(
                            fom = nyFom,
                            tom = periode.tom,
                            behandlingId = personResultat.vilkårsvurdering.behandling.id
                        )
                    )
                }

                periodePåNyttVilkår.kanFlytteFom(periode) -> {
                    vilkårResultat.periodeFom = nyFom
                    vilkårResultat.erAutomatiskVurdert = false
                    vilkårResultat.oppdaterPekerTilBehandling()
                }

                periodePåNyttVilkår.kanFlytteTom(periode) -> {
                    vilkårResultat.periodeTom = nyTom
                    vilkårResultat.erAutomatiskVurdert = false
                    vilkårResultat.oppdaterPekerTilBehandling()
                }
            }
        }
    }

    fun lagFjernAdvarsel(personResultater: Set<PersonResultat>): String {
        var advarsel =
            "Du har gjort endringer i behandlingsgrunnlaget. Dersom du går videre vil vilkår for følgende personer fjernes:"
        personResultater.forEach {
            advarsel = advarsel.plus("\n${it.aktør.aktivFødselsnummer()}:")
            it.vilkårResultater.forEach { vilkårResultat ->
                advarsel = advarsel.plus("\n   - ${vilkårResultat.vilkårType.beskrivelse}")
            }
            advarsel = advarsel.plus("\n")
        }
        return advarsel
    }
}

fun standardbegrunnelserTilNedtrekksmenytekster(
    sanityBegrunnelser: List<SanityBegrunnelse>
) =
    Standardbegrunnelse
        .values()
        .groupBy { it.vedtakBegrunnelseType }
        .mapValues { begrunnelseGruppe ->
            begrunnelseGruppe.value
                .flatMap { vedtakBegrunnelse ->
                    vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
                        sanityBegrunnelser,
                        vedtakBegrunnelse
                    )
                }
        }

fun eøsStandardbegrunnelserTilNedtrekksmenytekster(
    sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>
) = EØSStandardbegrunnelse.values().groupBy { it.vedtakBegrunnelseType }
    .mapValues { begrunnelseGruppe ->
        begrunnelseGruppe.value.flatMap { vedtakBegrunnelse ->
            eøsBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
                sanityEØSBegrunnelser,
                vedtakBegrunnelse
            )
        }
    }

fun vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
    sanityBegrunnelser: List<SanityBegrunnelse>,
    vedtakBegrunnelse: Standardbegrunnelse
): List<RestVedtakBegrunnelseTilknyttetVilkår> {
    val sanityBegrunnelse = vedtakBegrunnelse.tilSanityBegrunnelse(sanityBegrunnelser) ?: return emptyList()

    val triggesAv = sanityBegrunnelse.tilTriggesAv()
    val visningsnavn = sanityBegrunnelse.navnISystem

    return if (triggesAv.vilkår.isEmpty()) {
        listOf(
            RestVedtakBegrunnelseTilknyttetVilkår(
                id = vedtakBegrunnelse,
                navn = visningsnavn,
                vilkår = null
            )
        )
    } else {
        triggesAv.vilkår.map {
            RestVedtakBegrunnelseTilknyttetVilkår(
                id = vedtakBegrunnelse,
                navn = visningsnavn,
                vilkår = it
            )
        }
    }
}

fun eøsBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(
    sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>,
    vedtakBegrunnelse: EØSStandardbegrunnelse
): List<RestVedtakBegrunnelseTilknyttetVilkår> {
    val eøsSanityBegrunnelse = vedtakBegrunnelse.tilSanityEØSBegrunnelse(sanityEØSBegrunnelser) ?: return emptyList()

    return listOf(
        RestVedtakBegrunnelseTilknyttetVilkår(
            id = vedtakBegrunnelse,
            navn = eøsSanityBegrunnelse.navnISystem,
            vilkår = null
        )
    )
}

fun genererPersonResultatForPerson(
    vilkårsvurdering: Vilkårsvurdering,
    person: Person
): PersonResultat {
    val personResultat = PersonResultat(
        vilkårsvurdering = vilkårsvurdering,
        aktør = person.aktør
    )

    val vilkårForPerson = Vilkår.hentVilkårFor(
        personType = person.type,
        ytelseType = vilkårsvurdering.behandling.hentYtelseTypeTilVilkår()
    )

    val vilkårResultater = vilkårForPerson.map { vilkår ->
        val fom = if (vilkår.gjelderAlltidFraBarnetsFødselsdato()) person.fødselsdato else null

        val tom: LocalDate? =
            when {
                person.erDød() -> person.dødsfall!!.dødsfallDato
                vilkår == Vilkår.UNDER_18_ÅR -> person.fødselsdato.til18ÅrsVilkårsdato()
                else -> null
            }

        VilkårResultat(
            personResultat = personResultat,
            erAutomatiskVurdert = when (vilkår) {
                Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP -> true
                else -> false
            },
            resultat = utledResultat(vilkår, person),
            vilkårType = vilkår,
            periodeFom = fom,
            periodeTom = tom,
            begrunnelse = utledBegrunnelse(vilkår, person),
            behandlingId = personResultat.vilkårsvurdering.behandling.id
        )
    }.toSortedSet(VilkårResultat.VilkårResultatComparator)

    personResultat.setSortedVilkårResultater(vilkårResultater)

    return personResultat
}

private fun utledResultat(
    vilkår: Vilkår,
    person: Person
) = when (vilkår) {
    Vilkår.UNDER_18_ÅR -> Resultat.OPPFYLT
    Vilkår.GIFT_PARTNERSKAP -> utledResultatForGiftPartnerskap(person)
    else -> Resultat.IKKE_VURDERT
}

private fun utledResultatForGiftPartnerskap(person: Person) =
    if (person.sivilstander.isEmpty() || person.sivilstander.sisteSivilstand()?.type?.somForventetHosBarn() == true) {
        Resultat.OPPFYLT
    } else Resultat.IKKE_VURDERT

private fun utledBegrunnelse(
    vilkår: Vilkår,
    person: Person
) = when {
    person.erDød() -> "Dødsfall"
    vilkår == Vilkår.UNDER_18_ÅR -> "Vurdert og satt automatisk"
    vilkår == Vilkår.GIFT_PARTNERSKAP -> if (person.sivilstander.sisteSivilstand()?.type?.somForventetHosBarn() == false) {
        "Vilkåret er forsøkt behandlet automatisk, men barnet er registrert som gift i " +
            "folkeregisteret. Vurder hvilke konsekvenser dette skal ha for behandlingen"
    } else ""

    else -> ""
}

fun validerVilkårStarterIkkeFørMigreringsdatoForMigreringsbehandling(
    vilkårsvurdering: Vilkårsvurdering,
    vilkårResultat: VilkårResultat,
    migreringsdato: LocalDate?
) {
    val behandling = vilkårsvurdering.behandling
    if (migreringsdato != null &&
        vilkårResultat.vilkårType !in listOf(Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP) &&
        vilkårResultat.periodeFom?.isBefore(migreringsdato) == true
    ) {
        throw FunksjonellFeil(
            melding = "${vilkårResultat.vilkårType} kan ikke endres før $migreringsdato " +
                "for fagsak=${behandling.fagsak.id}",
            frontendFeilmelding = "F.o.m. kan ikke settes tidligere " +
                "enn migreringsdato ${migreringsdato.tilKortString()}. " +
                "Ved behov for vurdering før dette, må behandlingen henlegges, " +
                "og migreringstidspunktet endres ved å opprette en ny migreringsbehandling."
        )
    }
}
