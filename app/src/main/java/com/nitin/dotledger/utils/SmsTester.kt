package com.nitin.dotledger.utils

object SmsTester {

    /**
     * Sample bank SMS messages for testing
     */
    val sampleBankSms = listOf(
        SampleSms(
            sender = "HDFCBK",
            message = "Your A/c XX1234 is debited with Rs.2500.00 on 27Dec25 and the available balance is Rs.15000.00. UPI Ref no 436372847382."
        ),
        SampleSms(
            sender = "ICICIB",
            message = "Rs.1,250.00 debited from A/c **4567 on 27-Dec-25 to Amazon India. Avl Bal: INR 25,340.50. Call 18002662 for dispute"
        ),
        SampleSms(
            sender = "AXISBK",
            message = "Your A/C XX9876 credited with INR 50000.00 on 27-DEC-25. Available Balance: INR 65000.00. Ref:SAL/DEC/2025"
        ),
        SampleSms(
            sender = "SBIIN",
            message = "Dear Customer, INR 350.00 debited from A/c **2345 on 27Dec25 for Swiggy payment. Available Balance: Rs.8,450.00"
        ),
        SampleSms(
            sender = "KOTAKB",
            message = "A/c X-1111 debited Rs 5,400.00 on 27-Dec-25. Info: ZOMATO. Available bal Rs 42,300.00"
        ),
        SampleSms(
            sender = "HDFCBK",
            message = "Rs.150.00 spent on HDFC Bank Credit Card XX4321 at STARBUCKS COFFEE on 27Dec25. Available Limit: Rs.45000.00"
        ),
        SampleSms(
            sender = "ICICIB",
            message = "Refund of Rs.899.00 processed to your Card **7890 on 27-Dec-25 from Flipkart. Avl Bal: Rs.32,150.00"
        ),
        SampleSms(
            sender = "AXISBK",
            message = "Your payment of Rs.2,599.00 to Reliance JIO is successful from A/c **3456. Balance: Rs.18,750.00"
        )
    )

    /**
     * Test SMS parsing with sample messages
     */
    fun testParsing(): List<TestResult> {
        return sampleBankSms.map { sample ->
            val parsed = SmsParser.parseTransaction(sample.sender, sample.message)
            TestResult(
                sample = sample,
                parsed = parsed,
                success = parsed != null
            )
        }
    }

    /**
     * Get test message by index
     */
    fun getTestMessage(index: Int): SampleSms? {
        return sampleBankSms.getOrNull(index)
    }
}

data class SampleSms(
    val sender: String,
    val message: String
)

data class TestResult(
    val sample: SampleSms,
    val parsed: ParsedTransaction?,
    val success: Boolean
)