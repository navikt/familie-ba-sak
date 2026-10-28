package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagKompetanse
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.ekstern.restDomene.VilkårResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilPersonResultatDto
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.EndringIPreutfyltVilkårLoggRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.ResultatBegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.time.YearMonth

class VilkårServiceEnhetTest {
    private val behandlingService = mockk<BehandlingService>()
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val personidentService = mockk<PersonidentService>()
    private val endringIPreutfyltVilkårLoggRepository = mockk<EndringIPreutfyltVilkårLoggRepository>()
    private val tilbakestillBehandlingService = mockk<TilbakestillBehandlingService>()
    private val kompetanseService = mockk<KompetanseService>()

    private val vilkårService =
        VilkårService(
            behandlingHentOgPersisterService = mockk(),
            behandlingstemaService = mockk(),
            behandlingService = behandlingService,
            vilkårsvurderingService = vilkårsvurderingService,
            personidentService = personidentService,
            persongrunnlagService = mockk(),
            endringIPreutfyltVilkårLoggRepository = endringIPreutfyltVilkårLoggRepository,
            tilbakestillBehandlingService = tilbakestillBehandlingService,
            kompetanseService = kompetanseService,
        )

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `skal lagre endring av kun begrunnelse uten å tilbakestille behandling`(erPreutfylt: Boolean) {
        // Arrange
        val vilkårsvurdering = opprettVilkårsvurdering(erPreutfylt)
        val personResultatDto = vilkårsvurdering.personResultater.single().tilPersonResultatDto()
        val vilkårResultatDto = personResultatDto.vilkårResultater.single().copy(begrunnelse = "Ny begrunnelse")

        // Act
        val personResultater =
            vilkårService.endreVilkår(
                behandlingId = vilkårsvurdering.behandling.id,
                vilkårId = vilkårResultatDto.id,
                personResultatDto = personResultatDto.copy(vilkårResultater = listOf(vilkårResultatDto)),
            )

        // Assert
        assertThat(
            personResultater
                .single()
                .vilkårResultater
                .single()
                .begrunnelse,
        ).isEqualTo("Ny begrunnelse")
        verify(exactly = 1) { vilkårsvurderingService.oppdater(vilkårsvurdering) }
        verify(exactly = 0) { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `skal ikke tilbakestille behandling når vilkåret er uendret`(erPreutfylt: Boolean) {
        // Arrange
        val vilkårsvurdering = opprettVilkårsvurdering(erPreutfylt)
        val personResultatDto = vilkårsvurdering.personResultater.single().tilPersonResultatDto()

        // Act
        vilkårService.endreVilkår(
            behandlingId = vilkårsvurdering.behandling.id,
            vilkårId = personResultatDto.vilkårResultater.single().id,
            personResultatDto = personResultatDto,
        )

        // Assert
        verify(exactly = 0) { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(any()) }
    }

    @ParameterizedTest(name = "preutfylt={0}, endring={1}")
    @MethodSource("endringerSomPåvirkerResultat")
    fun `skal tilbakestille behandling etter lagring når endringen påvirker resultatet`(
        erPreutfylt: Boolean,
        endring: (VilkårResultatDto) -> VilkårResultatDto,
    ) {
        // Arrange
        val vilkårsvurdering = opprettVilkårsvurdering(erPreutfylt)
        val personResultatDto = vilkårsvurdering.personResultater.single().tilPersonResultatDto()
        val vilkårResultatDto = endring(personResultatDto.vilkårResultater.single()).copy(begrunnelse = "Ny begrunnelse")

        // Act
        vilkårService.endreVilkår(
            behandlingId = vilkårsvurdering.behandling.id,
            vilkårId = vilkårResultatDto.id,
            personResultatDto = personResultatDto.copy(vilkårResultater = listOf(vilkårResultatDto)),
        )

        // Assert
        verify(exactly = 1) { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(vilkårsvurdering.behandling.id) }
        verifyOrder {
            vilkårsvurderingService.oppdater(vilkårsvurdering)
            tilbakestillBehandlingService.resettStegVedEndringPåVilkår(vilkårsvurdering.behandling.id)
        }
    }

    private fun opprettVilkårsvurdering(erPreutfylt: Boolean): Vilkårsvurdering {
        val behandling = lagBehandling()
        val aktør = behandling.fagsak.aktør
        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling) { vilkårsvurdering ->
                setOf(
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagVilkårResultat(
                                    personResultat = personResultat,
                                    behandlingId = behandling.id,
                                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                                    resultat = Resultat.OPPFYLT,
                                    periodeFom = LocalDate.of(2024, 1, 1),
                                    periodeTom = LocalDate.of(2024, 12, 31),
                                    begrunnelse = "Opprinnelig begrunnelse",
                                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                                    erPreutfylt = erPreutfylt,
                                ),
                            )
                        },
                    ),
                )
            }

        every { vilkårsvurderingService.hentAktivForBehandling(behandling.id) } returns vilkårsvurdering
        every { vilkårsvurderingService.oppdater(any()) } answers { firstArg() }
        every { personidentService.hentAktør(aktør.aktivFødselsnummer()) } returns aktør
        every { behandlingService.hentMigreringsdatoPåFagsak(behandling.fagsak.id) } returns null
        every { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id) } returns behandling
        every { endringIPreutfyltVilkårLoggRepository.findByVilkårResultatId(any()) } returns emptyList()
        every { endringIPreutfyltVilkårLoggRepository.save(any()) } answers { firstArg() }

        return vilkårsvurdering
    }

    @Nested
    inner class NullstillingAvKompetanse {
        private val behandling = lagBehandling()
        private val søker = behandling.fagsak.aktør
        private val barn = randomAktør()
        private val barnUtenVilkårsvurdering = randomAktør()
        private val nullstilteKompetanser = mutableListOf<Kompetanse>()

        @Test
        fun `skal nullstille kompetanse for barnet før behandlingen tilbakestilles når ny periode har annen utdypende vilkårsvurdering enn forrige periode`() {
            // Arrange
            val vilkårsvurdering =
                opprettVilkårsvurderingMedKompetanse(
                    barnetsBosattIRiket =
                        listOf(
                            oppfyltBosattIRiket(id = 11, fom = LocalDate.of(2024, 1, 1), tom = null, utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BARN_BOR_I_EØS),
                            ikkeVurdertBosattIRiket(id = 12),
                        ),
                )

            // Act
            endreVilkår(vilkårsvurdering, barn, vilkårId = 12) {
                it.copy(
                    periodeFom = LocalDate.of(2024, 8, 1),
                    resultat = Resultat.OPPFYLT,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE),
                )
            }

            // Assert
            assertThat(nullstilteKompetanser).containsExactly(
                Kompetanse.NULL.copy(fom = YearMonth.of(2024, 8), tom = null, barnAktører = setOf(barn)),
            )
            verifyOrder {
                kompetanseService.oppdaterKompetanse(any(), any())
                tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id)
            }
        }

        @Test
        fun `skal nullstille kompetanse for alle barn i kompetansene når søkers utdypende vilkårsvurdering endres`() {
            // Arrange
            val vilkårsvurdering =
                opprettVilkårsvurderingMedKompetanse(
                    søkersBosattIRiket =
                        listOf(
                            oppfyltBosattIRiket(id = 1, fom = LocalDate.of(2024, 1, 1), tom = LocalDate.of(2024, 12, 31), utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.OMFATTET_AV_NORSK_LOVGIVNING),
                        ),
                )

            // Act
            endreVilkår(vilkårsvurdering, søker, vilkårId = 1) {
                it.copy(utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.SØKER_OMFATTET_AV_UTENLANDSK_LOVGIVNING_BOSATT_I_NORGE))
            }

            // Assert
            val førsteMånedMedRett = YearMonth.of(2024, 2)
            assertThat(nullstilteKompetanser).containsExactly(
                Kompetanse.NULL.copy(fom = førsteMånedMedRett, tom = YearMonth.of(2024, 12), barnAktører = setOf(barn, barnUtenVilkårsvurdering)),
            )
        }

        @Test
        fun `skal kun nullstille kompetanse i månedene som får ny utdypende vilkårsvurdering når fom flyttes tidligere`() {
            // Arrange
            val vilkårsvurdering =
                opprettVilkårsvurderingMedKompetanse(
                    barnetsBosattIRiket =
                        listOf(
                            oppfyltBosattIRiket(id = 11, fom = LocalDate.of(2024, 1, 1), tom = LocalDate.of(2024, 6, 30), utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BARN_BOR_I_EØS),
                            oppfyltBosattIRiket(id = 12, fom = LocalDate.of(2024, 7, 1), tom = null, utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE),
                        ),
                )

            // Act
            endreVilkår(vilkårsvurdering, barn, vilkårId = 12) { it.copy(periodeFom = LocalDate.of(2024, 3, 1)) }

            // Assert
            assertThat(nullstilteKompetanser).containsExactly(
                Kompetanse.NULL.copy(fom = YearMonth.of(2024, 3), tom = YearMonth.of(2024, 6), barnAktører = setOf(barn)),
            )
        }

        @Test
        fun `skal nullstille kompetanse fra ny periode som starter etter forrige periodes tom`() {
            // Arrange
            val vilkårsvurdering =
                opprettVilkårsvurderingMedKompetanse(
                    barnetsBosattIRiket =
                        listOf(
                            oppfyltBosattIRiket(id = 11, fom = LocalDate.of(2024, 1, 1), tom = LocalDate.of(2024, 6, 30), utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BARN_BOR_I_EØS),
                            ikkeVurdertBosattIRiket(id = 12),
                        ),
                )

            // Act
            endreVilkår(vilkårsvurdering, barn, vilkårId = 12) {
                it.copy(
                    periodeFom = LocalDate.of(2024, 7, 1),
                    resultat = Resultat.OPPFYLT,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE),
                )
            }

            // Assert
            assertThat(nullstilteKompetanser).containsExactly(
                Kompetanse.NULL.copy(fom = YearMonth.of(2024, 7), tom = null, barnAktører = setOf(barn)),
            )
        }

        @Test
        fun `skal ikke nullstille kompetanse når perioden forkortes`() {
            // Arrange
            val vilkårsvurdering =
                opprettVilkårsvurderingMedKompetanse(
                    barnetsBosattIRiket =
                        listOf(
                            oppfyltBosattIRiket(id = 11, fom = LocalDate.of(2024, 1, 1), tom = null, utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BARN_BOR_I_EØS),
                        ),
                )

            // Act
            endreVilkår(vilkårsvurdering, barn, vilkårId = 11) { it.copy(periodeTom = LocalDate.of(2024, 6, 30)) }

            // Assert
            assertThat(nullstilteKompetanser).isEmpty()
            verify(exactly = 1) { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id) }
        }

        @ParameterizedTest
        @EnumSource(value = UtdypendeVilkårsvurdering::class, names = ["BOSATT_I_FINNMARK_NORD_TROMS", "BOSATT_PÅ_SVALBARD"])
        fun `skal ikke nullstille kompetanse når kun Finnmark- eller Svalbard-markering endres`(markering: UtdypendeVilkårsvurdering) {
            // Arrange
            val vilkårsvurdering =
                opprettVilkårsvurderingMedKompetanse(
                    barnetsBosattIRiket =
                        listOf(
                            oppfyltBosattIRiket(id = 11, fom = LocalDate.of(2024, 1, 1), tom = null, utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BARN_BOR_I_EØS),
                        ),
                )

            // Act
            endreVilkår(vilkårsvurdering, barn, vilkårId = 11) {
                it.copy(utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BARN_BOR_I_EØS, markering))
            }

            // Assert
            assertThat(nullstilteKompetanser).isEmpty()
            verify(exactly = 1) { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id) }
        }

        @Test
        fun `skal ikke nullstille kompetanse når en periode deles uten å endre utdypende vilkårsvurdering`() {
            // Arrange
            val vilkårsvurdering =
                opprettVilkårsvurderingMedKompetanse(
                    barnetsBosattIRiket =
                        listOf(
                            oppfyltBosattIRiket(id = 11, fom = LocalDate.of(2024, 1, 1), tom = null, utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.BARN_BOR_I_EØS),
                            ikkeVurdertBosattIRiket(id = 12),
                        ),
                )

            // Act
            endreVilkår(vilkårsvurdering, barn, vilkårId = 12) {
                it.copy(
                    periodeFom = LocalDate.of(2024, 8, 1),
                    resultat = Resultat.OPPFYLT,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BARN_BOR_I_EØS),
                )
            }

            // Assert
            assertThat(nullstilteKompetanser).isEmpty()
            verify(exactly = 1) { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id) }
        }

        private fun opprettVilkårsvurderingMedKompetanse(
            søkersBosattIRiket: List<VilkårResultat> = emptyList(),
            barnetsBosattIRiket: List<VilkårResultat> = emptyList(),
        ): Vilkårsvurdering {
            val vilkårsvurdering =
                lagVilkårsvurdering(behandling = behandling) { vilkårsvurdering ->
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = vilkårsvurdering,
                            aktør = søker,
                            lagVilkårResultater = { personResultat -> søkersBosattIRiket.onEach { it.personResultat = personResultat }.toSet() },
                        ),
                        lagPersonResultat(
                            vilkårsvurdering = vilkårsvurdering,
                            aktør = barn,
                            lagVilkårResultater = { personResultat ->
                                val barnetsUnder18ÅrVilkår = lagVilkårResultat(vilkårType = Vilkår.UNDER_18_ÅR)
                                (barnetsBosattIRiket + barnetsUnder18ÅrVilkår)
                                    .onEach { it.personResultat = personResultat }
                                    .toSet()
                            },
                        ),
                    )
                }

            every { vilkårsvurderingService.hentAktivForBehandling(behandling.id) } returns vilkårsvurdering
            every { vilkårsvurderingService.oppdater(any()) } answers { firstArg() }
            every { personidentService.hentAktør(søker.aktivFødselsnummer()) } returns søker
            every { personidentService.hentAktør(barn.aktivFødselsnummer()) } returns barn
            every { behandlingService.hentMigreringsdatoPåFagsak(behandling.fagsak.id) } returns null
            every { tilbakestillBehandlingService.resettStegVedEndringPåVilkår(behandling.id) } returns behandling
            every { kompetanseService.hentKompetanser(BehandlingId(behandling.id)) } returns
                listOf(lagKompetanse(behandlingId = behandling.id, barnAktører = setOf(barn, barnUtenVilkårsvurdering)))
            every { kompetanseService.oppdaterKompetanse(BehandlingId(behandling.id), capture(nullstilteKompetanser)) } just Runs

            return vilkårsvurdering
        }

        private fun oppfyltBosattIRiket(
            id: Long,
            fom: LocalDate,
            tom: LocalDate?,
            utdypendeVilkårsvurdering: UtdypendeVilkårsvurdering,
        ) = lagVilkårResultat(
            id = id,
            behandlingId = behandling.id,
            vilkårType = Vilkår.BOSATT_I_RIKET,
            resultat = Resultat.OPPFYLT,
            periodeFom = fom,
            periodeTom = tom,
            utdypendeVilkårsvurderinger = listOf(utdypendeVilkårsvurdering),
        )

        private fun ikkeVurdertBosattIRiket(id: Long) =
            lagVilkårResultat(
                id = id,
                behandlingId = behandling.id,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.IKKE_VURDERT,
                periodeFom = null,
                periodeTom = null,
            )

        private fun endreVilkår(
            vilkårsvurdering: Vilkårsvurdering,
            aktør: Aktør,
            vilkårId: Long,
            endring: (VilkårResultatDto) -> VilkårResultatDto,
        ) {
            val personResultatDto = vilkårsvurdering.personResultater.single { it.aktør == aktør }.tilPersonResultatDto()
            val vilkårResultatDto = endring(personResultatDto.vilkårResultater.single { it.id == vilkårId })

            vilkårService.endreVilkår(
                behandlingId = behandling.id,
                vilkårId = vilkårId,
                personResultatDto = personResultatDto.copy(vilkårResultater = listOf(vilkårResultatDto)),
            )
        }
    }

    companion object {
        @JvmStatic
        fun endringerSomPåvirkerResultat(): List<Arguments> {
            val endringer: Map<String, (VilkårResultatDto) -> VilkårResultatDto> =
                mapOf(
                    "periodeFom" to { it.copy(periodeFom = LocalDate.of(2024, 2, 1)) },
                    "periodeTom" to { it.copy(periodeTom = null) },
                    "resultat" to { it.copy(resultat = Resultat.IKKE_OPPFYLT) },
                    "resultatBegrunnelse" to { it.copy(resultatBegrunnelse = ResultatBegrunnelse.IKKE_AKTUELT) },
                    "erEksplisittAvslagPåSøknad" to { it.copy(erEksplisittAvslagPåSøknad = true) },
                    "vurderesEtter" to { it.copy(vurderesEtter = Regelverk.NASJONALE_REGLER) },
                    "utdypendeVilkårsvurderinger" to { it.copy(utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP)) },
                    "avslagBegrunnelser" to { it.copy(avslagBegrunnelser = listOf(Standardbegrunnelse.AVSLAG_LOVLIG_OPPHOLD_EØS_BORGER)) },
                )

            return listOf(false, true).flatMap { erPreutfylt ->
                endringer.map { (navn, endring) -> Arguments.of(erPreutfylt, named(navn, endring)) }
            }
        }
    }
}
