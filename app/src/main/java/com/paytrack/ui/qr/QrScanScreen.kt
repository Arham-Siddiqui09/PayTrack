package com.paytrack.ui.qr

import android.Manifest
import android.content.Intent
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.paytrack.viewmodel.PendingConfirmationUiState
import com.paytrack.viewmodel.QrScanUiState
import java.util.concurrent.Executors

@Composable
fun QrScanRoute(
    uiState: QrScanUiState,
    onPermissionResult: (Boolean) -> Unit,
    onQrScanned: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onFolderSelected: (String) -> Unit,
    onStartManualConfirmation: () -> Unit,
    onRefreshApps: () -> Unit,
    onLaunchPayment: (String) -> Intent?,
    onPaymentAppOpened: (String) -> Unit,
    onLaunchFailed: () -> Unit,
    onConfirmResult: (Boolean) -> Unit,
    onScanAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult
    )
    val paymentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshApps()
    }

    LaunchedEffect(Unit) {
        onRefreshApps()
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    QrScanScreen(
        uiState = uiState,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onQrScanned = onQrScanned,
        onAmountChanged = onAmountChanged,
        onFolderSelected = onFolderSelected,
        onStartManualConfirmation = onStartManualConfirmation,
        onRefreshApps = onRefreshApps,
        onChooseUpiApp = { packageName ->
            val intent = onLaunchPayment(packageName)
            if (intent == null) return@QrScanScreen
            runCatching {
                paymentLauncher.launch(intent)
                onPaymentAppOpened(packageName)
            }
                .onFailure { onLaunchFailed() }
        },
        onConfirmResult = onConfirmResult,
        onScanAgain = onScanAgain,
        modifier = modifier
    )
}

@Composable
fun QrScanScreen(
    uiState: QrScanUiState,
    onRequestPermission: () -> Unit,
    onQrScanned: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onFolderSelected: (String) -> Unit,
    onStartManualConfirmation: () -> Unit,
    onRefreshApps: () -> Unit,
    onChooseUpiApp: (String) -> Unit,
    onConfirmResult: (Boolean) -> Unit,
    onScanAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showCenteredScanner = uiState.scannedPayeeVpa.isBlank() && uiState.pendingConfirmation == null

    if (showCenteredScanner) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Scan payment QR",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Hold the merchant QR inside the frame to scan it quickly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (uiState.hasCameraPermission) {
                    QrCameraPreview(
                        enabled = true,
                        onQrScanned = onQrScanned
                    )
                } else {
                    PermissionCard(onRequestPermission = onRequestPermission)
                }

                uiState.scanError?.let { error ->
                    MessageCard(
                        title = "Scanner issue",
                        message = error,
                        tone = Color(0xFFFFE5E5)
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Scan payment QR",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            uiState.scanError?.let { error ->
                item { MessageCard(title = "Scanner issue", message = error, tone = Color(0xFFFFE5E5)) }
            }

            if (uiState.scannedPayeeVpa.isNotBlank()) {
                item {
                    PaymentReviewCard(
                        merchantName = uiState.scannedMerchantName,
                        payeeVpa = uiState.scannedPayeeVpa,
                        note = uiState.scannedNote,
                        scannedAmountText = uiState.scannedAmountText,
                        amountInput = uiState.amountInput,
                        isAmountLocked = uiState.isAmountLocked,
                        onAmountChanged = onAmountChanged,
                        folderOptions = uiState.folders,
                        selectedFolderId = uiState.selectedFolderId,
                        selectedFolderBalance = uiState.selectedFolderBalance,
                        projectedBalance = uiState.projectedBalance,
                        onFolderSelected = onFolderSelected
                    )
                }

                item {
                    UpiAppPickerCard(
                        merchantName = uiState.scannedMerchantName,
                        payeeVpa = uiState.scannedPayeeVpa,
                        isAmountLocked = uiState.isAmountLocked,
                        appItems = uiState.availableUpiApps,
                        paymentError = uiState.paymentError,
                        onStartManualConfirmation = onStartManualConfirmation,
                        onRefreshApps = onRefreshApps,
                        onChooseUpiApp = onChooseUpiApp
                    )
                }

                item {
                    Button(
                        onClick = onScanAgain,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scan Another QR")
                    }
                }
            }

            uiState.pendingConfirmation?.let { confirmation ->
                item {
                    ConfirmationCard(
                        confirmation = confirmation,
                        onConfirmResult = onConfirmResult
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Camera access is required to scan merchant QR codes.",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onRequestPermission) {
                Text("Grant Camera Access")
            }
        }
    }
}

