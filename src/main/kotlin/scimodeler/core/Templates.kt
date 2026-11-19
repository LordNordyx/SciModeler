package scimodeler.core

data class Template(val name: String, val description: String, val doc: () -> ModelDoc)

object Templates {
    private fun i(label: String, vararg bullets: String) = Item(label = label, bullets = bullets.toList())

    val all = listOf(
        Template("Collaboration wheel", "Wheel with six factors and callouts") {
            ModelDoc(
                title = "Model of interprofessional collaboration",
                center = "Interprofessional collaboration",
                layout = LayoutType.WHEEL,
                items = listOf(
                    i("Organisational support", "Team members are reachable when needed", "Support from management and decision-makers"),
                    i("Shared decision-making", "Active participation in therapy decisions", "Changes based on observations of the patient"),
                    i("Roles & communication", "Roles and tasks of each profession are clear", "Mutual trust between professions", "Clear communication channels"),
                    i("Patient involvement", "Patient takes part in decisions", "Understanding of therapy is checked regularly"),
                    i("Education & development", "Continuous learning within the team", "Protocols linked to training"),
                    i("Interdisciplinary focus", "Better treatment through joint work", "Patient receives consistent information"),
                ),
                legend = "Add a short explanation of abbreviations or the source of the model here.",
            )
        },
        Template("Socio-ecological model", "Concentric levels of influence") {
            ModelDoc(
                title = "Socio-ecological model",
                center = "Individual",
                layout = LayoutType.ONION,
                items = listOf(
                    i("Interpersonal", "Family, friends, peers"),
                    i("Organisational", "Schools, workplaces, health services"),
                    i("Community", "Relationships between organisations", "Local norms and resources"),
                    i("Public policy", "National and local laws", "Health policy and funding"),
                ),
            )
        },
        Template("Research process", "Five-step process with descriptions") {
            ModelDoc(
                title = "Research design",
                center = "Mixed-methods study",
                layout = LayoutType.PROCESS,
                items = listOf(
                    i("Literature review", "Systematic search", "Identify research gap"),
                    i("Study design", "Research questions", "Sample and instruments"),
                    i("Data collection", "Survey", "Semi-structured interviews"),
                    i("Analysis", "Statistical analysis", "Thematic analysis"),
                    i("Dissemination", "Thesis", "Article and conference"),
                ),
            )
        },
        Template("PDCA cycle", "Plan–Do–Check–Act quality cycle") {
            ModelDoc(
                title = "PDCA cycle",
                center = "Continuous improvement",
                layout = LayoutType.CYCLE,
                style = Style(theme = "soft"),
                items = listOf(
                    i("Plan", "Define the problem", "Set objectives"),
                    i("Do", "Implement on a small scale"),
                    i("Check", "Measure and compare results"),
                    i("Act", "Standardise or adjust"),
                ),
            )
        },
        Template("Blank", "Start from scratch") {
            ModelDoc(
                title = "New model",
                center = "Core concept",
                items = listOf(i("First item"), i("Second item"), i("Third item")),
            )
        },
    )
}
