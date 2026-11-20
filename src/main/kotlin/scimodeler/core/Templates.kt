package scimodeler.core

data class Template(val name: String, val description: String, val doc: () -> ModelDoc) {
    val layout: String get() = doc().layout
}

object Templates {
    private fun i(label: String, icon: String? = null, vararg bullets: String, role: Role = Role.AUTO) =
        Item(label = label, bullets = bullets.toList(), icon = icon, role = role)

    val all = listOf(
        Template("Collaboration wheel", "Six factors around a core concept") {
            ModelDoc(
                title = "Model of interprofessional collaboration",
                center = "Interprofessional collaboration",
                layout = "wheel",
                items = listOf(
                    i("Organisational support", "briefcase", "Team members are reachable when needed", "Support from management and decision-makers"),
                    i("Shared decision-making", "scale", "Active participation in therapy decisions", "Changes based on observations of the patient"),
                    i("Roles & communication", "chat", "Roles and tasks of each profession are clear", "Mutual trust between professions", "Clear communication channels"),
                    i("Patient involvement", "user", "Patient takes part in decisions", "Understanding of therapy is checked regularly"),
                    i("Education & development", "cap", "Continuous learning within the team", "Protocols linked to training"),
                    i("Interdisciplinary focus", "network", "Better treatment through joint work", "Patient receives consistent information"),
                ),
                legend = "Add a short explanation of abbreviations or the source of the model here.",
            )
        },
        Template("Socio-ecological model", "Concentric levels of influence") {
            ModelDoc(
                title = "Socio-ecological model", center = "Individual", layout = "onion",
                items = listOf(
                    i("Interpersonal", null, "Family, friends, peers"),
                    i("Organisational", null, "Schools, workplaces, health services"),
                    i("Community", null, "Relationships between organisations", "Local norms and resources"),
                    i("Public policy", null, "National and local laws", "Health policy and funding"),
                ),
            )
        },
        Template("Communication model", "Core concept branching into five skills") {
            ModelDoc(
                title = "Patient communication model", center = "LEARN", layout = "branches",
                items = listOf(
                    i("Listen", "chat", "Hear the patient's view of the problem"),
                    i("Empathise", "heart", "Acknowledge feelings and concerns"),
                    i("Analyse", "search", "Compare perspectives and identify gaps"),
                    i("Respond", "send", "Explain recommendations clearly"),
                    i("Negotiate", "users", "Agree on a shared plan of care"),
                ),
            )
        },
        Template("SIPOC", "Process scope from suppliers to customers") {
            ModelDoc(
                title = "SIPOC model", center = "SIPOC", layout = "arc",
                items = listOf(
                    i("Suppliers", "cube", "Who provides the inputs"),
                    i("Inputs", "input", "Materials, information, resources"),
                    i("Process", "gear", "The main steps that add value"),
                    i("Outputs", "output", "Products or services delivered"),
                    i("Customers", "users", "Who receives the outputs"),
                ),
            )
        },
        Template("Responsibility cone", "Roles from responsible to informed") {
            ModelDoc(
                title = "RASCI responsibility framework", layout = "cone",
                items = listOf(
                    i("Responsible", "gear", "Does the work to complete the task"),
                    i("Accountable", "scale", "Owns the outcome and signs off"),
                    i("Support", "users", "Provides resources and assistance"),
                    i("Consulted", "chat", "Gives input before decisions"),
                    i("Informed", "info", "Kept up to date on progress"),
                ),
            )
        },
        Template("Recruitment funnel", "Narrowing stages of a study sample") {
            ModelDoc(
                title = "Participant recruitment", layout = "funnel",
                items = listOf(
                    i("Invited", null, "All eligible patients contacted"),
                    i("Responded", null, "Returned the screening form"),
                    i("Eligible", null, "Met inclusion criteria"),
                    i("Consented", null, "Signed informed consent"),
                    i("Analysed", null, "Completed follow-up"),
                ),
            )
        },
        Template("Change stages", "Five stages of individual change") {
            ModelDoc(
                title = "Stages of change", center = "Change", layout = "fanout",
                items = listOf(
                    i("Awareness", "eye", "Understanding why change is needed"),
                    i("Desire", "heart", "Willingness to support the change"),
                    i("Knowledge", "book", "Knowing how to change"),
                    i("Ability", "gear", "Implementing new skills and behaviours"),
                    i("Reinforcement", "refresh", "Sustaining the change over time"),
                ),
            )
        },
        Template("Stepwise path", "Alternating steps along a path") {
            ModelDoc(
                title = "Implementation path", layout = "zigzag",
                items = listOf(
                    i("Assess", "search", "Map current practice and barriers"),
                    i("Design", "clipboard", "Choose strategies and outcomes"),
                    i("Pilot", "flask", "Test on one ward"),
                    i("Adapt", "refresh", "Refine based on feedback"),
                    i("Scale up", "trend", "Roll out to the organisation"),
                ),
            )
        },
        Template("Maturity levels", "Chevron timeline with icons") {
            ModelDoc(
                title = "Capability maturity levels", layout = "milestones",
                items = listOf(
                    i("Initial", "alert", "Processes are ad hoc and reactive"),
                    i("Managed", "clipboard", "Basic planning and tracking"),
                    i("Defined", "document", "Standard processes across teams"),
                    i("Measured", "chart", "Processes measured and controlled"),
                    i("Optimising", "trend", "Continuous improvement"),
                ),
            )
        },
        Template("Five dimensions", "Overlapping rings around a core") {
            ModelDoc(
                title = "Dimensions of quality of care", center = "Quality", layout = "flower",
                items = listOf(
                    i("Safety", "shield", "Avoiding harm to patients"),
                    i("Effectiveness", "target", "Evidence-based services"),
                    i("Patient-centredness", "heart", "Respecting preferences and values"),
                    i("Timeliness", "clock", "Reducing waits and delays"),
                    i("Efficiency", "coins", "Avoiding waste of resources"),
                ),
            )
        },
        Template("Research workflow", "Stage cards with a feedback loop") {
            ModelDoc(
                title = "From evidence to practice", center = "Results guide the next cycle", layout = "cards",
                items = listOf(
                    i("Evidence", "database", "Literature review", "Registry data"),
                    i("Discover", "bulb", "Hypotheses", "Key variables"),
                    i("Integrate", "gear", "Intervention design", "Pilot study"),
                    i("Evaluate", "check", "Outcome measures", "Long-term follow-up"),
                ),
            )
        },
        Template("Research model", "Independent variables → measures → outcome") {
            ModelDoc(
                title = "Research model", center = "Patient adherence", layout = "variables",
                items = listOf(
                    i("Health literacy", role = Role.INPUT),
                    i("Social support", role = Role.INPUT),
                    i("Pharmacist counselling", role = Role.INPUT),
                    i("Refill rate", role = Role.OUTPUT),
                    i("Self-reported adherence", role = Role.OUTPUT),
                    i("Missed doses", role = Role.OUTPUT),
                ),
            )
        },
        Template("Conceptual framework", "Inputs and context around a research question") {
            ModelDoc(
                title = "Conceptual framework", center = "Central research question", layout = "framework",
                items = listOf(
                    i("Independent variables", role = Role.INPUT),
                    i("Contributing factors", role = Role.INPUT),
                    i("Contextual elements", role = Role.INPUT),
                    i("Moderating variables", role = Role.INPUT),
                    i("Dependent variables", role = Role.OUTPUT),
                    i("Outcome measures", role = Role.OUTPUT),
                    i("Impact assessment", role = Role.OUTPUT),
                    i("Theoretical lens", role = Role.CONTEXT),
                    i("Methodology", role = Role.CONTEXT),
                    i("Data collection", role = Role.CONTEXT),
                ),
            )
        },
        Template("Research process", "Five-step process with descriptions") {
            ModelDoc(
                title = "Research design", center = "Mixed-methods study", layout = "process",
                items = listOf(
                    i("Literature review", null, "Systematic search", "Identify research gap"),
                    i("Study design", null, "Research questions", "Sample and instruments"),
                    i("Data collection", null, "Survey", "Semi-structured interviews"),
                    i("Analysis", null, "Statistical analysis", "Thematic analysis"),
                    i("Dissemination", null, "Thesis", "Article and conference"),
                ),
            )
        },
        Template("PDCA cycle", "Plan–Do–Check–Act quality cycle") {
            ModelDoc(
                title = "PDCA cycle", center = "Continuous improvement", layout = "cycle",
                style = Style(theme = "soft"),
                items = listOf(
                    i("Plan", "calendar", "Define the problem", "Set objectives"),
                    i("Do", "gear", "Implement on a small scale"),
                    i("Check", "search", "Measure and compare results"),
                    i("Act", "refresh", "Standardise or adjust"),
                ),
            )
        },
        Template("Blank", "Start from scratch") {
            ModelDoc(title = "New model", center = "Core concept", items = listOf(i("First item"), i("Second item"), i("Third item")))
        },
    )
}
