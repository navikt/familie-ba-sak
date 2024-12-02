package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.common.lagAnnenVurdering
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VilkårsvurderingTest {
    @Nested
    inner class ErOpplysningspliktVilkårOppfyltTest {
        @Test
        fun `skal exception om personresultater er tom`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = { emptySet() },
                )

            // Act & assert
            val exception =
                assertThrows<NoSuchElementException> {
                    vilkårsvurdering.erOpplysningspliktVilkårOppfylt()
                }
            assertThat(exception.message).isEqualTo("Collection contains no element matching the predicate.")
        }

        @Test
        fun `skal kaste exception om ingen vilkår er for søker`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = vilkårsvurdering.behandling.fagsak.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            vilkårType = Vilkår.UNDER_18_ÅR,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            ),
                        )
                    },
                )

            // Act & assert
            val exception =
                assertThrows<NoSuchElementException> {
                    vilkårsvurdering.erOpplysningspliktVilkårOppfylt()
                }
            assertThat(exception.message).isEqualTo("Collection contains no element matching the predicate.")
        }

        @Test
        fun `skal returnere false om annen vurdering er tom`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = vilkårsvurdering.behandling.fagsak.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            vilkårType = Vilkår.BOR_MED_SØKER,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = { emptySet() },
                            ),
                        )
                    },
                )

            // Act
            val erOpplysningspliktVilkårOppfylt = vilkårsvurdering.erOpplysningspliktVilkårOppfylt()

            // Assert
            assertThat(erOpplysningspliktVilkårOppfylt).isFalse()
        }

        @Test
        fun `skal returnere false om annen vurdering opplysningsplikt ikke er oppfylt`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = vilkårsvurdering.behandling.fagsak.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            vilkårType = Vilkår.BOR_MED_SØKER,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = {
                                    setOf(
                                        lagAnnenVurdering(
                                            personResultat = it,
                                            type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                                            resultat = Resultat.IKKE_OPPFYLT,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            // Act
            val erOpplysningspliktVilkårOppfylt = vilkårsvurdering.erOpplysningspliktVilkårOppfylt()

            // Assert
            assertThat(erOpplysningspliktVilkårOppfylt).isFalse()
        }

        @Test
        fun `skal returnere true om annen vurdering opplysningsplikt er oppfylt`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = vilkårsvurdering.behandling.fagsak.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            vilkårType = Vilkår.BOR_MED_SØKER,
                                        ),
                                    )
                                },
                                lagAnnenVurderinger = {
                                    setOf(
                                        lagAnnenVurdering(
                                            personResultat = it,
                                            type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                                            resultat = Resultat.OPPFYLT,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            // Act
            val erOpplysningspliktVilkårOppfylt = vilkårsvurdering.erOpplysningspliktVilkårOppfylt()

            // Assert
            assertThat(erOpplysningspliktVilkårOppfylt).isTrue()
        }
    }
}
