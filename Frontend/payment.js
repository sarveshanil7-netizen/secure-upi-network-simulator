
const API_URL = "http://localhost:8080/api";

function setMessage(id, message) {
    const element = document.getElementById(id);
    if (element) {
        element.textContent = message;
    }
}

function getLoggedInUsername() {
    const username = sessionStorage.getItem("loggedInUsername");

    if (!username) {
        window.location.href = "login.html";
        return null;
    }

    return username;
}

async function makePayment(receiverAccount, amountText, password, messageId) {
    const username = getLoggedInUsername();

    if (!username) {
        setMessage(messageId, "Please log in again.");
        return;
    }

    if (!/^\d+(\.\d{1,2})?$/.test(amountText)) {
        setMessage(messageId, "Enter a valid amount.");
        return;
    }

    const amount = Number(amountText);

    if (!Number.isFinite(amount) || amount <= 0) {
        setMessage(messageId, "Amount must be greater than zero.");
        return;
    }

    if (!/^\d{12}$/.test(receiverAccount)) {
        setMessage(messageId, "Enter a valid 12-digit account number.");
        return;
    }

    if (!password) {
        setMessage(messageId, "Enter your payment password.");
        return;
    }

    try {
        setMessage(messageId, "Processing payment...");

        const response = await fetch(`${API_URL}/payment`, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({
                username: username,
                receiverAccount: receiverAccount,
                amount: amount.toFixed(2),
                password: password
            })
        });

        const result = await response.json();

        if (!response.ok || !result.success) {
            setMessage(
                messageId,
                result.message || "Payment failed."
            );
            return;
        }

        alert(
            "Payment successful!\n" +
            "Transaction ID: " + result.transactionId + "\n" +
            "Amount: ₹" + amount.toFixed(2)
        );

        window.location.href = "homepage.html";
    } catch (error) {
        console.error(error);
        setMessage(
            messageId,
            "Cannot connect to the payment server. Make sure UPIApplication is running."
        );
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const username = getLoggedInUsername();
    if (!username) return;

    const payByAccount = document.getElementById("payByAccount");

    if (payByAccount) {
        payByAccount.addEventListener("click", () => {
            const receiverAccount =
                document.getElementById("receiverAccount").value.trim();

            const amountText =
                document.getElementById("amount").value.trim();

            const passwordField =
                document.getElementById("paymentPassword");

            const password = passwordField
                ? passwordField.value
                : "";

            makePayment(
                receiverAccount,
                amountText,
                password,
                "accountMessage"
            );
        });
    }

    const payByPhone = document.getElementById("payByPhone");

    if (payByPhone) {
        payByPhone.addEventListener("click", async () => {
            const phone =
                document.getElementById("receiverPhone").value.trim();

            const phoneAmount =
                document.getElementById("phoneAmount").value.trim();

            const passwordField =
                document.getElementById("paymentPassword");

            const password = passwordField
                ? passwordField.value
                : "";

            if (!/^\d{10}$/.test(phone)) {
                setMessage("phoneMessage", "Enter a valid 10-digit phone number.");
                return;
            }

            try {
                const response = await fetch(`${API_URL}/account-by-phone?phone=${encodeURIComponent(phone)}`);
                const result = await response.json();

                if (!response.ok || !result.accountNumber) {
                    setMessage("phoneMessage", "This phone number is not registered.");
                    return;
                }

                await makePayment(
                    result.accountNumber,
                    phoneAmount,
                    password,
                    "phoneMessage"
                );
            } catch (error) {
                console.error(error);
                setMessage("phoneMessage", "Cannot connect to the payment server.");
            }
        });
    }
});
