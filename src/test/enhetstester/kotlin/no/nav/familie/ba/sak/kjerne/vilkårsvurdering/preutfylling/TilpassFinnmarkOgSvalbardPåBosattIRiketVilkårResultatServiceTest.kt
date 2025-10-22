package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAdresse
import no.nav.familie.ba.sak.datagenerator.lagAdresser
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms
import no.nav.familie.kontrakter.felles.svalbard.SvalbardKommune
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TilpassFinnmarkOgSvalbardPåBosattIRiketVilkårResultatServiceTest {
    private val pdlRestKlient: SystemOnlyPdlRestKlient = mockk(relaxed = true)

    private val featureToggleService = mockk<FeatureToggleService>()

    private val tilpassFinnmarkOgSvalbardPåBosattIRiketService =
        TilpassFinnmarkOgSvalbardPåBosattIRiketService(
            pdlRestKlient = pdlRestKlient,
        )

    @BeforeEach
    fun setup() {
        every { featureToggleService.isEnabled(FeatureToggle.NY_PREUTFYLLING_FOR_BOSATT_I_RIKET_VILKÅR_VED_AUTOVEDTAK_FINNMARK_SVALBARD) } returns true
    }

    @Test
    fun `skal generere bosatt i riket vilkår for person som har oppholdsadresse på Svalbard`() {
        // Arrange
        val behandling = lagBehandling()

        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

        val personResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
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
        vilkårsvurdering.personResultater.plus(personResultat)

        val adresser =
            lagAdresser(
                oppholdsadresse =
                    listOf(
                        lagAdresse(
                            gyldigFraOgMed = LocalDate.of(2021, 1, 1),
                            gyldigTilOgMed = null,
                            matrikkeladresse = lagMatrikkeladresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                        ),
                    ),
            )

        // Act
        val vilkårresultat =
            tilpassFinnmarkOgSvalbardPåBosattIRiketService.tilpassFinnmarkOgSvalbardtilleggPåBosattIRiketVilkårResultat(
                personResultat = personResultat,
                adresserForPerson = adresser,
                behandling = behandling,
                cutOffFomDato = LocalDate.of(2025, 9, 1),
            )

        // Assert
        assertThat(vilkårresultat).hasSize(2)
        assertThat(vilkårresultat).anySatisfy {
            assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
            assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(it.begrunnelse).isEqualTo("")
            assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
            assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
            assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
        }
        assertThat(vilkårresultat).anySatisfy {
            assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
            assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(it.begrunnelse).isEqualTo("Fylt ut automatisk fra registerdata i PDL\n")
            assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
            assertThat(it.periodeTom).isNull()
            assertThat(it.utdypendeVilkårsvurderinger).containsOnly(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)
        }
    }

    @Test
    fun `skal generere bosatt i riket vilkår for person som har bostedsadresse i Finnmark eller Nord-Troms`() {
        // Arrange
        val behandling = lagBehandling()

        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

        val personResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
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
        vilkårsvurdering.personResultater.plus(personResultat)

        val adresser =
            lagAdresser(
                bostedsadresser =
                    listOf(
                        lagAdresse(
                            gyldigFraOgMed = LocalDate.of(2021, 1, 1),
                            gyldigTilOgMed = null,
                            matrikkeladresse = lagMatrikkeladresse(kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer),
                        ),
                    ),
            )

        // Act
        val vilkårresultat =
            tilpassFinnmarkOgSvalbardPåBosattIRiketService.tilpassFinnmarkOgSvalbardtilleggPåBosattIRiketVilkårResultat(
                personResultat = personResultat,
                adresserForPerson = adresser,
                behandling = behandling,
                cutOffFomDato = LocalDate.of(2025, 9, 1),
            )

        // Assert
        assertThat(vilkårresultat).hasSize(2)
        assertThat(vilkårresultat).anySatisfy {
            assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
            assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(it.begrunnelse).isEqualTo("")
            assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
            assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
            assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
        }
        assertThat(vilkårresultat).anySatisfy {
            assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
            assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(it.begrunnelse).isEqualTo("Fylt ut automatisk fra registerdata i PDL\n")
            assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
            assertThat(it.periodeTom).isNull()
            assertThat(it.utdypendeVilkårsvurderinger).containsOnly(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
        }
    }

    @Test
    fun `skal generere bosatt i riket vilkår for person som har delt bosted i Finnmark eller Nord-Troms`() {
        // Arrange
        val behandling = lagBehandling()

        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

        val personResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
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
        vilkårsvurdering.personResultater.plus(personResultat)

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

        // Act
        val vilkårresultat =
            tilpassFinnmarkOgSvalbardPåBosattIRiketService.tilpassFinnmarkOgSvalbardtilleggPåBosattIRiketVilkårResultat(
                personResultat = personResultat,
                adresserForPerson = adresser,
                behandling = behandling,
                cutOffFomDato = LocalDate.of(2025, 9, 1),
            )

        // Assert
        assertThat(vilkårresultat).hasSize(2)
        assertThat(vilkårresultat).anySatisfy {
            assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
            assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(it.begrunnelse).isEqualTo("")
            assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
            assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
            assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
        }
        assertThat(vilkårresultat).anySatisfy {
            assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
            assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(it.begrunnelse).isEqualTo("Fylt ut automatisk fra registerdata i PDL\n")
            assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
            assertThat(it.periodeTom).isNull()
            assertThat(it.utdypendeVilkårsvurderinger).containsOnly(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
        }
    }

    @Test
    fun `skal generere bosatt i riket vilkår og fjerne utdypende vilkårsvurdering Svalbard om man har flyttet bort`() {
        // Arrange
        val behandling = lagBehandling()

        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

        val personResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                lagVilkårResultater = {
                    setOf(
                        lagVilkårResultat(
                            personResultat = it,
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2021, 1, 1),
                            periodeTom = null,
                            behandlingId = behandling.id,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD),
                        ),
                    )
                },
            )
        vilkårsvurdering.personResultater.plus(personResultat)

        val adresser =
            lagAdresser(
                delteBosteder =
                    listOf(
                        lagAdresse(
                            gyldigFraOgMed = LocalDate.of(2021, 1, 1),
                            gyldigTilOgMed = null,
                            matrikkeladresse = lagMatrikkeladresse(kommunenummer = "001"),
                        ),
                    ),
            )

        // Act
        val vilkårresultat =
            tilpassFinnmarkOgSvalbardPåBosattIRiketService.tilpassFinnmarkOgSvalbardtilleggPåBosattIRiketVilkårResultat(
                personResultat = personResultat,
                adresserForPerson = adresser,
                behandling = behandling,
                cutOffFomDato = LocalDate.of(2025, 9, 1),
            )

        // Assert
        assertThat(vilkårresultat).hasSize(2)
        assertThat(vilkårresultat).anySatisfy {
            assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
            assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(it.begrunnelse).isEqualTo("")
            assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
            assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
            assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
        }
        assertThat(vilkårresultat).anySatisfy {
            assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
            assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(it.begrunnelse).isEqualTo("Fylt ut automatisk fra registerdata i PDL\n")
            assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
            assertThat(it.periodeTom).isNull()
            assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
        }
    }

    @Test
    fun `skal generere bosatt i riket vilkår og fjerne utdypende vilkårsvurdering Finnmark eller Nord-Troms om man har flyttet bort`() {
        // Arrange
        val behandling = lagBehandling()

        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

        val personResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                lagVilkårResultater = {
                    setOf(
                        lagVilkårResultat(
                            personResultat = it,
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2021, 1, 1),
                            periodeTom = null,
                            behandlingId = behandling.id,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS),
                        ),
                    )
                },
            )
        vilkårsvurdering.personResultater.plus(personResultat)

        val adresser =
            lagAdresser(
                delteBosteder =
                    listOf(
                        lagAdresse(
                            gyldigFraOgMed = LocalDate.of(2021, 1, 1),
                            gyldigTilOgMed = null,
                            matrikkeladresse = lagMatrikkeladresse(kommunenummer = "001"),
                        ),
                    ),
            )

        // Act
        val vilkårresultat =
            tilpassFinnmarkOgSvalbardPåBosattIRiketService.tilpassFinnmarkOgSvalbardtilleggPåBosattIRiketVilkårResultat(
                personResultat = personResultat,
                adresserForPerson = adresser,
                behandling = behandling,
                cutOffFomDato = LocalDate.of(2025, 9, 1),
            )

        // Assert
        assertThat(vilkårresultat).hasSize(2)
        assertThat(vilkårresultat).anySatisfy {
            assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
            assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(it.begrunnelse).isEqualTo("")
            assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
            assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
            assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
        }
        assertThat(vilkårresultat).anySatisfy {
            assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
            assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(it.begrunnelse).isEqualTo("Fylt ut automatisk fra registerdata i PDL\n")
            assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
            assertThat(it.periodeTom).isNull()
            assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
        }
    }

    @Test
    fun `skal generere bosatt i riket vilkår for person som hverken bor på Svalbard eller i Finnmark eller Nord-Troms`() {
        // Arrange
        val behandling = lagBehandling()

        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

        val personResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
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
        vilkårsvurdering.personResultater.plus(personResultat)

        val adresser =
            lagAdresser(
                delteBosteder =
                    listOf(
                        lagAdresse(
                            gyldigFraOgMed = LocalDate.of(2021, 1, 1),
                            gyldigTilOgMed = null,
                            matrikkeladresse = lagMatrikkeladresse(kommunenummer = "0001"),
                        ),
                    ),
            )

        // Act
        val vilkårresultat =
            tilpassFinnmarkOgSvalbardPåBosattIRiketService.tilpassFinnmarkOgSvalbardtilleggPåBosattIRiketVilkårResultat(
                personResultat = personResultat,
                adresserForPerson = adresser,
                behandling = behandling,
                cutOffFomDato = LocalDate.of(2025, 9, 1),
            )

        // Assert
        assertThat(vilkårresultat).hasSize(2)
        assertThat(vilkårresultat).anySatisfy {
            assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
            assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(it.begrunnelse).isEqualTo("")
            assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
            assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
            assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
        }
        assertThat(vilkårresultat).anySatisfy {
            assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
            assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(it.begrunnelse).isEqualTo("Fylt ut automatisk fra registerdata i PDL\n")
            assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
            assertThat(it.periodeTom).isNull()
            assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
        }
    }
}
