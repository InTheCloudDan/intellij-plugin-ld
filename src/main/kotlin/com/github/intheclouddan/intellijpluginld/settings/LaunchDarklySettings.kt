package com.github.intheclouddan.intellijpluginld.settings

import com.github.intheclouddan.intellijpluginld.LaunchDarklyApiClient
import com.github.intheclouddan.intellijpluginld.messaging.DefaultMessageBusService
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.*
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.withTextBinding
import javax.swing.DefaultComboBoxModel
import javax.swing.JPanel
import javax.swing.JPasswordField

/*
 * Maintain state of what LaunchDarkly Project to connect to.
 */
@State(name = "LaunchDarklyConfig", storages = [Storage("launchdarkly.xml")])
open class LaunchDarklyConfig(project: Project) : PersistentStateComponent<LaunchDarklyConfig.ConfigState> {
    val project: Project = project
    var ldState: ConfigState = ConfigState()

    companion object {
        fun getInstance(project: Project): LaunchDarklyConfig {
            return ServiceManager.getService(project, LaunchDarklyConfig(project)::class.java)
        }
    }

    override fun getState(): ConfigState {
        return ldState
    }

    override fun loadState(state: ConfigState) {
        ldState = state
    }

    // Not in working state.
    fun isConfigured(): Boolean {
        if (ldState.project == "" || ldState.environment == "" || ldState.authorization == "") {
            return false
        }
        return true
    }

//    fun creds(key: String) {
//        var setKey = ConfigState::credName.javaClass as String
//        setKey = key
//
//    }

    data class ConfigState(
            override var credName: String = "",
            override var project: String = "",
            override var environment: String = "",
            override var refreshRate: Int = -1,
            override var baseUri: String = ""
    ) : LDSettings {
        private val key: String = "apiKey"
        //var credStoreName = "launchdarkly-intellij-$credName"
        //private var credentialAttributes: CredentialAttributes =


        // Stored in System Credential store
        override var authorization: String
            get() = PasswordSafe.instance.getPassword(CredentialAttributes(generateServiceName(
                    "launchdarkly-intellij-$credName",
                    key
            ))) ?: ""
            set(value) {
                if (credName == "") {
                    return
                }
                val credentials = Credentials("", value)
                PasswordSafe.instance.set(CredentialAttributes(generateServiceName(
                        "launchdarkly-intellij-$credName",
                        key
                )), credentials)
            }

        override fun isConfigured(): Boolean {
            if (project == "" || environment == "" || authorization == "") {
                return false
            }
            return true
        }

    }
}

class LaunchDarklyConfigurable(private val project: Project) : BoundConfigurable(displayName = "LaunchDarkly Plugin") {
    private val apiField = JPasswordField()
    private val messageBusService = project.service<DefaultMessageBusService>()
    private val mergedSettings = project.service<LaunchDarklyMergedSettings>()
    private val settings = LaunchDarklyConfig.getInstance(project).ldState
    private val origApiKey = settings.authorization
    private val origBaseUri = settings.baseUri
    private var modified = false
    private var panel = JPanel()
    private var apiUpdate = false
    private var lastSelectedProject = ""
    lateinit var projectContainer: MutableList<com.launchdarkly.api.model.Project>
    lateinit var environmentContainer: com.launchdarkly.api.model.Project

    private lateinit var defaultMessage: String
    private lateinit var projectBox: DefaultComboBoxModel<String>
    private lateinit var environmentBox: DefaultComboBoxModel<String>

    init {
        try {
            projectContainer = getProjects(null, null)
            if (projectContainer.size > 0) {
                environmentContainer = projectContainer.find { it.key == settings.project }
                        ?: projectContainer.firstOrNull() as com.launchdarkly.api.model.Project
            }
        } catch (err: Exception) {
            defaultMessage = "Check API Key"
        }
    }

