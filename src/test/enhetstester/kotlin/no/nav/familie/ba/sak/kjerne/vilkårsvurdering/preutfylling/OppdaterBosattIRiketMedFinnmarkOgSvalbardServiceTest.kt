package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.cucumber.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagAdresse
import no.nav.familie.ba.sak.datagenerator.lagAdresser
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagSøknad
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_FRA_SØKNAD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.svalbard.SvalbardKommune
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class OppdaterBosattIRiketMedFinnmarkOgSvalbardServiceTest {
    private val systemOnlyPdlRestKlient: SystemOnlyPdlRestKlient = mockk(relaxed = true)
    private val søknadService: SøknadService = mockk(relaxed = true)
    private val persongrunnlagService: PersongrunnlagService = mockk(relaxed = true)
    private val featureToggleService = mockk<FeatureToggleService>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()

    private val oppdaterBosattIRiketMedFinnmarkOgSvalbardService =
        OppdaterBosattIRiketMedFinnmarkOgSvalbardService(
            persongrunnlagService = persongrunnlagService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )

    @BeforeEach
    fun setup() {
        every { featureToggleService.isEnabled(FeatureToggle.NY_PREUTFYLLING_FOR_BOSATT_I_RIKET_VILKÅR_VED_AUTOVEDTAK_FINNMARK_SVALBARD) } returns true
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(any(), any()) } returns emptyList()
    }

    @Nested
    inner class OppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat {
        @Test
        fun `skal oppdatere utvidet vilkårsvurderinger for 'Bosatt i riket'-vilkåret for person som har oppholdsadresse på Svalbard`() {
            // Arrange
            val behandling = lagBehandling()

            val personResultat =
                lagPersonResultat(
                    vilkårsvurdering = lagVilkårsvurdering(behandling = behandling),
                    lagVilkårResultater = {
                        setOf(
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2021, 1, 1),
                                periodeTom = null,
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = emptyList(),
                            ),
                        )
                    },
                )

            val adresser =
                lagAdresser(
                    oppholdsadresse =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = LocalDate.of(2022, 1, 1),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                            ),
                        ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(any(), any()) } returns emptyList()

            // Act
            val vilkårresultat =
                oppdaterBosattIRiketMedFinnmarkOgSvalbardService.oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
                    personResultat = personResultat,
                    adresserForPerson = adresser,
                    behandling = behandling,
                )

            // Assert
            assertThat(vilkårresultat).hasSize(2)
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
                assertThat(it.periodeTom).isNull()
                assertThat(it.utdypendeVilkårsvurderinger).containsOnly(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)
            }
        }

        @Test
        fun `skal oppdatere utvidet vilkårsvurderinger for 'Bosatt i riket'-vilkåret for person som har bostedsadresse i Finnmark eller Nord-Troms`() {
            // Arrange
            val behandling = lagBehandling()

            val personResultat =
                lagPersonResultat(
                    vilkårsvurdering = lagVilkårsvurdering(behandling = behandling),
                    lagVilkårResultater = {
                        setOf(
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2021, 1, 1),
                                periodeTom = null,
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = emptyList(),
                                begrunnelse = "Begrunnelse som ikke skal endres",
                            ),
                        )
                    },
                )

            val adresser =
                lagAdresser(
                    bostedsadresser =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = LocalDate.of(2022, 1, 1),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer),
                            ),
                        ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, any()) } returns emptyList()

            // Act
            val vilkårresultat =
                oppdaterBosattIRiketMedFinnmarkOgSvalbardService.oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
                    personResultat = personResultat,
                    adresserForPerson = adresser,
                    behandling = behandling,
                )

            // Assert
            assertThat(vilkårresultat).hasSize(2)
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
                assertThat(it.periodeTom).isNull()
                assertThat(it.utdypendeVilkårsvurderinger).containsOnly(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
            }
        }

        @Test
        fun `skal oppdatere utvidet vilkårsvurderinger for 'Bosatt i riket'-vilkåret for person som har delt bosted i Finnmark eller Nord-Troms`() {
            // Arrange
            val behandling = lagBehandling()

            val personResultat =
                lagPersonResultat(
                    vilkårsvurdering = lagVilkårsvurdering(behandling = behandling),
                    lagVilkårResultater = {
                        setOf(
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2021, 1, 1),
                                periodeTom = null,
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = emptyList(),
                            ),
                        )
                    },
                )

            val adresser =
                lagAdresser(
                    delteBosteder =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = LocalDate.of(2021, 1, 1),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer),
                            ),
                        ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, any()) } returns emptyList()

            // Act
            val vilkårresultat =
                oppdaterBosattIRiketMedFinnmarkOgSvalbardService.oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
                    personResultat = personResultat,
                    adresserForPerson = adresser,
                    behandling = behandling,
                )

            // Assert
            assertThat(vilkårresultat).hasSize(2)
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
                assertThat(it.periodeTom).isNull()
                assertThat(it.utdypendeVilkårsvurderinger).containsOnly(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
            }
        }

        @Test
        fun `skal oppdatere utvidet vilkårsvurderinger for 'Bosatt i riket'-vilkåret om man har flyttet bort fra Svalbard`() {
            // Arrange
            val behandling = lagBehandling()

            val personResultat =
                lagPersonResultat(
                    vilkårsvurdering = lagVilkårsvurdering(behandling = behandling),
                    lagVilkårResultater = {
                        setOf(
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2020, 1, 1),
                                periodeTom = LocalDate.of(2020, 12, 31),
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = listOf(),
                                begrunnelse = "Begrunnelse som ikke skal endres",
                            ),
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2021, 1, 1),
                                periodeTom = null,
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD),
                                begrunnelse = "Begrunnelse som skal endres",
                            ),
                        )
                    },
                )

            val adresser =
                lagAdresser(
                    oppholdsadresse =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = LocalDate.of(2021, 1, 1),
                                gyldigTilOgMed = LocalDate.of(2025, 10, 15),
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                            ),
                        ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, any()) } returns emptyList()

            // Act
            val vilkårresultat =
                oppdaterBosattIRiketMedFinnmarkOgSvalbardService.oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
                    personResultat = personResultat,
                    adresserForPerson = adresser,
                    behandling = behandling,
                )

            // Assert
            assertThat(vilkårresultat).hasSize(4)
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo("Begrunnelse som ikke skal endres")
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2020, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2020, 12, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 10, 15))
                assertThat(it.utdypendeVilkårsvurderinger).isEqualTo(listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD))
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 10, 16))
                assertThat(it.periodeTom).isNull()
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
        }

        @Test
        fun `skal oppdatere utvidet vilkårsvurderinger for 'Bosatt i riket'-vilkåret om man har flyttet bort fra Finnmark eller Nord-Troms`() {
            // Arrange
            val behandling = lagBehandling()

            val personResultat =
                lagPersonResultat(
                    vilkårsvurdering = lagVilkårsvurdering(behandling = behandling),
                    lagVilkårResultater = {
                        setOf(
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2020, 1, 1),
                                periodeTom = LocalDate.of(2020, 12, 31),
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = listOf(),
                                begrunnelse = "Begrunnelse som ikke skal endres",
                            ),
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2021, 1, 1),
                                periodeTom = null,
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS),
                                begrunnelse = "Begrunnelse som skal endres",
                            ),
                        )
                    },
                )

            val adresser =
                lagAdresser(
                    bostedsadresser =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = LocalDate.of(2021, 1, 1),
                                gyldigTilOgMed = LocalDate.of(2025, 10, 15),
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                            ),
                        ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, any()) } returns emptyList()

            // Act
            val vilkårresultat =
                oppdaterBosattIRiketMedFinnmarkOgSvalbardService.oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
                    personResultat = personResultat,
                    adresserForPerson = adresser,
                    behandling = behandling,
                )

            // Assert
            assertThat(vilkårresultat).hasSize(4)
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo("Begrunnelse som ikke skal endres")
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2020, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2020, 12, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 10, 15))
                assertThat(it.utdypendeVilkårsvurderinger).isEqualTo(listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS))
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 10, 16))
                assertThat(it.periodeTom).isNull()
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
        }

        @Test
        fun `skal ikke endre 'Bosatt i riket'-vilkåret hvis personen hverken bor på Svalbard eller i Finnmark eller Nord-Troms`() {
            // Arrange
            val behandling = lagBehandling()

            val personResultat =
                lagPersonResultat(
                    vilkårsvurdering = lagVilkårsvurdering(behandling = behandling),
                    lagVilkårResultater = {
                        setOf(
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2021, 1, 1),
                                periodeTom = null,
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = emptyList(),
                                begrunnelse = "Begrunnelse som ikke skal endres",
                            ),
                        )
                    },
                )

            val adresser =
                lagAdresser(
                    bostedsadresser =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = LocalDate.of(2021, 1, 1),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = "0301"),
                            ),
                        ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, any()) } returns emptyList()

            // Act
            val vilkårresultat =
                oppdaterBosattIRiketMedFinnmarkOgSvalbardService.oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
                    personResultat = personResultat,
                    adresserForPerson = adresser,
                    behandling = behandling,
                )

            // Assert
            assertThat(vilkårresultat).hasSize(1)
            assertThat(vilkårresultat.single()).isEqualTo(personResultat.vilkårResultater.first())
        }
    }
}
