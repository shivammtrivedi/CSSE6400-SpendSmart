const bcrypt = require("bcrypt");
const mongoose = require("mongoose");

require("../models/user");
require("../models/transaction");

const User = mongoose.model("user");
const Transaction = mongoose.model("transaction");

const SEED_TAG = "[seed-demo]";
const DEFAULT_EMAIL = "test@example.com";
const DEFAULT_PASSWORD = "password123";
const DEFAULT_ACCOUNT_NUMBER = 5551234;
const BASE_BALANCE = 300000;

const CATEGORIES = ["food", "drinks", "rent"];

function buildSeedTransactions(accountNumber) {
    const transactions = [];
    for (let i = 0; i < 30; i += 1) {
        const category = CATEGORIES[i % CATEGORIES.length];
        let amount;

        if (category === "food") amount = 1200 + i * 37;
        else if (category === "drinks") amount = 700 + i * 21;
        else amount = 9000 + i * 53; // rent

        const when = new Date();
        when.setDate(when.getDate() - (29 - i));

        transactions.push({
            transactionType: "Withdraw",
            accountNumber,
            sender: "merchant",
            description: `${SEED_TAG} category=${category} item=${i + 1}`,
            transactionAmount: amount,
            transactionTime: when,
        });
    }
    return transactions;
}

async function ensureMongoConnected() {
    if (mongoose.connection.readyState === 1) return;
    await new Promise((resolve, reject) => {
        const onConnected = () => {
            mongoose.connection.off("error", onError);
            resolve();
        };
        const onError = (err) => {
            mongoose.connection.off("connected", onConnected);
            reject(err);
        };
        mongoose.connection.once("connected", onConnected);
        mongoose.connection.once("error", onError);
    });
}

async function seedDemoAccount() {
    if (String(process.env.BANK_API_SEED_DATA).toLowerCase() !== "true") {
        console.log("Bank API seed disabled (BANK_API_SEED_DATA != true)");
        return;
    }

    await ensureMongoConnected();

    const email = (process.env.BANK_API_SEED_EMAIL || DEFAULT_EMAIL).toLowerCase();
    const password = process.env.BANK_API_SEED_PASSWORD || DEFAULT_PASSWORD;
    const accountNumber = Number(process.env.BANK_API_SEED_ACCOUNT_NUMBER || DEFAULT_ACCOUNT_NUMBER);

    let user = await User.findOne({ email });
    if (!user) {
        const hashedPassword = await bcrypt.hash(password, 10);
        user = await User.create({
            fullName: "Test User",
            email,
            password: hashedPassword,
            accountNumber,
            accountType: "savings",
            accountBalance: BASE_BALANCE,
        });
        console.log(`Bank API seed: created demo user ${email}`);
    } else {
        console.log(`Bank API seed: demo user already exists ${email}`);
    }

    const seedTxns = buildSeedTransactions(user.accountNumber);
    const seededAmount = seedTxns.reduce((sum, t) => sum + t.transactionAmount, 0);

    // Keep this idempotent: remove old demo txns, then recreate 30 canonical records.
    await Transaction.deleteMany({
        accountNumber: user.accountNumber,
        description: new RegExp(`^\\${SEED_TAG}`),
    });
    await Transaction.insertMany(seedTxns);

    user.accountBalance = Math.max(BASE_BALANCE - seededAmount, 0);
    await user.save();

    console.log(`Bank API seed: inserted ${seedTxns.length} demo transactions for ${email}`);
}

module.exports = { seedDemoAccount };
