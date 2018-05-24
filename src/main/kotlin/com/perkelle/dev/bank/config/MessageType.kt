package com.perkelle.dev.bank.config

enum class MessageType(val configName: String) {
    PREFIX("prefix"),
    NO_PERMISSION("no-permission"),
    PLAYER_ONLY("player-only"),
    SPECIFY_PLAYER("specify-player"),
    HELP("help"),
    OWN_BALANCE("own-balance"),
    OTHER_BALANCE("other-balance"),
    NEVER_JOINED("never-joined"),
    SPECIFY_AMOUNT("specify-amount"),
    DEPOSIT_OWN("deposit-own"),
    DEPOSIT_OTHER("deposit-other"),
    WITHDRAW_OWN("withdraw-own"),
    WITHDRAW_OTHER("withdraw-other"),
    TOO_POOR("not-enough-money"),
    TOO_POOR_OTHER("other-not-enough-money"),
    ERROR("error"),
    BALTOP_FIRST("baltop-first-line"),
    BALTOP_LINE("baltop-balance-line"),
    UPDATED_BALANCE("updated-balance"),
    UPDATED_OTHER_BALANCE("updated-other-balance"),
}