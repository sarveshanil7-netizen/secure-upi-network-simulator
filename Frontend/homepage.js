
const API_URL = "http://localhost:8080/api";

const demoUsers = {
    Sarvesh1: "111100000001",
    Srimano2: "111100000002",
    kasi3: "111100000003",
    Hasiba4: "111100000004"
};

document.addEventListener("DOMContentLoaded", () => {
    const paymentCards = document.querySelectorAll("#homepage-body .payment");
    const checkButtons = document.querySelectorAll("#check .check-btn");

    paymentCards.forEach((card, index) => {
        card.addEventListener("click", () => {
            if (index === 0) showPaymentForm("qr");
            if (index === 1) showPaymentForm("account");
            if (index === 2) showPaymentForm("phone");
        });
    });

    if (checkButtons[0]) {
        checkButtons[0].addEventListener("click", checkBalance);
    }

    if (checkButtons[1]) {
        checkButtons[1].addEventListener("click", showHistory);
    }

    loadBalance();
});

async function apiRequest(url, options = {}) {
    const response = await fetch(`${API_URL}${url}`, options);

    if (!response.ok) {
        throw new Error(`Request failed: HTTP ${response.status}`);
    }

    return response.json();
}

function getLoggedInAccount() {
    const username = sessionStorage.getItem("loggedInUsername");
    return demoUsers[username] || null;
}

async function loadBalance() {
    const balanceElement = document.getElementById("accountBalance");

    if (!balanceElement) return;

    const accountNumber = getLoggedInAccount();

    if (!accountNumber) {
        balanceElement.textContent = "Please log in to view your balance.";
        return;
    }

    try {
        const data = await apiRequest(
            `/balance?account=${encodeURIComponent(accountNumber)}`
        );

        balanceElement.textContent =
            `₹${Number(data.balance).toLocaleString("en-IN", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            })}`;
    } catch (error) {
        console.error("Balance error:", error);
        balanceElement.textContent = "Unable to load balance.";
    }
}

async function checkBalance() {
    const accountNumber = prompt("Enter your 12-digit account number:");

    if (!accountNumber) return;

    if (!/^\d{12}$/.test(accountNumber.trim())) {
        alert("Please enter a valid 12-digit account number.");
        return;
    }

    try {
        const data = await apiRequest(
            `/balance?account=${encodeURIComponent(accountNumber.trim())}`
        );

        alert(
            `Account: ${data.account}\n` +
            `Balance: ₹${Number(data.balance).toFixed(2)}`
        );
    } catch (error) {
        alert("Could not retrieve the balance. " + error.message);
    }
}

async function showHistory() {
    try {
        const transactions = await apiRequest("/history");

        if (!Array.isArray(transactions) || transactions.length === 0) {
            alert("No transactions found.");
            return;
        }

        const historyText = transactions.map(transaction =>
            typeof transaction === "string"
                ? transaction
                : JSON.stringify(transaction)
        ).join("\n");

        alert("Transaction History\n\n" + historyText);
    } catch (error) {
        alert("Could not load transaction history. " + error.message);
    }
}

