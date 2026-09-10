package cn.mine.minestars.ui.pages.workspace

import android.graphics.Typeface
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import cn.mine.minestars.data.db.entity.WorkspaceEntity
import cn.mine.minestars.data.repository.WorkspaceRepository
import org.koin.compose.koinInject

@Composable
fun WorkspaceTerminalPage(id: String) {
    val repository: WorkspaceRepository = koinInject()
    var workspace by remember { mutableStateOf<WorkspaceEntity?>(null) }

    LaunchedEffect(id) {
        workspace = repository.getWorkspace(id)
    }

    WorkspaceTerminalContent(
        root = workspace?.root,
        contentPadding = PaddingValues(),
    )
}

@Composable
private fun WorkspaceTerminalContent(
    root: String?,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val terminalTextSizePx = with(LocalDensity.current) { 13.sp.toPx().toInt() }
    val terminalTypeface = Typeface.MONOSPACE
    var finished by remember(root) { mutableStateOf(false) }
    var controlDown by remember(root) { mutableStateOf(false) }
    var altDown by remember(root) { mutableStateOf(false) }
    val sessionClient = remember(root) {
        WorkspaceTerminalSessionClient(context.applicationContext) {
            finished = true
        }
    }
    val viewClient = remember(root) {
        WorkspaceTerminalViewClient(context)
    }
    viewClient.controlDown = controlDown
    viewClient.altDown = altDown

    if (root == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "工作区加载中...",
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        return
    }

    val sessionState by produceState<TerminalSessionUiState>(
        initialValue = TerminalSessionUiState.Loading,
        root,
        sessionClient,
    ) {
        value = if (!workspaceRootfsReady(context, root)) {
            TerminalSessionUiState.NotInstalled
        } else {
            withContext(Dispatchers.IO) {
                prepareWorkspaceTerminalSession(context, root)
            }
            if (!isActive) return@produceState
            val session = createWorkspaceTerminalSession(context, root, sessionClient)
            if (!isActive) {
                session.finishIfRunning()
                return@produceState
            }
            TerminalSessionUiState.Ready(session)
        }
    }

    if (sessionState !is TerminalSessionUiState.Ready) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (sessionState is TerminalSessionUiState.NotInstalled) {
                    "请先安装 Rootfs"
                } else {
                    "启动中..."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        return
    }
    val session = (sessionState as TerminalSessionUiState.Ready).session

    DisposableEffect(session) {
        onDispose {
            sessionClient.terminalView = null
            viewClient.terminalView = null
            session.finishIfRunning()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        color = Color.Black,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        TerminalView(viewContext, null).apply {
                            isFocusable = true
                            isFocusableInTouchMode = true
                            setTextSize(terminalTextSizePx)
                            setTypeface(terminalTypeface)
                            setTerminalViewClient(viewClient)
                            attachSession(session)
                            sessionClient.terminalView = this
                            viewClient.terminalView = this
                            setOnTouchListener { _, event ->
                                if (event.action == MotionEvent.ACTION_UP) {
                                    viewClient.focusAndShowKeyboard()
                                }
                                false
                            }
                            post {
                                viewClient.focusAndShowKeyboard()
                            }
                        }
                    },
                    update = { terminalView ->
                        terminalView.isFocusable = true
                        terminalView.isFocusableInTouchMode = true
                        terminalView.setTextSize(terminalTextSizePx)
                        terminalView.setTypeface(terminalTypeface)
                        terminalView.setTerminalViewClient(viewClient)
                        sessionClient.terminalView = terminalView
                        viewClient.terminalView = terminalView
                        terminalView.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                viewClient.focusAndShowKeyboard()
                            }
                            false
                        }
                        terminalView.attachSession(session)
                        terminalView.onScreenUpdated()
                    },
                )
                if (finished) {
                    Text(
                        text = "终端已退出",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
            TerminalExtraKeysBar(
                controlDown = controlDown,
                altDown = altDown,
                onControlToggle = { controlDown = !controlDown },
                onAltToggle = { altDown = !altDown },
                onSendText = { session.writeText(it) },
            )
        }
    }
}

@Composable
private fun TerminalExtraKeysBar(
    controlDown: Boolean,
    altDown: Boolean,
    onControlToggle: () -> Unit,
    onAltToggle: () -> Unit,
    onSendText: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2D2D))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TerminalExtraKey("ESC") { onSendText("") }
        TerminalExtraKey("TAB") { onSendText("\t") }
        TerminalExtraKey("CTRL", selected = controlDown, onClick = onControlToggle)
        TerminalExtraKey("ALT", selected = altDown, onClick = onAltToggle)
        TerminalExtraKey("-") { onSendText("-") }
        TerminalExtraKey("/") { onSendText("/") }
        TerminalExtraKey("|") { onSendText("|") }
        TerminalExtraKey("←") { onSendText("[D") }
        TerminalExtraKey("↓") { onSendText("[B") }
        TerminalExtraKey("↑") { onSendText("[A") }
        TerminalExtraKey("→") { onSendText("[C") }
        TerminalExtraKey("HOME") { onSendText("[H") }
        TerminalExtraKey("END") { onSendText("[F") }
    }
}

@Composable
private fun TerminalExtraKey(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .background(
                color = if (selected) {
                    Color(0xFF4FC3F7)
                } else {
                    Color.White.copy(alpha = 0.12f)
                },
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) {
            Color.Black
        } else {
            Color.White.copy(alpha = 0.9f)
        },
    )
}

private fun TerminalSession.writeText(text: String) {
    val bytes = text.toByteArray()
    write(bytes, 0, bytes.size)
}

private sealed interface TerminalSessionUiState {
    data object Loading : TerminalSessionUiState
    data object NotInstalled : TerminalSessionUiState
    data class Ready(val session: TerminalSession) : TerminalSessionUiState
}
