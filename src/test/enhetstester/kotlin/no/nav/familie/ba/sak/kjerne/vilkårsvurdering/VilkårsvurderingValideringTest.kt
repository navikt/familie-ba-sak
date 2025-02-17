package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class VilkårsvurderingValideringTest {
    @Nested
    inner class ValiderIkkeBlandetRegelverk {
        @Test
        fun `skal kaste feil hvis søker vurderes etter nasjonal og minst ett barn etter EØS`() {
            val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
            val søker = lagPersonEnkel(PersonType.SØKER)
            val barn1 = lagPersonEnkel(PersonType.BARN)
            val barn2 = lagPersonEnkel(PersonType.BARN)
            val personResultatSøker = byggPersonResultatForPersonEnkel(søker, Regelverk.NASJONALE_REGLER, vilkårsvurdering)
            val personResultatBarn1 = byggPersonResultatForPersonEnkel(barn1, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)
            val personResultatBarn2 = byggPersonResultatForPersonEnkel(barn2, Regelverk.NASJONALE_REGLER, vilkårsvurdering)

            vilkårsvurdering.personResultater =
                setOf(
                    personResultatSøker,
                    personResultatBarn1,
                    personResultatBarn2,
                )

            assertThrows<FunksjonellFeil> {
                validerIkkeBlandetRegelverk(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1, barn2),
                    behandling = lagBehandling(),
                )
            }
        }

        @Test
        fun `skal ikke kaste feil hvis søker vurderes etter nasjonal og minst ett barn etter EØS om der er satsendring`() {
            val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
            val søker = lagPersonEnkel(PersonType.SØKER)
            val barn1 = lagPersonEnkel(PersonType.BARN)
            val barn2 = lagPersonEnkel(PersonType.BARN)
            val personResultatSøker = byggPersonResultatForPersonEnkel(søker, Regelverk.NASJONALE_REGLER, vilkårsvurdering)
            val personResultatBarn1 = byggPersonResultatForPersonEnkel(barn1, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)
            val personResultatBarn2 = byggPersonResultatForPersonEnkel(barn2, Regelverk.NASJONALE_REGLER, vilkårsvurdering)

            vilkårsvurdering.personResultater =
                setOf(
                    personResultatSøker,
                    personResultatBarn1,
                    personResultatBarn2,
                )

            assertDoesNotThrow {
                validerIkkeBlandetRegelverk(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1, barn2),
                    behandling = lagBehandling(årsak = BehandlingÅrsak.SATSENDRING),
                )
            }
        }

        @Test
        fun `skal ikke kaste feil hvis både søker og barn vurderes etter eøs`() {
            val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
            val søker = lagPersonEnkel(PersonType.SØKER)
            val barn1 = lagPersonEnkel(PersonType.BARN)
            val personResultatSøker = byggPersonResultatForPersonEnkel(søker, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)
            val personResultatBarn1 = byggPersonResultatForPersonEnkel(barn1, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)

            vilkårsvurdering.personResultater =
                setOf(
                    personResultatSøker,
                    personResultatBarn1,
                )

            assertDoesNotThrow {
                validerIkkeBlandetRegelverk(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1),
                    behandling = lagBehandling(),
                )
            }
        }

        @Test
        fun `skal ikke kaste feil hvis søker vurderes etter eøs, men barn vurderes etter nasjonal`() {
            val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
            val søker = lagPersonEnkel(PersonType.SØKER)
            val barn1 = lagPersonEnkel(PersonType.BARN)
            val personResultatSøker = byggPersonResultatForPersonEnkel(søker, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)
            val personResultatBarn1 = byggPersonResultatForPersonEnkel(barn1, Regelverk.NASJONALE_REGLER, vilkårsvurdering)

            vilkårsvurdering.personResultater =
                setOf(
                    personResultatSøker,
                    personResultatBarn1,
                )

            assertDoesNotThrow {
                validerIkkeBlandetRegelverk(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1),
                    behandling = lagBehandling(),
                )
            }
        }

        @Test
        fun `skal ikke kaste feil hvis både søker og barn vurderes etter nasjonal og eøs, men i samme perioder`() {
            val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
            val søker = lagPersonEnkel(PersonType.SØKER)
            val barn = lagPersonEnkel(PersonType.BARN)

            val vilkårPerioder =
                listOf(
                    VilkårPeriode(
                        regelverk = Regelverk.EØS_FORORDNINGEN,
                        fom = LocalDate.now().minusMonths(9),
                        tom = LocalDate.now().minusMonths(1),
                    ),
                    VilkårPeriode(
                        regelverk = Regelverk.NASJONALE_REGLER,
                        fom = LocalDate.now().minusMonths(1).plusMonths(1),
                        tom = null,
                    ),
                )

            val personResultatSøker = byggPersonResultatForPersonIPerioder(søker, vilkårPerioder, vilkårsvurdering)
            val personResultatBarn = byggPersonResultatForPersonIPerioder(barn, vilkårPerioder, vilkårsvurdering)

            vilkårsvurdering.personResultater =
                setOf(
                    personResultatSøker,
                    personResultatBarn,
                )

            assertDoesNotThrow {
                validerIkkeBlandetRegelverk(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn),
                    behandling = lagBehandling(),
                )
            }
        }
    }

    @Nested
    inner class Valider18ÅrsVilkårEksistererFraFødselsdato {
        @Test
        fun `skal kaste feil hvis barn ikke har 18-års vilkår vurdert fra fødselsdato`() {
            val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
            val barn = lagPersonEnkel(PersonType.BARN)
            val personResultatBarn = byggPersonResultatForPersonEnkel(barn, Regelverk.NASJONALE_REGLER, vilkårsvurdering)

            personResultatBarn.vilkårResultater.first { it.vilkårType == Vilkår.UNDER_18_ÅR }.periodeFom =
                barn.fødselsdato.minusDays(1)

            vilkårsvurdering.personResultater = setOf(personResultatBarn)

            assertThrows<FunksjonellFeil> {
                valider18ÅrsVilkårEksistererFraFødselsdato(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(barn),
                    behandling = lagBehandling(),
                )
            }
        }

        @ParameterizedTest
        @EnumSource(value = BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING"])
        fun `skal ikke kaste feil for satsendring og månedlig valutajustering selv om barn ikke har 18-års vilkår vurdert fra fødselsdato`(
            årsak: BehandlingÅrsak,
        ) {
            val behandling = lagBehandling(årsak = årsak)
            val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
            val barn = lagPersonEnkel(PersonType.BARN)
            val personResultatBarn = byggPersonResultatForPersonEnkel(barn, Regelverk.NASJONALE_REGLER, vilkårsvurdering)

            personResultatBarn.vilkårResultater.first { it.vilkårType == Vilkår.UNDER_18_ÅR }.periodeFom =
                barn.fødselsdato.minusDays(1)

            vilkårsvurdering.personResultater = setOf(personResultatBarn)

            assertDoesNotThrow {
                valider18ÅrsVilkårEksistererFraFødselsdato(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(barn),
                    behandling = behandling,
                )
            }
        }
    }

    private fun byggPersonResultatForPersonEnkel(
        person: PersonEnkel,
        regelverk: Regelverk,
        vilkårsvurdering: Vilkårsvurdering,
    ): PersonResultat {
        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)
        val vilkårResultater =
            lagVilkårResultatForPersonIPeriode(
                vilkårsvurdering = vilkårsvurdering,
                person = lagPerson(type = person.type, aktør = person.aktør, fødselsdato = person.fødselsdato),
                periodeFom = LocalDate.now().minusMonths(2),
                periodeTom = LocalDate.now().minusMonths(1),
                vurderesEtter = regelverk,
                personResultat = personResultat,
            )

        personResultat.setSortedVilkårResultater(vilkårResultater)

        return personResultat
    }

    private data class VilkårPeriode(
        val fom: LocalDate,
        val tom: LocalDate?,
        val regelverk: Regelverk,
    )

    private fun byggPersonResultatForPersonIPerioder(
        person: PersonEnkel,
        perioder: List<VilkårPeriode>,
        vilkårsvurdering: Vilkårsvurdering,
    ): PersonResultat {
        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)
        val vilkårResultater =
            perioder
                .flatMap {
                    lagVilkårResultatForPersonIPeriode(
                        vilkårsvurdering = vilkårsvurdering,
                        person = lagPerson(type = person.type, aktør = person.aktør),
                        periodeFom = it.fom,
                        periodeTom = it.tom,
                        vurderesEtter = it.regelverk,
                        personResultat = personResultat,
                    )
                }.toSet()

        personResultat.setSortedVilkårResultater(vilkårResultater)

        return personResultat
    }

    private fun lagVilkårResultatForPersonIPeriode(
        person: Person,
        personResultat: PersonResultat,
        vilkårsvurdering: Vilkårsvurdering,
        periodeFom: LocalDate,
        periodeTom: LocalDate?,
        vurderesEtter: Regelverk,
    ): Set<VilkårResultat> =
        Vilkår
            .hentVilkårFor(
                personType = person.type,
                fagsakType = FagsakType.NORMAL,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            ).map {
                VilkårResultat(
                    personResultat = personResultat,
                    periodeFom = if (it.gjelderAlltidFraBarnetsFødselsdato()) person.fødselsdato else periodeFom,
                    periodeTom = periodeTom,
                    vilkårType = it,
                    resultat = Resultat.OPPFYLT,
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                    vurderesEtter = vurderesEtter,
                )
            }.toSet()

    private fun lagPersonEnkel(personType: PersonType): PersonEnkel =
        PersonEnkel(
            type = personType,
            aktør = randomAktør(),
            dødsfallDato = null,
            fødselsdato =
                if (personType == PersonType.SØKER) {
                    LocalDate.now().minusYears(34)
                } else {
                    LocalDate
                        .now()
                        .minusYears(4)
                },
            målform = Målform.NB,
        )
}
