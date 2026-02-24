package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresse
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseDeltBosted
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurderingMedOverstyrendeResultater
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PreutfyllBorMedSøkerServiceTest {
    private val persongrunnlagService: PersongrunnlagService = mockk(relaxed = true)

    private val preutfyllBorMedSøkerService =
        PreutfyllBorMedSøkerService(
            persongrunnlagService,
        )

    @Test
    fun `skal preutfylle bor fast hos søker vilkår til oppfylt om barn bor på samme adresse som søker`() {
        // Arrange
        val nåDato = LocalDate.now()

        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()

        val behandling = lagBehandling()

        val persongrunnlagMedSammeAdresseForAllePersoner =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = aktørSøker.aktivFødselsnummer(),
                barnasIdenter = listOf(aktørBarn.aktivFødselsnummer()),
                søkerAktør = aktørSøker,
                barnAktør = listOf(aktørBarn),
            ).also { persongrunnlag ->
                persongrunnlag.personer.forEach { person ->
                    person.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresse(matrikkelId = 12345L).also {
                                it.periode =
                                    DatoIntervallEntitet(
                                        fom = nåDato.minusYears(10),
                                        tom = null,
                                    )
                                it.person = person
                            },
                        )
                }
            }
        every { persongrunnlagService.hentAktivThrows(behandlingId = behandling.id) } returns persongrunnlagMedSammeAdresseForAllePersoner

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                behandling = behandling,
                søker = persongrunnlagMedSammeAdresseForAllePersoner.søker,
                barna = persongrunnlagMedSammeAdresseForAllePersoner.barna,
                overstyrendeVilkårResultater = emptyMap(),
            )

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .single {
                    it.vilkårType == Vilkår.BOR_MED_SØKER
                }
        assertThat(borFastHosSøkerVilkår.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(borFastHosSøkerVilkår.periodeFom).isEqualTo(nåDato.minusYears(10))
        assertThat(borFastHosSøkerVilkår.periodeTom).isNull()
    }

    @Test
    fun `skal preutfylle bor fast hos søker vilkår til ikke oppfylt om barn ikke bor på samme adresse som søker`() {
        // Arrange
        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()
        val fødselsdatoSøker = LocalDate.now().minusYears(30)
        val fødselsdatoBarn = LocalDate.now().minusYears(10)

        val behandling = lagBehandling()

        val under18ÅrVilkårResultat = lagVilkårResultat(vilkårType = Vilkår.UNDER_18_ÅR, resultat = Resultat.OPPFYLT, periodeFom = fødselsdatoBarn, periodeTom = fødselsdatoBarn.plusYears(18))

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                søker = lagPerson(type = PersonType.SØKER, aktør = aktørSøker, fødselsdato = fødselsdatoSøker),
                barna = listOf(lagPerson(type = PersonType.BARN, aktør = aktørBarn, fødselsdato = fødselsdatoBarn)),
                behandling = behandling,
                overstyrendeVilkårResultater =
                    mapOf(
                        aktørBarn.aktørId to listOf(under18ÅrVilkårResultat),
                    ),
            )

        val persongrunnlagForskjelligAdresseForSøkerOgBarn =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = aktørSøker.aktivFødselsnummer(),
                barnasIdenter = listOf(aktørBarn.aktivFødselsnummer()),
                søkerAktør = aktørSøker,
                barnAktør = listOf(aktørBarn),
            ).also { persongrunnlag ->
                persongrunnlag.personer.forEach { person ->
                    person.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresse(matrikkelId = if (person.type == PersonType.SØKER) 12345L else 98765L)
                                .also {
                                    it.periode =
                                        DatoIntervallEntitet(
                                            fom = if (person.type == PersonType.SØKER) fødselsdatoSøker else fødselsdatoBarn,
                                            tom = null,
                                        )
                                    it.person = person
                                },
                        )
                    person.deltBosted = mutableListOf()
                }
            }
        every { persongrunnlagService.hentAktivThrows(behandlingId = behandling.id) } returns persongrunnlagForskjelligAdresseForSøkerOgBarn

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .single { it.vilkårType == Vilkår.BOR_MED_SØKER }

        assertThat(borFastHosSøkerVilkår.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertThat(borFastHosSøkerVilkår.begrunnelse).isEqualTo("$PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT- Har ikke samme fast eller delt bostedsadresse som søker")
    }

    @Test
    fun `skal preutfylle bor fast hos søker vilkår til ikke oppfylt om barn har bodd på samme adresse i mindre enn 3 mnd`() {
        // Arrange
        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()
        val fødselsdatoSøker = LocalDate.now().minusYears(30)
        val fødselsdatoBarn = LocalDate.now().minusYears(5)

        val behandling = lagBehandling()

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                søker = lagPerson(type = PersonType.SØKER, aktør = aktørSøker, fødselsdato = fødselsdatoSøker),
                barna = listOf(lagPerson(type = PersonType.BARN, aktør = aktørBarn, fødselsdato = fødselsdatoBarn)),
                behandling = behandling,
                overstyrendeVilkårResultater = emptyMap(),
            )

        val persongrunnlagBarnHarBoddKun2MånederPåSammeAdresseSomSøker =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = aktørSøker.aktivFødselsnummer(),
                barnasIdenter = listOf(aktørBarn.aktivFødselsnummer()),
                søkerAktør = aktørSøker,
                barnAktør = listOf(aktørBarn),
            ).also { persongrunnlag ->
                persongrunnlag.personer.forEach { person ->
                    person.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresse(matrikkelId = 12345L)
                                .also {
                                    it.periode =
                                        DatoIntervallEntitet(
                                            fom = if (person.type == PersonType.SØKER) LocalDate.now().minusYears(1) else LocalDate.now().minusMonths(2),
                                            tom = null,
                                        )
                                    it.person = person
                                },
                        )
                    person.deltBosted = mutableListOf()
                }
            }
        every { persongrunnlagService.hentAktivThrows(behandlingId = behandling.id) } returns persongrunnlagBarnHarBoddKun2MånederPåSammeAdresseSomSøker

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .filter { it.vilkårType == Vilkår.BOR_MED_SØKER }

        assertThat(borFastHosSøkerVilkår).allMatch { it.resultat == Resultat.IKKE_OPPFYLT }
    }

    @Test
    fun `skal gi riktig fom og tom på forskjellige perioder for bor fast hos søker vilkår`() {
        // Arrange
        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()
        val fødselsdatoSøker = LocalDate.now().minusYears(30)
        val fødselsdatoBarn = LocalDate.now().minusYears(10)
        val behandling = lagBehandling()
        val søker = lagPerson(type = PersonType.SØKER, aktør = aktørSøker, fødselsdato = fødselsdatoSøker)
        val barn = lagPerson(type = PersonType.BARN, aktør = aktørBarn, fødselsdato = fødselsdatoBarn)

        val under18ÅrVilkårResultat = lagVilkårResultat(vilkårType = Vilkår.UNDER_18_ÅR, resultat = Resultat.OPPFYLT, periodeFom = fødselsdatoBarn, periodeTom = fødselsdatoBarn.plusYears(18))

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                behandling = behandling,
                søker = søker,
                barna = listOf(barn),
                overstyrendeVilkårResultater =
                    mapOf(
                        aktørBarn.aktørId to listOf(under18ÅrVilkårResultat),
                    ),
            )

        val persongrunnlagSøkerOgBarnFLyttetMellomDiverseAdresser =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = aktørSøker.aktivFødselsnummer(),
                barnasIdenter = listOf(aktørBarn.aktivFødselsnummer()),
                søkerAktør = aktørSøker,
                barnAktør = listOf(aktørBarn),
            ).also { persongrunnlag ->
                persongrunnlag.personer.forEach { person ->
                    person.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresse(matrikkelId = if (person.type == PersonType.SØKER) 12345L else 6789L)
                                .also {
                                    it.periode =
                                        DatoIntervallEntitet(
                                            fom = if (person.type == PersonType.SØKER) fødselsdatoSøker else fødselsdatoBarn,
                                            tom = null,
                                        )
                                    it.person = person
                                },
                            lagGrVegadresse(matrikkelId = if (person.type == PersonType.SØKER) 6789L else 98765L)
                                .also {
                                    it.periode =
                                        DatoIntervallEntitet(
                                            fom = if (person.type == PersonType.SØKER) LocalDate.now().minusYears(15) else LocalDate.now().minusYears(5),
                                            tom = null,
                                        )
                                    it.person = person
                                },
                        )
                    person.deltBosted = mutableListOf()
                }
            }
        every { persongrunnlagService.hentAktivThrows(behandlingId = behandling.id) } returns persongrunnlagSøkerOgBarnFLyttetMellomDiverseAdresser

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .filter { it.vilkårType == Vilkår.BOR_MED_SØKER }

        assertThat(borFastHosSøkerVilkår).hasSize(2)
        assertThat(borFastHosSøkerVilkår.first { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
        assertThat(borFastHosSøkerVilkår.first().periodeFom).isEqualTo(LocalDate.now().minusYears(10))
        assertThat(borFastHosSøkerVilkår.first().periodeTom).isEqualTo(LocalDate.now().minusYears(5).minusDays(1))
        assertThat(borFastHosSøkerVilkår.last { it.resultat == Resultat.OPPFYLT }).isNotNull
        assertThat(borFastHosSøkerVilkår.last().periodeFom).isEqualTo(LocalDate.now().minusYears(5))
        assertThat(borFastHosSøkerVilkår.last().periodeTom).isNull()
    }

    @Test
    fun `skal gi riktig begrunnelse for oppfylt bor fast hos søker vilkår`() {
        // Arrange
        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()

        val behandling = lagBehandling()

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                søker = lagPerson(type = PersonType.SØKER, aktør = aktørSøker),
                barna = listOf(lagPerson(type = PersonType.BARN, aktør = aktørBarn)),
                behandling = behandling,
                overstyrendeVilkårResultater = emptyMap(),
            )

        val persongrunnlagAlleHarSammeAdresse =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = aktørSøker.aktivFødselsnummer(),
                barnasIdenter = listOf(aktørBarn.aktivFødselsnummer()),
                søkerAktør = aktørSøker,
                barnAktør = listOf(aktørBarn),
            ).also { persongrunnlag ->
                persongrunnlag.personer.forEach { person ->
                    person.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresse(matrikkelId = 12345L)
                                .also {
                                    it.periode =
                                        DatoIntervallEntitet(
                                            fom = LocalDate.now().minusYears(10),
                                            tom = null,
                                        )
                                    it.person = person
                                },
                        )
                    person.deltBosted = mutableListOf()
                }
            }
        every { persongrunnlagService.hentAktivThrows(behandlingId = behandling.id) } returns persongrunnlagAlleHarSammeAdresse

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .single {
                    it.vilkårType == Vilkår.BOR_MED_SØKER
                }

        assertThat(borFastHosSøkerVilkår.begrunnelse)
            .isEqualTo("$PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT- Har samme bostedsadresse som søker.")
    }

    @Test
    fun `Skal filtrere vekk perioder før barn har sin første adresse i Norge`() {
        // Arrange
        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()

        val behandling = lagBehandling()

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                søker = lagPerson(type = PersonType.SØKER, aktør = aktørSøker),
                barna = listOf(lagPerson(type = PersonType.BARN, aktør = aktørBarn)),
                behandling = behandling,
                overstyrendeVilkårResultater = emptyMap(),
            )

        val persongrunnlagBarnHarBoddKortereEnnSøkerPåSammeAdresse =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = aktørSøker.aktivFødselsnummer(),
                barnasIdenter = listOf(aktørBarn.aktivFødselsnummer()),
                søkerAktør = aktørSøker,
                barnAktør = listOf(aktørBarn),
            ).also { persongrunnlag ->
                persongrunnlag.personer.forEach { person ->
                    person.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresse(matrikkelId = 12345L)
                                .also {
                                    it.periode =
                                        DatoIntervallEntitet(
                                            fom = if (person.type == PersonType.SØKER) LocalDate.now().minusYears(10) else LocalDate.now().minusYears(2),
                                            tom = null,
                                        )
                                    it.person = person
                                },
                        )
                    person.deltBosted = mutableListOf()
                }
            }
        every { persongrunnlagService.hentAktivThrows(behandlingId = behandling.id) } returns persongrunnlagBarnHarBoddKortereEnnSøkerPåSammeAdresse

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .single {
                    it.vilkårType == Vilkår.BOR_MED_SØKER
                }

        assertThat(borFastHosSøkerVilkår.periodeFom).isEqualTo(LocalDate.now().minusYears(2))
    }

    @Test
    fun `Skal filtrere vekk perioder før søker har sin første adresse i Norge`() {
        // Arrange
        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()

        val behandling = lagBehandling()

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                søker = lagPerson(type = PersonType.SØKER, aktør = aktørSøker),
                barna = listOf(lagPerson(type = PersonType.BARN, aktør = aktørBarn)),
                behandling = behandling,
                overstyrendeVilkårResultater = emptyMap(),
            )

        val persongrunnlagBarnHarBoddLengerEnnSøkerPåSammeAdresse =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = aktørSøker.aktivFødselsnummer(),
                barnasIdenter = listOf(aktørBarn.aktivFødselsnummer()),
                søkerAktør = aktørSøker,
                barnAktør = listOf(aktørBarn),
            ).also { persongrunnlag ->
                persongrunnlag.personer.forEach { person ->
                    person.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresse(matrikkelId = 12345L)
                                .also {
                                    it.periode =
                                        DatoIntervallEntitet(
                                            fom = if (person.type == PersonType.SØKER) LocalDate.now().minusYears(2) else LocalDate.now().minusYears(10),
                                            tom = null,
                                        )
                                    it.person = person
                                },
                        )
                    person.deltBosted = mutableListOf()
                }
            }
        every { persongrunnlagService.hentAktivThrows(behandlingId = behandling.id) } returns persongrunnlagBarnHarBoddLengerEnnSøkerPåSammeAdresse

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .single {
                    it.vilkårType == Vilkår.BOR_MED_SØKER
                }

        assertThat(borFastHosSøkerVilkår.periodeFom).isEqualTo(LocalDate.now().minusYears(2))
    }

    @Test
    fun `Skal sette 'oppfylt' og bor fast med søker ved delt-bosted-adresse`() {
        // Arrange
        val nåDato = LocalDate.now()

        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()

        val behandling = lagBehandling()

        val persongrunnlagMedSammeAdresseForAllePersoner =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = aktørSøker.aktivFødselsnummer(),
                barnasIdenter = listOf(aktørBarn.aktivFødselsnummer()),
                søkerAktør = aktørSøker,
                barnAktør = listOf(aktørBarn),
            ).also { persongrunnlag ->
                val søker = persongrunnlag.personer.single { it.type == PersonType.SØKER }
                søker.bostedsadresser =
                    mutableListOf(
                        lagGrVegadresse(matrikkelId = 12345L)
                            .also {
                                it.periode =
                                    DatoIntervallEntitet(
                                        fom = nåDato.minusYears(2),
                                        tom = null,
                                    )
                                it.person = søker
                            },
                    )
                val barn = persongrunnlag.personer.single { it.type == PersonType.BARN }
                barn.bostedsadresser =
                    mutableListOf(
                        lagGrVegadresse(matrikkelId = 54321L)
                            .also {
                                it.periode =
                                    DatoIntervallEntitet(
                                        fom = nåDato.minusYears(2),
                                        tom = null,
                                    )
                                it.person = barn
                            },
                    )
                barn.deltBosted =
                    mutableListOf(
                        lagGrVegadresseDeltBosted(matrikkelId = 12345L)
                            .also {
                                it.periode =
                                    DatoIntervallEntitet(
                                        fom = nåDato.minusYears(2),
                                        tom = null,
                                    )
                                it.person = barn
                            },
                    )
            }
        every { persongrunnlagService.hentAktivThrows(behandlingId = behandling.id) } returns persongrunnlagMedSammeAdresseForAllePersoner

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                behandling = behandling,
                søker = persongrunnlagMedSammeAdresseForAllePersoner.søker,
                barna = persongrunnlagMedSammeAdresseForAllePersoner.barna,
                overstyrendeVilkårResultater = emptyMap(),
            )
        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .single {
                    it.vilkårType == Vilkår.BOR_MED_SØKER
                }

        assertThat(borFastHosSøkerVilkår.periodeFom).isEqualTo(LocalDate.now().minusYears(2))
        assertThat(borFastHosSøkerVilkår.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(borFastHosSøkerVilkår.begrunnelse).isEqualTo("Fylt ut automatisk fra registerdata i PDL\n- Har samme delte bostedsadresse som søker.")
    }
}
