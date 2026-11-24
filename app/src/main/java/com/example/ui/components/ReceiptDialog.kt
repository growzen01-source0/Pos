package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.OrderEntity
import com.example.data.local.OrderWithItems
import com.example.data.model.formatPkr
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptDialog(
    orderWithItems: OrderWithItems,
    shopName: String,
    receiptFooter: String,
    onReceiveUdharPayment: (OrderEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
    val shortDateFormatter = SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault())
    val formattedDate = dateFormatter.format(Date(orderWithItems.order.createdAt))
    val orderNumberStr = "#%04d".format(orderWithItems.order.orderNumber)
    val order = orderWithItems.order
    val hasUdharDue = order.isUdharPending

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .testTag("receipt_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (hasUdharDue) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasUdharDue) Icons.Rounded.Receipt else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (hasUdharDue) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasUdharDue) {
                                "Udhar (Credit) • ${order.paymentMethod}"
                            } else {
                                "Paid in Full • ${order.paymentMethod}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (hasUdharDue) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = shopName,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Order $orderNumberStr",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Customer & Online Transaction Info Banner
                if (!order.customerName.isNullOrBlank() || !order.customerPhone.isNullOrBlank() || !order.transactionId.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            order.customerName?.let { name ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                            order.customerPhone?.let { phone ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = phone,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            order.transactionId?.let { txId ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PSID / Txn ID: $txId",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                DashedReceiptDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ITEM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                orderWithItems.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.quantity}x ${item.itemName}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "@ ${formatPkr(item.unitPricePkr)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = formatPkr(item.lineTotalPkr),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                DashedReceiptDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Items (${order.itemCount})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (hasUdharDue) "Udhar Active" else "Settled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ORDER TOTAL",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = formatPkr(order.totalPkr),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Highlight Paid Amount, Installments, and Remaining Udhar Balance
                if (hasUdharDue || orderWithItems.payments.size > 1 || order.paidPkr < order.totalPkr) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (hasUdharDue) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Paid",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = formatPkr(order.paidPkr),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "REMAINING UDHAR BALANCE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (hasUdharDue) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = formatPkr(order.udharPkr),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (hasUdharDue) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.tertiary
                                )
                            }

                            if (orderWithItems.payments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "PAYMENT LOG",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                orderWithItems.payments.forEach { p ->
                                    Text(
                                        text = "• ${shortDateFormatter.format(Date(p.paidAt))}: ${formatPkr(p.amountPkr)} (${p.paymentMethod})",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                val cashGiven = order.cashReceivedPkr
                if (!hasUdharDue && cashGiven != null && cashGiven > order.totalPkr) {
                    val change = cashGiven - order.totalPkr
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Cash Received: ${formatPkr(cashGiven)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Change: ${formatPkr(change)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                if (hasUdharDue) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onReceiveUdharPayment(order) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("receipt_receive_udhar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Receive Udhar Payment (${formatPkr(order.udharPkr)} Due)")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = receiptFooter,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Print & Share Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            printOrderReceipt(
                                context = context,
                                orderWithItems = orderWithItems,
                                shopName = shopName,
                                formattedDate = formattedDate,
                                footer = receiptFooter
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("receipt_print_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Print,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print")
                    }

                    OutlinedButton(
                        onClick = {
                            val shareText = buildPlainTextReceipt(
                                orderWithItems = orderWithItems,
                                shopName = shopName,
                                formattedDate = formattedDate,
                                footer = receiptFooter
                            )
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "$shopName Receipt $orderNumberStr")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(
                                Intent.createChooser(sendIntent, "Share Receipt")
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("receipt_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("receipt_done_button")
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun DashedReceiptDivider() {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        drawLine(
            color = lineColor,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        )
    }
}

private fun buildPlainTextReceipt(
    orderWithItems: OrderWithItems,
    shopName: String,
    formattedDate: String,
    footer: String
): String {
    val order = orderWithItems.order
    val orderNum = "#%04d".format(order.orderNumber)
    val shortDateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
    return buildString {
        appendLine(shopName)
        appendLine("Order: $orderNum")
        appendLine("Date: $formattedDate")
        appendLine("Payment: ${order.paymentMethod} (${if (order.isUdharPending) "Udhar" else "Settled"})")
        if (!order.transactionId.isNullOrBlank()) {
            appendLine("PSID / Transaction ID: ${order.transactionId}")
        }
        if (!order.customerName.isNullOrBlank()) {
            appendLine("Customer: ${order.customerName}")
        }
        if (!order.customerPhone.isNullOrBlank()) {
            appendLine("Phone: ${order.customerPhone}")
        }
        appendLine("--------------------------------")
        orderWithItems.items.forEach { item ->
            appendLine("${item.quantity}x ${item.itemName} — ${formatPkr(item.lineTotalPkr)}")
        }
        appendLine("--------------------------------")
        appendLine("ORDER TOTAL: ${formatPkr(order.totalPkr)}")
        if (order.isUdharPending || orderWithItems.payments.size > 1) {
            appendLine("PAID AMOUNT: ${formatPkr(order.paidPkr)}")
            appendLine("REMAINING UDHAR BALANCE: ${formatPkr(order.udharPkr)}")
            orderWithItems.payments.forEach { p ->
                appendLine("  • ${shortDateFormatter.format(Date(p.paidAt))}: ${formatPkr(p.amountPkr)} (${p.paymentMethod})")
            }
        } else {
            val cashGiven = order.cashReceivedPkr
            if (cashGiven != null && cashGiven > order.totalPkr) {
                appendLine("Cash Received: ${formatPkr(cashGiven)} | Change: ${formatPkr(cashGiven - order.totalPkr)}")
            }
        }
        appendLine()
        appendLine(footer)
    }
}

private fun printOrderReceipt(
    context: Context,
    orderWithItems: OrderWithItems,
    shopName: String,
    formattedDate: String,
    footer: String
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val orderNum = "#%04d".format(orderWithItems.order.orderNumber)
    val jobName = "$shopName Receipt $orderNum"

    val adapter = object : PrintDocumentAdapter() {
        private var pdfDocument: PrintedPdfDocument? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            pdfDocument = PrintedPdfDocument(context, newAttributes)
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder("receipt_$orderNum.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val doc = pdfDocument ?: return
            val page = doc.startPage(0)
            drawReceiptOnPdfCanvas(
                canvas = page.canvas,
                pageInfo = page.info,
                orderWithItems = orderWithItems,
                shopName = shopName,
                formattedDate = formattedDate,
                footer = footer
            )
            doc.finishPage(page)

            try {
                FileOutputStream(destination.fileDescriptor).use { out ->
                    doc.writeTo(out)
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                doc.close()
                pdfDocument = null
            }
        }
    }

    printManager.print(jobName, adapter, PrintAttributes.Builder().build())
}

private fun drawReceiptOnPdfCanvas(
    canvas: Canvas,
    pageInfo: PdfDocument.PageInfo,
    orderWithItems: OrderWithItems,
    shopName: String,
    formattedDate: String,
    footer: String
) {
    val order = orderWithItems.order
    val left = 40f
    val right = pageInfo.pageWidth - 40f
    var y = 52f

    val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val headingPaint = Paint().apply {
        color = Color.BLACK
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bodyPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 12f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }
    val boldMonoPaint = Paint().apply {
        color = Color.BLACK
        textSize = 13f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    val udharPaint = Paint().apply {
        color = Color.rgb(180, 70, 10)
        textSize = 14f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    val linePaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 1f
    }

    canvas.drawText(shopName, left, y, titlePaint)
    y += 22f
    canvas.drawText("Order #%04d  •  %s".format(order.orderNumber, formattedDate), left, y, bodyPaint)
    y += 18f
    canvas.drawText("Payment: ${order.paymentMethod}", left, y, bodyPaint)
    y += 18f

    if (!order.transactionId.isNullOrBlank()) {
        canvas.drawText("PSID / Transaction ID: ${order.transactionId}", left, y, bodyPaint)
        y += 18f
    }
    if (!order.customerName.isNullOrBlank() || !order.customerPhone.isNullOrBlank()) {
        val custLine = listOfNotNull(order.customerName, order.customerPhone).joinToString(" | ")
        canvas.drawText("Customer: $custLine", left, y, bodyPaint)
        y += 18f
    }

    y += 6f
    canvas.drawLine(left, y, right, y, linePaint)
    y += 20f

    canvas.drawText("ITEM", left, y, headingPaint)
    val amountHeader = "AMOUNT"
    canvas.drawText(amountHeader, right - headingPaint.measureText(amountHeader), y, headingPaint)
    y += 18f

    orderWithItems.items.forEach { item ->
        val label = "${item.quantity}x ${item.itemName}"
        val priceText = formatPkr(item.lineTotalPkr)
        canvas.drawText(label, left, y, bodyPaint)
        canvas.drawText(priceText, right - bodyPaint.measureText(priceText), y, bodyPaint)
        y += 18f
    }

    y += 6f
    canvas.drawLine(left, y, right, y, linePaint)
    y += 22f

    val totalText = formatPkr(order.totalPkr)
    canvas.drawText("ORDER TOTAL", left, y, boldMonoPaint)
    canvas.drawText(totalText, right - boldMonoPaint.measureText(totalText), y, boldMonoPaint)
    y += 20f

    if (order.isUdharPending || orderWithItems.payments.size > 1 || order.paidPkr < order.totalPkr) {
        val paidText = formatPkr(order.paidPkr)
        val udharText = formatPkr(order.udharPkr)
        canvas.drawText("PAID AMOUNT", left, y, boldMonoPaint)
        canvas.drawText(paidText, right - boldMonoPaint.measureText(paidText), y, boldMonoPaint)
        y += 20f
        canvas.drawText("REMAINING UDHAR BALANCE", left, y, udharPaint)
        canvas.drawText(udharText, right - udharPaint.measureText(udharText), y, udharPaint)
        y += 22f
    }

    y += 12f
    canvas.drawText(footer, left, y, bodyPaint)
}
