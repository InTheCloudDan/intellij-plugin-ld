package com.github.intheclouddan.intellijpluginld.hints

import com.intellij.codeInsight.hints.InlayHintsProviderFactory
import com.intellij.codeInsight.hints.ProviderInfo
import com.intellij.lang.Language
import com.intellij.openapi.project.Project

class LDHintsProviderFactory : InlayHintsProviderFactory {
    override fun getProvidersInfo(project: Project): List<ProviderInfo<out Any>> {
        return listOf(ProviderInfo(Language.ANY, LDHintsProvider()))
    }
}