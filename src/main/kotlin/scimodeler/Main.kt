package scimodeler

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.KeyShortcut
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import scimodeler.core.Templates
import scimodeler.ui.Actions
import scimodeler.ui.App
import scimodeler.ui.EditorState
import scimodeler.ui.FileOps
import java.io.File

private fun shortcut(key: Key, shift: Boolean = false) =
    KeyShortcut(key, ctrl = !FileOps.isMac, meta = FileOps.isMac, shift = shift)

fun main(args: Array<String>) {
    System.setProperty("apple.awt.application.name", "SciModeler")
    application {
        val state = remember { EditorState(Templates.all.first().doc()) }
        val actionsRef = remember { mutableStateOf<Actions?>(null) }
        val windowState = rememberWindowState(width = 1480.dp, height = 920.dp, position = WindowPosition(Alignment.Center))

        Window(
            onCloseRequest = {
                val a = actionsRef.value
                if (a == null) exitApplication() else a.guard { exitApplication() }
            },
            state = windowState,
            title = "${state.displayName}${if (state.dirty) " •" else ""} — SciModeler",
        ) {
            val scope = rememberCoroutineScope()
            val snackbar = remember { SnackbarHostState() }
            val actions = remember { Actions(state, window, scope, snackbar) }
            SideEffect { actionsRef.value = actions }
            LaunchedEffect(Unit) {
                args.firstOrNull()?.let { File(it) }?.takeIf { it.isFile }?.let { actions.openFile(it) }
            }

            MenuBar {
                Menu("File", mnemonic = 'F') {
                    Menu("New from template") {
                        Templates.all.forEach { t ->
                            Item(t.name, onClick = { actions.guard { actions.newFrom(t) } })
                        }
                    }
                    Item("Open…", shortcut = shortcut(Key.O), onClick = { actions.guard { actions.open() } })
                    Separator()
                    Item("Save", shortcut = shortcut(Key.S), onClick = { actions.save() })
                    Item("Save as…", shortcut = shortcut(Key.S, shift = true), onClick = { actions.saveAs() })
                    Separator()
                    Item("Export PNG…", shortcut = shortcut(Key.E), onClick = { actions.exportPng() })
                    Item("Export SVG…", shortcut = shortcut(Key.E, shift = true), onClick = { actions.exportSvg() })
                    Item("Copy image", shortcut = shortcut(Key.C, shift = true), onClick = { actions.copyImage() })
                }
                Menu("Edit", mnemonic = 'E') {
                    Item("Undo", enabled = state.canUndo, shortcut = shortcut(Key.Z), onClick = { state.undo() })
                    Item("Redo", enabled = state.canRedo, shortcut = shortcut(Key.Z, shift = true), onClick = { state.redo() })
                }
            }

            App(state, actions)
        }
    }
}
