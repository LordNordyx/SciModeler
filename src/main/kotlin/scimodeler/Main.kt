package scimodeler

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import scimodeler.core.SessionStore
import scimodeler.core.Templates
import scimodeler.ui.Actions
import scimodeler.ui.AppRoot
import scimodeler.ui.AwtDialogs
import scimodeler.ui.EditorState
import scimodeler.ui.FileOps
import scimodeler.ui.UiState
import java.awt.Dimension
import java.io.File

private fun shortcut(key: Key, shift: Boolean = false) =
    KeyShortcut(key, ctrl = !FileOps.isMac, meta = FileOps.isMac, shift = shift)

/** Restores the last session, or starts from the first template. */
fun initialState(): EditorState {
    val session = SessionStore.load()
    return if (session != null) EditorState(session.doc).also {
        it.load(session.doc, session.file?.takeIf { f -> f.isFile })
        it.dirty = session.dirty
    } else EditorState(Templates.all.first().doc())
}

fun main(args: Array<String>) {
    System.setProperty("apple.awt.application.name", "SciModeler")
    System.setProperty("apple.awt.application.appearance", "system")
    application {
        val state = remember { initialState() }
        val ui = remember { UiState() }
        val actionsRef = remember { mutableStateOf<Actions?>(null) }
        val windowState = rememberWindowState(width = 1500.dp, height = 940.dp, position = WindowPosition(Alignment.Center))

        Window(
            onCloseRequest = {
                val a = actionsRef.value
                a?.saveSession()
                // Untitled work is kept in the session, so only saved files need the prompt.
                if (a == null || state.file == null) exitApplication() else a.guard { exitApplication() }
            },
            state = windowState,
            title = "${state.displayName}${if (state.dirty) " •" else ""} — SciModeler",
            icon = painterResource("icon.png"),
            onPreviewKeyEvent = { e ->
                e.type == KeyEventType.KeyDown && e.key == Key.Escape && ui.dismissTop(actionsRef.value)
            },
        ) {
            LaunchedEffect(Unit) { window.minimumSize = Dimension(1180, 720) }
            val actions = actionsRef.value
            MenuBar {
                Menu("File", mnemonic = 'F') {
                    Item("New from template…", shortcut = shortcut(Key.N), onClick = { ui.templatesOpen = true })
                    Item("Open…", shortcut = shortcut(Key.O), onClick = { actions?.guard { actions.open() } })
                    Separator()
                    Item("Save", shortcut = shortcut(Key.S), onClick = { actions?.save() })
                    Item("Save as…", shortcut = shortcut(Key.S, shift = true), onClick = { actions?.saveAs() })
                    Separator()
                    Item("Export PNG…", shortcut = shortcut(Key.E), onClick = { actions?.exportPng() })
                    Item("Export SVG…", shortcut = shortcut(Key.E, shift = true), onClick = { actions?.exportSvg() })
                    Item("Copy image", shortcut = shortcut(Key.C, shift = true), onClick = { actions?.copyImage() })
                }
                Menu("Edit", mnemonic = 'E') {
                    Item("Undo", enabled = state.canUndo, shortcut = shortcut(Key.Z), onClick = { state.undo() })
                    Item("Redo", enabled = state.canRedo, shortcut = shortcut(Key.Z, shift = true), onClick = { state.redo() })
                }
            }
            AppRoot(state, AwtDialogs(window), ui) { a ->
                actionsRef.value = a
                args.firstOrNull()?.let { File(it) }?.takeIf { it.isFile }?.let { a.openFile(it) }
            }
        }
    }
}
