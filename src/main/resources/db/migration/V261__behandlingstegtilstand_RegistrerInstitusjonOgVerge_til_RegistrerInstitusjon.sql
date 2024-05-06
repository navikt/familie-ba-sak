UPDATE behandling_steg_tilstand

SET behandling_steg = 'REGISTRERE_INSTITUSJON'
WHERE behandling_steg = 'REGISTRERE_INSTITUSJON_OG_VERGE';