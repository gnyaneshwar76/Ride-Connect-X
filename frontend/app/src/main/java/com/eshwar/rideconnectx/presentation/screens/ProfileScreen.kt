package com.eshwar.rideconnectx.presentation.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eshwar.rideconnectx.core.util.ProfilePhotoStore
import com.eshwar.rideconnectx.domain.model.LoginMethod
import com.eshwar.rideconnectx.domain.model.Vehicle
import com.eshwar.rideconnectx.domain.model.VehicleColor
import com.eshwar.rideconnectx.domain.repository.ConnectionState
import com.eshwar.rideconnectx.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.eshwar.rideconnectx.core.util.AppPermissions
import com.eshwar.rideconnectx.presentation.components.BackHeader
import com.eshwar.rideconnectx.presentation.components.PrimaryButton
import com.eshwar.rideconnectx.presentation.components.RiderAvatar
import com.eshwar.rideconnectx.presentation.components.RcxPhotoFill
import com.eshwar.rideconnectx.presentation.components.SettingsDivider
import com.eshwar.rideconnectx.presentation.components.SettingsGroup
import com.eshwar.rideconnectx.presentation.components.SettingsLinkRow
import com.eshwar.rideconnectx.presentation.components.VehicleStage
import com.eshwar.rideconnectx.presentation.theme.Rcx
import com.eshwar.rideconnectx.presentation.theme.RcxType
import com.eshwar.rideconnectx.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Screen 21 — Profile.
 *
 * Account, vehicle and the four account actions. Guest and signed-in riders are
 * handled differently, as the design requires: a guest has no email and gets a
 * badge, and signing out returns them to Sign In rather than clearing an
 * account they never had.
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onChangeVehicle: () -> Unit,
    onSignedOut: () -> Unit,
    vm: ProfileViewModel = hiltViewModel(),
) {
    val c = Rcx.colors
    val account by vm.account.collectAsStateWithLifecycle()
    val location by vm.riderLocation.collectAsStateWithLifecycle()
    val selection by vm.vehicle.collectAsStateWithLifecycle()
    val connection by vm.connectionState.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf(false) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    val photoVersion by vm.photoVersion.collectAsStateWithLifecycle()
    val pendingPhoto by vm.pendingPhoto.collectAsStateWithLifecycle()

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { vm.onPhotoPicked(it) }

    // Same standard dialog the rest of the app uses.
    val requestPhotos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        pickPhoto.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    var confirmMigrate by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf(false) }
    val activity = LocalContext.current as android.app.Activity

    val connected = connection is ConnectionState.Connected

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(
            Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            BackHeader(title = "Profile", onBack = onBack)

            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    ProfileCard(
                        name = account.name,
                        email = account.email,
                        location = location,
                        isGuest = account.isGuest,
                        method = account.method,
                        photoVersion = photoVersion,
                        onEditPhoto = { showPhotoSheet = true },
                    )
                }

                item {
                    VehicleCard(
                        vehicle = selection.vehicle,
                        color = selection.color,
                        connected = connected,
                        onChange = onChangeVehicle,
                    )
                }

                item {
                    SettingsGroup("Account", Icons.Filled.Person) {
                        SettingsLinkRow(
                            title = "Edit profile",
                            subtitle = "Name and city",
                            onClick = { editing = true },
                        )
                        SettingsDivider()
                        SettingsLinkRow(
                            title = "Change vehicle",
                            subtitle = selection.vehicle?.name ?: "No vehicle selected",
                            onClick = onChangeVehicle,
                        )
                        SettingsDivider()
                        // Moving accounts keeps everything. Sign-out already
                        // leaves the rider name, nickname, city, vehicle, paint
                        // and photo on the device — only the account identity
                        // goes — and signing in to a fresh account now carries
                        // all of it up to the new one.
                        SettingsLinkRow(
                            title = if (account.isGuest)
                                "Save to an account"
                            else
                                "Move to another account",
                            subtitle = if (account.isGuest)
                                "Sign in with Google or email — your profile and vehicle come with you"
                            else
                                "Sign in with a different email — your profile and vehicle come with you",
                            onClick = { confirmMigrate = true },
                        )
                        SettingsDivider()
                        SettingsLinkRow(
                            title = "Sign out",
                            subtitle = if (account.isGuest)
                                "Ends this guest session"
                            else
                                "Signs out of ${account.email.ifBlank { "your account" }}",
                            accent = c.amber,
                            chevron = false,
                            onClick = { confirmSignOut = true },
                        )
                        SettingsDivider()
                        SettingsLinkRow(
                            title = "Delete local data",
                            subtitle = "Rides, notifications, service records and contacts",
                            accent = c.red,
                            chevron = false,
                            onClick = { confirmDelete = true },
                        )
                        // Required by Google Play for any app that creates
                        // accounts: deletion inside the app, not by email.
                        if (!account.isGuest) {
                            SettingsDivider()
                            SettingsLinkRow(
                                title = "Delete account",
                                subtitle = "Permanently removes your account and its data",
                                accent = c.red,
                                chevron = false,
                                onClick = { confirmDeleteAccount = true },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPhotoSheet) {
        PhotoActionSheet(
            hasPhoto = vm.hasPhoto,
            onChoose = {
                showPhotoSheet = false
                requestPhotos.launch(AppPermissions.photos.manifest.toTypedArray())
            },
            onRemove = {
                showPhotoSheet = false
                vm.removePhoto()
            },
            onDismiss = { showPhotoSheet = false },
        )
    }

    // Framing happens before anything is written, same as Create Profile.
    pendingPhoto?.let { pending ->
        PhotoCropDialog(
            source = pending,
            decode = vm::decodeForCrop,
            onCancel = vm::cancelCrop,
            onConfirm = vm::confirmCrop,
        )
    }

    if (editing) {
        EditProfileSheet(
            initialName = account.name,
            initialLocation = location,
            validate = vm::validateName,
            onDismiss = { editing = false },
            onSave = { name, city ->
                vm.saveProfile(name, city)
                editing = false
            },
        )
    }

    if (confirmMigrate) {
        ConfirmSheet(
            title = if (account.isGuest) "Save to an account?" else "Move to another account?",
            body = "You'll be taken to sign in. Your rider name, nickname, city, " +
                "vehicle, paint and photo stay on this phone and are saved to " +
                "whichever account you sign in with next.\n\n" +
                "Rides, service records and emergency contacts stay on this " +
                "phone either way.",
            confirmLabel = "Continue to sign in",
            onDismiss = { confirmMigrate = false },
            onConfirm = {
                confirmMigrate = false
                // The sheet above promises the profile follows the rider to the
                // next account, so this is the one path that keeps it.
                vm.signOut(keepLocalProfile = true, onDone = onSignedOut)
            },
        )
    }

    if (confirmSignOut) {
        ConfirmSheet(
            title = "Sign out?",
            body = if (account.isGuest)
                "Guest profiles live only on this phone, so signing out ends this " +
                    "session. Your rides and settings stay on the device."
            else
                "You will be returned to sign in. Your profile is safe in your " +
                    "account and comes back when you sign in again.",
            confirmLabel = "Sign out",
            onDismiss = { confirmSignOut = false },
            onConfirm = {
                confirmSignOut = false
                vm.signOut(onDone = onSignedOut)
            },
        )
    }

    if (confirmDeleteAccount) {
        val needsPassword = account.method == LoginMethod.EMAIL
        var password by remember { mutableStateOf("") }
        ConfirmSheet(
            title = "Delete your account?",
            body = "This permanently deletes your account, and with it your profile, " +
                "saved places and rides in the cloud, plus this account's contacts, " +
                "service records and rides on this phone. It can't be undone.\n\n" +
                if (needsPassword) "Enter your password to confirm."
                else "Google will ask you to confirm it's you.",
            confirmLabel = if (deletingAccount) "Deleting…" else "Delete forever",
            onDismiss = { if (!deletingAccount) confirmDeleteAccount = false },
            onConfirm = {
                if (deletingAccount || (needsPassword && password.isEmpty())) return@ConfirmSheet
                deletingAccount = true
                vm.deleteAccount(activity, password.takeIf { needsPassword }) { error ->
                    deletingAccount = false
                    if (error == null) {
                        confirmDeleteAccount = false
                        onSignedOut()
                    } else {
                        android.widget.Toast.makeText(activity, error, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            },
            extra = if (needsPassword) {
                {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else null,
        )
    }

    if (confirmDelete) {
        ConfirmSheet(
            title = "Delete local data?",
            body = "Removes ride history, notifications, service records and " +
                "emergency contacts from this phone, and forgets the paired " +
                "vehicle. Your account is not deleted.",
            confirmLabel = "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                vm.deleteLocalData { }
            },
        )
    }
}

/* ── Profile card ─────────────────────────────────────────────────── */

@Composable
private fun ProfileCard(
    name: String,
    email: String,
    location: String,
    isGuest: Boolean,
    method: LoginMethod,
    photoVersion: Int,
    onEditPhoto: () -> Unit,
) {
    val c = Rcx.colors
    val shape = RoundedCornerShape(24.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.border, shape)
    ) {
        // Cover banner across the top of the card, with the avatar overlapping
        // its lower half. The gradient runs the photo into the card colour so
        // the two are one surface rather than a picture with a card under it.
        RcxPhotoFill(
            res = R.drawable.img_profile_cover,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .align(Alignment.TopCenter),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to c.card,
                        )
                    )
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 46.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Tappable, with a camera badge. The picture could previously only
            // be set during Create Profile — so the screen actually called
            // "Profile" could show it and not change it, and anyone who skipped
            // it at setup had no route back to it at all.
            Box(contentAlignment = Alignment.BottomEnd) {
                RiderAvatar(
                    name = name,
                    size = 92.dp,
                    version = photoVersion,
                    modifier = Modifier.clickable(onClick = onEditPhoto),
                )
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(c.blue)
                        .clickable(onClick = onEditPhoto),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = stringResource(R.string.profile_change_photo),
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                name.ifBlank { "Rider" },
                style = RcxType.Section.copy(fontSize = 19.sp),
                color = c.text,
            )

            // Guests have no email; showing an empty line instead would look broken.
            if (email.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(email, style = RcxType.BodySmall.copy(fontSize = 13.sp), color = c.muted)
            }

            if (location.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(location, style = RcxType.BodySmall.copy(fontSize = 12.sp), color = c.muted)
            }

            Spacer(Modifier.height(12.dp))

            AccountBadge(isGuest = isGuest, method = method)
        }
    }
}

@Composable
private fun AccountBadge(isGuest: Boolean, method: LoginMethod) {
    val c = Rcx.colors
    val (label, accent) = when {
        isGuest -> "GUEST MODE" to c.amber
        method == LoginMethod.GOOGLE -> "GOOGLE ACCOUNT" to c.blue
        method == LoginMethod.EMAIL -> "EMAIL ACCOUNT" to c.blue
        else -> "SIGNED OUT" to c.muted
    }
    Text(
        label,
        style = RcxType.MonoTiny,
        color = accent,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.11f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/* ── Vehicle card ─────────────────────────────────────────────────── */

@Composable
private fun VehicleCard(
    vehicle: Vehicle?,
    color: VehicleColor?,
    connected: Boolean,
    onChange: () -> Unit,
) {
    val c = Rcx.colors
    val shape = RoundedCornerShape(22.dp)
    val vehicleName = vehicle?.name.orEmpty()
    val colorName = color?.name.orEmpty()

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.border, shape)
            .clickable(onClick = onChange)
            .padding(18.dp),
    ) {
        Text("MY VEHICLE", style = RcxType.MonoTiny, color = c.muted)

        // Same treatment as the dashboard: the machine is the point of the
        // card, so it gets the width rather than a thumbnail in the corner.
        if (vehicle != null) {
            Spacer(Modifier.height(12.dp))
            VehicleStage(
                vehicle = vehicle,
                colorway = color,
                accent = c.blue,
                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                height = 180.dp,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            vehicleName.ifBlank { "No vehicle selected" },
            style = RcxType.Wordmark.copy(fontSize = 19.sp),
            color = c.text,
        )
        if (colorName.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                color?.let { paint ->
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(paint.primary)
                            .border(1.dp, c.border, CircleShape)
                    )
                }
                Text(colorName, style = RcxType.BodySmall.copy(fontSize = 12.sp), color = c.muted)
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background((if (connected) c.green else c.muted).copy(alpha = 0.07f))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Bluetooth,
                null,
                Modifier.size(14.dp),
                tint = if (connected) c.green else c.muted,
            )
            Text(
                if (connected) "Connected" else "Not connected",
                style = RcxType.BodySmall.copy(fontSize = 12.sp),
                color = if (connected) c.green else c.muted,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.TwoWheeler, null, Modifier.size(14.dp), tint = c.muted.copy(alpha = 0.5f))
        }
    }
}

/* ── Edit profile ─────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoActionSheet(
    hasPhoto: Boolean,
    onChoose: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Rcx.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.card,
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                stringResource(R.string.profile_photo_title),
                style = RcxType.Section.copy(fontSize = 17.sp),
                color = c.text,
            )
            Spacer(Modifier.height(18.dp))

            PrimaryButton(
                label = stringResource(R.string.profile_change_photo),
                onClick = onChoose,
                modifier = Modifier.fillMaxWidth(),
            )

            // Only offered when there is something to remove.
            if (hasPhoto) {
                Spacer(Modifier.height(10.dp))
                PrimaryButton(
                    label = stringResource(R.string.profile_remove_photo),
                    onClick = onRemove,
                    secondary = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    initialName: String,
    initialLocation: String,
    validate: (String) -> String?,
    onDismiss: () -> Unit,
    onSave: (name: String, location: String) -> Unit,
) {
    val c = Rcx.colors
    var name by remember { mutableStateOf(initialName) }
    var location by remember { mutableStateOf(initialLocation) }
    var showError by remember { mutableStateOf(false) }
    val error = validate(name)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.card,
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Edit profile", style = RcxType.Section.copy(fontSize = 17.sp), color = c.text)
            Spacer(Modifier.height(6.dp))
            Text(
                "Your name is what your scooter's cluster greets you by.",
                style = RcxType.BodySmall.copy(fontSize = 12.sp),
                color = c.muted,
            )
            Spacer(Modifier.height(18.dp))

            ProfileField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Your name",
            )
            if (showError && error != null) {
                Text(
                    error,
                    style = RcxType.BodySmall.copy(fontSize = 12.sp),
                    color = c.red,
                    modifier = Modifier.padding(start = 4.dp, top = 5.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            ProfileField(
                value = location,
                onValueChange = { location = it },
                placeholder = "City",
            )

            Spacer(Modifier.height(22.dp))

            PrimaryButton(
                label = "Save",
                onClick = {
                    if (error != null) showError = true else onSave(name.trim(), location.trim())
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val c = Rcx.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = RcxType.Body.copy(fontSize = 14.sp), color = c.muted) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = RcxType.Body.copy(fontSize = 15.sp, color = c.text),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = c.card2,
            unfocusedContainerColor = c.card2,
            focusedBorderColor = c.blue,
            unfocusedBorderColor = c.border,
            cursorColor = c.blue,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/* ── Helpers ──────────────────────────────────────────────────────── */