    override fun createPanel(): DialogPanel {
        panel = panel {
            noteRow("Any settings manually selected here will override the corresponding Application settings.")
            noteRow("Project and Environment selections will populate based on key permissions.")
            row("API Key:") { apiField().withTextBinding(PropertyBinding({ settings.authorization }, { settings.authorization = it })) }
            try {
                projectBox = if (::projectContainer.isInitialized) {
                    DefaultComboBoxModel(projectContainer.map { it.key }.toTypedArray())
                } else {
                    DefaultComboBoxModel(arrayOf(defaultMessage))
                }
                row("Project") {
                    comboBox(projectBox, settings::project, renderer = SimpleListCellRenderer.create<String> { label, value, _ ->
                        label.text = value
                    })

                }

                environmentBox = if (::environmentContainer.isInitialized) {
                    DefaultComboBoxModel(environmentContainer.environments.map { it.key }.toTypedArray())
                } else {
                    DefaultComboBoxModel(arrayOf("Please select a Project"))
                }
                row("Environments:") {
                    comboBox(environmentBox, settings::environment, renderer = SimpleListCellRenderer.create<String> { label, value, _ ->
                        label.text = value
                    })
                }

            } catch (err: Exception) {
                println(err)
            }
            noteRow("Leaving Refresh Rate as -1 will inherit value from Application settings.")
            hideableRow("Refresh Rate(in Minutes):") { intTextField(settings::refreshRate) }
            hideableRow("Base URL:") { textField(settings::baseUri) }
        }
        return panel as DialogPanel
    }

    override fun isModified(): Boolean {
        if (settings.credName != project.name) settings.credName = project.name
        if ((settings.authorization != origApiKey || settings.baseUri != origBaseUri) && !apiUpdate) {
            try {
                settings.credName = project.name
                val uri = if (settings.baseUri != "") settings.baseUri else mergedSettings.baseUri
                projectContainer = getProjects(settings.authorization, uri)
                with(projectBox) {
                    removeAllElements()
                    if (selectedItem == null || selectedItem.toString() == "Check API Key") {
                        selectedItem = projectContainer.map { it.key }.firstOrNull()
                    }
                    projectContainer.map { addElement(it.key) }
                }
                apiUpdate = true
            } catch (err: Error) {
                println(err)
            }
        }
//
//        if () {
//            try {
//                projectContainer = getProjects()
//                with(projectBox) {
//                    removeAllElements()
//                    if (selectedItem == null || selectedItem.toString() == "Check API Key") {
//                        selectedItem = projectContainer.map { it.key }.firstOrNull()
//                    }
//                    projectContainer.map { addElement(it.key) }
//                }
//                apiUpdate = true
//            } catch (err: Error) {
//                println(err)
//            }
//        }
        if (::projectContainer.isInitialized && lastSelectedProject != projectBox.selectedItem.toString()) {
            lastSelectedProject = projectBox.selectedItem.toString()
            try {
                environmentContainer = projectContainer.find { it.key == projectBox.selectedItem.toString() }!!
                val envMap = environmentContainer.environments.map { it.key }.sorted()
                if (::environmentBox.isInitialized) {
                    with(environmentBox) {
                        removeAllElements()
                        envMap.map { addElement(it) }
                        if (selectedItem == null || selectedItem.toString() == "Please select a Project") {
                            selectedItem = if (settings.environment != "") settings.environment else envMap.firstOrNull()
                        }
                    }
                }
            } catch (err: Error) {
                println(err)
            }
        }

        if (::projectBox.isInitialized || settings.project != projectBox.selectedItem.toString()) {
            modified = true
        }

        if (::environmentBox.isInitialized) {
            if (settings.environment != environmentBox.selectedItem.toString()) {
                modified = true
            }
        }

        val sup = super.isModified()
        return modified || sup
    }

    override fun apply() {
        super.apply()

        if (settings.project != projectBox.selectedItem.toString() && projectBox.selectedItem.toString() != defaultMessage) {
            settings.project = projectBox.selectedItem.toString()
        }

        if (settings.environment != environmentBox.selectedItem.toString() && environmentBox.selectedItem.toString() != "Please select a Project") {
            settings.environment = environmentBox.selectedItem.toString()
        }

        settings.credName = project.name
        if ((projectBox.selectedItem != "Check API Key") && modified) {
            val publisher = project.messageBus.syncPublisher(messageBusService.configurationEnabledTopic)
            publisher.notify(true)
            println("notifying")
        }

    }

    fun getProjects(apiKey: String?, baseUri: String?): MutableList<com.launchdarkly.api.model.Project> {
        val projectApi = LaunchDarklyApiClient.projectInstance(project, apiKey, baseUri)
        return projectApi.projects.items.sortedBy { it.key } as MutableList<com.launchdarkly.api.model.Project>
    }

}
