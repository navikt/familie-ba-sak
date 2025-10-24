package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.EøsSkjemaerForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.PersonopplysningGrunnlagForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class RegistrerPersongrunnlagEnhetTest {
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val personopplysningGrunnlagForNyBehandlingService: PersonopplysningGrunnlagForNyBehandlingService = mockk()
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService = mockk()
    private val kompetanseService: KompetanseService = mockk()
    private val valutakursService: ValutakursService = mockk()
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService = mockk()
    private val vilkårService: VilkårService = mockk()

    private val registrerPersongrunnlagSteg =
        RegistrerPersongrunnlag(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vilkårsvurderingForNyBehandlingService = vilkårsvurderingForNyBehandlingService,
            personopplysningGrunnlagForNyBehandlingService = personopplysningGrunnlagForNyBehandlingService,
            eøsSkjemaerForNyBehandlingService =
                EøsSkjemaerForNyBehandlingService(
                    kompetanseService = kompetanseService,
                    utenlandskPeriodebeløpService = utenlandskPeriodebeløpService,
                    valutakursService = valutakursService,
                ),
            vilkårService = vilkårService,
        )

    @Test
    fun `Kopierer kompetanser, valutakurser og utenlandsk periodebeløp til ny behandling`() {
        val mor = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val behandling1 = lagBehandling()
        val behandling2 = lagBehandling()

        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling2) } returns behandling1

        every {
            personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
                behandling = behandling2,
                forrigeBehandlingSomErVedtatt = behandling1,
                søkerIdent = mor.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn1.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer()),
            )
        } just runs

        every {
            vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
                behandling = behandling2,
                forrigeBehandlingSomErVedtatt = behandling1,
            )
        } just runs

        every {
            kompetanseService.kopierOgErstattKompetanser(
                BehandlingId(behandling1.id),
                BehandlingId(behandling2.id),
            )
        } just runs
        every {
            valutakursService.kopierOgErstattValutakurser(
                BehandlingId(behandling1.id),
                BehandlingId(behandling2.id),
            )
        } just runs
        every {
            utenlandskPeriodebeløpService.kopierOgErstattUtenlandskPeriodebeløp(
                BehandlingId(behandling1.id),
                BehandlingId(behandling2.id),
            )
        } just runs

        registrerPersongrunnlagSteg.utførStegOgAngiNeste(
            behandling = behandling2,
            data =
                RegistrerPersongrunnlagDTO(
                    ident = mor.aktør.aktivFødselsnummer(),
                    barnasIdenter = listOf(barn1.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer()),
                ),
        )

        verify(exactly = 1) {
            kompetanseService.kopierOgErstattKompetanser(
                BehandlingId(behandling1.id),
                BehandlingId(behandling2.id),
            )
            valutakursService.kopierOgErstattValutakurser(
                BehandlingId(behandling1.id),
                BehandlingId(behandling2.id),
            )
            utenlandskPeriodebeløpService.kopierOgErstattUtenlandskPeriodebeløp(
                BehandlingId(behandling1.id),
                BehandlingId(behandling2.id),
            )
        }
    }

    @Nested
    inner class PostValiderSteg {
        @ParameterizedTest
        @EnumSource(BehandlingÅrsak::class, names = ["FINNMARKSTILLEGG", "SVALBARDTILLEGG"], mode = EnumSource.Mode.EXCLUDE)
        fun `skal ikke validere om behandlingen har en annen årsak enn FINNMARKSTILLEGG eller SVALBARDTILLEGG`(
            behandlingÅrsak: BehandlingÅrsak,
        ) {
            // Arrange
            val behandling = lagBehandling(årsak = behandlingÅrsak)

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null

            // Act & Assert
            assertDoesNotThrow { registrerPersongrunnlagSteg.postValiderSteg(behandling) }
        }

        @Test
        fun `Skal kaste feil i finnmarkstillegg-behandlinger om forrige vedtatte behandling ikke finnes`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null

            // Act & Assert
            assertThatThrownBy { registrerPersongrunnlagSteg.postValiderSteg(behandling) }.hasMessage(
                "Vi kan ikke kjøre behandling med årsak ${behandling.opprettetÅrsak} dersom det ikke finnes en tidligere behandling. Behandling: ${behandling.id}",
            )
        }

        @Test
        fun `Skal kaste feil i finnmarkstillegg-behandlinger hvis det ikke er endringer i 'Bosatt i riket'-vilkåret`() {
            // Arrange
            val person = lagPerson()

            val forrigeBehandling = lagBehandling()
            val forrigeVilkårsvurdering =
                lagVilkårsvurdering(behandling = forrigeBehandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = forrigeBehandling,
                            person = person,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 1, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS),
                        ),
                    )
                }

            val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)
            val vilkårsvurdering =
                lagVilkårsvurdering(behandling = behandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = behandling,
                            person = person,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 1, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS),
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
            every { vilkårService.hentVilkårsvurderingThrows(forrigeBehandling.id) } returns forrigeVilkårsvurdering
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vilkårsvurdering

            // Act & Assert
            assertThatThrownBy { registrerPersongrunnlagSteg.postValiderSteg(behandling) }
                .hasMessage("Ingen endring i 'Bosatt i riket'-vilkåret")
        }

        @Test
        fun `Skal ikke kaste feil i finnmarkstillegg-behandlinger hvis utdypende vilkårsvurdering i 'Bosatt i riket'-vilkåret er endret`() {
            // Arrange
            val person = lagPerson()

            val forrigeBehandling = lagBehandling()
            val forrigeVilkårsvurdering =
                lagVilkårsvurdering(behandling = forrigeBehandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = forrigeBehandling,
                            person = person,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 1, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS),
                        ),
                    )
                }

            val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)
            val vilkårsvurdering =
                lagVilkårsvurdering(behandling = behandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = behandling,
                            person = person,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 1, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(),
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
            every { vilkårService.hentVilkårsvurderingThrows(forrigeBehandling.id) } returns forrigeVilkårsvurdering
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vilkårsvurdering

            // Act & Assert
            assertThatCode { registrerPersongrunnlagSteg.postValiderSteg(behandling) }.doesNotThrowAnyException()
        }

        @Test
        fun `Skal ikke kaste feil i finnmarkstillegg-behandlinger hvis periode i 'Bosatt i riket'-vilkåret er endret`() {
            // Arrange
            val person = lagPerson()

            val forrigeBehandling = lagBehandling()
            val forrigeVilkårsvurdering =
                lagVilkårsvurdering(behandling = forrigeBehandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = forrigeBehandling,
                            person = person,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 1, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS),
                        ),
                    )
                }

            val behandling = lagBehandling(årsak = BehandlingÅrsak.FINNMARKSTILLEGG)
            val vilkårsvurdering =
                lagVilkårsvurdering(behandling = behandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = behandling,
                            person = person,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 2, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS),
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
            every { vilkårService.hentVilkårsvurderingThrows(forrigeBehandling.id) } returns forrigeVilkårsvurdering
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vilkårsvurdering

            // Act & Assert
            assertThatCode { registrerPersongrunnlagSteg.postValiderSteg(behandling) }.doesNotThrowAnyException()
        }

        @Test
        fun `Skal kaste feil i svalbardtillegg-behandlinger om forrige vedtatte behandling ikke finnes`() {
            // Arrange
            val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG)

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null

            // Act & Assert
            assertThatThrownBy { registrerPersongrunnlagSteg.postValiderSteg(behandling) }.hasMessage(
                "Vi kan ikke kjøre behandling med årsak ${behandling.opprettetÅrsak} dersom det ikke finnes en tidligere behandling. Behandling: ${behandling.id}",
            )
        }

        @Test
        fun `Skal kaste feil i svalbardtillegg-behandlinger hvis det ikke er endringer i 'Bosatt i riket'-vilkåret`() {
            // Arrange
            val person = lagPerson()

            val forrigeBehandling = lagBehandling()
            val forrigeVilkårsvurdering =
                lagVilkårsvurdering(behandling = forrigeBehandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = forrigeBehandling,
                            person = person,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 1, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD),
                        ),
                    )
                }

            val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG)
            val vilkårsvurdering =
                lagVilkårsvurdering(behandling = behandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = behandling,
                            person = person,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 1, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD),
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
            every { vilkårService.hentVilkårsvurderingThrows(forrigeBehandling.id) } returns forrigeVilkårsvurdering
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vilkårsvurdering

            // Act & Assert
            assertThatThrownBy { registrerPersongrunnlagSteg.postValiderSteg(behandling) }.hasMessage(
                "Ingen endring i 'Bosatt i riket'-vilkåret",
            )
        }

        @Test
        fun `Skal ikke kaste feil i svalbardtillegg-behandlinger hvis utdypende vilkårsvurdering i 'Bosatt i riket'-vilkåret er endret`() {
            // Arrange
            val person = lagPerson()

            val forrigeBehandling = lagBehandling()
            val forrigeVilkårsvurdering =
                lagVilkårsvurdering(behandling = forrigeBehandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = forrigeBehandling,
                            person = person,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 1, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD),
                        ),
                    )
                }

            val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG)
            val vilkårsvurdering =
                lagVilkårsvurdering(behandling = behandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = behandling,
                            person = person,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 1, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(),
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
            every { vilkårService.hentVilkårsvurderingThrows(forrigeBehandling.id) } returns forrigeVilkårsvurdering
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vilkårsvurdering

            // Act & Assert
            assertThatCode { registrerPersongrunnlagSteg.postValiderSteg(behandling) }.doesNotThrowAnyException()
        }

        @Test
        fun `Skal ikke kaste feil i svalbardtillegg-behandlinger hvis periode i 'Bosatt i riket'-vilkåret er endret`() {
            // Arrange
            val person = lagPerson()

            val forrigeBehandling = lagBehandling()
            val forrigeVilkårsvurdering =
                lagVilkårsvurdering(behandling = forrigeBehandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = forrigeBehandling,
                            person = person,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 1, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD),
                        ),
                    )
                }

            val behandling = lagBehandling(årsak = BehandlingÅrsak.SVALBARDTILLEGG)
            val vilkårsvurdering =
                lagVilkårsvurdering(behandling = behandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = behandling,
                            person = person,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 2, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD),
                        ),
                    )
                }

            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
            every { vilkårService.hentVilkårsvurderingThrows(forrigeBehandling.id) } returns forrigeVilkårsvurdering
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vilkårsvurdering

            // Act & Assert
            assertThatCode { registrerPersongrunnlagSteg.postValiderSteg(behandling) }.doesNotThrowAnyException()
        }
    }
}
