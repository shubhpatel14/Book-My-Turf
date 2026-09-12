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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onDone: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (shown) 1f else .86f, tween(380, easing = FastOutSlowInEasing), label = "splash")
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(300), label = "splashAlpha")
    LaunchedEffect(Unit) { shown = true; delay(1_200); onDone() }
    Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
        Column(Modifier.scale(scale).alpha(alpha), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(142.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(118.dp).background(Lime.copy(.1f), CircleShape))
                AnimatedBrandCrest(Modifier.size(132.dp))
            }
            Text("BOOKMYTURF", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, letterSpacing = (-.8).sp)
            Spacer(Modifier.height(14.dp)); Box(Modifier.width(72.dp).height(3.dp).background(Lime)); Spacer(Modifier.height(14.dp))
            Text("BOOK. PLAY. DOMINATE.", color = Lime, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp)
        }
        Text("INDIA'S GAME. ONE APP.", Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp), color = TextMuted.copy(.65f), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        Triple("FIND YOUR\nTURF", "Discover premium sports turfs near you.", Sport.FOOTBALL),
        Triple("PICK YOUR\nSLOT", "Choose the perfect date and time.", Sport.CRICKET),
        Triple("BOOK &\nPLAY", "Secure your slot with easy UPI payment.", Sport.BADMINTON)
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
            repeat(pages.size) { Box(Modifier.width(if (it == pager.currentPage) 28.dp else 7.dp).height(7.dp).background(if (it == pager.currentPage) Lime else Line, CircleShape)) }
        }
        Button(
            onClick = { if (pager.currentPage == pages.lastIndex) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 24.dp, bottom = 20.dp).height(54.dp),
            shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)
        ) { Text(if (pager.currentPage == pages.lastIndex) "GET STARTED" else "NEXT", style = MaterialTheme.typography.labelLarge); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.ArrowForward, null) }
    }
}

@Composable
private fun AppTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, password: Boolean = false, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, leadingIcon = { Icon(icon, null) },
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), singleLine = true, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Lime, focusedLabelColor = Lime, unfocusedBorderColor = Line)
    )
}

@Composable
fun LoginScreen(vm: AppViewModel, onUser: () -> Unit, onAdmin: () -> Unit, onRegister: () -> Unit) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        Spacer(Modifier.height(28.dp)); BrandMark(); Spacer(Modifier.height(54.dp))
        Text("WELCOME\nBACK, PLAYER.", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(10.dp)); Text("Sign in and get back in the game.", color = TextMuted)
        Spacer(Modifier.height(30.dp))
        AppTextField(identifier, { identifier = it }, "Mobile number or email", Icons.Default.Person)
        Spacer(Modifier.height(14.dp)); AppTextField(password, { password = it }, "Password", Icons.Default.Lock, true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { error = "Password reset link simulated for demo." }) { Text("FORGOT PASSWORD?", color = Lime, style = MaterialTheme.typography.labelMedium) } }
        if (error != null) Text(error!!, color = if (error!!.contains("simulated")) TextMuted else Danger, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Login", onClick = {
            if (vm.login(identifier, password)) { if (vm.authRole.value == AuthRole.ADMIN) onAdmin() else onUser() }
            else error = "Enter a valid email/mobile and a password with 6+ characters."
        })
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { vm.login("demo@bookmyturf.app", "player123"); onUser() }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Line)) {
            Icon(Icons.Default.SportsSoccer, null); Spacer(Modifier.width(10.dp)); Text("CONTINUE WITH DEMO USER", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(22.dp))
        Surface(color = Steel, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Lime.copy(.25f))) {
            Column(Modifier.padding(16.dp)) {
                Text("DEMO ADMIN CREDENTIALS", color = Lime, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp)); Text("admin@bookmyturf.demo", fontWeight = FontWeight.Bold); Text("Admin@12345", color = TextMuted)
                Spacer(Modifier.height(8.dp)); TextButton(onClick = { identifier = "admin@bookmyturf.demo"; password = "Admin@12345" }, contentPadding = PaddingValues(0.dp)) { Text("USE CREDENTIALS", color = Lime) }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Text("NEW TO THE ARENA?", color = TextMuted, style = MaterialTheme.typography.labelMedium); TextButton(onClick = onRegister) { Text("CREATE ACCOUNT", color = Lime, style = MaterialTheme.typography.labelMedium) } }
    }
}

@Composable
fun RegistrationScreen(vm: AppViewModel, onBack: () -> Unit, onComplete: () -> Unit) {
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
        Spacer(Modifier.height(14.dp)); Text("JOIN THE\nARENA.", style = MaterialTheme.typography.displayMedium); Text("Create your player profile.", color = TextMuted)
        Spacer(Modifier.height(28.dp))
        AppTextField(name, { name = it }, "Full name", Icons.Default.Person); Spacer(Modifier.height(12.dp))
        AppTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, "Mobile number", Icons.Default.Phone, keyboardType = KeyboardType.Phone); Spacer(Modifier.height(12.dp))
        AppTextField(email, { email = it }, "Email", Icons.Default.Email, keyboardType = KeyboardType.Email); Spacer(Modifier.height(12.dp))
        AppTextField(password, { password = it }, "Password", Icons.Default.Lock, true); Spacer(Modifier.height(12.dp))
        AppTextField(confirm, { confirm = it }, "Confirm password", Icons.Default.Lock, true)
        if (error != null) { Spacer(Modifier.height(10.dp)); Text(error!!, color = Danger, style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(24.dp)); PrimaryButton("Create Account", {
            error = when { name.trim().length < 3 -> "Enter your full name."; phone.length != 10 -> "Enter a valid 10-digit Indian mobile number."; !email.contains("@") -> "Enter a valid email."; password.length < 8 -> "Password must be at least 8 characters."; password != confirm -> "Passwords do not match."; else -> null }
            if (error == null) { vm.login(email, password); onComplete() }
        })
        Spacer(Modifier.height(16.dp)); Text("By joining, you agree to the Terms & Conditions and Privacy Policy.", color = TextMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
