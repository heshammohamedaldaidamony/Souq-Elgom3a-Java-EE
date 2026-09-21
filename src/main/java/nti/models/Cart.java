package nti.models;

import java.util.ArrayList;
import java.util.List;

public class Cart {

    private List<CartItem> items;

    public Cart() {
        this.items = new ArrayList<>();
    }

    public Cart(List<CartItem> items) {
        this.items = (items != null) ? items : new ArrayList<>();
    }

    /**
     * Add one item (appends to the list — the Service ensures no duplicates).
     */
    public void addItem(CartItem item) {
        this.items.add(item);
    }

    /**
     * Total price across all items.
     */
    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    /**
     * Total number of individual units (sum of quantities).
     * Example: 2 laptops + 3 mice = 5 items.
     */
    public int getTotalItems() {
        int total = 0;
        for (CartItem item : items) {
            total += item.getQuantity();
        }
        return total;
    }

    /**
     * Number of distinct products in the cart.
     * Example: 2 laptops + 3 mice = 2 distinct products.
     */
    public int getDistinctItemCount() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    @Override
    public String toString() {
        return "Cart{" +
                "distinctItems=" + items.size() +
                ", totalQuantity=" + getTotalItems() +
                ", totalPrice=" + getTotalPrice() +
                '}';
    }
}