@Composable
private fun PaymentReviewCard(
    merchantName: String,
    payeeVpa: String,
    note: String?,
    scannedAmountText: String,
    amountInput: String,
    isAmountLocked: Boolean,
    onAmountChanged: (String) -> Unit,
    folderOptions: List<com.paytrack.viewmodel.FolderPickerUiState>,
    selectedFolderId: String?,
    selectedFolderBalance: String,
    projectedBalance: String,
    onFolderSelected: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = merchantName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = payeeVpa,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            note?.let { noteText ->
                Text(
                    text = noteText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (scannedAmountText.isNotBlank()) {
                Text(
                    text = "QR amount: $scannedAmountText",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = amountInput,
                onValueChange = onAmountChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (isAmountLocked) "Amount from merchant QR" else "Amount") },
                singleLine = true,
                readOnly = isAmountLocked,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            if (isAmountLocked) {
                Text(
                    text = "This QR already includes a fixed merchant amount, so PayTrack keeps that value unchanged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Select folder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            folderOptions.forEach { folder ->
                FolderOptionRow(
                    name = folder.name,
                    balance = folder.balance,
                    selected = folder.id == selectedFolderId,
                    onClick = { onFolderSelected(folder.id) }
                )
            }

            if (selectedFolderId != null) {
                Text(
                    text = "Available balance: $selectedFolderBalance",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (projectedBalance.isNotBlank()) {
                    Text(
                        text = "Balance after payment: $projectedBalance",
                        color = if (projectedBalance.startsWith("-")) Color(0xFFB3261E) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderOptionRow(
    name: String,
    balance: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontWeight = FontWeight.SemiBold)
        Text(text = balance)
    }
}

@Composable
private fun UpiAppPickerCard(
    merchantName: String,
    payeeVpa: String,
    isAmountLocked: Boolean,
    appItems: List<com.paytrack.viewmodel.UpiAppUiState>,
    paymentError: String?,
    onStartManualConfirmation: () -> Unit,
    onRefreshApps: () -> Unit,
    onChooseUpiApp: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Choose a UPI app",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Fallback for merchant QR issues",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isAmountLocked) {
                            "If direct app opening fails for this merchant QR, pay $merchantName ($payeeVpa) in GPay manually, then come back and confirm here."
                        } else {
                            "If direct app opening fails, pay $merchantName ($payeeVpa) in your UPI app manually, then come back and confirm here."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onStartManualConfirmation,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("I Will Pay Manually And Confirm")
                    }
                }
            }

            if (appItems.isEmpty()) {
                Text(
                    text = "No UPI apps were found. Install one and refresh this list.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onRefreshApps) {
                    Text("Refresh UPI Apps")
                }
            } else {
                appItems.forEach { app ->
                    Button(
                        onClick = { onChooseUpiApp(app.packageName) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open ${app.label} To Scan Again")
                    }
                }
            }

            paymentError?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = Color(0xFFB3261E)
                )
            }
        }
    }
}

@Composable
private fun ConfirmationCard(
    confirmation: PendingConfirmationUiState,
    onConfirmResult: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Confirm payment result",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text("Merchant: ${confirmation.merchantName}")
            Text("Folder: ${confirmation.folderName}")
            Text("Amount: ${confirmation.amount}")
            Text("UPI app: ${confirmation.appLabel}")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onConfirmResult(true) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Payment Successful")
                }
                Button(
                    onClick = { onConfirmResult(false) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Failed / Cancelled")
                }
            }
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    message: String,
    tone: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = tone),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.Bold)
            Text(text = message)
        }
    }
}

@Composable
private fun QrCameraPreview(
    enabled: Boolean,
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var lastValue by remember { mutableStateOf<String?>(null) }

    DisposableEffect(enabled) {
        if (!enabled) {
            onDispose { }
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val executor = Executors.newSingleThreadExecutor()
            val scanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build().also { previewUseCase ->
                    previewUseCase.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { imageProxy ->
                    processImageProxy(
                        imageProxy = imageProxy,
                        onCodeFound = { value ->
                            if (lastValue != value) {
                                lastValue = value
                                onQrScanned(value)
                            }
                        },
                        scanner = scanner
                    )
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }, ContextCompat.getMainExecutor(context))

            onDispose {
                runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
                scanner.close()
                executor.shutdown()
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color.Black, RoundedCornerShape(24.dp))
    )
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    onCodeFound: (String) -> Unit,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner
) {
    val mediaImage = imageProxy.image ?: run {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.let(onCodeFound)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}
