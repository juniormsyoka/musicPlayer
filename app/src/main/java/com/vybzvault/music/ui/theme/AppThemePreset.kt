package com.vybzvault.music.ui.theme

enum class AppThemePreset(
    val title: String,
    val description: String
) {
    MIDNIGHT_NEON(
        title = "Midnight Neon",
        description = "Deep dark base with high-energy orange and glowing cyan highlights"
    ),
    WAVEFORM_GREEN(
        title = "Waveform Green",
        description = "Crisp technical dark base with teal and green highlights"
    ),
    LO_FI_BLUSH(
        title = "Lo-Fi Blush",
        description = "Soft cozy muted pinks and warm grays on light backgrounds"
    ),
    RETRO_ANALOG(
        title = "Retro Analog Vinyl",
        description = "Nostalgic cream paper tones with warm analog accents"
    );

    companion object {
        val DEFAULT = MIDNIGHT_NEON
    }
}
