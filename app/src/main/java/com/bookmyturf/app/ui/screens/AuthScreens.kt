package com.bookmyturf.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookmyturf.app.data.Sport
import com.bookmyturf.app.R
import com.bookmyturf.app.ui.AppViewModel
import com.bookmyturf.app.ui.AuthRole
import com.bookmyturf.app.ui.components.*
import com.bookmyturf.app.ui.theme.*
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(sessionReady: Boolean, onDone: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (shown) 1f else .86f, tween(380, easing = FastOutSlowInEasing), label = "splash")
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(300), label = "splashAlpha")
    LaunchedEffect(Unit) { shown = true }
    LaunchedEffect(sessionReady) { if (sessionReady) { delay(1_200); onDone() } }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF4920C8), PulsePurpleDark))
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(310.dp).background(Color.White.copy(.025f), CircleShape))
        Box(Modifier.size(220.dp).background(Color.White.copy(.035f), CircleShape))
        Column(Modifier.scale(scale).alpha(alpha), horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark()
        }
        Text("INDIA'S GAME. ONE APP.", Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 30.dp), color = WarmWhite.copy(.7f), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        Triple("Find your turf", "Discover premium sports turfs near you.", Sport.FOOTBALL),
        Triple("Pick your slot", "Choose the perfect date and time.", Sport.CRICKET),
        Triple("Book & play", "Secure your slot with easy UPI payment.", Sport.BADMINTON)
    )
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize().background(Ink)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            val item = pages[page]
            Box(Modifier.fillMaxSize()) {
                TurfVisual(page + 1, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(.08f), Ink.copy(.35f), Ink))))
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 24.dp, vertical = 118.dp)) {
                    SportIcon(item.third, Modifier.size(34.dp), )
                    Spacer(Modifier.height(18.dp))
                    Text(item.first, style = MaterialTheme.typography.displayLarge, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text(item.second, style = MaterialTheme.typography.bodyLarge, color = TextMuted)
                }
            }
        }
        TextButton(onClick = onDone, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp)) { Text("SKIP", color = Color.White) }
        Row(Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 24.dp, bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(pages.size) { Box(Modifier.width(if (it == pager.currentPage) 28.dp else 7.dp).height(7.dp).background(if (it == pager.currentPage) PulseLavender else Line, CircleShape)) }
        }
        Button(
            onClick = { if (pager.currentPage == pages.lastIndex) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 24.dp, bottom = 20.dp).height(54.dp),
            shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = PulseLavender, contentColor = Ink)
        ) { Text(if (pager.currentPage == pages.lastIndex) "GET STARTED" else "NEXT", style = MaterialTheme.typography.labelLarge); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.ArrowForward, null) }
    }
}

@Composable
private fun AppTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, password: Boolean = false, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, leadingIcon = { Icon(icon, null) },
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), singleLine = true, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseLavender, focusedLabelColor = PulseLavender, unfocusedBorderColor = Line)
    )
}

@Composable
fun LoginScreen(vm: AppViewModel, onUser: () -> Unit, onAdmin: () -> Unit) {
    val busy by vm.authBusy
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun finishLogin(message: String?) {
        error = message
        if (message == null) { if (vm.authRole.value == AuthRole.ADMIN) onAdmin() else onUser() }
    }
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).imePadding()) {
        Box(
            Modifier.fillMaxWidth().height(250.dp).background(
                Brush.verticalGradient(listOf(Color(0xFF4B23C7), PulsePurpleDark, Ink))
            )
        ) {
            Box(Modifier.size(220.dp).align(Alignment.TopEnd).offset(x = 60.dp, y = (-45).dp).background(Color.White.copy(.035f), CircleShape))
            BrandMark(Modifier.align(Alignment.Center))
        }
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding()) {
            Text("Your next game\nstarts here.", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp)); Text("Sign in to discover and book arenas near you.", color = TextMuted)
            Spacer(Modifier.height(26.dp))
            if (error != null) Text(error!!, color = Danger, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        error = null
                        try {
                            val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
                            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                            val credential = CredentialManager.create(context).getCredential(context, request).credential
                            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                vm.loginWithGoogle(GoogleIdTokenCredential.createFrom(credential.data).idToken, ::finishLogin)
                            } else error = "Google returned an unsupported credential."
                        } catch (_: GetCredentialException) {
                            error = "Google sign-in was cancelled or no account is available."
                        } catch (exception: Exception) {
                            error = exception.message ?: "Google sign-in failed."
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PulseLavender),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) { Text(if (busy) "Signing in…" else "Continue", fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(14.dp))
            Text("Choose your Google account. New users are registered automatically; admin access is assigned securely to the approved account.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
fun RegistrationScreen(vm: AppViewModel, onBack: () -> Unit, onComplete: () -> Unit) {
    val busy by vm.authBusy
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
        Spacer(Modifier.height(14.dp)); Text("Join the arena", style = MaterialTheme.typography.displayMedium); Text("Create your player profile.", color = TextMuted)
        Spacer(Modifier.height(28.dp))
        AppTextField(name, { name = it }, "Full name", Icons.Default.Person); Spacer(Modifier.height(12.dp))
        AppTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, "Mobile number", Icons.Default.Phone, keyboardType = KeyboardType.Phone); Spacer(Modifier.height(12.dp))
        AppTextField(email, { email = it }, "Email", Icons.Default.Email, keyboardType = KeyboardType.Email); Spacer(Modifier.height(12.dp))
        AppTextField(password, { password = it }, "Password", Icons.Default.Lock, true); Spacer(Modifier.height(12.dp))
        AppTextField(confirm, { confirm = it }, "Confirm password", Icons.Default.Lock, true)
        if (error != null) { Spacer(Modifier.height(10.dp)); Text(error!!, color = Danger, style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(24.dp)); PrimaryButton(if (busy) "Creating account…" else "Create account", enabled = !busy, onClick = {
            error = when { name.trim().length < 3 -> "Enter your full name."; phone.length != 10 -> "Enter a valid 10-digit Indian mobile number."; !email.contains("@") -> "Enter a valid email."; password.length < 8 -> "Password must be at least 8 characters."; password != confirm -> "Passwords do not match."; else -> null }
            if (error == null) vm.register(name, phone, email, password, confirm) { message -> error = message; if (message == null) onComplete() }
        })
        Spacer(Modifier.height(16.dp)); Text("By joining, you agree to the Terms & Conditions and Privacy Policy.", color = TextMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
