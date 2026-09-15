package com.bookmyturf.app.data

import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Clean payment abstraction so the demo simulation can be replaced with
 * a real payment gateway (Razorpay, Cashfree, etc.) without altering the
 * booking UI or ViewModel architecture.
 */
interface PaymentService {
    suspend fun processPayment(request: PaymentRequest): PaymentResult
}

data class PaymentRequest(
    val amount: Double,
    val turfName: String,
    val customerName: String,
    val paymentMethod: String = "DEMO_UPI"
)

sealed class PaymentResult {
    data class Success(
        val transactionId: String,
        val amount: Double,
        val timestamp: Long,
        val method: String
    ) : PaymentResult()

    data class Failed(
        val reason: String,
        val errorCode: String? = null
    ) : PaymentResult()

    data object Cancelled : PaymentResult()
}

enum class DemoPaymentMode {
    SUCCESS,
    FAILED,
    CANCELLED
}

/**
 * Realistic Indian UPI Demo Payment implementation.
 * Simulates real gateway communication (2.5s delay) and returns realistic
 * demo transaction IDs (e.g. BMT-DEMO-8F72A91C).
 */
class DemoPaymentService : PaymentService {
    var mode: DemoPaymentMode = DemoPaymentMode.SUCCESS

    override suspend fun processPayment(request: PaymentRequest): PaymentResult {
        // Realistic 2.5-second processing delay matching modern UPI gateways
        delay(2500)
        return when (mode) {
            DemoPaymentMode.SUCCESS -> {
                val hex = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
                PaymentResult.Success(
                    transactionId = "BMT-DEMO-$hex",
                    amount = request.amount,
                    timestamp = System.currentTimeMillis(),
                    method = request.paymentMethod
                )
            }
            DemoPaymentMode.FAILED -> {
                PaymentResult.Failed("Demo transaction declined by simulated UPI network. (Developer simulation mode)")
            }
            DemoPaymentMode.CANCELLED -> {
                PaymentResult.Cancelled
            }
        }
    }
}
