package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.ekstern.restDomene.NyttVilkårDto
import no.nav.familie.ba.sak.ekstern.restDomene.SlettVilkårDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilPersonResultatDto
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori.ORDINÆR
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori.UTVIDET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.TEKNISK_ENDRING
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.EndringIPreutfyltVilkårLoggRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UTVIDET_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VilkårServiceTest(
    @Autowired private val vilkårService: VilkårService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val endringIPreutfyltVilkårLoggRepository: EndringIPreutfyltVilkårLoggRepository,
) : AbstractSpringIntegrationTest() {
    private lateinit var behandling: Behandling
    private lateinit var søkerFnr: String
    private lateinit var barnFnr: String
    private lateinit var søkerAktør: Aktør
    private lateinit var barnAktør: Aktør

    @BeforeEach
    fun setUp() {
        søkerFnr = leggTilPersonInfo(LocalDate.now().minusYears(30))
        barnFnr = leggTilPersonInfo(LocalDate.now().minusYears(1))

        søkerAktør = personidentService.hentOgLagreAktør(søkerFnr, true)
        barnAktør = personidentService.hentOgLagreAktør(barnFnr, true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        behandling =
            behandlingService.lagreNyOgDeaktiverGammelBehandling(
                lagBehandlingUtenId(
                    fagsak = fagsak,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    behandlingKategori = BehandlingKategori.NASJONAL,
                    underkategori = ORDINÆR,
                    årsak = BehandlingÅrsak.SØKNAD,
                    status = BehandlingStatus.UTREDES,
                ),
            )

        persongrunnlagService.lagreOgDeaktiverGammel(
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                søkerAktør = søkerAktør,
                barnAktør = listOf(barnAktør),
            ),
        )
    }

    @Nested
    inner class HentVilkårsvurdering {
        @Test
        fun `skal returnere vilkårsvurdering når den finnes`() {
            // Arrange
            vilkårsvurderingService.lagreNyOgDeaktiverGammel(lagVilkårsvurdering(behandling = behandling))

            // Act
            val vilkårsvurdering = vilkårService.hentVilkårsvurdering(behandling.id)

            // Assert
            assertThat(vilkårsvurdering).isNotNull()
            assertThat(vilkårsvurdering!!.behandling.id).isEqualTo(behandling.id)
        }

        @Test
        fun `skal returnere null når vilkårsvurdering ikke finnes`() {
            // Act
            val vilkårsvurdering = vilkårService.hentVilkårsvurdering(behandling.id)

            // Assert
            assertThat(vilkårsvurdering).isNull()
        }
    }

    @Nested
    inner class HentVilkårsvurderingThrows {
        @Test
        fun `skal returnere vilkårsvurdering når den finnes`() {
            // Arrange
            vilkårsvurderingService.lagreNyOgDeaktiverGammel(lagVilkårsvurdering(behandling = behandling))

            // Act
            val vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandling.id)

            // Assert
            assertThat(vilkårsvurdering).isNotNull()
            assertThat(vilkårsvurdering.behandling.id).isEqualTo(behandling.id)
        }

        @Test
        fun `skal kaste Feil når vilkårsvurdering ikke finnes`() {
            // Arrange
            val behandlingIdUtenVilkårsvurdering = 1L

            // Act & Assert
            val feil = assertThrows<Feil> { vilkårService.hentVilkårsvurderingThrows(behandlingIdUtenVilkårsvurdering) }

            assertThat(feil.message).isEqualTo("Fant ikke aktiv vilkårsvurdering for behandling $behandlingIdUtenVilkårsvurdering")
            assertThat(feil.frontendFeilmelding).isEqualTo(VilkårService.FANT_IKKE_AKTIV_VILKÅRSVURDERING_FEILMELDING)
        }
    }

    @Nested
    inner class EndreVilkår {
        @Test
        fun `skal kaste Feil når vilkår med gitt id ikke finnes`() {
            // Arrange
            val vilkårsvurdering =
                vilkårsvurderingService.lagreNyOgDeaktiverGammel(
                    lagVilkårsvurdering(behandling = behandling) {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = søkerAktør,
                            ),
                        )
                    },
                )

            val restPersonResultat =
                vilkårsvurdering
                    .personResultater
                    .single()
                    .tilPersonResultatDto()

            val ikkeEksisterendeVilkårId = 1L

            // Act & Assert
            val feil =
                assertThrows<Feil> {
                    vilkårService.endreVilkår(
                        behandlingId = behandling.id,
                        vilkårId = ikkeEksisterendeVilkårId,
                        personResultatDto = restPersonResultat,
                    )
                }

            assertThat(feil.message).isEqualTo("Fant ikke vilkårResultat med id $ikkeEksisterendeVilkårId ved oppdatering av vilkår")
        }

        @Test
        fun `skal returnere uendret liste når vilkår ikke er endret`() {
            // Arrange
            val vilkårsvurdering =
                vilkårsvurderingService.lagreNyOgDeaktiverGammel(
                    lagVilkårsvurdering(behandling = behandling) { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søkerAktør,
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barnAktør,
                            ),
                        )
                    },
                )

            val restPersonResultater = vilkårsvurdering.personResultater.map { it.tilPersonResultatDto() }
            val restPersonResultatSøker = restPersonResultater.first { it.personIdent == søkerFnr }
            val restVilkårResultat = restPersonResultatSøker.vilkårResultater.first()

            // Act
            val restPersonResultaterMedEndringer =
                vilkårService
                    .endreVilkår(
                        behandlingId = behandling.id,
                        vilkårId = restVilkårResultat.id,
                        personResultatDto = restPersonResultatSøker,
                    )

            // Assert
            assertThat(restPersonResultaterMedEndringer)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .ignoringFieldsMatchingRegexes(".*opprettetTidspunkt", ".*endretTidspunkt")
                .isEqualTo(restPersonResultater)
        }

        @ParameterizedTest
        @ValueSource(strings = ["", PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT])
        fun `skal kaste Feil når preutfylt vilkår er endret og har tom eller automatisk utfylt begrunnelse`(
            begrunnelseFraFrontend: String,
        ) {
            // Arrange
            val vilkårsvurdering =
                vilkårsvurderingService.lagreNyOgDeaktiverGammel(
                    lagVilkårsvurdering(behandling = behandling) { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søkerAktør,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = it,
                                            behandlingId = behandling.id,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            erPreutfylt = true,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            val restPersonResultat = vilkårsvurdering.personResultater.single().tilPersonResultatDto()
            val restVilkårResultat = restPersonResultat.vilkårResultater.single()

            val restPersonResultatMedEndring =
                restPersonResultat.copy(
                    vilkårResultater =
                        listOf(
                            restVilkårResultat.copy(
                                periodeFom = LocalDate.of(2024, 1, 1),
                                begrunnelse = begrunnelseFraFrontend,
                            ),
                        ),
                )

            // Act
            val funksjonellFeil =
                assertThrows<FunksjonellFeil> {
                    vilkårService
                        .endreVilkår(
                            behandlingId = behandling.id,
                            vilkårId = restVilkårResultat.id,
                            personResultatDto = restPersonResultatMedEndring,
                        )
                }

            assertThat(funksjonellFeil.frontendFeilmelding).isEqualTo("Du har endret vilkåret, og må derfor fjerne den automatiske begrunnelsen og fylle inn en ny begrunnelse.")
        }

        @Test
        fun `skal tillate endring av kun begrunnelse`() {
            // Arrange
            val vilkårsvurdering =
                vilkårsvurderingService.lagreNyOgDeaktiverGammel(
                    lagVilkårsvurdering(behandling = behandling) { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søkerAktør,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = it,
                                            behandlingId = behandling.id,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            erPreutfylt = true,
                                            begrunnelse = PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            val restPersonResultat = vilkårsvurdering.personResultater.single().tilPersonResultatDto()
            val restVilkårResultat = restPersonResultat.vilkårResultater.single()

            val restPersonResultatMedEndring =
                restPersonResultat.copy(
                    vilkårResultater =
                        listOf(
                            restVilkårResultat.copy(
                                begrunnelse = restVilkårResultat.begrunnelse + "\nEkstra informasjon",
                            ),
                        ),
                )

            // Act
            vilkårService
                .endreVilkår(
                    behandlingId = behandling.id,
                    vilkårId = restVilkårResultat.id,
                    personResultatDto = restPersonResultatMedEndring,
                )

            // Assert
            val oppdatertVilkårResultat =
                vilkårService
                    .hentVilkårsvurderingThrows(behandling.id)
                    .personResultater
                    .single()
                    .vilkårResultater
                    .single()

            assertThat(oppdatertVilkårResultat.erAutomatiskVurdert).isFalse()
            assertThat(oppdatertVilkårResultat.erOpprinneligPreutfylt).isTrue()
            assertThat(oppdatertVilkårResultat.begrunnelse).isEqualTo(restVilkårResultat.begrunnelse + "\nEkstra informasjon")
        }

        @Test
        fun `skal oppdatere preutfylt vilkår når begrunnelse er endret`() {
            // Arrange
            val vilkårsvurdering =
                vilkårsvurderingService.lagreNyOgDeaktiverGammel(
                    lagVilkårsvurdering(behandling = behandling) { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søkerAktør,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = it,
                                            behandlingId = behandling.id,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            erPreutfylt = true,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            val restPersonResultat = vilkårsvurdering.personResultater.single().tilPersonResultatDto()
            val restVilkårResultat = restPersonResultat.vilkårResultater.single()

            val restPersonResultatMedEndring =
                restPersonResultat.copy(
                    vilkårResultater =
                        listOf(
                            restVilkårResultat
                                .copy(
                                    periodeFom = LocalDate.of(2024, 1, 1),
                                    begrunnelse = "Ny begrunnelse",
                                ),
                        ),
                )

            // Act
            vilkårService.endreVilkår(
                behandlingId = behandling.id,
                vilkårId = restVilkårResultat.id,
                personResultatDto = restPersonResultatMedEndring,
            )

            // Assert
            val oppdatertVilkårResultat =
                vilkårService
                    .hentVilkårsvurderingThrows(behandling.id)
                    .personResultater
                    .single()
                    .vilkårResultater
                    .single()

            assertThat(oppdatertVilkårResultat.periodeFom).isEqualTo(LocalDate.of(2024, 1, 1))
            assertThat(oppdatertVilkårResultat.erAutomatiskVurdert).isFalse()
            assertThat(oppdatertVilkårResultat.erOpprinneligPreutfylt).isTrue()
            assertThat(oppdatertVilkårResultat.begrunnelse).isEqualTo("Ny begrunnelse")
        }

        @Test
        fun `skal lagre logg når preutfylt vilkår endres`() {
            // Arrange
            val vilkårsvurdering =
                vilkårsvurderingService.lagreNyOgDeaktiverGammel(
                    lagVilkårsvurdering(behandling = behandling) { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søkerAktør,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = it,
                                            behandlingId = behandling.id,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            resultat = Resultat.IKKE_OPPFYLT,
                                            vurderesEtter = Regelverk.NASJONALE_REGLER,
                                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS),
                                            erPreutfylt = true,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            val restPersonResultat = vilkårsvurdering.personResultater.single().tilPersonResultatDto()
            val restVilkårResultat = restPersonResultat.vilkårResultater.single()

            val restPersonResultatMedEndring =
                restPersonResultat.copy(
                    vilkårResultater =
                        listOf(
                            restVilkårResultat
                                .copy(
                                    periodeFom = LocalDate.of(2024, 1, 1),
                                    begrunnelse = "Ny begrunnelse",
                                    resultat = Resultat.OPPFYLT,
                                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD),
                                ),
                        ),
                )

            // Act
            vilkårService.endreVilkår(
                behandlingId = behandling.id,
                vilkårId = restVilkårResultat.id,
                personResultatDto = restPersonResultatMedEndring,
            )

            // Assert
            val endringIPreutfyltVilkårLogger = endringIPreutfyltVilkårLoggRepository.findAll().filter { it.behandling.id == behandling.id }

            assertThat(endringIPreutfyltVilkårLogger).hasSize(1)
            with(endringIPreutfyltVilkårLogger.single()) {
                assertThat(nyResultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(forrigeResultat).isEqualTo(Resultat.IKKE_OPPFYLT)
                assertThat(forrigeVurderesEtter).isEqualTo(Regelverk.NASJONALE_REGLER)
                assertThat(nyVurderesEtter).isEqualTo(Regelverk.EØS_FORORDNINGEN)
                assertThat(forrigeUtdypendeVilkårsvurdering).containsExactly(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
                assertThat(nyUtdypendeVilkårsvurdering).containsExactly(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)
                assertThat(begrunnelse).isEqualTo("Ny begrunnelse")
            }
        }
    }

    @Nested
    inner class DeleteVilkårsperiode {
        @Test
        fun `skal nullstille vilkårsperiode hvis vilkår har én periode`() {
            // Arrange
            val vilkårsvurdering =
                vilkårsvurderingService.lagreNyOgDeaktiverGammel(
                    lagVilkårsvurdering(behandling = behandling) { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søkerAktør,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = it,
                                            behandlingId = behandling.id,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            periodeFom = LocalDate.of(2025, 1, 1),
                                            periodeTom = LocalDate.of(2025, 6, 30),
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            val vilkårResultat =
                vilkårsvurdering.personResultater
                    .single()
                    .vilkårResultater
                    .single()

            // Act
            vilkårService.deleteVilkårsperiode(
                behandlingId = behandling.id,
                vilkårId = vilkårResultat.id,
                aktør = søkerAktør,
            )

            // Assert
            val oppdatertVilkårResultat =
                vilkårService
                    .hentVilkårsvurderingThrows(behandling.id)
                    .personResultater
                    .single()
                    .vilkårResultater
                    .single()

            assertThat(oppdatertVilkårResultat.periodeFom).isNull()
            assertThat(oppdatertVilkårResultat.periodeTom).isNull()
            assertThat(oppdatertVilkårResultat.resultat).isEqualTo(Resultat.IKKE_VURDERT)
            assertThat(oppdatertVilkårResultat.begrunnelse).isBlank()
        }

        @Test
        fun `skal slette vilkårsperiode hvis vilkår har mer enn én periode`() {
            // Arrange
            val vilkårsvurdering =
                vilkårsvurderingService.lagreNyOgDeaktiverGammel(
                    lagVilkårsvurdering(behandling = behandling) { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søkerAktør,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = it,
                                            behandlingId = behandling.id,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            periodeFom = LocalDate.of(2025, 1, 1),
                                            periodeTom = LocalDate.of(2025, 6, 30),
                                        ),
                                        lagVilkårResultat(
                                            personResultat = it,
                                            behandlingId = behandling.id,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            periodeFom = LocalDate.of(2025, 7, 1),
                                            periodeTom = null,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            val vilkårResultater =
                vilkårsvurdering
                    .personResultater
                    .single()
                    .vilkårResultater

            val vilkårResultatSomSkalSlettes = vilkårResultater.first()
            val vilkårResultatSomSkalVæreUendret = vilkårResultater.last()

            // Act
            vilkårService.deleteVilkårsperiode(
                behandlingId = behandling.id,
                vilkårId = vilkårResultatSomSkalSlettes.id,
                aktør = søkerAktør,
            )

            // Assert
            val oppdatertVilkårResultat =
                vilkårService
                    .hentVilkårsvurderingThrows(behandling.id)
                    .personResultater
                    .single()
                    .vilkårResultater
                    .single()

            assertThat(oppdatertVilkårResultat)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*opprettetTidspunkt", ".*endretTidspunkt", ".*personResultat")
                .isEqualTo(vilkårResultatSomSkalVæreUendret)
        }
    }

    @Nested
    inner class DeleteVilkår {
        @Test
        fun `skal kaste FunksjonellFeil når man prøver å slette vilkår som ikke er UTVIDET_BARNETRYGD`() {
            // Arrange
            vilkårsvurderingService.lagreNyOgDeaktiverGammel(
                lagVilkårsvurdering(behandling = behandling) { vilkårsvurdering ->
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = vilkårsvurdering,
                            aktør = søkerAktør,
                        ),
                    )
                },
            )

            val slettVilkårDto =
                SlettVilkårDto(
                    personIdent = søkerFnr,
                    vilkårType = LOVLIG_OPPHOLD,
                )

            // Act & Assert
            val funksjonellFeil =
                assertThrows<FunksjonellFeil> {
                    vilkårService.deleteVilkår(
                        behandlingId = behandling.id,
                        slettVilkårDto = slettVilkårDto,
                    )
                }

            assertThat(funksjonellFeil.message).isEqualTo("Vilkår ${LOVLIG_OPPHOLD.beskrivelse} kan ikke slettes for behandling ${behandling.id}")
        }

        @Test
        fun `skal oppdatere behandlingstema når UTVIDET_BARNETRYGD-vilkår slettes`() {
            // Arrange
            behandlingHentOgPersisterService.lagreOgFlush(
                behandling.copy(
                    underkategori = UTVIDET,
                    opprettetÅrsak = TEKNISK_ENDRING,
                ),
            )

            vilkårsvurderingService.lagreNyOgDeaktiverGammel(
                lagVilkårsvurdering(behandling = behandling) { vilkårsvurdering ->
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = vilkårsvurdering,
                            aktør = søkerAktør,
                            lagVilkårResultater = {
                                setOf(
                                    lagVilkårResultat(
                                        personResultat = it,
                                        behandlingId = behandling.id,
                                        vilkårType = UTVIDET_BARNETRYGD,
                                    ),
                                )
                            },
                        ),
                        lagPersonResultat(
                            vilkårsvurdering = vilkårsvurdering,
                            aktør = barnAktør,
                        ),
                    )
                },
            )

            val slettVilkårDto =
                SlettVilkårDto(
                    personIdent = søkerFnr,
                    vilkårType = UTVIDET_BARNETRYGD,
                )

            // Act
            vilkårService.deleteVilkår(
                behandlingId = behandling.id,
                slettVilkårDto = slettVilkårDto,
            )

            // Assert
            val oppdatertBehandling = behandlingHentOgPersisterService.hent(behandlingId = behandling.id)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(ORDINÆR)
        }
    }

    @Nested
    inner class PostVilkår {
        @ParameterizedTest
        @EnumSource(value = BehandlingÅrsak::class, names = ["SØKNAD", "NYE_OPPLYSNINGER", "ÅRLIG_KONTROLL", "DØDSFALL_BRUKER"], mode = INCLUDE)
        fun `skal kaste FunksjonellFeil når man prøver å legge til utvidet barnetrygd for behandling med feil type`(
            behandlingÅrsak: BehandlingÅrsak,
        ) {
            // Arrange
            behandlingHentOgPersisterService.lagreOgFlush(behandling.copy(opprettetÅrsak = behandlingÅrsak))

            vilkårsvurderingService.lagreNyOgDeaktiverGammel(lagVilkårsvurdering(behandling = behandling))

            val nyttVilkårDto =
                NyttVilkårDto(
                    personIdent = barnFnr,
                    vilkårType = UTVIDET_BARNETRYGD,
                )

            // Act & Assert
            val funksjonellFeil =
                assertThrows<FunksjonellFeil> {
                    vilkårService.postVilkår(
                        behandlingId = behandling.id,
                        nyttVilkårDto = nyttVilkårDto,
                    )
                }

            assertThat(funksjonellFeil.frontendFeilmelding).isEqualTo(
                "${UTVIDET_BARNETRYGD.beskrivelse} kan ikke legges til " +
                    "for behandling ${behandling.id} med behandlingsårsak ${behandlingÅrsak.visningsnavn}",
            )
        }

        @Test
        fun `skal kaste FunksjonellFeil når man prøver å legge til utvidet barnetrygd for barn`() {
            // Arrange
            behandlingHentOgPersisterService.lagreOgFlush(behandling.copy(opprettetÅrsak = TEKNISK_ENDRING))

            vilkårsvurderingService.lagreNyOgDeaktiverGammel(lagVilkårsvurdering(behandling = behandling))

            val nyttVilkårDto =
                NyttVilkårDto(
                    personIdent = barnFnr,
                    vilkårType = UTVIDET_BARNETRYGD,
                )

            // Act & Assert
            val funksjonellFeil =
                assertThrows<FunksjonellFeil> {
                    vilkårService.postVilkår(
                        behandlingId = behandling.id,
                        nyttVilkårDto = nyttVilkårDto,
                    )
                }

            assertThat(funksjonellFeil.frontendFeilmelding).isEqualTo("${UTVIDET_BARNETRYGD.beskrivelse} kan ikke legges til for BARN")
        }

        @Test
        fun `skal oppdatere behandlingstema når UTVIDET_BARNETRYGD-vilkår legges til`() {
            // Arrange
            behandlingHentOgPersisterService.lagreOgFlush(
                behandling.copy(
                    underkategori = ORDINÆR,
                    opprettetÅrsak = TEKNISK_ENDRING,
                ),
            )

            vilkårsvurderingService.lagreNyOgDeaktiverGammel(
                lagVilkårsvurdering(behandling = behandling) {
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = søkerAktør,
                        ),
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = barnAktør,
                        ),
                    )
                },
            )

            val nyttVilkårDto =
                NyttVilkårDto(
                    personIdent = søkerFnr,
                    vilkårType = UTVIDET_BARNETRYGD,
                )

            // Act
            vilkårService.postVilkår(
                behandlingId = behandling.id,
                nyttVilkårDto = nyttVilkårDto,
            )

            // Assert
            val oppdatertBehandling = behandlingHentOgPersisterService.hent(behandlingId = behandling.id)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(UTVIDET)
        }

        @Test
        fun `skal legge til vilkår når input er gyldig`() {
            // Arrange
            vilkårsvurderingService.lagreNyOgDeaktiverGammel(
                lagVilkårsvurdering(behandling = behandling) {
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = søkerAktør,
                            lagVilkårResultater = { emptySet() },
                        ),
                    )
                },
            )

            val nyttVilkårDto =
                NyttVilkårDto(
                    personIdent = søkerFnr,
                    vilkårType = LOVLIG_OPPHOLD,
                )

            // Act
            vilkårService.postVilkår(
                behandlingId = behandling.id,
                nyttVilkårDto = nyttVilkårDto,
            )

            // Assert
            val vilkårResultat =
                vilkårsvurderingService
                    .hentAktivForBehandlingThrows(behandling.id)
                    .personResultater
                    .single()
                    .vilkårResultater
                    .single()

            assertThat(vilkårResultat.vilkårType).isEqualTo(LOVLIG_OPPHOLD)
        }
    }
}
