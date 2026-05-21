package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.genererbarnasvilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class AutomatiskVilkårUtfyllingServiceTest {
    private val vilkårsvurderingService: VilkårsvurderingService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()

    private val automatiskVilkårUtfyllingService =
        AutomatiskVilkårUtfyllingService(
            vilkårsvurderingService,
            persongrunnlagService,
            behandlingHentOgPersisterService,
            featureToggleService,
        )

    @BeforeEach
    fun setup() {
        every { featureToggleService.isEnabled(FeatureToggle.KAN_GENERERE_BARNAS_VILKÅR) } returns true
    }

    @Nested
    inner class UtfyllVilkårAutomatiskForNyeBarn {
        @Test
        fun `søkers BOSATT_I_RIKET kopieres til barnets BOSATT_I_RIKET, LOVLIG_OPPHOLD og BOR_MED_SØKER`() {
            // Arrange
            val behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)
            val søkerAktør = randomAktør()
            val barnAktør = randomAktør()
            val barnFødselsdato = LocalDate.of(2020, 1, 1)
            val søkerVilkårFom = LocalDate.of(2020, 1, 1)
            val søkerVilkårTom = LocalDate.of(2025, 12, 31)

            val søker = lagPerson(aktør = søkerAktør, type = PersonType.SØKER)
            val barn = lagPerson(aktør = barnAktør, type = PersonType.BARN, fødselsdato = barnFødselsdato)
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = { vv ->
                        val søkerPersonResultat =
                            lagPersonResultat(
                                vilkårsvurdering = vv,
                                aktør = søkerAktør,
                                lagVilkårResultater = { pr ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = søkerVilkårFom,
                                            periodeTom = søkerVilkårTom,
                                            behandlingId = behandling.id,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            )

                        val barnPersonResultat =
                            lagPersonResultat(
                                vilkårsvurdering = vv,
                                aktør = barnAktør,
                                lagVilkårResultater = { pr ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.UNDER_18_ÅR,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = barnFødselsdato,
                                            periodeTom = barnFødselsdato.plusYears(18),
                                            behandlingId = behandling.id,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.GIFT_PARTNERSKAP,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = barnFødselsdato,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.IKKE_VURDERT,
                                            periodeFom = null,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.LOVLIG_OPPHOLD,
                                            resultat = Resultat.IKKE_VURDERT,
                                            periodeFom = null,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOR_MED_SØKER,
                                            resultat = Resultat.IKKE_VURDERT,
                                            periodeFom = null,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            )

                        setOf(søkerPersonResultat, barnPersonResultat)
                    },
                )

            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
            every { persongrunnlagService.finnNyeBarn(behandling, null) } returns listOf(barn)
            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
            every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id) } returns vilkårsvurdering

            // Act
            automatiskVilkårUtfyllingService.utfyllVilkårAutomatiskForNyeBarn(behandling.id)

            // Assert
            val barnResultat = vilkårsvurdering.personResultater.first { it.aktør == barnAktør }
            val vilkårSomSkalHaSøkersPeriode = setOf(Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD, Vilkår.BOR_MED_SØKER)

            vilkårSomSkalHaSøkersPeriode.forEach { vilkår ->
                val vilkårForBarn = barnResultat.vilkårResultater.filter { it.vilkårType == vilkår }
                assertEquals(1, vilkårForBarn.size, "Forventet 1 periode for $vilkår")
                assertEquals(søkerVilkårFom, vilkårForBarn.single().periodeFom, "Feil fom for $vilkår")
                assertEquals(søkerVilkårTom, vilkårForBarn.single().periodeTom, "Feil tom for $vilkår")
                assertEquals(Resultat.OPPFYLT, vilkårForBarn.single().resultat, "Feil resultat for $vilkår")
            }
        }

        @Test
        fun `barnets UNDER_18_ÅR og GIFT_PARTNERSKAP beholdes uendret etter generering`() {
            // Arrange
            val behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)
            val søkerAktør = randomAktør()
            val barnAktør = randomAktør()
            val barnFødselsdato = LocalDate.of(2020, 1, 1)
            val forventetUnder18Tom = barnFødselsdato.plusYears(18)

            val søker = lagPerson(aktør = søkerAktør, type = PersonType.SØKER)
            val barn = lagPerson(aktør = barnAktør, type = PersonType.BARN, fødselsdato = barnFødselsdato)
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = { vv ->
                        val søkerPersonResultat =
                            lagPersonResultat(
                                vilkårsvurdering = vv,
                                aktør = søkerAktør,
                                lagVilkårResultater = { pr ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = barnFødselsdato,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            )

                        val barnPersonResultat =
                            lagPersonResultat(
                                vilkårsvurdering = vv,
                                aktør = barnAktør,
                                lagVilkårResultater = { pr ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.UNDER_18_ÅR,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = barnFødselsdato,
                                            periodeTom = forventetUnder18Tom,
                                            behandlingId = behandling.id,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.GIFT_PARTNERSKAP,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = barnFødselsdato,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.IKKE_VURDERT,
                                            periodeFom = null,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            )

                        setOf(søkerPersonResultat, barnPersonResultat)
                    },
                )

            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
            every { persongrunnlagService.finnNyeBarn(behandling, null) } returns listOf(barn)
            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
            every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id) } returns vilkårsvurdering

            // Act
            automatiskVilkårUtfyllingService.utfyllVilkårAutomatiskForNyeBarn(behandling.id)

            // Assert
            val barnResultat = vilkårsvurdering.personResultater.first { it.aktør == barnAktør }

            val under18Vilkår = barnResultat.vilkårResultater.filter { it.vilkårType == Vilkår.UNDER_18_ÅR }
            assertEquals(1, under18Vilkår.size)
            assertEquals(barnFødselsdato, under18Vilkår.single().periodeFom)
            assertEquals(forventetUnder18Tom, under18Vilkår.single().periodeTom)

            val giftPartnerskapVilkår = barnResultat.vilkårResultater.filter { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }
            assertEquals(1, giftPartnerskapVilkår.size)
            assertEquals(barnFødselsdato, giftPartnerskapVilkår.single().periodeFom)
        }

        @Test
        fun `barnets vilkår beskjæres fra barnets fødselsdato når søkers vilkår starter tidligere`() {
            // Arrange
            val behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)
            val søkerAktør = randomAktør()
            val barnAktør = randomAktør()
            val barnFødselsdato = LocalDate.of(2022, 6, 15)
            val søkerVilkårFom = LocalDate.of(2019, 1, 1) // Søker har vilkår fra FØR barnets fødsel

            val søker = lagPerson(aktør = søkerAktør, type = PersonType.SØKER)
            val barn = lagPerson(aktør = barnAktør, type = PersonType.BARN, fødselsdato = barnFødselsdato)
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = { vv ->
                        val søkerPersonResultat =
                            lagPersonResultat(
                                vilkårsvurdering = vv,
                                aktør = søkerAktør,
                                lagVilkårResultater = { pr ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = søkerVilkårFom,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            )

                        val barnPersonResultat =
                            lagPersonResultat(
                                vilkårsvurdering = vv,
                                aktør = barnAktør,
                                lagVilkårResultater = { pr ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.UNDER_18_ÅR,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = barnFødselsdato,
                                            periodeTom = barnFødselsdato.plusYears(18),
                                            behandlingId = behandling.id,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.IKKE_VURDERT,
                                            periodeFom = null,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            )

                        setOf(søkerPersonResultat, barnPersonResultat)
                    },
                )

            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
            every { persongrunnlagService.finnNyeBarn(behandling, null) } returns listOf(barn)
            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
            every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id) } returns vilkårsvurdering

            // Act
            automatiskVilkårUtfyllingService.utfyllVilkårAutomatiskForNyeBarn(behandling.id)

            // Assert
            val barnResultat = vilkårsvurdering.personResultater.first { it.aktør == barnAktør }
            val bosattVilkår = barnResultat.vilkårResultater.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

            assertEquals(1, bosattVilkår.size)
            assertEquals(
                barnFødselsdato,
                bosattVilkår.single().periodeFom,
                "Barnets BOSATT_I_RIKET skal starte fra barnets fødselsdato",
            )
        }

        @Test
        fun `to søkerperioder med likt resultat slås sammen til én periode for barnet`() {
            // Arrange
            val behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)
            val søkerAktør = randomAktør()
            val barnAktør = randomAktør()
            val barnFødselsdato = LocalDate.of(2020, 1, 1)

            val søker = lagPerson(aktør = søkerAktør, type = PersonType.SØKER)
            val barn = lagPerson(aktør = barnAktør, type = PersonType.BARN, fødselsdato = barnFødselsdato)
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = { vv ->
                        val søkerPersonResultat =
                            lagPersonResultat(
                                vilkårsvurdering = vv,
                                aktør = søkerAktør,
                                lagVilkårResultater = { pr ->
                                    // To påfølgende OPPFYLT-perioder hos søker (back-to-back, ingen gap)
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2020, 1, 1),
                                            periodeTom = LocalDate.of(2021, 12, 31),
                                            behandlingId = behandling.id,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2022, 1, 1),
                                            periodeTom = LocalDate.of(2023, 12, 31),
                                            behandlingId = behandling.id,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            )

                        val barnPersonResultat =
                            lagPersonResultat(
                                vilkårsvurdering = vv,
                                aktør = barnAktør,
                                lagVilkårResultater = { pr ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.UNDER_18_ÅR,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = barnFødselsdato,
                                            periodeTom = barnFødselsdato.plusYears(18),
                                            behandlingId = behandling.id,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.IKKE_VURDERT,
                                            periodeFom = null,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            )

                        setOf(søkerPersonResultat, barnPersonResultat)
                    },
                )

            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
            every { persongrunnlagService.finnNyeBarn(behandling, null) } returns listOf(barn)
            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
            every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id) } returns vilkårsvurdering

            // Act
            automatiskVilkårUtfyllingService.utfyllVilkårAutomatiskForNyeBarn(behandling.id)

            // Assert
            val barnResultat = vilkårsvurdering.personResultater.first { it.aktør == barnAktør }
            val bosattVilkår = barnResultat.vilkårResultater.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

            assertEquals(1, bosattVilkår.size, "To OPPFYLT-perioder back-to-back skal slås til én")
            assertEquals(LocalDate.of(2020, 1, 1), bosattVilkår.single().periodeFom)
            assertEquals(LocalDate.of(2023, 12, 31), bosattVilkår.single().periodeTom)
        }

        @Test
        fun `to søkerperioder med ulikt resultat beholdes som separate perioder for barnet`() {
            // Arrange
            val behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)
            val søkerAktør = randomAktør()
            val barnAktør = randomAktør()
            val barnFødselsdato = LocalDate.of(2020, 1, 1)

            val søker = lagPerson(aktør = søkerAktør, type = PersonType.SØKER)
            val barn = lagPerson(aktør = barnAktør, type = PersonType.BARN, fødselsdato = barnFødselsdato)
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = { vv ->
                        val søkerPersonResultat =
                            lagPersonResultat(
                                vilkårsvurdering = vv,
                                aktør = søkerAktør,
                                lagVilkårResultater = { pr ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2020, 1, 1),
                                            periodeTom = LocalDate.of(2021, 12, 31),
                                            behandlingId = behandling.id,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.IKKE_OPPFYLT,
                                            periodeFom = LocalDate.of(2022, 1, 1),
                                            periodeTom = LocalDate.of(2023, 12, 31),
                                            behandlingId = behandling.id,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            )

                        val barnPersonResultat =
                            lagPersonResultat(
                                vilkårsvurdering = vv,
                                aktør = barnAktør,
                                lagVilkårResultater = { pr ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.UNDER_18_ÅR,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = barnFødselsdato,
                                            periodeTom = barnFødselsdato.plusYears(18),
                                            behandlingId = behandling.id,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.IKKE_VURDERT,
                                            periodeFom = null,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.LOVLIG_OPPHOLD,
                                            resultat = Resultat.IKKE_VURDERT,
                                            periodeFom = null,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOR_MED_SØKER,
                                            resultat = Resultat.IKKE_VURDERT,
                                            periodeFom = null,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            )

                        setOf(søkerPersonResultat, barnPersonResultat)
                    },
                )

            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
            every { persongrunnlagService.finnNyeBarn(behandling, null) } returns listOf(barn)
            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
            every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id) } returns vilkårsvurdering

            // Act
            automatiskVilkårUtfyllingService.utfyllVilkårAutomatiskForNyeBarn(behandling.id)

            // Assert
            val barnResultat = vilkårsvurdering.personResultater.first { it.aktør == barnAktør }
            val bosattVilkår = barnResultat.vilkårResultater.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

            assertEquals(2, bosattVilkår.size, "To perioder med ulikt resultat skal beholdes som separate perioder")
            assertEquals(
                setOf(Resultat.OPPFYLT, Resultat.IKKE_OPPFYLT),
                bosattVilkår.map { it.resultat }.toSet(),
            )
        }

        @Test
        fun `genererte vilkår for to barn behandles uavhengig basert på hvert barns fødselsdato`() {
            // Arrange
            val behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)
            val søkerAktør = randomAktør()
            val barn1Aktør = randomAktør()
            val barn2Aktør = randomAktør()
            val barn1Fødselsdato = LocalDate.of(2020, 3, 1)
            val barn2Fødselsdato = LocalDate.of(2022, 9, 1)
            val søkerVilkårFom = LocalDate.of(2018, 1, 1)

            val søker = lagPerson(aktør = søkerAktør, type = PersonType.SØKER)
            val barn1 = lagPerson(aktør = barn1Aktør, type = PersonType.BARN, fødselsdato = barn1Fødselsdato)
            val barn2 = lagPerson(aktør = barn2Aktør, type = PersonType.BARN, fødselsdato = barn2Fødselsdato)
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1, barn2)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = { vv ->
                        val søkerPersonResultat =
                            lagPersonResultat(
                                vilkårsvurdering = vv,
                                aktør = søkerAktør,
                                lagVilkårResultater = { pr ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = søkerVilkårFom,
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            )

                        fun lagBarnPersonResultat(
                            barnAktør: Aktør,
                            fødselsdato: LocalDate,
                        ) = lagPersonResultat(
                            vilkårsvurdering = vv,
                            aktør = barnAktør,
                            lagVilkårResultater = { pr ->
                                setOf(
                                    lagVilkårResultat(
                                        personResultat = pr,
                                        vilkårType = Vilkår.UNDER_18_ÅR,
                                        resultat = Resultat.OPPFYLT,
                                        periodeFom = fødselsdato,
                                        periodeTom = fødselsdato.plusYears(18),
                                        behandlingId = behandling.id,
                                    ),
                                    lagVilkårResultat(
                                        personResultat = pr,
                                        vilkårType = Vilkår.BOSATT_I_RIKET,
                                        resultat = Resultat.IKKE_VURDERT,
                                        periodeFom = null,
                                        periodeTom = null,
                                        behandlingId = behandling.id,
                                    ),
                                )
                            },
                            lagAnnenVurderinger = { emptySet() },
                        )

                        setOf(
                            søkerPersonResultat,
                            lagBarnPersonResultat(barn1Aktør, barn1Fødselsdato),
                            lagBarnPersonResultat(barn2Aktør, barn2Fødselsdato),
                        )
                    },
                )

            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
            every { persongrunnlagService.finnNyeBarn(behandling, null) } returns listOf(barn1, barn2)
            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
            every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id) } returns vilkårsvurdering

            // Act
            automatiskVilkårUtfyllingService.utfyllVilkårAutomatiskForNyeBarn(behandling.id)

            // Assert
            val barn1Resultat = vilkårsvurdering.personResultater.first { it.aktør == barn1Aktør }
            val barn1Bosatt = barn1Resultat.vilkårResultater.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
            assertEquals(barn1Fødselsdato, barn1Bosatt.single().periodeFom, "Barn1 skal ha fom = sin fødselsdato")

            val barn2Resultat = vilkårsvurdering.personResultater.first { it.aktør == barn2Aktør }
            val barn2Bosatt = barn2Resultat.vilkårResultater.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
            assertEquals(barn2Fødselsdato, barn2Bosatt.single().periodeFom, "Barn2 skal ha fom = sin fødselsdato")
        }

        @Test
        fun `kaster Feil når behandlingstype er REVURDERING med årsak som ikke er SØKNAD`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = BehandlingKategori.EØS,
                    behandlingType = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                )
            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling

            // Act & Assert
            assertThrows<Feil> { automatiskVilkårUtfyllingService.utfyllVilkårAutomatiskForNyeBarn(behandling.id) }
        }

        @Test
        fun `kaster Feil når behandlingskategori er NASJONAL`() {
            // Arrange
            val behandling = lagBehandling(behandlingKategori = BehandlingKategori.NASJONAL)
            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling

            // Act & Assert
            assertThrows<Feil> { automatiskVilkårUtfyllingService.utfyllVilkårAutomatiskForNyeBarn(behandling.id) }
        }

        @Test
        fun `kaster Feil når det ikke finnes nye barn i behandlingen`() {
            // Arrange
            val behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)
            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
            every { persongrunnlagService.finnNyeBarn(behandling, null) } returns emptyList()

            // Act & Assert
            assertThrows<Feil> { automatiskVilkårUtfyllingService.utfyllVilkårAutomatiskForNyeBarn(behandling.id) }
        }

        @Test
        fun `eksisterende barn som ikke er nye får ikke generert vilkår`() {
            // Arrange
            val behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)
            val søkerAktør = randomAktør()
            val nyttBarnAktør = randomAktør()
            val eksisterendeBarnAktør = randomAktør()
            val nyttBarnFødselsdato = LocalDate.of(2023, 6, 1)
            val eksisterendeBarnFødselsdato = LocalDate.of(2020, 1, 1)

            val søker = lagPerson(aktør = søkerAktør, type = PersonType.SØKER)
            val nyttBarn = lagPerson(aktør = nyttBarnAktør, type = PersonType.BARN, fødselsdato = nyttBarnFødselsdato)
            val eksisterendeBarn = lagPerson(aktør = eksisterendeBarnAktør, type = PersonType.BARN, fødselsdato = eksisterendeBarnFødselsdato)
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, nyttBarn, eksisterendeBarn)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = { vv ->
                        val søkerPersonResultat =
                            lagPersonResultat(
                                vilkårsvurdering = vv,
                                aktør = søkerAktør,
                                lagVilkårResultater = { pr ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = pr,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2018, 1, 1),
                                            periodeTom = null,
                                            behandlingId = behandling.id,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            )

                        fun lagBarnResultat(
                            barnAktør: Aktør,
                            fødselsdato: LocalDate,
                        ) = lagPersonResultat(
                            vilkårsvurdering = vv,
                            aktør = barnAktør,
                            lagVilkårResultater = { pr ->
                                setOf(
                                    lagVilkårResultat(
                                        personResultat = pr,
                                        vilkårType = Vilkår.UNDER_18_ÅR,
                                        resultat = Resultat.OPPFYLT,
                                        periodeFom = fødselsdato,
                                        periodeTom = fødselsdato.plusYears(18),
                                        behandlingId = behandling.id,
                                    ),
                                    lagVilkårResultat(
                                        personResultat = pr,
                                        vilkårType = Vilkår.BOSATT_I_RIKET,
                                        resultat = Resultat.IKKE_VURDERT,
                                        periodeFom = null,
                                        periodeTom = null,
                                        behandlingId = behandling.id,
                                    ),
                                )
                            },
                            lagAnnenVurderinger = { emptySet() },
                        )

                        setOf(
                            søkerPersonResultat,
                            lagBarnResultat(nyttBarnAktør, nyttBarnFødselsdato),
                            lagBarnResultat(eksisterendeBarnAktør, eksisterendeBarnFødselsdato),
                        )
                    },
                )

            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
            every { persongrunnlagService.finnNyeBarn(behandling, null) } returns listOf(nyttBarn)
            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
            every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id) } returns vilkårsvurdering

            // Act
            automatiskVilkårUtfyllingService.utfyllVilkårAutomatiskForNyeBarn(behandling.id)

            // Assert
            val nyttBarnResultat = vilkårsvurdering.personResultater.first { it.aktør == nyttBarnAktør }
            val nyttBarnBosatt = nyttBarnResultat.vilkårResultater.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
            assertEquals(1, nyttBarnBosatt.size, "Nytt barn skal ha fått generert BOSATT_I_RIKET")
            assertEquals(nyttBarnFødselsdato, nyttBarnBosatt.single().periodeFom, "Nytt barn skal ha fom = sin fødselsdato")

            // Assert
            val eksisterendeBarnResultat = vilkårsvurdering.personResultater.first { it.aktør == eksisterendeBarnAktør }
            val eksisterendeBarnBosatt = eksisterendeBarnResultat.vilkårResultater.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
            assertEquals(1, eksisterendeBarnBosatt.size)
            assertEquals(Resultat.IKKE_VURDERT, eksisterendeBarnBosatt.single().resultat, "Eksisterende barn skal ikke ha fått endret vilkår")
            assertEquals(null, eksisterendeBarnBosatt.single().periodeFom, "Eksisterende barn skal ikke ha fått endret periodeFom")
        }
    }
}
