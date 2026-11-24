package com.example.ui.components

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCartCheckout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MenuItemEntity
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.formatPkr
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosBillingScreen(
    categories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    filteredMenuItems: List<MenuItemEntity>,
    cartItems: List<CartItem>,
    cartTotalPkr: Int,
    cartItemCount: Int,
    selectedPaymentMethod: PaymentMethod,
    amountPaidInput: String,
    isAutoUdhar: Boolean,
    udharRemainingPkr: Int,
    cashChangePkr: Int?,
    transactionIdInput: String,
    customerNameInput: String,
    customerPhoneInput: String,
    checkoutError: String?,
    onSelectCategory: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onAddToCart: (MenuItemEntity) -> Unit,
    onDecrementCartItem: (Long) -> Unit,
    onClearCart: () -> Unit,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    onAmountPaidChange: (String) -> Unit,
    onFullAmountShortcut: () -> Unit,
    onZeroAmountShortcut: () -> Unit,
    onTransactionIdChange: (String) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onCustomerPhoneChange: (String) -> Unit,
    onCompleteOrder: () -> Boolean,
    modifier: Modifier = Modifier
) {
    var showCartSheet by rememberSaveable { mutableStateOf(false) }
    val cartQuantities = cartItems.associate { it.menuItem.id to it.quantity }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedLayout = maxWidth >= 760.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Left/Main Menu Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search pizza, shawarma, zinger...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search menu"
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("pos_search_input")
                    )

                    // Category Filter Chips
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(categories, key = { it }) { category ->
                            val selected = category == selectedCategory
                            val visual = categoryVisualFor(category)
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectCategory(category) },
                                label = {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = visual.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("pos_category_chip_$category")
                            )
                        }
                    }

                    // Menu Grid
                    if (filteredMenuItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No matching items",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Try another category or add items in Menu editor.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 162.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = if (!isExpandedLayout && cartItemCount > 0) 96.dp else 20.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredMenuItems, key = { it.id }) { item ->
                                val qtyInCart = cartQuantities[item.id] ?: 0
                                PosMenuCard(
                                    item = item,
                                    quantityInCart = qtyInCart,
                                    onTapAdd = { onAddToCart(item) },
                                    onDecrement = { onDecrementCartItem(item.id) }
                                )
                            }
                        }
                    }
                }

                // Sticky Bottom Cart Bar for Phone/Compact Screens
                if (!isExpandedLayout) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = cartItemCount > 0,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Surface(
                            onClick = { showCartSheet = true },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .testTag("sticky_cart_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = cartItemCount.toString(),
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = formatPkr(cartTotalPkr),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = "Tap for Cash / Online & Checkout",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Checkout",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.ShoppingCartCheckout,
                                        contentDescription = "Open cart checkout",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Docked Checkout Panel for Tablet/Expanded Screens
            if (isExpandedLayout) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .width(390.dp)
                        .fillMaxHeight()
                        .padding(top = 8.dp, end = 16.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    CartCheckoutContent(
                        cartItems = cartItems,
                        cartTotalPkr = cartTotalPkr,
                        selectedPaymentMethod = selectedPaymentMethod,
                        amountPaidInput = amountPaidInput,
                        isAutoUdhar = isAutoUdhar,
                        udharRemainingPkr = udharRemainingPkr,
                        cashChangePkr = cashChangePkr,
                        transactionIdInput = transactionIdInput,
                        customerNameInput = customerNameInput,
                        customerPhoneInput = customerPhoneInput,
                        checkoutError = checkoutError,
                        onIncrement = { onAddToCart(it.menuItem) },
                        onDecrement = onDecrementCartItem,
                        onClearCart = onClearCart,
                        onSelectPaymentMethod = onSelectPaymentMethod,
                        onAmountPaidChange = onAmountPaidChange,
                        onFullAmountShortcut = onFullAmountShortcut,
                        onZeroAmountShortcut = onZeroAmountShortcut,
                        onTransactionIdChange = onTransactionIdChange,
                        onCustomerNameChange = onCustomerNameChange,
                        onCustomerPhoneChange = onCustomerPhoneChange,
                        onCompleteOrder = { onCompleteOrder() },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }

    if (showCartSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = sheetState
        ) {
            CartCheckoutContent(
                cartItems = cartItems,
                cartTotalPkr = cartTotalPkr,
                selectedPaymentMethod = selectedPaymentMethod,
                amountPaidInput = amountPaidInput,
                isAutoUdhar = isAutoUdhar,
                udharRemainingPkr = udharRemainingPkr,
                cashChangePkr = cashChangePkr,
                transactionIdInput = transactionIdInput,
                customerNameInput = customerNameInput,
                customerPhoneInput = customerPhoneInput,
                checkoutError = checkoutError,
                onIncrement = { onAddToCart(it.menuItem) },
                onDecrement = onDecrementCartItem,
                onClearCart = {
                    onClearCart()
                    showCartSheet = false
                },
                onSelectPaymentMethod = onSelectPaymentMethod,
                onAmountPaidChange = onAmountPaidChange,
                onFullAmountShortcut = onFullAmountShortcut,
                onZeroAmountShortcut = onZeroAmountShortcut,
                onTransactionIdChange = onTransactionIdChange,
                onCustomerNameChange = onCustomerNameChange,
                onCustomerPhoneChange = onCustomerPhoneChange,
                onCompleteOrder = {
                    if (onCompleteOrder()) {
                        showCartSheet = false
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun PosMenuCard(
    item: MenuItemEntity,
    quantityInCart: Int,
    onTapAdd: () -> Unit,
    onDecrement: () -> Unit
) {
    val visual = categoryVisualFor(item.category)
    val inCart = quantityInCart > 0

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (inCart) 4.dp else 1.dp,
        shadowElevation = if (inCart) 4.dp else 1.dp,
        border = BorderStroke(
            width = if (inCart) 2.dp else 1.dp,
            color = if (inCart) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.isAvailable) 1f else 0.55f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = item.isAvailable, onClick = onTapAdd)
            .testTag("menu_item_card_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = visual.tintBackground,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = item.category,
                            tint = visual.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!item.isAvailable) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "SOLD OUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (inCart) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${quantityInCart}x",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = visual.accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )

            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (inCart) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("card_dec_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = "Remove one",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = formatPkr(item.pricePkr),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(
                        onClick = onTapAdd,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("card_inc_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add one more",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPkr(item.pricePkr),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add ${item.name}",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
