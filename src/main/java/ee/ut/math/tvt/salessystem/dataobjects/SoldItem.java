package ee.ut.math.tvt.salessystem.dataobjects;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "SOLD_ITEM")
public class SoldItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @ManyToOne
    @JoinColumn(name = "stock_item_id")
    private StockItem stockItem;

    @Column(name = "name")
    private String name;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "price")
    private double price;

    @Column(name = "total")
    private double total;

    public SoldItem(StockItem stockItem, int quantity, double total) {
        this.stockItem = stockItem;
        this.name = stockItem.getName();
        this.price = stockItem.getPrice();
        this.quantity = quantity;
        this.total = total;
    }

    public SoldItem() {

    }


    public double getTotal(){
        return total;
    }
    public void setTotal(double total){
        this.total = total;
    }
    public Sale getSales() {
        return sale;
    }
    public void setSale(Sale sale){
        this.sale = sale;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.total = calculateTotal();
    }

    public double calculateTotal() {
        return quantity * price;
    }

    public Long getStockItemId() {
        return stockItem.getId();
    }
    public void setStockItemId(Long stockItemId) {
        this.stockItem.setId(stockItemId);
    }

    public StockItem getStockItem() {
        return this.stockItem;
    }

    @Override
    public String toString() {
        return "Solditem{ StockItemId= " + stockItem.getID() + ", name= " + name + ", quantity= " + quantity + "}";
    }
}
