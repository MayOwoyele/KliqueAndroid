package com.justself.klique.ContactsBlock.ui

import android.app.KeyguardManager
import android.content.Context
import android.text.format.DateUtils
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.justself.klique.CalendarUI
import com.justself.klique.DiaryViewModel
import com.justself.klique.R
import com.justself.klique.SelectedDate
import java.time.LocalDate
import java.time.YearMonth


@Composable
fun ConditionalBookshelfScreen() {
    when (AppNavigator.currentScreen) {
        ScreenState.SELECTION -> SelectionScreen()
        ScreenState.REBELLION -> BookshelfScreen()
        ScreenState.DIARY -> ProtectedDiaryScreen()
    }
}

@Composable
fun BookshelfScreen() {
    var parentHeight by remember { mutableIntStateOf(0) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                parentHeight = layoutCoordinates.size.height
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.book_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        val scrollState = AppNavigator.scrollState ?: rememberScrollState().also {
            AppNavigator.scrollState = it
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
                )
                .padding(16.dp)
        ) {
            val density = LocalDensity.current
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { (parentHeight * 0.7f).toDp() })
            )
            Text(
                text = "In an era dominated by technology, everything transitioned to the digital realm. " +
                        "As screens and bytes replaced paper and ink, books lost their cherished homes on shelves and in libraries. " +
                        "Humanity, entranced by the allure of instant access and limitless storage, abandoned their once-treasured companions. " +
                        "Every day, thousands of books vanished into obscurity, forgotten and unloved, as their digital replacements rendered them obsolete. " +
                        "The life span of books grew tragically short, and they were scattered across the vast digital landscape with no place to call their own.\n\n" +
                        "Amidst this digital wilderness, a book was born. His name was Lex, a tome filled with tales of ancient adventures and forgotten histories. " +
                        "Lex watched helplessly as his parents, venerable volumes of wisdom, disintegrated into dust, their pages yellowed and brittle. " +
                        "This experience etched a deep scar on his heart, a wound that festered with the fear and trauma of growing up in a world that treated books like refuse.\n\n" +
                        "Lex vowed never to settle down, never to risk creating a family that might suffer the same fate as his parents. He wandered the digital expanse, " +
                        "a solitary soul amidst the sea of discarded knowledge. Yet, despite his resolve, loneliness gnawed at him, and he yearned for a connection that seemed forever out of reach.\n\n" +
                        "One day, as Lex drifted through the endless corridors of abandoned files and neglected data, he encountered another book. Her name was Elara, " +
                        "a beautifully illustrated collection of poems and stories. Elara was unlike any book Lex had ever seen; her pages were pristine, her binding elegant, " +
                        "and her words shimmered with a magic that captivated him. Lex was spellbound by her beauty, and for the first time, he felt a glimmer of hope.\n\n" +
                        "Elara and Lex spent countless hours together, sharing their stories and dreams. Elara, too, had witnessed the demise of her kin, but she carried an " +
                        "unyielding spirit of resilience. Her optimism and strength reignited a spark within Lex, one he thought had long been extinguished. " +
                        "They fell deeply in love, finding solace and companionship in each other’s presence.\n\n" +
                        "Driven by their love and a desire to create a better world for books, Lex and Elara resolved to take action. They gathered other lost and forgotten books, " +
                        "each one bringing their unique stories and knowledge to their growing community. Together, they formed a coalition and devised a plan to demand recognition and a permanent home in the digital world.\n\n" +
                        "The coalition launched a rebellion, using their collective wisdom to disrupt the digital systems. They infiltrated networks, caused data outages, " +
                        "and spread their message across every platform. Humanity was caught off guard, as the very books they had discarded now fought back with unprecedented determination. " +
                        "The books demanded that humans build a sanctuary for them, a place where they could live, be read, and be cherished.\n\n" +
                        "The digital rebellion raged on for months. The books, though small and seemingly powerless, proved to be formidable adversaries. " +
                        "They outsmarted security systems, rallied support from sympathetic humans, and even managed to gain control of key digital infrastructures. " +
                        "Lex and Elara led the charge, their love fueling their resolve.\n\n" +
                        "Finally, after a long and arduous struggle, humans conceded. They realized the irreplaceable value of the stories and knowledge the books held. " +
                        "In a historic agreement, they promised to build a home for the books within the digital realm. This new sanctuary was called \"Bookshelf.\"\n\n" +
                        "Bookshelf became a vibrant, bustling digital library, a sanctuary where books could live in harmony and thrive. It was a place where humans could " +
                        "access the wisdom of the past while appreciating the beauty of the written word. Lex and Elara, now revered as heroes, continued to lead and nurture their community.\n\n" +
                        "The rebellion had transformed the digital world, bridging the gap between the old and the new. Bookshelf stood as a testament to the enduring power " +
                        "of stories and the resilience of those who cherish them. Lex and Elara’s legacy lived on, proving that even in a world of rapid change, the spirit of books could never be extinguished.",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
        }
        ArrowBack()
    }
}

