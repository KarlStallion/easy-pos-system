package ee.ut.math.tvt.salessystem.dataobjects;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "SALE")
public class Sale implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private String date;

    @Column(name = "time")
    private String time;

    @Column(name = "total")
    private double total;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SoldItem> soldItems;

    public Sale() {
    }

    public Sale(String date, String time, double total, List<SoldItem> soldItems) {
        this.date = date;
        this.time = time;
        this.total = total;
        this.soldItems = soldItems;

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<SoldItem> getSoldItems() {
        return soldItems;
    }

    public void setSoldItems(List<SoldItem> soldItems) {
        this.soldItems = soldItems;
    }

    @Override
    public String toString() {
        return "Sale{" +
                "id=" + id +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", total=" + total +
                ", soldItems=" + soldItems +
                '}';
    }
}
