package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
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
        aktørerMedUtvidetAndelerIForrigeBehandling: List<Aktør>,
    ): Vilkårsvurdering {
        val (vilkårsvurdering) = VilkårsvurderingUtils.flyttResultaterTilInitielt(
            aktivVilkårsvurdering = forrigeBehandlingVilkårsvurdering,
            initiellVilkårsvurdering = initiellVilkårsvurdering,
            løpendeUnderkategori = løpendeUnderkategori,
            aktørerMedUtvidetAndelerIForrigeBehandling = aktørerMedUtvidetAndelerIForrigeBehandling,
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

            val oppfylteVilkårForPerson = forrigeBehandlingVilkårsvurdering.personResultater
                .single { it.aktør == person.aktør }.vilkårResultater
                .filter { it.erOppfylt() }

            val vilkårTyperForPerson = oppfylteVilkårForPerson
                .map { it.vilkårType }

            val vilkårResultaterMedNyPeriode = vilkårTyperForPerson.map { vilkår ->
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

                // Når vi endrer migreringsdato flyttes den alltid bakover. Vilkårresultatet som forskyves vil derfor alltid være det med lavest periodeFom
                val eksisterendeVilkårSomSkalForskyves =
                    oppfylteVilkårForPerson.filter { it.vilkårType == vilkår }.minBy { it.periodeFom!! }
                VilkårResultatMedNyPeriode(eksisterendeVilkårSomSkalForskyves, fom, tom)
            }

            // Sørger for at justerer periodeFom og periodeTom etter at øvrige felter er kopiert fra forrige vilkårresultat.
            val kopierteVilkårResultaterMedNyPeriode = vilkårResultaterMedNyPeriode.map {
                it.vilkårResultat.tilKopiForNyttPersonResultat(personResultat)
                    .also { vilkårResultat ->
                        vilkårResultat.periodeFom = it.fom
                        vilkårResultat.periodeTom = it.tom
                        if (vilkårResultat.begrunnelse.isEmpty()) {
                            vilkårResultat.begrunnelse = "Migrering"
                        }
                    }
            }.toMutableSet()

            val kopierteManglendeOppfylteVilkårResultater =
                VilkårsvurderingMigreringUtils.finnManglendeOppfylteVilkårResultaterFraForrigeVilkårsvurdering(
                    vilkårResultaterMedNyPeriode.map { it.vilkårResultat },
                    oppfylteVilkårForPerson,
                ).map { it.tilKopiForNyttPersonResultat(personResultat) }

            kopierteVilkårResultaterMedNyPeriode.addAll(kopierteManglendeOppfylteVilkårResultater)

            personResultat.setSortedVilkårResultater(kopierteVilkårResultaterMedNyPeriode)

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

fun finnAktørerMedUtvidetFraAndeler(andeler: List<AndelTilkjentYtelse>): List<Aktør> {
    return andeler.filter { it.erUtvidet() }.map { it.aktør }
}

data class VilkårResultatMedNyPeriode(val vilkårResultat: VilkårResultat, val fom: LocalDate, val tom: LocalDate?)
