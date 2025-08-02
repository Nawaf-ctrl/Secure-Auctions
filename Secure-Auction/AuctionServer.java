
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.Signature;  



import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;





import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;










public class AuctionServer extends UnicastRemoteObject implements Auction {
    private PrivateKey privateKey;
           private PublicKey publicKey;  
    private Map<Integer, PublicKey> userPublicKeys = new HashMap<>();


    private Map<Integer, String> tokens = new HashMap<>();                 
    private Map<Integer, String> challenges = new HashMap<>();             // stores challenges for each user
    private Map<Integer, String> users = new HashMap<>();     
    
    
    private Map<Integer, AuctionItem> items = new HashMap<>();   

         private Map<Integer, Boolean> itemIsOpen = new HashMap<>();            
    private Map<Integer, Integer> itemHighestBidder = new HashMap<>();     
                private int userCounter = 1;
    private int itemCounter = 1;

    
    protected AuctionServer() throws RemoteException {
        super();
        generateOrLoadKeyPair();
    }

    
    private void generateOrLoadKeyPair() {
        Path privateKeyPath = Paths.get("server_private.key");
        Path publicKeyPath = Paths.get("keys", "server_public.key");
        try {
            
            if (Files.exists(privateKeyPath)) {
                
                loadKeys(privateKeyPath, publicKeyPath);
            } else {
                
                generateNewKeyPair(privateKeyPath, publicKeyPath);
            }
        } catch (Exception e) {
            System.err.println("Error in generating or loading key pair: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    private void loadKeys(Path privateKeyPath, Path publicKeyPath) throws Exception {
        byte[] privateKeyBytes = Files.readAllBytes(privateKeyPath);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.privateKey = keyFactory.generatePrivate(privateKeySpec);

        
        if (Files.exists(publicKeyPath)) {
            byte[] publicKeyBytes = Files.readAllBytes(publicKeyPath);
            this.publicKey = keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(publicKeyBytes));
        }

        System.out.println("Private key loaded successfully from " + privateKeyPath.toString());
    }

    
    private void generateNewKeyPair(Path privateKeyPath, Path publicKeyPath) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        this.privateKey = pair.getPrivate();
        this.publicKey = pair.getPublic();

        byte[] privateKeyBytes = privateKey.getEncoded();
        byte[] publicKeyBytes = publicKey.getEncoded();

        
        Files.write(privateKeyPath, privateKeyBytes);
        Files.createDirectories(publicKeyPath.getParent()); 
        Files.write(publicKeyPath, publicKeyBytes);

        System.out.println("Generated new RSA key pair and stored them:\n  Private Key: " + privateKeyPath.toString() + "\n  Public Key: " + publicKeyPath.toString());
    }

    
    @Override
    public synchronized int register(String email, PublicKey pkey) throws RemoteException {
        int userID = userCounter++;
        users.put(userID, email);
        userPublicKeys.put(userID, pkey); 
        System.out.println("User registered: " + email + " with ID: " + userID);
        return userID;
    }

    
    @Override
    public synchronized ChallengeInfo challenge(int userID, String clientChallenge) throws RemoteException {
        try {
            String serverChallenge = "challenge_" + System.currentTimeMillis();
            challenges.put(userID, serverChallenge);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey); 
            signature.update(serverChallenge.getBytes());

            byte[] serverResponse = signature.sign();
            ChallengeInfo info = new ChallengeInfo();
            info.response = serverResponse;
            info.serverChallenge = serverChallenge;
            return info;
        } catch (Exception e) {
            throw new RemoteException("Error generating challenge", e);
        }
    }

    @Override
    public synchronized TokenInfo authenticate(int userID, byte[] signature) throws RemoteException {
        try {
            String serverChallenge = challenges.get(userID);
            if (serverChallenge == null) {
                throw new RemoteException("Challenge expired or not found");
            }

            
                    Signature sig = Signature.getInstance("SHA256withRSA");


                PublicKey clientPublicKey = userPublicKeys.get(userID);


            if (clientPublicKey == null) 
            
            {
                throw new RemoteException("No public key found for iD " + userID);
            }
            sig.initVerify(clientPublicKey);

            sig.update(serverChallenge.getBytes());

            if (sig.verify(signature)) {
               
                String tokenStr = generateToken();
                long expiryTime = System.currentTimeMillis() + 10000; 

                tokens.put(userID, tokenStr);

                TokenInfo tokenInfo = new TokenInfo();
                tokenInfo.token = tokenStr;
                tokenInfo.expiryTime = expiryTime;
                return tokenInfo;
            } else {
                throw new RemoteException("verify failed  ");
            }
        } catch (Exception e) {
            throw new RemoteException("Authentication failed", e);
        }
    }

    
    private String generateToken()
    
    {

        return Base64.getEncoder().encodeToString((System.nanoTime() + "").getBytes());
    }

   
    private boolean isTokenValid(int userID, String token) {
        String validToken = tokens.get(userID);
        return validToken != null && validToken.equals(token);
    }

    
    @Override
    public AuctionItem getSpec(int userID, int itemID, String token) throws RemoteException {
        if (!isTokenValid(userID, token)) 
        {

            throw new RemoteException("Invalid or expired token");


        }
        AuctionItem item = items.get(itemID);
        if (item == null)
        
        {
            throw new RemoteException("Item not found.");
        }
        return item;
    }

    @Override
    public synchronized int newAuction(int userID, AuctionSaleItem saleItem, String token) throws RemoteException {
        if (!isTokenValid(userID, token)) {
            throw new RemoteException("Invalid or expired token");
        }

        int itemID = itemCounter++;
        AuctionItem auctionItem = new AuctionItem();
        auctionItem.itemID = itemID;

        auctionItem.name = saleItem.name;


        auctionItem.description = saleItem.description;
                auctionItem.highestBid = saleItem.reservePrice;

        items.put(itemID, auctionItem);
        itemIsOpen.put(itemID, true);  
        itemHighestBidder.put(itemID, 0);  

        System.out.println("New auction created: " + auctionItem.name + " with ID: " + itemID);
        return itemID;
    }

    @Override
    public AuctionItem[] listItems(int userID, String token) throws RemoteException {
        if (!isTokenValid(userID, token)) 
        {
            
            return null;
        }

        
        return items.values().stream()
                .filter(item -> itemIsOpen.getOrDefault(item.itemID, false))
                .toArray(AuctionItem[]::new);
    }

    @Override
    public boolean bid(int userID, int itemID, int price, String token) throws RemoteException {
        if (!isTokenValid(userID, token)) {
            throw new RemoteException("Invalid or expired token");
        }

        
        AuctionItem item = items.get(itemID);
        if (item == null || !itemIsOpen.getOrDefault(itemID, false)) {
            throw new RemoteException("Auction is closed or item not found.");
        }

        
        if (price <= item.highestBid) 
        {


            return false;
        }

        item.highestBid = price;

        itemHighestBidder.put(itemID, userID);
                System.out.println("New bid on item ID " + itemID + " by user ID " + userID + " with price " + price);




        return true;
    }

    @Override
    public AuctionResult closeAuction(int userID, int itemID, String token) throws RemoteException {
        if (!isTokenValid(userID, token)) 
        {


                        throw new RemoteException("Invalid or expired token");
        }

        AuctionItem item = items.get(itemID);
        Boolean open = itemIsOpen.get(itemID);
        if (item == null || open == null || !open) 
        {
                throw new RemoteException("Auction already closed or item not found");


        }

        
        itemIsOpen.put(itemID, false);

        AuctionResult result = new AuctionResult();

        int highestBidderID = itemHighestBidder.getOrDefault(itemID, 0);
        if (highestBidderID == 0) 
        
        {
            
            result.winningEmail = "";    
            result.winningPrice = 0;
            System.out.println("Auction closed for item ID " + itemID + " with no bids.");
        } else {
            String winningEmail = users.get(highestBidderID);
            int finalPrice = item.highestBid;

            result.winningEmail = winningEmail;
            result.winningPrice = finalPrice;

            

            System.out.println("Auction closed for item ID " + itemID + ". Winner: " + winningEmail + ", Final Price: " + finalPrice);
        }

        return result;
    }

    
    public static void main(String[] args) 
    {
        try {
            
            Registry registry;
            try {
                        registry = LocateRegistry.getRegistry(1099);


                registry.list(); //reachable

                 System.out.println("Using existing RMI registry on port 1099.");
            } catch (RemoteException e) {
                //new 
                registry = LocateRegistry.createRegistry(1099);

                  System.out.println("RMI registry started on port 1099.");
            }

            
            AuctionServer server = new AuctionServer();

            
            Naming.rebind("Auction", server);
            System.out.println("Auction Server is ready and bound to Auction");
        } catch (Exception e) 
        
        {
            System.err.println("Server exception:" + e.toString());
            e.printStackTrace();
        }
    }
}