function showPaymentForm(method) {
    let form = document.getElementById("payment-form");

    if (!form) {
        form = document.createElement("section");
        form.id = "payment-form";
        form.className = "payment-form";
        document.getElementById("homepage-body").appendChild(form);
    }

    const labels = {
        qr: "QR Code / Receiver Account Number",
        account: "Receiver Account Number",
        phone: "Receiver Phone Number"
    };

    form.innerHTML = `
        <h2>Pay Using ${
            method === "qr" ? "QR Code" :
            method === "account" ? "Account Number" : "Phone Number"
        }</h2>

        <label for="sender-account">Your Account Number</label>
        <input id="sender-account" type="text"
            value="${getLoggedInAccount() || ""}"
            placeholder="12-digit account number">

        <label for="receiver-detail">${labels[method]}</label>
        <input id="receiver-detail" type="text"
            placeholder="${
                method === "phone" ? "10-digit phone number" :
                method === "qr" ? "Enter scanned QR text or account number" :
                "12-digit account number"
            }">

        ${method === "qr" ? `
            <button type="button" id="scan-qr">Scan QR</button>
            <div id="qr-reader"></div>
        ` : ""}

        <label for="payment-amount">Amount (₹)</label>
        <input id="payment-amount" type="number"
            min="0.01" step="0.01" placeholder="Enter amount">

        <button type="button" id="send-payment">Pay Now</button>
        <button type="button" id="close-payment">Close</button>

        <p id="payment-message" role="status"></p>
    `;

    document.getElementById("close-payment").onclick = async () => {
        await stopQrScanner();
        form.remove();
    };

    document.getElementById("send-payment").onclick = () => {
        sendPayment(method);
    };

    const scanButton = document.getElementById("scan-qr");
    if (scanButton) {
        scanButton.onclick = startQrScanner;
    }

    form.scrollIntoView({ behavior: "smooth", block: "start" });
}

async function sendPayment(method) {
    const senderAccount =
        document.getElementById("sender-account").value.trim();

    const receiverInput =
        document.getElementById("receiver-detail").value.trim();

    const amountText =
        document.getElementById("payment-amount").value;

    const amount = Number(amountText);
    const message = document.getElementById("payment-message");

    if (!senderAccount || !receiverInput || !amountText) {
        message.textContent = "Please fill in all fields.";
        return;
    }

    if (!/^\d{12}$/.test(senderAccount)) {
        message.textContent = "Enter a valid 12-digit sender account number.";
        return;
    }

    if (!Number.isFinite(amount) || amount <= 0 ||
        !/^\d+(\.\d{1,2})?$/.test(amountText)) {
        message.textContent =
            "Enter a valid amount with at most two decimal places.";
        return;
    }

    let receiverAccount = receiverInput;

    if (method === "qr" && receiverInput.startsWith("simupi://")) {
        try {
            const qrUrl = new URL(receiverInput);
            receiverAccount =
                qrUrl.searchParams.get("accountNumber") || "";
        } catch {
            message.textContent = "Invalid QR code.";
            return;
        }
    }

    if (method === "phone") {
        if (!/^\d{10}$/.test(receiverInput)) {
            message.textContent = "Enter a valid 10-digit phone number.";
            return;
        }

        message.textContent =
            "Phone-number lookup is not yet available in the Java HTTP API.";
        return;
    }

    if (!/^\d{12}$/.test(receiverAccount)) {
        message.textContent =
            "Enter a valid 12-digit receiver account number.";
        return;
    }

    if (senderAccount === receiverAccount) {
        message.textContent = "Sender and receiver must be different.";
        return;
    }

    message.textContent =
        "The website payment endpoint is not connected yet. " +
        "Use your TCPClient for payment testing for now.";
}

let qrScanner = null;

async function startQrScanner() {
    const message = document.getElementById("payment-message");

    if (!window.Html5Qrcode) {
        message.textContent =
            "QR scanner library is unavailable. Enter the account number manually.";
        return;
    }

    try {
        qrScanner = new Html5Qrcode("qr-reader");

        await qrScanner.start(
            { facingMode: "environment" },
            { fps: 10, qrbox: 200 },
            async decodedText => {
                document.getElementById("receiver-detail").value = decodedText;
                message.textContent = "QR code scanned.";
                await stopQrScanner();
            }
        );
    } catch (error) {
        console.error("QR scanner error:", error);
        message.textContent =
            "Camera unavailable. Allow camera access or enter the account number manually.";
    }
}

async function stopQrScanner() {
    if (!qrScanner) return;

    try {
        if (qrScanner.isScanning) {
            await qrScanner.stop();
        }

        qrScanner.clear();
    } catch (error) {
        console.error("Could not stop QR scanner:", error);
    }

    qrScanner = null;
}
