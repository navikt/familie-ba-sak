UPDATE behandling_steg_tilstand
SET behandling_steg = 'FILTRERING_AUTOMATISK_BEHANDLING'
WHERE behandling_steg = 'FILTRERING_FØDSELSHENDELSER';