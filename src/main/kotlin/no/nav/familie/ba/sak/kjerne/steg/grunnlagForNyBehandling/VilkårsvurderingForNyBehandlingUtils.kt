package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårResultatUtils.genererVilkårResultatForEtVilkårPåEnPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingMigreringUtils
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.genererPersonResultatForPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.gjelderAlltidFraBarnetsFødselsdato
import java.time.LocalDate

data class VilkårsvurderingForNyBehandlingUtils(
    val personopplysningGrunnlag: PersonopplysningGrunnlag,
) {
    fun genererInitiellVilkårsvurdering(
        behandling: Behandling,
        barnaAktørSomAlleredeErVurdert: List<Aktør>,
    ): Vilkårsvurdering {
        return Vilkårsvurdering(behandling = behandling).apply {
            when {
                behandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD &&
                    behandling.opprettetÅrsak == BehandlingÅrsak.MIGRERING -> {
                    personResultater = lagPersonResultaterForMigreringsbehandling(
                        vilkårsvurdering = this,
                    )
                }

                behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE -> {
                    personResultater = lagPersonResultaterForFødselshendelse(
                        vilkårsvurdering = this,
                        barnaAktørSomAlleredeErVurdert = barnaAktørSomAlleredeErVurdert,
                    )
                }

                !behandling.skalBehandlesAutomatisk -> {
                    personResultater = lagPersonResultaterForManuellVilkårsvurdering(
                        vilkårsvurdering = this,
                    )
                }

                else -> personResultater = lagPersonResultaterForTomVilkårsvurdering(
                    vilkårsvurdering = this,
                )
            }
        }
    }

    fun genererVilkårsvurderingFraForrigeVedtattBehandling(
        initiellVilkårsvurdering: Vilkårsvurdering,
        forrigeBehandlingVilkårsvurdering: Vilkårsvurdering,
        behandling: Behandling,
        løpendeUnderkategori: BehandlingUnderkategori?,
    ): Vilkårsvurdering {
        val (vilkårsvurdering) = VilkårsvurderingUtils.flyttResultaterTilInitielt(
            aktivVilkårsvurdering = forrigeBehandlingVilkårsvurdering,
            initiellVilkårsvurdering = initiellVilkårsvurdering,
            løpendeUnderkategori = løpendeUnderkategori,
            forrigeBehandlingVilkårsvurdering = forrigeBehandlingVilkårsvurdering,
        )

        return if (behandling.type == BehandlingType.REVURDERING) {
            hentVilkårsvurderingMedDødsdatoSomTomDato(vilkårsvurdering)
        } else {
            vilkårsvurdering
        }
    }

    fun hentVilkårsvurderingMedDødsdatoSomTomDato(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        vilkårsvurdering.personResultater.forEach { personResultat ->
            val person = personopplysningGrunnlag.søkerOgBarn.single { it.aktør == personResultat.aktør }

            if (person.erDød()) {
                val dødsDato = person.dødsfall!!.dødsfallDato

                Vilkår.values().forEach { vilkårType ->
                    val vilkårAvTypeMedSenesteTom =
                        personResultat.vilkårResultater.filter { it.vilkårType == vilkårType }
                            .maxByOrNull { it.periodeTom ?: TIDENES_ENDE }

                    if (vilkårAvTypeMedSenesteTom != null && dødsDato.isBefore(
                            vilkårAvTypeMedSenesteTom.periodeTom ?: TIDENES_ENDE,
                        ) && dødsDato.isAfter(vilkårAvTypeMedSenesteTom.periodeFom)
                    ) {
                        vilkårAvTypeMedSenesteTom.periodeTom = dødsDato
                        vilkårAvTypeMedSenesteTom.begrunnelse = "Dødsfall"
                    }
                }
            }
        }
        return vilkårsvurdering
    }

    private fun lagPersonResultaterForMigreringsbehandling(
        vilkårsvurdering: Vilkårsvurdering,
    ): Set<PersonResultat> {
        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val vilkårTyperForPerson = Vilkår.hentVilkårFor(
                personType = person.type,
                fagsakType = vilkårsvurdering.behandling.fagsak.type,
                behandlingUnderkategori = vilkårsvurdering.behandling.underkategori,
            )

            val vilkårResultater = vilkårTyperForPerson.map { vilkår ->
                val fom = if (vilkår.gjelderAlltidFraBarnetsFødselsdato()) person.fødselsdato else null

                val tom: LocalDate? =
                    if (vilkår == Vilkår.UNDER_18_ÅR) person.fødselsdato.til18ÅrsVilkårsdato() else null

                val begrunnelse = "Migrering"

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = false,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = vilkår,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = begrunnelse,
                    behandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSortedSet(VilkårResultat.VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    private fun lagPersonResultaterForFødselshendelse(
        vilkårsvurdering: Vilkårsvurdering,
        barnaAktørSomAlleredeErVurdert: List<Aktør>,
    ): Set<PersonResultat> {
        val annenForelder = personopplysningGrunnlag.annenForelder
        val eldsteBarnSomVurderesSinFødselsdato =
            personopplysningGrunnlag.barna.filter { !barnaAktørSomAlleredeErVurdert.contains(it.aktør) }
                .maxByOrNull { it.fødselsdato }?.fødselsdato
                ?: throw Feil("Finner ingen barn på persongrunnlag")

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val vilkårForPerson = Vilkår.hentVilkårFor(
                personType = person.type,
                fagsakType = vilkårsvurdering.behandling.fagsak.type,
                behandlingUnderkategori = vilkårsvurdering.behandling.underkategori,
            )

            val vilkårResultater = vilkårForPerson.map { vilkår ->
                genererVilkårResultatForEtVilkårPåEnPerson(
                    person = person,
                    annenForelder = annenForelder,
                    eldsteBarnSinFødselsdato = eldsteBarnSomVurderesSinFødselsdato,
                    personResultat = personResultat,
                    vilkår = vilkår,
                )
            }

            personResultat.setSortedVilkårResultater(vilkårResultater.toSet())

            personResultat
        }.toSet()
    }

    private fun lagPersonResultaterForManuellVilkårsvurdering(
        vilkårsvurdering: Vilkårsvurdering,
    ): Set<PersonResultat> {
        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            genererPersonResultatForPerson(vilkårsvurdering, person)
        }.toSet()
    }

    private fun lagPersonResultaterForTomVilkårsvurdering(
        vilkårsvurdering: Vilkårsvurdering,
    ): Set<PersonResultat> {
        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val vilkårForPerson = Vilkår.hentVilkårFor(
                personType = person.type,
                fagsakType = vilkårsvurdering.behandling.fagsak.type,
                behandlingUnderkategori = vilkårsvurdering.behandling.underkategori,
            )

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

    fun lagPersonResultaterForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
        vilkårsvurdering: Vilkårsvurdering,
        forrigeBehandlingVilkårsvurdering: Vilkårsvurdering,
        nyMigreringsdato: LocalDate,
    ): Set<PersonResultat> {
        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val vilkårTyperForPerson = forrigeBehandlingVilkårsvurdering.personResultater
                .single { it.aktør == person.aktør }.vilkårResultater
                .filter { it.resultat == Resultat.OPPFYLT }
                .map { it.vilkårType }

            val vilkårResultater = vilkårTyperForPerson.map { vilkår ->
                val fom = VilkårsvurderingMigreringUtils.utledPeriodeFom(
                    forrigeBehandlingVilkårsvurdering,
                    vilkår,
                    person,
                    nyMigreringsdato,
                )

                val tom: LocalDate? =
                    VilkårsvurderingMigreringUtils.utledPeriodeTom(
                        forrigeBehandlingVilkårsvurdering,
                        vilkår,
                        person,
                        fom,
                    )

                val begrunnelse = "Migrering"

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = false,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = vilkår,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = begrunnelse,
                    behandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSortedSet(VilkårResultat.VilkårResultatComparator)

            val manglendePerioder = VilkårsvurderingMigreringUtils.kopiManglendePerioderFraForrigeVilkårsvurdering(
                vilkårResultater,
                forrigeBehandlingVilkårsvurdering,
                person,
            )
            vilkårResultater.addAll(manglendePerioder.map { it.kopierMedParent(personResultat) }.toSet())
            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    fun lagPersonResultaterForHelmanuellMigrering(
        vilkårsvurdering: Vilkårsvurdering,
        nyMigreringsdato: LocalDate,
    ): Set<PersonResultat> {
        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val vilkårTyperForPerson = Vilkår.hentVilkårFor(
                personType = person.type,
                fagsakType = vilkårsvurdering.behandling.fagsak.type,
                behandlingUnderkategori = vilkårsvurdering.behandling.underkategori,
            )
            val vilkårResultater = vilkårTyperForPerson.map { vilkår ->
                val fom = when {
                    vilkår.gjelderAlltidFraBarnetsFødselsdato() -> person.fødselsdato
                    nyMigreringsdato.isBefore(person.fødselsdato) -> person.fødselsdato
                    else -> nyMigreringsdato
                }

                val tom: LocalDate? = when (vilkår) {
                    Vilkår.UNDER_18_ÅR -> person.fødselsdato.plusYears(18)
                        .minusDays(1)

                    else -> null
                }

                val begrunnelse = "Migrering"

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = false,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = vilkår,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = begrunnelse,
                    behandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSortedSet(VilkårResultat.VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }
}

fun førstegangskjøringAvVilkårsvurdering(aktivVilkårsvurdering: Vilkårsvurdering?): Boolean {
    return aktivVilkårsvurdering == null
}
