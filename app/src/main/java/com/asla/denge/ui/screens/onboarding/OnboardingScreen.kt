package com.asla.denge.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asla.denge.R
import com.asla.denge.domain.repository.AuthRepository
import com.asla.denge.domain.repository.GenreRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Editorial Anime/Manga Onboarding Flow (v2.0.4).
 * - Image merges seamlessly into the background without box borders
 * - Suggestions chip cleanly wraps whole name block without word splitting
 * - Floating dialog ("kotak melayang") for custom genre setup like v1.5.6
 * - Fixed persistent bottom button raised comfortably above system navigation bar
 * - Mandatory setup with zero skip
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    authRepository: AuthRepository = koinInject(),
    genreRepository: GenreRepository = koinInject(),
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var listenerName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }

    val genres by genreRepository.getGenresFlow().collectAsState(initial = emptyList())
    val selectedGenreCount = genres.count { it.isSelected }

    fun finishOnboarding() {
        if (isSubmitting) return
        isSubmitting = true
        coroutineScope.launch {
            val finalName = listenerName.trim().ifBlank { "Music Lover" }
            authRepository.setUserName(finalName)
            genreRepository.setCompletedOnboarding(true)
            onComplete()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                // Step Content Area (Smooth slide transitions)
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(tween(260)) { width -> width } + fadeIn(tween(260)))
                                .togetherWith(slideOutHorizontally(tween(220)) { width -> -width } + fadeOut(tween(220)))
                        } else {
                            (slideInHorizontally(tween(260)) { width -> -width } + fadeIn(tween(260)))
                                .togetherWith(slideOutHorizontally(tween(220)) { width -> width } + fadeOut(tween(220)))
                        }
                    },
                    label = "onboardingStepTransition",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) { step ->
                    when (step) {
                        0 -> EditorialGreetingStep()
                        1 -> ProfileNameStep(
                            name = listenerName,
                            onNameChange = { listenerName = it },
                            onDone = {
                                if (listenerName.trim().isNotBlank()) {
                                    currentStep = 2
                                }
                            }
                        )
                        2 -> CoffeeGenreTasteStep(
                            genres = genres,
                            onToggleGenre = { id -> genreRepository.toggleGenreSelection(id) },
                            onAddCustomGenre = { name, query -> genreRepository.addCustomGenre(name, query) }
                        )
                    }
                }

                // Elevated Pinned Bottom Section (Raised button with stepper pills placed underneath)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 6.dp, bottom = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val isButtonEnabled = when (currentStep) {
                        0 -> true
                        1 -> listenerName.trim().isNotBlank()
                        2 -> selectedGenreCount >= 1
                        else -> true
                    }

                    Button(
                        onClick = {
                            when (currentStep) {
                                0 -> currentStep = 1
                                1 -> currentStep = 2
                                2 -> finishOnboarding()
                            }
                        },
                        enabled = isButtonEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (currentStep) {
                                    0 -> "Get Started"
                                    1 -> "Continue"
                                    else -> "Start Listening"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3-Step Pill Stepper (Smoothly reflects step progress below action button)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(3) { index ->
                            val isActive = index == currentStep
                            val isPassed = index < currentStep
                            Box(
                                modifier = Modifier
                                    .height(5.dp)
                                    .width(if (isActive) 24.dp else 7.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            isActive -> MaterialTheme.colorScheme.primary
                                            isPassed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            // Floating Back Button Overlay (Smooth scale & fade animation matching scroll-to-top button)
            AnimatedVisibility(
                visible = currentStep > 0,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 8.dp)
                    .align(Alignment.TopStart),
            ) {
                FloatingActionButton(
                    onClick = { currentStep -= 1 },
                    containerColor = Color(0xFF2C1D17),
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp,
                        focusedElevation = 2.dp,
                        hoveredElevation = 2.dp
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .border(
                            1.dp,
                            Color(0xFF5A3E31).copy(alpha = 0.6f),
                            CircleShape
                        ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Step 0: Prominent Anime/Manga Art seamlessly merged into the background.
 */
@Composable
private fun EditorialGreetingStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Seamless Anime/Manga Artwork without any enclosing card box or hard clipping
        Image(
            painter = painterResource(id = R.drawable.onboarding_anime_art),
            contentDescription = "Déngé Anime Music Artwork",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Editorial Headline & Poetic Typography
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = "Where words fail,",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                ),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "music speaks true.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondary, // Warm caramel roast
                )

                // Hand-drawn brush underline curve
                Canvas(modifier = Modifier.size(width = 160.dp, height = 7.dp)) {
                    val path = Path().apply {
                        moveTo(4.dp.toPx(), 3.5.dp.toPx())
                        quadraticTo(
                            size.width / 2f, 6.dp.toPx(),
                            size.width - 4.dp.toPx(), 2.dp.toPx()
                        )
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF73513C),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ad-free streaming, peaceful melodies, and personal rhythms tuned for your everyday serenity.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Step 1: Personalize Listener Profile Name (Mandatory).
 * Suggestions wrap cleanly as whole blocks with FlowRow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileNameStep(
    name: String,
    onNameChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val suggestions = listOf(
        "Alex", "Coffee Lover", "Aria", "Night Owl", "Melody",
        "Serenade", "Echo", "Lofi Chill", "Velvet", "Daydreamer"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(84.dp))

        // Profile Avatar Circle with dark espresso background and bright icon/letter
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center,
        ) {
            val initial = name.trim().take(1).uppercase()
            if (initial.isNotBlank()) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your listener name to personalize your greetings and library.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { onNameChange(it.take(25)) },
            placeholder = {
                Text(
                    text = "e.g. Asla, Alex, or Coffee Lover",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingIcon = {
                if (name.isNotBlank()) {
                    IconButton(onClick = { onNameChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onDone()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick suggestions in FlowRow so chips wrap as complete unbroken blocks
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onNameChange(suggestion) },
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * Step 2: Musical Taste & Genre Selection.
 * Uses floating popup dialog ("kotak melayang") like v1.5.6 for adding custom genres.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoffeeGenreTasteStep(
    genres: List<com.asla.denge.domain.model.MusicGenre>,
    onToggleGenre: (String) -> Unit,
    onAddCustomGenre: (String, String) -> Unit,
) {
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var customGenreName by remember { mutableStateOf("") }
    var customSearchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Floating Custom Genre Dialog (Matching v1.5.6 aesthetic)
    if (showAddCustomDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCustomDialog = false
                customGenreName = ""
                customSearchQuery = ""
            },
            title = {
                Text(
                    text = "Add Custom Genre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a genre name, artist, or music vibe (e.g. Indie Pop, Acoustic, Jazz, Lofi)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Genre Name",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customGenreName,
                        onValueChange = { customGenreName = it },
                        placeholder = { Text("e.g. Indie Pop / Acoustic") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Search Keywords (Optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customSearchQuery,
                        onValueChange = { customSearchQuery = it },
                        placeholder = { Text("e.g. acoustic guitar songs, indie mix") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customGenreName.isNotBlank()) {
                            onAddCustomGenre(
                                customGenreName.trim(),
                                customSearchQuery.trim().ifBlank { customGenreName.trim() }
                            )
                            customGenreName = ""
                            customSearchQuery = ""
                            showAddCustomDialog = false
                            focusManager.clearFocus()
                        }
                    },
                    enabled = customGenreName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCustomDialog = false
                    customGenreName = ""
                    customSearchQuery = ""
                    focusManager.clearFocus()
                }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(72.dp))

        Text(
            text = "Pick Your Favorite Vibes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Select genres to tailor your home feed. You can change this anytime in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Floating Dialog Trigger Button ("Add Custom Genre")
        Button(
            onClick = { showAddCustomDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Custom Genre",
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Genre Chips Flow (Warm Coffee Palette, Check icon when selected, sized like search shortcuts)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            genres.forEach { genre ->
                val isSelected = genre.isSelected
                Surface(
                    onClick = { onToggleGenre(genre.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = genre.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
