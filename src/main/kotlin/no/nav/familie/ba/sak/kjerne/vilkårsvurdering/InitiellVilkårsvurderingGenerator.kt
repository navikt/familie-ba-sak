package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

class InitiellVilkårsvurderingGenerator(
    private val behandling: Behandling,
    private val personopplysningGrunnlag: PersonopplysningGrunnlag
) {

    fun genererInitiellVilkårsvurdering(
        barnaSomAlleredeErVurdert: List<Aktør>
    ): Vilkårsvurdering {
        return Vilkårsvurdering(behandling = behandling).apply {
            when {
                behandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD &&
                    behandling.opprettetÅrsak == BehandlingÅrsak.MIGRERING -> {
                    personResultater = lagVilkårsvurderingForMigreringsbehandling(this)
                }
                behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE -> {
                    personResultater = lagVilkårsvurderingForFødselshendelse(this, barnaSomAlleredeErVurdert)
                }
                !behandling.skalBehandlesAutomatisk -> {
                    personResultater = lagManuellVilkårsvurdering(this)
                }
                else -> personResultater = lagTomVilkårsvurdering(this)
            }
        }
    }

    private fun lagVilkårsvurderingForMigreringsbehandling(vilkårsvurdering: Vilkårsvurdering): Set<PersonResultat> {
        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            // NB Dette må gjøres om når vi skal begynne å migrere EØS-saker
            val ytelseType = if (person.type == PersonType.SØKER) when (vilkårsvurdering.behandling.underkategori) {
                BehandlingUnderkategori.UTVIDET -> YtelseType.UTVIDET_BARNETRYGD
                BehandlingUnderkategori.ORDINÆR -> YtelseType.ORDINÆR_BARNETRYGD
            } else YtelseType.ORDINÆR_BARNETRYGD

            val vilkårTyperForPerson = Vilkår.hentVilkårFor(person.type, ytelseType = ytelseType)

            val vilkårResultater = vilkårTyperForPerson.map { vilkår ->
                val fom = if (vilkår.gjelderAlltidFraBarnetsFødselsdato()) person.fødselsdato else null

                val tom: LocalDate? =
                    if (vilkår == Vilkår.UNDER_18_ÅR) person.fødselsdato.plusYears(18).minusDays(1) else null

                val begrunnelse = "Migrering"

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = false,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = vilkår,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = begrunnelse,
                    behandlingId = personResultat.vilkårsvurdering.behandling.id
                )
            }.toSortedSet(VilkårResultat.VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    private fun lagVilkårsvurderingForFødselshendelse(
        vilkårsvurdering: Vilkårsvurdering,
        barnaSomAlleredeErVurdert: List<Aktør>
    ): Set<PersonResultat> {
        val annenForelder = personopplysningGrunnlag.annenForelder
        val eldsteBarnSomVurderesSinFødselsdato =
            personopplysningGrunnlag.barna.filter { !barnaSomAlleredeErVurdert.contains(it.aktør) }
                .maxByOrNull { it.fødselsdato }?.fødselsdato
                ?: throw Feil("Finner ingen barn på persongrunnlag")

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val vilkårForPerson = Vilkår.hentVilkårFor(person.type)

            val vilkårResultater = vilkårForPerson.map { vilkår ->
                genererVilkårResultatForEtVilkårPåEnPerson(
                    person = person,
                    annenForelder = annenForelder,
                    eldsteBarnSinFødselsdato = eldsteBarnSomVurderesSinFødselsdato,
                    personResultat = personResultat,
                    vilkår = vilkår
                )
            }

            personResultat.setSortedVilkårResultater(vilkårResultater.toSet())

            personResultat
        }.toSet()
    }

    private fun lagManuellVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering): Set<PersonResultat> {
        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            genererPersonResultatForPerson(vilkårsvurdering, person)
        }.toSet()
    }

    private fun lagTomVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering): Set<PersonResultat> {
        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val vilkårForPerson = Vilkår.hentVilkårFor(person.type)

            val vilkårResultater = vilkårForPerson.map { vilkår ->
                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat = Resultat.IKKE_VURDERT,
                    vilkårType = vilkår,
                    begrunnelse = "",
                    behandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSortedSet(VilkårResultat.VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    private fun genererVilkårResultatForEtVilkårPåEnPerson(
        person: Person,
        eldsteBarnSinFødselsdato: LocalDate,
        personResultat: PersonResultat,
        vilkår: Vilkår,
        annenForelder: Person? = null,
    ): VilkårResultat {
        val automatiskVurderingResultat = vilkår.vurderVilkår(
            person = person,
            annenForelder = annenForelder,
            vurderFra = eldsteBarnSinFødselsdato
        )

        val fom = if (eldsteBarnSinFødselsdato >= person.fødselsdato) eldsteBarnSinFødselsdato else person.fødselsdato

        val tom: LocalDate? =
            if (vilkår == Vilkår.UNDER_18_ÅR) {
                person.fødselsdato.til18ÅrsVilkårsdato()
            } else null

        return VilkårResultat(
            regelInput = automatiskVurderingResultat.regelInput,
            personResultat = personResultat,
            erAutomatiskVurdert = true,
            resultat = automatiskVurderingResultat.resultat,
            vilkårType = vilkår,
            periodeFom = fom,
            periodeTom = tom,
            begrunnelse = "Vurdert og satt automatisk: ${automatiskVurderingResultat.evaluering.begrunnelse}",
            behandlingId = personResultat.vilkårsvurdering.behandling.id,
            evalueringÅrsaker = automatiskVurderingResultat.evaluering.evalueringÅrsaker.map { it.toString() }
        )
    }
}
