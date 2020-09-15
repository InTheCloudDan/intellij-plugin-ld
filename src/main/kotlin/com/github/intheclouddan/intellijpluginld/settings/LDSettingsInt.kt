package com.github.intheclouddan.intellijpluginld.settings

interface LDSettings {
    var credName: String
    var project: String
    var environment: String
    var refreshRate: Int
    var baseUri: String
    var authorization: String

    fun isConfigured(): Boolean
}