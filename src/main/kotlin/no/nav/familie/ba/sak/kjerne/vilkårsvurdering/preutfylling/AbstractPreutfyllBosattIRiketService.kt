package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.iUkraina
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.tilPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.time.LocalDate

abstract class AbstractPreutfyllBosattIRiketService(
    val persongrunnlagService: PersongrunnlagService,
) {
    fun preutfyllBosattIRiket(
        vilkårsvurdering: Vilkårsvurdering,
        barnSomSkalVurderesIFødselshendelse: List<String>? = null,
    ) {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id)
        val identer = finnIdenterViSkalPreutfylleFor(vilkårsvurdering, barnSomSkalVurderesIFødselshendelse)

        vilkårsvurdering.personResultater
            .filter { it.aktør.aktivFødselsnummer() in identer }
            .forEach { personResultat ->
                val person = personResultat.aktør.tilPerson(personopplysningGrunnlag)
                if (person.statsborgerskap.iUkraina()) {
                    return@forEach
                }

                val datoForBeskjæringAvFom = finnDatoForBeskjæringAvFom(person, personopplysningGrunnlag)

                val nyeBosattIRiketVilkårResultater =
                    genererBosattIRiketVilkårResultat(
                        behandling = vilkårsvurdering.behandling,
                        personResultat = personResultat,
                        datoForBeskjæringAvFom = datoForBeskjæringAvFom,
                        person = person,
                    )

                if (nyeBosattIRiketVilkårResultater.isNotEmpty()) {
                    personResultat.vilkårResultater.removeIf { it.vilkårType == BOSATT_I_RIKET }
                    personResultat.vilkårResultater.addAll(nyeBosattIRiketVilkårResultater)
                }
            }
    }

    protected abstract fun genererBosattIRiketVilkårResultat(
        behandling: Behandling,
        personResultat: PersonResultat,
        datoForBeskjæringAvFom: LocalDate,
        person: Person,
    ): Set<VilkårResultat>

    protected fun Tidslinje<Delvilkår>.vurderFinnmarkOgSvalbardtillegg(
        adresserForPerson: Adresser,
    ): Tidslinje<Delvilkår> {
        val erBosattIFinnmarkEllerNordTromsTidslinje = adresserForPerson.lagErBosattIFinnmarkEllerNordTromsTidslinje()
        val erOppholdsadressePåSvalbardTidslinje = adresserForPerson.lagErOppholdsadresserPåSvalbardTidslinje()
        return this.kombinerMed(erBosattIFinnmarkEllerNordTromsTidslinje, erOppholdsadressePåSvalbardTidslinje) { delvilkår, erBosattIFinnmarkEllerNordTroms, erOppholdsadressePåSvalbard ->
            when (delvilkår) {
                is OppfyltDelvilkår -> {
                    val utdypendeVilkårsvurderinger =
                        when {
                            erOppholdsadressePåSvalbard == true -> listOf(BOSATT_PÅ_SVALBARD)
                            erBosattIFinnmarkEllerNordTroms == true -> listOf(BOSATT_I_FINNMARK_NORD_TROMS)
                            else -> emptyList()
                        }
                    delvilkår.copy(utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger)
                }

                else -> {
                    delvilkår
                }
            }
        }
    }

    protected fun List<Periode<Delvilkår>>.tilVilkårResultater(personResultat: PersonResultat): Set<VilkårResultat> =
        this
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
                    evalueringÅrsaker = erBosattINorgePeriode.verdi.ikkeOppfyltEvalueringÅrsaker.map { it.name },
                )
            }.toSet()

    private fun finnDatoForBeskjæringAvFom(
        person: Person,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
    ): LocalDate =
        if (person.type == PersonType.SØKER) {
            personopplysningGrunnlag.eldsteBarnSinFødselsdato ?: person.fødselsdato
        } else {
            person.fødselsdato
        }

    private fun finnIdenterViSkalPreutfylleFor(
        vilkårsvurdering: Vilkårsvurdering,
        barnSomSkalVurderesIFødselshendelse: List<String>?,
    ): List<String> {
        val behandling = vilkårsvurdering.behandling
        val identerIBehandling = vilkårsvurdering.personResultater.map { it.aktør.aktivFødselsnummer() }

        return when (vilkårsvurdering.behandling.opprettetÅrsak) {
            BehandlingÅrsak.FØDSELSHENDELSE -> {
                if (barnSomSkalVurderesIFødselshendelse.isNullOrEmpty()) throw Feil("Barn som skal vurderes er ikke definert for fødselshendelse")
                val identerSomSkalPreutfylles =
                    if (behandling.type == FØRSTEGANGSBEHANDLING) {
                        barnSomSkalVurderesIFødselshendelse + behandling.fagsak.aktør.aktivFødselsnummer()
                    } else {
                        barnSomSkalVurderesIFødselshendelse
                    }

                identerIBehandling.filter { identerSomSkalPreutfylles.contains(it) }
            }

            else -> {
                identerIBehandling
            }
        }
    }
}
