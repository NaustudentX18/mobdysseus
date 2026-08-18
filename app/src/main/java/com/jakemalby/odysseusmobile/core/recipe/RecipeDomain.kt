package com.jakemalby.odysseusmobile.core.recipe

import com.jakemalby.odysseusmobile.capability.CapabilityCatalog
import com.jakemalby.odysseusmobile.capability.CapabilityDenyRules
import com.jakemalby.odysseusmobile.capability.CapabilityId

enum class RecipeInputKind { TEXT, DOCUMENT_REFERENCE }
enum class RecipeModelCapability { TEXT, VISION, AUDIO, TOOL_USE }

data class RecipeInputField(
    val key: String,
    val label: String,
    val kind: RecipeInputKind,
    val required: Boolean = true,
    val maxLength: Int = 16_000,
) {
    init {
        require(key.matches(KEY_PATTERN)) { "Input key must be a lowercase identifier" }
        require(label.isNotBlank())
        require(maxLength > 0)
    }

    companion object {
        private val KEY_PATTERN = Regex("[a-z][a-z0-9_]*")
    }
}

data class RecipeModelRequirement(
    val capabilities: Set<RecipeModelCapability>,
) {
    init { require(capabilities.isNotEmpty()) }
}

data class RecipeDefinition(
    val id: String,
    val version: Int,
    val displayName: String,
    val promptTemplate: String,
    val inputSchema: List<RecipeInputField>,
    val modelRequirement: RecipeModelRequirement,
    val capabilityAllowlist: Set<CapabilityId> = emptySet(),
    val requiredTools: Set<String> = emptySet(),
) {
    init {
        require(id.matches(ID_PATTERN)) { "Recipe id must be stable and lowercase" }
        require(version > 0)
        require(displayName.isNotBlank())
        require(promptTemplate.isNotBlank())
    }

    val versionedId: String get() = "$id@$version"

    companion object {
        private val ID_PATTERN = Regex("[a-z0-9][a-z0-9._-]*")
    }
}

enum class RecipeValidationCode {
    DUPLICATE_INPUT_KEY,
    UNKNOWN_TEMPLATE_INPUT,
    MISSING_TEMPLATE_INPUT,
    PROHIBITED_CAPABILITY,
    UNKNOWN_CAPABILITY,
    INVALID_TOOL_ID,
}

data class RecipeValidationIssue(
    val code: RecipeValidationCode,
    val subject: String,
)

object RecipeValidator {
    private val placeholder = Regex("\\{\\{([a-z][a-z0-9_]*)}}")
    private val toolId = Regex("[a-z0-9][a-z0-9._-]*")

    fun validate(definition: RecipeDefinition): List<RecipeValidationIssue> = buildList {
        val inputKeys = definition.inputSchema.map { it.key }
        inputKeys.groupingBy { it }.eachCount()
            .filterValues { it > 1 }
            .keys
            .sorted()
            .forEach { add(RecipeValidationIssue(RecipeValidationCode.DUPLICATE_INPUT_KEY, it)) }

        val placeholders = placeholder.findAll(definition.promptTemplate)
            .map { it.groupValues[1] }
            .toSet()
        (placeholders - inputKeys.toSet()).sorted().forEach {
            add(RecipeValidationIssue(RecipeValidationCode.UNKNOWN_TEMPLATE_INPUT, it))
        }
        (definition.inputSchema.filter { it.required }.map { it.key }.toSet() - placeholders)
            .sorted()
            .forEach { add(RecipeValidationIssue(RecipeValidationCode.MISSING_TEMPLATE_INPUT, it)) }

        definition.capabilityAllowlist.sortedBy { it.name }.forEach { capability ->
            when {
                CapabilityDenyRules.isDenied(capability) -> add(
                    RecipeValidationIssue(RecipeValidationCode.PROHIBITED_CAPABILITY, capability.name),
                )
                CapabilityCatalog.descriptor(capability) == null -> add(
                    RecipeValidationIssue(RecipeValidationCode.UNKNOWN_CAPABILITY, capability.name),
                )
            }
        }
        definition.requiredTools.filterNot { it.matches(toolId) }.sorted().forEach {
            add(RecipeValidationIssue(RecipeValidationCode.INVALID_TOOL_ID, it))
        }
    }
}

/** Immutable recipes shipped with the APK. Execution is supplied by a separate local runtime. */
object BuiltInRecipes {
    val quickChat = RecipeDefinition(
        id = "quick-chat",
        version = 1,
        displayName = "Quick Chat",
        promptTemplate = "Answer clearly and concisely.\n\nUser: {{message}}",
        inputSchema = listOf(RecipeInputField("message", "Message", RecipeInputKind.TEXT)),
        modelRequirement = RecipeModelRequirement(setOf(RecipeModelCapability.TEXT)),
    )

    val deepWork = RecipeDefinition(
        id = "deep-work",
        version = 1,
        displayName = "Deep Work",
        promptTemplate = "Develop a careful work plan for this goal: {{goal}}\n\nContext: {{context}}",
        inputSchema = listOf(
            RecipeInputField("goal", "Goal", RecipeInputKind.TEXT, maxLength = 8_000),
            RecipeInputField("context", "Context", RecipeInputKind.TEXT, required = false),
        ),
        modelRequirement = RecipeModelRequirement(setOf(RecipeModelCapability.TEXT)),
        capabilityAllowlist = setOf(CapabilityId.READ_PRIVATE_WORKSPACE, CapabilityId.CREATE_TASK),
    )

    val documentCompanion = RecipeDefinition(
        id = "document-companion",
        version = 1,
        displayName = "Document Companion",
        promptTemplate = "Use the selected local document {{document_id}} to answer: {{question}}",
        inputSchema = listOf(
            RecipeInputField("document_id", "Document", RecipeInputKind.DOCUMENT_REFERENCE, maxLength = 256),
            RecipeInputField("question", "Question", RecipeInputKind.TEXT, maxLength = 8_000),
        ),
        modelRequirement = RecipeModelRequirement(setOf(RecipeModelCapability.TEXT)),
        capabilityAllowlist = setOf(CapabilityId.READ_PRIVATE_WORKSPACE),
    )

    val voiceCapture = RecipeDefinition(
        id = "voice-capture",
        version = 1,
        displayName = "Voice Capture",
        promptTemplate = "Turn this locally captured transcript into a concise note: {{transcript}}",
        inputSchema = listOf(RecipeInputField("transcript", "Transcript", RecipeInputKind.TEXT, maxLength = 32_000)),
        modelRequirement = RecipeModelRequirement(setOf(RecipeModelCapability.TEXT)),
        capabilityAllowlist = setOf(CapabilityId.CREATE_NOTE),
    )

    val all: List<RecipeDefinition> = listOf(quickChat, deepWork, documentCompanion, voiceCapture)
}