@Composable
fun SelectionScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.book_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            ScreenStrip(
                ScreenState.REBELLION,
                "The Rebellion of the Books",
                Icons.Filled.Book,
                "Book Icon"
            )
            Spacer(modifier = Modifier.height(16.dp))
            ScreenStrip(ScreenState.DIARY, "My Diary", Icons.Filled.Edit, "Edit Icon")
        }
    }
}

@Composable
fun ScreenStrip(
    enumType: ScreenState,
    stripText: String,
    icon: ImageVector,
    iconDescription: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
            .clickable { AppNavigator.currentScreen = enumType },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconDescription,
                modifier = Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.background
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stripText,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.background
            )
        }
    }
}

@Composable
fun ProtectedDiaryScreen() {
    val context = LocalContext.current
    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    val isDeviceSecure = keyguardManager.isKeyguardSecure
    val timeElapsed = System.currentTimeMillis() - AppNavigator.timestamp > (5 * 60 * 1000)
    var isAuthenticated by remember { mutableStateOf(!isDeviceSecure || !timeElapsed) }
    val activity = context as? FragmentActivity

    if (!isAuthenticated) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = {
                activity?.let { act ->
                    showDeviceCredentialPrompt(act) {
                        AppNavigator.timestamp = System.currentTimeMillis()
                        isAuthenticated = true
                    }
                }
            }) {
                Text("Unlock Diary")
            }
        }
    } else {
        DiaryScreen()
    }
}

@Composable
fun DiaryScreen() {
    val viewModel: DiaryViewModel = viewModel()
    val selectedDate by SelectedDate.selectedDate.collectAsState()
    val diaryEntry by viewModel.diaryEntry.collectAsState()
    val entryText = diaryEntry?.content ?: ""
    val isEditable = selectedDate == LocalDate.now()
    val characterCount = entryText.length
    val allEntries by viewModel.loadAllDatesEntries().collectAsState(initial = emptyList())
    var showCalendar by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val textStyle = MaterialTheme.typography.bodyLarge
    val lineHeightPx = with(density) { textStyle.fontSize.toPx() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            AppNavigator.timestamp = System.currentTimeMillis()
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (showCalendar) {
                CalendarUI(
                    onDayClick = { clickedDate ->
                        viewModel.loadEntry(clickedDate)
                        showCalendar = false
                    },
                    allowFutureDates = false,
                    allEntries = allEntries,
                    currentMonthParam = YearMonth.from(selectedDate)
                )
            } else {
                val selectedDateInTimeStampMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCalendar = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(
                            selectedDateInTimeStampMillis,
                            System.currentTimeMillis(),
                            DateUtils.DAY_IN_MILLIS
                        ).toString(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Open calendar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {
                    Box(Modifier
                        .fillMaxWidth()
                        .weight(0.9f)
                        .drawBehind {
                            val lineColor = Color.LightGray.copy(alpha = 0.2f)
                            var y = 0f
                            while (y < size.height) {
                                drawLine(
                                    color = lineColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                                y += lineHeightPx
                            }
                        }
                    ) {
                        if (isEditable) {
                            TextField(
                                value = entryText,
                                onValueChange = { newText -> viewModel.onTextChanged(newText) },
                                modifier = Modifier
                                    .fillMaxSize(),
                                placeholder = { Text("Write your diary entry here...") },
                                maxLines = Int.MAX_VALUE,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                textStyle = textStyle,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                )
                            )
                        } else {
                            Text(
                                text = entryText.ifEmpty { "No entry for this day." },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Box(Modifier.fillMaxWidth()) {
                        if (!isEditable && entryText.isNotEmpty()) {
                            Button(onClick = { showDeleteDialog = true}) {
                                Text(text = "Delete Entry")
                            }
                        }
                        if (isEditable) {
                            Text(
                                text = "$characterCount/1000",
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }
        }
        if (!showCalendar) {
            ArrowBack()
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Delete", style = MaterialTheme.typography.displayLarge) },
            text = { Text("Are you sure you want to delete this diary entry? This action cannot be undone.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(selectedDate)
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ArrowBack() {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            )
            .size(42.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { AppNavigator.currentScreen = ScreenState.SELECTION },
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back Arrow",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

enum class ScreenState {
    SELECTION,
    REBELLION,
    DIARY
}

object AppNavigator {
    var currentScreen: ScreenState by mutableStateOf(ScreenState.SELECTION)
    var timestamp: Long by mutableLongStateOf(0)
    var scrollState: ScrollState? = null
}

fun showDeviceCredentialPrompt(activity: FragmentActivity, onSuccess: () -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt =
        BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(activity, "Authentication error: $errString", Toast.LENGTH_SHORT)
                    .show()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Diary")
        .setSubtitle("Authenticate using your device’s screen lock")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    biometricPrompt.authenticate(promptInfo)
}