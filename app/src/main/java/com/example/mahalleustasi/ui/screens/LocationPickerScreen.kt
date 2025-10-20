package com.example.mahalleustasi.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun LocationPickerScreen(navController: NavController) {
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val requestPerms = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { map ->
            hasLocationPermission = (map[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                    (map[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        }
    )

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            hasLocationPermission = true
        } else {
            requestPerms.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }
    val cameraPositionState = rememberCameraPositionState()
    var properties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = false)) }

    val selected = remember { mutableStateOf<LatLng?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings,
            properties = properties,
            onMapLongClick = { latLng -> selected.value = latLng }
        ) {
            selected.value?.let { pos ->
                Marker(state = rememberMarkerState(position = pos), title = "Seçilen Konum")
            }
        }

        // Seçim yapıldığında kamerayı marker üzerine yaklaştır
        LaunchedEffect(selected.value) {
            selected.value?.let { pos ->
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 16f))
            }
        }

        // İzinler verildiyse MyLocation'ı aç ve kamerayı son konuma taşı
        LaunchedEffect(hasLocationPermission) {
            if (hasLocationPermission) {
                properties = properties.copy(isMyLocationEnabled = true)
                val last = runCatching { fusedClient.lastLocation }.getOrNull()?.await()
                last?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            } else {
                // Varsayılan: İstanbul merkezine odakla (örn. 41.015, 28.979)
                val ist = LatLng(41.015, 28.979)
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(ist, 10f))
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = selected.value?.let { "Seçilen: ${it.latitude}, ${it.longitude}" } ?: "Haritada uzun basarak konum seçin")
            Button(
                onClick = {
                    val prev = navController.previousBackStackEntry
                    val pos = selected.value
                    if (prev != null && pos != null) {
                        prev.savedStateHandle["picked_lat"] = pos.latitude
                        prev.savedStateHandle["picked_lng"] = pos.longitude
                        navController.popBackStack()
                    }
                },
                enabled = selected.value != null,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Konumu Kullan")
            }
        }
    }
}
