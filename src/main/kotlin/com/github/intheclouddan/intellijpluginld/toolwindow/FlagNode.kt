package com.github.intheclouddan.intellijpluginld.toolwindow

import com.github.intheclouddan.intellijpluginld.LDIcons
import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyConfig
import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.api.model.*
import java.util.*
import javax.swing.Icon

class RootNode(flags: FeatureFlags, settings: LaunchDarklyConfig) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    private val flags = flags
    private val settings = settings

    override fun getChildren(): Array<SimpleNode> {
        if (myChildren.isEmpty() && flags.items != null) {
            for (flag in flags.items) {
                myChildren.add(FlagNodeParent(flag, settings, flags))
            }
        } else {
            myChildren.add(FlagNodeBase("LaunchDarkly Plugin is not configurable properly."))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        data.setPresentableText("root")
    }

}

class FlagNodeParent(flag: FeatureFlag, settings: LaunchDarklyConfig, flags: FeatureFlags) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    val flag: FeatureFlag = flag
    val flags = flags
    val settings = settings


    override fun getChildren(): Array<SimpleNode> {
        val env = flag.environments[settings.ldState.environment]!!
        if (myChildren.isEmpty()) {
            myChildren.add(FlagNodeBase("Key: ${flag.key}", LDIcons.FLAG_KEY))
            if (flag.description != "") {
                myChildren.add(FlagNodeBase("Description: ${flag.description}", LDIcons.DESCRIPTION))
            }
            myChildren.add(FlagNodeVariations(flag))
            var enabledIcon: Icon
            if (env.isOn) {
                enabledIcon = LDIcons.TOGGLE_ON
            } else {
                enabledIcon = LDIcons.TOGGLE_OFF
            }
            myChildren.add(FlagNodeBase("Enabled: ${flag.environments[settings.ldState.environment]!!.isOn}", enabledIcon))
            if (env.prerequisites.size > 0) {
                myChildren.add(FlagNodePrerequisites(flag, env.prerequisites, flags))
            }
            if (env.fallthrough != null) {
                myChildren.add(FlagNodeFallthrough(flag))
            }
            if (env.offVariation != null) {
                myChildren.add(FlagNodeBase("Off Variation: ${flag.variations[env.offVariation].name ?: flag.variations[env.offVariation].value}", LDIcons.OFF_VARIATION))
            }
            if (flag.tags.size > 0) {
                myChildren.add(FlagNodeTags(flag.tags))
            }
            if (flag.defaults != null) {
                myChildren.add(FlagNodeDefaults(flag))
            }
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        val label = flag.name ?: flag.key
        data.setPresentableText(label)
        data.setIcon(LDIcons.FLAG)
    }
}

class FlagNodeBase(label: String, labelIcon: Icon? = null) : SimpleNode() {
    val label: String = label
    val labelIcon = labelIcon

    override fun getChildren(): Array<SimpleNode> {
        return SimpleNode.NO_CHILDREN
    }

    override fun update(data: PresentationData) {
        data.setPresentableText(label)
        data.tooltip = label
        if (labelIcon != null) {
            data.setIcon(labelIcon)
        }
    }
}

class FlagNodeVariations(flag: FeatureFlag) : SimpleNode() {
    var flag: FeatureFlag = flag
    private var myChildren: MutableList<SimpleNode> = ArrayList()


    override fun getChildren(): Array<SimpleNode> {
        for (variation in flag.variations) {
            myChildren.add(FlagNodeVariation(variation, false))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        data.setPresentableText("Variations")
        data.setIcon(LDIcons.VARIATION)
    }
}

class FlagNodeVariation(variation: Variation, child: Boolean) : SimpleNode() {
    val variation = variation
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
        if (variation.name != null) {
            myChildren.add(FlagNodeBase("Value: ${variation.value.toString()}", LDIcons.DESCRIPTION))
        }
        if (variation.description != null) {
            myChildren.add(FlagNodeBase("Description ${variation.description}", LDIcons.DESCRIPTION))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        var label: String = variation.name ?: variation.value.toString()
        data.setPresentableText(label)
    }
}

class FlagNodeTags(tags: List<String>) : SimpleNode() {
    val tags: List<String> = tags
    private var myChildren: MutableList<SimpleNode> = ArrayList()


    override fun getChildren(): Array<SimpleNode> {
        for (tag in tags) {
            myChildren.add(FlagNodeBase(tag))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        data.setPresentableText("Tags")
        data.setIcon(LDIcons.TAGS)
    }
}

class FlagNodeFallthrough(flag: FeatureFlag) : SimpleNode() {
    val flag = flag
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    val env = flag.environments.keys.first()

    override fun getChildren(): Array<SimpleNode> {
        if (flag.environments[env]!!.fallthrough.variation != null) {
            return SimpleNode.NO_CHILDREN
        }
        myChildren.add(FlagNodeRollout(flag.environments[env]!!.fallthrough.rollout, flag.variations))
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        var label: String
        if (flag.environments[env]!!.fallthrough.variation != null) {
            label = "Fallthrough: ${flag.variations[flag.environments[env]!!.fallthrough.variation].name ?: flag.variations[flag.environments[env]!!.fallthrough.variation].value}"
        } else {
            label = "Fallthrough"
        }
        data.setPresentableText(label)
        data.setIcon(LDIcons.DESCRIPTION)
    }
}

class FlagNodeRollout(rollout: Rollout, variations: List<Variation>) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    val rollout = rollout
    val variations = variations

    override fun getChildren(): Array<SimpleNode> {
        myChildren.add(FlagNodeBase("bucketBy ${rollout.bucketBy}", LDIcons.DESCRIPTION))
        for (variation in rollout.variations) {
            myChildren.add(FlagNodeBase("Variation: ${variations[variation.variation].name ?: variations[variation.variation].value}"))
            myChildren.add(FlagNodeBase("Weight: ${variation.weight / 1000.0}%"))

        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        data.setPresentableText("Rollout")
        data.setIcon(LDIcons.VARIATION)
    }
}

class FlagNodeDefaults(flag: FeatureFlag) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    val flag = flag

    override fun getChildren(): Array<SimpleNode> {
        myChildren.add(FlagNodeBase("Off Variation: ${flag.variations[flag.defaults.offVariation]}", LDIcons.VARIATION))
        myChildren.add(FlagNodeBase("On Variation: ${flag.variations[flag.defaults.onVariation]}", LDIcons.VARIATION))

        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        data.setPresentableText("Default Variations")
        data.setIcon(LDIcons.VARIATION)
    }
}

class FlagNodePrerequisites(flag: FeatureFlag, prereqs: List<Prerequisite>, flags: FeatureFlags) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    val flag = flag
    val flags = flags
    val prereqs = prereqs

    override fun getChildren(): Array<SimpleNode> {
        prereqs.map {
            myChildren.add(FlagNodeBase("Flag Key: ${it.key}", LDIcons.FLAG))
            val key = it.key
            val flagVariation = flags.items.find { key == key }
            myChildren.add(FlagNodeBase("Variation: ${flagVariation!!.variations[it.variation].name ?: flagVariation!!.variations[it.variation].value}", LDIcons.VARIATION))

        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        data.setPresentableText("Prequisites")
        data.setIcon(LDIcons.PREREQUISITE)
    }
}