package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurderingMedOverstyrendeResultater
import no.nav.familie.ba.sak.kjerne.autovedtak.OppdaterUtdypendeVilkårForBosattIRiketMedFinnmarkOgSvalbardService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingMetrics
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.reflect.full.declaredMemberProperties

class VilkårsvurderingForNyBehandlingServiceTest {
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val behandlingService = mockk<BehandlingService>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val behandlingstemaService = mockk<BehandlingstemaService>()
    private val endretUtbetalingAndelService = mockk<EndretUtbetalingAndelService>()
    private val vilkårsvurderingMetrics = mockk<VilkårsvurderingMetrics>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val preutfyllVilkårService = mockk<PreutfyllVilkårService>()
    private val oppdaterUtdypendeVilkårForBosattIRiketMedFinnmarkOgSvalbardService = mockk<OppdaterUtdypendeVilkårForBosattIRiketMedFinnmarkOgSvalbardService>()

    private val vilkårsvurderingForNyBehandlingService =
        VilkårsvurderingForNyBehandlingService(
            vilkårsvurderingService = vilkårsvurderingService,
            behandlingService = behandlingService,
            persongrunnlagService = persongrunnlagService,
            behandlingstemaService = behandlingstemaService,
            endretUtbetalingAndelService = endretUtbetalingAndelService,
            vilkårsvurderingMetrics = vilkårsvurderingMetrics,
            andelerTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            preutfyllVilkårService = preutfyllVilkårService,
            oppdaterUtdypendeVilkårForBosattIRiketMedFinnmarkOgSvalbardService = oppdaterUtdypendeVilkårForBosattIRiketMedFinnmarkOgSvalbardService,
        )

