import java.rmi.Naming;
import java.security.PublicKey;
import java.util.Scanner;

public class AuctionClient {
    public static void main(String[] args) {
        try {
            
            Auction auction = (Auction) Naming.lookup("rmi://localhost:1099/Auction");
            Scanner scanner = new Scanner(System.in);
            System.out.println("Welcome to Auction System" );



            
                
            System.out.print("Enter your email please: ");
            String email = scanner.nextLine();
           
            
            PublicKey publicKey = getClientPublicKey(); 
            int userID = auction.register(email, publicKey);
            System.out.println("Your user ID: " + userID);

            
            String token = authenticateUser(userID, auction, scanner); 

            
            while (true) {
                System.out.println("\nMenu:");
                System.out.println("1. Create new auction");
                System.out.println("2 List open auctions");
                System.out.println("4 Bid on an item");
                System.out.println("4 Close an auction");
                System.out.println("5. Get item details");
                System.out.println("6 Exit");
                System.out.print("Choose an option: ");
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        
                        System.out.print("Enter item name: ");
                        String itemName = scanner.nextLine();
                        System.out.print("Enter item description: ");
                        String itemDescription = scanner.nextLine();
                        System.out.print("Enter starting price (reserve price): ");
                        int startingPrice = Integer.parseInt(scanner.nextLine());

                        AuctionSaleItem saleItem = new AuctionSaleItem();
                        saleItem.name = itemName;
                        saleItem.description = itemDescription;
                        saleItem.reservePrice = startingPrice;

                        int itemID = auction.newAuction(userID, saleItem, token);
                        if (itemID == -1) {
                            System.out.println(" failed");
                        } else {
                            System.out.println("Auction  item ID: " + itemID);
                        }
                        break;

                    case 2:
                        

                                AuctionItem[] items = auction.listItems(userID, token);
                        if (items == null) 
                        {
                            System.out.println("unable to  list item  ");
                            break;


                        }

                        System.out.println("open auctions:");
                        if (items.length > 0) {
                            for (AuctionItem item : items) {
                                        System.out.println("Item ID: " + item.itemID + ", Name: " + item.name + ", Current Bid: " + item.highestBid);
                            }
                        } else {
                            System.out.println("No open auctions available.");
                        }
                        break;

                    case 3:
                        


                        System.out.print("Enter item ID to bid on: ");
                        int bidItemID = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter your bid amount: ");
                        int bidAmount = Integer.parseInt(scanner.nextLine());

                        boolean success = auction.bid(userID, bidItemID, bidAmount, token);
                        if (success) {
                            System.out.println(" bid placed successfully.");
                        } else {
                            System.out.println("Your bid may be lower than the current highest bid or the token is invalid/expired.");
                        }
                        break;

                    case 4:
                        
                        System.out.print("Enter Item ID to close: ");
                        int closeItemID = Integer.parseInt(scanner.nextLine());

                        AuctionResult result = auction.closeAuction(userID, closeItemID, token);
                        if (result == null) {
                            System.out.println("Failed to close auction. The token may be invalid/expired or the item may not be found.");
                        } else if (result.winningEmail != null && !result.winningEmail.isEmpty()) {
                            System.out.println("Auction closed. Winner Email: " + result.winningEmail +
                                    ", Final Price: " + result.winningPrice);
                        } else {
                            System.out.println("Auction closed with no bids.");
                        }
                         break;

                    case 5:
                        
                        System.out.print("Enter Item ID to get details: ");
                        int detailItemID = Integer.parseInt(scanner.nextLine());

                        AuctionItem itemDetail = auction.getSpec(userID, detailItemID, token);
                        if (itemDetail == null) {
                            System.out.println("Item not found or token may be invalid/expired.");
                        } else {
                            System.out.println("Item Details:");
                            System.out.println("Item ID: " + itemDetail.itemID);
                            System.out.println("Name: " + itemDetail.name);
                            System.out.println("Description: " + itemDetail.description);
                            System.out.println("Highest Bid: " + itemDetail.highestBid);
                        }
                        break;

                    case 6:
                        
                        System.out.println("Exiting...");
                        scanner.close();
                        System.exit(0);

                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    
    private static PublicKey getClientPublicKey() {    
        return null;
    }

    
                private static String authenticateUser(int userID, Auction auction, Scanner scanner) {
        
                    return "sampleToken"; 
    }
}
