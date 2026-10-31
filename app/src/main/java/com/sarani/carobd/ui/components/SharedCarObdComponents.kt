package com.sarani.carobd.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarani.carobd.R
import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.ui.design.CarObdSizes
import com.sarani.carobd.ui.design.CornerRadius
import com.sarani.carobd.ui.design.Elevation
import com.sarani.carobd.ui.design.SemanticColors
import com.sarani.carobd.ui.design.Spacing

/**
 * Standard top app bar for CarOBD screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarObdTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.ui_back)
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

/**
 * Global Vehicle Context Header banner displayed at the top of ownership screens.
 */
@Composable
fun VehicleContextHeader(
    vehicle: OwnedVehicle?,
    modifier: Modifier = Modifier,
    onSwitchVehicle: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CornerRadius.shapeMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CornerRadius.shapePill)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.compact))
                Column(modifier = Modifier.weight(1f)) {
                    if (vehicle != null) {
                        Text(
                            text = vehicle.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val unknownOdometer = stringResource(R.string.ownership_odometer_unknown)
                        val summary = buildList {
                            if ((vehicle.year ?: 0) > 0) add(vehicle.year.toString())
                            if (!vehicle.engineCode.isNullOrBlank()) add(vehicle.engineCode)
                            val odo = vehicle.currentOdometerKm
                            add(if (odo == null) unknownOdometer else "%,d km".format(odo))
                        }.joinToString(" • ")

                        if (summary.isNotBlank()) {
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.parts_no_active_vehicle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            TextButton(onClick = onSwitchVehicle) {
                Text(
                    text = stringResource(R.string.maintenance_switch_vehicle),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Standard Section Header with optional action link.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.compact),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!actionLabel.isNullOrBlank() && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

enum class StatusChipType {
    SUCCESS,
    WARNING,
    CRITICAL,
    INFO,
    NEUTRAL
}

/**
 * Semantic status chip communicating state consistently without misleading health assertions.
 */
@Composable
fun StatusChip(
    label: String,
    type: StatusChipType,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val (bg, fg) = when (type) {
        StatusChipType.SUCCESS -> SemanticColors.successContainer to SemanticColors.onSuccessContainer
        StatusChipType.WARNING -> SemanticColors.warningContainer to SemanticColors.onWarningContainer
        StatusChipType.CRITICAL -> SemanticColors.criticalContainer to SemanticColors.onCriticalContainer
        StatusChipType.INFO -> SemanticColors.infoContainer to SemanticColors.onInfoContainer
        StatusChipType.NEUTRAL -> SemanticColors.neutralMutedContainer to SemanticColors.onNeutralMutedContainer
    }

    Surface(
        color = bg,
        shape = CornerRadius.shapeSmall,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.compact, vertical = Spacing.micro),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.micro))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Standard compact KPI counter (e.g. Overdue, Due Soon, Total Parts).
 */
@Composable
fun CompactStat(
    count: Int,
    label: String,
    modifier: Modifier = Modifier,
    type: StatusChipType = StatusChipType.NEUTRAL,
    onClick: (() -> Unit)? = null
) {
    val (bg, fg) = when (type) {
        StatusChipType.CRITICAL -> SemanticColors.criticalContainer to SemanticColors.onCriticalContainer
        StatusChipType.WARNING -> SemanticColors.warningContainer to SemanticColors.onWarningContainer
        StatusChipType.SUCCESS -> SemanticColors.successContainer to SemanticColors.onSuccessContainer
        StatusChipType.INFO -> SemanticColors.infoContainer to SemanticColors.onInfoContainer
        StatusChipType.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bg,
        shape = CornerRadius.shapeMedium,
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.compact),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = fg
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Prominent feature destination card for top-level navigation hubs (Home screen).
 */
@Composable
fun PrimaryFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeType: StatusChipType = StatusChipType.NEUTRAL
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CornerRadius.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.standard),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CornerRadius.shapeMedium)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.compact)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!badgeText.isNullOrBlank()) {
                        StatusChip(label = badgeText, type = badgeType)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(Spacing.compact))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Standard Empty State container.
 */
@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CornerRadius.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(Spacing.medium))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.compact))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.medium)
            )
            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(Spacing.standard))
                Button(
                    onClick = onActionClick,
                    shape = CornerRadius.shapeMedium
                ) {
                    Text(text = actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Standard Provenance Badge clearly communicating data origin and verification level.
 */
@Composable
fun ProvenanceBadge(
    confidence: ConfidenceLevel,
    modifier: Modifier = Modifier
) {
    val (label, type) = when (confidence) {
        ConfidenceLevel.VERIFIED_OEM -> "OEM VERIFIED / کارخانه" to StatusChipType.SUCCESS
        ConfidenceLevel.COMMUNITY_CONFIRMED -> "COMMUNITY / مراجع فنی" to StatusChipType.INFO
        ConfidenceLevel.VERIFIED_STANDARD -> "STANDARD / استاندارد" to StatusChipType.SUCCESS
        ConfidenceLevel.HIGH_CONFIDENCE -> "HIGH CONFIDENCE" to StatusChipType.INFO
        ConfidenceLevel.EXPERIMENTAL -> "EXPERIMENTAL / آزمایشی" to StatusChipType.WARNING
        ConfidenceLevel.UNKNOWN -> "UNVERIFIED / نامشخص" to StatusChipType.NEUTRAL
    }
    StatusChip(label = label, type = type, modifier = modifier)
}

/**
 * Canonical CarOBD Card providing standardized surface styling, padding, and elevation.
 */
@Composable
fun CarObdCard(
    modifier: Modifier = Modifier,
    shape: Shape = CornerRadius.shapeLarge,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable(onClick = onClick),
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border
        ) {
            content()
        }
    }
}

/**
 * Standard primary call-to-action button respecting minimum 48dp touch target and clear loading state.
 */
@Composable
fun CarObdPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = CornerRadius.shapeMedium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(CarObdSizes.buttonHeight)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(CarObdSizes.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(Spacing.compact))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Standard secondary button (outlined) for non-primary actions.
 */
@Composable
fun CarObdSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CornerRadius.shapeMedium,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(CarObdSizes.buttonHeight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(CarObdSizes.iconSmall)
                )
                Spacer(modifier = Modifier.width(Spacing.compact))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Canonical Error State container for failed data loading or hardware communications.
 */
@Composable
fun ErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = stringResource(R.string.ui_retry)
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CornerRadius.shapeLarge,
        colors = CardDefaults.cardColors(containerColor = SemanticColors.criticalContainer.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = SemanticColors.critical,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(Spacing.medium))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SemanticColors.onCriticalContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.compact))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SemanticColors.onCriticalContainer.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.medium)
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(Spacing.standard))
                Button(
                    onClick = onRetry,
                    shape = CornerRadius.shapeMedium,
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticColors.critical)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(CarObdSizes.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(Spacing.compact))
                    Text(text = retryLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
