package com.example.moneypad.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import com.example.moneypad.utils.HtmlConverter.parseHtmlToAnnotatedString
import com.example.moneypad.utils.HtmlConverter.toHtmlString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritePartScreen(
    storyId: String,
    partId: String? = null,
    onNavigateBack: () -> Unit,
    onPartSaved: () -> Unit,
    viewModel: StoryViewModel,
    navController: androidx.navigation.NavController? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val parts by viewModel.currentParts.collectAsState()
    val partToEdit = parts.find { it.id == partId }

    var title by remember(partToEdit) { mutableStateOf(partToEdit?.title ?: "") }
    var content by remember(partToEdit) {
        mutableStateOf(TextFieldValue(annotatedString = (partToEdit?.content ?: "").parseHtmlToAnnotatedString()))
    }
    var headerImageUri by remember(partToEdit) { 
        mutableStateOf<android.net.Uri?>(if (partToEdit?.headerImageUrl != null) android.net.Uri.parse(partToEdit.headerImageUrl) else null) 
    }
    var isPublished by remember(partToEdit) { mutableStateOf(partToEdit?.isPublished ?: false) }
    var expanded by remember { mutableStateOf(false) }
    var showSaveDraftDialog by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Auto-scroll to cursor when selection changes
    LaunchedEffect(content.selection, content.text) {
        textLayoutResult?.let { layout ->
            val cursor = content.selection.start
            if (cursor >= 0 && cursor <= content.text.length) {
                val cursorRect = layout.getCursorRect(cursor)
                bringIntoViewRequester.bringIntoView(cursorRect)
            }
        }
    }

    // Track last saved state to avoid showing "Save as Draft" dialog after publishing/saving
    var lastSavedTitle by remember(partToEdit) { mutableStateOf(partToEdit?.title ?: "") }
    var lastSavedContent by remember(partToEdit) { mutableStateOf(partToEdit?.content ?: "") }

    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    val hasChanges = remember(title, content.annotatedString, lastSavedTitle, lastSavedContent) {
        title != lastSavedTitle || content.annotatedString.toHtmlString() != lastSavedContent
    }

    // Save as draft and go back
    fun saveDraftAndExit() {
        viewModel.savePartAsDraft(storyId, title, content.annotatedString.toHtmlString(), partId)
        onNavigateBack()
    }

    // Intercept the system back gesture/button
    BackHandler {
        if (hasChanges && (title.isNotBlank() || content.annotatedString.text.isNotBlank())) {
            showSaveDraftDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Save-draft confirmation dialog
    if (showSaveDraftDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDraftDialog = false },
            title = { Text("Save as Draft?") },
            text = { Text("Do you want to save your chapter as a draft before leaving?") },
            confirmButton = {
                Button(onClick = {
                    showSaveDraftDialog = false
                    saveDraftAndExit()
                }) {
                    Text("Save Draft")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDraftDialog = false
                    onNavigateBack()
                }) {
                    Text("Discard")
                }
            }
        )
    }

    var activeStyles by remember { mutableStateOf(setOf<SpanStyle>()) }
    var overrideStyles by remember { mutableStateOf(false) }

    val headerMediaLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            headerImageUri = uri
        }
    }

    fun toggleStyle(style: SpanStyle) {
        val selection = content.selection
        if (!selection.collapsed) {
            val newAnnotatedString = AnnotatedString.Builder(content.annotatedString).apply {
                addStyle(style, selection.start, selection.end)
            }.toAnnotatedString()
            content = content.copy(annotatedString = newAnnotatedString)
        } else {
            activeStyles = if (activeStyles.contains(style)) {
                activeStyles - style
            } else {
                activeStyles + style
            }
            overrideStyles = true
        }
    }

    fun applyFont(fontFamily: FontFamily) {
        val style = SpanStyle(fontFamily = fontFamily)
        val selection = content.selection
        if (!selection.collapsed) {
            val newAnnotatedString = AnnotatedString.Builder(content.annotatedString).apply {
                addStyle(style, selection.start, selection.end)
            }.toAnnotatedString()
            content = content.copy(annotatedString = newAnnotatedString)
        } else {
            activeStyles = activeStyles.filter { it.fontFamily == null }.toSet() + style
            overrideStyles = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges && (title.isNotBlank() || content.text.isNotBlank())) {
                            showSaveDraftDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.annotatedString.text.isNotBlank()) {
                                if (isPublished) {
                                    viewModel.updatePartStatus(storyId, partId ?: "", title, content.annotatedString.toHtmlString(), false)
                                    isPublished = false
                                    lastSavedTitle = title
                                    lastSavedContent = content.annotatedString.toHtmlString()
                                    android.widget.Toast.makeText(context, "Part unpublished", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addPartToStory(storyId, title, content.annotatedString.toHtmlString(), partId, true)
                                    isPublished = true
                                    lastSavedTitle = title
                                    lastSavedContent = content.annotatedString.toHtmlString()
                                    android.widget.Toast.makeText(context, "Part published", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(36.dp)
                    ) {
                        Text(if (isPublished) "Unpublish" else "Publish", fontSize = 14.sp)
                    }

                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save as Draft") },
                                onClick = {
                                    expanded = false
                                    saveDraftAndExit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View as Reader's POV") },
                                onClick = {
                                    expanded = false
                                    if (partId != null) {
                                        navController?.navigate("read/$storyId/$partId")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    if (partId != null) {
                                        viewModel.deletePart(partId)
                                        onNavigateBack()
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(top = innerPadding.calculateTopPadding())
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header Media Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { headerMediaLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (headerImageUri != null) {
                        coil.compose.AsyncImage(
                            model = headerImageUri,
                            contentDescription = "Header Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "Add Image",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                                Icon(
                                    Icons.Default.OndemandVideo,
                                    contentDescription = "Add Video",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to add Header Media", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Chapter Title
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            "Chapter Title",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.LightGray
                    ),
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Body Writing Area
                androidx.compose.foundation.text.BasicTextField(
                    value = content,
                    onValueChange = { newValue: TextFieldValue ->
                        val oldText = content.text
                        val newText = newValue.text

                        if (oldText == newText) {
                            content = newValue.copy(annotatedString = content.annotatedString)
                            if (content.selection != newValue.selection) {
                                if (overrideStyles) {
                                    overrideStyles = false
                                } else {
                                    if (newValue.selection.collapsed) {
                                        val cursor = newValue.selection.start
                                        if (cursor > 0) {
                                            val stylesAtCursor = content.annotatedString.spanStyles
                                                .filter { it.start < cursor && it.end >= cursor }
                                                .map { it.item }
                                                .toSet()
                                            activeStyles = stylesAtCursor
                                        } else {
                                            activeStyles = emptySet()
                                        }
                                    } else {
                                        val start = newValue.selection.start
                                        val end = newValue.selection.end
                                        if (start < end) {
                                            val stylesAtSelection = content.annotatedString.spanStyles
                                                .filter { it.start < end && it.end > start }
                                                .map { it.item }
                                                .toSet()
                                            activeStyles = stylesAtSelection
                                        }
                                    }
                                }
                            }
                        } else {
                            var prefixLen = 0
                            while (prefixLen < oldText.length && prefixLen < newText.length && oldText[prefixLen] == newText[prefixLen]) {
                                prefixLen++
                            }
                            var suffixLen = 0
                            while (suffixLen < oldText.length - prefixLen && suffixLen < newText.length - prefixLen && oldText[oldText.length - 1 - suffixLen] == newText[newText.length - 1 - suffixLen]) {
                                suffixLen++
                            }

                            val replaceStart = prefixLen
                            val replaceEndOld = oldText.length - suffixLen
                            val replaceEndNew = newText.length - suffixLen

                            val builder = AnnotatedString.Builder(newText)

                            content.annotatedString.spanStyles.forEach { span ->
                                val s = span.start
                                val e = span.end

                                if (e <= replaceStart) {
                                    builder.addStyle(span.item, s, e)
                                } else if (s >= replaceEndOld) {
                                    builder.addStyle(span.item, s + (replaceEndNew - replaceEndOld), e + (replaceEndNew - replaceEndOld))
                                } else {
                                    if (s < replaceStart) {
                                        builder.addStyle(span.item, s, replaceStart)
                                    }
                                    if (e > replaceEndOld) {
                                        builder.addStyle(span.item, replaceEndNew, e + (replaceEndNew - replaceEndOld))
                                    }
                                }
                            }

                            if (replaceEndNew > replaceStart && activeStyles.isNotEmpty()) {
                                activeStyles.forEach { style ->
                                    builder.addStyle(style, replaceStart, replaceEndNew)
                                }
                            }

                            content = newValue.copy(annotatedString = builder.toAnnotatedString())
                            overrideStyles = false
                        }
                    },
                    onTextLayout = { result -> textLayoutResult = result },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = textColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (content.text.isEmpty()) {
                                Text(
                                    "Tap here to start writing...",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                            }
                            innerTextField()
                            
                            textLayoutResult?.let { layout ->
                                content.annotatedString.getStringAnnotations("IMAGE", 0, content.text.length).forEach { annotation ->
                                    val start = annotation.start
                                    if (start < content.text.length) {
                                        val boundingBox = layout.getBoundingBox(start)
                                        coil.compose.AsyncImage(
                                            model = annotation.item,
                                            contentDescription = "Inline Image",
                                            modifier = Modifier
                                                .absoluteOffset(
                                                    x = 0.dp, 
                                                    y = with(androidx.compose.ui.platform.LocalDensity.current) { boundingBox.top.toDp() }
                                                )
                                                .height(with(androidx.compose.ui.platform.LocalDensity.current) { boundingBox.height.toDp() })
                                                .fillMaxWidth(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }

            // Formatting Toolbar (Moved inside content column)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(onClick = { showFontMenu = true }) {
                            Icon(Icons.Default.FormatSize, contentDescription = "Font Style")
                        }
                        DropdownMenu(
                            expanded = showFontMenu,
                            onDismissRequest = { showFontMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Default") },
                                onClick = { showFontMenu = false; applyFont(FontFamily.Default) }
                            )
                            DropdownMenuItem(
                                text = { Text("Serif") },
                                onClick = { showFontMenu = false; applyFont(FontFamily.Serif) }
                            )
                            DropdownMenuItem(
                                text = { Text("Monospace") },
                                onClick = { showFontMenu = false; applyFont(FontFamily.Monospace) }
                            )
                            DropdownMenuItem(
                                text = { Text("Cursive") },
                                onClick = { showFontMenu = false; applyFont(FontFamily.Cursive) }
                            )
                        }
                    }

                    val isBold = activeStyles.any { it.fontWeight == FontWeight.Bold }
                    IconToggleButton(
                        checked = isBold,
                        onCheckedChange = { toggleStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                    }

                    val isItalic = activeStyles.any { it.fontStyle == FontStyle.Italic }
                    IconToggleButton(
                        checked = isItalic,
                        onCheckedChange = { toggleStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                    }

                    val isUnderlined = activeStyles.any { it.textDecoration == TextDecoration.Underline }
                    IconToggleButton(
                        checked = isUnderlined,
                        onCheckedChange = { toggleStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
                    }

                    val isHighlighted = activeStyles.any { it.background == androidx.compose.ui.graphics.Color.Yellow }
                    IconToggleButton(
                        checked = isHighlighted,
                        onCheckedChange = { toggleStyle(SpanStyle(background = androidx.compose.ui.graphics.Color.Yellow)) },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Highlight, contentDescription = "Highlight")
                    }

                    IconButton(onClick = {
                        val cursor = content.selection.start
                        val builder = AnnotatedString.Builder()
                        builder.append(content.annotatedString.subSequence(0, cursor))
                        builder.append("♡")
                        builder.append(content.annotatedString.subSequence(content.selection.end, content.annotatedString.length))
                        content = content.copy(
                            annotatedString = builder.toAnnotatedString(),
                            selection = androidx.compose.ui.text.TextRange(cursor + 1)
                        )
                    }) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Like")
                    }
                }
            }
        }
    }
}
