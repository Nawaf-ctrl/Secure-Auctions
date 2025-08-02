# 🧾 Distributed Auction System (Java RMI)

A scalable, secure Java RMI-based auction system that supports listing, bidding, and user authentication using RSA-based challenge-response.

---

## 🚀 Features

### ✅ Core Auctioning
- Register users and assign unique IDs
- Create new auction listings with reserve prices
- Browse active auctions
- Place bids on listed items
- Close auctions and determine winners
- Support for multiple concurrent clients

### 🔐 Secure Mode (RSA Authentication)
- Public key registration
- 3-step challenge-response authentication using 2048-bit RSA
- One-time, time-limited tokens (10s expiry) for secure API calls
- All requests validated with `SHA256withRSA` digital signatures

---

## 📦 RMI Interface

### Public Methods

```java
int register(String email);
AuctionItem getSpec(int itemID);
int newAuction(int userID, AuctionSaleItem item);
AuctionItem[] listItems();
AuctionResult closeAuction(int userID, int itemID);
boolean bid(int userID, int itemID, int price);
With Authentication Enabled
java
Copy
Edit
int register(String email, PublicKey pkey);
ChallengeInfo challenge(int userID, String clientChallenge);
TokenInfo authenticate(int userID, byte[] signature);

AuctionItem getSpec(int userID, int itemID, String token);
int newAuction(int userID, AuctionSaleItem item, String token);
AuctionItem[] listItems(int userID, String token);
AuctionResult closeAuction(int userID, int itemID, String token);
boolean bid(int userID, int itemID, int price, String token);
🧱 Object Models
java
Copy
Edit
// Auction item returned to clients
class AuctionItem implements Serializable {
    int itemID;
    String name;
    String description;
    int highestBid;
}

// Used to create listings
class AuctionSaleItem implements Serializable {
    String name;
    String description;
    int reservePrice;
}

// Returned when auction closes
class AuctionResult implements Serializable {
    String winningEmail;
    int winningPrice;
}
Secure Auth Models
java
Copy
Edit
// Returned during server challenge
class ChallengeInfo implements Serializable {
    byte[] response;           // server signature of client challenge
    String serverChallenge;
}

// Token used for authenticated access
class TokenInfo implements Serializable {
    String token;              // one-time use
    long expiryTime;           // Unix timestamp
}
🔧 Running the Server
bash
Copy
Edit
./server.sh
Starts RMI registry and compiles all necessary classes.

Server is available under the name "Auction".

💡 Usage
Basic Mode
Register a user

List or create auction items

Bid on items and close auctions

Secure Mode
Register with RSA public key

Perform challenge-response authentication

Use token for any auction interaction (10s expiry)

🔐 Key Folder Structure
vbnet
Copy
Edit
/keys
├── testKey.aes            # AES key (optional)
└── server_public.key      # RSA public key for clients
🧪 Testing and Timings
Server must fully start within 5 seconds of running server.sh.

Tokens must be validated as:

Issued by the server

Not expired

Not reused
