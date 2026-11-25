package com.hamzaapps.sitecalc.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hamzaapps.sitecalc.BuildConfig
import com.hamzaapps.sitecalc.R
import com.hamzaapps.sitecalc.billing.Monetization
import com.hamzaapps.sitecalc.ui.MainViewModel
import com.hamzaapps.sitecalc.ui.components.ConfirmDialog
import com.hamzaapps.sitecalc.ui.components.SectionTitle
import com.hamzaapps.sitecalc.util.Fmt

fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No browser or store app installed.
    }
}

private val BAG_SIZES = listOf(25.0, 42.5, 50.0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val theme by vm.themeMode.collectAsStateWithLifecycle()
    val isPro by vm.isPro.collectAsStateWithLifecycle()
    val price by vm.proPrice.collectAsStateWithLifecycle()
    val currency by vm.currency.collectAsStateWithLifecycle()
    val bagKg by vm.bagKg.collectAsStateWithLifecycle()
    val privacyRequired by vm.privacyOptionsRequired.collectAsStateWithLifecycle()
    var currencyText by rememberSaveable(currency) { mutableStateOf(currency) }
    var pendingImport by rememberSaveable { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri.toString()
    }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .testTag("settings_screen"),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Column(Modifier.padding(16.dp).fillMaxWidth()) {
                Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.titleMedium)
                if (isPro) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Text(stringResource(R.string.pro_active), Modifier.padding(start = 8.dp))
                    }
                } else {
                    Text(stringResource(R.string.pro_benefits), Modifier.padding(vertical = 8.dp))
                    Button(onClick = { context.findActivity()?.let(vm::buyPro) }, modifier = Modifier.testTag("buy_pro")) {
                        Text(price?.let { stringResource(R.string.buy_pro_price, it) } ?: stringResource(R.string.remove_ads_go_pro))
                    }
                    TextButton(onClick = vm::restorePurchases) { Text(stringResource(R.string.restore_purchase)) }
                }
            }
        }

        SectionTitle(stringResource(R.string.settings_units))
        OutlinedTextField(
            value = currencyText,
            onValueChange = {
                currencyText = it.take(6)
                vm.setCurrency(currencyText)
            },
            label = { Text(stringResource(R.string.currency)) },
            placeholder = { Text(stringResource(R.string.currency_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("currency"),
        )
        Text(stringResource(R.string.cement_bag_size), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BAG_SIZES.forEach { kg ->
                FilterChip(selected = bagKg == kg, onClick = { vm.setBagKg(kg) }, label = { Text(stringResource(R.string.kg_value, Fmt.number(kg))) })
            }
        }

        SectionTitle(stringResource(R.string.settings_appearance))
        val themes = listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            themes.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = theme == index,
                    onClick = { vm.setTheme(index) },
                    shape = SegmentedButtonDefaults.itemShape(index, themes.size),
                ) { Text(stringResource(label)) }
            }
        }

        SectionTitle(stringResource(R.string.settings_data))
        Text(stringResource(R.string.backup_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = { exportLauncher.launch("site-calc-backup.json") }) { Text(stringResource(R.string.export_backup)) }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                modifier = Modifier.padding(start = 8.dp),
            ) { Text(stringResource(R.string.import_backup)) }
        }

        SectionTitle(stringResource(R.string.settings_about))
        if (privacyRequired) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.ad_privacy)) },
                trailingContent = {
                    TextButton(onClick = { context.findActivity()?.let(vm::showPrivacyOptions) }) { Text(stringResource(R.string.open)) }
                },
            )
            HorizontalDivider()
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.rate_app)) },
            trailingContent = {
                TextButton(onClick = { context.openUrl("market://details?id=${context.packageName}") }) { Text(stringResource(R.string.open)) }
            },
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.privacy_policy)) },
            trailingContent = {
                TextButton(onClick = { context.openUrl(Monetization.PRIVACY_POLICY_URL) }) { Text(stringResource(R.string.open)) }
            },
        )
        HorizontalDivider()
        ListItem(headlineContent = { Text(stringResource(R.string.version)) }, trailingContent = { Text(BuildConfig.VERSION_NAME) })
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.import_backup),
            message = stringResource(R.string.confirm_import),
            confirmLabel = stringResource(R.string.replace),
            onConfirm = { vm.importBackup(uri.toUri()) },
            onDismiss = { pendingImport = null },
        )
    }
}