    @BeforeEach()
    fun setup() {
        every { featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_PERSONOPPLYSNIGSGRUNNLAG) } returns true
    }

    @Nested
    inner class OpprettVilkårsvurderingUtenomHovedflyt {
        @Nested
        inner class Satsendring {
            @Test
            fun `skal kopiere vilkårsvurdering fra forrige behandling ved satsendring - alle vilkår for alle personer er oppfylt`() {
                // Arrange
                val søker = lagPerson(type = PersonType.SØKER)
                val barn = lagPerson(type = PersonType.BARN)
                val fagsak = Fagsak(aktør = søker.aktør)
                val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SATSENDRING)
                val forrigeBehandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SØKNAD)

                val forrigeVilkårsvurdering =
                    lagVilkårsvurderingMedOverstyrendeResultater(
                        søker = søker,
                        barna = listOf(barn),
                        behandling = forrigeBehandling,
                        overstyrendeVilkårResultater = emptyMap(),
                        id = 1,
                    )
                val forventetNåværendeVilkårsvurdering =
                    lagVilkårsvurderingMedOverstyrendeResultater(
                        søker = søker,
                        barna = listOf(barn),
                        behandling = behandling,
                        overstyrendeVilkårResultater = emptyMap(),
                    )

                every { vilkårsvurderingService.hentAktivForBehandling(behandlingId = forrigeBehandling.id) } returns forrigeVilkårsvurdering

                val slot = slot<Vilkårsvurdering>()

                every { vilkårsvurderingService.lagreNyOgDeaktiverGammel(capture(slot)) } returnsArgument 0

                every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
                    lagTestPersonopplysningGrunnlag(
                        forrigeBehandling.id,
                        søker,
                        barn,
                    )

                every {
                    endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
                        behandling,
                        forrigeBehandling,
                    )
                } just runs

                // Act
                vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
                    behandling = behandling,
                    forrigeBehandlingSomErVedtatt = forrigeBehandling,
                )

                // Assert
                verify(exactly = 1) { vilkårsvurderingService.lagreNyOgDeaktiverGammel(any()) }

                validerKopiertVilkårsvurdering(slot.captured, forrigeVilkårsvurdering, forventetNåværendeVilkårsvurdering)
            }

            @Test
            fun `skal kopiere vilkårsvurdering fra forrige behandling ved satsendring - alle VilkårResultater er ikke oppfylt`() {
                // Arrange
                val søker = lagPerson(type = PersonType.SØKER)
                val barn = lagPerson(type = PersonType.BARN)
                val fagsak = Fagsak(aktør = søker.aktør)
                val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SATSENDRING)
                val forrigeBehandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SØKNAD)

                val forrigeVilkårsvurdering =
                    lagVilkårsvurderingMedOverstyrendeResultater(
                        søker = søker,
                        barna = listOf(barn),
                        behandling = forrigeBehandling,
                        overstyrendeVilkårResultater =
                            mapOf(
                                Pair(
                                    barn.aktør.aktørId,
                                    listOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.OPPFYLT,
                                            periodeTom = LocalDate.now().minusMonths(4),
                                            behandlingId = forrigeBehandling.id,
                                        ),
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.IKKE_OPPFYLT,
                                            periodeFom = null,
                                            periodeTom = null,
                                            behandlingId = forrigeBehandling.id,
                                        ),
                                    ),
                                ),
                            ),
                        id = 1,
                    )
                val forventetNåværendeVilkårsvurdering =
                    lagVilkårsvurderingMedOverstyrendeResultater(
                        søker = søker,
                        barna = listOf(barn),
                        behandling = behandling,
                        overstyrendeVilkårResultater =
                            mapOf(
                                Pair(
                                    barn.aktør.aktørId,
                                    listOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.OPPFYLT,
                                            periodeTom = LocalDate.now().minusMonths(4),
                                            behandlingId = behandling.id,
                                        ),
                                    ),
                                ),
                            ),
                    )

                every { vilkårsvurderingService.hentAktivForBehandling(behandlingId = forrigeBehandling.id) } returns forrigeVilkårsvurdering

                val slot = slot<Vilkårsvurdering>()

                every { vilkårsvurderingService.lagreNyOgDeaktiverGammel(capture(slot)) } returnsArgument 0

                every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
                    lagTestPersonopplysningGrunnlag(
                        forrigeBehandling.id,
                        søker,
                        barn,
                    )

                every {
                    endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
                        behandling,
                        forrigeBehandling,
                    )
                } just runs

                // Act
                vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
                    behandling = behandling,
                    forrigeBehandlingSomErVedtatt = forrigeBehandling,
                )

                // Assert
                verify(exactly = 1) { vilkårsvurderingService.lagreNyOgDeaktiverGammel(any()) }

                validerKopiertVilkårsvurdering(slot.captured, forrigeVilkårsvurdering, forventetNåværendeVilkårsvurdering)
            }

            @Test
            fun `skal kopiere vilkårsvurdering fra forrige behandling ved satsendring - ett barn har ikke oppfylt alle vilkår og har ingen tilkjent ytelse fra forrige behandling`() {
                // Arrange
                val søker = lagPerson(type = PersonType.SØKER)
                val barn1 = lagPerson(type = PersonType.BARN)
                val barn2 = lagPerson(type = PersonType.BARN)
                val barna = listOf(barn1, barn2)
                val fagsak = Fagsak(aktør = søker.aktør)
                val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SATSENDRING)
                val forrigeBehandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SØKNAD)

                val forrigeVilkårsvurdering =
                    lagVilkårsvurderingMedOverstyrendeResultater(
                        søker = søker,
                        barna = barna,
                        behandling = forrigeBehandling,
                        overstyrendeVilkårResultater =
                            mapOf(
                                Pair(
                                    barn1.aktør.aktørId,
                                    listOf(
                                        lagVilkårResultat(
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.IKKE_OPPFYLT,
                                            periodeFom = null,
                                            periodeTom = null,
                                            behandlingId = forrigeBehandling.id,
                                        ),
                                    ),
                                ),
                            ),
                        id = 1,
                    )
                val forventetNåværendeVilkårsvurdering =
                    lagVilkårsvurderingMedOverstyrendeResultater(
                        søker = søker,
                        barna = listOf(barn2),
                        behandling = behandling,
                        overstyrendeVilkårResultater = emptyMap(),
                    )

                every { vilkårsvurderingService.hentAktivForBehandling(behandlingId = forrigeBehandling.id) } returns forrigeVilkårsvurdering

                val slot = slot<Vilkårsvurdering>()

                every { vilkårsvurderingService.lagreNyOgDeaktiverGammel(capture(slot)) } returnsArgument 0

                every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
                    lagTestPersonopplysningGrunnlag(
                        forrigeBehandling.id,
                        søker,
                        barn2,
                    )

                every {
                    endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
                        behandling,
                        forrigeBehandling,
                    )
                } just runs

                // Act
                vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
                    behandling = behandling,
                    forrigeBehandlingSomErVedtatt = forrigeBehandling,
                )

                // Assert
                verify(exactly = 1) { vilkårsvurderingService.lagreNyOgDeaktiverGammel(any()) }

                validerKopiertVilkårsvurdering(slot.captured, forrigeVilkårsvurdering, forventetNåværendeVilkårsvurdering)
            }
        }

        @Nested
        inner class Finnmarkstillegg {
            @Test
            fun `skal kaste exception om ingen forrige vedtatt behandling blir funnet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
                            behandling = behandling,
                            forrigeBehandlingSomErVedtatt = null,
                        )
                    }
                assertThat(exception.message).isEqualTo(
                    "Kan ikke opprette behandling med årsak ${behandling.opprettetÅrsak} hvis det ikke finnes en tidligere behandling",
                )
            }

            @Test
            fun `skal kaste exception om vilkårsvurdering ikke finnes for forrige behandling`() {
                // Arrange
                val person = lagPerson()
                val fagsak = lagFagsak(aktør = person.aktør)
                val behandling = lagBehandling(id = 1L, fagsak = fagsak, årsak = BehandlingÅrsak.FINNMARKSTILLEGG)
                val forrigeBehandling = lagBehandling(id = 2L, fagsak = fagsak, årsak = BehandlingÅrsak.FINNMARKSTILLEGG)

                val personopplysningGrunnlag =
                    lagPersonopplysningGrunnlag(
                        behandlingId = behandling.id,
                        lagPersoner = { setOf(person) },
                    )

                every { persongrunnlagService.hentAktivThrows(behandling.id) } returns personopplysningGrunnlag
                every { vilkårsvurderingService.hentAktivForBehandling(forrigeBehandling.id) } returns null

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
                            behandling = behandling,
                            forrigeBehandlingSomErVedtatt = forrigeBehandling,
                        )
                    }
                assertThat(exception.message).isEqualTo("Fant ikke aktiv vilkårsvurdering for behandling ${forrigeBehandling.id}")
            }

            @Test
            fun `skal lage ny vilkårsvurdering for finnmarkstillegg`() {
                // Arrange
                val person = lagPerson()
                val fagsak = lagFagsak(aktør = person.aktør)
                val behandling = lagBehandling(id = 1L, fagsak = fagsak, årsak = BehandlingÅrsak.FINNMARKSTILLEGG)
                val forrigeBehandling = lagBehandling(id = 2L, fagsak = fagsak, årsak = BehandlingÅrsak.FINNMARKSTILLEGG)

                val personopplysningGrunnlag =
                    lagPersonopplysningGrunnlag(
                        behandlingId = behandling.id,
                        lagPersoner = { setOf(person) },
                    )

                val forrigeVilkårsvurdering =
                    lagVilkårsvurdering(
                        behandling = forrigeBehandling,
                        lagPersonResultater = { emptySet() },
                    )

                val forrigePersonResultat =
                    lagPersonResultat(
                        vilkårsvurdering = forrigeVilkårsvurdering,
                        aktør = forrigeBehandling.fagsak.aktør,
                    )

                val forrigeVilkårResultat =
                    lagVilkårResultat(
                        personResultat = forrigePersonResultat,
                        behandlingId = forrigeBehandling.id,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        periodeFom = LocalDate.of(1980, 1, 1),
                        periodeTom = null,
                    )

                forrigeVilkårsvurdering.personResultater = setOf(forrigePersonResultat)
                forrigePersonResultat.vilkårResultater.add(forrigeVilkårResultat)

                val vilkårsvurderingSlot = slot<Vilkårsvurdering>()

                every { persongrunnlagService.hentAktivThrows(behandling.id) } returns personopplysningGrunnlag
                every { vilkårsvurderingService.hentAktivForBehandling(forrigeBehandling.id) } returns forrigeVilkårsvurdering
                every {
                    oppdaterUtdypendeVilkårForBosattIRiketMedFinnmarkOgSvalbardService.oppdaterUtdypendeVilkårForBosattIRiketMedFinnmarkOgSvalbard(
                        vilkårsvurdering = any<Vilkårsvurdering>(),
                    )
                } just runs
                every { endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(behandling, forrigeBehandling) } just runs
                every { vilkårsvurderingService.lagreNyOgDeaktiverGammel(capture(vilkårsvurderingSlot)) } returnsArgument 0

                // Act
                vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
                    behandling = behandling,
                    forrigeBehandlingSomErVedtatt = forrigeBehandling,
                )

                // Assert
                val nyVilkårsvurdering = vilkårsvurderingSlot.captured
                assertThat(nyVilkårsvurdering.id).isNotNull()
                assertThat(nyVilkårsvurdering.behandling).isEqualTo(behandling)
                assertThat(nyVilkårsvurdering.aktiv).isTrue()
                assertThat(nyVilkårsvurdering.opprettetAv).isEqualTo("VL")
                assertThat(nyVilkårsvurdering.endretAv).isEqualTo("VL")
                assertThat(nyVilkårsvurdering.endretTidspunkt).isNotNull()
                assertThat(nyVilkårsvurdering.personResultater).hasSize(forrigeVilkårsvurdering.personResultater.size)
                assertThat(nyVilkårsvurdering.personResultater).anySatisfy {
                    assertThat(it.id).isNotNull()
                    assertThat(it.aktør).isEqualTo(fagsak.aktør)
                    assertThat(it.vilkårResultater).hasSize(forrigePersonResultat.vilkårResultater.size)
                    assertThat(it.vilkårResultater).anySatisfy {
                        assertThat(it.id).isNotNull()
                        assertThat(it.personResultat?.aktør).isEqualTo(fagsak.aktør)
                        assertThat(it.personResultat?.andreVurderinger).isEmpty()
                        assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                        assertThat(it.periodeFom).isEqualTo(LocalDate.of(1980, 1, 1))
                        assertThat(it.periodeTom).isNull()
                    }
                }
            }
        }

        @Nested
        inner class Svalbardtillegg {
            @Test
            fun `skal kaste exception om ingen forrige vedtatt behandling blir funnet`() {
                // Arrange
                val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG)

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
                            behandling = behandling,
                            forrigeBehandlingSomErVedtatt = null,
                        )
                    }
                assertThat(exception.message).isEqualTo(
                    "Kan ikke opprette behandling med årsak ${behandling.opprettetÅrsak} hvis det ikke finnes en tidligere behandling",
                )
            }

            @Test
            fun `skal kaste exception om vilkårsvurdering ikke finnes for forrige behandling`() {
                // Arrange
                val person = lagPerson()
                val fagsak = lagFagsak(aktør = person.aktør)
                val behandling = lagBehandling(id = 1L, fagsak = fagsak, årsak = BehandlingÅrsak.SVALBARDTILLEGG)
                val forrigeBehandling = lagBehandling(id = 2L, fagsak = fagsak, årsak = BehandlingÅrsak.SVALBARDTILLEGG)

                val personopplysningGrunnlag =
                    lagPersonopplysningGrunnlag(
                        behandlingId = behandling.id,
                        lagPersoner = { setOf(person) },
                    )

                every { persongrunnlagService.hentAktivThrows(behandling.id) } returns personopplysningGrunnlag
                every { vilkårsvurderingService.hentAktivForBehandling(forrigeBehandling.id) } returns null

                // Act & assert
                val exception =
                    assertThrows<Feil> {
                        vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
                            behandling = behandling,
                            forrigeBehandlingSomErVedtatt = forrigeBehandling,
                        )
                    }
                assertThat(exception.message).isEqualTo("Fant ikke aktiv vilkårsvurdering for behandling ${forrigeBehandling.id}")
            }

            @Test
            fun `skal lage ny vilkårsvurdering for svalbardtillegg`() {
                // Arrange
                val person = lagPerson()
                val fagsak = lagFagsak(aktør = person.aktør)
                val behandling = lagBehandling(id = 1L, fagsak = fagsak, årsak = BehandlingÅrsak.SVALBARDTILLEGG)
                val forrigeBehandling = lagBehandling(id = 2L, fagsak = fagsak, årsak = BehandlingÅrsak.SVALBARDTILLEGG)

                val personopplysningGrunnlag =
                    lagPersonopplysningGrunnlag(
                        behandlingId = behandling.id,
                        lagPersoner = { setOf(person) },
                    )

                val forrigeVilkårsvurdering =
                    lagVilkårsvurdering(
                        behandling = forrigeBehandling,
                        lagPersonResultater = { emptySet() },
                    )

                val forrigePersonResultat =
                    lagPersonResultat(
                        vilkårsvurdering = forrigeVilkårsvurdering,
                        aktør = forrigeBehandling.fagsak.aktør,
                    )

                val forrigeVilkårResultat =
                    lagVilkårResultat(
                        personResultat = forrigePersonResultat,
                        behandlingId = forrigeBehandling.id,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        periodeFom = LocalDate.of(1980, 1, 1),
                        periodeTom = null,
                    )

                forrigeVilkårsvurdering.personResultater = setOf(forrigePersonResultat)
                forrigePersonResultat.vilkårResultater.add(forrigeVilkårResultat)

                val vilkårsvurderingSlot = slot<Vilkårsvurdering>()

                every { persongrunnlagService.hentAktivThrows(behandling.id) } returns personopplysningGrunnlag
                every { vilkårsvurderingService.hentAktivForBehandling(forrigeBehandling.id) } returns forrigeVilkårsvurdering
                every {
                    oppdaterUtdypendeVilkårForBosattIRiketMedFinnmarkOgSvalbardService.oppdaterUtdypendeVilkårForBosattIRiketMedFinnmarkOgSvalbard(
                        vilkårsvurdering = any<Vilkårsvurdering>(),
                    )
                } just runs
                every { endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(behandling, forrigeBehandling) } just runs
                every { vilkårsvurderingService.lagreNyOgDeaktiverGammel(capture(vilkårsvurderingSlot)) } returnsArgument 0

                // Act
                vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
                    behandling = behandling,
                    forrigeBehandlingSomErVedtatt = forrigeBehandling,
                )

                // Assert
                val nyVilkårsvurdering = vilkårsvurderingSlot.captured
                assertThat(nyVilkårsvurdering.id).isNotNull()
                assertThat(nyVilkårsvurdering.behandling).isEqualTo(behandling)
                assertThat(nyVilkårsvurdering.aktiv).isTrue()
                assertThat(nyVilkårsvurdering.opprettetAv).isEqualTo("VL")
                assertThat(nyVilkårsvurdering.endretAv).isEqualTo("VL")
                assertThat(nyVilkårsvurdering.endretTidspunkt).isNotNull()
                assertThat(nyVilkårsvurdering.personResultater).hasSize(forrigeVilkårsvurdering.personResultater.size)
                assertThat(nyVilkårsvurdering.personResultater).anySatisfy {
                    assertThat(it.id).isNotNull()
                    assertThat(it.aktør).isEqualTo(fagsak.aktør)
                    assertThat(it.vilkårResultater).hasSize(forrigePersonResultat.vilkårResultater.size)
                    assertThat(it.vilkårResultater).anySatisfy {
                        assertThat(it.id).isNotNull()
                        assertThat(it.personResultat?.aktør).isEqualTo(fagsak.aktør)
                        assertThat(it.personResultat?.andreVurderinger).isEmpty()
                        assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                        assertThat(it.periodeFom).isEqualTo(LocalDate.of(1980, 1, 1))
                        assertThat(it.periodeTom).isNull()
                    }
                }
            }
        }
    }

    companion object {
        fun validerKopiertVilkårsvurdering(
            kopiertVilkårsvurdering: Vilkårsvurdering,
            forrigeVilkårsvurdering: Vilkårsvurdering,
            forventetNåværendeVilkårsvurdering: Vilkårsvurdering,
        ) {
            assertThat(kopiertVilkårsvurdering.id).isNotEqualTo(forrigeVilkårsvurdering.id)
            assertThat(kopiertVilkårsvurdering.behandling.id).isNotEqualTo(forrigeVilkårsvurdering.behandling.id)

            kopiertVilkårsvurdering.personResultater.forEach {
                assertThat(it.aktør).isEqualTo(forventetNåværendeVilkårsvurdering.personResultater.first { personResultat -> personResultat.aktør.aktivFødselsnummer() == it.aktør.aktivFødselsnummer() }.aktør)
            }

            assertThat(kopiertVilkårsvurdering.personResultater.flatMap { it.vilkårResultater }.size).isEqualTo(
                forventetNåværendeVilkårsvurdering.personResultater.flatMap { it.vilkårResultater }.size,
            )

            val kopierteOgForrigeVilkårResultaterGruppertEtterVilkårType =
                kopiertVilkårsvurdering.personResultater.fold(mutableListOf<Pair<List<VilkårResultat>, List<VilkårResultat>>>()) { acc, personResultat ->
                    val vilkårResultaterForrigeBehandlingForPerson =
                        forventetNåværendeVilkårsvurdering.personResultater
                            .filter { it.aktør.aktivFødselsnummer() == personResultat.aktør.aktivFødselsnummer() }
                            .flatMap { it.vilkårResultater }
                    acc.addAll(
                        personResultat.vilkårResultater
                            .groupBy { it.vilkårType }
                            .map { (vilkårType, vilkårResultater) ->
                                Pair(
                                    vilkårResultater,
                                    vilkårResultaterForrigeBehandlingForPerson.filter { forrigeVilkårResultat -> forrigeVilkårResultat.vilkårType == vilkårType },
                                )
                            },
                    )
                    acc
                }

            val baseEntitetFelter =
                BaseEntitet::class.declaredMemberProperties.map { it.name }.toTypedArray()
            kopierteOgForrigeVilkårResultaterGruppertEtterVilkårType.forEach {
                assertThat(it.first)
                    .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                        "id",
                        "personResultat",
                        *baseEntitetFelter,
                    ).isEqualTo(it.second)
            }
        }
    }
}
