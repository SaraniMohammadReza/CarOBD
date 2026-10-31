package com.sarani.carobd.ui.screens.guide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sarani.carobd.R
import com.sarani.carobd.core.model.GuideAlertType
import com.sarani.carobd.core.model.GuideContentBlock
import com.sarani.carobd.core.model.GuideTopic
import com.sarani.carobd.core.vehicle.guide.GuideRepository
import com.sarani.carobd.ui.components.CarObdCard
import com.sarani.carobd.ui.components.CarObdTopBar
import com.sarani.carobd.ui.components.SectionHeader
import com.sarani.carobd.ui.components.StatusChip
import com.sarani.carobd.ui.components.StatusChipType
import com.sarani.carobd.ui.design.CornerRadius
import com.sarani.carobd.ui.design.SemanticColors
import com.sarani.carobd.ui.design.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    guideRepository: GuideRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val topics = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            guideRepository.getAllTopics()
        } else {
            guideRepository.searchTopics(searchQuery)
        }
    }

    val isPersian = LocalConfiguration.current.locales[0].language.startsWith("fa")

    Scaffold(
        topBar = {
            CarObdTopBar(
                title = stringResource(R.string.guide_title),
                onNavigateBack = onNavigateBack
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.standard),
            contentPadding = PaddingValues(vertical = Spacing.standard),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.guide_search_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CornerRadius.shapeMedium,
                    singleLine = true
                )
            }

            items(topics, key = { it.id }) { topic ->
                GuideTopicCard(topic = topic, isPersian = isPersian)
            }
        }
    }
}

@Composable
private fun GuideTopicCard(
    topic: GuideTopic,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    CarObdCard(
        modifier = modifier,
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPersian) topic.titleFa else topic.titleEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.micro))
                    Text(
                        text = if (isPersian) topic.summaryFa else topic.summaryEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.standard),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    topic.blocks.forEach { block ->
                        when (block) {
                            is GuideContentBlock.Paragraph -> {
                                Text(
                                    text = if (isPersian) block.textFa else block.textEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            is GuideContentBlock.Alert -> {
                                val (bg, fg, chipType) = when (block.type) {
                                    GuideAlertType.WARNING -> Triple(
                                        SemanticColors.warningContainer,
                                        SemanticColors.onWarningContainer,
                                        StatusChipType.WARNING
                                    )
                                    GuideAlertType.CAUTION -> Triple(
                                        SemanticColors.criticalContainer,
                                        SemanticColors.onCriticalContainer,
                                        StatusChipType.CRITICAL
                                    )
                                    GuideAlertType.INFO -> Triple(
                                        SemanticColors.infoContainer,
                                        SemanticColors.onInfoContainer,
                                        StatusChipType.INFO
                                    )
                                    GuideAlertType.TIP -> Triple(
                                        SemanticColors.successContainer,
                                        SemanticColors.onSuccessContainer,
                                        StatusChipType.SUCCESS
                                    )
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = CornerRadius.shapeMedium,
                                    colors = CardDefaults.cardColors(containerColor = bg)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(Spacing.medium)
                                    ) {
                                        StatusChip(
                                            label = if (isPersian) block.titleFa else block.titleEn,
                                            type = chipType
                                        )
                                        Spacer(modifier = Modifier.height(Spacing.compact))
                                        Text(
                                            text = if (isPersian) block.messageFa else block.messageEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = fg
                                        )
                                    }
                                }
                            }
                            is GuideContentBlock.KeyValues -> {
                                val title = if (isPersian) block.titleFa else block.titleEn
                                if (!title.isNullOrBlank()) {
                                    SectionHeader(title = title)
                                }
                                block.entries.forEach { kv ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = Spacing.micro),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isPersian) kv.keyFa else kv.keyEn,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isPersian) kv.valueFa else kv.valueEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            is GuideContentBlock.BulletList -> {
                                val items = if (isPersian) block.itemsFa else block.itemsEn
                                items.forEach { item ->
                                    Text(
                                        text = "• $item",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            is GuideContentBlock.CodeBlock -> {
                                Text(
                                    text = block.code,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
