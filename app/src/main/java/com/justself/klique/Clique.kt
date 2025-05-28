package com.justself.klique

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Groups2
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.justself.klique.ContactsBlock.Contacts.repository.ContactsRepository
import com.justself.klique.ContactsBlock.Contacts.ui.CheckContactsPermission
import com.justself.klique.ContactsBlock.Contacts.ui.ContactsViewModel
import com.justself.klique.MyKliqueApp.Companion.appContext

@Composable
fun CliqueScreen(navController: NavController, defaultScreen: CliqueScreenState = CliqueScreenState.REQUESTS) {
    val viewModel = CliqueScreenObject
    var presentScreen by remember { mutableStateOf(defaultScreen) }
    var displayWarningDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.resetUncheckedCount()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        when (presentScreen) {
            CliqueScreenState.REQUESTS -> {
                RequestsComposable(navController)
                AddButton(
                    { presentScreen = CliqueScreenState.MY_CLIQUE },
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    icon = Icons.Default.Add,
                    "Add Contact As Clique"
                )
            }

            CliqueScreenState.MY_CLIQUE -> {
                MyCliqueComposable(
                    { presentScreen = CliqueScreenState.CONTACTS_SELECTION },
                    { presentScreen = CliqueScreenState.REQUESTS },
                    navController,
                    { displayWarningDialog = true }
                )
            }

            CliqueScreenState.CONTACTS_SELECTION -> {
                ContactSelector { presentScreen = CliqueScreenState.MY_CLIQUE }
            }
        }
        if (displayWarningDialog) {
            Dialog(onDismissRequest = { displayWarningDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Heads up!",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Sending a clique request to someone else will remove you from your current clique.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(16.dp))
                        Row {
                            Text(
                                "Never mind",
                                modifier = Modifier
                                    .clickable { displayWarningDialog = false }
                                    .background(
                                        MaterialTheme.colorScheme.secondary,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.background,
                                style = MaterialTheme.typography.displayLarge
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "Continue",
                                modifier = Modifier
                                    .clickable {
                                        presentScreen = CliqueScreenState.CONTACTS_SELECTION
                                        displayWarningDialog = false
                                    }
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.background,
                                style = MaterialTheme.typography.displayLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CliqueTileRequest(person: Clique, navController: NavController, onClick: (Int) -> Unit) {
    val profileImage = NetworkUtils.fixLocalHostUrl(person.profileImage)
    val size = 40.dp
    Row(modifier = Modifier
        .clickable { onClick(person.userId) }
        .padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier
            .size(size)
            .clickable { Screen.BioScreen.navigate(navController, person.userId) }) {
            ProfileAvatar(profileImage, size)
        }
        Text(
            person.name,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        VerifiedBadge(person.isVerified, MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun AcceptOrDeclineRow(onAcceptClick: () -> Unit, onDeclineClick: () -> Unit) {
    val padding = 10.dp
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(0.3f))
        Text("Accept", modifier = Modifier
            .clickable { onAcceptClick() }
            .padding(horizontal = padding))
        Text("Decline", modifier = Modifier
            .clickable { onDeclineClick() }
            .padding(horizontal = padding))
    }
}

@Composable
fun ProfileAvatar(
    profileImageUrl: String,
    size: Dp,
    ringWidth: Dp = 4.dp,
    ringColor: Color = MaterialTheme.colorScheme.onPrimary,
    fallbackColor: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition()
    val sweepStart by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart)
    )

    Box(Modifier.size(size)) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(color = fallbackColor, radius = size.toPx() / 2f)
        }
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(profileImageUrl)
                .crossfade(true)
                .build(),
            placeholder = ColorPainter(fallbackColor),    // optional
            error       = ColorPainter(fallbackColor)     // optional
        )

        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Canvas(Modifier.matchParentSize()) {
            val stroke = Stroke(width = ringWidth.toPx(), cap = StrokeCap.Round)
            val radius = size.toPx() / 2f - ringWidth.toPx() / 2f
            drawArc(
                color = ringColor,
                startAngle = sweepStart,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(size.toPx() / 2f - radius, size.toPx() / 2f - radius),
                size = Size(radius * 2, radius * 2),
                style = stroke
            )
        }
    }
}

@Composable
fun RequestsComposable(navController: NavController) {
    val cliqueScreenObject = CliqueScreenObject
    val cliqueMembersRequest by cliqueScreenObject.cliqueMembersRequest.collectAsState()
    var clickedUserId by remember { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row {
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Rounded.Groups2,
                contentDescription = "Clique",
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Text(
                "Clique Requests",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(16.dp)
            )
            Spacer(Modifier.weight(1f))
        }
        if (cliqueMembersRequest.isNotEmpty()) {
            LazyColumn {
                items(cliqueMembersRequest) { member ->
                    CliqueTileRequest(member, navController) {
                        clickedUserId = if (clickedUserId == member.userId) {
                            null
                        } else {
                            it
                        }
                    }
                    AnimatedVisibility(
                        visible = member.userId == clickedUserId,
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> -fullHeight / 2 }
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> -fullHeight / 2 }
                        ) + fadeOut()
                    ) {
                        AcceptOrDeclineRow(
                            onAcceptClick = {
                                cliqueScreenObject.acceptRequest(member)
                                clickedUserId = null
                            },
                            onDeclineClick = {
                                cliqueScreenObject.rejectRequest(member.userId)
                                clickedUserId = null
                            }
                        )
                    }
                }
            }
        } else {
            Text(
                "You have no pending clique requests, view your clique at the plus button below",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun MyCliqueComposable(
    onToggleSelectionScreen: () -> Unit,
    onBackToPresentScreen: () -> Unit,
    navController: NavController,
    ifHasCliqueMembers: () -> Unit
) {
    LaunchedEffect(Unit) {
        CliqueScreenObject.fetchMyCliqueMembers()
    }
    val cliqueScreenObject = CliqueScreenObject
    val myCliqueMembers by cliqueScreenObject.myCliqueMembers.collectAsState()
    if (myCliqueMembers == null) {
        LoadingScreen(onBackToPresentScreen = onBackToPresentScreen)
    } else if (myCliqueMembers!!.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Text(
                "You have no clique members yet, add with the plus button below",
                Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )
            AddButton(
                onToggleSelectionScreen,
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                icon = Icons.Default.Add,
                "Add Contact As Clique"
            )
            BackArrowButton(onBackToPresentScreen, Modifier.align(Alignment.TopStart))
        }
    } else {
        Column(Modifier.fillMaxWidth()) {
            BackArrowButton(onBackToPresentScreen, Modifier.align(Alignment.Start))
            LazyColumn {
                items(myCliqueMembers!!) { member ->
                    CliqueTile(member, navController)
                }
            }
            Text(
                "Join a different Clique",
                modifier = Modifier
                    .clickable { ifHasCliqueMembers() }
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .padding(5.dp)
                    .align(Alignment.End),
                color = MaterialTheme.colorScheme.background,
                style = MaterialTheme.typography.displayLarge
            )
        }
    }
}

@Composable
fun LoadingScreen(
    size: Dp = 100.dp,
    ringWidth: Dp = 4.dp,
    baseColor: Color = Color.Transparent,
    ringColor: Color = MaterialTheme.colorScheme.onPrimary,
    animationDurationMs: Int = 2000,
    onBackToPresentScreen: () -> Unit
) {
    val transition = rememberInfiniteTransition()
    val sweepStart by transition.animateFloat(
        0f, 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(size)
                .align(Alignment.Center)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val strokePx = ringWidth.toPx()
                val radiusPx = size.toPx() / 2f
                drawCircle(color = baseColor, radius = radiusPx)
                drawArc(
                    color = ringColor,
                    startAngle = sweepStart,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(strokePx / 2, strokePx / 2),
                    size = Size((radiusPx * 2) - strokePx, (radiusPx * 2) - strokePx),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        BackArrowButton(onBackToPresentScreen, Modifier.align(Alignment.TopStart))
    }
}

@Composable
fun BackArrowButton(onBackToPresentScreen: () -> Unit, modifier: Modifier) {
    IconButton(onClick = { onBackToPresentScreen() }, modifier) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

@Composable
fun ContactSelector(onBackToPresentScreen: () -> Unit) {
    val contactViewModel: ContactsViewModel = remember {
        ContactsViewModel(
            ContactsRepository(
                appContext.contentResolver,
                appContext
            )
        )
    }
    var hasContactsPermission by remember { mutableStateOf(false) }
    var myClique by remember { mutableIntStateOf(0) }
    CheckContactsPermission(
        onPermissionResult = { granted ->
            hasContactsPermission = granted
        },
        onPermissionGranted = {
            contactViewModel.updateContactFromHomeScreen(appContext)
        }
    )
    if (!hasContactsPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Contacts permission is required.")
        }
        return
    }
    val contacts by contactViewModel.contacts.collectAsState()
    val appUsers = contacts.filter {
        it.customerId != null
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BackArrowButton(onBackToPresentScreen, Modifier.align(Alignment.Start))
            Text("You can only select one contact as your clique")
            LazyColumn {
                items(appUsers) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val isNowChecked = myClique != user.customerId
                                myClique = if (isNowChecked) user.customerId!! else 0
                            }
                            .padding(16.dp),            // your choice of padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = (user.customerId == myClique),
                            onCheckedChange = { isChecked ->
                                myClique = if (isChecked) user.customerId!! else 0
                            }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            Text(
                "Send Request",
                modifier = Modifier
                    .clickable {
                        if (myClique > 0) {
                            CliqueScreenObject.sendCliqueRequest(myClique)
                        }
                        myClique = 0
                    }
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp)
                    .align(Alignment.End),
                color = MaterialTheme.colorScheme.background
            )
        }
    }
}

enum class CliqueScreenState(val title: String) {
    REQUESTS("requests"),
    MY_CLIQUE("myClique"),
    CONTACTS_SELECTION("contactsSelection");
    companion object{
        fun returnScreenState(name: String): CliqueScreenState? {
            return entries.find { it.title == name }
        }
    }
}