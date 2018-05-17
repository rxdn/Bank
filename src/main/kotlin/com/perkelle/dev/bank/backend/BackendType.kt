package com.perkelle.dev.bank.backend

enum class BackendType(val simpleName: String) {
    FLATFILE("file"),
    DATABASE("mysql"),
}