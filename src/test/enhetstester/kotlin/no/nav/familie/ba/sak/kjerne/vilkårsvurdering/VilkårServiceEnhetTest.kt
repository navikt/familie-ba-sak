package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.ekstern.restDomene.VilkårResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilPersonResultatDto
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.EndringIPreutfyltVilkårLoggRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.ResultatBegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class VilkårServiceEnhetTest {
    private val behandlingService = mockk<BehandlingService>()
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val personidentService = mockk<PersonidentService>()
    private val endringIPreutfyltVilkårLoggRepository = mockk<EndringIPreutfyltVilkårLoggRepository>()
    private val tilbakestillBehandlingService = mockk<TilbakestillBehandlingService>()

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
